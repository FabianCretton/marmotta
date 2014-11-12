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
package ch.hevs.overLOD.dataView.webservices;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.marmotta.client.ClientConfiguration;
import org.apache.marmotta.client.util.HTTPUtil;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.apache.marmotta.platform.core.api.logging.LoggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.PageViewHit;

import ch.hevs.overLOD.dataView.api.DataView;
import ch.hevs.overLOD.dataView.exceptions.DataViewException;

/**
 * Web Service for DataViews management
 * 
 * @author Fabian Cretton, HES-SO OverLOD surfer project
 */
@Path("/dataView")
@ApplicationScoped
public class DataViewWebService {

    //@Inject
    // private Logger log;
	// I don't know why, but it seems that an Inject here will not work, to be investigated
    private Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    @Inject
    private DataView dataViewService;

    @Inject
    private ConfigurationService configurationService;
    
    @Context
    UriInfo uri ;
    
    // private ClientConfiguration clientConfig;
    
    private static final String URL_QUERY_SERVICE  = "/sparql/select";

    /*
     * Comments not for java doc
     * The possible mimetypes are visible in SparqlServiceImpl
     * with references to the W3C standards (where the mimetypes can be seen), as:
     * 	http://www.w3.org/ns/formats/data/SPARQL_Results_CSV
     *  http://www.w3.org/ns/formats/data/SPARQL_Results_JSON
     *  
     *  header - "Authorization": When Marmotta is in a secure mode, http authentication is used with user/pwd information
     *		This information is then needed to call the other web service
     * 		Setting the user/password inside the ClientConfiguration() seems of no use as those information
     * 		are not handled by the HttpUtils methods, and anyway this information has to be set on the
     * 		GET object, not on the HttpClient     */

