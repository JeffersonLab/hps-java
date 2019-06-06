package org.hps.recon.tracking.ztrack;

import java.util.List;

/**
 * A simple navigator which returns a list of material planes between where the
 * track is now and the destination z location.
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class SimpleDetectorNavigator implements GeoNavigator {

    SimpleDetector _det;
    int[] indices = new int[2];

    public SimpleDetectorNavigator(SimpleDetector det) {
        _det = det;
    }

    public Status FindIntersections(ZTrackParam par, double zOut, List<MaterialInfo> inter) {
        double zCurrent = par.GetZ();
        _det.indicesInRange(zCurrent, zOut, indices);
        boolean downstream = zOut >= zCurrent;
        List<MaterialInfo> planes = _det.getPlanes();
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
