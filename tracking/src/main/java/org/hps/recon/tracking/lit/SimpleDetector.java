package org.hps.recon.tracking.lit;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * A class which represents a simple detector as a list of CbmLitMaterialInfo
 * elements. These are assumed to be planar elements and are sorted in
 * increasing z position.
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class SimpleDetector {

    // use a TreeSet to make sure there are no duplicates, 
    // and also to naturally order in z, since CbmLitMaterialInfo extends Comparable
    TreeSet<CbmLitMaterialInfo> det = new TreeSet<CbmLitMaterialInfo>();
    // use a List for fast indexing
    List<CbmLitMaterialInfo> detList;

    double zMin = 9999.;
    double zMax = -9999.;

    // temporary object
    CbmLitMaterialInfo probe = new CbmLitMaterialInfo();
    CbmLitMaterialInfo response;

    public void addDetectorPlane(CbmLitMaterialInfo p) {
        if (p.GetZpos() < zMin) {
            zMin = p.GetZpos();
        }
        if (p.GetZpos() > zMax) {
            zMax = p.GetZpos();
        }
        det.add(p);
        //update the detList
        detList = new ArrayList<CbmLitMaterialInfo>(det);
    }

    public List<CbmLitMaterialInfo> getPlanes() {
        return detList;
    }

    public double[] getZPositions() {
        double[] z = new double[detList.size()];
        int index = 0;
        for (CbmLitMaterialInfo m : detList) {
            z[index++] = m.GetZpos();
        }
        return z;
    }

    /**
     * Returns the indices in the List of detector elements which fall within
     * the given range first index is strictly greater than that of zStart
     * second index includes zEnd
     *
     * @param zStart z position at which to start searching
     * @param zEnd z position at end of range
     * @param indices array returning first and last indices of detector
     * elements found in range
     */
    public void indicesInRange(double zStart, double zEnd, int[] indices) {
        //TODO put in some sanity checks to make sure range is within the detector.
        if (zStart < zEnd) {
            probe.SetZpos(zStart);
            response = det.higher(probe);
            indices[0] = detList.indexOf(response);
            probe.SetZpos(zEnd);
            response = det.floor(probe);
            indices[1] = detList.indexOf(response);
        } else {
            probe.SetZpos(zStart);
            response = det.lower(probe);
            indices[0] = detList.indexOf(response);
            probe.SetZpos(zEnd);
            response = det.ceiling(probe);
            indices[1] = detList.indexOf(response);
        }
    }

    public double zMin() {
        return zMin;
    }

    public double zMax() {
        return zMax;
    }
    //TODO add convenience methods to return planes within a z range, etc. 
}
