package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.Collection;
import org.hps.conditions.deprecated.HPSSVTCalibrationConstants.ChannelConstants;
import org.lcsim.event.RawTrackerHit;

/**
 * 
 * @author Matt Graham
 */
// FIXME: Is there some other description besides "dumb" that could be used in this class name?
// --JM
// TODO: Add class documentation.
public class DumbShaperFit implements ShaperFitAlgorithm {

    public DumbShaperFit() {
    }

    @Override
    public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, ChannelConstants constants) {
        short[] adcVals = rth.getADCValues();
        return this.fitShape(adcVals, constants);
    }

    public Collection<ShapeFitParameters> fitShape(short[] adcVals, ChannelConstants constants) {
        ShapeFitParameters fitresults = new ShapeFitParameters();
        double[] pedSub = { -99.0, -99.0, -99.0, -99.0, -99.0, -99.0 };
        double maxADC = -99999;
        int iMax = -1;
        double t0 = -999;
        for (int i = 0; i < 6; i++) {
            pedSub[i] = adcVals[i] - constants.getPedestal();
            if (pedSub[i] > maxADC) {
                maxADC = pedSub[i];
                iMax = i;
            }
        }
        if (iMax > 0 && iMax < 5) {
            t0 = (pedSub[iMax - 1] * 24.0 * (iMax - 1) + pedSub[iMax] * 24.0 * (iMax) + pedSub[iMax + 1] * 24.0 * (iMax + 1)) / (pedSub[iMax - 1] + pedSub[iMax] + pedSub[iMax + 1]);
        } else if (iMax == 0) {
            t0 = (pedSub[iMax] * 24.0 * (iMax) + pedSub[iMax + 1] * 24.0 * (iMax + 1)) / (pedSub[iMax] + pedSub[iMax + 1]);
        } else if (iMax == 5) {
            t0 = (pedSub[iMax] * 24.0 * (iMax) + pedSub[iMax - 1] * 24.0 * (iMax - 1)) / (pedSub[iMax - 1] + pedSub[iMax]);
        }

        // mg...put in a cut here to make sure pulse shape is reasonable
        // if not, set t0 to -99 (which will fail the later t0>0 cut
        if (iMax == 0 || iMax == 5)
            t0 = -99;
        // make sure it goes up below iMax
        for (int i = 0; i < iMax; i++) {
            if (pedSub[i + 1] < pedSub[i])
                t0 = -99;
        }
        // ...and down below iMax
        for (int i = iMax; i < 5; i++) {
            if (pedSub[i + 1] > pedSub[i])
                t0 = -99;
        }

        fitresults.setAmp(maxADC);
        fitresults.setT0(t0);

        ArrayList<ShapeFitParameters> fits = new ArrayList<ShapeFitParameters>();
        fits.add(fitresults);
        return fits;
    }
}
