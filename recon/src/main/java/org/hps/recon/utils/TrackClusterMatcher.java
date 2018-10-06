package org.hps.recon.utils;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.FieldMap;

/**
 * Utility used to determine if a track and cluster are matched.
 *
 * @author <a href="mailto:moreno1@ucsc.edu">Omar Moreno</a>
 */
public class TrackClusterMatcher {

    Double beamEnergy;

    /**
     * The B field map
     */
    FieldMap bFieldMap = null;

    // Plotting
    private ITree tree;
    private IHistogramFactory histogramFactory;
    private Map<String, IHistogram1D> plots1D;
    private Map<String, IHistogram2D> plots2D;

    /**
     * Flag used to determine if plots are enabled/disabled
     */
    boolean enablePlots = false;

    /**
     * Flag used to determine whether the analytic or field map extrapolator
     * should be used.
     */
    boolean useAnalyticExtrapolator = false;

    /**
     * These cuts are set at +/- 4 sigma extracted from Gaussian fits to the
     * track-cluster residual distributions. The data used to determine these
     * limits is a pass 2 test file (t2.6) using run 5772.
     */
    private double topClusterTrackMatchDeltaXLow = -14.5; // mm 
    private double topClusterTrackMatchDeltaXHigh = 23.5; // mm 
    private double bottomClusterTrackMatchDeltaXLow = -19.5; // mm 
    private double bottomClusterTrackMatchDeltaXHigh = 16.5; // mm 

    private double topClusterTrackMatchDeltaYLow = -21.5; // mm 
    private double topClusterTrackMatchDeltaYHigh = 28; // mm 
    private double bottomClusterTrackMatchDeltaYLow = -28; // mm 
    private double bottomClusterTrackMatchDeltaYHigh = 24; // mm 

    /**
     * Rafo's parameterization of cluster-seed x/y position residuals as function of energy.
     * 
     * Derived using GBL/seed tracks, non-analytic extrapolation, uncorrected cluster positions,
     * and EngRun2015-Nominal-v4-4-fieldmap detector.
     * 
     *  f = p0+e*(p1+e*(p2+e*(p3+e*(p4+e*p5))))
     */
    double dyMeanBotElecGBL_noL6_2015[] = {-7.17364, 51.9077, -149.155, 147.207, };
    double dySigmBotElecGBL_noL6_2015[] = {13.7868, 9.41886, -156, 209.963, };
    double dyMeanBotPosiGBL_noL6_2015[] = {11.2292, -165.876, 591.592, -601.435, };
    double dySigmBotPosiGBL_noL6_2015[] = {5.1586, 79.949, -321.238, 313.925, };
    double dyMeanTopElecGBL_noL6_2015[] = {6.33503, -64.1156, 204.924, -204.87, };
    double dySigmTopElecGBL_noL6_2015[] = {29.3164, -130.8, 254.59, -178.537, };
    double dyMeanTopPosiGBL_noL6_2015[] = {0.394555, 43.2088, -222.677, 252.737, };
    double dySigmTopPosiGBL_noL6_2015[] = {20.4963, -47.4179, 3.31726, 54.2533, };
    double dxMeanBotElecGBL_noL6_2015[] = {84.2526, -618.986, 1549.52, -1273.73, };
    double dxSigmBotElecGBL_noL6_2015[] = {43.669, -207.526, 423.221, -317.005, };
    double dxMeanBotPosiGBL_noL6_2015[] = {-81.5944, 593.329, -1440.75, 1144.2, };
    double dxSigmBotPosiGBL_noL6_2015[] = {16.9999, 22.0071, -201.033, 214.312, };
    double dxMeanTopElecGBL_noL6_2015[] = {70.2111, -490.225, 1170.85, -906.215, };
    double dxSigmTopElecGBL_noL6_2015[] = {23.5848, -22.8375, -100.662, 162.045, };
    double dxMeanTopPosiGBL_noL6_2015[] = {-106.445, 825.712, -2119.92, 1795.38, };
    double dxSigmTopPosiGBL_noL6_2015[] = {31.8189, -120.106, 238.918, -212.629, };
    double dyMeanBotElecGBL_hasL6_2015[] = {4.55098, -47.132, 164.208, -251.225, 138.445, };
    double dySigmBotElecGBL_hasL6_2015[] = {-1.07979, 69.2823, -280.618, 427.443, -223.653, };
    double dyMeanBotPosiGBL_hasL6_2015[] = {-6.61989, 45.9481, -134.208, 173.095, -85.3071, };
    double dySigmBotPosiGBL_hasL6_2015[] = {14.5271, -66.9343, 155.271, -169.818, 71.0514, };
    double dyMeanTopElecGBL_hasL6_2015[] = {-16.4252, 137.898, -421.075, 567.766, -281.956, };
    double dySigmTopElecGBL_hasL6_2015[] = {12.6111, -58.7843, 142.594, -162.391, 71.1072, };
    double dyMeanTopPosiGBL_hasL6_2015[] = {-2.72525, 23.3503, -71.8925, 105.965, -57.5752, };
    double dySigmTopPosiGBL_hasL6_2015[] = {14.5742, -78.1711, 219.883, -297.48, 154.408, };
    double dxMeanBotElecGBL_hasL6_2015[] = {-6.23259, 86.3158, -284.914, 379.255, -183.232, };
    double dxSigmBotElecGBL_hasL6_2015[] = {23.6521, -135.592, 366.388, -461.049, 220.64, };
    double dxMeanBotPosiGBL_hasL6_2015[] = {-11.6694, 78.7264, -226.947, 282.234, -122.858, };
    double dxSigmBotPosiGBL_hasL6_2015[] = {19.2137, -86.8682, 183.822, -178.96, 65.566, };
    double dxMeanTopElecGBL_hasL6_2015[] = {9.33562, -47.4252, 137.437, -169.957, 74.312, };
    double dxSigmTopElecGBL_hasL6_2015[] = {16.6497, -76.1094, 179.447, -204.136, 89.8847, };
    double dxMeanTopPosiGBL_hasL6_2015[] = {-10.195, 57.1086, -123.843, 128.667, -45.7468, };
    double dxSigmTopPosiGBL_hasL6_2015[] = {23.3435, -118.887, 277.13, -296.727, 119.435, };
    
