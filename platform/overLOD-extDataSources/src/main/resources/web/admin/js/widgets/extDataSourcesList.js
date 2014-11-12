 /**
 * Javascript to manage the list of External Data Sources
 *
 * Author: Fabian Cretton - HES-SO Valais
 * @id the div id where the UI's HTML is built
 * @param host The basic URL where Marmotta runs
 */
function EDSList(id,host) {

		this.loader =$("<img style='position: relative;top: 4px;margin-left: 10px;' src='../public/img/loader/ajax-loader_small.gif'>");

    var container = $("#"+id);

    var extDataSourcesClient = new ExtDataSources(host);

		// var EDSParamsWS = host + "EDS/EDSParams" ;
		
    // var style = $("<style type='text/css'>.td_title{font-weight:bold;width:100px}</style>")

	this.buildList = function(){
			this.loader.show() ;

			container.empty() ;
			container.append($("<h2></h2>").append("External Data Sources (EDS)").append(this.loader));
			// container.append($("<p></p>").append("Configured External Data Sources:")) ;
			
      var listTable = $("<table></table>").addClass("simple_table");
			listTable.attr('id', 'EDSTable') ;
			var titleRow = $("<tr></tr>") ;
			titleRow.append("<th>Context</th>") ;
			titleRow.append("<th>Type</th>");
			titleRow.append("<th>URL</th>");
			titleRow.append("<th>TimeStamp</th>");
			titleRow.append("<th>&nbsp;</th>");
			titleRow.append("<th>&nbsp;</th>");
			listTable.append(titleRow) ;
			container.append(listTable);
		
			// add one line in the table of EDS
			function appendEDS(context, EDSParams) {
				$("<tr>", {"id": "context_" + context })
				.append($("<td>", {"text": context}))
				.append($("<td>", {"text": EDSParams.EDSType}))
				.append($("<td>", {"text": EDSParams.url}))
				//.append($("<td>", {"text": EDSParams.timeStamp}))
				.append($("<td>", {"text": new Date(parseInt(EDSParams.timeStamp)).toLocaleDateString("en-US")}))
				.append($("<td>").append("<a href=\"#\" onclick=\"updateEDS('" + context + "', '" + EDSParams.url + "');return false;\">force update</a>" ))
				.append($("<td>").append("<a href=\"#\" onclick=\"deleteEDS('" + context + "');return false;\">delete</a>" ))
				.appendTo($("table#EDSTable > tbody:last"));  
			}		

			function successDisplayList(data)
			{
					$.each(data, function(context, EDSParams) {
						appendEDS(context, EDSParams);
					});
			}
			
			function 	errorList(jqXhr, textStatus, error)		
			{
				alert("ERROR getting External Data Sources list (" + jqXhr.statusText + ": " + jqXhr.responseText +")");
			}
			
			extDataSourcesClient.getEDSParamsList(successDisplayList, errorList) ;
			this.loader.hide() ;
		}
		
			/* Could be useful to test if EDSParams.timeStamp can be converted to a date or not 
			function isValidDate(d) {
				if ( Object.prototype.toString.call(d) !== "[object Date]" )
					return false;
				return !isNaN(d.getTime());
			}
			*/
			
		this.buildList() ;
}