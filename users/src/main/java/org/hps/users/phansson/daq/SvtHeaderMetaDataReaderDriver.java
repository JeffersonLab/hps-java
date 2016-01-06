/**
 * 
 */
package org.hps.users.phansson.daq;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.util.BasicLogFormatter;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.log.LogUtil;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtHeaderMetaDataReaderDriver extends Driver {
    
    private static Logger logger = LogUtil.create(SvtHeaderMetaDataReaderDriver.class.getSimpleName(), new BasicLogFormatter(), Level.INFO);

    /**
     * 
     */
    public SvtHeaderMetaDataReaderDriver() {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    protected void process(EventHeader event) {
        
        
        if( event.getIntegerParameters().containsKey("svt_event_header_good")) {
            int[] isOK = event.getIntegerParameters().get("svt_event_header_good");
            
            logger.info("found SVT event header FLAG: " + isOK[0]);

        } else {
            logger.info("NO SVT event header FLAG found");
        }
        
        for(Map.Entry<String, int[]> entry : event.getIntegerParameters().entrySet()) {
            
            if(entry.getKey().contains("svt_event_header_roc")) {
                int[] svt_tails = entry.getValue();
                logger.info("found SVT header \"" + Integer.toHexString(svt_tails[0]) + "\" for \"" + entry.getKey()+ "\"");
            }

            if(entry.getKey().contains("svt_event_tail_roc")) {
                int[] svt_tails = entry.getValue();
                logger.info("found SVT tail \"" + Integer.toHexString(svt_tails[0]) + "\" for \"" + entry.getKey()+ "\"");
            }

            if(entry.getKey().contains("svt_multisample_headers_roc")) {
                int[] svt_tails = entry.getValue();
                logger.info("found " + svt_tails.length + " SVT multisample headers for \"" + entry.getKey()+ "\":");
                for(int i=0; i< svt_tails.length/4; ++i) {
                    String str = "multisample header " + Integer.toString(i);                
                    for(int j=0; j<4; ++j)
                            str += " " + Integer.toHexString(svt_tails[i*4+j]);
                    logger.info(str);
                }
            }
        }
                
        
    }

}
