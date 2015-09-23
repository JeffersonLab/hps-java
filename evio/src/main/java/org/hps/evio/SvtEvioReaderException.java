/**
 * 
 */
package org.hps.evio;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtEvioReaderException extends Exception {

   
    /**
     * @param message
     */
    public SvtEvioReaderException(String message) {
        super(message);
    }

    public SvtEvioReaderException(SvtEvioReaderException e) {
       super(e);
    }

    

}
