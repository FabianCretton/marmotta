 /**
 * Javascript to manage the edition of an External Data Source: saving parameters and upload
 * Currently two EDS types are handled:
 *	- RDFFile: a RDF file from its URL
 *	- LinkedData: a linked data resource from its URL
 *
 * The context for a new EDS must be a new context specific for that EDS, 
 * 	as an EDS update will clear the context and reload the EDS content 
 * 	a check will be done on the server side to ensure that the context don't exist yet
 * 
 * Author: Fabian Cretton - HES-SO Valais
 * @id the div id where the UI's HTML is built
 * @listEditor the EDSList instance for the html page, needed to refresh the EDS list when adding a new one 
 * @param host The basic URL where Marmotta runs
 */
function EDSEditor(id,listEditor,host) {
		var pageListEditor = listEditor ;
		
    var LMF = new MarmottaClient(host);
		
    var extDataSourcesClient = new ExtDataSources(host);
		
    var loader =$("<img style='position: relative;top: 4px;margin-left: 10px;' src='../public/img/loader/ajax-loader_small.gif'>");

    var container = $("#"+id);

    var style = $("<style type='text/css'>.td_title{font-weight:bold;width:100px}</style>")

    var stepChooseEDSType = $("<div></div>");
    var stepInputEDSSource = $("<div></div>");
    var stepInputEDSCommons = $("<div></div>");
    var button = $("<div style='margin-top:20px'></div>");

    var metadata_types;
    var example_context;

    function init() {

        $.getJSON("../../import/types",function(data) {
            metadata_types = data;
        });

        $.getJSON("../../config/data/kiwi.host",function(data) {
						// FC: used for the default context when choosing 'new context' -> use my example one
            example_context = data["kiwi.host"] + "context/" ; // name";
						// example_context = "http://www.websemantique.ch/people/rdf/fabiancretton.rdf";
        });        
        
        container.empty();
        container.append(style);
        container.append(stepChooseEDSType);
        container.append(stepInputEDSSource);
        container.append(stepInputEDSCommons);
        container.append(button);

        stepChooseEDSType.append($("<h2></h2>").append("Add an external data source").append(loader));
				// import_type defined in referencer.html
				 
			// Web RDF File interface 
       stepChooseEDSType.append($("<a class='import_type' ></a>").text("RDF file (URL)").click(function(){
					buildEDSSource("RDFFile", "http://www.websemantique.ch/people/rdf/fabiancretton.rdf") ;
       }));

				stepChooseEDSType.append("<span>|</span>");
			 
			// Linked Data Resource
       stepChooseEDSType.append($("<a class='import_type' ></a>").text("Linked Data Resource").click(function(){
					buildEDSSource("LinkedData", "http://dbpedia.org/resource/Martigny") ;
       }));			 
    }

		// UI to enter the URL, then 'OK' button to display the next steps
		function buildEDSSource(EDS_Type, defaultURL)
		{
          button.empty();
          stepInputEDSCommons.empty();
          stepInputEDSSource.empty();
					// FC adding default value for tests
          //var input = $("<input type='text' style='width: 300px' >");
					var input = $("<input type='text' style='width: 500px' value='"+ defaultURL + "'>");
					
          var stepInputEDSSourceTable = $("<table></table>") ; //.addClass("importer_table"); // importer_table defined in referencer.html
          stepInputEDSSourceTable.append($("<tr></tr>").append("<td class='td_title'>Data source type</td>").append("<td>" + EDS_Type + "</td>"));
					stepInputEDSSourceTable.append($("<tr></tr>").append("<td class='td_title'>URL</td>").append($("<td></td>").append(input)));
          stepInputEDSSource.append(stepInputEDSSourceTable);

					// clicking the button will allow to update some fields, as the mimeType and context
          stepInputEDSSource.append($("<button></button>").text("ok").click(function(){
              stepInputEDSCommons.empty();
              if(input.val()==undefined || input.val()=="") alert("Define an URL first!");
              else if(!isUrl(input.val())) alert("URL is not valid!")
              else buildContextMimeTypeSaveButton(input, EDS_Type, input.val()); // pass the URL value as context default value
          }));
}
					
	function success(result)
	{
	alert('success:' + result) ;
	}

	// receives a JQuery jqXhr object
	// responseText being the response from the server
	function importFail(jqXhr)
	{
		alert('The external data source could not be imported (' + jqXhr.status +'-' + jqXhr.statusText + '): ' + jqXhr.responseText) ;
    loader.hide();
	}
	
	// The EDS parameters have been saved and the import is a running task
	// update the list of EDS, eventhough the import is running
	// WARNING: not handled so far to remove the EDS from the list if the import fails, as it is an asynchronous operation
	function importRDFFileStarted(result)
	{
    alert("upload is running; you can control the running import tasks on $LMF/core/admin/tasks.html");
    loader.hide();
		pageListEditor.buildList() ;
	}
	
	function importLDResource(result)
	{
    alert("Linked Data resource successfully imported: "+ result);
    loader.hide();
		pageListEditor.buildList() ;
	}
	
	/*
	*	Common interface for EDS: 
	*	- MimeType if the type of EDS is RDFFile
	*	- Context 
	* - Save/import button
	*
	* input_field: the text field for URL or Linked Data Resource
	* EDSType: "RDFFile" or "LinkedData"
	*/
    function buildContextMimeTypeSaveButton(input_field,EDS_Type, defaultContextValue) {
        var source_filetype; // type of source: so far it is the mimetype of the RDFFile
        var source_filetype_input;
        var context;
        var context_input;

        function waitForMetadataTypes() {
						stepInputEDSCommons.append("(loading MetadataTypes...)");
            if(metadata_types==undefined) setTimeout(waitForMetadataTypes,1000);
            else writeTable()
        }
				
				if (EDS_Type == "RDFFile")
					waitForMetadataTypes();
				else 
					writeTable() ;
					
        function writeTable() {
            button.empty();
						
						stepInputEDSCommons.empty() ;
						
						if (EDS_Type == "RDFFile") // automatically detect the file's mimetype from file's extension
							{
							var table = $("<table></table>"); // .addClass("importer_table"); // importer_table defined in referencer.html
							
							checkFileType();
							
							var td_mime = $("<td></td>");

							createMimeTD(td_mime);
							table.append($("<tr></tr>").append("<td class='td_title'>Mime</td>").append(td_mime));
							
							stepInputEDSCommons.append(table);
							}

						// Context
						context_input = $("<input style='width: 500px' value='" + defaultContextValue + "' />");
            var contextTable = $("<table></table>") ; //.addClass("importer_table");
            contextTable.append($("<tr></tr>").append("<td class='td_title'>Context</td>").append($("<td></td>").append(context_input)));
            stepInputEDSCommons.append(contextTable);
            
						var bSaveParams= $("<button  style='font-weight:bold'></button>").text("Save params & import!").click(function(){
                context = context_input.val();
 							
                if(context==null) {
                    alert("context must be defined!"); return;
                }
								
                if(!isUrl(context)) {
                    alert("context must be an url!"); return;
                }
								
								var urlValue = input_field.val() ;
               if(urlValue==null) {
                    alert("URL must be defined!"); return;
                }
								
                if(!isUrl(urlValue)) {
                    alert("URL must be an url!"); return;
                }

								// Save the parameters and upload the file
								loader.show() ;
								
								if (EDS_Type == "RDFFile")
									{
									var mimeTypeValue = source_filetype_input.val() ;
									extDataSourcesClient.addRDFFileURLandImport(EDS_Type, urlValue, mimeTypeValue, context, importRDFFileStarted,  importFail) ;
									}
								else // LinkedData
									{
									extDataSourcesClient.addRDFFileURLandImport(EDS_Type, urlValue, "application/rdf+xml", context, importLDResource,  importFail) ;
									}
            });
            button.append(bSaveParams);

        }

        function createMimeTD(td) {
            source_filetype_input = $("<select></select>");
            for(var i in metadata_types) {
                    source_filetype_input.append("<option>"+metadata_types[i]+"</option>");
                }
            td.empty().append(source_filetype_input);
            if(source_filetype)source_filetype_input.val(source_filetype);
        }

        function checkFileType() {
            function checkRDF() {
                var inp = input_field.val();
                var mimeType = null;
                $.ajax({
                    url:   "../../import/types",
                    data:  { 'filename': inp },
                    async: false,
                    dataType: 'json',
                    success: function(data) {
                        if(data.length > 0) {
                            mimeType = data[0];
                        }
                    }
                });
                return mimeType;
            }
						
            source_filetype = checkRDF();
        }
    }

    function isUrl(s) {
	    var regexp = /(file|ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/
	    return regexp.test(s);
    }

    init();
		loader.hide() ;
}
