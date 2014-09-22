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
 * Creates a ExternalDataSources object to access the Web Service
 *
 * Author: Fabian Cretton - HES-SO Valais
 * @param url The basic URL where Marmotta runs
 */
function ExtDataSources(url) {

    if( url==undefined) throw "url must be defined"; //test if url is defined
    if( url.lastIndexOf("/")==url.length-1) url=url.substring(0,url.length-1); //clean url
    //default options
    var options = {
        extDataSourcesPath : {
						path : "/EDS" //path to webservices
        },
        sparql : {
            path : "/sparql"
        }
    }

    //create http stub
    var HTTP = new HTTP_Client(url);

		/**
		 *  A client for External Data Sources
		 */
		var extDataSourcesClient = new ExtDataSourcesClient(options.extDataSourcesPath)

		this.extDataSourcesClient = {
				/**
				 * hello - testing web service GET
				 * @param name any string
				 * @param onsuccess Function is executed on success with string result data as parameter.
				 * @param onfailure Function is executed on failure. It takes a ServerError object.(OPTIONAL)
				 */
				hello : function(name,onsuccess,onfailure) {
						extDataSourcesClient.hello(name,onsuccess,onfailure);
				},
				
				/**
				 * saveEDSParams4FileURL - saving EDS parameters for a file from URL
				 * @param url of the file
				 * @param mimetype of the file
				 * @param context Named graph where the file will be store locally
				 * @param onsuccess Function is executed on success with string result data as parameter.
				 * @param onfailure Function is executed on failure. It takes a ServerError object.(OPTIONAL)
				 */
				saveEDSParams4FileURL : function(EDSType, url,mimetype,context,onsuccess,onfailure) {
						extDataSourcesClient.saveEDSParams4FileURL(EDSType, url,mimetype,context,onsuccess,onfailure);
				},
				
				/**
				 * saveEDSParams4FileURLAndImport - saving EDS parameters for a file from URL, then import the File
				 * @param url of the file
				 * @param mimetype of the file
				 * @param context Named graph where the file will be store locally
				 * @param onsuccess Function is executed on success with string result data as parameter.
				 * @param onfailure Function is executed on failure. It takes a ServerError object.(OPTIONAL)
				 */
				saveEDSParams4FileURLAndImport : function(EDSType, url,mimetype,context,onsuccess,onfailure) {
						extDataSourcesClient.saveEDSParams4FileURLAndImport(EDSType, url,mimetype,context,onsuccess,onfailure);
				}				
		}
		
		    /**
     * Internal "External Data Sources" implementation
     * @param options
     */
    function ExtDataSourcesClient(options) {

				// only saving the EDS parameters, not implemented yet
				/*
        this.saveEDSParams4FileURL = function(EDSType, url,mimetype,context,onsuccess,onfailure) {
            var params = {EDSType:EDSType,url:url,context:context};
             HTTP.post(options.path+"/EDSParams",params,null,mimetype,{
                 200:function(data){if(onsuccess)onsuccess(data);console.debug("saveEDSParams4FileURL successful");},
								"default":function(err,response){if(onfailure)onfailure(new ServerError(err,response.status));else throw new Error(err)}
             });
        }	
				*/
				
        this.saveEDSParams4FileURLAndImport = function(EDSType, url,mimetype,context,onsuccess,onfailure) {
            var params = {EDSType:EDSType,url:url,context:context};
             HTTP.post(options.path+"/EDSParams",params,null,mimetype,{
                 200:function(data){if(onsuccess)onsuccess(data,url,mimetype,context);;
								 console.debug("saveEDSParams4FileURLAndImport successful");},
								"default":function(err,response){if(onfailure)onfailure(new ServerError(err,response.status));else throw new Error(err)}
             });
        }	
				
    }		
		
    /**
     * A ServerError Object
     * @param message
     * @param status
     */
    function ServerError(message,status) {
       function getStatus(){
            switch(status) {
                case 203: return "Non-Authoritative Information";
                case 204: return "No Content";
                case 400: return "Bad Request";
                case 401: return "Unauthorized";
                case 403: return "Forbidden";
                case 404: return "Not Found";
                case 406: return "Not Acceptable";
                case 412: return "Invalid Content-Type";
                case 415: return "Unsupported Media Type";
                case 500: return "Internal Server Error";
                default: return "Unknown or not implmented";
            }
        }
        this.status = status;
        this.message = message;
        this.name = getStatus();
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

