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

package org.n52.api.v1.srv;

import static org.n52.server.mgmt.ConfigurationContext.getSOSMetadatas;
import static org.n52.shared.requests.query.QueryParameters.createEmptyFilterQuery;

import java.util.ArrayList;
import java.util.Collection;

import org.n52.api.v1.io.CategoryConverter;
import org.n52.shared.serializable.pojos.sos.Feature;
import org.n52.shared.serializable.pojos.sos.Offering;
import org.n52.shared.serializable.pojos.sos.Phenomenon;
import org.n52.shared.serializable.pojos.sos.Procedure;
import org.n52.shared.serializable.pojos.sos.SOSMetadata;
import org.n52.shared.serializable.pojos.sos.SosTimeseries;
import org.n52.shared.serializable.pojos.sos.Station;
import org.n52.shared.serializable.pojos.sos.TimeseriesParametersLookup;
import org.n52.web.v1.srv.SearchService;
import org.n52.web.v1.srv.search.CategorySearchResult;
import org.n52.web.v1.srv.search.FeatureSearchResult;
import org.n52.web.v1.srv.search.OfferingSearchResult;
import org.n52.web.v1.srv.search.PhenomenonSearchResult;
import org.n52.web.v1.srv.search.ProcedureSearchResult;
import org.n52.web.v1.srv.search.SearchResult;
import org.n52.web.v1.srv.search.ServiceSearchResult;
import org.n52.web.v1.srv.search.StationSearchResult;
import org.n52.web.v1.srv.search.TimeseriesSearchResult;

public class SearchAdapter implements SearchService {

    @Override
    public Collection<SearchResult> searchResources(String search, String locale) {
        // language specific search is not supported by the aggregation component
        return searchResources(search);
    }

    private Collection<SearchResult> searchResources(String search) {
        
        // TODO extend search logic to support composed search strings

        ArrayList<SearchResult> results = new ArrayList<SearchResult>();
        results.addAll(requestServices(search));
        results.addAll(requestStations(search));
        results.addAll(requestTimeseries(search));
        results.addAll(requestTSParameters(search));
        return results;
    }

    private Collection<SearchResult> requestTimeseries(String search) {
        Collection<SearchResult> results = new ArrayList<SearchResult>();
        for (SOSMetadata metadata : getSOSMetadatas()) {
            SosTimeseries[] timeseries = metadata.getMatchingTimeseries(createEmptyFilterQuery());
            for (SosTimeseries ts : timeseries) {
                if (containsSearchString(ts.getFeature().getLabel(), search)
                        || containsSearchString(ts.getPhenomenon().getLabel(), search)
                        || containsSearchString(ts.getProcedure().getLabel(), search)
                        || containsSearchString(ts.getOffering().getLabel(), search)
                        || containsSearchString(ts.getCategory(), search)) {
                    results.add(new TimeseriesSearchResult(ts.getTimeseriesId(), ts.getLabel()));
                }
            }
        }
        return results;
    }

    private Collection<SearchResult> requestStations(String search) {
        Collection<SearchResult> results = new ArrayList<SearchResult>();
        for (SOSMetadata metadata : getSOSMetadatas()) {
            for (Station station : metadata.getStations()) {
                if (containsSearchString(station.getLabel(), search)) {
                    results.add(new StationSearchResult(station.getGlobalId(), station.getLabel()));
                }
            }
        }
        return results;
    }

    private Collection<SearchResult> requestTSParameters(String search) {
        Collection<SearchResult> results = new ArrayList<SearchResult>();
        for (SOSMetadata metadata : getSOSMetadatas()) {
            TimeseriesParametersLookup lookup = metadata.getTimeseriesParametersLookup();
            // offerings
            for (Offering offering : lookup.getOfferings()) {
                if (containsSearchString(offering.getLabel(), search)) {
                    results.add(new OfferingSearchResult(offering.getGlobalId(), offering.getLabel()));
                }
            }
            // features
            for (Feature feature : lookup.getFeatures()) {
                if (containsSearchString(feature.getLabel(), search)) {
                    results.add(new FeatureSearchResult(feature.getGlobalId(), feature.getLabel()));
                }
            }
            // procedures
            for (Procedure procedure : lookup.getProcedures()) {
                if (containsSearchString(procedure.getLabel(), search)) {
                    results.add(new ProcedureSearchResult(procedure.getGlobalId(), procedure.getLabel()));
                }
            }
            // phenomena
            for (Phenomenon phenomenon : lookup.getPhenomenons()) {
                if (containsSearchString(phenomenon.getLabel(), search)) {
                    results.add(new PhenomenonSearchResult(phenomenon.getGlobalId(), phenomenon.getLabel()));
                }
            }
            // categories
            SosTimeseries[] timeseries = metadata.getMatchingTimeseries(createEmptyFilterQuery());
            for (SosTimeseries sosTimeseries : timeseries) {
                if (containsSearchString(sosTimeseries.getCategory(), search)) {
                    CategoryConverter converter = new CategoryConverter(metadata);
                    String generateCategoryId = converter.generateId(sosTimeseries.getCategory());
                    results.add(new CategorySearchResult(generateCategoryId, sosTimeseries.getCategory()));
                }
            }
        }
        return results;
    }

    private Collection<SearchResult> requestServices(String search) {
        Collection<SearchResult> results = new ArrayList<SearchResult>();
        for (SOSMetadata metadata : getSOSMetadatas()) {
            if (containsSearchString(metadata.getTitle(), search)) {
                results.add(new ServiceSearchResult(metadata.getGlobalId(), metadata.getTitle()));
            }
        }
        return results;
    }

    /**
     * @param label
     *        the label to check.
     * @param searchToken
     *        the input search token.
     * @return <code>true</code> if the <code>label</code> contains the <code>searchToken</code> and ignores
     *         the case.
     */
    private boolean containsSearchString(String label, String searchToken) {
        return label.toLowerCase().contains(searchToken.toLowerCase());
    }

}
