/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking;


/**
 * HPS extension of the fitter algorithm to enable the use of local classes
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 * @version $Id: HelixFitter.java,v 1.2 2013/11/07 03:54:58 phansson Exp $ $Date: 2013/11/07 03:54:58 $ $Author: phansson $ 
 */
public class HelixFitter extends org.lcsim.recon.tracking.seedtracker.HelixFitter {
    
    public HelixFitter(MaterialManager materialManager) {
        super(materialManager);
        //replace the multiple scattering to that given as parameter to be able to use a local version and not lcsim one
        _scattering = new MultipleScattering(materialManager);

    }
    
    public void setDebug(boolean debug) {
        super.setDebug(debug);
        _scattering.setDebug(debug);
    } 
    


}
