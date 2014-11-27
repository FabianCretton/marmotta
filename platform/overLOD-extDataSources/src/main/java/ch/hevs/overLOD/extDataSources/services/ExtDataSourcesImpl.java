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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.marmotta.platform.core.events.ConfigurationChangedEvent;
import org.openrdf.model.Statement;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.QueryResults;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;
import org.topbraid.spin.constraints.ConstraintViolation;
import org.topbraid.spin.constraints.SPINConstraints;
import org.topbraid.spin.system.SPINLabels;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;

import ch.hevs.overLOD.extDataSources.EDSParams.EDSParams;
import ch.hevs.overLOD.extDataSources.EDSParams.EDSParamsList;
import ch.hevs.overLOD.extDataSources.api.ExtDataSources;
import ch.hevs.overLOD.extDataSources.exceptions.ExtDataSourcesException;

/**
 * Default Implementation of {@link ExtDataSources}
 * 
 * Two different information are handled for one EDS:
 * - the parameters for that EDS
 * 		The list of External Data Sources parameters (EDSParams) is read and written to a .json file in: 
 * 		configurationService.getHome() + "/EDS/EDSParamsList.json"
 * - the content of the data, loaded in a standard Marmotta's context
 * 
 * For more information about the overriden methods, see "ExtDataSources"
 * See ExtDataSourcesWebService comment for a general introduction to the EDS module
 * 
 * @author Fabian Cretton, HES-SO OverLOD surfer project
 * http://www.hevs.ch/fr/rad-instituts/institut-informatique-de-gestion/projets/overlod-surfer-6349
 */
@ApplicationScoped
public class ExtDataSourcesImpl implements ExtDataSources {

	@Inject
	private Logger log;

	@Inject
	private ConfigurationService configurationService;

	private EDSParamsList cacheEDSParamsList = null;

	private static final String URL_UPLOAD_SERVICE = "/import/upload";

	// file references initialized in initialize() method
	String marmottaHomeFolder = null;
	String EDSParamsListFile = null;
	String spinTemplateFile = null;
	String spinConstraintsFolder = null;
	String EDSFiltersFolder = null;

	Model modelSpinTemplates = null;

    // From the module configuration
	boolean spinTemplateFileReload = false ; // will also be set to true if the name of the template file was changed in the config
	String spinTemplateFileName = null ;
	
	@PostConstruct
	public void initialize() {
		log.debug("ExtDataSourcesImpl initialize() - loading EDSParams list from disk");

		// file/folder path initialization
		marmottaHomeFolder = configurationService.getHome();
		EDSParamsListFile = marmottaHomeFolder + "\\EDS\\EDSParamsList.json";
		spinConstraintsFolder = marmottaHomeFolder + "\\EDS\\SPIN\\Constraints\\";
		EDSFiltersFolder = marmottaHomeFolder + "\\EDS\\EDSFilters\\";

		// Initialize SPIN system functions and templates
		SPINModuleRegistry.get().init();

		try {
			loadEDSParamsListFromDisk();
		} catch (ExtDataSourcesException e) {
			log.error("ExtDataSourcesImpl.initialize(), loadEDSParamsListFromDisk() exception: " + e.getMessage());
		}

		readConfiguration() ;
    }
    
    /**
     * Read configuration
     * And load the template file from disk if:
     * - never loaded so far (spinTemplateFileName == null)
     * - force reload from configuration
     * - the filename has changed
     */
	public void readConfiguration()
    {
		String newSpinTemplateFileName = configurationService.getStringConfiguration("EDS.dataValidation_SPINTemplateFile"); 
    	spinTemplateFileReload = configurationService.getBooleanConfiguration("EDS.dataValidation_SPINTemplateFile_ForceReload");

    	log.debug("EDS configuration 'dataValidation_SPINTemplateFile_ForceReload': {}", spinTemplateFileReload) ;
    	log.debug("EDS configuration 'dataValidation_SPINTemplateFile': {}", newSpinTemplateFileName) ;

    	if (spinTemplateFileReload || spinTemplateFileName == null || !newSpinTemplateFileName.equals(spinTemplateFileName))
    	{
        	spinTemplateFileName = newSpinTemplateFileName ;
        	spinTemplateFile = marmottaHomeFolder + "\\EDS\\SPIN\\Templates\\" + newSpinTemplateFileName ;

    		try {
    			loadModelSpinTemplatesFromDisk();
    			if (spinTemplateFileReload)
    				setConfSpinTemplateFileForceReload(false); // then change back the value to false

    		} catch (ExtDataSourcesException e) {
    			log.error("ExtDataSourcesImpl.initialize(), loadModelSpinTemplatesFromDisk() exception: " + e.getMessage());
    		}
    	}
    }

