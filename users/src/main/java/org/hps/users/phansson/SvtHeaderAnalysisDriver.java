/**
 * 
 */
package org.hps.users.phansson;

import hep.aida.IHistogram2D;
import hep.aida.IPlotter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.evio.SvtEvioReader;
import org.hps.evio.SvtEvioUtils;
import org.hps.readout.svt.SvtHeaderDataInfo;
import org.hps.util.BasicLogFormatter;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.log.LogUtil;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtHeaderAnalysisDriver extends Driver {

    private final AIDA aida = AIDA.defaultInstance();
    
    private final String HeaderCollectionName = "SvtHeaders"; 
    private final Logger logger = LogUtil.create(SvtHeaderAnalysisDriver.class.getSimpleName(), new BasicLogFormatter(), Level.INFO);
    //private final int nROC = 14;
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
    
    /**
     * 
     */
    public SvtHeaderAnalysisDriver() {
         
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
    
        if( !event.hasCollection(GenericObject.class, HeaderCollectionName) ) 
            return;
        
        List<GenericObject> headers = event.get(GenericObject.class, HeaderCollectionName);
        
        logger.fine("Found " + headers.size() + " SvtHeaders in event " + event.getEventNumber() + " run " + event.getRunNumber());
        
        for(GenericObject header : headers ) {
            logger.fine("nint " + header.getNInt());
            int roc = SvtHeaderDataInfo.getNum(header);
            int syncError = SvtEvioUtils.getSvtTailSyncErrorBit(SvtHeaderDataInfo.getTail(header));
            int oFError = SvtEvioUtils.getSvtTailOFErrorBit(SvtHeaderDataInfo.getTail(header));
            int skipCount = SvtEvioUtils.getSvtTailMultisampleSkipCount(SvtHeaderDataInfo.getTail(header));
            //int eventNr = SvtEvioUtils.getSvtDataEventCounter( SvtHeaderDataInfo.getHeader(header) );
             
            if( syncError != 0 && syncError != 1)
                throw new RuntimeException("invalid value for error bit " + syncError);
            rceSyncErrorCount.fill(roc, syncError);
            rceOFErrorCount.fill(roc, oFError);
            rceSkipCount.fill(roc, skipCount);
            
            
            int nMultisamples = SvtEvioUtils.getSvtTailMultisampleCount(SvtHeaderDataInfo.getTail(header));
            logger.fine(nMultisamples + " multisamples");
            int multisampleErrorBits = 0;
            for(int iMultisample = 0; iMultisample != nMultisamples; ++iMultisample) {
                int multisampleHeader = SvtHeaderDataInfo.getMultisample(iMultisample, header);
                logger.fine("found multisample header: " + Integer.toHexString(multisampleHeader));
                int multisampleErrorBit = SvtEvioUtils.getErrorBitFromMultisampleHeader(multisampleHeader);
                logger.fine("found multisample header error bit: " + multisampleErrorBit);
                if( multisampleErrorBit != 0) {
                    multisampleErrorBits++;
                    logger.info("multisample header error: run " + event.getRunNumber() + " event " + event.getEventNumber() 
                            + " roc " + roc + " feb " + SvtEvioUtils.getFebIDFromMultisampleTail(multisampleHeader) 
                            + " hybrid " + SvtEvioUtils.getFebHybridIDFromMultisampleTail(multisampleHeader)  
                            + " apv " + SvtEvioUtils.getApvFromMultisampleTail(multisampleHeader));
                }
            }
            rceMultisampleErrorCount.fill(roc, multisampleErrorBits > 0 ? 1 : 0);
            if( syncError > 0) nRceSyncErrorCountN++;
            if( oFError > 0 ) nRceOFErrorCount++;
            if( skipCount > 0 ) nRceSkipCount++;
            if( multisampleErrorBits > 0 ) nRceMultisampleErrorCount++;
            nRceSvtHeaders++;
            
            
            
        }
        
        
        
    }
    
    @Override
    protected void endOfData() {
        
        logger.info("nRceSvtHeaders " + nRceSvtHeaders);
        logger.info("nRceSyncErrorCountN " + nRceSyncErrorCountN);
        logger.info("nRceOFErrorCount " + nRceOFErrorCount);
        logger.info("nRceSkipCount " + nRceSkipCount);
        logger.info("nRceMultisampleErrorCount " + nRceMultisampleErrorCount);
        
    }
    

}
