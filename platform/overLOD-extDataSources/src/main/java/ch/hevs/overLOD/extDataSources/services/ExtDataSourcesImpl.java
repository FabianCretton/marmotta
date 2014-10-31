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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.util.EntityUtils;
import org.apache.marmotta.client.ClientConfiguration;
import org.apache.marmotta.client.clients.ImportClient;
import org.apache.marmotta.client.exception.MarmottaClientException;
import org.apache.marmotta.client.util.HTTPUtil;
import org.apache.marmotta.commons.sesame.model.ModelCommons;
import org.apache.marmotta.ldclient.api.provider.DataProvider;
import org.apache.marmotta.ldclient.exception.DataRetrievalException;
import org.apache.marmotta.ldclient.model.ClientResponse;
import org.apache.marmotta.ldclient.services.ldclient.LDClient;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;

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

    private static final String URL_UPLOAD_SERVICE = "/import/upload";
    
    @PostConstruct
    public void initialize() 
    {
    	log.debug("ExtDataSourcesImpl initialize() - loading EDSParams list from disk") ;

    	try {
			loadEDSParamsListFromDisk() ;
		} catch (ExtDataSourcesException e) {
			log.error("ExtDataSourcesImpl.initialize(), loadEDSParamsListFromDisk() exception: " + e.getMessage() );
		}
    }
    
    @Override
    public TreeMap<String,EDSParams> getEDSParamsList() throws ExtDataSourcesException
    {
        return cacheEDSParamsList.getList() ;
    }
    
    @Override
    public String addEDSParams(String EDSType, String contentType, String url, String context, String timeStamp) throws ExtDataSourcesException {
        log.debug("saving EDSParams EDSType:{}, contentType:{} url:{} context:{}...", EDSType, contentType, url, context);
        
        // an EDS is read only, once created it will not change, if params change (the url, the mimetype), than a new EDS has to be created
        // so ensure here that an EDS is not saved with an existing context (the identifier)
        if (cacheEDSParamsList.exists(context))
        	throw new ExtDataSourcesException("A new EDS can't be added with a context already used for another one") ;
        	
        EDSParams aEDSParams = new EDSParams(EDSType, contentType, url, context, timeStamp) ;
        
        synchronized(this) // to avoid concurrent access to the object and saving the serialization. If more is needed, see 'mutex' 
        	{
	        cacheEDSParamsList.addEDSParams(aEDSParams);
	        
	       	serializeObjectToJSON(cacheEDSParamsList, configurationService.getHome() + "/EDSParamsList.json") ;
	       	log.debug("saving EDSParams EDSType:{}, url:{} context:{}...done!", EDSType, url, context);
        	}
        
        return "Parameters saved for External Data Source: " + context ;
    }

    @Override
    public boolean setEDSParamsTimeStamp(String context, String timeStamp) throws ExtDataSourcesException
    {
        log.debug("update timeStep of EDSParams identified by context:{}", context);
    	
        if (cacheEDSParamsList.setEDSParamsTimeStamp(context, timeStamp))
        {
        	serializeObjectToJSON(cacheEDSParamsList, configurationService.getHome() + "/EDSParamsList.json") ;
            log.debug("saving EDSParams list after a timeStamp update...done!");
            return true ;
        }
        else
        {
            log.debug("can't set the timeStamp");
        	return false ;
        }
        
    }
    
    @Override
    public EDSParams getEDSParams(String context)
    {
    	return cacheEDSParamsList.get(context) ;
    }

    @Override
    public String deleteEDSParams(String context) throws ExtDataSourcesException {
        log.debug("delete EDSParams identified by context:{}", context);
        
        synchronized(this) // to avoid concurrent access to the object and saving the serialization. If more is needed, see 'mutex' 
	    	{
	        if (!cacheEDSParamsList.deleteEDSParams(context)) // context not found in the list
	        	throw new ExtDataSourcesException("Context '"+ context + "' not found in the list of EDS") ;
	        
	       	serializeObjectToJSON(cacheEDSParamsList, configurationService.getHome() + "/EDSParamsList.json") ;
	    	}
        
        log.debug("saving EDSParams list after a delete...done!");
        
        return "EDS deleted from External Data Sources list: " + context ;
    }

    /**
     * If the file don't exist yet, then create it (could happen on the first run of the module)
     * @throws ExtDataSourcesException 
     */
    private void loadEDSParamsListFromDisk() throws ExtDataSourcesException
    {
    	if (cacheEDSParamsList == null)
    	{
    		String EDSParamsListFile = configurationService.getHome() + "\\EDSParamsList.json" ;
    		cacheEDSParamsList = (EDSParamsList) JSONSerializationToObject(EDSParamsListFile, new EDSParamsList().getClass()) ;
    		
        	if (cacheEDSParamsList == null)
        	{
    	        log.info("Creating EDS file as it doesn't exist yet:" + EDSParamsListFile);
        		cacheEDSParamsList = new EDSParamsList()  ;
        		
               	serializeObjectToJSON(cacheEDSParamsList, EDSParamsListFile) ;
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
			
	        object = mapper.readValue(new File(filePath), objectClass);
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
	public synchronized void serializeObjectToJSON(Object object, String filePath) throws ExtDataSourcesException
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

	/**
	 * Import data that can be handled by LDClient
	 * 	So far only "linked data" is tested, that is a resource retrieved according to linked data principles
	 * 
	 * LDClient retrieves data in a temporary store, than data is queried from the result
	 * and imported in Marmotta using the ImportClient
	 * 
     * @param marmottaURL ther marmottaServer URL, needed to call ImportClient
     * @param EDSType the type of EDS, as "RDFFile" or "LinkedData"
     * This parameter is not really used as the LDClient.retrieveResource() handles automatically which provider/endpoint to call
     * depending on the URL
     * @param url the url of the Linked Data resource to upload, for instance "http://dbpedia.org/resource/Martigny"
     * @param context the context (named graph) in which the data will be saved
     * @throws ExtDataSourcesException with an error message
     * @return a string with a validation message
     */
	@Override
	public String importWithLDClient(String marmottaURL, String headerAuth, String EDSType, String url, String context)  throws ExtDataSourcesException
	{
        log.debug("importLDResource:{} -> {}", url, context);
        
        long importedTriplesCount = 0 ;
        
        // create a LDClient with default configuration
        // this will include default providers, as "Linked Data" which is the one we need so far
		LDClient ldclient = new LDClient();

		Set<DataProvider> providers = ldclient.getDataProviders() ;
		// pour linked data: "Linked Data"
		
		/*
        for(DataProvider provider : providers) {
            System.out.println("- LDClient Provider a: " + provider.getName());
        }*/
			
        /*
         * A ping on a linked data resource will fail, don't do it
		if (!ldclient.ping(url))
			throw new ExtDataSourcesException("The requested Linked Data resource can't be accessed: "+ url);
		*/
        
        RepositoryConnection connection = null ;
        
		try {
			// retrieveResource will look for a provider for that resource
			ClientResponse result = ldclient.retrieveResource(url);
			
	        connection = ModelCommons.asRepository(result.getData()).getConnection();
	        connection.begin();
	        
	        importedTriplesCount = connection.size();

	        // example to show how a SPARQL could be run on the data
	        // this would be useful for overLOD futur options as
	        // - validating the content
	        // - importing only part of the data
	        /*
	        String sparql = "select * where {?s ?p ?o}" ;
	        GraphQuery testLabel = connection.prepareGraphQuery(QueryLanguage.SPARQL, sparql);
	        GraphQueryResult res = testLabel.evaluate();
	        */
	        
	        // Get the content
	        RDFFormat format = RDFFormat.TURTLE ;
            StringWriter out = new StringWriter();
            connection.export(Rio.createWriter(format, out));
            
            // I intended to use Marmotta ImportClient to upload the data
            // but that was not working with a secured Marmotta: 401 from the ImportClient
            // I tried specifying user/pwd in the configuration: new ClientConfiguration(marmottaURL, "admin", "pass123");
            // but with no success - still getting a 401 (is the format correct) ?
            // I did some test by taking user/pwd into account in HTTPUtil.createPost() and that seemed ok
            // see commented code there
            // But so far I keep my own uploadDataset() here
            
            ClientConfiguration configuration = new ClientConfiguration(marmottaURL);
            /*
            // decode headerAuth to set user/pwd
            if (headerAuth != null && !headerAuth.equals(""))             	
            {
            	log.debug("headerAuth specified, decoding: " + headerAuth); // "Basic YWRtaW46cGFzczEyMw=="
            	if (headerAuth.toLowerCase().startsWith("basic "))
            		headerAuth = headerAuth.substring(6) ;
            	log.debug("headerAuth after removing 'basic ': " + headerAuth);
	        	byte[] authDecodedByte= Base64.decodeBase64(headerAuth) ;
	        	// DatatypeConverter.parseBase64Binary(headerAuth) ;
	        	// 
	        	log.debug("debug test: " + new String(Base64.decodeBase64("YWRtaW46cGFzczEyMw==")));
	        	String authDecoded = new String(authDecodedByte) ;
	        	log.debug("decoded auth: "+ authDecoded) ;
	        	int colonPos = authDecoded.indexOf(":") ;
	        	if (colonPos > 0)
	        		{
	        		String user = authDecoded.substring(0, colonPos) ;
	        		String pwd =  authDecoded.substring(colonPos+1) ;
	        		log.debug("Creating ClientConfiguration with user:pwd: '" + user + ":" + pwd+"'") ;
	        		
	        		configuration = new ClientConfiguration(marmottaURL, user, pwd);
	        		}
            }
            else
            	log.debug("no headerAuth specified");
            	
            ///ClientConfiguration configuration = new ClientConfiguration(marmottaURL, "admin", "pass123");
            
            ImportClient importClient = new ImportClient(configuration);
            
            */

            configuration.setMarmottaContext(context);

            byte[] barray = out.toString().getBytes("UTF-8");
            InputStream is = new ByteArrayInputStream(barray);
            
            uploadDataset(headerAuth, configuration, is, format.getDefaultMIMEType());
        	//importClient.uploadDataset(is, format.getDefaultMIMEType());
            // Another method overload exists that accept the string, and it will handle the transformation to InputStream
			// importClient.uploadDataset(out.toString(), format.getDefaultMIMEType());

		} catch (Exception e) {
			throw new ExtDataSourcesException(e.getMessage());
		} finally {
			if (connection != null)
			{
				try {
					connection.commit();
			        connection.close();
				} catch (RepositoryException e) {
					throw new ExtDataSourcesException("Exception closing the LDClient connection: " + e.getMessage());				}
			}
			
			ldclient.shutdown();
        }
		
		return "Data successfully imported using the LDClient (" + importedTriplesCount + " triples)" ;
	}
	
	/*
	 * This code is a copy of ImportClient.uploadDataset()
	 * in order to handle the HTTP authentication
	 * The modification was to add:
	 *    post.setHeader("Authorization", headerAuth);
	 *    
	 *  Another modification was to throw exceptions from ResponseHandler
	 *  	otherwise errors where not sent back to the user, but only to the logs
	 */
    public void uploadDataset(String headerAuth, ClientConfiguration config, final InputStream in, final String mimeType) throws IOException, ExtDataSourcesException {
        //Preconditions.checkArgument(acceptableTypes.contains(mimeType));
    	
        HttpClient httpClient = HTTPUtil.createClient(config);

        HttpPost post = HTTPUtil.createPost(URL_UPLOAD_SERVICE, config);
        post.setHeader("Content-Type", mimeType);
        
        if (headerAuth != null && !headerAuth.equals("")) // null if marmotta is configured with no security option
        	post.setHeader("Authorization", headerAuth);
        
        ContentProducer cp = new ContentProducer() {
            @Override
            public void writeTo(OutputStream outstream) throws IOException {
                ByteStreams.copy(in,outstream);
            }
        };
        post.setEntity(new EntityTemplate(cp));
        
        ResponseHandler<Boolean> handler = new ResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                EntityUtils.consume(response.getEntity());
                switch(response.getStatusLine().getStatusCode()) {
                    case 200:
                        log.debug("dataset uploaded updated successfully");
                        return true;
                    case 412:
                        log.error("mime type {} not acceptable by import service",mimeType);
                        throw new IOException("mime type not acceptable by import service: "+ mimeType) ;
                        // return false;
                    default:
                        log.error("error uploading dataset: {} {}",new Object[] {response.getStatusLine().getStatusCode(),response.getStatusLine().getReasonPhrase()});
                        throw new IOException("error uploading: "+ response.getStatusLine().getStatusCode());
                        // return false;
                }
            }
        };

        try {
            httpClient.execute(post, handler);
        } catch(IOException ex) {
            post.abort();
            throw ex;
        } finally {
            post.releaseConnection();
        }

    }
}