    /**
     * Detect a change in the EDS configuration, and if so reload the configuration values
     * @param event
     */
    public void configurationEventHandler(@Observes ConfigurationChangedEvent event) 
    {
   		if (event.containsChangedKeyWithPrefix("EDS.dataValidation")) {
   	    	log.debug("EDS: Reloading configuration - change detected");
   			readConfiguration() ;
    	}
   	}
    
    /**
     * Change the configuration value for "EDS.dataValidation_SPINTemplateFile_ForceReload"
     * @param value true/false
     */
	public void setConfSpinTemplateFileForceReload(boolean value) {
		configurationService.setBooleanConfiguration("EDS.dataValidation_SPINTemplateFile_ForceReload", value);
	}

	/*
	 * (non-Javadoc)
	 * @see ch.hevs.overLOD.extDataSources.api.ExtDataSources#getEDSParamsList()
	 */
	@Override
	public TreeMap<String, EDSParams> getEDSParamsList() throws ExtDataSourcesException {
		return cacheEDSParamsList.getList();
	}

	/*
	 * (non-Javadoc)
	 * @see ch.hevs.overLOD.extDataSources.api.ExtDataSources#getDataFiltersList()
	 */
	@Override
    public ArrayList<String> getDataFiltersList() throws ExtDataSourcesException{
		return getFolderFilesList(EDSFiltersFolder) ;
	}
	
	/*
	 * (non-Javadoc)
	 * @see ch.hevs.overLOD.extDataSources.api.ExtDataSources#getDataValidatorsList()
	 */
	@Override
    public ArrayList<String> getDataValidatorsList() throws ExtDataSourcesException
    {
		return getFolderFilesList(spinConstraintsFolder) ;
    }

	/**
	 * Get the list of files found in a folder
	 * @param folderPath the folder to be listed
	 * @return
	 */
	public ArrayList<String> getFolderFilesList(String folderPath)
	{
    	ArrayList<String> filesList = new ArrayList<String>() ;

        File folder = new File(folderPath);

        File[] listOfFiles = folder.listFiles(); 
       
        for (int i = 0; i < listOfFiles.length; i++) 
			 if (listOfFiles[i].isFile()) 
				 filesList.add(listOfFiles[i].getName()) ;

    	return filesList ;
	}
	
	/*
	 * (non-Javadoc)
	 * @see ch.hevs.overLOD.extDataSources.api.ExtDataSources#addEDSParams(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String addEDSParams(String EDSType, String contentType, String url, String context, String timeStamp, String filterFileName, String validationFileName) throws ExtDataSourcesException {
		log.debug("saving EDSParams EDSType:{}, contentType:{} url:{} context:{} timeStamp:{} filterFileName:{} validationFileName:{}", EDSType, contentType, url, context, timeStamp, filterFileName, validationFileName);

		// Ensure the context for the new EDS don't exist yet
		if (cacheEDSParamsList.exists(context))
			throw new ExtDataSourcesException("A new EDS can't be added with a context already used for another one");

		EDSParams aEDSParams = new EDSParams(EDSType, contentType, url, context, timeStamp, filterFileName, validationFileName);

		synchronized (this) // to avoid concurrent access to the object and while saving the serialization
		{
			cacheEDSParamsList.addEDSParams(aEDSParams);

			serializeObjectToJSON(cacheEDSParamsList, EDSParamsListFile);
			log.debug("EDSParams saved!", EDSType, url, context);
		}

		return "Parameters saved for External Data Source: " + context;
	}

	/*
	 * (non-Javadoc)
	 * @see ch.hevs.overLOD.extDataSources.api.ExtDataSources#setEDSParamsTimeStamp(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean setEDSParamsTimeStamp(String context, String timeStamp) throws ExtDataSourcesException {
		log.debug("update timeStep of EDSParams identified by context:{}", context);

		if (cacheEDSParamsList.setEDSParamsTimeStamp(context, timeStamp)) {
			serializeObjectToJSON(cacheEDSParamsList, EDSParamsListFile);
			log.debug("saving EDSParams list after a timeStamp update...done!");
			return true;
		} else {
			log.debug("can't set the EDS timeStamp");
			return false;
		}

	}

	/*
	 * (non-Javadoc)
	 * @see ch.hevs.overLOD.extDataSources.api.ExtDataSources#getEDSParams(java.lang.String)
	 */
	@Override
	public EDSParams getEDSParams(String context) {
		return cacheEDSParamsList.get(context);
	}

