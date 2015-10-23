/**
 * 
 */
package org.hps.users.phansson;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hps.analysis.trigger.util.TriggerDataUtils;
import org.hps.evio.AugmentedSvtEvioReader;
import org.hps.evio.SvtEventFlagger;
import org.hps.record.svt.SvtEventHeaderChecker;
import org.hps.record.svt.SvtEvioUtils;
import org.hps.record.svt.SvtHeaderDataInfo;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderException;
import org.hps.util.BasicLogFormatter;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtHeaderAnalysisDriver extends Driver {

    private final Logger logger = Logger.getLogger(SvtHeaderAnalysisDriver.class.getSimpleName());
    private int nEventsProcessed = 0;
    private Date eventDate = new Date(0);
    private int nEventsProcessedHeaderBad = 0;
    private int nEventsProcessedWithHeaderInfo = 0;
    private int nRceSyncErrorCountN = 0;
    private int nRceOFErrorCount = 0;
    private int nRceSkipCount = 0;
    private int nRceMultisampleErrorCount = 0;
    private int nRceSvtHeaders = 0;
    private String logFileName = "dummy.log";
    FileWriter fileWriter; 
    PrintWriter printWriter;
    private final String triggerBankCollectionName = "TriggerBank";
    Map<Integer, Map<String, Integer> > exceptionCount = new HashMap<Integer, Map<String,Integer>>(); 

    

    
    /**
     *  Default constructor
     */
    public SvtHeaderAnalysisDriver() {
         logger.setLevel(Level.INFO);
    }
    
    public void setLogFileName(String name) {
        this.logFileName = name;
    }
    
    
    @Override
    protected void detectorChanged(Detector detector) {
        
        try {
            FileHandler fh = new FileHandler(logFileName);
            fh.setFormatter(new BasicLogFormatter());
            logger.addHandler(fh);
        } catch (SecurityException | IOException e1) {
            e1.printStackTrace();
        }
        
//        final int n = SvtEvioReader.MAX_ROC_BANK_TAG+1 -  SvtEvioReader.MIN_ROC_BANK_TAG;
        
    }
    
    
    
    @Override
    protected void process(EventHeader event) {
        
        // try to get event date 
        // if no trigger bank use the last date found
        if(event.hasCollection(GenericObject.class, triggerBankCollectionName)) {
            Date currentEventDate = TriggerDataUtils.getEventTimeStamp(event, triggerBankCollectionName);
            if( currentEventDate == null) {
                logger.finest("Couldn't get event date from trigger bank for processed " + nEventsProcessed);
            } else {
                eventDate = currentEventDate;
            }
        }
        
        // log start of run
        if( nEventsProcessed == 0 )
            logger.info("startOfRun: run " + event.getRunNumber() + " event " + event.getEventNumber() + " processed " + nEventsProcessed +  " date " + eventDate.toString());
    
        // find header flag
        int headerFlag = -1;
        if( event.getIntegerParameters().containsKey("svt_event_header_good")) {
            int[] isOK = event.getIntegerParameters().get("svt_event_header_good");
            logger.fine("svt_event_header_good " +isOK[0] + " for run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + eventDate.toString() + " processed " + nEventsProcessed);
            headerFlag = isOK[0];
        }
        
        // print warning if there is no status flag in the event header!
        if( headerFlag == -1)
            logger.warning("No svt_event_header_good flag found for run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + eventDate.toString() + " processed " + nEventsProcessed);

        // print if the flag is bad
        if (headerFlag == 0) {
            logger.info("svt_event_header_good " + headerFlag + " for run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + eventDate.toString() + " processed " + nEventsProcessed);
            nEventsProcessedHeaderBad++;
        }
        
        
        List<SvtHeaderDataInfo> headerDataInfoList = SvtEventFlagger.getHeaderInfoToMetaData(event);
        
        logger.fine("found " + headerDataInfoList.size() + " SvtHeaderDataInfo in this event");
        
        // check that heder info is there if there was an error
        if( headerFlag == 0 && headerDataInfoList.size()==0)
            throw new RuntimeException("event has bad flag " + headerFlag + " but no SvtHeaderDataInfo");
        
        
        // Get all the exceptions
        List<SvtEvioHeaderException> exceptions = SvtEventHeaderChecker.checkSvtHeaders(headerDataInfoList);
        

        // Catalog and count them
        
        if(exceptions.size()>0) {
            nEventsProcessedWithHeaderInfo++;
            logger.info("found " + exceptions.size() + " SvtEvioHeaderExceptions in this event");
        }
        
        for(SvtEvioHeaderException e : exceptions) {
            String str = SvtEventHeaderChecker.getSvtEvioHeaderExceptionCompactMessage(e);
            logger.info("Run " + event.getRunNumber() + " event " + event.getEventNumber() + " " + str);
            String name = SvtEventHeaderChecker.getSvtEvioHeaderExceptionName(e);
            Integer roc = SvtEventHeaderChecker.getDAQComponentFromExceptionMsg(e, "num");
            if(!exceptionCount.containsKey(roc)) {
                Map<String, Integer> m = new HashMap<String,Integer>();
                exceptionCount.put(roc, m);
            }
            Map<String, Integer> typeCount = exceptionCount.get(roc);
            if( !typeCount.containsKey(name) ) typeCount.put(name, 0);
            int n = typeCount.get(name) + 1;
            typeCount.put(name, n);
        }
        
        // number of headers processed is just the size
        nRceSvtHeaders += exceptionCount.size();
        
        nEventsProcessed++;
        
        /*
        
        // count how many containers of SVT header info I see
        // they should only be there when there is an error
        // check that the event flag and this make sense
        int[] nRceErrorsPerEvent = {0,0,0};
        
        Map<Integer, Integer> rceHeaderCount = new HashMap<Integer, Integer>();

        // Get all the headers in the event
        for(Map.Entry<String, int[]> entry : event.getIntegerParameters().entrySet()) {
            
            int roc = SvtEventFlagger.getRocFromSvtHeaderName(entry.getKey());
            
            if( roc == -1) {
                logger.fine("skip this entry \"" + entry.getKey());
                continue;
            }
            
            logger.fine("processing entry \"" + entry.getKey()+ "\"" + " for roc "  + roc);
            
            // initialize count
            if (!rceHeaderCount.containsKey(roc)) 
                rceHeaderCount.put(roc, 0);
            rceHeaderCount.put(roc, rceHeaderCount.get(roc) + 1 );
            
            
            int[] value = entry.getValue();
           
            // log number of errors
            int syncError = 0;
            int oFError = 0;
            int skipCount = 0;
            int multisampleErrorBits = 0;
            
            
            // check if this is a header
            if(entry.getKey().contains("svt_event_header_roc")) {
                logger.fine("found SVT header \"" + Integer.toHexString(value[0]) + "\" for \"" + entry.getKey()+ "\"" + " roc + " + roc);
                nRceErrorsPerEvent[0]++;
            }
            
            
            // Analyze the SVT event tails
            if(entry.getKey().contains("svt_event_tail_roc")) {
                nRceErrorsPerEvent[1]++;

                logger.fine("found SVT tail \"" + Integer.toHexString(value[0]) + "\" for \"" + entry.getKey()+ "\""+ " roc + "  + roc );
                
                // find the SVT event error information in the header
                syncError = SvtEvioUtils.getSvtTailSyncErrorBit(value[0]);
                oFError = SvtEvioUtils.getSvtTailOFErrorBit(value[0]);
                skipCount = SvtEvioUtils.getSvtTailMultisampleSkipCount(value[0]);
                
                // check bits if applicable
                checkBitValueRange(oFError);
                checkBitValueRange(syncError);
                
                // print header errors to log
                if( syncError != 0)
                    logger.info("syncError: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + eventDate.toString() + " processed " + nEventsProcessed +  " date " + eventDate.toString() + " roc " + roc);
                
                if( oFError != 0) 
                    logger.info("oFError: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + eventDate.toString()  + " processed " + nEventsProcessed +  " date " + eventDate.toString() + " roc " + roc);
                
                for(int i=0; i < skipCount; ++i) {
                    if( oFError != 0) {
                        logger.info("skipCount: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + eventDate.toString()  + " processed " + nEventsProcessed +  " date " + eventDate.toString() + " roc " + roc);
                    }
                }
            }

            
            if(entry.getKey().contains("svt_multisample_headers_roc")) {

                nRceErrorsPerEvent[2]++;

                logger.fine("found " + value.length + " SVT multisample headers:");

                
                for(int i=0; i< value.length/4; ++i) {
                    
                    // extract the headers
                    int[] multisampleHeader = new int[4];
                    System.arraycopy(value, i*4, multisampleHeader, 0, multisampleHeader.length);
                    String str = "multisample header " + Integer.toString(i);                
                    for(int j=0; j<4; ++j) str += " " + Integer.toHexString(multisampleHeader[j]);
                    logger.fine(str);
                    
                    // get the multisample tail word
                    int multisampleHeaderTail = SvtEvioUtils.getMultisampleTailWord(multisampleHeader);
                    logger.fine("found multisample tail: " + Integer.toHexString(multisampleHeaderTail));

                    // get the error bit
                    int multisampleErrorBit = SvtEvioUtils.getErrorBitFromMultisampleHeader(multisampleHeaderTail);
                    checkBitValueRange(multisampleErrorBit);
                    logger.fine("found multisample tail error bit: " + multisampleErrorBit);
                    
                    if( multisampleErrorBit != 0) {
                        multisampleErrorBits++;
                        logger.info("multisample tail error: run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + eventDate.toString() 
                                + " roc " + roc + " feb " + SvtEvioUtils.getFebIDFromMultisampleTail(multisampleHeaderTail) 
                                + " hybrid " + SvtEvioUtils.getFebHybridIDFromMultisampleTail(multisampleHeaderTail)  
                                + " apv " + SvtEvioUtils.getApvFromMultisampleTail(multisampleHeaderTail));
                    }
                }
            }
            
            // keep track how many headers have errors
            if( syncError > 0) nRceSyncErrorCountN++;
            if( oFError > 0 ) nRceOFErrorCount++;
            if( skipCount > 0 ) nRceSkipCount++;
            if( multisampleErrorBits > 0 ) nRceMultisampleErrorCount++;
        }
        // check that the counts make sense
        if(! ( nRceErrorsPerEvent[0] == nRceErrorsPerEvent[1] && nRceErrorsPerEvent[0] == nRceErrorsPerEvent[2])) 
            throw new RuntimeException("counts of header, tail and multisample headers are crazy: " + nRceErrorsPerEvent[0] + " vs " +nRceErrorsPerEvent[1] + " vs " + nRceErrorsPerEvent[2]);
                
        if( headerFlag == 0 && nRceErrorsPerEvent[0]==0)
            throw new RuntimeException("event has bad flag " + headerFlag + " but counts of header, tail and multisample headers are zero?: " + nRceErrorsPerEvent[0] + " vs " +nRceErrorsPerEvent[1] + " vs " + nRceErrorsPerEvent[2]);
        
        
        for(Map.Entry<Integer, Integer> entry : rceHeaderCount.entrySet())
            logger.fine("ROC " + entry.getKey() + " count " + entry.getValue());
        */

      
    }
    
    private void checkBitValueRange(int val) {
        if( val != 0 && val != 1)
            throw new RuntimeException("invalid value for error bit " + val);
    }
    
    @Override
    protected void endOfData() {
        logger.info("nEventsProcessed " + nEventsProcessed);
        logger.info("nEventsProcessedHeaderBad " + nEventsProcessedHeaderBad);
        logger.info("nEventsProcessedWithHeaderInfo " + nEventsProcessedWithHeaderInfo);
        logger.info("nRceSvtHeaders " + nRceSvtHeaders);
        //Map<Integer, Map<String, Integer> > exceptionCount = new HashMap<Integer, Map<String,Integer>>(); 
        
        //print roc's with errors
        String rocs = "";
        for(Integer roc : exceptionCount.keySet()) rocs += roc + " ";
        logger.info("There were " + exceptionCount.keySet().size() + " rocs with any error: " + rocs);
        
        //print type of errors
        Map<String, Integer> errorTypeCount = new HashMap<String,Integer>(); 
        for(Map.Entry<Integer, Map<String, Integer> > rCount : exceptionCount.entrySet()) {
            Map<String,Integer> m = rCount.getValue();
            for(Map.Entry<String,Integer> entry : m.entrySet()) {
                if( !errorTypeCount.containsKey(entry.getKey()) ) {
                    errorTypeCount.put(entry.getKey(),0);
                }
                int n = errorTypeCount.get(entry.getKey()) + 1;
                errorTypeCount.put(entry.getKey(), n);
            }
        }
        logger.info("There are " + errorTypeCount.entrySet().size()+ " type of error occuring in the ROCs:");
        for(Map.Entry<String,Integer> entry : errorTypeCount.entrySet()) {
            String rocsWithError = "";
            for(Map.Entry<Integer, Map<String, Integer> > rCount : exceptionCount.entrySet()) {
                if(rCount.getValue().containsKey(entry.getKey())) {
                    rocsWithError += rCount.getKey() + ":" + rCount.getValue().get(entry.getKey()) + " ";
                }
            }
            logger.info(entry.getKey() + " " + entry.getValue() + " (individual roc counts " + rocsWithError + " )");
        }
        
//        logger.info("nRceSyncErrorCountN " + nRceSyncErrorCountN);
//        logger.info("nRceOFErrorCount " + nRceOFErrorCount);
//        logger.info("nRceSkipCount " + nRceSkipCount);
//        logger.info("nRceMultisampleErrorCount " + nRceMultisampleErrorCount);
        
    }
    

}
