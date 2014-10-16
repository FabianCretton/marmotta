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
import ch.hevs.overLOD.extDataSources.api.ExtDataSources;
import ch.hevs.overLOD.extDataSources.exceptions.ExtDataSourcesException;

/**
 * Web Service for External Data Sources (EDS) management
 * 
 * @author Fabian Cretton, HES-SO OverLOD surfer project
 */
@Path("/EDS")
@ApplicationScoped
public class ExtDataSourcesWebService {

    @Inject
    private Logger log;
    
    @Inject
    private ExtDataSources extDSService;

    @Inject
    private ContextService contextService;
    
    @Context
    UriInfo uri ;
    
    private ClientConfiguration clientConfig;

    //@Inject
    //private ClientConfiguration iclientConfig;
    
    private static final String URL_EXTERNAL_IMPORT_SERVICE  = "/import/external";
    
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
			mappings = (extDSService != null ? extDSService.getEDSParamsList() : new TreeMap<String,EDSParams>());
		} catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while retrieving External Data Sources list: "+ e.getMessage()).build();
		}
        
        return Response.ok().entity(mappings).build();
    }
    
    /*
     * The actual implementation of adding an EDS and launching the data load
     * 
     * Import a file from URL, by calling the 'import' web service
     * That web service will start a thread to load the data asynchronously
     * 
     * headerAuth: http authentication
     * 	When Marmotta is in a secure mode (restricted for instance), http authentication is used with user/pwd information
     *		This information is then needed to call the other web service
     * 		Setting the user/password inside the ClientConfiguration() seems of no use as those information
     * 		are not handled by the HttpUtils methods, and anyway this information has to be set on the
     * 		POST object, not on the HttpClient
     * contentType: the mimetype of the data to be loaded
     * url: the url of the data
     * context: the context to which the data will be saved in the store
     * return true if ok, otherwise throws an exception
     * throws ExtDataSourcesException
     */
    private Boolean callImportWS(String headerAuth, String contentType, String url, String context) throws ExtDataSourcesException
    {
    	// create a client configuration with the current WS uri's
        clientConfig = new ClientConfiguration(uri.getBaseUri().toString()) ;
        
        HttpClient httpClient = HTTPUtil.createClient(clientConfig);

        String serviceUrl = null ;
		try {
			serviceUrl = clientConfig.getMarmottaUri() + URL_EXTERNAL_IMPORT_SERVICE
					+ "?url=" + URLEncoder.encode(url, "utf-8")
					+ "&context=" + URLEncoder.encode(context, "utf-8");
		} catch (Exception e) {
            log.error("could not encode URI parameter",e.getMessage());
			throw new ExtDataSourcesException(e.getMessage());
		}

    	log.debug("callImportWS- server internal POST call to the import web service:" + serviceUrl);
    	
        HttpPost post = new HttpPost(serviceUrl);
        post.setHeader("Content-Type", contentType);
        
        // if a credential is passed to this web service, pass it to the one we call
        // otherwise the user will be asked for credential, and this call will fail
        if (headerAuth != null && !headerAuth.equals("")) // null if marmotta is configured with no security option
        	post.setHeader("Authorization", headerAuth);
        
        try {
            HttpResponse response = httpClient.execute(post);
            
            switch(response.getStatusLine().getStatusCode()) {
                case 200: 
                    log.debug("import thread started");
                    return true;
                case 412: 
                    log.error("mimetype not supported");
        			throw new ExtDataSourcesException("mimetype not supported: " + response.getStatusLine().getReasonPhrase());
                default:
                    log.error("error importing:{}",response.getStatusLine().getReasonPhrase());
        			throw new ExtDataSourcesException("error importing:: " + response.getStatusLine().getReasonPhrase());
            }
            
        } catch (UnsupportedEncodingException e) {
            log.error("Import Web Service Error - could not encode URI parameter",e.getMessage());
			throw new ExtDataSourcesException("Import Web Service Error - could not encode URI parameter (" + e.getMessage() + ")");
		} catch (Exception e) {
            log.error("Import Web Service exception:",e.getMessage());
			throw new ExtDataSourcesException("Import Web Service Error: " + e.getMessage());
		} finally {
            post.releaseConnection();
        }    
    }
    
    /**
     * Add a new External Data Source parameters and launch the asynchronous loading process
     * 
     * @param headerAuth http authentication
     * @param contentType mimetype of the data source
     * @param context Named Graph to which the file is uploaded in the store
     * @param EDSType a predefined value for the type of EDS - so far only 'RDFFile' is used 
     * @param url address of the file to be uploaded
     * @HTTP 200 in case the query was executed successfully
     * @HTTP 500 in case there was an error during the execution
     * @return a string which is either a validation message or an error message
     */
    /*
     * data load is based on the 'import' web service
     */
    @POST
    @Path("/EDSParams")
    public Response addEDS(@HeaderParam("Authorization") String headerAuth, @HeaderParam("Content-Type") String contentType, @QueryParam("EDSType") String EDSType, @QueryParam("url") String url, @QueryParam("context") String context) {
        log.debug("POST addEDS Content-type:{}, EDSType:{}, url:{} context:{}", contentType, EDSType, url, context);
        // System.out.println("POST addEDS Content-type: "+contentType+", EDSType: "+EDSType+", url: "+ url + ", context: "+ context);
        
        if (StringUtils.isBlank(context))
            return Response.status(Status.NOT_ACCEPTABLE).entity("Web Service call error: missing 'context' parameter").build();
        if (StringUtils.isBlank(url))
            return Response.status(Status.NOT_ACCEPTABLE).entity("Web Service call error: missing 'url' parameter").build();
        if (StringUtils.isBlank(EDSType))
            return Response.status(Status.NOT_ACCEPTABLE).entity("Web Service call error: missing 'EDSType' parameter").build();
        
        /*
         * Test that  this context does not exist already
         */
        if (contextService.getContext(context) != null) // if the context don't exist, null is returned
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error - a new EDS can't be created with an existing context").build();
        
        Response testErrorResponse = testRDFFileURLAccess(url) ;
        if (testErrorResponse != null) // if a Response is sent back, an error occured accessing the url
        	return testErrorResponse ;
        
        // Start the import
        try {
			callImportWS(headerAuth, contentType, url, context) ;
		} catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
        	
        // If import started, save the EDS parameters
        try {
			return Response.ok(extDSService.addEDSParams(EDSType, contentType, url, context)).build();
		} catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error - The import is running, but an error occured while saving External Data Source parameters: "+ e.getMessage()).build();
		}
    }

    /*
     * For a file: test that the URL is accessible
     * The Marmotta's import web service does a test with only "conn.connect()"
     * But that test will succeed if the host is accessible, 
     * which doesn't mean that the specific resource is accessible.
     * So here I test with a request to the "HEAD"
     * Handling a timeout could improve this test
     * 
     * Return a Response object in case the URL is not accessible, otherwise 'null' which means here 'success'
     */
    private Response testRDFFileURLAccess(String url)
    {
	    log.debug("testing URL: "+ url) ;
    	// System.out.println("test:" + url) ;
	    
	    URL finalUrl = null;
	    HttpURLConnection conn = null ;
	    try {
			finalUrl = new URL(url);
	        conn = (HttpURLConnection) finalUrl.openConnection();
	        //conn.connect();
	        conn.setRequestMethod("HEAD");
	        int responseCode = conn.getResponseCode() ;
	        if (responseCode != 200) {
	            log.debug("status different than 200: "+responseCode) ;
	            return Response.status(502).entity("the URL passed as argument cannot be retrieved: " + responseCode).build();
	        } 
		} catch (MalformedURLException e) {
	        log.debug("malformed URL: "+ e.getMessage()) ;
	        return Response.status(502).entity("the URL passed as argument cannot be retrieved - malformed URL").build();
	    } catch(IOException e) {
	        log.debug("IOException: "+ e.getMessage()) ;
	        return Response.status(502).entity("the URL passed as argument cannot be retrieved:"+ e.getMessage()).build();
	    } finally {
	    	if (conn != null)
	    		conn.disconnect();
	    }
	    
    	// System.out.println("test successful") ;
	    log.debug("test successful") ;
	    
	    return null ;
    }
    /**
     * Update an existing External Data Source with the current content of the external file
     * Achieved by deleting the existing context, then launching the asynchronous loading process
     * 
     * @param headerAuth http authentication
     * @param context Named Graph that identifies the EDS and correspond to its context in the store
     * @HTTP 200 in case the query was executed successfully
     * @HTTP 500 in case there was an error during the execution
     * @return a string which is either a validation message or an error message
     */
    /*
     * the update is made of a delete and than an add
     * to ensure that all triples are updated in the corresponding context
     */
    @PUT
    @Path("/EDSParams")
    public Response updateEDS(@HeaderParam("Authorization") String headerAuth, @QueryParam("context") String context) {
        log.debug("PUT updateEDS context:{}", context);

        if (StringUtils.isBlank(context))
            return Response.status(Status.NOT_ACCEPTABLE).entity("Web Service call error: missing 'context' parameter").build();

        // EDSType (and mime-type ?) must be read from the stored EDS
        EDSParams theEDSParams = extDSService.getEDSParams(context) ;
        
        if (theEDSParams == null)
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error - No EDS correspond to the context '" + context + "'").build();
        
        Response testErrorResponse = testRDFFileURLAccess(theEDSParams.url) ;
        if (testErrorResponse != null) // if a Response is sent back, an error occured accessing the url
        	return testErrorResponse ;
        
    	// System.out.println("test passed") ;

        // First delete the existing Context
		if (!contextService.removeContext(context))
            return Response.status(500).entity("Update aborted, the existing context can't be deleted: "+context).build();
		
        // Then re-start the import
        try {
			callImportWS(headerAuth, theEDSParams.contentType, theEDSParams.url, theEDSParams.context) ;
		} catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

		return Response.ok("The EDS is currently updating").build();
    }
    
	/**
	  * Deletes a EDS: its configuration and its named graph (if deleteGraph=true)
	*
	 * @param context the graph (context) of the EDS, which is used to identify the EDS
	 * @param deleteGraph if false: delete only the EDS configuration, otherwise delete the graph (context) as well 
     * @HTTP 200 in case the delete was executed successfully
     * @HTTP 500 in case there was an error during the execution
	 * @return
	 */
    @DELETE
    @Path("/EDSParams")
    public Response delete(@QueryParam("context") String context, @QueryParam("deleteGraph") boolean deleteGraph) {
    	log.debug("Delete EDS - delete graph:"+ deleteGraph + " - context:"+context) ;

    	boolean deleteSuccessful = true ;
    	
        if (StringUtils.isBlank(context)) {
            return Response.status(Status.NOT_ACCEPTABLE).entity("Web Service call error: missing 'context' parameter").build();
        } else {
        	if (deleteGraph)
        		deleteSuccessful = contextService.removeContext(context);
        }

        // Delete the EDS parameters
        try {
        	if (deleteGraph && !deleteSuccessful)
        		return Response.ok(extDSService.deleteEDSParams(context) + " \n(BUT an error occured to delete the context! Please delete it manually.)").build();
        	else
        		return Response.ok(extDSService.deleteEDSParams(context)).build();
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
     * Get the list of outdated data sources
     * ...to be corrected
     * @return Return ...
     */
    @GET
    @Path("/checkUpdates")
    @Produces("application/json")
    public Response checkEDS() {
        log.debug("GET checkEDS");

        try {
			TreeMap<String, EDSParams> mappings = extDSService.getEDSParamsList() ;
			
	        Iterator<String> it = mappings.keySet().iterator();

	        while(it.hasNext()){
	          String context = (String)it.next();
	          String url = ((EDSParams) mappings.get(context)).url ; 
	          checkEDSDate(url) ;
	        }
		} catch (ExtDataSourcesException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        return Response.ok().entity("blabla").build();
    }
    
    private void checkEDSDate(String url)
    {
        // String url = "http://www.websemantique.ch/people/rdf/blabla.rdf" ;
        
    	System.out.println("checkEDS-1:" + url) ;
	    
	    URL finalUrl = null;
	    HttpURLConnection conn = null ;
	    try {
			finalUrl = new URL(url);
	        conn = (HttpURLConnection) finalUrl.openConnection();
	        conn.setRequestMethod("HEAD");
	        long lastModified = conn.getLastModified() ;
	        System.out.println("Last-Modified: " + new Date(lastModified) + " (" + lastModified + ")");
	        /*
	        int responseCode = conn.getResponseCode() ;
	        if (responseCode != 200) {
	            log.debug("status different than 200: "+responseCode) ;
	            return Response.status(502).entity("the URL passed as argument cannot be retrieved: " + responseCode).build();
	        } 
	        */
		} catch (MalformedURLException e) {
	        log.debug("malformed URL: "+ e.getMessage()) ;
	        //return Response.status(502).entity("the URL passed as argument cannot be retrieved - malformed URL").build();
	    } catch(IOException e) {
	        log.debug("IOException: "+ e.getMessage()) ;
	        //return Response.status(502).entity("the URL passed as argument cannot be retrieved:"+ e.getMessage()).build();
	    } finally {
	    	if (conn != null)
	    		conn.disconnect();
	    }
	    
    	// System.out.println("test successful") ;
	    log.debug("test successful") ;
	    
        // return Response.ok().entity("blabla").build();
    }    
}
