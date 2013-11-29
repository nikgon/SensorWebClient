/**
 * ﻿Copyright (C) 2012
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */

package org.n52.server.da;

import static org.n52.oxf.sos.adapter.ISOSRequestBuilder.GET_FOI_SERVICE_PARAMETER;
import static org.n52.oxf.sos.adapter.ISOSRequestBuilder.GET_FOI_VERSION_PARAMETER;
import static org.n52.oxf.sos.adapter.SOSAdapter.GET_FEATURE_OF_INTEREST;
import static org.n52.server.parser.ConnectorUtils.setVersionNumbersToMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.opengis.gml.x32.FeaturePropertyType;
import net.opengis.sampling.x20.SFSamplingFeatureDocument;
import net.opengis.sampling.x20.SFSamplingFeatureType;
import net.opengis.sos.x20.GetFeatureOfInterestResponseDocument;

import org.apache.xmlbeans.XmlObject;
import org.n52.io.crs.CRSUtils;
import org.n52.oxf.OXFException;
import org.n52.oxf.adapter.OperationResult;
import org.n52.oxf.adapter.ParameterContainer;
import org.n52.oxf.ows.ServiceDescriptor;
import org.n52.oxf.ows.capabilities.Contents;
import org.n52.oxf.ows.capabilities.IBoundingBox;
import org.n52.oxf.ows.capabilities.Operation;
import org.n52.oxf.sos.adapter.SOSAdapter;
import org.n52.oxf.sos.capabilities.ObservationOffering;
import org.n52.server.parser.ConnectorUtils;
import org.n52.server.util.SosAdapterFactory;
import org.n52.shared.serializable.pojos.sos.Feature;
import org.n52.shared.serializable.pojos.sos.Offering;
import org.n52.shared.serializable.pojos.sos.Phenomenon;
import org.n52.shared.serializable.pojos.sos.Procedure;
import org.n52.shared.serializable.pojos.sos.SOSMetadata;
import org.n52.shared.serializable.pojos.sos.SosService;
import org.n52.shared.serializable.pojos.sos.SosTimeseries;
import org.n52.shared.serializable.pojos.sos.Station;
import org.n52.shared.serializable.pojos.sos.TimeseriesParametersLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MetadataHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataHandler.class);
	
	private ServiceDescriptor serviceDescriptor;

	private SOSAdapter adapter;
	
	private SOSMetadata sosMetadata;
	
	protected MetadataHandler(SOSMetadata metadata) {
	    this.sosMetadata = metadata;
	}
	
	protected void infoLogServiceSummary(SOSMetadata metadata) {
        int timeseriesCount = 0;
        int stationCount = metadata.getStations().size();
        for (Station station : metadata.getStations()) {
            timeseriesCount += station.getObservedTimeseries().size();
        }
        String serviceName = metadata.getConfiguredItemName();
        LOGGER.info("{} cached: {} stations with {} timeseries.", serviceName, stationCount, timeseriesCount);
    }

	public abstract SOSMetadata performMetadataCompletion(String sosUrl, String sosVersion) throws Exception;
	
	public abstract SOSMetadata updateMetadata(SOSMetadata metadata) throws Exception;

	protected SOSMetadata initMetadata(String sosUrl, String sosVersion) {
        serviceDescriptor = ConnectorUtils.getServiceDescriptor(sosMetadata.getServiceUrl(), getSosAdapter());
		String sosTitle = serviceDescriptor.getServiceIdentification().getTitle();
		String omResponseFormat = ConnectorUtils.getResponseFormat(serviceDescriptor, "om");
		String smlVersion = ConnectorUtils.getSMLVersion(serviceDescriptor, sosVersion);
		// TODO check why no omFormat and smlVersion exists
		if (omResponseFormat == null) {
			omResponseFormat = "http://www.opengis.net/om/2.0";
		}
		if (smlVersion == null) {
			smlVersion = "http://www.opengis.net/sensorML/1.0.1";
		}

		setVersionNumbersToMetadata(sosUrl, sosTitle, sosVersion, omResponseFormat, smlVersion);
		return sosMetadata;
	}

    protected SOSAdapter createSosAdapter(SOSMetadata metadata) {
        return SosAdapterFactory.createSosAdapter(metadata);
    }

	protected Collection<SosTimeseries> createObservingTimeseries(String sosUrl) throws OXFException {
		// association: Offering - FOIs
		Map<String, String[]> offeringFoiMap = new HashMap<String, String[]>();

		// association: Offering - Procedures
		Map<String, String[]> offeringProcMap = new HashMap<String, String[]>();

		// association: Offering - Phenomenons
		Map<String, String[]> offeringPhenMap = new HashMap<String, String[]>();

		
		HashSet<String> featureIds = new HashSet<String>();

		Contents contents = serviceDescriptor.getContents();
		for (int i = 0; i < contents.getDataIdentificationCount(); i++) {
			ObservationOffering offering = (ObservationOffering) contents.getDataIdentification(i);

			updateBBox(offering);

			String offeringID = offering.getIdentifier();

			// associate:
			String[] procArray = getProceduresFor(offering);
			offeringProcMap.put(offeringID, procArray);

			// associate:
			String[] phenArray = offering.getObservedProperties();
			offeringPhenMap.put(offeringID, phenArray);

			// associate:
			String[] foiArray = offering.getFeatureOfInterest();
			offeringFoiMap.put(offeringID, foiArray);
			
			// iterate over fois to delete double entries for the request
			for (int j = 0; j < foiArray.length; j++) {
				featureIds.add(foiArray[j]);
			}
		}
		
		TimeseriesParametersLookup lookup = sosMetadata.getTimeseriesParametersLookup();

		// add fois
		for (String featureId : featureIds) {
			lookup.addFeature(new Feature(featureId, sosUrl));
		}

		Collection<SosTimeseries> allObservedTimeseries = new ArrayList<SosTimeseries>();
		// FOI -> Procedure -> Phenomenon
		for (String offeringId : offeringFoiMap.keySet()) {
			for (String procedure : offeringProcMap.get(offeringId)) {
				if (procedure.contains("urn:ogc:generalizationMethod:")) {
					sosMetadata.setCanGeneralize(true);
				} else {
					for (String phenomenon : offeringPhenMap.get(offeringId)) {
						/*
						 * add a timeseries for each procedure-phenomenon-offering
						 * constellation. One of such constellation may be later
						 * related to n features.
						 */
						SosTimeseries timeseries = new SosTimeseries();
						timeseries.setPhenomenon(new Phenomenon(phenomenon, sosUrl));
						timeseries.setProcedure(new Procedure(procedure, sosUrl));
						timeseries.setOffering(new Offering(offeringId, sosUrl));
						timeseries.setSosService(new SosService(sosMetadata.getServiceUrl(), sosMetadata.getVersion()));
						timeseries.getSosService().setLabel(sosMetadata.getTitle());
						allObservedTimeseries.add(timeseries);
					}
					// add procedures
					lookup.addProcedure(new Procedure(procedure, sosUrl));
					for (String phenomenonId : offeringPhenMap.get(offeringId)) {
						lookup.addPhenomenon(new Phenomenon(phenomenonId, sosUrl));
					}
				}
			}
			// add offering
			lookup.addOffering(new Offering(offeringId, sosUrl));
		}
		return allObservedTimeseries;
	}
	
	protected String[] getProceduresFor(ObservationOffering offering) {
        return offering.getProcedures();
    }

    protected void normalizeDefaultCategories(Collection<SosTimeseries> observingTimeseries) {
		for (SosTimeseries timeseries : observingTimeseries) {
			String phenomenon = timeseries.getPhenomenonId();
			String category = phenomenon.substring(phenomenon.lastIndexOf(":") + 1);
			timeseries.setCategory(category);
		}
	}

	protected void updateBBox(ObservationOffering offering) {
		IBoundingBox sosBbox = null;
		sosBbox = ConnectorUtils.createBbox(sosBbox, offering);

		try {
			if (sosBbox != null && !sosBbox.getCRS().startsWith("EPSG")) {
				String tmp = "EPSG:"
						+ sosBbox.getCRS().split(":")[sosBbox.getCRS()
								.split(":").length - 1];
				sosMetadata.setSrs(tmp);
			} else {
				sosMetadata.setSrs(sosBbox.getCRS());
			}
		} catch (Exception e) {
			LOGGER.error("Could not insert spatial metadata", e);
		}
	}
	
	protected SOSAdapter getSosAdapter() {
		return adapter == null ? createSosAdapter(sosMetadata) : adapter;
	}
	
	protected void setSosAdapter(SOSAdapter sosAdapter) {
	    this.adapter = sosAdapter;
	}

	/**
	 * Creates an {@link CRSUtils} according to metadata settings
	 * (e.g. if XY axis order shall be enforced during coordinate
	 * transformation).
	 * 
	 * @param metadata
	 *            the SOS metadata containing SOS instance configuration.
	 */
	protected CRSUtils createReferencingHelper() {
	    if (sosMetadata.isForceXYAxisOrder()) {
            return CRSUtils.createEpsgForcedXYAxisOrder();
        } else {
            return CRSUtils.createEpsgStrictAxisOrder();
        }
	}
	
	protected Collection<String> getFoisByProcedure(String procedure)
			throws OXFException {
		ArrayList<String> fois = new ArrayList<String>();
		String url = sosMetadata.getServiceUrl();
		try {
			ParameterContainer container = new ParameterContainer();
			container.addParameterShell(GET_FOI_SERVICE_PARAMETER, "SOS");
			container.addParameterShell(GET_FOI_VERSION_PARAMETER, sosMetadata.getVersion());
			container.addParameterShell("procedure", procedure);
			Operation operation = new Operation(GET_FEATURE_OF_INTEREST, url, url);
			OperationResult result = getSosAdapter().doOperation(operation, container);
			XmlObject foiResponse = XmlObject.Factory.parse(result
					.getIncomingResultAsStream());
			if (foiResponse instanceof GetFeatureOfInterestResponseDocument) {
				GetFeatureOfInterestResponseDocument foiResDoc = (GetFeatureOfInterestResponseDocument) foiResponse;
				for (FeaturePropertyType featurePropertyType : foiResDoc
						.getGetFeatureOfInterestResponse()
						.getFeatureMemberArray()) {
					SFSamplingFeatureDocument samplingFeature = SFSamplingFeatureDocument.Factory
							.parse(featurePropertyType.xmlText());
					SFSamplingFeatureType sfSamplingFeature = samplingFeature
							.getSFSamplingFeature();
					fois.add(sfSamplingFeature.getIdentifier()
							.getStringValue());
				}
			} else {
				throw new OXFException("No valid GetFeatureOfInterestResponse");
			}
		} catch (Exception e) {
			LOGGER.error("Error while send GetFeatureOfInterest: "
					+ e.getCause());
			throw new OXFException(e);
		}
		return fois;
	}
	
	public ServiceDescriptor getServiceDescriptor() {
		return serviceDescriptor;
	}

}
