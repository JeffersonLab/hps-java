/**
 * 
 */
package org.hps.evio;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtEvioHeaderException extends SvtEvioReaderException {

   public SvtEvioHeaderException(String message) {
        super(message);
    }

    public SvtEvioHeaderException(SvtEvioHeaderException e) {
       super(e);
    }

    

}
