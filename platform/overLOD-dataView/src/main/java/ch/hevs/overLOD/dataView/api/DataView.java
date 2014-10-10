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
package ch.hevs.overLOD.dataView.api;

import java.util.ArrayList;
import java.util.TreeMap;

import ch.hevs.overLOD.dataView.exceptions.DataViewException;

public interface DataView {

    public ArrayList<String> getDataViewsList() throws DataViewException ;
	
    /**
     * Echoes <code>"Hello &lt;name&gt;"</code>.
     * 
     * @param name
     *            the name to echo
     * @return <code>"Hello &lt;name&gt;"</code>
     */
    public String helloWorld(String name);

}
