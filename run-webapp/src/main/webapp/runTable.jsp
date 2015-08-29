<%@ page contentType="text/html" import="java.util.*,org.hps.run.database.RunSummary,java.text.SimpleDateFormat"%>
<!DOCTYPE html>
<html>

<!-- application stylesheet -->
<link rel="stylesheet" href="css/style.css" />

<!-- include links and scripts for tablesorter jquery plugin -->
<%@include file="html/tablesorter.html" %>

<!-- sort the table after doc loads -->
<script>
    $(document).ready(function() 
        { 
            $("#run-table")
            .tablesorter({widthFixed: true, widgets: ['zebra']})
            .tablesorterPager({container: $("#pager"), size: 20});
        }
    );
</script>

<body>
	<h1>HPS Run Table</h1>
	<hr/>
	
	<!-- full run table with tablesorter theme, sorting and pagination -->
	<table id="run-table" class="tablesorter-blue">
		<thead>
			<tr>
				<th>Run</th>
				<th>Start Date</th>
				<th>End Date</th>
				<th>Events</th>
				<th>Files</th>
				<th>End Okay</th>
				<th>Run Okay</th>
				<th>Updated</th>
				<th>Created</th>
			</tr>
		</thead>
		<tbody>
		    <!-- get the list of run summaries from the request and create a table row for each one -->
			<% 
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                List<RunSummary> runSummaries = (List<RunSummary>) request.getAttribute("RunSummaries");
                for (RunSummary runSummary : runSummaries) { 
            %>
            <!-- table row links to its run summary page -->
			<tr onclick="document.location.href='run?run=<%= runSummary.getRun() %>';">
				<td><%= runSummary.getRun() %></td>
				<td><%= dateFormat.format(runSummary.getStartDate()) %></td>
				<td><%= dateFormat.format(runSummary.getEndDate()) %></td>
				<td><%= runSummary.getTotalEvents() %></td>
				<td><%= runSummary.getTotalFiles() %></td>
				<td><%= runSummary.getEndOkay() %></td>
				<td><%= runSummary.getRunOkay() %></td>
				<td><%= dateFormat.format(runSummary.getUpdated()) %></td>
				<td><%= dateFormat.format(runSummary.getCreated()) %></td>
			</tr>
			<% } %>		
		<tbody>
	</table>
	
	<!-- tablesorter paging container -->
    <%@include file="html/pager.html" %>
    
</body>
</html>
