/**
 * 
 */
package org.hps.evio;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtEvioHeaderSyncErrorException extends SvtEvioHeaderException {

   public SvtEvioHeaderSyncErrorException(String message) {
        super(message);
    }

    public SvtEvioHeaderSyncErrorException(SvtEvioHeaderSyncErrorException e) {
       super(e);
    }

    

}
