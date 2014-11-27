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
package ch.hevs.overLOD.dataView.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.marmotta.client.ClientConfiguration;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.apache.marmotta.platform.core.events.ConfigurationChangedEvent;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import ch.hevs.overLOD.dataView.api.DataView;
import ch.hevs.overLOD.dataView.exceptions.DataViewException;

/**
 * Default Implementation of DataView
 * 
 * For more information about the overriden methods, see "DataView"
 * See DataViewWebService comment for a general introduction to the EDS module
 *
 * @author Fabian Cretton, HES-SO OverLOD surfer project
 * http://www.hevs.ch/fr/rad-instituts/institut-informatique-de-gestion/projets/overlod-surfer-6349
 */
@ApplicationScoped
public class DataViewImpl implements DataView {
    String fileExtension = ".sparql" ; // extension of the DataViews files (sparql queries)

    @Inject
    private Logger log;

    @Inject
    private ConfigurationService configurationService;

    String dataViewFolder ;
    
    // Will be read from the module configuration
    boolean googleAnalyticsEnabled = false ; 
    String googleAnalyticsTrackingID = "" ;

    @PostConstruct
    public void initialize() 
    {
    	dataViewFolder = configurationService.getHome() + File.separator + "dataViews" ;

    	log.debug("DataViewImpl initialize() - make sure the dataViews folder does exist: " + dataViewFolder) ;

        File folder = new File(dataViewFolder);
        if (!folder.exists())
        {
				log.debug("dataViews folder don't exist in Marmotta-home, it will be created now.");
				folder.mkdir() ;
        }
        
        // finally add a separator for further operations
        dataViewFolder += File.separator ;
        
        readConfiguration() ;
    }
    
    public void readConfiguration()
    {
    	googleAnalyticsEnabled = configurationService.getBooleanConfiguration("dataView.GoogleAnalytics", false) ;
    	googleAnalyticsTrackingID = configurationService.getStringConfiguration("dataView.GoogleAnalyticsTrackingID", null) ;
    	log.debug("DataView configuration 'dataView.GoogleAnalytics': {}", googleAnalyticsEnabled) ;
    	log.debug("DataView configuration 'dataView.GoogleAnalyticsTrackingID': {}", googleAnalyticsTrackingID) ;
    }

    /*
     * (non-Javadoc)
     * @see ch.hevs.overLOD.dataView.api.DataView#getGoogleAnalyticsTrackingID()
     */
    @Override
    public String getGoogleAnalyticsTrackingID()
    {
    	if (googleAnalyticsEnabled)
    		return googleAnalyticsTrackingID ;
    	
    	return null ;
    }
    
    /**
     * Detect a change in the dataView configuration, and if so reload the configuration values
     * @param event
     */
    public void configurationEventHandler(@Observes ConfigurationChangedEvent event) 
    {
   		if (event.containsChangedKeyWithPrefix("dataView.")) {
   	    	log.debug("Data View: Reloading configuration - change detected");
   			readConfiguration() ;
    	}
   	}
    
    /*
     * (non-Javadoc)
     * @see ch.hevs.overLOD.dataView.api.DataView#getDataViewsList()
     */
    @Override
    public ArrayList<String> getDataViewsList() throws DataViewException
    {
    	ArrayList<String> dataViewsList = new ArrayList<String>() ;

        String fileName;
        
        File folder = new File(dataViewFolder);
        
        File[] listOfFiles = folder.listFiles(); 
       
        int fileExtensionLength = fileExtension.length() ;
        
        for (int i = 0; i < listOfFiles.length; i++) 
        {
			 if (listOfFiles[i].isFile()) 
			 {
			 fileName = listOfFiles[i].getName();
			 if (fileName.toLowerCase().endsWith(fileExtension))
			   	 dataViewsList.add(fileName.substring(0, fileName.length()-fileExtensionLength)) ;
			 }
        }

    	return dataViewsList ;
    }

    /*
     * (non-Javadoc)
     * @see ch.hevs.overLOD.dataView.api.DataView#saveDataView(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public synchronized String saveDataView(String viewName, String query, boolean update) throws DataViewException {
        log.debug("saveDataView {}", viewName);
        
    	String dataViewFileName = viewName + fileExtension ;
    	
        File folder = new File(dataViewFolder);
        File dataViewfile = new File(dataViewFolder+dataViewFileName);
        
        if (!update) // add a new DataView
        {
        	if (dataViewfile.exists())
        		throw new DataViewException("Impossible to create the new DataView '" + viewName + "' as that DataView already exists!") ;
        		
        	try {
        		dataViewfile.getCanonicalPath();// to check if the filename is valid: if not, an error will be thrown
				dataViewfile.createNewFile();
			} catch (IOException e) {
        		throw new DataViewException("Impossible to create the new DataView file for '" + viewName + "': " + e.getMessage()) ;
			}        		
        }
        else // update an existing dataView
        	if (!dataViewfile.exists())
        		throw new DataViewException("Impossible to update the DataView '" + viewName + "': file not found!") ;
        		
        // Update the file content with the query
		FileOutputStream fop = null;
		try {
			fop = new FileOutputStream(dataViewfile);
			// get the content in bytes
			byte[] contentInBytes = query.getBytes();
	 
			fop.write(contentInBytes);
			fop.flush();
			fop.close();
		} catch (Exception e) {
    		if (fop != null)
				try {
					fop.close();
				} catch (IOException e1) {
					log.error("Error writing the dataView content for '{}': {}", viewName, e1.getMessage());
				}
    		
    		throw new DataViewException("Impossible to save the query to the DataView file '" + viewName + "': " + e.getMessage()) ;
		}
        	
        return "DataView '" + viewName + "' saved successfully!";
    }

    /*
     * (non-Javadoc)
     * @see ch.hevs.overLOD.dataView.api.DataView#readDataViewQuery(java.lang.String)
     */
	@Override
    public String readDataViewQuery(String viewName) throws IOException
    {
    String query = null ;
    String queryFile = configurationService.getHome() + File.separator + "dataViews" + File.separator + viewName + ".sparql" ;

    // Read the .sparql file
    FileInputStream inputStream = null;
    
		try {
			inputStream = new FileInputStream(queryFile);
			query = IOUtils.toString(inputStream);
		} catch (IOException e) {
			log.error("DataView - accessing " + queryFile + " exception:" + e.getMessage());
			throw(e) ;
		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				log.error("DataView - closing " + queryFile + " exception:" + e.getMessage());
			}
		}
		
		return query ;
    }
	
	/*
	 * (non-Javadoc)
	 * @see ch.hevs.overLOD.dataView.api.DataView#deleteDataView(java.lang.String)
	 */
    @Override
    public synchronized String deleteDataView(String viewName)  throws DataViewException {
        log.debug("deleteDataView {}", viewName);
        
    	String dataViewFileName = viewName + fileExtension ;
    	
        File folder = new File(dataViewFolder);
        File dataViewfile = new File(dataViewFolder+dataViewFileName);
        
        if (!dataViewfile.exists())
    		throw new DataViewException("The data view '" + viewName + "' don't exist!") ;

        if (!dataViewfile.delete())
    		throw new DataViewException("Impossible to delete the file associated to data view '" + viewName + "'") ;
        	
        return "DataView '" + viewName + "' deleted successfully!";
    }
}
