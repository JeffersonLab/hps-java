package org.hps.recon.tracking.kalman;

public class KalHit {
    SiModule module;
    Measurement hit;
    KalHit(SiModule module, Measurement hit) {
        this.module = module;
        this.hit = hit;
    }
    
    void print(String s) {
        System.out.format("Hit %s in layer %d, detector %d, value=%10.5f\n", s, module.Layer, module.detector, hit.v);
    }
}
