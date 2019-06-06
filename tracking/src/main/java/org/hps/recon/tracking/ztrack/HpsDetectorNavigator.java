package org.hps.recon.tracking.ztrack;

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

    //TODO improve this method, currently will return ALL planes in the z range
    //TODO may want to split into top and bottom separately
    public Status FindIntersections(ZTrackParam par, double zOut, List<DetectorPlane> inter) {
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

        return Status.SUCCESS;
    }
}
