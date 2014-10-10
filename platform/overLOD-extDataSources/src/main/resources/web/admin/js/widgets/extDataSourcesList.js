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
			// alert('buildList') ;
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
			listTable.append(titleRow) ;
      container.append(listTable);
		
			// add one line in the table of EDS
			function appendEDS(context, EDSParams) {
				$("<tr>", {"id": "context_" + context })
				.append($("<td>", {"text": context}))
				.append($("<td>", {"text": EDSParams.EDSType}))
				.append($("<td>", {"text": EDSParams.url}))
				.append($("<td>").append("<a href=\"#\" onclick=\"deleteEDS();return false;\">delete</a>" ))
				.appendTo($("table#EDSTable > tbody:last"));  
				/*
				$("table#EDSTable  > tbody:last").append("<tr id=\"context-" + context + "\"><td>" + context + "</td><td>" + EDSParams.EDSType + "</td><td>" + EDSParams.url + "</td><td><a href=\"#\" class=\"deletePrefix\">delete</a></td></tr>");
				*/
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