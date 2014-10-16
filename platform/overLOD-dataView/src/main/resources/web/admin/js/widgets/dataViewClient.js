/**
 * Creates a DataView object to access the Web Service
 *
 * Author: Fabian Cretton - OverLOD Project - HES-SO Valais
 * @param url The basic URL where Marmotta runs
 */
function DataView(url) {
    if( url==undefined) throw "url must be defined"; //test if url is defined
    if( url.lastIndexOf("/")==url.length-1) url=url.substring(0,url.length-1); //clean url
    //default options
    var options = {
        dataViewPath : {
						path : "/dataView" //path to webservices
        }
    }

    //create http stub
    var HTTP = new HTTP_Client(url);

		/**
		 * hello - testing web service GET
		 * @param name any string
		 * @param onsuccess Function is executed on success with string result data as parameter.
		 * @param onfailure Function is executed on failure. It takes a ServerError object.(OPTIONAL)
		 */
	 this.hello = function(name,onsuccess,onfailure) {
				var params = {name:name};
				HTTP.get(options.dataViewPath.path + "/hello",params,null, "text/plain; charset=utf8",{200:function(data){if(onsuccess) onsuccess(data);
						console.debug("query returned successful");},
						"default":function(err,response){if(onfailure)onfailure(new ServerError(err,response.status));else throw new Error(err)}
				});
		}

 		/**
		 * getDataView - getting data from a DataView
		 * @param name of the data view
		 * @param queryParams: a javascrip object arrays of parameters that should be added to the query string.
		 *				those parameters are predefined for the SPARQL query corresponding to the 'viewName'
		 * @param onsuccess Function is executed on success with string result data as parameter.
		 * @param onfailure Function is executed on failure. It takes a ServerError object.(OPTIONAL)
		 */
		this.getDataView = function(viewName,queryParams,resultMimeType,onsuccess,onfailure) {
				var params = {viewName:viewName};
				if (queryParams != null)
					$.extend(params, queryParams);
				
				HTTP.get(options.dataViewPath.path,params,null, resultMimeType,{200:function(data){if(onsuccess) onsuccess(data);
						console.debug("query returned successful");},
						"default":function(err,response){if(onfailure)onfailure(new ServerError(err,response.status));else throw new Error(err)}
				});
		}
		
		/**
		 * getDataViewsList - getting the list of DataViews
		 * @param onsuccess Function is executed on success with string result data as parameter.
		 * @param onfailure Function is executed on failure. It takes a ServerError object.(OPTIONAL)
		 */
		this.getDataViewsList = function(onsuccess,onfailure) {
				HTTP.get(options.dataViewPath.path + "/list",null,null, null,{200:function(data){if(onsuccess) onsuccess(data);
						console.debug("query returned successful");},
						"default":function(err,response){if(onfailure)onfailure(new ServerError(err,response.status));else throw new Error(err)}
				});
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
                default: return "Unknown or not implemented";
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
						console.debug("url:" + _url) ;
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

