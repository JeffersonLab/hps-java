package org.hps.users.phansson.daq;

import hep.aida.IHistogram2D;
import hep.aida.IPlotter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.analysis.trigger.util.TriggerDataUtils;
import org.hps.evio.SvtEvioReader;
import org.hps.record.svt.SvtEvioUtils;
import org.hps.util.BasicLogFormatter;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class SvtOldHeaderAnalysisDriver extends Driver {

    private final AIDA aida = AIDA.defaultInstance();
    
    private final String HeaderCollectionName = "SvtHeaders"; 
    private final Logger LOGGER = Logger.getLogger(SvtOldHeaderAnalysisDriver.class.getSimpleName());
    private int nEventsProcessed = 0;
    private Date eventDate = new Date(0);
    private IHistogram2D rceSyncErrorCount;
    private IHistogram2D rceOFErrorCount;
    private IHistogram2D rceSkipCount;
    private IHistogram2D rceMultisampleErrorCount;
    //private Map< Integer, Map< String, IHistogram1D> > histError = new HashMap< Integer, Map<String, IHistogram1D >>();
    private int nRceSyncErrorCountN = 0;
    private int nRceOFErrorCount = 0;
    private int nRceSkipCount = 0;
    private int nRceMultisampleErrorCount = 0;
    private int nRceSvtHeaders = 0;
    IPlotter plotter;

    private String logFileName = "dummy.log";
    FileWriter fileWriter; 
    PrintWriter printWriter;

    private boolean showPlots = false;
    private final String triggerBankCollectionName = "TriggerBank";
    
    /**
     * 
     */
    public SvtOldHeaderAnalysisDriver() {
         
    }
    
    public void setLogFileName(String name) {
        this.logFileName = name;
    }
    
    public void setShowPlots(boolean show) {
        this.showPlots = show;
    }
    
    @Override
    protected void detectorChanged(Detector detector) {
        
        FileHandler fh;
        try {
            fh = new FileHandler(logFileName);
            LOGGER.addHandler(fh);
            fh.setFormatter(new BasicLogFormatter());
        } catch (SecurityException | IOException e1) {
            e1.printStackTrace();
        }
        
        plotter = aida.analysisFactory().createPlotterFactory().create("rceSyncError");
        plotter.createRegions(2, 2);
        final int n = SvtEvioReader.MAX_ROC_BANK_TAG+1 -  SvtEvioReader.MIN_ROC_BANK_TAG;
        rceSyncErrorCount = aida.histogram2D("rceSyncError", n, SvtEvioReader.MIN_ROC_BANK_TAG, SvtEvioReader.MAX_ROC_BANK_TAG+1, 2, 0, 1);
        rceOFErrorCount = aida.histogram2D("rceOFError", n, SvtEvioReader.MIN_ROC_BANK_TAG, SvtEvioReader.MAX_ROC_BANK_TAG+1, 2, 0, 1);
        rceSkipCount = aida.histogram2D("rceSkipCount", n, SvtEvioReader.MIN_ROC_BANK_TAG, SvtEvioReader.MAX_ROC_BANK_TAG+1, 2, 0, 1);
        rceMultisampleErrorCount = aida.histogram2D("rceMultisampleError", n, SvtEvioReader.MIN_ROC_BANK_TAG, SvtEvioReader.MAX_ROC_BANK_TAG+1, 2, 0, 1);
        
        plotter.region(0).plot(rceSyncErrorCount);
        plotter.region(1).plot(rceOFErrorCount);
        plotter.region(2).plot(rceSkipCount);
        plotter.region(3).plot(rceMultisampleErrorCount);
        if(showPlots ) plotter.show();
        
        
    }
    
    
    @Override
    protected void process(EventHeader event) {
        
        if(event.hasCollection(GenericObject.class, triggerBankCollectionName)) {
            Date currentEventDate = TriggerDataUtils.getEventTimeStamp(event, triggerBankCollectionName);
            if( currentEventDate == null) {
                LOGGER.info("Couldn't get event date from trigger bank for processed " + nEventsProcessed);
                //  throw new RuntimeException("Couldn't get event date from trigger bank!");
            } else {
                eventDate = currentEventDate;
            }
        }
        // log start of run
        if( nEventsProcessed == 0 )
            LOGGER.info("startOfRun: run " + event.getRunNumber() + " event " + event.getEventNumber() + " processed " + nEventsProcessed +  " date " + eventDate.toString());
    
        if( !event.hasCollection(GenericObject.class, HeaderCollectionName) ) 
            return;
        
        List<GenericObject> headers = event.get(GenericObject.class, HeaderCollectionName);
        
        LOGGER.fine("Found " + headers.size() + " SvtHeaders in event " + event.getEventNumber() + " run " + event.getRunNumber());
        
        for(GenericObject header : headers ) {
            LOGGER.fine("nint " + header.getNInt());
            int roc = SvtOldHeaderDataInfo.getNum(header);
            
            // find the errors in the header
            int syncError = SvtEvioUtils.getSvtTailSyncErrorBit(SvtOldHeaderDataInfo.getTail(header));
            int oFError = SvtEvioUtils.getSvtTailOFErrorBit(SvtOldHeaderDataInfo.getTail(header));
            int skipCount = SvtEvioUtils.getSvtTailMultisampleSkipCount(SvtOldHeaderDataInfo.getTail(header));
            
            // check bits
            checkBitValueRange(oFError);
            checkBitValueRange(syncError);
            
            boolean printEverything = false;
            
            // print header errors to log
            if( syncError != 0) {
                LOGGER.info("syncError: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + " processed " + nEventsProcessed +  " date " + eventDate.toString()
                            + " roc " + roc);
                printEverything = true;
            }            
            if( oFError != 0) {
                LOGGER.info("oFError: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + " processed " + nEventsProcessed +  " date " + eventDate.toString()
                            + " roc " + roc);
                printEverything = true;
            }  
            for(int i=0; i < skipCount; ++i) {
                if( oFError != 0) {
                    LOGGER.info("skipCount: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + " processed " + nEventsProcessed +  " date " + eventDate.toString()
                                + " roc " + roc);
                }
                printEverything = true;
            }
           
            printEverything = false;
            
            // Check for multisample tail error bit
            int nMultisamples = SvtEvioUtils.getSvtTailMultisampleCount(SvtOldHeaderDataInfo.getTail(header));
            LOGGER.fine(nMultisamples + " multisamples");
            int multisampleErrorBits = 0;
            for(int iMultisample = 0; iMultisample != nMultisamples; ++iMultisample) {
                int multisampleHeader = SvtOldHeaderDataInfo.getMultisample(iMultisample, header);
                LOGGER.log(printEverything ? Level.INFO : Level.FINE, "found multisample tail: " + Integer.toHexString(multisampleHeader));
                int multisampleErrorBit = SvtEvioUtils.getErrorBitFromMultisampleHeader(multisampleHeader);
                checkBitValueRange(multisampleErrorBit);
                LOGGER.log(printEverything ? Level.INFO : Level.FINE, "found multisample tail error bit: " + multisampleErrorBit);
                if( multisampleErrorBit != 0) {
                    multisampleErrorBits++;
                    LOGGER.info("multisample tail error: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + eventDate.toString() 
                            + " roc " + roc + " feb " + SvtEvioUtils.getFebIDFromMultisampleTail(multisampleHeader) 
                            + " hybrid " + SvtEvioUtils.getFebHybridIDFromMultisampleTail(multisampleHeader)  
                            + " apv " + SvtEvioUtils.getApvFromMultisampleTail(multisampleHeader));
                }
            }
            
            // keep track how many headers have errors
            rceSyncErrorCount.fill(roc, syncError);
            rceOFErrorCount.fill(roc, oFError);
            rceSkipCount.fill(roc, skipCount);
            rceMultisampleErrorCount.fill(roc, multisampleErrorBits > 0 ? 1 : 0);
            if( syncError > 0) nRceSyncErrorCountN++;
            if( oFError > 0 ) nRceOFErrorCount++;
            if( skipCount > 0 ) nRceSkipCount++;
            if( multisampleErrorBits > 0 ) nRceMultisampleErrorCount++;
            nRceSvtHeaders++;
            
            
            
        }
        
        
        nEventsProcessed++;
    }
    
    private void checkBitValueRange(int val) {
        if( val != 0 && val != 1)
            throw new RuntimeException("invalid value for error bit " + val);
    }
    
    @Override
    protected void endOfData() {
        LOGGER.info("endOfData: processed " + nEventsProcessed +  " date " + eventDate.toString());
        LOGGER.info("nRceSvtHeaders " + nRceSvtHeaders);
        LOGGER.info("nRceSyncErrorCountN " + nRceSyncErrorCountN);
        LOGGER.info("nRceOFErrorCount " + nRceOFErrorCount);
        LOGGER.info("nRceSkipCount " + nRceSkipCount);
        LOGGER.info("nRceMultisampleErrorCount " + nRceMultisampleErrorCount);
        
    }
    

}
