 /**
 * Javascript to manage the list of External Data Sources
 *
 * Author: Fabian Cretton - HES-SO Valais
 * @id the div id where the UI's HTML is built
 * @param host The basic URL where Marmotta runs
 */
function EDSList(id,host) {
    var loader =$("<img style='position: relative;top: 4px;margin-left: 10px;' src='../public/img/loader/ajax-loader_small.gif'>");

    var container = $("#"+id);

    var style = $("<style type='text/css'>.td_title{font-weight:bold;width:100px}</style>")

		this.buildList = function(){
			loader.show() ;

			container.empty() ;
			container.append($("<h2></h2>").append("External Data Sources").append(loader));
			container.append($("<p></p>").append("Configured External Data Sources:")) ;
			
      var listTable = $("<table></table>").addClass("simple_table");
			listTable.attr('id', 'EDSTable') ;
			var titleRow = $("<tr></tr>") ;
			titleRow.append("<th>Context</th>") ;
			titleRow.append("<th>Type</th>");
			titleRow.append("<th>URL</th>");
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
				.append($("<td>").append("<a href=\"#\" onclick=\"updateEDS('" + context + "', '" + EDSParams.url + "');return false;\">update</a>" ))
				.append($("<td>").append("<a href=\"#\" onclick=\"deleteEDS('" + context + "');return false;\">delete</a>" ))
				.appendTo($("table#EDSTable > tbody:last"));  
			}		
			
			$.getJSON("../EDSParams", function(data) {
					$.each(data, function(context, EDSParams) {
						appendEDS(context, EDSParams);
					});  								
			}).error(function(jqXhr, textStatus, error) {
					// textStatus just contains 'error'
					alert("ERROR getting External Data Sources list (" + jqXhr.statusText + ": " + jqXhr.responseText);
			});		

			loader.hide() ;
		} ;
		
		this.buildList() ;
}