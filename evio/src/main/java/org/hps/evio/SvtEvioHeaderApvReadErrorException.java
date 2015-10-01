/**
 * 
 */
package org.hps.evio;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtEvioHeaderApvReadErrorException extends SvtEvioHeaderException {

   public SvtEvioHeaderApvReadErrorException(String message) {
        super(message);
    }

    public SvtEvioHeaderApvReadErrorException(SvtEvioHeaderApvReadErrorException e) {
       super(e);
    }

    

}