    /*edges of the fits*/
    double pHigh_hasL6_2015 = .8;
    double pHigh_noL6_2015 = .5;
    double pLow_hasL6_2015 = .18;
    double pLow_noL6_2015 = .18;
    
    // parameters for 2.3 GeV running.
    double dyMeanBotElecGBL_noL6_2016[] = {-6.48114, 22.2464, -24.0258, 7.82766, };
    double dySigmBotElecGBL_noL6_2016[] = {17.9252, -41.3164, 41.3304, -14.7517, };
    double dyMeanBotPosiGBL_noL6_2016[] = {-11.7136, 34.6864, -32.7093, 10.1458, };
    double dySigmBotPosiGBL_noL6_2016[] = {26.568, -77.1315, 91.2976, -37.8038, };
    double dyMeanTopElecGBL_noL6_2016[] = {-7.89099, 32.4447, -44.7542, 20.3456, };
    double dySigmTopElecGBL_noL6_2016[] = {17.7918, -36.6478, 29.4694, -8.06169, };
    double dyMeanTopPosiGBL_noL6_2016[] = {-2.58173, 11.3186, -17.3132, 7.42765, };
    double dySigmTopPosiGBL_noL6_2016[] = {-2.18936, 43.6352, -75.0924, 35.9227, };
    double dxMeanBotElecGBL_noL6_2016[] = {42.964, -128.548, 140.918, -51.64, };
    double dxSigmBotElecGBL_noL6_2016[] = {36.8183, -99.2736, 106.351, -39.844, };
    double dxMeanBotPosiGBL_noL6_2016[] = {-46.6173, 157.474, -187.009, 73.96, };
    double dxSigmBotPosiGBL_noL6_2016[] = {46.0317, -143.346, 172.54, -70.9532, };
    double dxMeanTopElecGBL_noL6_2016[] = {30.9052, -90.09, 104.774, -41.15, };
    double dxSigmTopElecGBL_noL6_2016[] = {8.16952, 29.1376, -77.9399, 44.8599, };
    double dxMeanTopPosiGBL_noL6_2016[] = {-59.7332, 195.964, -218.42, 82.1346, };
    double dxSigmTopPosiGBL_noL6_2016[] = {64.1991, -208.484, 247.029, -98.3336, };
    double dyMeanBotElecGBL_hasL6_2016[] = {-6.28213, 33.3392, -61.5168, 45.574, -11.71, };
    double dySigmBotElecGBL_hasL6_2016[] = {9.92401, -25.3032, 31.6691, -18.2118, 3.92029, };
    double dyMeanBotPosiGBL_hasL6_2016[] = {5.01792, -23.2565, 35.5235, -23.1599, 5.40634, };
    double dySigmBotPosiGBL_hasL6_2016[] = {0.815639, 17.5981, -37.0339, 27.7743, -7.11745, };
    double dyMeanTopElecGBL_hasL6_2016[] = {3.23873, -23.9294, 51.0193, -40.8843, 11.0352, };
    double dySigmTopElecGBL_hasL6_2016[] = {9.40385, -21.4277, 24.1184, -12.3961, 2.31535, };
    double dyMeanTopPosiGBL_hasL6_2016[] = {-7.05313, 26.9298, -38.4908, 24.3693, -5.65434, };
    double dySigmTopPosiGBL_hasL6_2016[] = {4.16791, 1.32443, -10.1539, 9.019, -2.38522, };
    double dxMeanBotElecGBL_hasL6_2016[] = {-4.48925, 42.3, -72.9497, 50.2509, -12.4418, };
    double dxSigmBotElecGBL_hasL6_2016[] = {11.4499, -27.7882, 36.0098, -22.508, 5.50197, };
    double dxMeanBotPosiGBL_hasL6_2016[] = {-6.64995, 14.7855, -18.5416, 10.4191, -2.00325, };
    double dxSigmBotPosiGBL_hasL6_2016[] = {7.49727, -6.337, -2.95785, 6.44046, -2.16203, };
    double dxMeanTopElecGBL_hasL6_2016[] = {-4.53952, 45.9687, -80.7531, 58.3937, -15.0482, };
    double dxSigmTopElecGBL_hasL6_2016[] = {11.0752, -25.3525, 30.0412, -16.6484, 3.52064, };
    double dxMeanTopPosiGBL_hasL6_2016[] = {-13.2928, 37.9236, -45.849, 26.8323, -5.80805, };
    double dxSigmTopPosiGBL_hasL6_2016[] = {9.36327, -16.2788, 14.5396, -5.60705, 0.705979, };
    
