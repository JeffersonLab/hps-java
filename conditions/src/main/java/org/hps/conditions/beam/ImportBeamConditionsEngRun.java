package org.hps.conditions.beam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.beam.BeamConditions.BeamConditionsCollection;
import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * <p>
 * Import beam measurements into the database from a text file.
 * <p>
 * This has the format:<br/> 
 * run current x y
 * <p>
 * The beam energy is hard-coded to 1.92 GeV for now, pending updates with better information.
 *  
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class ImportBeamConditionsEngRun {

    static double beamEnergy = 1.92;
    static double nullValue = -999.0;
    
    private ImportBeamConditionsEngRun() {
    }
    
    public static void main(String[] args) throws Exception {
        
        if (args.length == 0) {
            throw new RuntimeException("missing file list argument");
        }
        
        String fileName = args[0];
        if (!new File(fileName).exists()) {
            throw new IOException("The file " + fileName + " does not exist.");
        }
        
        Map<Integer, BeamConditions> beamMap = new LinkedHashMap<Integer, BeamConditions>();
                
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = reader.readLine()) != null) {
            
            String[] values = line.split(" "); 
            
            BeamConditions beam = new BeamConditions();
            
            setValue(beam, "current", values[1]);
            setValue(beam, "position_x", values[2]);
            setValue(beam, "position_y", values[3]);
                        
            if (beam.getFieldValue("current") == null) {
                // Use null value to indicate beam was not measured.
                beam.setFieldValue("energy", null);
            } else if (((Double)beam.getFieldValue("current")) == 0) {
                // Use zero for no beam.
                beam.setFieldValue("energy", 0);
            } else {
                // Use nominal beam energy from ECAL commissioning.
                beam.setFieldValue("energy", beamEnergy);
            }
                       
            beamMap.put(Integer.parseInt(values[0]), beam);
        }
        reader.close();
        
        System.out.println("printing beam conditions parsed from " + fileName + " ...");
        System.out.println("run id current x y energy");
        for (Entry<Integer, BeamConditions> entry : beamMap.entrySet()) {
            System.out.print(entry.getKey() + " ");
            System.out.println(entry.getValue() + " ");
        }
        
        DatabaseConditionsManager manager = new DatabaseConditionsManager();
        manager.setLogLevel(Level.ALL);
        
        for (Entry<Integer, BeamConditions> entry : beamMap.entrySet()) {
            int run = entry.getKey();
            BeamConditions beam = entry.getValue();
            int collectionId = manager.getNextCollectionID("beam");
            ConditionsRecord record = 
                    new ConditionsRecord(collectionId, run, run, "beam", "beam", "imported from HPS_Runs.pdf", "eng_run");
            System.out.println(record);
            System.out.println(beam);
            BeamConditionsCollection collection = new BeamConditionsCollection();
            collection.add(beam);
            manager.insertCollection(collection);
            record.insert();
        }        
        manager.closeConnection();
    }    
    
    static void setValue(BeamConditions beam, String fieldName, String rawValue) {
        double value = Double.parseDouble(rawValue);
        if (value != nullValue) {                
            beam.setFieldValue(fieldName, value);
        } else {
            beam.setFieldValue(fieldName, null);
        }
    }    
}