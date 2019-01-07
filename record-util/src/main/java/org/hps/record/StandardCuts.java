package org.hps.record;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

public class StandardCuts {
    // max number of hits a track can share with other tracks
    private int maxSharedHitsPerTrack;
    // max GBL chisq prob for all tracks 
    private double maxTrackChisqProb;
    // max GBL raw chisq for various numbers of hits
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
    // this should be set in data according to beam energy, but may be different in MC
    private double trackClusterTimeOffset;
    
    
    // members that don't involve chi2 can only be set ONCE
    private boolean maxElectronPset = false;
    private boolean minMollerPset = false;
    private boolean maxMollerPset = false;
    private boolean maxVertexPset = false;
    private boolean OffsetSet = false;
    private boolean maxSharedHitsPerTrackSet = false;
    private boolean maxMatchDtSet = false;
    private boolean maxVertexClusterDtSet = false;
    
    
    public double getTrackClusterTimeOffset() {
        return trackClusterTimeOffset;
    }
    
    public void setTrackClusterTimeOffset(double input) {
        if (!OffsetSet) {
            trackClusterTimeOffset = input;
            OffsetSet = true;
        }
    }
    
    public void setMaxSharedHitsPerTrack(int input) {
        if (!maxSharedHitsPerTrackSet) {
            maxSharedHitsPerTrack = input;
            maxSharedHitsPerTrackSet = true;
        }
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
        if (!maxVertexPset) {
            maxVertexP = input;
            maxVertexPset = true;
        }
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
        if (!maxMatchDtSet) {
            maxMatchDt = input;
            maxMatchDtSet = true;
        }
    }
    public double getMaxMatchDt() {
        return maxMatchDt;
    }
    
    public void setMaxVertexClusterDt(double input) {
        if (!maxVertexClusterDtSet) {
            maxVertexClusterDt = input;
            maxVertexClusterDtSet = true;
        }
    }
    public double getMaxVertexClusterDt() {
        return maxVertexClusterDt;
    }
    
    public void setMaxElectronP(double input) {
        if (!maxElectronPset) {
            maxElectronP = input;
            maxElectronPset = true;
        }
    }
    public double getMaxElectronP() {
        return maxElectronP;
    }
    
    public void setMaxMollerP(double input) {
        if (!maxMollerPset) {
            maxMollerP = input;
            maxMollerPset = true;
        }
    }
    public double getMaxMollerP() {
        return maxMollerP;
    }
    
    public void setMinMollerP(double input) {
        if (!minMollerPset) {
            minMollerP = input;
            minMollerPset = true;
        }
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
        
        maxElectronP = 0.75*ebeam;
        minMollerP = 0.8*ebeam;
        maxMollerP = 1.2*ebeam;
        maxVertexP = 1.2*ebeam;
        if (ebeam < 2)
            trackClusterTimeOffset=43;
        else
            trackClusterTimeOffset=55;
    }
    
    public void changeChisqTrackProb(double prob) {        
        for (int dof : maxTrackChisq.keySet()) {
            maxTrackChisq.put(dof, new ChiSquaredDistribution(dof).inverseCumulativeProbability(1.0-prob));
        }
    }
    
    public void changeBeamEnergy(double ebeam) {
        setMaxElectronP(0.75*ebeam);
        setMinMollerP(0.8*ebeam);
        setMaxMollerP(1.2*ebeam);
        setMaxVertexP(1.2*ebeam);
        if (ebeam < 2)
            setTrackClusterTimeOffset(43);
        else
            setTrackClusterTimeOffset(55);
    }
    
    public StandardCuts() {
        this(1.056);
    }
}
