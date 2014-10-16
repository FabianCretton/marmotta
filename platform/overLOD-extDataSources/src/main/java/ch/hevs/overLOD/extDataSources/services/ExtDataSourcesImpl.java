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
package ch.hevs.overLOD.extDataSources.services;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import ch.hevs.overLOD.extDataSources.EDSParams.EDSParams;
import ch.hevs.overLOD.extDataSources.EDSParams.EDSParamsList;
import ch.hevs.overLOD.extDataSources.api.ExtDataSources;
import ch.hevs.overLOD.extDataSources.exceptions.ExtDataSourcesException;

/**
 * Default Implementation of {@link ExtDataSources}
 * 
 * Handles the list of External Data Sources parameters (EDSParams)
 * that is read and written to a .json file in: configurationService.getHome() + "/EDSParamsList.json"
 * 
 * @author Fabian Cretton, HES-SO OverLOD surfer project
 */
@ApplicationScoped
public class ExtDataSourcesImpl implements ExtDataSources {

    @Inject
    private Logger log;

    @Inject
    private ConfigurationService configurationService;

    private EDSParamsList cacheEDSParamsList = null ;
    
    @Override
    public TreeMap<String,EDSParams> getEDSParamsList() throws ExtDataSourcesException
    {
		ensureEDSParamsListIsLoaded() ;
        
        return cacheEDSParamsList.getList() ;
    }
    
    @Override
    public String addEDSParams(String EDSType, String contentType, String url, String context) throws ExtDataSourcesException {
        log.debug("saving EDSParams EDSType:{}, contentType:{} url:{} context:{}...", EDSType, contentType, url, context);
        
        // an EDS is read only, once created it will not change, if params change (the url, the mimetype), than a new EDS has to be created
        // so ensure here that an EDS is not saved with an existing context (the identifier)
        if (cacheEDSParamsList.exists(context))
        	throw new ExtDataSourcesException("A new EDS can't be added with a context already used for another one") ;
        	
        EDSParams aEDSParams = new EDSParams(EDSType, contentType, url, context) ;
        
        ensureEDSParamsListIsLoaded() ;
        
        cacheEDSParamsList.addEDSParams(aEDSParams);
        
       	serializeObjectToJSON(cacheEDSParamsList, configurationService.getHome() + "/EDSParamsList.json") ;
        log.debug("saving EDSParams EDSType:{}, url:{} context:{}...done!", EDSType, url, context);
        
        return "Parameters saved for External Data Source: " + context ;
    }

    
    @Override
    public EDSParams getEDSParams(String context)
    {
    	return cacheEDSParamsList.get(context) ;
    }

    @Override
    public String deleteEDSParams(String context) throws ExtDataSourcesException {
        log.debug("delete EDSParams identified by context:{}", context);
        
        ensureEDSParamsListIsLoaded() ;
        
        if (!cacheEDSParamsList.deleteEDSParams(context)) // context not found in the list
        	throw new ExtDataSourcesException("Context '"+ context + "' not found in the list of EDS") ;
        
       	serializeObjectToJSON(cacheEDSParamsList, configurationService.getHome() + "/EDSParamsList.json") ;
        log.debug("saving EDSParams list after a delete...done!");
        
        return "EDS deleted from External Data Sources list: " + context ;
    }

    /**
     * If the file don't exist yet, then create it.
     * Could happen on the first run of the module
     * @throws ExtDataSourcesException 
     * @throws Exception
     */
    private void ensureEDSParamsListIsLoaded() throws ExtDataSourcesException
    {
    	if (cacheEDSParamsList == null)
    	{
    		cacheEDSParamsList = (EDSParamsList) JSONSerializationToObject(configurationService.getHome() + "/EDSParamsList.json", new EDSParamsList().getClass()) ;
    		
        	if (cacheEDSParamsList == null)
        	{
    	        log.info("Creating EDS file as it doesn't exist yet:" + configurationService.getHome() + "/EDSParamsList.json");
        		cacheEDSParamsList = new EDSParamsList()  ;
        		
               	serializeObjectToJSON(cacheEDSParamsList, configurationService.getHome() + "/EDSParamsList.json") ;
        	}
    	}
    }
    
    /**
     * Read a JSON serialization from a file
     * @param filePath
     * @param objectForClass an instance of the object that is expected
     * @return the object, otherwise null if io exception
     */
    private Object JSONSerializationToObject(String filePath, Class objectClass) // , Object objectForClass)
    {
		Object object = null ;
		ObjectMapper mapper = new ObjectMapper();
		
		try {
	        log.debug("JSONSerialization2Object - loading: "+ filePath);
			
			//object = mapper.readValue(new File(filePath), objectForClass.getClass());
	        object = mapper.readValue(new File(filePath), objectClass);
			 
	        log.debug("JSONSerialization2Object - done!");
			//System.out.println(mapper.writeValueAsString(object));
		} catch (Exception e1) {
	        log.error("JSONSerialization2Object :" +e1.getMessage());
	        object = null ;
		}

		return object ;
    }
    
    /**
     * Use Jackson to export an object to a file
     * @param object
     * @param filePath
     * @throws ExtDataSourcesException
     */
	public void serializeObjectToJSON(Object object, String filePath) throws ExtDataSourcesException
	{
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			// display to console
	        log.debug("serializeObject2JSON - saving...");
			// convert object to json string, and save to a file
			mapper.writeValue(new File(filePath), object);
	        log.debug("Service serializeObject2JSON - done!");
		} catch (Exception e) {
	        log.error("serializeObject2JSON :" +e.getMessage());
			throw new ExtDataSourcesException(e.getMessage());
		}
	}
}
