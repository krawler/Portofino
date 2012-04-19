/*
 * Copyright (C) 2005-2012 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.manydesigns.portofino.pageactions.m2m.configuration;

import com.manydesigns.elements.annotations.Multiline;
import com.manydesigns.elements.annotations.Required;
import com.manydesigns.portofino.application.Application;
import com.manydesigns.portofino.application.QueryUtils;
import com.manydesigns.portofino.dispatcher.PageActionConfiguration;
import com.manydesigns.portofino.model.database.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
@XmlRootElement(name = "configuration")
@XmlAccessorType(XmlAccessType.NONE)
public class ManyToManyConfiguration implements PageActionConfiguration {
    public static final String copyright =
            "Copyright (c) 2005-2012, ManyDesigns srl";

    //**************************************************************************
    // Fields
    //**************************************************************************

    protected String oneExpression;
    protected String onePropertyName;
    protected SelectionProviderReference oneSelectionProvider;

    protected SelectionProviderReference manySelectionProvider;

    protected String database;
    protected String query;
    protected String viewType;

    //**************************************************************************
    // Fields for wire-up
    //**************************************************************************

    protected String actualOnePropertyName;
    protected Database actualOneDatabase;
    protected Database actualManyDatabase;
    protected Database actualRelationDatabase;
    protected Table actualRelationTable;
    protected Table actualManyTable;
    protected ViewType actualViewType;

    //**************************************************************************
    // Logging
    //**************************************************************************

    public static final Logger logger =
            LoggerFactory.getLogger(ManyToManyConfiguration.class);

    //**************************************************************************
    // Constructors
    //**************************************************************************

    public ManyToManyConfiguration() {}

    //**************************************************************************
    // Configuration implementation
    //**************************************************************************

    public void init(Application application) {
        if(viewType == null) {
            viewType = ViewType.CHECKBOXES.name();
        }
        actualViewType = ViewType.valueOf(viewType);

        if(database != null && query != null) {
            actualRelationDatabase = DatabaseLogic.findDatabaseByName(application.getModel(), database);
            if(actualRelationDatabase != null) {
                actualRelationTable = QueryUtils.getTableFromQueryString(actualRelationDatabase, query);

                if(actualRelationTable != null) {
                    if(manySelectionProvider != null) {
                        manySelectionProvider.init(actualRelationTable);
                        ModelSelectionProvider actualSelectionProvider = manySelectionProvider.getActualSelectionProvider();
                        String manyDatabaseName = actualSelectionProvider.getToDatabase();
                        actualManyDatabase =
                            DatabaseLogic.findDatabaseByName(application.getModel(), manyDatabaseName);
                        actualManyTable = actualSelectionProvider.getToTable();
                        if(actualManyTable == null && actualSelectionProvider instanceof DatabaseSelectionProvider) {
                            logger.debug("Trying to determine the many table from the selection provider query");
                            String hql = ((DatabaseSelectionProvider) actualSelectionProvider).getHql();
                            if(hql != null) {
                                actualManyTable = QueryUtils.getTableFromQueryString(actualManyDatabase, hql);
                            }
                        }
                        if(actualManyTable == null) {
                            logger.error("Invalid selection provider: only foreign keys or HQL selection providers that select a single entity are supported");
                        }
                    } else {
                        logger.error("Many-side selection provider is required");
                    }

                    if(oneSelectionProvider != null) {
                        oneSelectionProvider.init(actualRelationTable);
                        String oneDatabaseName = oneSelectionProvider.getActualSelectionProvider().getToDatabase();
                        actualOneDatabase =
                            DatabaseLogic.findDatabaseByName(application.getModel(), oneDatabaseName);
                    }
                } else {
                    logger.error("Table not found");
                }
            } else {
                logger.error("Relation database " + database + " not found");
            }

            if(StringUtils.isBlank(oneExpression)) {
                //TODO chiave multipla
                try {
                    actualOnePropertyName =
                            getOneSelectionProvider().getActualSelectionProvider().getReferences().get(0)
                                    .getActualFromColumn().getActualPropertyName();
                } catch (Throwable t) {
                    logger.error("Couldn't determine one property name", t);
                }
            } else {
                actualOnePropertyName = onePropertyName;
            }
        }
    }

    //**************************************************************************
    // Getters/setters
    //**************************************************************************

    @Required
    @XmlAttribute(required = true)
    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    @Required
    @Multiline
    @XmlAttribute(required = true)
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Required
    @XmlAttribute(required = true)
    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    @XmlAttribute(required = false)
    public String getOneExpression() {
        return oneExpression;
    }

    public void setOneExpression(String oneExpression) {
        this.oneExpression = oneExpression;
    }

    @XmlAttribute(required = false)
    public String getOnePropertyName() {
        return onePropertyName;
    }

    public void setOnePropertyName(String onePropertyName) {
        this.onePropertyName = onePropertyName;
    }

    @XmlElement(name = "one", required = false)
    public SelectionProviderReference getOneSelectionProvider() {
        return oneSelectionProvider;
    }

    public void setOneSelectionProvider(SelectionProviderReference oneSelectionProvider) {
        this.oneSelectionProvider = oneSelectionProvider;
    }

    @Required
    @XmlElement(name = "many", required = true)
    public SelectionProviderReference getManySelectionProvider() {
        return manySelectionProvider;
    }

    public void setManySelectionProvider(SelectionProviderReference manySelectionProvider) {
        this.manySelectionProvider = manySelectionProvider;
    }

    public Database getActualOneDatabase() {
        return actualOneDatabase;
    }

    public Database getActualManyDatabase() {
        return actualManyDatabase;
    }

    public ViewType getActualViewType() {
        return actualViewType;
    }

    public Database getActualRelationDatabase() {
        return actualRelationDatabase;
    }

    public Table getActualRelationTable() {
        return actualRelationTable;
    }

    public Table getActualManyTable() {
        return actualManyTable;
    }

    public String getActualOnePropertyName() {
        return actualOnePropertyName;
    }
}