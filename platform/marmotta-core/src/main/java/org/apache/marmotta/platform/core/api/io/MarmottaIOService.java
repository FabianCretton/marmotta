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
package org.apache.marmotta.platform.core.api.io;

import org.openrdf.rio.RDFFormat;

import java.util.List;

/**
 * Manages RDF parsers and serializers
 *
 * User: Thomas Kurz
 * Date: 18.02.11
 * Time: 10:37
 */
public interface MarmottaIOService {

	/**
	 * returns a list of all mimetypes which can be parsed by implemented parsers
	 * @return
	 */
	public List<String> getAcceptTypes();

	/**
	 * returns a list of all mimetypes which can be produced by implemented serializers
	 * @return
	 */
	public List<String> getProducedTypes();

	/**
	 * returns a serializer for a given mimetype; null if no serializer defined
	 * @param mimetype
	 * @return
	 */
	public RDFFormat getSerializer(String mimetype);
	/**
	 * returns a parser for a given mimetype; null if no parser defined
	 * @param mimetype
	 * @return
	 */
	public RDFFormat getParser(String mimetype);

}
