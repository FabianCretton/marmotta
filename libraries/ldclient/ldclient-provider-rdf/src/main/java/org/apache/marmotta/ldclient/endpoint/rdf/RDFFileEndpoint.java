/**
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
package org.apache.marmotta.ldclient.endpoint.rdf;

import org.apache.marmotta.commons.http.ContentType;
import org.apache.marmotta.ldclient.api.endpoint.Endpoint;

/**
 * A specialised default endpoint configuration for RDF files as:
 * http://www.w3.org/People/Berners-Lee/card.rdf
 * 
 * the regex match urls (starts with 'http(s):' that ends with a file extension
 * of ".rdf", ".n3" or ".ttl"
 * <p/>
 * @author Fabian Cretton, HES-SO OverLOD surfer project
 */
public class RDFFileEndpoint extends Endpoint {

    public RDFFileEndpoint() {
        super("RDF File", "RDF File", "(^http(s?):.*(\\.(?i)(rdf|n3|ttl))$)", null, 86400L);
        setPriority(PRIORITY_HIGH);
        addContentType(new ContentType("application", "rdf+xml", 1.0));
        addContentType(new ContentType("text", "turtle", 1.0));
        addContentType(new ContentType("text", "n3", 1.0));
        addContentType(new ContentType("text", "plain", 0.5)); // it seems that some .ttl files are returned as plain text
        //addContentType(new ContentType("text", "rdf+n3", 0.8));
        //addContentType(new ContentType("application", "ld+json", 0.5));
        //addContentType(new ContentType("application","rdf+json",0.8));
    }
}
