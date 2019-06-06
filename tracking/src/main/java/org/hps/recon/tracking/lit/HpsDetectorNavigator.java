package org.hps.recon.tracking.lit;

import java.util.List;

/**
 * A simple navigator which returns a list of detector planes between where the
 * track is now and the destination detector plane.
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HpsDetectorNavigator {

    HpsDetector _det;
    int[] indices = new int[2];

    public HpsDetectorNavigator(HpsDetector det) {
        _det = det;
    }

    //TODO add method which takes a DetectorPlane as target instead of z
    // but first let's see if we need it...
    public LitStatus FindIntersections(CbmLitTrackParam par, double zOut, List<DetectorPlane> inter) {
        double zCurrent = par.GetZ();
        _det.indicesInRange(zCurrent, zOut, indices);
        boolean downstream = zOut >= zCurrent;
        List<DetectorPlane> planes = _det.getPlanes();
        if (downstream) {
            for (int index = indices[0]; index <= indices[1]; ++index) {
                inter.add(planes.get(index));
            }
        } else {
            for (int index = indices[0]; index >= indices[1]; --index) {
                inter.add(planes.get(index));
            }
        }

        return LitStatus.kLITSUCCESS;
    }
}
