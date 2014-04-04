package org.hps.recon.tracking;

/**
 * HPS extension of the fitter algorithm to enable the use of local classes
 * @author Per Hansson <phansson@slac.stanford.edu>
 * @version $Id: $
 */
// FIXME: This class does not seem to override any of the superclass's behavior. Is it needed? --JM
public class HelixFitter extends org.lcsim.recon.tracking.seedtracker.HelixFitter {

    public HelixFitter(MaterialManager materialManager) {
        super(materialManager);
        // replace the multiple scattering to that given as parameter to be able to use a local
        // version and not lcsim one
        _scattering = new MultipleScattering(materialManager);

    }

    public void setDebug(boolean debug) {
        super.setDebug(debug);
        _scattering.setDebug(debug);
    }

}
