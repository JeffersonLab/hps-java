package org.hps.recon.tracking.kalman;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Relationships between hits, silicon modules, and track candidates
 */
class KalHit {
    SiModule module;                    // The silicon module in which the hit is found
    Measurement hit;                    // The measurement object corresponding to the hit
    Set<TrackCandidate> tkrCandidates;  // List of candidate tracks that incorporate this hit
    
    /** 
     * Kalman hit constructor
     * @param module       silicon module object
     * @param hit          measurement hit object
     */
    KalHit(SiModule module, Measurement hit) {
        this.module = module;
        this.hit = hit;
        tkrCandidates = new HashSet<TrackCandidate>();
    }
    
    /**
     * Is the hit on a stereo layer?
     * @return     true or false
     */
    boolean isStereo() {
        return module.isStereo;
    }
    
    /**
     * Debug printout of a given Kalman hit
     * @param s    Arbitrary string to identify the printout
     */
    void print(String s) {
        System.out.format("%s", this.toString(s));
    }
    
    /**
     * Debug printout to a string for a given Kalman hit
     * @param s     Arbitrary string, to identify the printout
     * @return      The full string, in printable format.
     */
    String toString(String s) {
        int ntks = hit.tracks.size();
        String str;
        if (s=="short") {
            int idx = module.hits.indexOf(hit);
            if (module.isStereo) {
                str = String.format(" {%d %d %d %d %5.1f %6.3f} ", module.Layer, module.detector, idx, ntks, hit.time, hit.energy);
            } else {
                str = String.format(" (%d %d %d %d %5.1f %6.3f) ", module.Layer, module.detector, idx, ntks, hit.time, hit.energy);
            }
        } else {
            str = String.format("Hit %s in layer %d, detector %d, hit %d, value=%10.5f, #tkrs=%d, candidate chi2=", s, module.Layer, module.detector, module.hits.indexOf(hit), hit.v, ntks);
            for (TrackCandidate cnd : tkrCandidates) {
                str=str+String.format(" (%7.3f %d)", cnd.chi2s, cnd.hashCode());
            }
            str=str+String.format("\n");
        }
        return str;
    }
    
    /**
     *  Comparator function for sorting hits on a track candidate
     */
    static Comparator<KalHit> HitComparator = new Comparator<KalHit>() {
        public int compare(KalHit h1, KalHit h2) {
            if (h1 == h2) return 0;
            int lyr1 = h1.module.Layer;
            int lyr2 = h2.module.Layer;
            if (lyr1 < lyr2) {
                return -1;
            } else if (lyr2 < lyr1) {
                return 1;
            } else {
                int d1 = h1.module.detector;
                int d2 = h2.module.detector;
                if (d1 < d2) {
                    return -1;
                } else if (d1 > d2) {
                    return 1;
                } else {
                    int i1 = h1.module.hits.indexOf(h1.hit);
                    int i2 = h2.module.hits.indexOf(h2.hit);
                    if (i1 < i2) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            }
        }
    };
}
