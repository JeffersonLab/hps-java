package org.hps.conditions.svt;

import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * This test reads Test Run bad channel collections from the database
 * using a conditions series and checks that the correct number
 * of channels are flagged using several different runs.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunSvtBadChannelsTest extends TestCase {
    
    static String detectorName = "HPS-TestRun-v5";
    
    /*
    mysql> select run_start, run_end, name from conditions where table_name like 'test_run_svt_bad_channels';
    +-----------+---------+---------------------------+
    | run_start | run_end | name                      |
    +-----------+---------+---------------------------+
    |         0 |    1365 | test_run_svt_bad_channels |
    |      1351 |    1351 | test_run_svt_bad_channels |
    |      1353 |    1353 | test_run_svt_bad_channels |
    |      1354 |    1354 | test_run_svt_bad_channels |
    |      1358 |    1358 | test_run_svt_bad_channels |
    |      1359 |    1359 | test_run_svt_bad_channels |
    |      1360 |    1360 | test_run_svt_bad_channels |
    +-----------+---------+---------------------------+
    */    
    static int[] runNumbers = new int[] {0, 1351, 1353, 1354, 1358, 1359, 1360};

    /*
    mysql> select collection_id, count(*) from test_run_svt_bad_channels group by collection_id;
    +---------------+----------+
    | collection_id | count(*) |
    +---------------+----------+
    |             1 |       50 |
    |             2 |      392 |
    |             3 |      427 |
    |             4 |      457 |
    |             5 |      298 |
    |             6 |      424 |
    |             7 |      424 |
    +---------------+----------+
    */    
    static int[] badChannelCount = {50, 392, 427, 457, 298, 424, 424};    
    
    public void testSvtBadChannels() throws ConditionsNotFoundException {
                
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setXmlConfig("/org/hps/conditions/config/conditions_database_testrun_2012.xml");
        conditionsManager.setLogLevel(Level.WARNING);
        
        for (int i = 0; i < runNumbers.length; i++) {  
        
            int runNumber = runNumbers[i];
            
            System.out.println("-------------");
            System.out.println("Run #" + runNumber);
            System.out.println("-------------");
            System.out.println();
            
            conditionsManager.setDetector(detectorName, runNumber);
        
            ConditionsSeries<SvtBadChannel, SvtBadChannelCollection> series = 
                    conditionsManager.getConditionsSeries(SvtBadChannelCollection.class, "test_run_svt_bad_channels");
        
            int totalBadChannels = 0;
            for (ConditionsObjectCollection<SvtBadChannel> collection : series) {
                //System.out.println(collection.getConditionsRecord());
                totalBadChannels += collection.size();
            }        
            System.out.println("found " + totalBadChannels + " total bad chanenls");
           
            // The run 0 channels are for all runs.
            int expectedBadChannels = badChannelCount[0];
            if (runNumber != 0) {
                // Add bad channels from individual runs.
                expectedBadChannels += badChannelCount[i];
            }
            System.out.println("expected " + expectedBadChannels + " bad channels");
            
            assertEquals("Wrong number of bad channels for run #" + runNumber, expectedBadChannels, totalBadChannels);
            
            System.out.println();
        }
    }
}