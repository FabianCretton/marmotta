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

/**
 * Creates a ExternalDataSources object which implements all methods below.
 *
 * Author: Fabian Cretton
 * @param url The basic URL where Marmotta runs
 * @param opts an options object (OPTIONAL)
 */
function ExtDataSources(url) {

    if( url==undefined) throw "url must be defined"; //test if url is defined
    if( url.lastIndexOf("/")==url.length-1) url=url.substring(0,url.length-1); //clean url
    //default options
    var options = {
        extDataSourcesPath : {
            path : "/EDS" //path to config webservices
        },
        sparql : {
            path : "/sparql"
        }
    }

    //create http stub
    var HTTP = new HTTP_Client(url);

		/**
		 *  A client for SPARQL queries
		 */
		var sparqlClient = new SparqlClient(options.sparql);

		this.sparqlClient = {
				/**
				 * issue SPARQL select
				 * @param query A SPARQL select query
				 * @param onsuccess Function is executed on success with SPARQL/JSON result data as parameter.
				 * @param onfailure Function is executed on failure. It takes a ServerError object.(OPTIONAL)
				 */
				select : function(query,onsuccess,onfailure) {
						sparqlClient.select(query,onsuccess,onfailure);
				}
		}

		/**
		 *  A client for External Data Sources
		 */
		var extDataSourcesClient = new ExtDataSourcesClient(options.extDataSourcesPath)

		this.extDataSourcesClient = {
				/**
				 * issue SPARQL select
				 * @param query A SPARQL select query
				 * @param onsuccess Function is executed on success with SPARQL/JSON result data as parameter.
				 * @param onfailure Function is executed on failure. It takes a ServerError object.(OPTIONAL)
				 */
				hello : function(name,onsuccess,onfailure) {
						extDataSourcesClient.hello(name,onsuccess,onfailure);
				}
		}
		
		    /**
     * Internal Spaqrl Client implementation
     * @param options
     */
    function ExtDataSourcesClient(options) {
        this.hello = function(name,onsuccess,onfailure) {
            HTTP.get(options.path,name,{200:function(data){if(onsuccess)onsuccess(data);
								console.debug("query returned successful");},
                "default":function(err,response){if(onfailure)onfailure(new ServerError(err,response.status));else throw new Error(err)}
            });
        }
    }		
    /**
     * Internal Spaqrl Client implementation
     * @param options
     */
    function SparqlClient(options) {
        this.select = function(query,onsuccess,onfailure) {
            HTTP.get(options.path+"/select",{query:encodeURIComponent(query)},null,"application/sparql-results+json",{
                200:function(data){if(onsuccess)onsuccess(JSON.parse(data).results.bindings);console.debug("query returned successful");},
                "default":function(err,response){if(onfailure)onfailure(new ServerError(err,response.status));else throw new Error(err)}
            });
        }
    }
		
    /**
     * HTTP Client based on XMLHTTPRequest Object, allows RESTful interaction (GET;PUT;POST;DELETE)
     * @param url
     */
    function HTTP_Client(url) {

        function createRequest() {
            var request = null;
            if (window.XMLHttpRequest) {
                request = new XMLHttpRequest();
            } else if (window.ActiveXObject) {
                request = new ActiveXObject("Microsoft.XMLHTTP");
            } else {
                throw "request object can not be created"
            }
            return request;
        }

        //build a query param string
        function buildQueryParms(params) {
            if(params==null||params.length==0) return "";
            var s="?"
            for(prop in params) {
                s+=prop+"="+params[prop]+"&";
            } return s.substring(0,s.length-1);
        }

        //fire request, the method takes a callback object which can contain several callback functions for different HTTP Response codes
        function doRequest(method,path,queryParams,data,mimetype,callbacks) {
            mimetype = mimetype ||  "application/json";
            var _url = url+path+buildQueryParms(queryParams);
             var request = createRequest();
             request.onreadystatechange = function() {
                if (request.readyState==4) {
                    if(callbacks.hasOwnProperty(request.status)) {
                        callbacks[request.status](request.responseText,request);
                    } else if (callbacks.hasOwnProperty("default")) {
                        callbacks["default"](request.responseText,request);
                    } else {
                        throw "Status:"+request.status+",Text:"+request.responseText;
                    }
                }
             };
             request.open(method, _url, true);
             if(method=="PUT"||method=="POST")request.setRequestHeader("Content-Type",mimetype);
             if(method=="GET")request.setRequestHeader("Accept",mimetype);
             request.send( data );
        }

        this.get = function(path,queryParams,data,mimetype,callbacks) {
             doRequest("GET",path,queryParams,data,mimetype,callbacks);
        }

        this.put = function(path,queryParams,data,mimetype,callbacks) {
             doRequest("PUT",path,queryParams,data,mimetype,callbacks);
        }

        this.post = function(path,queryParams,data,mimetype,callbacks) {
             doRequest("POST",path,queryParams,data,mimetype,callbacks);
        }

        this.delete = function(path,queryParams,data,mimetype,callbacks) {
             doRequest("DELETE",path,queryParams,data,mimetype,callbacks);
        }
    }		
}