	/*
	 * (non-Javadoc)
	 * @see ch.hevs.overLOD.extDataSources.api.ExtDataSources#deleteEDSParams(java.lang.String)
	 */
	@Override
	public String deleteEDSParams(String context) throws ExtDataSourcesException {
		log.debug("delete EDSParams identified by context:{}", context);

		// to avoid concurrent access to the object and while saving the serialization 
		synchronized (this) 
		{
			if (!cacheEDSParamsList.deleteEDSParams(context)) // context not found in the
				throw new ExtDataSourcesException("Context '" + context + "' not found in the list of EDS");

			serializeObjectToJSON(cacheEDSParamsList, EDSParamsListFile);
		}

		log.debug("saving EDSParams list after a delete...done!");

		return "EDS deleted from External Data Sources list: " + context;
	}

	/**
	 * Load the EDS list from the json serialization on disk The file path and
	 * name is stored in the class member EDSParamsListFile
	 * 
	 * If the file don't exist yet, then create it (could happen on the first
	 * run of the module)
	 * 
	 * @throws ExtDataSourcesException Exception with an error message, in case
	 *         the EDS list can't be loaded from disk
	 */
	private void loadEDSParamsListFromDisk() throws ExtDataSourcesException {
		if (cacheEDSParamsList == null) {
			cacheEDSParamsList = (EDSParamsList) JSONSerializationToObject(EDSParamsListFile, new EDSParamsList().getClass());

			if (cacheEDSParamsList == null) {
				log.info("Creating EDS file as it doesn't exist yet:" + EDSParamsListFile);
				cacheEDSParamsList = new EDSParamsList();

				serializeObjectToJSON(cacheEDSParamsList, EDSParamsListFile);
			}
		}
	}

	/**
	 * Read the SPIN template file from disk
	 * 	spinTemplateFile is a class member, set by ReadConfiguration()
	 * @throws ExtDataSourcesException
	 */
	private void loadModelSpinTemplatesFromDisk() throws ExtDataSourcesException {
		log.debug("loadModelSpinTemplatesFromDisk: {}", spinTemplateFile);

		// if the model did already exist, it is discarded and recreated
		modelSpinTemplates = ModelFactory.createDefaultModel();
		try {
			FileInputStream inputStream = new FileInputStream(spinTemplateFile);
			modelSpinTemplates.read(inputStream, null, FileUtils.langTurtle);
			inputStream.close();
		} catch (Exception e) {
			modelSpinTemplates = null;
			throw new ExtDataSourcesException("EDS Constraints template file can't be loaded from disk: " + e.getMessage());
		}

		log.debug("EDS Constraints template file size: {}", modelSpinTemplates.size());
	}

	/**
	 * Read a JSON serialization from a file
	 * 
	 * @param filePath
	 * @param objectForClass an instance of the object that is expected
	 * @return the object, otherwise null if io exception
	 */
	private Object JSONSerializationToObject(String filePath, Class objectClass)
	{
		Object object = null;
		ObjectMapper mapper = new ObjectMapper();

		try {
			log.debug("JSONSerialization2Object - loading: {}", filePath);

			object = mapper.readValue(new File(filePath), objectClass);
		} catch (Exception e1) {
			log.error("JSONSerialization2Object :" + e1.getMessage());
			object = null;
		}

		return object;
	}

