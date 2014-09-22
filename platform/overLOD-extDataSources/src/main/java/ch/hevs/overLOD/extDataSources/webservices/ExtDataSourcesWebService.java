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
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.marmotta.client.ClientConfiguration;
import org.apache.marmotta.client.util.HTTPUtil;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.hevs.overLOD.extDataSources.EDSParams.EDSParams;
import ch.hevs.overLOD.extDataSources.api.*;
import ch.hevs.overLOD.extDataSources.exceptions.*;

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

    @Context
    UriInfo uri ;
    
    private ClientConfiguration clientConfig;

    private static final String URL_EXTERNAL_IMPORT_SERVICE  = "/import/external";
    
    /**
     * Return a TreeMap that is the ordered list of configured EDS
     * @return
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
    
    /**
     * Import a file from URL, by calling the 'import' web service
     * That web service will start a thread to load the data asynchronously
     * 
     * @param contentType
     * @param url
     * @param context
     * @return true if ok, otherwise throws an exception
     * @throws ExtDataSourcesException
     */
    private Boolean callImportWS(String contentType, String url, String context) throws ExtDataSourcesException
    {
    	// create a client configuration with the current WS uri's
        clientConfig = new ClientConfiguration(uri.getBaseUri().toString()) ;
        
        HttpClient httpClient = HTTPUtil.createClient(clientConfig);

        String serviceUrl = null ;
		try {
			serviceUrl = clientConfig.getMarmottaUri() + URL_EXTERNAL_IMPORT_SERVICE 
					+ "?url=" + URLEncoder.encode(url, "utf-8")
					+ "&context=" + URLEncoder.encode(context, "utf-8");
		} catch (UnsupportedEncodingException e) {
            log.error("could not encode URI parameter",e.getMessage());
			throw new ExtDataSourcesException(e.getMessage());
		}

    	log.debug("callImportWS- server internal POST call to the import web service:" + serviceUrl);
        
        HttpPost post = new HttpPost(serviceUrl);
        post.setHeader("Content-Type", contentType);

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
     * Save one External Data Source parameters and start loading it using the 'import' Web Service of Marmotta
     * @param EDSType, so far only 'RDFFile' is handled 
     * @param url of the file to be uploaded
     * @param context Named Graph to which the file is uploaded
     * @return
     * @throws ExtDataSourcesException
     */
    @POST
    @Path("/EDSParams")
    public Response addEDS(@HeaderParam("Content-Type") String contentType, @QueryParam("EDSType") String EDSType, @QueryParam("url") String url, @QueryParam("context") String context) {
        log.debug("POST addEDS Content-type:{}, EDSType:{}, url:{} context:{}", contentType, EDSType, url, context);

        // First starts the import
        try {
			callImportWS(contentType, url, context) ;
		} catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
        	
        // If import started, save the EDS parameters
        try {
			return Response.ok(extDSService.saveEDSParams(EDSType, url, context)).build();
		} catch (ExtDataSourcesException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error - The import is running, but an error occured while saving External Data Source parameters: "+ e.getMessage()).build();
		}
    }

}
