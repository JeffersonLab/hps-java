/**
 * 
 */
package org.hps.evio;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtEvioHeaderOFErrorException extends SvtEvioHeaderException {

   public SvtEvioHeaderOFErrorException(String message) {
        super(message);
    }

    public SvtEvioHeaderOFErrorException(SvtEvioHeaderOFErrorException e) {
       super(e);
    }

    

}
