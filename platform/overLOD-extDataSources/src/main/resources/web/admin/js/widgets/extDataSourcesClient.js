/**
 * Creates a ExternalDataSources object to access the Web Service
 * Based on JQuery 1.8
 *
 * Author: Fabian Cretton - overLOD project - HES-SO Valais
 * @param url The basic URL where Marmotta runs
 */
function ExtDataSources(serverUrl) {

    if( serverUrl==undefined) throw "url must be defined"; //test if url is defined
    if( serverUrl.lastIndexOf("/")==serverUrl.length-1) serverUrl=serverUrl.substring(0,serverUrl.length-1); //clean url
    //default options
		// var EDSParamsPath = serverUrl + "/EDS/EDSParams" ;
    var options = {
        EDSParams : {
						path : serverUrl + "/EDS/EDSParams"
        },
        checkUpdates : {
						path : serverUrl + "/EDS/checkUpdates"
        }
    }
		
		/**
		 * Get the list of EDSParams
		 * @param onsuccess Function is executed on success with a JSON object which is a list of EDSParams. (OPTIONAL)
		 * @param onfailure Function is executed on failure. It takes a JQuery jqXhr object.(OPTIONAL)
		 */		
		this.getEDSParamsList = function(onsuccess, onfailure)
		{
			$.getJSON(options.EDSParams.path, function(data) {
					if(onsuccess)
						onsuccess(data);
					else	
					 console.debug("getEDSParamsList successful");
			}).error(function(jqXhr, textStatus, error) {
					if(onfailure)
						onfailure(jqXhr) ;
					else	
					 console.debug("getEDSParamsList failed (" + jqXhr.statusText + ": " + jqXhr.responseText + ")");
			});			
		}
		
		/**
		 * Add a new EDS for an RDF file (URL): save its parameters, then import the File
		 * @param url of the file
		 * @param mimeType of the file
		 * @param context Named graph where the file will be store locally
		 * @param onsuccess Function is executed on success with string result data as parameter.(OPTIONAL)
		 * @param onfailure Function is executed on failure. It takes a JQuery jqXhr object.(OPTIONAL)
		 */
		this.addRDFFileURLandImport = function(EDSType, url, mimeType, context, onsuccess, onfailure)
		{
				$.ajax({
					url: options.EDSParams.path + '?EDSType=' + EDSType + '&url='+ encodeURIComponent(url) + '&context='+ encodeURIComponent(context),
					type: 'POST',
					contentType: mimeType,
					//data: params,
					//dataType:'text',
					success: function(result) {
							if(onsuccess)
								onsuccess(result);
							else	
							 console.debug("addRDFFileURLandImport successful");
					},
				 error: function (jqXhr, textStatus, error) {
							if(onfailure)
								onfailure(jqXhr) ;
							else	
							 console.debug("getDataView failed (" + jqXhr.statusText + ": " + jqXhr.responseText + ")");
				}
			});
		}
		
		/**
		 * Update an EDS content, identified by its context
		 * @param context Named graph and identifier
		 * @param onsuccess Function is executed on success with a String success message. (OPTIONAL)
		 * @param onfailure Function is executed on failure. It takes a JQuery jqXhr object.(OPTIONAL)
		 */		
		this.updateEDS = function(context, onsuccess, onfailure)
		{
				$.ajax({
				url: options.EDSParams.path + '?context='+ encodeURIComponent(context),
				type: 'PUT',
				success: function(result) {
					if(onsuccess)
						onsuccess(result);
					else	
					 console.debug("updateEDS successful");
				},
			 error: function (jqXhr, textStatus, error) {
					if(onfailure)
						onfailure(jqXhr) ;
					else	
					 console.debug("updateEDS failed (" + jqXhr.statusText + ": " + jqXhr.responseText + ")");
				}
			});
		}

		/**
		 * Delete an EDS (params and its content-named graph), identified by its context
		 * @param context Named graph and identifier
		 * @param onsuccess Function is executed on success with a String success message. (OPTIONAL)
		 * @param onfailure Function is executed on failure. It takes a JQuery jqXhr object.(OPTIONAL)
		 */		
		this.deleteEDS = function(context, onsuccess, onfailure)
		{
				$.ajax({
					url: options.EDSParams.path + '?context=' + encodeURIComponent(context) + '&deleteGraph=true',
					type: 'DELETE',
				success: function(result) {
					if(onsuccess)
						onsuccess(result);
					else	
					 console.debug("deleteEDS successful");
				},
			 error: function (jqXhr, textStatus, error) {
					if(onfailure)
						onfailure(jqXhr) ;
					else	
					 console.debug("deleteEDS failed (" + jqXhr.statusText + ": " + jqXhr.responseText + ")");
				}
			});
		}
		

		/**
		 * Check for all EDS if an update is necessary
		 * @param onsuccess Function is executed on success with a JSON object containing result.stringList, a list of strings which are the contexts being updated (OPTIONAL)
		 * @param onfailure Function is executed on failure. It takes a JQuery jqXhr object.(OPTIONAL)
		 */		
		this.checkEDSForUpdates = function(onsuccess, onfailure)
		{
				$.ajax({
							url: options.checkUpdates.path,
							type: 'PUT',
				success: function(result) {
					if(onsuccess)
						onsuccess(result);
					else	
					 console.debug("checkEDSForUpdates successful");
				},
			 error: function (jqXhr, textStatus, error) {
					if(onfailure)
						onfailure(jqXhr) ;
					else	
					 console.debug("checkEDSForUpdates failed (" + jqXhr.statusText + ": " + jqXhr.responseText + ")");
				}
			});
		}
		
}

