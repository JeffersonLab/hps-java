<%@ page contentType="text/html" import="java.util.*,java.text.SimpleDateFormat,org.hps.record.epics.*,org.hps.run.database.*" %>
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
        int run = Integer.parseInt(request.getParameterValues("run")[0]);      
        List<EpicsVariable> epicsVariables = (List<EpicsVariable>) request.getAttribute("EpicsVariables");
        EpicsType epicsType = (EpicsType) request.getAttribute("EpicsType");
        List<EpicsData> epicsDataList = (List<EpicsData>) request.getAttribute("EpicsDataList");
    %>
    <h1>HPS Run <%= run %> - EPICS <%= epicsType.getTypeCode() %>s Data</h1>
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
                    for (EpicsVariable epicsVariable : epicsVariables) {
                %>
                <th><%= epicsVariable.getVariableName() %></th>
                <%
                    }
                %>
            </tr>
        </thead>
        <tbody>
            <%
                for (EpicsData epicsData : epicsDataList) {
            %>
            <tr>
                <td><%=epicsData.getEpicsHeader().getSequence()%></td>
                <td><%=epicsData.getEpicsHeader().getTimestamp()%></td>
                <%
                    for (EpicsVariable epicsVariable : epicsVariables) {
                %>
                <td><%=epicsData.getValue(epicsVariable.getVariableName())%></td>
                <%
                    }
                %>
            </tr>
            <%
                }
            %>
        
        <tbody>
    </table>

    <!-- tablesorter paging container -->
    <%@include file="html/pager.html"%>

</body>
</html>
