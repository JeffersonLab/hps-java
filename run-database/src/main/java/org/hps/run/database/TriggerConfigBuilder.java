package org.hps.run.database;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hps.record.daqconfig.TriggerConfigEvioProcessor;
import org.hps.record.evio.EvioFileUtilities;
import org.hps.record.triggerbank.TriggerConfigData;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

public class TriggerConfigBuilder extends AbstractRunBuilder {
    
    private TriggerConfigData triggerConfig;
    private List<File> files = null;
    
    void setFiles(List<File> files) {
        this.files = files;
    }
    
    void build() {        
        int fileIndex = files.size() - 1;
        TriggerConfigEvioProcessor processor = new TriggerConfigEvioProcessor();
        while (triggerConfig == null && fileIndex >= 0) {
            File file = files.get(fileIndex);
            try {
                EvioReader reader = EvioFileUtilities.open(file, true);
                EvioEvent evioEvent = reader.parseNextEvent();
                while (evioEvent != null) {
                    processor.process(evioEvent);
                    if (processor.getTriggerConfigData() != null 
                            && processor.getTriggerConfigData().isValid()) {
                        triggerConfig = processor.getTriggerConfigData();
                        break;
                    }
                    evioEvent = reader.parseNextEvent();
                }
                fileIndex -= 1;
            } catch (EvioException | IOException e) {
                throw new RuntimeException(e);
            }            
        }
    }
    
    TriggerConfigData getTriggerConfigData() {
        return triggerConfig;
    }
}
