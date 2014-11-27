/*
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
package ch.hevs.overLOD.extDataSources.webservices;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.marmotta.client.ClientConfiguration;
import org.apache.marmotta.client.util.HTTPUtil;
import org.apache.marmotta.platform.core.api.triplestore.ContextService;
import org.openrdf.model.URI;
import org.slf4j.Logger;

import ch.hevs.overLOD.extDataSources.EDSParams.EDSParams;
import ch.hevs.overLOD.extDataSources.EDSParams.StringListForWSReturn;
import ch.hevs.overLOD.extDataSources.api.ExtDataSources;
import ch.hevs.overLOD.extDataSources.exceptions.ExtDataSourcesException;

/**
 * Web Service for External Data Sources (EDS) management
 * 
 * External Data Sources (EDS) aims at managing data coming from external sources, 
 * as RDF files, RDFa, data from a SPARQL end-point or even non-RDF structured data 
 * that needs to be translated to RDF (RDfized) as for instance Microdata from schema.org.
 * 
 * When discovering Marmotta, we found out that this feature is really similar 
 * to Marmotta's LDCache feature, but we still needed a few different behaviors.
 * 
 * Currently Linked Data and RDF files are handled using LDClient, in a synchronous way sofar
 * 	A new LDClient for RDF Files has been created (this module thus needs the proper version of 'ldclient-provider-rdf')
 *  	Make sure the RDFFileProvider and RDFFileEndpoint are registered in LDClient for the
 * 		RDF files handling to work correctly. When compiling, this is tested by the unit test.
 *  Other LDClients should be tested in the future
 * 
 * So far EDS parameters are read only and can be set only when creating the EDS
 * 	to change the parameters, the EDS must be deleted and a new one created by the user
 * 
 * See the module's about.html for more information 
 *
 * @author Fabian Cretton, HES-SO OverLOD surfer project
 * http://www.hevs.ch/fr/rad-instituts/institut-informatique-de-gestion/projets/overlod-surfer-6349
 */
@Path("/EDS")
@ApplicationScoped
public class ExtDataSourcesWebService {

    @Inject
    private Logger log;
    
    @Inject
    private ExtDataSources edsService;

    @Inject
    private ContextService contextService;
    
    @Context
    UriInfo uri ;
    
    /**
     * Get the list of configured External Data Sources (EDS)
     * @return Return a TreeMap that is the ordered list of configured EDS
     */
    @GET
    @Path("/EDSParams")
    @Produces("application/json")
    public Response getEDSParamsList() {
        log.debug("GET getEDSParamsList");

        TreeMap<String, EDSParams> mappings;
		try {
			mappings = (edsService != null ? edsService.getEDSParamsList() : new TreeMap<String,EDSParams>());
		} catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while retrieving External Data Sources list: "+ e.getMessage()).build();
		}
        
