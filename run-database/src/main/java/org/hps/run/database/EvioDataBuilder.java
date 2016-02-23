package org.hps.run.database;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.hps.record.epics.EpicsData;
import org.hps.record.epics.EpicsRunProcessor;
import org.hps.record.evio.EvioFileSource;
import org.hps.record.evio.EvioFileUtilities;
import org.hps.record.evio.EvioLoop;
import org.hps.record.scalers.ScalerData;
import org.hps.record.scalers.ScalersEvioProcessor;

/**
 * Extracts lists of EPICS and scaler data in an EVIO file and insert
 * them into the run database.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class EvioDataBuilder extends AbstractRunBuilder {

    private Logger LOGGER = Logger.getLogger(EvioDataBuilder.class.getPackage().getName());
    private File evioFile;
    private List<EpicsData> epicsData;
    private List<ScalerData> scalerData;
    
    void setEvioFile(File evioFile) {
        this.evioFile = evioFile;
    }
    
    List<EpicsData> getEpicsData() {
        return epicsData;
    }
    
    List<ScalerData> getScalerData() {
        return scalerData;
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
        loop.loop(-1);
        this.epicsData = epicsProcessor.getEpicsData();
        this.scalerData = scalersProcessor.getScalerData();
    }
    
    public void main(String args[]) {
        
        if (args.length == 0) {
            throw new RuntimeException("No command line arguments provided.");
        }
        String path = args[0];
        File file = new File(path);
        int run = EvioFileUtilities.getRunFromName(file);
        
        EvioDataBuilder builder = new EvioDataBuilder();
        builder.setEvioFile(file);
        builder.build();
        
        if (!builder.getEpicsData().isEmpty()) {
            RunManager runManager = null;
            try {
                runManager = new RunManager();
                runManager.setRun(run);
                runManager.updateEpicsData(epicsData);
            } finally {
                runManager.closeConnection();
            }
        } else {
            LOGGER.warning("No EPICS data was found to insert into run database.");
        }
    }
}