    /*edges of the fits*/
    double pHigh_hasL6_2016 = 1.6;
    double pHigh_noL6_2016 = 1.0;
    double pLow_hasL6_2016 = .44;
    double pLow_noL6_2016 = .44;
    
    /**
     * Z position to start extrapolation from
     */
    double extStartPos = 700; // mm

    /**
     * The extrapolation step size
     */
    double stepSize = 5.; // mm

    private boolean snapToEdge = true;
    
    public void setSnapToEdge(boolean val){
        this.snapToEdge = val;
    }
    

    /**
     * Constructor
     */
    public TrackClusterMatcher() {
    }

    /**
     * Enable/disable booking, filling of Ecal cluster and extrapolated track 
     * position plots.
     * 
     * @param enablePlots true to enable, false to disable
     */
    public void enablePlots(boolean enablePlots) {
        this.enablePlots = enablePlots;
        if (enablePlots == true) {
            this.bookHistograms();
        }
    }

    /**
     * Set the 3D field map to be used by the extrapolator.
     *
     * @param bFieldMap The {@link FieldMap} object containing a mapping to the
     * 3D field map.
     */
    public void setBFieldMap(FieldMap bFieldMap) {
        this.bFieldMap = bFieldMap;
    }

    /**
     * Use the analytic track extrapolator i.e. the no fringe extrapolator. The
     * field map extrapolator is used by default.
     *
     * @param useAnalyticExtrapolator Set to true to use the analytic
     * extrapolator, false otherwise.
     */
    public void setUseAnalyticExtrapolator(boolean useAnalyticExtrapolator) {
        this.useAnalyticExtrapolator = useAnalyticExtrapolator;
    }

    /**
     * Set the window in which the x residual of the extrapolated bottom track
     * position at the Ecal and the Ecal cluster position must be within to be
     * considered a 'good match'
     *
     * @param xLow
     * @param xHigh
     */
    public void setBottomClusterTrackDxCuts(double xLow, double xHigh) {
        this.topClusterTrackMatchDeltaXLow = xLow;
        this.topClusterTrackMatchDeltaXHigh = xHigh;
    }

    /**
     * Set the window in which the y residual of the extrapolated bottom track
     * position at the Ecal and the Ecal cluster position must be within to be
     * considered a 'good match'
     *
     * @param yLow
     * @param yHigh
     */
    public void setBottomClusterTrackDyCuts(double yLow, double yHigh) {
        this.topClusterTrackMatchDeltaYLow = yLow;
        this.topClusterTrackMatchDeltaYHigh = yHigh;
    }

    /**
     * Set the window in which the x residual of the extrapolated top track
     * position at the Ecal and the Ecal cluster position must be within to be
     * considered a 'good match'
     *
     * @param xLow
     * @param xHigh
     */
    public void setTopClusterTrackDxCuts(double xLow, double xHigh) {
        this.topClusterTrackMatchDeltaXLow = xLow;
        this.topClusterTrackMatchDeltaXHigh = xHigh;
    }