	/**
	 * Use Jackson to export an object to a file
	 * 
	 * @param object
	 * @param filePath
	 * @throws ExtDataSourcesException
	 */
	public synchronized void serializeObjectToJSON(Object object, String filePath) throws ExtDataSourcesException {
		ObjectMapper mapper = new ObjectMapper();

		try {
			log.debug("serializeObject2JSON - saving.");
			// convert object to json string, and save to a file
			mapper.writeValue(new File(filePath), object);
		} catch (Exception e) {
			log.error("serializeObject2JSON :" + e.getMessage());
			throw new ExtDataSourcesException(e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see ch.hevs.overLOD.extDataSources.api.ExtDataSources#importWithLDClient(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String importWithLDClient(String marmottaURL, String headerAuth, String EDSType, String url, String context, String filterFileName, String validationFileName) throws ExtDataSourcesException {
		log.debug("importWithLDClient:{} -> {}", url, context);

		long importedTriplesCount = 0;

		// create a LDClient with default configuration
		// this will include default providers, as "Linked Data", as well as
		// providers we created as RDFFile
		LDClient ldclient = new LDClient();

		Set<DataProvider> providers = ldclient.getDataProviders();

		RepositoryConnection connection = null;

		try {
			ClientResponse result = ldclient.retrieveResource(url); // look for a provider for that resource

			connection = ModelCommons.asRepository(result.getData()).getConnection();
			connection.begin();

			RDFFormat format = RDFFormat.TURTLE;
			StringWriter stringWriter = new StringWriter();

			if (filterFileName != null) {
				log.debug("Import with filter: {}", filterFileName);
				RDFWriter rdfWriter = Rio.createWriter(format, stringWriter);

				String sparqlQuery = readFilterQuery(filterFileName); 
																		
				GraphQuery constructQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, sparqlQuery);
				GraphQueryResult sparqlResult = constructQuery.evaluate();
				// A model is build just to get the size of the resulting triples
				// To be more efficient, we could check the context size() after the import in Marmotta
				// and then replace those lines by: connection.prepareGraphQuery(QueryLanguage.SPARQL, sparql).evaluate(rdfWriter);
				importedTriplesCount = QueryResults.asModel(sparqlResult).size();
				
				log.debug("Size of the filtered data: " + importedTriplesCount) ; 
				
				// Write the results to the writer
				constructQuery.evaluate(rdfWriter); // to the underlying stringWriter
			} else {
				log.debug("Import the full data with no filter");
				importedTriplesCount = connection.size();

				// Get the retrieved content
				connection.export(Rio.createWriter(format, stringWriter));
			}

			byte[] barray = stringWriter.toString().getBytes("UTF-8");
			InputStream is = new ByteArrayInputStream(barray);

			// spin constraint validation on the filtered data
			if (validationFileName != null) {
				dataConstraintsValidation(is, FileUtils.langTurtle, validationFileName); // SPIN is based on Jena, so pass a Jena parameter (FileUtils) for the data format
				is.reset() ; // after reading the input stream, come back at the beginning (supported by ByteArrayInputStream)
			} else
				log.debug("Import with no constraint validation");

			
			ClientConfiguration configuration = new ClientConfiguration(marmottaURL);
			configuration.setMarmottaContext(context);

			// Import the data
			uploadDataset(headerAuth, configuration, is, format.getDefaultMIMEType());
			is.close();
		} catch (Exception e) {
			log.error("importWithLDClient exception: " + e.getMessage());
			throw new ExtDataSourcesException(e.getMessage());
		} finally {
			if (connection != null) {
				try {
					connection.commit();
					connection.close();
				} catch (RepositoryException e) {
					throw new ExtDataSourcesException("Exception closing the LDClient connection: " + e.getMessage());
				}
			}

			ldclient.shutdown();
		}

		return "Data successfully imported using the LDClient (" + importedTriplesCount + " triples)";
	}

	/**
	 * This method is not used yet, but could serve to extract the user/pwd
	 * from the authentication parameter, and set those value to a clientConfiguration
	 */
	public void extractUserPwdFromAuth(String headerAuth, ClientConfiguration configurationClient)
	{
		if (headerAuth != null && !headerAuth.equals("")) 
		{
		  log.debug("headerAuth specified, decoding: " + headerAuth); // example: "Basic YWRtaW46cGFzczEyMw=="
		 
		 // could there be any other value here ? for instance if Marmotta is set to security.method "DIGEST"
		 if (headerAuth.toLowerCase().startsWith("basic ")) 
		  	headerAuth = headerAuth.substring(6) ;
		 
		  log.debug("headerAuth after removing 'basic ': " + headerAuth);
		  
		  byte[] authDecodedByte= Base64.decodeBase64(headerAuth) ;
		  DatatypeConverter.parseBase64Binary(headerAuth) ;
		  
		  // log.debug("debug test: " + new String(Base64.decodeBase64("YWRtaW46cGFzczEyMw=="))); 
		  String authDecoded = new String(authDecodedByte) ;
		  
		  int colonPos = authDecoded.indexOf(":") ;
		  if (colonPos > 0) {
	       String user = authDecoded.substring(0, colonPos) ; 
		   String pwd = authDecoded.substring(colonPos+1) ; 
		   log.debug("Updating ClientConfiguration with user:pwd: '" + user + ":" + pwd+"'") ;
		  
		   configurationClient.setMarmottaUser(user);
		   configurationClient.setMarmottaPassword(pwd);
		  }
		}
		else log.debug("no headerAuth specified");
		
	}
	/**
	 * Get a Filter query from disk file
	 * 
	 * @param fileName the full name of the file (including extension)
	 * @return the content of the file, a SPARQL query
	 * @throws IOException
	 */
	public String readFilterQuery(String fileName) throws IOException {
		String query = null;
		String queryFile = EDSFiltersFolder + fileName;

		// Read the .sparql file
		FileInputStream inputStream = null;

		try {
			inputStream = new FileInputStream(queryFile);
			query = IOUtils.toString(inputStream);
		} catch (IOException e) {
			log.error("Reading filter '" + queryFile + "' exception:" + e.getMessage());
			throw (e);
		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				log.error("Closing filter file '" + queryFile + "' exception:" + e.getMessage());
			}
		}

		return query;
	}

