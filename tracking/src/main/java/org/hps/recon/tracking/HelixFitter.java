package org.hps.recon.tracking;

import java.util.List;

import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.recon.tracking.seedtracker.ScatterAngle;

/**
 * HPS extension of the fitter algorithm to enable the use of local classes
 * 
 * @author Per Hansson <phansson@slac.stanford.edu>
 * @version $Id: $
 */
// FIXME: This class does not seem to override any of the superclass's behavior.
// Is it needed? --JM
public class HelixFitter extends org.lcsim.recon.tracking.seedtracker.HelixFitter {

    public HelixFitter(MaterialManager materialManager) {
        super(materialManager);
        // replace the multiple scattering to that given as parameter to be able
        // to use a local
        // version and not lcsim one
        _scattering = new MultipleScattering(materialManager);

    }

    public HelixFitter(MaterialManager materialManager, boolean doIterativeHelix) {
        super(materialManager);
        _scattering = new MultipleScattering(materialManager);
        setIterative(doIterativeHelix);
    }

    public void setIterative(boolean value) {
        ((MultipleScattering) _scattering).setIterativeHelix(value);
    }

    public void setDebug(boolean debug) {
        super.setDebug(debug);
        _scattering.setDebug(debug);
    }

    public List<ScatterAngle> FindScatters(HelicalTrackFit helix) {
        return _scattering.FindScatters(helix);
    }

}