    /**
     * Set the window in which the y residual of the extrapolated top track
     * position at the Ecal and the Ecal cluster position must be within to be
     * considered a 'good match'
     *
     * @param yLow
     * @param yHigh
     */
    public void setTopClusterTrackDyCuts(double yLow, double yHigh) {
        this.topClusterTrackMatchDeltaYLow = yLow;
        this.topClusterTrackMatchDeltaYHigh = yHigh;
    }

    /**
     * Get distance between track and cluster.
     * 
     * @param cluster
     * @param track
     * @return distance between cluster and track
     */
    public double getDistance(Cluster cluster,Track track) {
        
        // Get the cluster position
        Hep3Vector cPos = new BasicHep3Vector(cluster.getPosition());
        
        // Extrapolate the track to the Ecal cluster position
        Hep3Vector tPos = null;
        if (this.useAnalyticExtrapolator) {
            tPos = TrackUtils.extrapolateTrack(track, cPos.z());
        } else {
            TrackState trackStateAtEcal = TrackUtils.getTrackStateAtECal(track);
            tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            tPos = CoordinateTransformations.transformVectorToDetector(tPos);
        }
       
        return Math.sqrt(Math.pow(cPos.x()-tPos.x(),2)+Math.pow(cPos.y()-tPos.y(),2));
    }
    
