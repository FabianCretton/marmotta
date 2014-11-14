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
package ch.hevs.overLOD.extDataSources.api;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import ch.hevs.overLOD.extDataSources.EDSParams.EDSParams;
import ch.hevs.overLOD.extDataSources.exceptions.ExtDataSourcesException;

/**
 * External Data Source (EDS) API
 * 
 * @author Fabian Cretton, HES-SO OverLOD surfer project
 * 
 */
public interface ExtDataSources {

    public TreeMap<String,EDSParams> getEDSParamsList() throws ExtDataSourcesException ;

    /**
     * Save one External Data Source (EDS) parameters
     * 
     * @param EDSType the type of EDS
     * @param contentType the mime type of the data
     * @param url the url of the EDS
     * @param context the context (Named Graph) where this EDS is saved locally
     * @param timeStamp the EDS current timestamp
	 * @param filterFileName name of file (including file extension) that will
	 *        allow to import only part of the data using a SPARQL CONSTRUCT
	 *        query. This file must be available in the folder
	 *        %marmotta-home%/EDS/EDSFilters/. null to import the all data)
	 * @param validationFileName name of file (including file extension) that
	 *        will allow to check the data validity using SPIN constraints. This
	 *        file must be available in the folder
	 *        %marmotta-home%/EDS/SPIN/Constraints/. null to import data without
	 *        validation)
	 *        
     * @return a string confirming the operation succeeded 
     * @throws ExtDataSourcesException 
     */
    public String addEDSParams(String EDSType, String contentType, String url, String context, String timeStamp, String filterFileName, String validationFileName) throws ExtDataSourcesException;

    /**
     * Get the EDSParams identified by the context
     * @param context the identifier of the EDS
     * @return an EDSParams object or null if not found
     */
    public EDSParams getEDSParams(String context) ;
    
    /**
     * Delete and EDS from the list
     * @param context the context (Named Graph) where this EDS is saved locally, which is its identifier
     * @return a string confirming the operation succeeded
     * @throws ExtDataSourcesException
     */
    public String deleteEDSParams(String context) throws ExtDataSourcesException ;
    
    /**
     * Set a new value for the timeStamp of an EDS
     * @param context  the context (Named Graph) where this EDS is saved locally, which is its identifier
     * @param timeStamp a string representing the timeStamp
     * @return true/false whether the value has been saved or not
     */
    public boolean setEDSParamsTimeStamp(String context, String timeStamp) throws ExtDataSourcesException ;
    
	/**
	 * Import data that can be handled by LDClient So far "linked data" and RDF
	 * files were tested
	 * 
	 * LDClient retrieves data in a temporary store, than data is queried from
	 * the result and imported in Marmotta using the ImportClient
	 * 
	 * @param marmottaURL marmottaServer URL, needed to call ImportClient
	 * @param EDSType the type of EDS, as "RDFFile" or "LinkedData" This
	 *        parameter is not really used as the LDClient.retrieveResource()
	 *        handles automatically which provider/endpoint to call depending on
	 *        the URL
	 * @param url the url of the Linked Data resource to upload, for instance
	 *        "http://dbpedia.org/resource/Martigny"
	 * @param context the context (named graph) in which the data will be saved
	 * @param filterFileName name of file (including file extension) that will
	 *        allow to import only part of the data using a SPARQL CONSTRUCT
	 *        query. This file must be available in the folder
	 *        %marmotta-home%/EDS/EDSFilters/. null to import the all data)
	 * @param validationFileName name of file (including file extension) that
	 *        will allow to check the data validity using SPIN constraints. This
	 *        file must be available in the folder
	 *        %marmotta-home%/EDS/SPIN/Constraints/. null to import data without
	 *        validation)
	 * @throws ExtDataSourcesException with an error message
	 * @return a string with a validation message
	 */
	public String importWithLDClient(String marmottaURL, String headerAuth, String LDClientType, String url, String context, String filterFileName, String validationFileName)  throws ExtDataSourcesException ;
 
    public ArrayList<String> getDataFiltersList() throws ExtDataSourcesException ;
    public ArrayList<String> getDataValidatorsList() throws ExtDataSourcesException ;

}