	/**
	 * Check one rdf data set (passed as InputStream) againts a SPIN constraints file
	 * If constraints violation are detected, an Exception is raised with
	 * all violations in the text message Based on SPIN, which is based on Jena
	 * 
	 * The constraint checking is based on 3 'customized' files (and then other
	 * SPIN files): 
	 * - spinTemplateFile: a set of SPIN templates, defined once
	 * and for all Saved in marmotta-home\EDS\SPIN\Templates\
	 * the file name is a module's parameter that can be set by the administrator 
	 * - constraintsFileName: the constraints definition based on the
	 * "spinTemplateFile", and defined for a specific class (passed as
	 * parameter) 
	 * Those files are found in marmotta-home\EDS\SPIN\Constraints
	 * and can be selected by the user when adding an EDS 
	 * - rdfDataInputStream: the rdfGraph to be checked (passed as parameter)
	 * 
	 * All those files are joined in a common model, to run the SPIN constraints
	 * checking
	 * 
	 * This method is based on SPIN examples files
	 * 
	 * @param rdfDataInputStream the rdfGraph to be checked, as an InputStream
	 * @param rdfDataFormat jena RDF data format taken from FileUtils, as
	 *        FileUtils.langTurtle
	 * @param constraintsFileName name of a file containing the constraints,
	 *        available in "marmotta-home\EDS\SPIN\Constraints\" folder
	 * @throws ExtDataSourcesException if constraints violation are detected, an
	 *         Exception is raised with all violations in the text message. An
	 *         exception is also raised of other errors
	 */

