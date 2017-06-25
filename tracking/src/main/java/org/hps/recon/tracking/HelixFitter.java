package org.hps.recon.tracking;

import java.util.List;

import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.recon.tracking.seedtracker.ScatterAngle;

/**
 * HPS extension of the fitter algorithm to enable the use of local classes
 * 
 * @author Per Hansson <phansson@slac.stanford.edu>
 * @author Miriam Diamond
 * @version $Id: $
 */

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

    public List<ScatterAngle> FindScatters(HelicalTrackFit helix) {
        return _scattering.FindScatters(helix);
    }

}
