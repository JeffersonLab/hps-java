<%@ page contentType="text/html" import="java.util.*,org.hps.run.database.*,java.text.SimpleDateFormat"%>
<!DOCTYPE html>
<html>
<link rel="stylesheet" href="css/style.css" />
<body>
    <h1>HPS Run Summary</h1>
    <hr />
    <%
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        RunSummary runSummary = (RunSummary) request.getAttribute("RunSummary"); 
    %>
    <p><b>run:</b> <%= runSummary.getRun() %></p>
    <p><b>start:</b> <%= dateFormat.format(runSummary.getStartDate()) %></p>
    <p><b>end:</b> <%= dateFormat.format(runSummary.getEndDate()) %></p>
    <p><b>events:</b> <%= runSummary.getTotalEvents() %></p>
    <p><b>files:</b> <%= runSummary.getTotalFiles() %></p>
    <p><b>end okay:</b> <%= runSummary.getEndOkay() %></p>
    <p><b>run okay:</b> <%= runSummary.getRunOkay() %></p>
    <p><b>updated:</b> <%= dateFormat.format(runSummary.getUpdated()) %></p>
    <p><b>created:</b> <%= dateFormat.format(runSummary.getCreated()) %></p>
    <hr/>	
    <p>
        <a href="epics?run=<%= runSummary.getRun() %>&epicsBankType=<%= EpicsType.EPICS_1S %>">EPICS 1s Data</a>        
    </p>		
    <p>
        <a href="epics?run=<%= runSummary.getRun() %>&epicsBankType=<%= EpicsType.EPICS_10S %>">EPICS 10s Data</a>
    </p>
    <p>
        <a href="scalers?run=<%= runSummary.getRun() %>">Scaler Data</a>
    </p>
</body>
</html>
