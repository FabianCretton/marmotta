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

import org.apache.marmotta.platform.core.test.base.EmbeddedMarmotta;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;

import ch.hevs.overLOD.dataView.api.DataView;

public class DataViewServiceTest {

    private static EmbeddedMarmotta marmotta;
    private static DataView myService;

    @BeforeClass
    public static void setUp() {
        marmotta = new EmbeddedMarmotta();
        myService = marmotta.getService(DataView.class);
    }

    @AfterClass
    public static void tearDown() {
        marmotta.shutdown();
    }

}
