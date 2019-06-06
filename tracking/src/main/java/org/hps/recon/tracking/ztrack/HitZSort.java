package org.hps.recon.tracking.ztrack;

import java.util.Comparator;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HitZSort implements Comparator {

    private boolean _downstream;

    public HitZSort(boolean downstream) {
        _downstream = downstream;
    }

    /**
     * The compare function used for sorting. Comparison is done on hir z
     * position.
     *
     * @param obj1 Hit1
     * @param obj2 Hit2
     * @return
     * <ol>
     * <li> -1 if Hit1 > Hit2
     * <li> 0 if Hit1 = Hit2
     * <li> 1 if Hit1 < Hit2 </ol>
     */
    public int compare(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return 0;
        }
        Hit v1 = (Hit) obj1;
        Hit v2 = (Hit) obj2;
        if (_downstream) {
            if (v1.GetZ() > v2.GetZ()) {
                return 1;
            }
            if (v1.GetZ() < v2.GetZ()) {
                return -1;
            }
            return 0;
        } else {
            if (v1.GetZ() > v2.GetZ()) {
                return -1;
            }
            if (v1.GetZ() < v2.GetZ()) {
                return 1;
            }
            return 0;
        }
    }
}
