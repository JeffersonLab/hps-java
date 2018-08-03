package org.hps.record;

public class StandardCuts {
    // max number of hits a track can share with other tracks
    private int maxSharedHitsPerTrack;
    // max GBL chisq/dof for track
    private double maxTrackChisqNorm;
    // max (absolute) chisq for track-cluster match for recon particle 
    private double maxMatchChisq;
    // max time diff [ns] between track and cluster time for recon particle
    private double maxMatchDt;
    // max vertex momentum (magnitude)
    private double maxVertexP;
    // max (absolute) chisq for vertex fit
    private double maxVertexChisq;
    // max time diff [ns] between the two recon particle clusters in vertex
    private double maxVertexClusterDt;
    
    // max momentum for good electron recon particle
    private double maxElectronP;
    // min momentum sum of tracks in good Moller vertex
    private double minMollerP;
    // max momentum sum of tracks in good Moller vertex
    private double maxMollerP;
    
    private double trackClusterTimeOffset;
    
    
    // helpers
    private boolean maxElectronPset = false;
    private boolean minMollerPset = false;
    private boolean maxMollerPset = false;
    private boolean maxVertexPset = false;
    private boolean maxMatchChisqset = false;
    
    public double getTrackClusterTimeOffset() {
        return trackClusterTimeOffset;
    }
    
    public void setMaxSharedHitsPerTrack(int input) {
        maxSharedHitsPerTrack = input;
    }
    public int getMaxSharedHitsPerTrack() {
        return maxSharedHitsPerTrack;
    }
    
    public void setMaxTrackChisqNorm(double input) {
        maxTrackChisqNorm = input;
    }
    public double getMaxTrackChisqNorm() {
        return maxTrackChisqNorm;
    }
    
    public void setMaxMatchChisq(double input) {
        maxMatchChisq = input;
        maxMatchChisqset = true;
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
    
    public void setMaxVertexChisq(double input) {
        maxVertexChisq = input;
    }
    public double getMaxVertexChisq() {
        return maxVertexChisq;
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
        maxTrackChisqNorm = 6.0;
        maxMatchChisq = 10.0;
        maxMatchDt = 6.0;
        maxVertexChisq = 75.0;
        maxVertexClusterDt = 2.0;
        
        maxElectronPset = false;
        minMollerPset = false;
        maxMollerPset = false;
        maxVertexPset = false;
        maxMatchChisqset = false;
        
        changeBeamEnergy(ebeam);
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
        if (!maxMatchChisqset) {
            if (ebeam < 2)
                trackClusterTimeOffset = 43;
            else
                trackClusterTimeOffset = 55;
        }
    }
    
    public StandardCuts() {
        new StandardCuts(1.056);
    }
}
