/**
 * 
 */
package org.hps.users.phansson;

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
public class SvtHeaderMetaData extends Driver {
    
    private static Logger logger = LogUtil.create(SvtHeaderMetaData.class.getSimpleName(), new BasicLogFormatter(), Level.INFO);

    /**
     * 
     */
    public SvtHeaderMetaData() {
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
        
        
        if( event.getIntegerParameters().containsKey("svt_event_headers")) {
            int[] svt_headers = event.getIntegerParameters().get("svt_event_headers");
            logger.info("found " + svt_headers.length + " SVT event headers:");
            for(int i=0; i< svt_headers.length; ++i) logger.info("header: " + svt_headers[i]);
        } else {
            logger.info("NO SVT event headers found");
        }

        
        if( event.getIntegerParameters().containsKey("svt_event_tails")) {
            int[] svt_tails = event.getIntegerParameters().get("svt_event_tails");
            logger.info("found " + svt_tails.length + " SVT event tails:");
            for(int i=0; i< svt_tails.length; ++i) logger.info("tail: " + svt_tails[i]);
        } else {
            logger.info("NO SVT event tails found");
        }

        for(Map.Entry<String, int[]> entry : event.getIntegerParameters().entrySet()) {
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
