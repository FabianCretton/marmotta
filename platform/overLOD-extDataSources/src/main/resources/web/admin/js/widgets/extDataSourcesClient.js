/**
 * Creates a ExternalDataSources object to access the Web Service
 *
 * Author: Fabian Cretton - overLOD project - HES-SO Valais
 * @param url The basic URL where Marmotta runs
 */
function ExtDataSources(serverUrl) {

    if( serverUrl==undefined) throw "url must be defined"; //test if url is defined
    if( serverUrl.lastIndexOf("/")==serverUrl.length-1) serverUrl=serverUrl.substring(0,serverUrl.length-1); //clean url
    //default options
		var EDSPath = serverUrl + "/EDS/EDSParams" ;

		/**
		 * addRDFFileURLandImport - saving EDS parameters for a file from URL, then import the File
		 * based on JQuery 1.8
		 * @param url of the file
		 * @param mimeType of the file
		 * @param context Named graph where the file will be store locally
		 * @param onsuccess Function is executed on success with string result data as parameter.(OPTIONAL)
		 * @param onfailure Function is executed on failure. It takes a JQuery jqXhr object.(OPTIONAL)
		 */
		this.addRDFFileURLandImport = function(EDSType, url, mimeType, context, onsuccess, onfailure)
		{
				$.ajax({
					url: EDSPath + '?EDSType=' + EDSType + '&url='+ encodeURIComponent(url) + '&context='+ encodeURIComponent(context),
					type: 'POST',
					contentType: mimeType,
					//data: params,
					//dataType:'text',
					success: function(result) {
							if(onsuccess)
								onsuccess();
								
							// console.debug("addRDFFileURLandImport successful");
					},
				 error: function (jqXhr, textStatus, error) {
							if(onfailure)
								onfailure(jqXhr) ;
				}
			});

		}
}

