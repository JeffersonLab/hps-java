package org.hps.run.database;

import java.io.File;
import java.sql.Connection;
import java.util.List;

import junit.framework.TestCase;

import org.hps.conditions.database.ConnectionParameters;
import org.srs.datacat.client.ClientBuilder;

public class RunBuilderTest extends TestCase {
    
    private static final int RUN = 5403;
    private static String DATACAT_URL = "http://localhost:8080/datacat-v0.5-SNAPSHOT/r";
    private static String SPREADSHEET = "/work/hps/rundb/HPS_Runs_2015_Sheet1.csv";
    private static String FOLDER = "/HPS/test";
    private static String SITE = "SLAC";
    
    private static final ConnectionParameters CONNECTION_PARAMETERS = 
            new ConnectionParameters("root", "derp", "hps_run_db", "localhost");
        
    public void testRunBuilder() throws Exception {
        
        RunSummaryImpl runSummary = new RunSummaryImpl(RUN);
        
        // datacat
        DatacatBuilder datacatBuilder = new DatacatBuilder();
        datacatBuilder.setDatacatClient(new ClientBuilder().setUrl(DATACAT_URL).build());
        datacatBuilder.setFolder(FOLDER);
        datacatBuilder.setSite(SITE);
        datacatBuilder.setRunSummary(runSummary);
        datacatBuilder.build();
        
        List<File> files = datacatBuilder.getFileList();
        
        // livetime measurements
        LivetimeBuilder livetimeBuilder = new LivetimeBuilder();
        livetimeBuilder.setRunSummary(runSummary);
        livetimeBuilder.setFiles(files);
        livetimeBuilder.build();
        
        // trigger config
        TriggerConfigBuilder configBuilder = new TriggerConfigBuilder();
        configBuilder.setFiles(files);
        configBuilder.build();
        
        // run spreadsheet
        SpreadsheetBuilder spreadsheetBuilder = new SpreadsheetBuilder();
        spreadsheetBuilder.setSpreadsheetFile(new File(SPREADSHEET));
        spreadsheetBuilder.setRunSummary(datacatBuilder.getRunSummary());
        spreadsheetBuilder.build();
        
        // database updater
        Connection connection = CONNECTION_PARAMETERS.createConnection();
        DatabaseUpdater updater = new DatabaseUpdater(connection);
        updater.setRunSummary(runSummary);
        System.out.println("built run summary ...");
        System.out.println(runSummary);
        //updater.setTriggerConfigData(configBuilder.getTriggerConfigData());
        //updater.update();
    }
}
