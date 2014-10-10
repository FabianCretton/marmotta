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
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.marmotta.client.ClientConfiguration;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.slf4j.Logger;

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

    @Inject
    private Logger log;

    @Inject
    private ConfigurationService configurationService;

    @Override
    public ArrayList<String> getDataViewsList() throws DataViewException
    {
    	ArrayList dataViewsList = new ArrayList<String>() ;

        String dataViewFolderPath = configurationService.getHome() + File.separator + "dataViews" + File.separator ;
    	
    	// dataViewsList.add("dataView1") ; // , "select * where {?s ?p ?o} limit 10") ;
    	// dataViewsList.add("dataView2") ; // , "select ?s where {?s ?p ?o} limit 10") ;
        String fileName;
        File folder = new File(dataViewFolderPath);
        File[] listOfFiles = folder.listFiles(); 
       
        String fileExtension = ".sparql" ;
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

        // System.out.println("service getDataViewsList(), size: "+ dataViewsList.size());

    	return dataViewsList ;
    }

    @Override
    public String helloWorld(String name) {
        log.debug("Greeting {}", name);
        return "Hello " + name;
    }

}
