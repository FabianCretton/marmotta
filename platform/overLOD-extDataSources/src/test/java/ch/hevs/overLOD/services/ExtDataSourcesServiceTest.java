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
package ch.hevs.overLOD.services;

import java.util.Set;
import java.util.TreeMap;

import org.apache.marmotta.ldclient.api.endpoint.Endpoint;
import org.apache.marmotta.ldclient.api.provider.DataProvider;
import org.apache.marmotta.ldclient.services.ldclient.LDClient;
import org.apache.marmotta.platform.core.test.base.EmbeddedMarmotta;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.hevs.overLOD.extDataSources.EDSParams.EDSParams;
import ch.hevs.overLOD.extDataSources.api.ExtDataSources;
import ch.hevs.overLOD.extDataSources.exceptions.ExtDataSourcesException;

public class ExtDataSourcesServiceTest {

    private static EmbeddedMarmotta marmotta;
    private static ExtDataSources myService;

    @BeforeClass
    public static void setUp() {
        marmotta = new EmbeddedMarmotta();
        myService = marmotta.getService(ExtDataSources.class);
    }

    @Test
    public void testGetEDSParamsList() {
    	Object obj = null ;
    	
    	try {
			obj = myService.getEDSParamsList() ;
		} catch (ExtDataSourcesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	Assert.assertTrue(obj instanceof TreeMap);
    	// TreeMap<String,EDSParams>
    }
    
    @Test
    public void testLDClientProviders()
    {
    	// ensure that the "RDF File" endpoint is loaded in Marmotta and will handle RDF files import
		LDClient ldclient = new LDClient();

		Endpoint endpoint =ldclient.getEndpoint("http://foo.rdf") ;
		Assert.assertTrue(endpoint.getName().equals("RDF File")) ;
		
		endpoint =ldclient.getEndpoint("http://foo.ttl") ;
		Assert.assertTrue(endpoint.getName().equals("RDF File")) ;
		
		endpoint =ldclient.getEndpoint("http://foo.n3") ;
		Assert.assertTrue(endpoint.getName().equals("RDF File")) ;
    }

    @AfterClass
    public static void tearDown() {
        marmotta.shutdown();
    }

}
