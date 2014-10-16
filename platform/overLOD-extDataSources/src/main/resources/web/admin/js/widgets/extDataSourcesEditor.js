 /**
 * Javascript to manage the edition of an External Data Source: saving parameters and upload
 *
 * Author: Fabian Cretton - HES-SO Valais
 * @id the div id where the UI's HTML is built
 * @listEditor the EDSList instance for the html page, needed to refresh the EDS list when adding a new one 
 * @param host The basic URL where Marmotta runs
 */
function EDSEditor(id,listEditor,host) {
		var pageListEditor = listEditor ;
		
    var LMF = new MarmottaClient(host);
		
    var extDataSources = new ExtDataSources(host);
		
    //TODO
    var loader =$("<img style='position: relative;top: 4px;margin-left: 10px;' src='../public/img/loader/ajax-loader_small.gif'>");

    var container = $("#"+id);

    var style = $("<style type='text/css'>.td_title{font-weight:bold;width:100px}</style>")

    var stepChooseEDSType = $("<div></div>");
    var stepInputEDSSource = $("<div></div>");
    var stepInputEDSCommons = $("<div></div>");
    var stepInputContext = $("<div></div>");
    var button = $("<div style='margin-top:20px'></div>");

    var metadata_types;
    var contexts;
    var example_context;

    function init() {

        $.getJSON("../../import/types",function(data) {
            metadata_types = data;
        });

        $.getJSON("../../context/list",function(data) {
            loader.hide();
            contexts = data;
        });
        
        $.getJSON("../../config/data/kiwi.host",function(data) {
						// FC: used for the default context when choosing 'new context' -> use my example one
            example_context = data["kiwi.host"] + "context/" ; // name";
						// example_context = "http://www.websemantique.ch/people/rdf/fabiancretton.rdf";
        });        
        
        container.empty();
        container.append(style);
        container.append(stepChooseEDSType); // stepChooseEDSType
        container.append(stepInputEDSSource);
        container.append(stepInputEDSCommons);
        container.append(stepInputContext);
        container.append(button);

        stepChooseEDSType.append($("<h2></h2>").append("Edit external data source").append(loader));
				// import_type defined in referencer.html
				
			// Web RDF File interface 
       stepChooseEDSType.append($("<a class='import_type' ></a>").text("RDF file (URL)").click(function(){
          button.empty();
          stepInputContext.empty()
          stepInputEDSCommons.empty();
          stepInputEDSSource.empty();
					// FC adding default value for tests
          //var input = $("<input type='text' style='width: 300px' >");
					var input = $("<input type='text' style='width: 500px' value='http://www.websemantique.ch/people/rdf/fabiancretton.rdf'>");
					
          var stepInputEDSSourceTable = $("<table></table>") ; //.addClass("importer_table"); // importer_table defined in referencer.html
          stepInputEDSSourceTable.append($("<tr></tr>").append("<td class='td_title'>Data source type</td>").append("<td>WebRDFFile</td>"));
					stepInputEDSSourceTable.append($("<tr></tr>").append("<td class='td_title'>URL</td>").append($("<td></td>").append(input)));
          stepInputEDSSource.append(stepInputEDSSourceTable);

					// clicking the button will allow to update some fields, as the mimeType
          stepInputEDSSource.append($("<button></button>").text("ok").click(function(){
              stepInputEDSCommons.empty();
              if(input.val()==undefined || input.val()=="") alert("Define an URL first!");
              else if(!isUrl(input.val())) alert("URL is not valid!")
              else buildCommonUI(input,"RDFFile", input.val()); // pass the URL value as context default value
          }));
       }));

				stepChooseEDSType.append("<span>|</span>");
			 
				// Local RDF File interface 
        stepChooseEDSType.append($("<a class='import_type'></a>").text("Another EDS type").click(function(){
          button.empty();
          stepInputContext.empty();
          stepInputEDSCommons.empty();
          stepInputEDSSource.empty();
					
          var stepInputEDSSourceTable = $("<table></table>") ;
          stepInputEDSSourceTable.append($("<tr></tr>").append("<td class='td_title'>Data source type</td>").append("<td>To be implemented</td>"));
					
          stepInputEDSSource.append(stepInputEDSSourceTable);
					
					/*
          var input = $("<input type='file'>");
					stepInputEDSSourceTable.append($("<tr></tr>").append("<td class='td_title'>File</td>").append($("<td></td>").append(input)));
					
          input.change(function(){
							console.log(input.get(0).files[0]) ;
							//alert(input.get(0).files[0]) ;
              stepInputEDSCommons.empty();
              if(input.val()==undefined || input.val()=="") 
								alert("Select a file first!");
              else 
								buildCommonUI(input,"LocalRDFFile", example_context+input.val()); // build a default context name, adding the name of the selected file
          });
					*/
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
	
	function importFail_old(serverError)
	{
		alert('The external data source could not be imported (' + serverError.name + '): ' + serverError.message) ;
    loader.hide();
	}
	
	function uploadFile(EDSType, url, mimeType, context, onsuccess, onfailure)
	{
				$.ajax({
					url: '/EDS/EDSParams?EDSType=' + EDSType + '&url='+ encodeURIComponent(url) + '&context='+ encodeURIComponent(context),
					type: 'POST',
					contentType: mimeType,
					//data: params,
					//dataType:'text',
					success: function(result) {
							if(onsuccess)
								onsuccess();
								
							console.debug("saveEDSParams4FileURLAndImport successful");
					},
				 error: function (jqXhr, textStatus, error) {
							if(onfailure)
								onfailure(jqXhr) ;
				}
			});
	}
	
	// The EDS parameters have been saved and the import is a running task
	// update the list of EDS, eventhough the import is running
	// WARNING: not handled so far to remove the EDS from the list if the import fails, as it is an asynchronous operation
	function importStarted()
	{
    alert("upload is running; you can control the running import tasks on $LMF/core/admin/tasks.html");
    loader.hide();
		pageListEditor.buildList() ;
	}
	
	// input_field: the file input selection for local file, the text field for URL
	// EDSType: so far "WebRDFFile" or "LocalRDFFile"
    function buildCommonUI(input_field,EDS_Type, defaultContextValue) {
        var source_filetype; // type of source: so far it is the serialization of an RDFFile
        var source_filetype_input;
        var context;
        var context_input;
        var context_type="default";

        function waitForMetadataTypes() {
						stepInputEDSCommons.append("(loading MetadataTypes...)");
            if(metadata_types==undefined) setTimeout(waitForMetadataTypes,1000);
            else writeTable()
        }
        waitForMetadataTypes();

        function writeTable() {
            stepInputContext.empty();
            button.empty();
						stepInputEDSCommons.empty() ;
            checkFileType();
            var table = $("<table></table>"); // .addClass("importer_table"); // importer_table defined in referencer.html

            var td_mime = $("<td></td>");

            createMimeTD(td_mime);
            table.append($("<tr></tr>").append("<td class='td_title'>Mime</td>").append(td_mime));

            stepInputEDSCommons.append(table);

						// FC
						context_type = "define new" ;
						stepInputContext.empty() ;
						context_input = $("<input style='width: 500px' value='" + defaultContextValue + "' />");
            var contextTable = $("<table></table>") ; //.addClass("importer_table");
            contextTable.append($("<tr></tr>").append("<td class='td_title'>Context</td>").append($("<td></td>").append(context_input)));
            stepInputContext.append(contextTable);
            
						var bSaveParams= $("<button  style='font-weight:bold'></button>").text("Save params & import!").click(function(){
                context = context_type=="default"?undefined:context_input.val();
                context = context==null?context=null:context;
 							
                if(context==null) {
                    alert("context must be defined!"); return;
                }
								
                if(!isUrl(context)) {
                    alert("context must be an url!"); return;
                }
								
								// Warning: if the goal here is to avoid saving in a context that already exists
								// then it would be necessary to 'refresh' the list of context, as it might have changed
								// 	i.e. another user could have deleted/added contexts in the meanwhile
								// this should be performed on the server 
								//alert('context already exists:' + checkExistingContext(context)) ;

								var urlValue = input_field.val() ;
               if(urlValue==null) {
                    alert("URL must be defined!"); return;
                }
								
                if(!isUrl(urlValue)) {
                    alert("URL must be an url!"); return;
                }
								
								var mimeTypeValue= source_filetype_input.val()
								
								// Save the parameters and upload the file
								loader.show() ;
								
								//uploadFile(EDS_Type, urlValue, mimeTypeValue, context, importStarted, importFail) ;
								extDataSources.addRDFFileURLandImport(EDS_Type, urlValue, mimeTypeValue, context, importStarted, importFail) ;
								
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

				// Check if the context passed as parameter already exists or not
				function checkExistingContext(aContext)
				{
            for(var i in contexts) {
							if (aContext == contexts[i])
								return true ;
            }
						
						return false ;
				}
				
        function createContexts() {
            stepInputContext.empty();
            context=undefined;
            if(context_type=="use existing") {
                context_input = $("<select></select>");
                if(contexts.length==0) {
										stepInputContext.append("no existing context, default is used.");
                }  else {
                    for(var i in contexts) {
                        context_input.append("<option>"+contexts[i]+"</option>")
                    }
                    stepInputContext.append("Context: ").append(context_input);
                }
            } else if(context_type=="define new"){
                context_input = $("<input style='width: 500px' value='" + example_context + "' />");
                stepInputContext.append("Context: ").append(context_input);
            }
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
}