    /**
     * Get data from a predefined DataView (a predefined query)
     * <br> 
     * If any parameter is expected for the underlying query, they should be passed as other query params and they should start with the "p_" prefix
     * @param headerAuth http authentication
     * @param acceptMimeType the required mimeType of sparql results can be passed as Header\Accept or in the 'format' params, exemples are "application/sparql-results+json", "application/sparql-results+xml", "text/csv"
     * @param viewName the name of the predefined view that is requested
     * @param mimeTypeAsParam the required mimeType of sparql results can be passed as Header\Accept or in the 'format' params, exemples are "application/sparql-results+json", "application/sparql-results+xml", "text/csv" 
     * @HTTP 200 in case the query was executed successfully
     * @HTTP 400 if a querystring parameter is missing
     * @HTTP 500 in case there was an error during the execution
     * @return the view's query result in the format passed as argument in case of success, otherwise an error message
     */
    @GET
    public Response getDataView(@HeaderParam("Authorization") String headerAuth, @HeaderParam("Accept") String acceptMimeType, @Context UriInfo uriInfo, @QueryParam("viewName") String viewName, @QueryParam("format") String mimeTypeAsParam) {
        log.debug("Requesting DataView: {}", viewName);
    	
        String GoogleAnalyticsTrackingID = dataViewService.getGoogleAnalyticsTrackingID();
        
        if (GoogleAnalyticsTrackingID != null)
        {
        	GoogleAnalytics ga = new GoogleAnalytics(GoogleAnalyticsTrackingID) ;
        	//ga.post(new PageViewHit(uri.getBaseUri().toString()+"dataView?viewName=" + viewName, "test"));
        	ga.post(new PageViewHit("dataView?viewName=" + viewName, "test"));
        	log.debug("new PageViewHit for GoogleAnalytics, with trackingId:"+ GoogleAnalyticsTrackingID) ;
        }
    	
    	String mimeType = acceptMimeType ;
    	
        if (StringUtils.isEmpty(acceptMimeType))
        	{	
        	if (StringUtils.isEmpty(mimeTypeAsParam)) {
	            log.warn("No mimetype specified");
	            // No name given? Invalid request.
	            return Response.status(Status.BAD_REQUEST).entity("Missing header 'Accept' or parameter 'format'").build();
	        	}
        	else
        		mimeType = mimeTypeAsParam ;
        	}
        
        if (StringUtils.isEmpty(viewName)) {
            log.warn("No view specified");
            // No name given? Invalid request.
            return Response.status(Status.BAD_REQUEST).entity("Missing Parameter 'viewName'").build();
        }
        
        String query = null ;
        
        try
        {
        	query = dataViewService.readDataViewQuery(viewName) ;
        }
        catch (IOException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error accessing the query file corresponding to the view '"+viewName+"' !").build();
		}

        // Dynamically read if there is any parameter for the SPARQL query
        // they are identified as they should start with "p_"
        // those parameters are predefined 'string' in the .sparql query file
        // and so far, a simple replace() will be done with the specific value
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters(); 
        Iterator<String> it = queryParams.keySet().iterator();

        while(it.hasNext()){
          String paramName = (String)it.next();
          
          if (paramName.startsWith("p_"))
			try {
				query = query.replace(paramName, URLDecoder.decode(queryParams.getFirst(paramName), "UTF-8")) ;
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        log.debug("final query:" + query) ;
        
       return getDataViewImpl(headerAuth, mimeType, query) ;
    }

    /**
     * Save a new DataView: save a SPARQL query to a dataViewName.sparql file
     * @param headerAuth http authentication
     * @param viewName the name of the view which will be the name of the file, must be a non-existing name
     * @param query the SPARQL query 
     * @HTTP 200 in case the query was executed successfully
     * @HTTP 400 if a querystring parameter is missing
     * @HTTP 500 in case there was an error during the execution
     * @return a success or error message
     */
    @POST
    public Response addDataView(@HeaderParam("Authorization") String headerAuth, @QueryParam("viewName") String viewName, @QueryParam("query") String query) {
        log.debug("Adding DataView: {} - {}", viewName, query);
        
        if (StringUtils.isEmpty(viewName)) {
            log.warn("No viewName specified");
            // No name given? Invalid request.
            return Response.status(Status.BAD_REQUEST).entity("Missing Parameter 'viewName'").build();
        }

        if (StringUtils.isEmpty(query)) {
            log.warn("No query specified");
            // No name given? Invalid request.
            return Response.status(Status.BAD_REQUEST).entity("Missing Parameter 'query'").build();
        }

        String saveRes = null ;
		try {
			if (dataViewService != null)
				saveRes = dataViewService.saveDataView(viewName, query, false); // false for adding a new data view
			else
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("dataViewService not instanciated").build();
		} catch (DataViewException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while adding the '" + viewName + "' Data View: "+ e.getMessage()).build();
		}

        return Response.ok(saveRes).build();
    }
    
    /**
     * Update and existing DataView: save a SPARQL query to a dataViewName.sparql file
     * @param headerAuth http authentication
     * @param viewName the name of the view which will be the name of the file, must be a non-existing name
     * @param query the SPARQL query 
     * @HTTP 200 in case the query was executed successfully
     * @HTTP 500 in case there was an error during the execution
     * @return a success or error message
     */
    @PUT
    public Response updateDataView(@HeaderParam("Authorization") String headerAuth, @QueryParam("viewName") String viewName, @QueryParam("query") String query) {
        log.debug("Updating DataView: {} - {}", viewName, query);
    	
        if (StringUtils.isEmpty(viewName)) {
            log.warn("No view specified");
            // No name given? Invalid request.
            return Response.status(Status.BAD_REQUEST).entity("Missing Parameter 'viewName'").build();
        }

        if (StringUtils.isEmpty(query)) {
            log.warn("No query specified");
            // No name given? Invalid request.
            return Response.status(Status.BAD_REQUEST).entity("Missing Parameter 'query'").build();
        }

        String saveRes = null ;
		try {
			if (dataViewService != null)
				saveRes = dataViewService.saveDataView(viewName, query, true); // true for update
			else
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("dataViewService not instanciated").build();
		} catch (DataViewException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while updating the '" + viewName + "' Data View: "+ e.getMessage()).build();
		}

        return Response.ok(saveRes).build();
    }  
    
    /**
     * Delete a DataView
     * @param headerAuth http authentication
     * @param viewName the name of the view which will be the name of the file, must be a non-existing name
     * @HTTP 200 in case the query was executed successfully
     * @HTTP 400 if a querystring parameter is missing
     * @HTTP 500 in case there was an error during the execution
     * @return a success or error message
     */
    @DELETE
    public Response addDataView(@HeaderParam("Authorization") String headerAuth, @QueryParam("viewName") String viewName) {
        log.debug("Deleting DataView: {}", viewName);
        
        if (StringUtils.isEmpty(viewName)) {
            log.warn("No viewName specified");
            // No name given? Invalid request.
            return Response.status(Status.BAD_REQUEST).entity("Missing Parameter 'viewName'").build();
        }

        String saveRes = null ;
		try {
			if (dataViewService != null)
				saveRes = dataViewService.deleteDataView(viewName);
			else
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("dataViewService not instanciated").build();
		} catch (DataViewException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while deleting the '" + viewName + "' Data View: "+ e.getMessage()).build();
		}

        return Response.ok(saveRes).build();
    }    
    
    /**
     * Get the SPARQL query corresponding to a predefined DataView (a predefined query)
     * @param viewName the name of the predefined view that is requested
     * @HTTP 200 in case the query was executed successfully
     * @HTTP 400 if a querystring parameter is missing
     * @HTTP 500 in case there was an error during the execution
     * @return the SPARQL query as a string, otherwise an error message
     */
    @GET
    @Path("/query")
    public Response getDataViewQuery(@QueryParam("viewName") String viewName) {
        if (StringUtils.isEmpty(viewName)) {
            log.warn("No view specified");
            // No name given? Invalid request.
            return Response.status(Status.BAD_REQUEST).entity("Missing Parameter 'viewName'").build();
        }
        
        log.debug("Requesting DataView Query: {}", viewName);
        
    	// create a client configuration with the current WS uri's
        // ClientConfiguration clientConfig = new ClientConfiguration(uri.getBaseUri().toString()) ;
        
        String query = null ;
        
        try
        {
        	query = dataViewService.readDataViewQuery(viewName) ;
        }
        catch (IOException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error accessing the query file corresponding to the view '"+viewName+"' !").build();
		}
        
       return Response.ok(query).build(); 
    }    
    /*
     * Read the SPARQL query corresponding to a viewName and return the string
     * 
     * MOVED TO dataViewImpl
     */
    /*
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
    */
    
    /*
     * actual implementation of getDataView
     * to return the result of the SPARQL query associated to the DataView
     */
    public Response getDataViewImpl(String headerAuth, String acceptMimeType, String query) {
    	// create a client configuration with the current WS uri's
    	ClientConfiguration clientConfig = new ClientConfiguration(uri.getBaseUri().toString()) ;
        
        HttpClient httpClient = HTTPUtil.createClient(clientConfig);

        String serviceUrl = null;

		try {
			serviceUrl = clientConfig.getMarmottaUri() + URL_QUERY_SERVICE + "?query=" + URLEncoder.encode(query, "utf-8");
		} catch (UnsupportedEncodingException e1) {
            log.error("could not encode query {} as parameter - exception: {}",query, e1.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("could not encode query '"+query+"' as parameter - exception: " + e1.getMessage()).build();
		}

        HttpGet get = new HttpGet(serviceUrl);

        get.setHeader("Accept", acceptMimeType) ; // predefined values could be: TupleQueryResultFormat.JSON.getDefaultMIMEType()
        
        // if a credential is passed to this web service, pass it to the one we call
        // otherwise the user will be asked for credential, and this call will fail
        if (headerAuth != null && !headerAuth.equals("")) // null if marmotta is configured with no security option
        	get.setHeader("Authorization", headerAuth);

        try {
            HttpResponse response = httpClient.execute(get);

            // I would have liked to just send back the 'response' 
            // but didn't find a way so far to convert the "org.apache.http.HttpResponse"
            // to javax.ws.rs.core.Response
            // So I get the text stream and send it back in a new javax.ws.rs.core.Response
            switch(response.getStatusLine().getStatusCode())
            	{
            	case 200:
            		log.debug("SPARQL Query {} evaluated successfully",query);
            		// Trials to send back the response directly:
            		// this throws an exception: java.net.SocketException: socket closed
            		//return Response.ok(response.getEntity().getContent()).build();
            		// this returns a "org.apache.http.conn.EofSensorInputStream@a0c514a"
            		//return Response.ok(response.getEntity().getContent().toString()).build();
            		
            		// Get the response's Text, and send it back in a new response object
            		
            		/* Do the copy manually line by line
            		HttpEntity entity = response.getEntity();
            		
            		// open question: is it correct that the CSV/TSV result needs a lineSeparator, but not the other ones ?
            		// answer after testing the IOUtils code here under: no, the new line is there for any format and thus should be added anyway
            		//boolean addLineSeparator = acceptMimeType.equals(TupleQueryResultFormat.CSV.getDefaultMIMEType()) || acceptMimeType.equals(TupleQueryResultFormat.TSV.getDefaultMIMEType()) ;
            		
            		StringBuilder sb = new StringBuilder();
            		try {
            		    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()), 65728);
            		    String line = null;

            		    line = reader.readLine() ;
            		    
            		    while (line != null) {
            		        sb.append(line);
            		        
            		        line = reader.readLine();
            		        
            		        // if (addLineSeparator && line != null)
            		        if (line != null)
            		        	sb.append(System.lineSeparator());
            		    }
            		}
            		catch (Exception e) {
            			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("error reading the SPARQL result InputStream: '" + e.getMessage() +"'").build();
            			}
            		
            		return Response.ok(sb.toString()).build();
            		*/
            		
            		// can we assume that the format is UTF-8 ?
            		return Response.ok(IOUtils.toString(response.getEntity().getContent(), "UTF-8")).build();
            		
        		default:
        			log.error("error evaluating SPARQL Select Query {}: {} {}",new Object[] {query,response.getStatusLine().getStatusCode(),response.getStatusLine().getReasonPhrase()});
        			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("error evaluating SPARQL Select Query '"+query+"' - Status Code:" + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase()).build();
            	}
        } catch(Exception e) {
			log.error("error evaluating SPARQL Select Query {}: {}", query, e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("error evaluating SPARQL Select Query '"+query+"' - Exception:" + e.getMessage()).build();
		} finally {
            get.releaseConnection();
        } 
    }
       
    /**
     * A service only used for unit test purpose
     * This tests that the SPARQL web service is correctly called with a predefined query
     * @param headerAuth http authentication
     * @param acceptMimeType mimeType of sparql results, as "application/sparql-results+json", "application/sparql-results+xml", "text/csv"
     * @HTTP 200 in case the query was executed successfully
     * @HTTP 500 in case there was an error during the execution
     * @return the view's query result in the format passed as argument in case of success, otherwise an error message
     */
    /*
     * The real getDataView() will read the query from a file found in the marmotta-home/DataView sub-folder
     * Currently, I didn't implement the creation of that folder and corresponding files that could be used
     * by the unit test (which executes in a temporary marmotta-home), hence this method for simple tests
     */
    @GET
    @Path("/test")
    public Response testGetDataView(@HeaderParam("Authorization") String headerAuth, @HeaderParam("Accept") String acceptMimeType) 
    {
        String query = "SELECT ?s ?p WHERE {?s ?p ?o} LIMIT 10" ;

        return getDataViewImpl(headerAuth, acceptMimeType, query) ;
    }
    
    /*
     * A trial to avoid copying the return from HTTPResponse to a new object
     * Code inspired by http://www.hascode.com/2013/12/jax-rs-2-0-rest-client-features-by-example/
     * But I couldn't make it work.
     * The invoke() and return are done (status 200 - ok), but the client is still waiting for a while, and then
     * displays the "success" but with no content
     * 
     * The ClientBuilder requires dependency such as:
		<dependency>
			<groupId>org.glassfish.jersey.core</groupId>
			<artifactId>jersey-client</artifactId>
			<version>2.12</version>
		</dependency>
		
     *   otherwise a classNotFound is thrown
    public Response getDataView_JAX_RS_Trial(@HeaderParam("Accept") String acceptMimeType, @QueryParam("viewName") String viewName) {
        if (StringUtils.isEmpty(viewName)) {
            log.warn("No view specified");
            // No name given? Invalid request.
            return Response.status(Status.BAD_REQUEST).entity("Missing Parameter 'viewName'").build();
        }

        log.debug("Requesting DataView: {}", viewName);
        
    	// create a client configuration with the current WS uri's
        clientConfig = new ClientConfiguration(uri.getBaseUri().toString()) ;
        
        String query = null ; // "SELECT ?s ?p WHERE {?s ?p ?o}" ; //  LIMIT 10" ;
        
        String queryFile = configurationService.getHome() + File.separator + "dataViews" + File.separator + viewName + ".sparql" ;

        // Read the .sparql file
        FileInputStream inputStream = null;
        try {
			inputStream = new FileInputStream(queryFile);
            query = IOUtils.toString(inputStream);
        } catch (IOException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while reading the query file corresponding to the view '"+viewName+" (" + queryFile + "): "  + e.getMessage()).build();
		} finally {
            try {
            	if (inputStream != null)
            		inputStream.close();
			} catch (IOException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while closing the query file corresponding to the view '"+viewName+" (" + queryFile + "): "  + e.getMessage()).build();
			}
        }        
        
        System.out.println("av new Client") ;
        Client client = ClientBuilder.newClient() ;
        System.out.println("ap new Client") ;
        Invocation sparqlClient = null ;
        
		try {
			
	        System.out.println("create client: "+clientConfig.getMarmottaUri() + URL_QUERY_SERVICE + "?query=" + URLEncoder.encode(query, "utf-8")) ;
	        
	        sparqlClient = client.target(clientConfig.getMarmottaUri() + URL_QUERY_SERVICE + "?query=" + URLEncoder.encode(query, "utf-8")).request().header("Accept", acceptMimeType).buildGet() ;
	        //sparqlClient = client.target(clientConfig.getMarmottaUri() + URL_QUERY_SERVICE + "?query=" + URLEncoder.encode(query, "utf-8")).request(acceptMimeType).buildGet() ;
		} catch (UnsupportedEncodingException e1) {
            log.error("could not encode query {} as parameter - exception: {}",query, e1.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("could not encode query '"+query+"' as parameter - exception: " + e1.getMessage()).build();
		}

		
		System.out.println("invoke!!") ;
		Response respSparqlClient = sparqlClient.invoke() ;
		System.out.println("response status:"+respSparqlClient.getStatus()) ;
		return Response.fromResponse(respSparqlClient).build() ;
		// also tried 
		// return respSparqlClient ;
    }
         */

    /**
     * Get the list of configured Data views
     * @return Return a Array of string that is the list of the names of the existing data views
     */
    @GET
    @Path("/list")
    @Produces("application/json")
    public Response getDataViewsList() {
        log.debug("GET getDataViewsList");
        
        ArrayList<String> dataViewsList ;
		try {
			dataViewsList = (dataViewService != null ? dataViewService.getDataViewsList() : new ArrayList<String>());
		} catch (DataViewException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while retrieving Data Views list: "+ e.getMessage()).build();
		}

        return Response.ok().entity(dataViewsList).build();
    }
}