        return Response.ok().entity(mappings).build();
    }
    
    /**
     * Add a new External Data Source parameters using LDClient
     * 
     * @param headerAuth http authentication
     * @param contentType mimetype of the data source
     * @param context Named Graph to which the file is uploaded in the store
     * @param EDSType a predefined value for the type of EDS - as 'RDFFile'
     * @param url address of the file to be uploaded
     * @param filterFileName name of file (including file extension) that will allow to import only part of the data using a SPARQL CONSTRUCT query. This file must be available in the folder %marmotta-home%/EDS/EDSFilters/. (OPTIONAL - don't specify it to import the all data)
     * @param validationFileName name of file (including file extension) that will allow to check the data validity using SPIN constraints. This file must be available in the folder %marmotta-home%/EDS/SPIN/Constraints/. (OPTIONAL - don't specify it to import data without validation)
     * 
     * @HTTP 200 in case the query was executed successfully
     * @HTTP 400 if a querystring parameter is missing
     * @HTTP 500 in case there was an error during the execution
     * @HTTP 502 in case there was an error while accessing the specified URL
     * @return a string which is either a validation message or an error message
     */
    /*
     * The timestamp:
     * - for an RDF file, it does sometimes work, other times not: for instance geoNames resources
     * don't have an HTTP last-modified, but the rdf file itself does contain a date triple, this hasn't been used sofar
     * - for a Linked Data resoure, it seems no HTTP last-modified is available for DBPedia, but 
     * also the 303 redirect should be handled, so this is let for later 
     */
    @POST
    @Path("/EDSParams")
    public Response addEDS(@HeaderParam("Authorization") String headerAuth, @HeaderParam("Content-Type") String contentType, @QueryParam("EDSType") String EDSType, @QueryParam("url") String url, @QueryParam("context") String context,
    		@QueryParam("filterFileName") String filterFileName, @QueryParam("validationFileName") String validationFileName) 
    {
        log.debug("POST addEDS Authorization:{}, Content-type:{}, EDSType:{}, url:{} context:{} filterFileName:{} validationFileName:{}", headerAuth, contentType, EDSType, url, context, filterFileName, validationFileName);
       
        String importWithLDClientResultString = "" ;
        
        if (StringUtils.isBlank(context))
            return Response.status(Status.BAD_REQUEST).entity("Web Service call error: missing 'context' parameter").build();
        if (StringUtils.isBlank(url))
            return Response.status(Status.BAD_REQUEST).entity("Web Service call error: missing 'url' parameter").build();
        if (StringUtils.isBlank(EDSType))
            return Response.status(Status.BAD_REQUEST).entity("Web Service call error: missing 'EDSType' parameter").build();
        
        // Ensure that the context for the new EDS don't exist
        if (contextService.getContext(context) != null) // if the context don't exist, null is returned
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error - a new EDS can't be created with an existing context").build();
        
        long timeStamp = 0 ;

        // test the URL Access, and at the same time get a timestamp if available
        try
        {
        timeStamp = testURLAccess(url) ;
        }
        catch(ExtDataSourcesException e){
        	return Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).build();
        }

        // Start the import using  LDClient
        try {
        	importWithLDClientResultString = edsService.importWithLDClient(uri.getBaseUri().toString(), headerAuth, EDSType, url, context, filterFileName, validationFileName) ;
		} catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
        	
        // If import done or started, save the EDS parameters
        try {
        	edsService.addEDSParams(EDSType, contentType, url, context, String.valueOf(timeStamp), filterFileName, validationFileName) ;        	
        	// return the confirmation string returned by importWithLD
			return Response.ok(importWithLDClientResultString).build();
		} catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error - The import is running, but an error occured while saving External Data Source parameters: "+ e.getMessage()).build();
		}
    }

    /**
     * For a URL: test that the URL is accessible
     * 
     * For information: the Marmotta's import web service does a test with only "conn.connect()"
     * But that test will succeed if the host is accessible, 
     * which doesn't mean that the specific resource is accessible.
     * The test here request HTTP "HEAD"
     * 
     * Handling a timeout could improve this test efficiency
     * 
     * We had a look at ldclient.ping(url), which seems to fail for a Linked Data URL
     * 
     * @param url the URL to be tested
     * @return the long value of conn.getLastModified(): milliseconds since 01.01.1970, if that value is '0', return the long value of conn.getExpiration()
     * @throws ExtDataSourcesException Throws an exception if the url can't be accessed
     */
    private long testURLAccess(String url) throws ExtDataSourcesException
    {
	    log.debug("testing URL: "+ url) ;
	    
	    long timestamp = 0 ; 
	    
	    URL finalUrl = null;
	    HttpURLConnection conn = null ;
	    try {
			finalUrl = new URL(url);
	        conn = (HttpURLConnection) finalUrl.openConnection();
	        log.debug("connection open") ;
	        conn.setRequestMethod("HEAD");
	        log.debug("request head") ;
	        int responseCode = conn.getResponseCode() ;
	        
	        if (responseCode != 200) {
	            log.debug("status different than 200: "+responseCode) ;
	            throw new ExtDataSourcesException("the URL passed as argument cannot be retrieved: " + responseCode);
	        } 
	        
	        // getLastModified() can't be used to test if the file exists or not
	        // -> if the URL don't exist, getLastModified just returns 0, with no error
	        timestamp = conn.getLastModified() ; // milliseconds since 01.01.1970
	        log.debug("Last modified: " + timestamp) ;
	        
	        if (timestamp == 0) // no "last-modified", take "expires" instead
	        {
	        	timestamp = conn.getExpiration() ;
	        	log.debug("Last modified not set, use 'expires' instead: " + timestamp) ;
	        }
		} catch (MalformedURLException e) {
	        log.debug("malformed URL: "+ e.getMessage()) ;
            throw new ExtDataSourcesException("the URL passed as argument cannot be retrieved - malformed URL (" + e.getMessage() + ")");
	    } catch(IOException e) {
	        log.debug("IOException: "+ e.getMessage()) ;
            throw new ExtDataSourcesException("the URL passed as argument cannot be retrieved - IO Exception (" + e.getMessage() + ")");
	    } finally {
	    	if (conn != null)
	    		conn.disconnect();
	    }
	    
	    log.debug("URL test successful") ;
	    
	    return timestamp ;
    }
    /**
     * Update an existing External Data Source with the current content of the external file
     * Achieved by deleting the existing context, then re-importing the data from the source
     * 
     * @param headerAuth http authentication
     * @param context Named Graph that identifies the EDS and correspond to its context in the store
     * @HTTP 200 in case the query was executed successfully
     * @HTTP 400 if a querystring parameter is missing
     * @HTTP 500 in case there was an error during the execution
     * @return a string which is either a validation message or an error message
     */
    @PUT
    @Path("/EDSParams")
    public Response updateEDS(@HeaderParam("Authorization") String headerAuth, @QueryParam("context") String context) {
        log.debug("PUT updateEDS context:{}", context);
        long timeStamp = 0 ;
        String updateConfirmationMsg = "" ;
        
        if (StringUtils.isBlank(context))
            return Response.status(Status.BAD_REQUEST).entity("Web Service call error: missing 'context' parameter").build();

        // Get the parameters for that EDS
        EDSParams theEDSParams = edsService.getEDSParams(context) ;
        
        if (theEDSParams == null)
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error - No EDS correspond to the context '" + context + "'").build();
        
        try
        {
        	timeStamp = testURLAccess(theEDSParams.url) ;
        }
        catch(ExtDataSourcesException e){
        	return Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).build();
        }
        
        // update the data in the store
        try
        {
        	updateConfirmationMsg = updateEDSinStore(headerAuth, theEDSParams) ;
        }
        catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
        
        // udpate time stamp, if it fails, the call still succeeds but an error message
        // is appended to the success message
       	String updateTimeStampErrorMsg = updateTimeStamp(theEDSParams, timeStamp) ;
        
		return Response.ok("The EDS was updated successfuly. " + updateConfirmationMsg + " " + updateTimeStampErrorMsg).build();
    }
    
    /**
     * Update an EDS: delete the context, reload the file
     * @param headerAuth the header authentication passed to the web service call
     * @param theEDSParams the EDSParams to update
     * @throws ExtDataSourcesException
     * @return the success message from edsService.importWithLDClient()
     */
    private String updateEDSinStore(String headerAuth, EDSParams theEDSParams) throws ExtDataSourcesException
    {
	    // First delete the existing Context
		if (!contextService.removeContext(theEDSParams.context))
			throw new ExtDataSourcesException("Update aborted, the existing context can't be deleted: "+theEDSParams.context) ;
		
		
	    // Then re-start the import
	    try {
        	return edsService.importWithLDClient(uri.getBaseUri().toString(), headerAuth,  theEDSParams.contentType, theEDSParams.url, theEDSParams.context, theEDSParams.filterFileName, theEDSParams.validationFileName) ;
		} catch (ExtDataSourcesException e) {
			throw new ExtDataSourcesException(e.getMessage()) ;
		}
    }
    
    /**
     *  Update the time stamp of an EDS
     * @param theEDSParams
     * @return an empty string if successful, otherwise a WARNING message
     */
    private String updateTimeStamp(EDSParams theEDSParams, long newTimeStamp)
    {
    long localTimeStamp = Long.valueOf(theEDSParams.timeStamp) ;
    if (newTimeStamp != localTimeStamp)
		try {
			if (!edsService.setEDSParamsTimeStamp(theEDSParams.context, String.valueOf(newTimeStamp)))
				return " WARNING: the timeStamp of the EDS could not be updated!" ;
		} catch (ExtDataSourcesException e) {
			return " WARNING: the timeStamp of the EDS could not be updated (" + e.getMessage() + ")" ;
		}
    
    return "" ;
    }
    
	/**
	 * Deletes a EDS: its parameters and its named graph (if deleteGraph=true)
	 * The returned message will handle information for both operations: delete context and delete parameters
	 * @param context the graph (context) of the EDS, which is used to identify the EDS
	 * @param deleteGraph if false: delete only the EDS configuration, otherwise delete the graph (context) as well 
     * @HTTP 200 in case the delete was executed successfully
     * @HTTP 400 if a querystring parameter is missing
     * @HTTP 500 in case there was an error during the execution
	 */
    @DELETE
    @Path("/EDSParams")
    public Response delete(@QueryParam("context") String context, @QueryParam("deleteGraph") boolean deleteGraph) {
    	log.debug("Delete EDS - delete graph:"+ deleteGraph + " - context:"+context) ;

    	boolean deleteSuccessful = true ;
    	
        if (StringUtils.isBlank(context)) {
            return Response.status(Status.BAD_REQUEST).entity("Web Service call error: missing 'context' parameter").build();
        } else {
        	if (deleteGraph)
        		deleteSuccessful = contextService.removeContext(context);
        }

        // Delete the EDS parameters and manage different messages
        try {
        	if (deleteGraph && !deleteSuccessful)
        		return Response.ok(edsService.deleteEDSParams(context) + " \n(BUT an error occured to delete the context! Please delete it manually.)").build();
        	else
        		return Response.ok(edsService.deleteEDSParams(context)).build();
		} catch (ExtDataSourcesException e) {
			log.debug("extDSService.deleteEDSParams Exception:"+ e.getMessage()) ;
			
			if (deleteGraph)
			{
				if (deleteSuccessful)
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error - The graph has been deleted but the EDS can't be deleted from the EDS list: "+ e.getMessage()).build();
				else
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error - The graph could not ge deleted and the EDS can't be deleted from the EDS list: "+ e.getMessage()).build();
			}
			else
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error - The EDS can't be deleted from the EDS list: "+ e.getMessage()).build();
		}
    }
    
    /**
     * Check the list of EDS, and launch the update of outdated ones
     * 
     * Currently two kind of timestamps are handled: either a "last-update" or an "expires"
     * All those timestamps are milliseconds since 1.1.1970
     * In this simple version, the kind of timestamp is differenciated by testing if the timestamp is more recent than today:
     * - if yes, timestamp is a past date, it is considered a "last-update" and so the current timestamp of the
     * 		original resource needs to be retrieved
     * - if no, timestamp is a futur date, it is considered as "expires" value and so no update is needed
     * 		when that timestamp will be tested after the "expiry date", the original resource will then be checked
     * 		This mechanism was needed as for DBPedia, the "expires" date does change everyday - certainly as an automatic setting,
     * 		but we don't want to update the data everyday so we wait until the "expires" date is passed.
     * 		Anyway this is not effective enough as it does not mean that the DBPedia resource did effectively change
     * 		So just for trials purpose.
     * 
     * @param headerAuth http authentication
     * @return Return a list of updated EDS, an empty list if all EDS were up-to-date
     */
    @PUT
    @Path("/checkUpdates")
    @Produces("application/json")
    public Response checkEDSUpdates(@HeaderParam("Authorization") String headerAuth) {
        log.debug("GET checkEDS");

        StringListForWSReturn updatedEDSList = new StringListForWSReturn() ;

        TreeMap<String, EDSParams> mappings = null ;
        
        // current date
	   	long currentTime = (new Date()).getTime(); // current time in milliseconds
        
        try {
			mappings = edsService.getEDSParamsList() ;
			
	        Iterator<String> it = mappings.keySet().iterator();

	        while(it.hasNext()){ // loop on all EDS
	          String context = (String)it.next();
	          EDSParams params = (EDSParams) mappings.get(context) ; 

	          long localTimeStamp = Long.valueOf(params.timeStamp) ;
	          
	          if (localTimeStamp <= currentTime) // if the timestamp of the EDS is a future date, it is considered an "expire" date which is thus not reached yet
	          {
		          long timeStamp = 0 ;
		          
		          try
		          {
		          timeStamp = testURLAccess(params.url) ;
		          }
		          catch(ExtDataSourcesException e){
		        	log.debug("Can't access EDS URL '" + params.url + "', exception: " + e.getMessage());
		          }
		          
		          if (timeStamp > localTimeStamp)
		          {
		        	  log.debug("Update needed for '" + params.context + "'") ;
		        	  
		              // update the data in the store
		              try
		              {
		              	updateEDSinStore(headerAuth, params) ;
		              	
		                // udpate time stamp, if it fails, the call still succeeds but an error message
		                // is appended to the success message
		               	String updateTimeStampErrorMsg = updateTimeStamp(params, timeStamp) ;
	
		               	if (updateTimeStampErrorMsg != "")
		               		updateTimeStampErrorMsg = " (" + updateTimeStampErrorMsg + ")" ;
	
		               	updatedEDSList.stringList.add(params.context + updateTimeStampErrorMsg) ;
		              }
		              catch (ExtDataSourcesException e) {
		      			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		      		}
		        	  
		          }
		          else
		          {
		        	  log.debug("Data up-to-date for '" + params.url + "' (timestamp considered as 'last-modified')") ;
		          }
	          }
	          else
	          {
	        	  log.debug("Data up-to-date for '" + params.url + "' (timestamp considered as 'expires', and not reached yet)") ;
	          }
	        }
		} catch (ExtDataSourcesException e1) {
  			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e1.getMessage()).build();
		}
        
        return Response.ok().entity(updatedEDSList).build();
    }
    
    /**
     * Get the list of data filter files, files that allow to import only a part of an external data source
     * @return Return a Array of string that is the list of the names of the existing filter files
     */
    @GET
    @Path("/filters/list")
    @Produces("application/json")
    public Response getDataFiltersList() {
        log.debug("GET getDataFiltersList");
        
        ArrayList<String> dataFiltersList ;
		try {
			dataFiltersList = (edsService != null ? edsService.getDataFiltersList() : new ArrayList<String>());
		} catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while retrieving Data Filters list: "+ e.getMessage()).build();
		}

        return Response.ok().entity(dataFiltersList).build();
    } 
    
    /**
     * Get the list of data validators files, files that allow to check data according to constraints
     * @return Return a Array of string that is the list of the names of the existing validators files
     */
    @GET
    @Path("/validators/list")
    @Produces("application/json")
    public Response getDataValidatorsList() {
        log.debug("GET getDataFiltersList");
        
        ArrayList<String> dataValidatorsList ;
		try {
			dataValidatorsList = (edsService != null ? edsService.getDataValidatorsList() : new ArrayList<String>());
		} catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while retrieving Data Validators list: "+ e.getMessage()).build();
		}

        return Response.ok().entity(dataValidatorsList).build();
    }       

}
