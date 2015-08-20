<%@ page contentType="text/html" import="java.util.*,org.hps.record.run.RunSummary,java.text.SimpleDateFormat"%>
<!DOCTYPE html>
<html>
<link rel="stylesheet" href="css/style.css" />
<body>
    <h1>HPS Run Summary</h1>
    <hr />
    <%
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        RunSummary runSummary = (RunSummary) request.getAttribute("RunSummary"); 
    %>
    <p>run: <%= runSummary.getRun() %></p>
    <p>start: <%= dateFormat.format(runSummary.getStartDate()) %></p>
    <p>end: <%= dateFormat.format(runSummary.getEndDate()) %></p>
    <p>events: <%= runSummary.getTotalEvents() %></p>
    <p>files: <%= runSummary.getTotalFiles() %></p>
    <p>end okay: <%= runSummary.getEndOkay() %></p>
    <p>run okay: <%= runSummary.getRunOkay() %></p>
    <p>updated: <%= dateFormat.format(runSummary.getUpdated()) %></p>
    <p>created: <%= dateFormat.format(runSummary.getCreated()) %></p>	
    <hr/>	
    <p>
        <a href="epics?run=<%= runSummary.getRun() %>&epicsBankType=2s">EPICS 2s Data</a>        
    </p>		
    <p>
        <a href="epics?run=<%= runSummary.getRun() %>&epicsBankType=20s">EPICS 20s Data</a>
    </p>
    <p>
        <a href="scalers?run=<%= runSummary.getRun() %>">Scaler Data</a>
    </p>
</body>
</html>
