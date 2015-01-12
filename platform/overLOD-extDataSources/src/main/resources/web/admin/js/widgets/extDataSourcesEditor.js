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
 * @param id the div id where the UI's HTML is built
 * @param listEditor the EDSList instance for the html page, needed to refresh the EDS list when adding a new one
 * @param host The basic URL where Marmotta runs
 */
function EDSEditor(id, listEditor, host) {
	var pageListEditor = listEditor;

	var extDataSourcesClient = new ExtDataSources(host);

	var marmottaHost = host;

	var loader = $("<img style='position: relative;top: 4px;margin-left: 10px;' src='../public/img/loader/ajax-loader_small.gif'>");

	var container = $("#" + id);

	var style = $("<style type='text/css'>.td_title{font-weight:bold;width:100px}</style>")

	var divChooseEDSType = $("<div></div>");
	var divEDSEdition = $("<div></div>");
	var divLoadingListsMsg = $("<div></div>");

	var contextExample;
	// following vars will be filled with returns from web service calls
	// will be 'undefined' as long as not loaded, and null if loading failed
	var metadataTypesList;
	var dataFiltersList;
	var dataValidatorsList;

	function init() {

		$.getJSON("../../import/types", function (data) {
			metadataTypesList = data;
		}).error(function (jqXhr, textStatus, error) {
			console.debug("get metadataTypesList failed (" + jqXhr.statusText + ": " + jqXhr.responseText + ")");
			metadataTypesList = null; // so that it is no more 'undefined'
		});

		extDataSourcesClient.getDataFiltersList(successFiltersList, errorFiltersList);
		extDataSourcesClient.getDataValidatorsList(successValidatorsList, errorValidatorsList);

		$.getJSON("../../config/data/kiwi.host", function (data) {
			contextExample = data["kiwi.host"] + "context/EDS/newContext";
		});

		container.empty();
		container.append(style);
		container.append(divChooseEDSType);
		container.append(divEDSEdition);
		container.append(divLoadingListsMsg);

		divChooseEDSType.append($("<h2></h2>").append("External data source edition").append(loader));
		// import_type defined in referencer.html

		// Import an RDF File interface
		divChooseEDSType.append($("<a class='import_type' ></a>").text("RDF file (URL)").click(function () {
				buildEDSEditionGUI("RDFFile", "http://dig.csail.mit.edu/2008/webdav/timbl/foaf.rdf");
			}));

		divChooseEDSType.append("<span>|</span>");

		// Import a Linked Data Resource
		divChooseEDSType.append($("<a class='import_type' ></a>").text("Linked Data Resource").click(function () {
				buildEDSEditionGUI("LinkedData", "http://dbpedia.org/resource/Martigny");
			}));
	}

	function buildEDSEditionGUI(EDS_Type, defaultURL) {
		var selectedSourceFileType; // type of source: so far it is the mimetype of the RDFFile
		var sourceFileTypeInput;
		var selectedContext;
		var contextInput;
		var filtersInput;
		var validatorsInput;

		divEDSEdition.empty();
		// FC adding default value for tests
		//var input = $("<input type='text' style='width: 300px' >");
		var urlInput = $("<input type='text' style='width: 500px' value='" + defaultURL + "'>");

		var tableEDSEdition = $("<table></table>"); //.addClass("importer_table"); // importer_table defined in referencer.html
		tableEDSEdition.append($("<tr></tr>").append("<td class='td_title'>EDS Type</td>").append("<td>" + EDS_Type + "</td>"));
		tableEDSEdition.append($("<tr></tr>").append("<td class='td_title'>URL</td>").append($("<td></td>").append(urlInput)));

		function waitForLists() {
			divLoadingListsMsg.empty();
			divLoadingListsMsg.append("(loading lists...)");

			if (EDS_Type == "RDFFile" && metadataTypesList == undefined) // metadataTypesList needed only for RDF files
				setTimeout(waitForLists, 1000);
			else if (dataFiltersList == undefined)
				setTimeout(waitForMetadataTypes, 1000);
			else if (dataValidatorsList == undefined)
				setTimeout(waitForMetadataTypes, 1000);
			else
				writeTable()
		}

		waitForLists();

		function writeTable() {
			divLoadingListsMsg.empty();

			if (EDS_Type == "RDFFile") {
				checkURLFileType(); // automatically detect the file's mimetype from file's extension

				var tdMime = $("<td></td>");
				createMimeTD(tdMime);
				tdMime.append($("<button></button>").text("from URL file's extension").click(function () {
						checkURLFileType(); // set the sourceFileTypeInput value
						sourceFileTypeInput.val(selectedSourceFileType);
					}));

				tableEDSEdition.append($("<tr></tr>").append("<td class='td_title'>Mime</td>").append(tdMime));
			}

			// filters list
			var tdFilters = $("<td></td>");
			createFiltersTD(tdFilters);
			tableEDSEdition.append($("<tr></tr>").append("<td class='td_title'>Data filter</td>").append(tdFilters));
			
			// validators list
			var tdValidators = $("<td></td>");
			createValidatorsTD(tdValidators);
			tableEDSEdition.append($("<tr></tr>").append("<td class='td_title'>Data validator</td>").append(tdValidators));
			
			// Context
			contextInput = $("<input style='width: 500px' value='" + contextExample + "'/>");
			var tdContextField = $("<td></td>").append(contextInput);
			tdContextField.append($("<button></button>").text("copy URL").click(function () {
					contextInput.val(urlInput.val()); // copy the url value as context value
				}));
				
			tableEDSEdition.append($("<tr></tr>").append("<td class='td_title'>Context</td>").append(tdContextField));
			
			divEDSEdition.append(tableEDSEdition);

			var bSaveParams = $("<button  style='font-weight:bold'></button>").text("Import EDS").click(function () {
					importEDS();
				});

			divEDSEdition.append(bSaveParams);
		}

		function importEDS() {
			selectedContext = contextInput.val();

			if (selectedContext == null) {
				alert("context must be defined!");
				return;
			}

			if (!isUrl(selectedContext)) {
				alert("context must be an url!");
				return;
			}

			var urlValue = urlInput.val();
			if (urlValue == null) {
				alert("URL must be defined!");
				return;
			}

			if (!isUrl(urlValue)) {
				alert("URL must be an url!");
				return;
			}

			// Save the parameters and upload the file
			loader.show();

			// values at 0 are "No filter" and "No validator"
			var filterFileName = null;
			if (filtersInput.prop('selectedIndex') != 0)
				filterFileName = filtersInput.val();

			var validatorFileName = null;
			if (validatorsInput.prop('selectedIndex') != 0)
				validatorFileName = validatorsInput.val();

			if (EDS_Type == "RDFFile") {
				var mimeTypeValue = sourceFileTypeInput.val();
				extDataSourcesClient.addEDS(EDS_Type, urlValue, mimeTypeValue, selectedContext, filterFileName, validatorFileName, importEDSSuccess, importFail);
			} else // LinkedData
			{
				extDataSourcesClient.addEDS(EDS_Type, urlValue, "application/rdf+xml", selectedContext, filterFileName, validatorFileName, importEDSSuccess, importFail);
			}
		}

		function createMimeTD(td) {
			sourceFileTypeInput = $("<select></select>");
			for (var i in metadataTypesList) {
				sourceFileTypeInput.append("<option>" + metadataTypesList[i] + "</option>");
			}
			td.empty().append(sourceFileTypeInput);
			if (selectedSourceFileType)
				sourceFileTypeInput.val(selectedSourceFileType);
		}

		function createFiltersTD(td) {
			filtersInput = $("<select></select>");
			filtersInput.append("<option>No filter</option>");
			// as data is a simple array of strings, as ["dataView1", "dataView2"],
			// function() does receive the index of each string
			$.each(dataFiltersList, function (index) {
				filtersInput.append("<option>" + dataFiltersList[index] + "</option>");
			});

			td.empty().append(filtersInput);
			//if (selectedSourceFileType)
			//	sourceFileTypeInput.val(selectedSourceFileType);
		}

		function createValidatorsTD(td) {
			validatorsInput = $("<select></select>");
			validatorsInput.append("<option>No validator</option>");
			// as data is a simple array of strings, as ["dataView1", "dataView2"],
			// function() does receive the index of each string
			$.each(dataValidatorsList, function (index) {
				validatorsInput.append("<option>" + dataValidatorsList[index] + "</option>");
			});

			td.empty().append(validatorsInput);
		}

		function checkURLFileType() {
			function checkRDF() {
				var inp = urlInput.val();
				var mimeType = null;
				$.ajax({
					url : "../../import/types",
					data : {
						'filename' : inp
					},
					async : false,
					dataType : 'json',
					success : function (data) {
						if (data.length > 0) {
							mimeType = data[0];
						}
					}
				});
				return mimeType;
			}

			selectedSourceFileType = checkRDF();
		}
	}

	// receives a JQuery jqXhr object, responseText being the response from the server
	function importFail(jqXhr) {
		alert('The external data source could not be imported (' + jqXhr.status + '-' + jqXhr.statusText + '): ' + jqXhr.responseText);
		loader.hide();
	}

	function importEDSSuccess(result) {
		alert("EDS successfully imported: " + result);
		loader.hide();
		pageListEditor.buildList();
	}

	function errorFiltersList(jqXhr) {
		alert('The data filters list could not be loaded (' + jqXhr.status + '-' + jqXhr.statusText + '): ' + jqXhr.responseText);
		dataFiltersList = null; // so that it is no more 'undefined'
	}

	function successFiltersList(data) {
		dataFiltersList = data;
	}

	function errorValidatorsList(jqXhr) {
		alert('The data validators list could not be loaded (' + jqXhr.status + '-' + jqXhr.statusText + '): ' + jqXhr.responseText);
		dataValidatorsList = null; // so that it is no more 'undefined'
	}

	function successValidatorsList(data) {
		dataValidatorsList = data;
	}

	function isUrl(s) {
		var regexp = /(file|ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/
			return regexp.test(s);
	}

	init();
	loader.hide();
}
