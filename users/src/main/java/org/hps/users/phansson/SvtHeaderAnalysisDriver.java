/**
 * 
 */
package org.hps.users.phansson;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hps.analysis.trigger.util.TriggerDataUtils;
import org.hps.record.svt.SvtEvioUtils;
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
    private int nRceSyncErrorCountN = 0;
    private int nRceOFErrorCount = 0;
    private int nRceSkipCount = 0;
    private int nRceMultisampleErrorCount = 0;
    private int nRceSvtHeaders = 0;
    private String logFileName = "dummy.log";
    FileWriter fileWriter; 
    PrintWriter printWriter;
    private final String triggerBankCollectionName = "TriggerBank";
    private static final Pattern rocIdPattern  = Pattern.compile("svt_.*_roc(\\d+)");

    
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
    
    
    private int getRoc(String seq) {
        Matcher m = rocIdPattern.matcher(seq);
        if(m == null) 
            throw new RuntimeException("null matcher, don't think this should happen");
        if( !m.matches() ) 
            return -1;
        else
            return Integer.parseInt( m.group(1) );
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
        if (headerFlag == 0) 
            logger.info("svt_event_header_good " + headerFlag + " for run " + event.getRunNumber() + " event " + event.getEventNumber() + " date " + eventDate.toString() + " processed " + nEventsProcessed);
                   
        
        Map<Integer, Integer> rceHeaderCount = new HashMap<Integer, Integer>();

        // Get all the headers in the event
        for(Map.Entry<String, int[]> entry : event.getIntegerParameters().entrySet()) {
            
            int roc = getRoc(entry.getKey());
            
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
            if(entry.getKey().contains("svt_event_header_roc"))
                logger.fine("found SVT header \"" + Integer.toHexString(value[0]) + "\" for \"" + entry.getKey()+ "\"" + " roc + " + roc);
            
            
            // Analyze the SVT event tails
            if(entry.getKey().contains("svt_event_tail_roc")) {

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
        
        
        for(Map.Entry<Integer, Integer> entry : rceHeaderCount.entrySet())
            logger.fine("ROC " + entry.getKey() + " count " + entry.getValue());

        // number of headers processed is just the size
        nRceSvtHeaders += rceHeaderCount.size();
        
        nEventsProcessed++;
    }
    
    private void checkBitValueRange(int val) {
        if( val != 0 && val != 1)
            throw new RuntimeException("invalid value for error bit " + val);
    }
    
    @Override
    protected void endOfData() {
        logger.info("endOfData: processed " + nEventsProcessed +  "events date " + eventDate.toString());
        logger.info("nRceSvtHeaders " + nRceSvtHeaders);
        logger.info("nRceSyncErrorCountN " + nRceSyncErrorCountN);
        logger.info("nRceOFErrorCount " + nRceOFErrorCount);
        logger.info("nRceSkipCount " + nRceSkipCount);
        logger.info("nRceMultisampleErrorCount " + nRceMultisampleErrorCount);
        
    }
    

}
