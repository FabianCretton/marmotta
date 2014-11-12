/**
 * Creates a DataView object to access the Web Service
 * Based on JQuery 1.8
 * This version is based on CORS to do cross-domain calls with http authentication
 * The user/pwd are set in the 'credentials' variable, in the form "user:pwd"
 *
 * Author: Fabian Cretton - OverLOD Project - HES-SO Valais
 * @param url The basic URL where Marmotta runs
 */
function DataView(url) {
		var credentials = "admin:pass123" ;
		var encodedCredentials = Base64.encode(credentials) ;
		
    if( url==undefined) throw "url must be defined"; //test if url is defined
    if( url.lastIndexOf("/")==url.length-1) url=url.substring(0,url.length-1); //clean url
		
    //default options
    var options = {
        dataViewPath : {
						path : url + "/dataView" //path to webservices
        }
    }
		
		if (!browserSupportsCors())
			alert("It seems your browser don't support CORS calls (dataViewClient_CORS.js)") ;

		/**
		 * getDataView - getting data from a DataView
		 * @param viewName of the data view
		 * @param queryParams: a javascrip object arrays of parameters that should be added to the query string.
		 *				those parameters are predefined for the SPARQL query corresponding to the 'viewName'
		 * @param resultMimeType mimeType for the resulting SPARQL result as: "application/sparql-results+json", "application/sparql-results+xml", "text/csv"
		 * @param onsuccess Function is executed on success with string result data as parameter.
		 * @param onfailure Function is executed on failure. It takes a ServerError object.(OPTIONAL)
		 *
		 * Returns a string containing the SPARQL result in the specified format (resultMimeType)
		 */
		this.getDataView = function(viewName,queryParams,resultMimeType,onsuccess,onfailure)
		{
				var params = {viewName:viewName};
				if (queryParams != null)
					$.extend(params, queryParams);

				$.ajax({
					url: options.dataViewPath.path,
					type: 'GET',
					data: params,
					dataType: 'text', // the returned data will be a text -> success: function(result) -> result is a text. 
					// if dataType is not specified, a javascript object is returned
					xhrFields: { 
						withCredentials: true // for CORS with http auth
					},
					beforeSend: function(xhrObj)
					{
							xhrObj.setRequestHeader("Accept",resultMimeType);
							xhrObj.setRequestHeader('Authorization', "Basic " + encodedCredentials); // http authentication
					},					
					success: function(result) 
					{
							if(onsuccess)
								onsuccess(result);
							else	
							 console.debug("getDataView successful");
					},
				 error: function (jqXhr, textStatus, error) 
				 {
							if(onfailure)
								onfailure(jqXhr) ;
							else	
							 console.debug("getDataView failed (" + jqXhr.statusText + ": " + jqXhr.responseText + ")");
					}
			});

		}
		
		/**
		 * Add a new DataView
		 * Fails if a dataView with the same viewName already exists
		 * @param viewName of the data view
		 * @param query the SPARQL query
		 * @param onsuccess Function is executed on success with string result data as parameter.
		 * @param onfailure Function is executed on failure. It takes a ServerError object.(OPTIONAL)
		*/
		this.addDataView = function(viewName, query, onsuccess, onfailure)
		{
				$.ajax({
					url: options.dataViewPath.path + '?viewName=' + viewName + '&query='+ encodeURIComponent(query),
					type: 'POST',
					xhrFields: { 
						withCredentials: true // for CORS with http auth
					},
					beforeSend: function(xhrObj)
					{
							xhrObj.setRequestHeader('Authorization', "Basic " + encodedCredentials); // http authentication
					},					
					success: function(result) {
							if(onsuccess)
								onsuccess(result);
							else	
							 console.debug("addDataView successful");
					},
				 error: function (jqXhr, textStatus, error) {
							if(onfailure)
								onfailure(jqXhr) ;
							else	
							 console.debug("addDataView failed (" + jqXhr.statusText + ": " + jqXhr.responseText + ")");
				}
			});

		}		

		/**
		 * Update an existing new DataView
		 * Fails if the dataView don't exist
		 * @param viewName of the data view
		 * @param query the new SPARQL query
		 * @param onsuccess Function is executed on success with string result data as parameter.
		 * @param onfailure Function is executed on failure. It takes a ServerError object.(OPTIONAL)
		*/
		this.updateDataView = function(viewName, query, onsuccess, onfailure)
		{
				$.ajax({
					url: options.dataViewPath.path + '?viewName=' + viewName + '&query='+ encodeURIComponent(query),
					type: 'PUT',
					dataType: 'text',
					xhrFields: { 
						withCredentials: true // for CORS with http auth
					},
					beforeSend: function(xhrObj)
					{
							xhrObj.setRequestHeader('Authorization', "Basic " + encodedCredentials); // http authentication
					},					
					success: function(result) {
							if(onsuccess)
								onsuccess(result);
							else	
							 console.debug("updateDataView successful");
					},
				 error: function (jqXhr, textStatus, error) {
							if(onfailure)
								onfailure(jqXhr) ;
							else	
							 console.debug("updateDataView failed (" + jqXhr.statusText + ": " + jqXhr.responseText + ")");
				}
			});
		}
		
		this.deleteDataView = function(viewName, onsuccess, onfailure)
		{
				$.ajax({
				url: options.dataViewPath.path + '?viewName=' + viewName,
				type: 'DELETE',
					xhrFields: { 
						withCredentials: true // for CORS with http auth
					},
					beforeSend: function(xhrObj)
					{
							xhrObj.setRequestHeader('Authorization', "Basic " + encodedCredentials); // http authentication
					},					
				success: function(result) {
						if(onsuccess)
							onsuccess(result);
						else	
						 console.debug("deleteDataView successful");
						},
					 error: function (jqXhr, textStatus, error) {
								if(onfailure)
									onfailure(jqXhr) ;
								else	
								 console.debug("deleteDataView failed (" + jqXhr.statusText + ": " + jqXhr.responseText + ")");
					}
					});
			}		
			
			function browserSupportsCors() {

				if ("withCredentials" in new XMLHttpRequest())
					return true;	
				else if (window.XDomainRequest)
					return true;
				else
					return false;
			}			
}

