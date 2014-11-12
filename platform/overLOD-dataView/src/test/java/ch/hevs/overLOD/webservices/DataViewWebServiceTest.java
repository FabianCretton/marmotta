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
package ch.hevs.overLOD.webservices;

import static org.hamcrest.Matchers.containsString;

import javax.inject.Inject;

import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.apache.marmotta.platform.core.services.config.ConfigurationServiceImpl;
import org.apache.marmotta.platform.core.test.base.JettyMarmotta;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.hevs.overLOD.dataView.webservices.DataViewWebService;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.DecoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.http.ContentType;

import org.apache.marmotta.platform.sparql.webservices.SparqlWebService ;

public class DataViewWebServiceTest {

    private static JettyMarmotta marmotta;

    //@Inject
    //private static ConfigurationService configurationService;

    @BeforeClass
    public static void beforeClass() {
        //marmotta = new JettyMarmotta("/dataView-test", 9090, DataViewWebService.class);
    	marmotta = new JettyMarmotta("/dataView-test", 9090, DataViewWebService.class, SparqlWebService.class);

    	/*
    	 * A trial to find out the marmotta-home directory
    	 * It will then be needed to create the \DataView sub-folder, and add a .sparql query file to it
    	 * I don't know yet if this could be done automatically with Maven or if it has to be done here
    	 * The following tests failed: no way to get the current home directory
    	 * Should I call the configuration web service and ask for 'marmotta.home', which is defined in the
    	 * configuration file ?
    	ConfigurationServiceImpl tst = new ConfigurationServiceImpl() ;
    	
    	System.out.println("fab tst av: "+ tst.getHome()) ;
    	if (configurationService == null)
        	System.out.println("configurationService == null") ;
    		
    	System.out.println("Marmotta home: "+ configurationService.getHome()) ;
    	System.out.println("fab tst ap") ;
    	*/
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 9090;
        RestAssured.basePath = "/dataView-test";
        RestAssured.config = RestAssuredConfig.newConfig().decoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-8"));
    }

    @AfterClass
    public static void afterClass() {
        if (marmotta != null) {
            marmotta.shutdown();
        }
    }

    @Test
    /*
     */
    public void getDataViewTest(){
        /*
         * The real getDataView() will read the query from a file found in the marmotta-home/DataView sub-folder
         * Currently, I didn't implement the creation of that folder and corresponding files that could be used
         * by the unit test (which executes in a temporary marmotta-home), hence this method for simple tests
         * 
         * This current test does call a service with a predefined SPARQL query
         */
    	
        RestAssured.given()
    	.expect()
	  	  .statusCode(200)
	  	  .contentType("application/json") 
	  .when()
	      .get("/dataView/test");

        /*
        RestAssured.given()
        	.param("viewName", "allTriplesLimit10")
        	.expect()
		  	  .statusCode(200)
		  	  .contentType("application/json") 
		  .when()
		      .get("/dataView");

		 // missing parameter 'viewName'
		RestAssured.expect()
		      .statusCode(500)
		  .when()
		      .get("/dataView");
		*/
    }    
}
