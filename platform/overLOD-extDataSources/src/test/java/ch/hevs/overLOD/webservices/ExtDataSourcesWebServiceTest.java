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

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import ch.hevs.overLOD.extDataSources.webservices.ExtDataSourcesWebService;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.DecoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.http.ContentType;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Ignore;
import org.junit.Test;
import org.apache.marmotta.platform.core.test.base.JettyMarmotta;

public class ExtDataSourcesWebServiceTest {

    private static JettyMarmotta marmotta;

    @BeforeClass
    public static void beforeClass() {
        marmotta = new JettyMarmotta("/EDS-test", 9090, ExtDataSourcesWebService.class);

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 9090;
        RestAssured.basePath = "/EDS-test";
        RestAssured.config = RestAssuredConfig.newConfig().decoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-8"));
        
        /*
         * No test is currently done on the POST to  "/EDSParams"
         * as this would launch an import asynchronously
         * In the Marmotta-core, no test is done on the import functionality, certainly for the same reason
         * 
         * A test is done for a bad request
         */
    }

    @AfterClass
    public static void afterClass() {
        if (marmotta != null) {
            marmotta.shutdown();
        }
    }

    @Test
    public void testGetEDSParams(){
		  RestAssured.expect()
		  	  .statusCode(200)
		  	  .contentType("application/json") 
		  .when()
		      .get("/EDS/EDSParams");
    }
    
    @Test
    public void testPostEDSParamsWithBadURL(){
    	/* 
    	 * In the current implementation, I don't need to pass the other parameters (headers and query)
    	 * as the url is tested first
    	 */
        expect().
        	statusCode(502).
        when().
        	post("/EDS/EDSParams?url=http://foo.rdf");
        
/*        			
		  RestAssured.expect()
		  	  .statusCode(200)
		  	  .contentType("application/json") 
		  .when()
		      .get("/EDS/EDSParams");
		      */
    }
}
