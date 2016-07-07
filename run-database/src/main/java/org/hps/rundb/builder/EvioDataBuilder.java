package org.hps.rundb.builder;

import java.io.File;
import java.util.List;

import org.hps.record.epics.EpicsData;
import org.hps.record.epics.EpicsRunProcessor;
import org.hps.record.evio.EvioFileSource;
import org.hps.record.evio.EvioLoop;
import org.hps.record.scalers.ScalerData;
import org.hps.record.scalers.ScalersEvioProcessor;
import org.hps.record.triggerbank.TriggerConfigData;
import org.hps.record.triggerbank.TriggerConfigEvioProcessor;

/**
 * Extracts EPICS data, scaler data and trigger configuration from an EVIO file.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class EvioDataBuilder extends AbstractRunBuilder {

    private File evioFile;
    private List<EpicsData> epicsData;
    private List<ScalerData> scalerData;
    private TriggerConfigData triggerConfig;
    
    void setEvioFile(File evioFile) {
        this.evioFile = evioFile;
    }
    
    List<EpicsData> getEpicsData() {
        return epicsData;
    }
    
    List<ScalerData> getScalerData() {
        return scalerData;
    }
    
    TriggerConfigData getTriggerConfig() {
        return triggerConfig;
    }
    
    @Override
    void build() {
        if (evioFile == null) {
            throw new RuntimeException("The EVIO file was not set.");
        }
        EvioLoop loop = new EvioLoop();
        EvioFileSource src = new EvioFileSource(evioFile);
        loop.setEvioFileSource(src);
        ScalersEvioProcessor scalersProcessor = new ScalersEvioProcessor();
        scalersProcessor.setResetEveryEvent(false);        
        EpicsRunProcessor epicsProcessor = new EpicsRunProcessor();
        loop.addProcessor(epicsProcessor);
        TriggerConfigEvioProcessor configProcessor = new TriggerConfigEvioProcessor();
        loop.addProcessor(configProcessor);
        loop.loop(-1);
        this.epicsData = epicsProcessor.getEpicsData();
        this.scalerData = scalersProcessor.getScalerData();
        this.triggerConfig = configProcessor.getTriggerConfigData();
    }
}
