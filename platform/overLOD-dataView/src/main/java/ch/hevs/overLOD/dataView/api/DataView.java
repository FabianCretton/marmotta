/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.hevs.overLOD.dataView.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import ch.hevs.overLOD.dataView.exceptions.DataViewException;

/**
 * DataView API
 *
 * See DataViewImpl for the current implementation
 * See DataViewWebService comment for a general introduction
 * 
 * If errors occure, exception are sent and intercepted by the calling web service
 * 
 * @author Fabian Cretton, HES-SO OverLOD surfer project
 * http://www.hevs.ch/fr/rad-instituts/institut-informatique-de-gestion/projets/overlod-surfer-6349
 */
public interface DataView {

	/**
	 * Get a string list of all existing dataViews, i.e. dataViews files on disk
	 * @return the list of all existing dataViews
	 * @throws DataViewException if any error occurs
	 */
    public ArrayList<String> getDataViewsList() throws DataViewException ;
	
    /**
     * Method used to add (update=false) or update (update=true) a DataView
     * An exception will be thrown if:
     * - adding with a viewName (file) that already exists
     * - updating with a viewName (file) that don't exist
     */
    
    /**
     * Method used to add or update a DataView
     * @param viewName the name of the dataView
     * @param query the SPARQL query for that dataView
     * @param update add (update=false) or update (update=true)
     * @return a validation message
     * @throws DataViewException an error
     */
    public String saveDataView(String viewName, String query, boolean update)  throws DataViewException ;

    /**
     * Delete a DataView
     * @param viewName the name of the dataView
     * @return a validation message
     * @throws DataViewException an error
     */
    public String deleteDataView(String viewName)  throws DataViewException ;
    
    /**
     * Two purposes: test if google analytics is enabled, if so, get the google analytics ID  
     * @return return the TrackingID from parameter "dataView.GoogleAnalyticsTrackingID" if "dataView.GoogleAnalytics" is true, otherwise null 
     */
    public String getGoogleAnalyticsTrackingID() ;
    
    /**
     * Read the SPARQL query corresponding to a viewName and return the string
     * @param viewName the name of the dataView
     * @return a String containing the SPARQL query
     * @throws IOException an error
     */
    public String readDataViewQuery(String viewName) throws IOException ;
}
