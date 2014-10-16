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
     * 
     * @return a string confirming the operation succeeded 
     * @throws ExtDataSourcesException 
     */
    public String addEDSParams(String EDSType, String contentType, String url, String context, String timeStamp) throws ExtDataSourcesException;

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
}
