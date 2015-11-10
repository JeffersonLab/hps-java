package org.hps.crawler;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManagerImplementation;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.util.loop.DummyConditionsConverter;
import org.lcsim.util.loop.DummyDetector;

/**
 * Reads metadata from LCIO files with reconstructed data.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class LcioMetadataReader implements FileMetadataReader {

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
        Map<String, Object> metaData = new HashMap<String, Object>();
        LCIOReader reader = null;
        try {        
            reader = new LCIOReader(file);               
            EventHeader eventHeader = null;
            int eventCount = 0;
            Integer run = null;
            try {
                while((eventHeader = reader.read()) != null) {
                    if (run == null) {
                        run = eventHeader.getRunNumber();
                    }            
                    eventCount++;
                }
            } catch (EOFException e) {
                e.printStackTrace();
            }
            metaData.put("eventCount", eventCount);
            metaData.put("runMin", run);
            metaData.put("runMax", run);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }        
        return metaData;
    }
}
