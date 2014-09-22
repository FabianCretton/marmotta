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
		//alert('instanciation...2') ;
		
    var extDataSources = new ExtDataSources(host);
		//alert('instanciation ok! 2') ;
		
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
            //example_context = data["kiwi.host"] + "context/name";
						example_context = "http://www.websemantique.ch/people/rdf/fabiancretton.rdf";
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
        stepChooseEDSType.append($("<a class='import_type'></a>").text("File").click(function(){
          button.empty();
          stepInputContext.empty();
          stepInputEDSCommons.empty();
          stepInputEDSSource.empty();
          stepInputEDSSource.append("<h2>2. Select file:</h2>");
					
          //stepInputEDSSourceTable.append($("<tr></tr>").append("<td class='td_title'>Source</td>").append("<td>URL</td>"));

          var input = $("<input type='file'>");
          stepInputEDSSource.append(input);
          input.change(function(){
              stepInputEDSCommons.empty();
              if(input.val()==undefined || input.val()=="") alert("Select a file first!");
              else buildCommonUI(input,"file");
          });
       }));
       stepChooseEDSType.append("<span>|</span>");
       stepChooseEDSType.append($("<a class='import_type' ></a>").text("URL").click(function(){
          button.empty();
          stepInputContext.empty()
          stepInputEDSCommons.empty();
          stepInputEDSSource.empty();
					// FC adding default value for tests
          //var input = $("<input type='text' style='width: 300px' >");
					var input = $("<input type='text' style='width: 500px' value='http://www.websemantique.ch/people/rdf/fabiancretton.rdf'>");
					
					//alert(input.html()) ;
					//alert(input.get(0).outerHTML) ;
          var stepInputEDSSourceTable = $("<table></table>") ; //.addClass("importer_table"); // importer_table defined in referencer.html
          stepInputEDSSourceTable.append($("<tr></tr>").append("<td class='td_title'>Source</td>").append("<td>URL</td>"));
					stepInputEDSSourceTable.append($("<tr></tr>").append("<td class='td_title'>URL</td>").append($("<td></td>").append(input)));
          stepInputEDSSource.append(stepInputEDSSourceTable);

					// clicking the button will allow to update some fields, as the mimeType
          stepInputEDSSource.append($("<button></button>").text("ok").click(function(){
              stepInputEDSCommons.empty();
              if(input.val()==undefined || input.val()=="") alert("Define an URL first!");
              else if(!isUrl(input.val())) alert("URL is not valid!")
              else buildCommonUI(input,"url");
          }));
					
					// FC show directly the next stepx
					//stepInputEDSCommons.empty();
					//buildCommonUI(input,"url");
       }));
			 /*
		  stepChooseEDSType.append("<span>|</span>");
			stepChooseEDSType.append($("<a class='import_type' ></a>").text("Test fab").click(function(){
				 button.empty();
				 stepInputContext.empty()
				 stepInputEDSCommons.empty();
				 stepInputEDSSource.empty();
				 //alert('start test Ext') ;
				 //runQuery() ;
				 tstExtDataSourcesGet() ;
       }));
			 */
    }

		/*
	function tstExtDataSourcesGet()
	{
		//alert('tst tstExtDataSourcesGet') ;
		extDataSources.extDataSourcesClient.hello('fabName', success, failure) ;
	}
*/
	function runQuery()
	{
		//alert('run query') ;
		var query = "SELECT * WHERE {?s ?p ?o} LIMIT 2" ;
		
		LMF.sparqlClient.select(query, success, failure) ;
	}

	function success(result)
	{
	alert('success:' + result) ;
	}

  // Fabian: during a trial, marmotta was shut down
	// 	the error.message was empty, and the error.name was "Unknown or not implmented" ! no clear message
	// the error object (a ServerError) is defined by marmotta.js
	function failure(serverError)
	{
	alert('error (' + serverError.name + '): ' + serverError.message) ;
	}
	
	function importFail(serverError)
	{
		alert('The external data source could not be imported (' + serverError.name + '): ' + serverError.message) ;
    loader.hide();
	}
	
	// The EDS parameters have been saved and the import is a running task
	// update the list of EDS, eventhough the import is running
	// WARNING: not handled so far to remove the EDS from the list if the import fails
	function importStarted(data,url,mimetype,context)
	{
    alert("upload is running; you can control the running import tasks on $LMF/core/admin/tasks.html");
    loader.hide();
		pageListEditor.buildList() ;
	}
	
    function buildCommonUI(input_field,source_type) {
        var source_filetype; // type of source: so far it is the serialization of an RDFFile
        var source_filetype_input;
        var context;
        var context_input;
        var context_type="default";

        function waitForMetadataTypes() {
            // stepInputEDSCommons.append("<h2>3. Import (..loading)</h2>");
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

						// FC adding 'option'
						/*
            table.append($("<tr></tr>").append("<td class='td_title'>Context</td>").append($("<td></td>").append(
                $("<select></select>").append("<option>default</option><option>use existing</option><option selected>define new</option>").change(function(){
                    context_type = $(this).val();
                    createContexts();
                })
            )));
						*/
						// FC
						context_type = "define new" ;
						stepInputContext.empty() ;
						context_input = $("<input style='width: 500px' value='" + example_context + "' />");
            var contextTable = $("<table></table>") ; //.addClass("importer_table");
            contextTable.append($("<tr></tr>").append("<td class='td_title'>Context</td>").append($("<td></td>").append(context_input)));
            stepInputContext.append(contextTable);
            
            //stepInputContext.append("Context: ").append(context_input);
								
						// createContexts();
						/*
            var bImport= $("<button  style='font-weight:bold'></button>").text("Import!").click(function(){
                context = context_type=="default"?undefined:context_input.val();
                context = context==null?context=null:context;
                var _url=undefined;
                if(context!=null && !isUrl(context)) {
                    alert("context must be an url!"); return;
                }
								
                if(source_type=="file") {
                    uploadLocalFile(input_field,source_filetype_input.val(),context);
                } else {
                    uploadFromUrl(input_field,source_filetype_input.val(),context);
                }
            });
            button.append(bImport);
						*/
						
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
								
								//alert('calling saveEDSParams4FileURL url:' + urlValue + ', context:' + context+ ', mime:'+mimeTypeValue) ;
								// extDataSources.extDataSourcesClient.saveEDSParams4FileURL('RDFFile', urlValue, mimeTypeValue, context, paramSavedPerformImport, failure) ;
								
								// Save the parameters and upload the file
								loader.show() ;
								extDataSources.extDataSourcesClient.saveEDSParams4FileURLAndImport('RDFFile', urlValue, mimeTypeValue, context, importStarted, importFail) ;
								
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
                    //stepInputContext.append("<h2>4. Select context uri:</h2>").append("no existing context, default is used.");
										stepInputContext.append("no existing context, default is used.");
                }  else {
                    for(var i in contexts) {
                        context_input.append("<option>"+contexts[i]+"</option>")
                    }
                    // stepInputContext.append("<h2>4. Select context url:</h2>").append(context_input);
                    stepInputContext.append("Context: ").append(context_input);
                }
            } else if(context_type=="define new"){
                context_input = $("<input style='width: 500px' value='" + example_context + "' />");
                // stepInputContext.append("<h2>4. Defined context url:</h2>").append(context_input);
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

		// *** the following methods might no more be useful in this .js, as those web services are now directly
		//		called from the ExtDataSources Web Service  ******
		
		// FC Warning: source_filetype_input is the input field itself 
    function uploadLocalFile(source_filetype_input,source_filetype,context) {
       loader.show();
       LMF.importClient.upload(source_filetype_input.get(0).files[0],source_filetype,context,function(){
          alert("import was successful");
           loader.hide();
					 return true ;
       },function(error){
          alert(error.name+": "+error.message);
           loader.hide();
					 return false ;
      });
    }

    function uploadFromUrl(url,mimeType,context) {
      loader.show();
      LMF.importClient.uploadFromUrl(url,mimeType,context,function(){
          alert("upload is running; you can control the running import tasks on $LMF/core/admin/tasks.html");
           loader.hide();
 			 		 pageListEditor.buildList() ;
      },function(error){
          alert(error.name+": "+error.message);
          loader.hide();
      })
    }

    init();
}