    /**
     * Calculate #sigma between cluster-track x/y position at calorimeter.
     *
     * Based on Rafo's parameterizations.  Requires non-analytic extrapolation
     * and uncorrected cluster positions.
     * 
     * @param cluster = position-uncorrected cluster
     * @param particle recon particle with tracks
     *
     * @return #sigma between cluster and track positions
     */
    public double getNSigmaPosition(Cluster cluster, ReconstructedParticle particle) {
        if (particle.getTracks().size()<1) return Double.MAX_VALUE;
        Track track=particle.getTracks().get(0);
        return getNSigmaPosition(cluster, track, new BasicHep3Vector(track.getTrackStates().get(0).getMomentum()).magnitude());
    }
    public double getNSigmaPosition(Cluster cluster, Track track, double p){
        
        
        if (this.useAnalyticExtrapolator)
            throw new RuntimeException("This is to be used with non-analytic extrapolator only.");

        // Get the cluster position:
        Hep3Vector cPos = new BasicHep3Vector(cluster.getPosition());

        // whether track is in top half of detector:
        final boolean isTopTrack = track.getTrackStates().get(0).getTanLambda() > 0;

        // ignore if track and cluster in different halves:
        if (isTopTrack != cPos.y()>0) return Double.MAX_VALUE;

        // Get the extrapolated track position at the calorimeter:
        TrackState trackStateAtEcal = TrackUtils.getTrackStateAtECal(track);
        if(trackStateAtEcal == null){
            // Track never made it to the ECAL, so it curled before doing this and probably extrapolateTrackUsingFieldMap aborted.
            return Double.MAX_VALUE;
        }
        Hep3Vector tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
        tPos = CoordinateTransformations.transformVectorToDetector(tPos);

        // whether it's a GBL track:
        final boolean isGBL = track.getType() >= 32;
       
        boolean hasL6 = false;
        for(TrackerHit hit : track.getTrackerHits()){
            if(TrackUtils.getLayer(hit) == 11)
                hasL6 = true;
        }
        
        // choose which parameterization of mean and sigma to use:
        double dxMean[],dyMean[],dxSigm[],dySigm[];
        int charge = TrackUtils.getCharge(track);
        
        boolean use1pt05parameters = false, use2pt3parameters;
        if(beamEnergy < 2){
            use1pt05parameters = true;
            use2pt3parameters = false;
        } else { // if we do 4.4 or 6.6 GeV or other beam energies, use the 2.3 GeV values,
            // until we have new parameters for that beam energy.  
            use1pt05parameters = false;
            use2pt3parameters = true;
        }
            
        
        if(use1pt05parameters){
            if (charge>0) {
                if (isTopTrack) {
                    dxMean = !hasL6 ? dxMeanTopPosiGBL_noL6_2015 : dxMeanTopPosiGBL_hasL6_2015;
                    dxSigm = !hasL6 ? dxSigmTopPosiGBL_noL6_2015 : dxSigmTopPosiGBL_hasL6_2015;
                    dyMean = !hasL6 ? dyMeanTopPosiGBL_noL6_2015 : dyMeanTopPosiGBL_hasL6_2015;
                    dySigm = !hasL6 ? dySigmTopPosiGBL_noL6_2015 : dySigmTopPosiGBL_hasL6_2015;
                }
                else {
                    dxMean = !hasL6 ? dxMeanBotPosiGBL_noL6_2015 : dxMeanBotPosiGBL_hasL6_2015;
                    dxSigm = !hasL6 ? dxSigmBotPosiGBL_noL6_2015 : dxSigmBotPosiGBL_hasL6_2015;
                    dyMean = !hasL6 ? dyMeanBotPosiGBL_noL6_2015 : dyMeanBotPosiGBL_hasL6_2015;
                    dySigm = !hasL6 ? dySigmBotPosiGBL_noL6_2015 : dySigmBotPosiGBL_hasL6_2015;
                }
            }
            else if (charge<0) {
                if (isTopTrack) {
                    dxMean = !hasL6 ? dxMeanTopElecGBL_noL6_2015 : dxMeanTopElecGBL_hasL6_2015;
                    dxSigm = !hasL6 ? dxSigmTopElecGBL_noL6_2015 : dxSigmTopElecGBL_hasL6_2015;
                    dyMean = !hasL6 ? dyMeanTopElecGBL_noL6_2015 : dyMeanTopElecGBL_hasL6_2015;
                    dySigm = !hasL6 ? dySigmTopElecGBL_noL6_2015 : dySigmTopElecGBL_hasL6_2015;
                }
                else {
                    dxMean = !hasL6 ? dxMeanBotElecGBL_noL6_2015 : dxMeanBotElecGBL_hasL6_2015;
                    dxSigm = !hasL6 ? dxSigmBotElecGBL_noL6_2015 : dxSigmBotElecGBL_hasL6_2015;
                    dyMean = !hasL6 ? dyMeanBotElecGBL_noL6_2015 : dyMeanBotElecGBL_hasL6_2015;
                    dySigm = !hasL6 ? dySigmBotElecGBL_noL6_2015 : dySigmBotElecGBL_hasL6_2015;
                }
            }
            else return Double.MAX_VALUE;
        }
        else if (use2pt3parameters){
            
            if (charge>0) {
                if (isTopTrack) {
                    dxMean = !hasL6 ? dxMeanTopPosiGBL_noL6_2016 : dxMeanTopPosiGBL_hasL6_2016;
                    dxSigm = !hasL6 ? dxSigmTopPosiGBL_noL6_2016 : dxSigmTopPosiGBL_hasL6_2016;
                    dyMean = !hasL6 ? dyMeanTopPosiGBL_noL6_2016 : dyMeanTopPosiGBL_hasL6_2016;
                    dySigm = !hasL6 ? dySigmTopPosiGBL_noL6_2016 : dySigmTopPosiGBL_hasL6_2016;
                }
                else {
                    dxMean = !hasL6 ? dxMeanBotPosiGBL_noL6_2016 : dxMeanBotPosiGBL_hasL6_2016;
                    dxSigm = !hasL6 ? dxSigmBotPosiGBL_noL6_2016 : dxSigmBotPosiGBL_hasL6_2016;
                    dyMean = !hasL6 ? dyMeanBotPosiGBL_noL6_2016 : dyMeanBotPosiGBL_hasL6_2016;
                    dySigm = !hasL6 ? dySigmBotPosiGBL_noL6_2016 : dySigmBotPosiGBL_hasL6_2016;
                }
            }
            else if (charge<0) {
                if (isTopTrack) {
                    dxMean = !hasL6 ? dxMeanTopElecGBL_noL6_2016 : dxMeanTopElecGBL_hasL6_2016;
                    dxSigm = !hasL6 ? dxSigmTopElecGBL_noL6_2016 : dxSigmTopElecGBL_hasL6_2016;
                    dyMean = !hasL6 ? dyMeanTopElecGBL_noL6_2016 : dyMeanTopElecGBL_hasL6_2016;
                    dySigm = !hasL6 ? dySigmTopElecGBL_noL6_2016 : dySigmTopElecGBL_hasL6_2016;
                }
                else {
                    dxMean = !hasL6 ? dxMeanBotElecGBL_noL6_2016 : dxMeanBotElecGBL_hasL6_2016;
                    dxSigm = !hasL6 ? dxSigmBotElecGBL_noL6_2016 : dxSigmBotElecGBL_hasL6_2016;
                    dyMean = !hasL6 ? dyMeanBotElecGBL_noL6_2016 : dyMeanBotElecGBL_hasL6_2016;
                    dySigm = !hasL6 ? dySigmBotElecGBL_noL6_2016 : dySigmBotElecGBL_hasL6_2016;
                }
            }
            else return Double.MAX_VALUE;
        }
        else 
            return Double.MAX_VALUE; //this line is never executed.   
        

        // Beyond the edges of the fits in momentum, assume that the parameters are constant:
        if(use1pt05parameters){
            if (p > pHigh_hasL6_2015 && hasL6) 
                p = pHigh_hasL6_2015;
            else if (p > pHigh_noL6_2015 && !hasL6) 
                p= pHigh_noL6_2015;
            else if (p < pLow_hasL6_2015 && hasL6)
                p = pLow_hasL6_2015;
            else if (p < pLow_noL6_2015 && !hasL6)
                p = pLow_noL6_2015;
        }
        if(use2pt3parameters){
            if (p > pHigh_hasL6_2016 && hasL6) 
                p = pHigh_hasL6_2016;
            else if (p > pHigh_noL6_2016 && !hasL6) 
                p= pHigh_noL6_2016;
            else if (p < pLow_hasL6_2016 && hasL6)
                p = pLow_hasL6_2016;
            else if (p < pLow_noL6_2016 && !hasL6)
                p = pLow_noL6_2016;
        }
        // calculate measured mean and sigma of deltaX and deltaY for this energy:
        double aDxMean=0,aDxSigm=0,aDyMean=0,aDySigm=0;
        for (int ii=dxMean.length-1; ii>=0; ii--) aDxMean = dxMean[ii] + p*aDxMean;
        for (int ii=dxSigm.length-1; ii>=0; ii--) aDxSigm = dxSigm[ii] + p*aDxSigm;
        for (int ii=dyMean.length-1; ii>=0; ii--) aDyMean = dyMean[ii] + p*aDyMean;
        for (int ii=dySigm.length-1; ii>=0; ii--) aDySigm = dySigm[ii] + p*aDySigm;

      //if the track's extrapolated position is within 1/2 a crystal width of the edge of 
        // the ecal edge, then move it to be 1/2 a crystal from the edge in y.  
       
        Hep3Vector originalTPos = tPos;
        
        if(snapToEdge )
            tPos= snapper.snapToEdge(tPos,cluster);
        
        
        // calculate nSigma between track and cluster:
        final double nSigmaX = (cPos.x() - tPos.x() - aDxMean) / aDxSigm;
        final double nSigmaY = (cPos.y() - tPos.y() - aDyMean) / aDySigm;
        
        double nSigma = Math.sqrt(nSigmaX*nSigmaX + nSigmaY*nSigmaY);
        
        if(debug && Math.abs(cPos.x()-tPos.x())<50 &&  Math.abs(cPos.y()-tPos.y())<50){
            System.out.println("TC MATCH RESULTS:");
            System.out.println("isTop:  " + isTopTrack);
            System.out.println("charge: " + charge);
            System.out.println("hasL6:  " + hasL6);
            System.out.println("p: " + p);
            System.out.println("cx: " + cPos.x());
            System.out.println("cy: " + cPos.y());
            System.out.println("tx: " + originalTPos.x());
            System.out.println("ty: " + originalTPos.y());
            
            System.out.println("nSigmaX: " + nSigmaX);
            System.out.println("nSigmaY: " + nSigmaY);
            System.out.println("nSigma: " + nSigma);
        }
        
        return nSigma;
        //return Math.sqrt( 1 / ( 1/nSigmaX/nSigmaX + 1/nSigmaY/nSigmaY ) );
    }

