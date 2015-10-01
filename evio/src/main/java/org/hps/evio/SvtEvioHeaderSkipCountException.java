/**
 * 
 */
package org.hps.evio;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtEvioHeaderSkipCountException extends SvtEvioHeaderException {

   public SvtEvioHeaderSkipCountException(String message) {
        super(message);
    }

    public SvtEvioHeaderSkipCountException(SvtEvioHeaderSkipCountException e) {
       super(e);
    }

    

}
