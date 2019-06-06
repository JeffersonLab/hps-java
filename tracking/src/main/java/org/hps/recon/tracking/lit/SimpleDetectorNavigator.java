package org.hps.recon.tracking.lit;

import java.util.List;

/**
 * A simple navigator which returns a list of material planes between where the
 * track is now and the destination z location.
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class SimpleDetectorNavigator implements CbmLitGeoNavigator {

    SimpleDetector _det;
    int[] indices = new int[2];

    public SimpleDetectorNavigator(SimpleDetector det) {
        _det = det;
    }

    public LitStatus FindIntersections(CbmLitTrackParam par, double zOut, List<CbmLitMaterialInfo> inter) {
        double zCurrent = par.GetZ();
        _det.indicesInRange(zCurrent, zOut, indices);
        boolean downstream = zOut >= zCurrent;
        List<CbmLitMaterialInfo> planes = _det.getPlanes();
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
