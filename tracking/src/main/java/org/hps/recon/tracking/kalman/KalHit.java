package org.hps.recon.tracking.kalman;

import java.util.Comparator;

public class KalHit {
    SiModule module;
    Measurement hit;
    KalHit(SiModule module, Measurement hit) {
        this.module = module;
        this.hit = hit;
    }
    
    boolean isStereo() {
        return module.isStereo;
    }
    void print(String s) {
        int ntks = hit.tracks.size();
        if (s=="short") {
            int idx = module.hits.indexOf(hit);
            System.out.format(" (%d %d %d %d) ", module.Layer, module.detector, idx, ntks);
        } else {
            System.out.format("Hit %s in layer %d, detector %d, value=%10.5f, #tkrs=%d\n", s, module.Layer, module.detector, hit.v, ntks);
        }
    }
    
    // Comparator function for sorting hits on a track candidate
    static Comparator<KalHit> HitComparator = new Comparator<KalHit>() {
        public int compare(KalHit h1, KalHit h2) {
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