    boolean debug = false;
    
    SnapToEdge snapper = new SnapToEdge();

    /**
     * Determine if a track is matched to a cluster. Currently, this is
     * determined by checking that the track and cluster are within the same
     * detector volume of each other and that the extrapolated track position is
     * within some defined distance of the cluster.
     *
     * @param cluster : The Ecal cluster to check
     * @param track : The SVT track to check
     * @return true if all cuts are pased, false otherwise.
     */
    public boolean isMatch(Cluster cluster, Track track) {

        // Check that the track and cluster are in the same detector volume.
        // If not, there is no way they can be a match.
        if ((track.getTrackStates().get(0).getTanLambda() > 0 && cluster.getPosition()[1] < 0)
                || (track.getTrackStates().get(0).getTanLambda() < 0 && cluster.getPosition()[1] > 0)) {
            return false;
        }

        // Get the cluster position
        Hep3Vector clusterPosition = new BasicHep3Vector(cluster.getPosition());
        //System.out.println("Cluster Position: " + clusterPosition.toString());

        // Extrapolate the track to the Ecal cluster position
        Hep3Vector trackPosAtEcal = null;
        if (this.useAnalyticExtrapolator) {
            //System.out.println("Using analytic field extrapolator."); 
            trackPosAtEcal = TrackUtils.extrapolateTrack(track, clusterPosition.z());
        } else {
            //System.out.println("Using field map extrapolator"); 
            TrackState trackStateAtEcal = TrackUtils.getTrackStateAtECal(track);
            trackPosAtEcal = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            trackPosAtEcal = CoordinateTransformations.transformVectorToDetector(trackPosAtEcal);
        }
        //System.out.println("Track position at Ecal: " + trackPosAtEcal.toString());

        // Calculate the difference between the cluster position at the Ecal and
        // the track in both the x and y directions
        double deltaX = clusterPosition.x() - trackPosAtEcal.x();
        double deltaY = clusterPosition.y() - trackPosAtEcal.y();

        //System.out.println("delta X: " + deltaX);
        //System.out.println("delta Y: " + deltaY);
        if (enablePlots) {
            if (track.getTrackStates().get(0).getTanLambda() > 0) {

                plots1D.get("Ecal cluster x - track x @ Ecal - top - all").fill(deltaX);
                plots2D.get("Ecal cluster x v track x @ Ecal - top - all").fill(clusterPosition.x(),
                        trackPosAtEcal.x());
                plots1D.get("Ecal cluster y - track y @ Ecal - top - all").fill(deltaY);
                plots2D.get("Ecal cluster y v track y @ Ecal - top - all").fill(clusterPosition.y(),
                        trackPosAtEcal.y());

            } else if (track.getTrackStates().get(0).getTanLambda() < 0) {

                plots1D.get("Ecal cluster x - track x @ Ecal - bottom - all").fill(deltaX);
                plots2D.get("Ecal cluster x v track x @ Ecal - bottom - all").fill(clusterPosition.x(),
                        trackPosAtEcal.x());
                plots1D.get("Ecal cluster y - track y @ Ecal - bottom - all").fill(deltaY);
                plots2D.get("Ecal cluster y v track y @ Ecal - bottom - all").fill(clusterPosition.y(),
                        trackPosAtEcal.y());
            }
        }

        // Check that dx and dy between the extrapolated track and cluster 
        // positions is reasonable.  Different requirements are imposed on 
        // top and bottom tracks in order to account for offsets.
        if ((track.getTrackStates().get(0).getTanLambda() > 0 && (deltaX > topClusterTrackMatchDeltaXHigh
                || deltaX < topClusterTrackMatchDeltaXLow))
                || (track.getTrackStates().get(0).getTanLambda() < 0 && (deltaX > bottomClusterTrackMatchDeltaXHigh
                || deltaX < bottomClusterTrackMatchDeltaXLow))) {
            return false;
        }

        if ((track.getTrackStates().get(0).getTanLambda() > 0 && (deltaY > topClusterTrackMatchDeltaYHigh
                || deltaY < topClusterTrackMatchDeltaYLow))
                || (track.getTrackStates().get(0).getTanLambda() < 0 && (deltaY > bottomClusterTrackMatchDeltaYHigh
                || deltaY < bottomClusterTrackMatchDeltaYLow))) {
            return false;
        }

        if (enablePlots) {
            if (track.getTrackStates().get(0).getTanLambda() > 0) {

                plots1D.get("Ecal cluster x - track x @ Ecal - top - matched").fill(deltaX);
                plots2D.get("Ecal cluster x v track x @ Ecal - top - matched").fill(clusterPosition.x(),
                        trackPosAtEcal.x());
                plots1D.get("Ecal cluster y - track y @ Ecal - top - matched").fill(deltaY);
                plots2D.get("Ecal cluster y v track y @ Ecal - top - matched").fill(clusterPosition.y(),
                        trackPosAtEcal.y());

            } else if (track.getTrackStates().get(0).getTanLambda() < 0) {

                plots1D.get("Ecal cluster x - track x @ Ecal - bottom - matched").fill(deltaX);
                plots2D.get("Ecal cluster x v track x @ Ecal - bottom - matched").fill(clusterPosition.x(),
                        trackPosAtEcal.x());
                plots1D.get("Ecal cluster y - track y @ Ecal - bottom - matched").fill(deltaY);
                plots2D.get("Ecal cluster y v track y @ Ecal - bottom - matched").fill(clusterPosition.y(),
                        trackPosAtEcal.y());
            }
        }

        // If all cuts are pased, return true.
        return true;
    }