	public void dataConstraintsValidation(InputStream rdfDataInputStream, String rdfDataFormat, String constraintsFileName) throws ExtDataSourcesException {
		log.debug("data constraints validation with constraintsFile: {}", constraintsFileName);

		if (modelSpinTemplates == null)
			throw new ExtDataSourcesException("modelSpinTemplates wasn't loaded properly, data constraints validation can't be performed");

		// Load Constraints/Shapes
		Model modelConstraints = ModelFactory.createDefaultModel();

		String spinConstraintsFile = null;
		try {
			FileInputStream inputStream = new FileInputStream(spinConstraintsFolder + constraintsFileName);
			modelConstraints.read(inputStream, null, FileUtils.langTurtle);
			inputStream.close();
		} catch (Exception e) {
			throw new ExtDataSourcesException("Data validation failed - unable to load the constraints file: " + spinConstraintsFolder
					+ constraintsFileName);
		}

		if (modelConstraints.size() == 0)
			throw new ExtDataSourcesException("Data validation failed - the loaded constraints file model has a size of 0: " + spinConstraintsFolder
					+ constraintsFileName);

		// Model for the data to be tested
		Model modelDataToBeTested = ModelFactory.createDefaultModel();

		try {
			modelDataToBeTested.read(rdfDataInputStream, null, rdfDataFormat);
		} catch (Exception e) {
			throw new ExtDataSourcesException("Data validation failed - unable to load the rdf data in the model: " + spinConstraintsFolder
					+ constraintsFileName);
		}

		if (modelConstraints.size() == 0)
			throw new ExtDataSourcesException("Data validation failed - the loaded rdf data model has a size of 0: " + spinConstraintsFolder
					+ constraintsFileName);

		// Create Model for the base graph with its imports
		MultiUnion union = new MultiUnion(new Graph[] { modelDataToBeTested.getGraph(), modelSpinTemplates.getGraph(), modelConstraints.getGraph(),
				SPL.getModel().getGraph(), SPIN.getModel().getGraph(), SP.getModel().getGraph() });

		Model unionModel = ModelFactory.createModelForGraph(union);

		// Register locally defined functions (none exist, but may in the
		// future)
		SPINModuleRegistry.get().registerAll(unionModel, null);

		// Run all constraints
		List<ConstraintViolation> cvs = SPINConstraints.check(unionModel, null);

		// Run constraints on a single instance only
		if (cvs.size() > 0) {
			String violationsList = null;

			log.debug("Detected constraint violations:");
			violationsList = "Detected constraint violations:\n";
			for (ConstraintViolation cv : cvs) {
				violationsList += "* " + cv.getMessage() + "\n";
				log.debug(" - at " + SPINLabels.get().getLabel(cv.getRoot()) + ": " + cv.getMessage());
			}

			throw new ExtDataSourcesException(violationsList);
		} else
			log.debug("No Constraint violations detected");
	}

	/*
	 * upload a dataset in the store, from an InputStream
	 *  
	 * This code is a copy of ImportClient.uploadDataset() in order to handle
	 * the HTTP authentication The modification was to add:
	 * post.setHeader("Authorization", headerAuth);
	 * 
	 * Another modification was to throw exceptions from ResponseHandler
	 * otherwise errors where not sent back to the user, but only to the logs
	 * 
	 * on 31.10.2014, original marmotta code HTTPUtil.getPost() was updated to
	 * handle http authentication based on ClientConfiguration's user/pwd.
	 * However no exception is thrown so far, so this personalized method is still useful
	 */
	public void uploadDataset(String headerAuth, ClientConfiguration config, final InputStream in, final String mimeType) throws IOException, ExtDataSourcesException {
		HttpClient httpClient = HTTPUtil.createClient(config);

		HttpPost post = null;
		try {
			post = HTTPUtil.createPost(URL_UPLOAD_SERVICE, config);
		} catch (URISyntaxException e) {
			throw new ExtDataSourcesException("HTTPUtil.createPost() exception: " + e.getMessage());
		}

		post.setHeader("Content-Type", mimeType);

		// null if marmotta is configured with no security option
		if (headerAuth != null && !headerAuth.equals("")) 
			post.setHeader("Authorization", headerAuth);

		ContentProducer cp = new ContentProducer() {
			@Override
			public void writeTo(OutputStream outstream) throws IOException {
				ByteStreams.copy(in, outstream);
			}
		};
		post.setEntity(new EntityTemplate(cp));

		ResponseHandler<Boolean> handler = new ResponseHandler<Boolean>() {
			@Override
			public Boolean handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
				EntityUtils.consume(response.getEntity());
				switch (response.getStatusLine().getStatusCode()) {
				case 200:
					log.debug("dataset uploaded successfully");
					return true;
				case 412:
					log.error("mime type {} not acceptable by import service", mimeType);
					throw new IOException("mime type not acceptable by import service: " + mimeType);
				default:
					log.error("error uploading dataset: {} {}", new Object[] { response.getStatusLine().getStatusCode(),
							response.getStatusLine().getReasonPhrase() });
					throw new IOException("error uploading: " + response.getStatusLine().getStatusCode());
				}
			}
		};

		try {
			httpClient.execute(post, handler);
		} catch (IOException ex) {
			post.abort();
			throw ex;
		} finally {
			post.releaseConnection();
		}
	}
}
