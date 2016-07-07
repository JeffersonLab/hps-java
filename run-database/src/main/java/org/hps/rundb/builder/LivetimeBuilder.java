package org.hps.rundb.builder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hps.record.evio.EvioFileUtilities;
import org.hps.record.scalers.ScalerData;
import org.hps.record.scalers.ScalerUtilities;
import org.hps.record.scalers.ScalersEvioProcessor;
import org.hps.record.scalers.ScalerUtilities.LiveTimeIndex;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * Computes livetimes from a set of EVIO files.
 * 
 * @author jeremym
 */
public class LivetimeBuilder extends AbstractRunBuilder {
    
    private List<File> files;
    private ScalerData scalerData;
    
    void setFiles(List<File> files) {
        this.files = files;
    }    
    
    void build() {
        if (files == null) {
            throw new RuntimeException("The list of files was never set.");
        }        
        int fileIndex = files.size() - 1;
        ScalersEvioProcessor processor = new ScalersEvioProcessor();
        processor.setResetEveryEvent(false);        
        while (scalerData == null && fileIndex >= 0) {
            File file = files.get(fileIndex);
            try {
                EvioReader reader = EvioFileUtilities.open(file, true);
                EvioEvent evioEvent = reader.parseNextEvent();
                while (evioEvent != null) {
                    processor.process(evioEvent);
                    evioEvent = reader.parseNextEvent();
                }
                if (processor.getCurrentScalerData() != null) {
                    scalerData = processor.getCurrentScalerData();
                    break;
                }
                fileIndex -= 1;
            } catch (EvioException | IOException e) {
                throw new RuntimeException(e);
            }            
        }
                
        if (scalerData != null) {
            double[] livetimes = ScalerUtilities.getLiveTimes(scalerData);
            getRunSummary().setLivetimeClock(livetimes[LiveTimeIndex.CLOCK.ordinal()]);
            getRunSummary().setLivetimeFcupTdc(livetimes[LiveTimeIndex.FCUP_TDC.ordinal()]);
            getRunSummary().setLivetimeFcupTrg(livetimes[LiveTimeIndex.FCUP_TRG.ordinal()]);
        } 
    }
}