    /**
     * Book histograms of Ecal cluster x/y vs extrapolated track x/y
     */
    private void bookHistograms() {

        plots1D = new HashMap<String, IHistogram1D>();
        plots2D = new HashMap<String, IHistogram2D>();

        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        //--- All tracks and clusters ---//
        //-------------------------------//
        //--- Top ---//
        plots1D.put("Ecal cluster x - track x @ Ecal - top - all",
                histogramFactory.createHistogram1D("Ecal cluster x - track x @ Ecal - top - all", 200, -200, 200));

        plots2D.put("Ecal cluster x v track x @ Ecal - top - all",
                histogramFactory.createHistogram2D("Ecal cluster x v track x @ Ecal - top - all", 200, -200, 200, 200, -200, 200));

        plots1D.put("Ecal cluster y - track y @ Ecal - top - all",
                histogramFactory.createHistogram1D("Ecal cluster y - track y @ Ecal - top - all", 100, -100, 100));

        plots2D.put("Ecal cluster y v track y @ Ecal - top - all",
                histogramFactory.createHistogram2D("Ecal cluster y v track  @ Ecal - top - all", 100, -100, 100, 100, -100, 100));

        //--- Bottom ---//
        plots1D.put("Ecal cluster x - track x @ Ecal - bottom - all",
                histogramFactory.createHistogram1D("Ecal cluster x - track x @ Ecal - bottom - all", 200, -200, 200));

        plots2D.put("Ecal cluster x v track x @ Ecal - bottom - all",
                histogramFactory.createHistogram2D("Ecal cluster x v track x @ Ecal - bottom - all", 200, -200, 200, 200, -200, 200));

        plots1D.put("Ecal cluster y - track y @ Ecal - bottom - all",
                histogramFactory.createHistogram1D("Ecal cluster y - track y @ Ecal - bottom - all", 100, -100, 100));

        plots2D.put("Ecal cluster y v track y @ Ecal - bottom - all",
                histogramFactory.createHistogram2D("Ecal cluster y v track  @ Ecal - bottom - all", 100, -100, 100, 100, -100, 100));

        //--- Matched tracks ---//
        //----------------------//
        //--- Top ---//
        plots1D.put("Ecal cluster x - track x @ Ecal - top - matched",
                histogramFactory.createHistogram1D("Ecal cluster x - track x @ Ecal - top - matched", 200, -200, 200));

        plots2D.put("Ecal cluster x v track x @ Ecal - top - matched",
                histogramFactory.createHistogram2D("Ecal cluster x v track x @ Ecal - top - matched", 200, -200, 200, 200, -200, 200));

        plots1D.put("Ecal cluster y - track y @ Ecal - top - matched",
                histogramFactory.createHistogram1D("Ecal cluster y - track y @ Ecal - top - matched", 100, -100, 100));

        plots2D.put("Ecal cluster y v track y @ Ecal - top - matched",
                histogramFactory.createHistogram2D("Ecal cluster y v track  @ Ecal - top - matched", 100, -100, 100, 100, -100, 100));

        //--- Bottom ---//
        plots1D.put("Ecal cluster x - track x @ Ecal - bottom - matched",
                histogramFactory.createHistogram1D("Ecal cluster x - track x @ Ecal - bottom - matched", 200, -200, 200));

        plots2D.put("Ecal cluster x v track x @ Ecal - bottom - matched",
                histogramFactory.createHistogram2D("Ecal cluster x v track x @ Ecal - bottom - matched", 200, -200, 200, 200, -200, 200));

        plots1D.put("Ecal cluster y - track y @ Ecal - bottom - matched",
                histogramFactory.createHistogram1D("Ecal cluster y - track y @ Ecal - bottom - matched", 100, -100, 100));

        plots2D.put("Ecal cluster y v track y @ Ecal - bottom - matched",
                histogramFactory.createHistogram2D("Ecal cluster y v track  @ Ecal - bottom - matched", 100, -100, 100, 100, -100, 100));

    }

    /**
     * Save the histograms to a ROO file
     */
    public void saveHistograms() {

        String rootFile = "track_cluster_matching_plots.root";
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Class to store track-cluster matching qualities.
     */
    public class TrackClusterMatch {
        private double nSigmaPositionMatch=Double.MAX_VALUE;
        public TrackClusterMatch(ReconstructedParticle pp, Cluster cc) {
            nSigmaPositionMatch = getNSigmaPosition(cc,pp);
        }
        public double getNSigmaPositionMatch() { return nSigmaPositionMatch; }
    }

    public void setBeamEnergy(double beamEnergy) {
        this.beamEnergy = beamEnergy;
    }
    
}
