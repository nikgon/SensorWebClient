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
package org.n52.client.view.gui.elements.layouts;

import java.util.ArrayList;

import org.n52.client.control.I18N;
import org.n52.client.eventBus.EventBus;
import org.n52.client.eventBus.events.ses.UnsubscribeEvent;
import org.n52.client.model.communication.requestManager.SesRequestManager;
import org.n52.client.model.data.representations.RuleRecord;
import org.n52.client.view.gui.elements.interfaces.Layout;
import org.n52.shared.serializable.pojos.BasicRuleDTO;
import org.n52.shared.serializable.pojos.ComplexRuleDTO;
import org.n52.shared.serializable.pojos.RuleDS;

import com.google.gwt.user.client.Cookies;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * The Class UserSubscriptionsLayout.
 * 
 * With this view the user can see all his subscriptions in a table.
 * Each subscription has informatio about the medium and the format
 * of the subscruption
 * 
 * @author <a href="mailto:osmanov@52north.org">Artur Osmanov</a>
 */
public class UserSubscriptionsLayout extends Layout {

    /** The own rules grid. */
    private ListGrid subscriptionsGrid;
    
    private RuleDS dataSource;
    
    private boolean first = true;

    /**
     * Instantiates a new user rule layout.
     */
    public UserSubscriptionsLayout() {
        super(I18N.sesClient.subscriptions());
        
        // init DataSource
        this.dataSource = new RuleDS();
        
        init();
    }

    /**
     * Inits the layout.
     */
    private void init() {

        this.subscriptionsGrid = new ListGrid() {
            @Override
            protected Canvas createRecordComponent(final ListGridRecord record, Integer colNum) {
                if (record != null) {

                    String fieldName = this.getFieldName(colNum);

                    if (fieldName.equals("subscribe")) {
                        // subscribe button
                        IButton subscribeButton = new IButton(I18N.sesClient.unsubscribe());
                        subscribeButton.setPrompt(I18N.sesClient.unsubscribeThisRule());
                        subscribeButton.setShowDown(false);
                        subscribeButton.setShowRollOver(false);
                        subscribeButton.setHeight(16);
                        subscribeButton.setWidth(140);
                        subscribeButton.setLayoutAlign(Alignment.CENTER);
                        subscribeButton.addClickHandler(new ClickHandler() {
                            public void onClick(ClickEvent event) {
                                String userID = Cookies.getCookie(SesRequestManager.COOKIE_USER_ID);
                                EventBus.getMainEventBus().fireEvent(new UnsubscribeEvent(record.getAttribute("name"), userID, record.getAttribute("medium"),record.getAttribute("format")));
                            }
                        });
                        return subscribeButton;
                    }
                    return null;
                }
                return null;
            }
        };

        this.subscriptionsGrid.setShowRecordComponents(true);
        this.subscriptionsGrid.setShowRecordComponentsByCell(true);
        this.subscriptionsGrid.setWidth100();
        this.subscriptionsGrid.setHeight100();
        this.subscriptionsGrid.setCanGroupBy(false);
        this.subscriptionsGrid.setDataSource(this.dataSource);
        this.subscriptionsGrid.setShowFilterEditor(true);
        this.subscriptionsGrid.setFilterOnKeypress(true);
        this.subscriptionsGrid.setAutoFetchData(true);
        this.subscriptionsGrid.setShowRollOver(false);
        this.subscriptionsGrid.sort(1, SortDirection.ASCENDING);

        // fields of the table
        ListGridField typeField = new ListGridField("type", I18N.sesClient.type());
        typeField.setWidth(60);
        typeField.setAlign(Alignment.CENTER);

        ListGridField nameField = new ListGridField("name", I18N.sesClient.name());
        nameField.setAlign(Alignment.CENTER);

        ListGridField descriptionField = new ListGridField("description", I18N.sesClient.description());
        descriptionField.setAlign(Alignment.CENTER);

        ListGridField mediumField = new ListGridField("medium", I18N.sesClient.medium());
        mediumField.setWidth(90);
        mediumField.setAlign(Alignment.CENTER);

        ListGridField formatField = new ListGridField("format", "Format");
        formatField.setWidth(90);
        formatField.setAlign(Alignment.CENTER);

        ListGridField subscribeField = new ListGridField("subscribe", I18N.sesClient.unsubscribe());
        subscribeField.setWidth(150);
        subscribeField.setAlign(Alignment.CENTER);
        subscribeField.setCanFilter(false);

        // set Fields
        this.subscriptionsGrid.setFields(typeField, nameField, descriptionField, mediumField, formatField, subscribeField);
        this.subscriptionsGrid.setCanResizeFields(false);

        this.form.setFields(this.headerItem);

        addMember(this.form);
        addMember(this.subscriptionsGrid);
    }

    /**
     * Fills the grid
     * 
     * @param basicRules 
     * @param complexRules 
     */
    public void setData(ArrayList<BasicRuleDTO> basicRules, ArrayList<ComplexRuleDTO> complexRules) {
        RuleRecord rule;
        BasicRuleDTO ruleDTO;
        ComplexRuleDTO complexDTO;
        
        if (!this.first) {
            this.subscriptionsGrid.selectAllRecords();
            this.subscriptionsGrid.removeSelectedData(); 
        }

        for (int i = 0; i < basicRules.size(); i++) {
            ruleDTO = basicRules.get(i);
            rule = new RuleRecord(I18N.sesClient.basic(), "", "", ruleDTO.getName(), ruleDTO.getDescription(), ruleDTO.getMedium(), ruleDTO.getFormat(), ruleDTO.isRelease(), ruleDTO.isSubscribed());
            
            this.subscriptionsGrid.addData(rule);
        }

        for (int i = 0; i < complexRules.size(); i++) {
            complexDTO = complexRules.get(i);
            rule = new RuleRecord(I18N.sesClient.complex(), "", "", complexDTO.getName(), complexDTO.getDescription(), complexDTO.getMedium(), complexDTO.getFormat(), complexDTO.isRelease(), complexDTO.isSubscribed());

            this.subscriptionsGrid.addData(rule);
        }
        
        this.first = false;
        this.subscriptionsGrid.fetchData();
    }
}