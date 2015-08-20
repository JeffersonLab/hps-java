<%@ page contentType="text/html" import="java.util.*,java.text.SimpleDateFormat,org.hps.record.epics.*"%>
<!DOCTYPE html>
<html>
<link rel="stylesheet" href="css/style.css" />

<!-- include links and scripts for tablesorter jquery plugin -->
<%@include file="html/tablesorter.html"%>

<!-- sort the table after doc loads -->
<script>
	$(document).ready(function() {
		$("#epics-table").tablesorter({
			widthFixed : true,
			widgets : [ 'zebra' ]
		}).tablesorterPager({
			container : $("#pager"),
			size : 50
		});
	});
</script>

<body>
    <%
        // Get the run number.
        int run = Integer.parseInt(request.getParameterValues("run")[0]);

        // Get the EPICS variable names.
        String epicsBankType = (String) request.getAttribute("EpicsBankType");
        List<String> variableNames = null;
        if (epicsBankType.equals("2s")) {
            variableNames = new ArrayList<String>(Epics2sVariables.getVariables().keySet());
        } else if (epicsBankType.equals("20s")) {
            variableNames = new ArrayList<String>(Epics20sVariables.getVariables().keySet());
        } else {
            throw new RuntimeException("bad EpicsBankType attribute: " + request.getAttribute("EpicsBankType"));
        }

        // Get the list of EPICS data for this run.
        List<EpicsData> epicsDataList = (List<EpicsData>) request.getAttribute("EpicsDataList");
    %>
    <h1>HPS Run <%= run %> - EPICS <%= epicsBankType %> Data</h1>
    <hr />

    <!--  
        TODO: put summary table here showing
        
        -number of epics data blocks
        -(last - first timestamp) = seconds
        -min sequence number
        -max sequence number
        
        -for each variable....
        -variable name
        -number of occurrences
        -mean value
    -->

    <!-- EPICS data table -->
    <table id="epics-table" class="tablesorter-blue">
        <thead>
            <tr>
                <th>Sequence</th>
                <th>Timestamp</th>
                <%
                    for (String variableName : variableNames) {
                %>
                <th><%= variableName %></th>
                <%
                    }
                %>
            </tr>
        </thead>
        <tbody>
            <%
                for (EpicsData epicsData : epicsDataList) {
                    if (epicsData.hasKey(variableNames.get(0))) {
            %>
            <tr>
                <td><%=epicsData.getEpicsHeader().getSequence()%></td>
                <td><%=epicsData.getEpicsHeader().getTimestamp()%></td>
                <%
                    for (String variableName : variableNames) {
                %>
                <td><%=epicsData.getValue(variableName)%></td>
                <%
                    }
                %>
            </tr>
            <%
                }
            %>
            <%
                }
            %>
        
        <tbody>
    </table>

    <!-- tablesorter paging container -->
    <%@include file="html/pager.html"%>

</body>
</html>
