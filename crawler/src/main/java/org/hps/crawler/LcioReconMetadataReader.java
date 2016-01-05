package org.hps.crawler;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManagerImplementation;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.util.loop.DummyConditionsConverter;
import org.lcsim.util.loop.DummyDetector;

/**
 * Reads metadata from LCIO files with reconstructed data.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class LcioReconMetadataReader implements FileMetadataReader {

    /**
     * Setup the conditions system in dummy mode.
     */
    static {
        ConditionsManager conditionsManager = ConditionsManager.defaultInstance();
        ConditionsReader dummyReader = ConditionsReader.createDummy();
        ((ConditionsManagerImplementation) conditionsManager).setConditionsReader(dummyReader, "DUMMY");
        DummyDetector detector = new DummyDetector("DUMMY");
        conditionsManager.registerConditionsConverter(new DummyConditionsConverter(detector));
    }
    
    /**
     * Get the metadata for the LCIO file.
     * 
     * @param file the LCIO file
     * @return the metadata map with key and value pairs
     */
    @Override
    public Map<String, Object> getMetadata(File file) throws IOException {
        
        //Set<String> collectionNames = new HashSet<String>();
        String detectorName = null;
        long eventCount = 0;
        Long run = null;
        LCIOReader reader = null;                
        try {        
            reader = new LCIOReader(file);               
            EventHeader eventHeader = null;
            try {
                while((eventHeader = reader.read()) != null) {
                    if (run == null) {
                        run = (long) eventHeader.getRunNumber();
                    }
                    if (detectorName == null) {
                        detectorName = eventHeader.getDetectorName();
                    }
                    //for (List<?> list : eventHeader.getLists()) {
                    //    LCMetaData metadata = eventHeader.getMetaData(list);
                    //    collectionNames.add(metadata.getName());
                    //}
                    eventCount++;
                }
            } catch (EOFException e) {
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }    
        
        // Build collection names string.
        //StringBuffer sb = new StringBuffer();
        //for (String collectionName : collectionNames) {
        //    sb.append(collectionName + ",");
        //}
        //sb.setLength(sb.length() - 1);
        
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("eventCount", eventCount);
        metadata.put("runMin", run);
        metadata.put("runMax", run);
        metadata.put("DETECTOR", detectorName);
        //metadata.put("COLLECTIONS", sb.toString());
        
        return metadata;
    }
}
