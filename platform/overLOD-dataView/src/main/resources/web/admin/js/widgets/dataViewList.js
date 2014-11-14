/**
 * Javascript to manage the list of Data views
 *
 * Author: Fabian Cretton/Liu Zhan - OverLOD Project - HES-SO Valais
 * @param id the div id where the UI's HTML is built
 * @param host The basic URL where Marmotta runs
 */
function DVList(id, host) {
	this.loader = $("<img style='position: relative;top: 4px;margin-left: 10px;' src='../public/img/loader/ajax-loader_small.gif'>");

	var container = $("#" + id);

	// var style = $("<style type='text/css'>.td_title{font-weight:bold;width:100px}</style>")

	this.buildList = function () {
		// alert('buildList') ;
		this.loader.show();

		container.empty();
		container.append($("<h2></h2>").append("Data Views").append(this.loader));

		var listTable = $("<table></table>").addClass("simple_table");
		listTable.attr('id', 'DVTable');
		var titleRow = $("<tr></tr>");
		titleRow.append("<th>Name</th>");
		titleRow.append("<th>&nbsp;</th>");
		titleRow.append("<th>&nbsp;</th>");

		listTable.append(titleRow);
		container.append(listTable);

		function appendDV(viewName) {
			$("<tr>", {
				"id" : "name_" + viewName
			})
			.append($("<td>", {
					"text" : viewName
				}))
			.append($("<td>").append("<a href=\"#\" onclick=\"showViewDetails('" + viewName + "');return false;\">Edit</a>"))
			.append($("<td>").append("<a href=\"#\" onclick=\"deleteDataView('" + viewName + "');return false;\">Delete</a>"))
			.appendTo($("table#DVTable > tbody:last"));
			// this append button don't work: the click is triggered when adding the button, and nothing happens when clicking the button
			// .append($("<button>", {"text": "show details"}).click(alert('coucou'))))

			/*
			// this works
			$("table#DVTable  > tbody:last").append("<tr id=\"name-" + viewName + "\"><td>" + viewName + "</td><td><a href=\"#\" onclick=\"showViewDetails('" + viewName + "');return false;\">show details</a></td></tr>");
			 */
		}

		$.getJSON("../list", function (data) {
			// as data is a simple array of strings, as ["dataView1", "dataView2"],
			// function() does receive the index of each string
			$.each(data, function (index) {
				appendDV(data[index]);
			});
		}).error(function (jqXhr, textStatus, error) {
			alert("ERROR getting Data Views list (" + jqXhr.statusText + ": " + jqXhr.responseText + ")");
		});

		this.loader.hide();
	};

	this.buildList();

}
