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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.marmotta.client.ClientConfiguration;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import ch.hevs.overLOD.dataView.api.DataView;
import ch.hevs.overLOD.dataView.exceptions.DataViewException;

/**
 * Default Implementation of {@link DataView}
 */
@ApplicationScoped
public class DataViewImpl implements DataView {
    String fileExtension = ".sparql" ; // extension of the files where DataViews are saved

    @Inject
    private Logger log;

    @Inject
    private ConfigurationService configurationService;

    String dataViewFolder ;

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
    }

    /**
     * Return the TrackingID from parameter if "dataView.GoogleAnalytics" is true
     * otherwise null
     */
    @Override
    public String getGoogleAnalyticsTrackingID()
    {
    	if (configurationService.getBooleanConfiguration("dataView.GoogleAnalytics", false))
    		return configurationService.getStringConfiguration("dataView.GoogleAnalyticsTrackingID", null) ;
    	
    	return null ;
    }
    
    @Override
    public ArrayList<String> getDataViewsList() throws DataViewException
    {
    	ArrayList dataViewsList = new ArrayList<String>() ;

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

    
    /**
     * Method used to add (update=false) or update (update=true) a DataView
     * An exception will be thrown if:
     * - adding with a viewName (file) that already exists
     * - updating with a viewName (file) that don't exist
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
        		
        // Update the file content: the query
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
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    		
    		throw new DataViewException("Impossible to save the query to the DataView file '" + viewName + "': " + e.getMessage()) ;
		}
        	
        return "DataView '" + viewName + "' saved successfully!";
    }
    
    /**
     * Delete a DataView
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
