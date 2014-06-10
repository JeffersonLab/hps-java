package org.hps.analysis.dataquality;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * sort of an interface for DQM analysis drivers creates the DQM database
 * manager, checks whether row exists in db etc
 *
 * @author mgraham on Apr 15, 2014 update mgraham on May 15, 2014 to include
 * calculateEndOfRunQuantities & printDQMData i.e. useful methods
 */
public class DataQualityMonitor extends Driver {

    public AIDA aida = AIDA.defaultInstance();
    public DQMDatabaseManager manager;
    public String recoVersion = "v0.0";
    public static int runNumber = 1350;
    public boolean overwriteDB = false;
    public boolean connectToDB = false;
    public boolean printDQMStrings = false;
    public Map<String, Double> monitoredQuantityMap = new HashMap<>();
    public void setRecoVersion(String recoVersion) {
        this.recoVersion = recoVersion;
    }

    public void setRunNumber(int run) {
        this.runNumber = run;
    }

    public void setOverwriteDB(boolean overwrite) {
        this.overwriteDB = overwrite;
    }

    public void setConnectToDB(boolean connect) {
        this.connectToDB = connect;
    }

    public void setPrintDQMStrings(boolean print) {
        this.printDQMStrings = print;
    }

    public void DataQualityMonitor() {

    }

    public void endOfData() {
        calculateEndOfRunQuantities();
        fillEndOfRunPlots();
        printDQMData();
        if (printDQMStrings)
            printDQMStrings();
        System.out.println("Should I write to the database?  " + connectToDB);
        if (connectToDB) {
            System.out.println("Connecting To Database...getting DQMDBManager");
            manager = DQMDatabaseManager.getInstance();
            //check to see if I need to make a new db entry
            boolean entryExists = false;
            try {
                entryExists = checkRowExists();
                if (entryExists)
                    System.out.println("Found an existing run/reco entry in the dqm database; overwrite = " + overwriteDB);
            } catch (SQLException ex) {
                Logger.getLogger(DataQualityMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (!entryExists)
                makeNewRow();
            dumpDQMData();
        }

    }

    private void makeNewRow() {
        System.out.println("is the data base connected?  " + manager.isConnected);
        if (manager.isConnected) {
            String ins = "insert into dqm SET runNumber=" + runNumber;
//            System.out.println(ins);
            manager.updateQuery(ins);
            ins = "update  dqm SET recoVersion='" + recoVersion + "' where runNumber=" + runNumber;
            manager.updateQuery(ins);
        }
        System.out.println("Made a new row for runNumber=" + runNumber + "; recoVersion=" + recoVersion);
    }

    private boolean checkRowExists() throws SQLException {
        String ins = "select * from dqm where " + getRunRecoString();
        ResultSet res = manager.selectQuery(ins);
        if (res.next()) //this is a funny way of determining if the ResultSet has any entries
            return true;
        return false;
    }

    public boolean checkSelectionIsNULL(String var) throws SQLException {
        String ins = "select "+var+" from dqm where " + getRunRecoString();
        ResultSet res = manager.selectQuery(ins);
        res.next();
        double result=res.getDouble(var);
        if(res.wasNull())
            return true;
        System.out.println("checkSelectionIsNULL::"+var+" = "+result);
        return false;
    }

    public String getRunRecoString() {
        return "runNumber=" + runNumber + " and recoVersion='" + recoVersion + "'";
    }

    //override this method to do something interesting   
    //like fill some plots that you only want to fill at end of data (e.g. for occupancies)
    public void fillEndOfRunPlots() {
    }

    //override this method to do something interesting   
    //like calculate averages etc. that can then be put in the db  
    public void calculateEndOfRunQuantities() {
    }

  
    public void dumpDQMData() {
        for (Map.Entry<String, Double> entry : monitoredQuantityMap.entrySet()) {
            String name = entry.getKey();
            double val = entry.getValue();
             boolean isnull=false;
               try {
                 isnull=checkSelectionIsNULL(name);
            } catch (SQLException ex) {
                Logger.getLogger(SvtMonitoring.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (!overwriteDB&&!isnull){                
                System.out.println("Not writing because "+name+" is already filled for this entry");
                continue; //entry exists and I don't want to overwrite                
            }
            String put = "update dqm SET "+name+" = " + val + " WHERE " + getRunRecoString();
            System.out.println(put);
            manager.updateQuery(put); 
           
        }
    }

    //override this method to do something interesting   
    //like print the DQM data log file
    public void printDQMData() {
    }

    //override this method to do something interesting   
    //like print the DQM db variable strings in a good 
    //format for making the db column headers
    public void printDQMStrings() {
    }
}
