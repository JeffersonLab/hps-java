package org.hps.record;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

public class StandardCuts {
    // max number of hits a track can share with other tracks
    private int maxSharedHitsPerTrack;
    // max GBL chisq for 6-hit track
    private Map<Integer, Double> maxTrackChisq;
    // max (absolute) chisq for track-cluster match for recon particle 
    private double maxMatchChisq;
    // max time diff [ns] between track and cluster time for recon particle
    private double maxMatchDt;
    // max vertex momentum (magnitude)
    private double maxVertexP;
    // max chisq prob for V0 vertex fit
    private double minVertexChisqProb;
    // max chisq prob for vertex fit
    private double minMollerChisqProb;
    // max time diff [ns] between the two recon particle clusters in vertex
    private double maxVertexClusterDt;
    
    // max momentum for good electron recon particle
    private double maxElectronP;
    // min momentum sum of tracks in good Moller vertex
    private double minMollerP;
    // max momentum sum of tracks in good Moller vertex
    private double maxMollerP;
    
    private double trackClusterTimeOffset;
    private double maxTrackChisqProb;
    
    // helpers
    private boolean maxElectronPset = false;
    private boolean minMollerPset = false;
    private boolean maxMollerPset = false;
    private boolean maxVertexPset = false;
    private boolean OffsetSet = false;
    
    public double getTrackClusterTimeOffset() {
        return trackClusterTimeOffset;
    }
    
    public void setTrackClusterTimeOffset(double input) {
        trackClusterTimeOffset = input;
        OffsetSet = true;
    }
    
    public void setMaxSharedHitsPerTrack(int input) {
        maxSharedHitsPerTrack = input;
    }
    public int getMaxSharedHitsPerTrack() {
        return maxSharedHitsPerTrack;
    }
    
    public void setMaxTrackChisq(int nhits, double input) {
        int dof = nhits*2-5;
        maxTrackChisq.put(dof, input);
    }
    
    public double getMaxTrackChisq(int nhits) {
        int dof = nhits*2-5;
        if (!maxTrackChisq.containsKey(dof)) {
            maxTrackChisq.put(dof, new ChiSquaredDistribution(dof).inverseCumulativeProbability(1.0-maxTrackChisqProb));
        }
        return maxTrackChisq.get(dof);
    }
    
    public void setMaxMatchChisq(double input) {
        maxMatchChisq = input;
    }
    public double getMaxMatchChisq() {
        return maxMatchChisq;
    }
    
    public void setMaxVertexP(double input) {
        maxVertexP = input;
        maxVertexPset = true;
    }
    public double getMaxVertexP() {
        return maxVertexP;
    }
    
    public void setMinVertexChisqProb(double input) {
        minVertexChisqProb = input;
    }
    public void setMinMollerChisqProb(double input) {
        minMollerChisqProb = input;
    }
    
    public double getMinVertexChisqProb() {
        return minVertexChisqProb;
    }
    public double getMinMollerChisqProb() {
        return minMollerChisqProb;
    }
    
    public void setMaxMatchDt(double input) {
        maxMatchDt = input;
    }
    public double getMaxMatchDt() {
        return maxMatchDt;
    }
    
    public void setMaxVertexClusterDt(double input) {
        maxVertexClusterDt = input;
    }
    public double getMaxVertexClusterDt() {
        return maxVertexClusterDt;
    }
    
    public void setMaxElectronP(double input) {
        maxElectronP = input;
        maxElectronPset = true;
    }
    public double getMaxElectronP() {
        return maxElectronP;
    }
    
    public void setMaxMollerP(double input) {
        maxMollerP = input;
        maxMollerPset = true;
    }
    public double getMaxMollerP() {
        return maxMollerP;
    }
    
    public void setMinMollerP(double input) {
        minMollerP = input;
        minMollerPset = true;
    }
    public double getMinMollerP() {
        return minMollerP;
    }
    
    public StandardCuts(double ebeam) {
        maxSharedHitsPerTrack = 5;
        maxMatchChisq = 10.0;
        maxMatchDt = 6.0;
        maxVertexClusterDt = 2.0;
        minVertexChisqProb = 0.00001;
        minMollerChisqProb = 0.00001;
        maxTrackChisqProb = 0.00001;
        
        maxElectronPset = false;
        minMollerPset = false;
        maxMollerPset = false;
        maxVertexPset = false;
        OffsetSet = false;
        
        maxTrackChisq = new HashMap<Integer, Double>();
        maxTrackChisq.put(5, new ChiSquaredDistribution(5).inverseCumulativeProbability(1.0-maxTrackChisqProb));
        maxTrackChisq.put(7, new ChiSquaredDistribution(7).inverseCumulativeProbability(1.0-maxTrackChisqProb));
        changeBeamEnergy(ebeam);
    }
    
    public void changeChisqTrackProb(double prob) {        
        for (int dof : maxTrackChisq.keySet()) {
            maxTrackChisq.put(dof, new ChiSquaredDistribution(dof).inverseCumulativeProbability(1.0-prob));
        }
    }
    
    public void changeBeamEnergy(double ebeam) {
        if (!maxElectronPset)
            maxElectronP = 0.75*ebeam;
        if (!minMollerPset)
            minMollerP = 0.8*ebeam;
        if (!maxMollerPset)
            maxMollerP = 1.2*ebeam;
        if (!maxVertexPset)
            maxVertexP = 1.2*ebeam;
        if (!OffsetSet) {
            if (ebeam < 2)
                trackClusterTimeOffset = 43;
            else
                trackClusterTimeOffset = 55;
        }
    }
    
    public StandardCuts() {
        this(1.056);
    }
}
