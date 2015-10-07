/**
 * 
 */
package org.hps.users.phansson;

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
import org.hps.evio.SvtEvioUtils;
import org.hps.record.svt.SvtHeaderDataInfo;
import org.hps.util.BasicLogFormatter;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtHeaderAnalysisDriver extends Driver {

    private final AIDA aida = AIDA.defaultInstance();
    
    private final String HeaderCollectionName = "SvtHeaders"; 
    private final Logger logger = Logger.getLogger(SvtHeaderAnalysisDriver.class.getSimpleName());
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
    public SvtHeaderAnalysisDriver() {
         logger.setLevel(Level.INFO);
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
            logger.addHandler(fh);
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
                logger.info("Couldn't get event date from trigger bank for processed " + nEventsProcessed);
                //  throw new RuntimeException("Couldn't get event date from trigger bank!");
            } else {
                eventDate = currentEventDate;
            }
        }
        // log start of run
        if( nEventsProcessed == 0 )
            logger.info("startOfRun: run " + event.getRunNumber() + " event " + event.getEventNumber() + " processed " + nEventsProcessed +  " date " + eventDate.toString());
    
        if( !event.hasCollection(GenericObject.class, HeaderCollectionName) ) 
            return;
        
        List<GenericObject> headers = event.get(GenericObject.class, HeaderCollectionName);
        
        logger.info("Found " + headers.size() + " SvtHeaders in event " + event.getEventNumber() + " run " + event.getRunNumber());
        
        for(GenericObject header : headers ) {
            logger.info("1");
            SvtHeaderDataInfo headerInfo  = (SvtHeaderDataInfo) header;
            int roc = headerInfo.getNum();
            logger.info("process roc  " +  roc);
            
            int svtHeader = headerInfo.getHeader();
            int svtTail = headerInfo.getTail();
            
            logger.info("svtHeader " + Integer.toHexString(svtHeader) + " svtTail " + Integer.toHexString(svtTail));

            
            // find the errors in the header
            int syncError = SvtEvioUtils.getSvtTailSyncErrorBit(svtTail);
            int oFError = SvtEvioUtils.getSvtTailOFErrorBit(svtTail);
            int skipCount = SvtEvioUtils.getSvtTailMultisampleSkipCount(svtTail);
            
            // check bits
            checkBitValueRange(oFError);
            checkBitValueRange(syncError);
            
            // print header errors to log
            if( syncError != 0) {
                logger.info("syncError: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + " processed " + nEventsProcessed +  " date " + eventDate.toString()
                            + " roc " + roc);
            }            
            if( oFError != 0) {
                logger.info("oFError: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + " processed " + nEventsProcessed +  " date " + eventDate.toString()
                            + " roc " + roc);
            }  
            for(int i=0; i < skipCount; ++i) {
                if( oFError != 0) {
                    logger.info("skipCount: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + " processed " + nEventsProcessed +  " date " + eventDate.toString()
                                + " roc " + roc);
                }
            }
            
            int nMultiSamples = headerInfo.getNumberOfMultisampleHeaders();
            
            logger.info("svtHeader " + Integer.toHexString(svtHeader) + " svtTail " + Integer.toHexString(svtTail) + " nMultiSamples " + nMultiSamples);
            
            //if(1==1)    continue;

            int multisampleErrorBits = 0;

            for(int iMultisample = 0; iMultisample < nMultiSamples; ++iMultisample) {

                logger.info("iMultisample " + iMultisample);

                logger.info(headerInfo.toString());

                int[] multisampleHeader = headerInfo.getMultisampleHeader(iMultisample);
                int multisampleHeaderTail = SvtEvioUtils.getMultisampleTailWord(multisampleHeader);
                logger.info("found multisample tail: " + Integer.toHexString(multisampleHeaderTail));
                int multisampleErrorBit = SvtEvioUtils.getErrorBitFromMultisampleHeader(multisampleHeaderTail);
                checkBitValueRange(multisampleErrorBit);
                logger.info("found multisample tail error bit: " + multisampleErrorBit);
                if( multisampleErrorBit != 0) {
                    multisampleErrorBits++;
                    logger.info("multisample tail error: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + eventDate.toString() 
                            + " roc " + roc + " feb " + SvtEvioUtils.getFebIDFromMultisampleTail(multisampleHeaderTail) 
                            + " hybrid " + SvtEvioUtils.getFebHybridIDFromMultisampleTail(multisampleHeaderTail)  
                            + " apv " + SvtEvioUtils.getApvFromMultisampleTail(multisampleHeaderTail));
                }
                
                //if (1==1) break;

                
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
        logger.info("endOfData: processed " + nEventsProcessed +  " date " + eventDate.toString());
        logger.info("nRceSvtHeaders " + nRceSvtHeaders);
        logger.info("nRceSyncErrorCountN " + nRceSyncErrorCountN);
        logger.info("nRceOFErrorCount " + nRceOFErrorCount);
        logger.info("nRceSkipCount " + nRceSkipCount);
        logger.info("nRceMultisampleErrorCount " + nRceMultisampleErrorCount);
        
    }
    

}
