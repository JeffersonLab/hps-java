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

import org.hps.conditions.ecal.EcalCrystalPosition;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalCrystalPosition.EcalCrystalPositionCollection;
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


    EcalCrystalPositionCollection positions;
    EcalChannelCollection channels;

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
    private static final double dxMeanTopPosiGBL[] = { 6.67414,-9.57296, 5.70647, 27.4523,-28.1103,-9.11424 };
    private static final double dxSigmTopPosiGBL[] = { 52.6437,-478.805, 1896.73,-3761.48, 3676.77,-1408.31 };
    private static final double dxMeanBotPosiGBL[] = { 4.13802, 15.8887,-74.2844,-9.78944, 308.541,-287.668 };
    private static final double dxSigmBotPosiGBL[] = { 37.6513,-294.851, 1002.15,-1639.08, 1228.02,-308.754 };

    private static final double dxMeanTopElecGBL[] = {-1.6473,  5.58701, 25.3977,-17.1523,-121.025, 145.969 };
    private static final double dxSigmTopElecGBL[] = { 48.7018,-423.599, 1592.66,-2959.99, 2668.97,-919.876 };
    private static final double dxMeanBotElecGBL[] = {-6.63558, 83.7763,-460.451, 1275.63,-1702.83, 873.913 };
    private static final double dxSigmBotElecGBL[] = { 47.0029,-411.784, 1586.52,-3083.37, 2985.58,-1145.53 };

    private static final double dyMeanTopPosiGBL[] = { 0.31245, 5.57585,-6.50267,-8.21688, 39.8607,-43.9661 };
    private static final double dySigmTopPosiGBL[] = { 33.0213,-275.174, 1168.77,-2642.34, 3045.52,-1406.21 };
    private static final double dyMeanBotPosiGBL[] = {-7.032,   74.9738,-383.972, 977.849,-1250.28, 637.75  };
    private static final double dySigmBotPosiGBL[] = { 19.019, -83.9253, 133.813, 119.883,-546.951, 405.207 };

    private static final double dyMeanTopElecGBL[] = { 2.48498,-20.4101, 62.9689, 25.6386,-259.957, 207.145 };
    private static final double dySigmTopElecGBL[] = { 8.65583, 120.676,-1166.43, 3811.72,-5383.19, 2787.42 };
    private static final double dyMeanBotElecGBL[] = {-10.5228, 112.329,-489.761, 953.037,-829.96,  260.772 };
    private static final double dySigmBotElecGBL[] = { 23.4856,-108.19,  158.7,   189.261,-682.034, 459.15  };

    private static final double dxMeanTopPosiSeed[] ={ 11.6245,-28.5061, 13.0332, 59.9465,-21.1014,-63.6126 };
    private static final double dxSigmTopPosiSeed[] ={ 61.5911,-540.596, 2077.22,-3973.22, 3704.45,-1332.07 };
    private static final double dxMeanBotPosiSeed[] ={ 4.53394, 11.3773,-63.7127,-2.81629, 273.868,-264.709 };
    private static final double dxSigmBotPosiSeed[] ={ 48.3163,-409.249, 1590.36,-3212.85, 3326.04,-1402.3  };

    private static final double dxMeanTopElecSeed[] ={ 2.14163,-20.8713, 76.3054,  34.894,-340.272,  295.24 };
    private static final double dxSigmTopElecSeed[] ={ 48.585, -385.166, 1320.26,-2157.45, 1581.06,-366.012 };
    private static final double dxMeanBotElecSeed[] ={-3.44302, 12.4687, 4.09878,-30.0057,-13.3151, 40.2707 };
    private static final double dxSigmBotElecSeed[] ={ 48.4089,-385.494, 1341.37,-2271.52, 1814.02,-526.555 };

    private static final double dyMeanTopPosiSeed[] ={-0.527741,10.4944, -18.242,-12.9155, 81.0116,-73.9773 };
    private static final double dySigmTopPosiSeed[] ={ 37.3097, -357.55, 1607.03,-3709.55, 4282.36,-1957.91 };
    private static final double dyMeanBotPosiSeed[] ={ 0.74392,-55.2003,  405.04,-1250.64, 1731.47,-887.262 };
    private static final double dySigmBotPosiSeed[] ={ 25.5776,-199.731,  754.59,-1408.72, 1240.36,-400.912 };

    private static final double dyMeanTopElecSeed[] ={ 2.85429,-24.0858, 69.0145, 34.1213,-297.752, 239.939 };
    private static final double dySigmTopElecSeed[] ={ 19.9111,-53.2699,-261.915,  1593.2,-2774.01, 1605.54 };
    private static final double dyMeanBotElecSeed[] ={-9.22963, 98.1346, -427.91, 840.225,-751.188, 250.792 };
    private static final double dySigmBotElecSeed[] ={ 21.7909,-85.4757,-56.9423, 977.522,-1902.05, 1137.92 };

    double dyMeanBotElecGBL_noL6_2016[] = {23.898, -145.794, 342.569, -386.272, 211.438, -45.1852, };
    double dySigmBotElecGBL_noL6_2016[] = {22.9261, -68.1747, 123.541, -140.775, 87.5706, -21.3632, };
    double dyMeanBotPosiGBL_noL6_2016[] = {58.774, -370.525, 858.462, -918.198, 458.768, -86.5076, };
    double dySigmBotPosiGBL_noL6_2016[] = {1.83929, 60.7155, -180.415, 209.254, -109.002, 21.0066, };
    double dyMeanTopElecGBL_noL6_2016[] = {-26.475, 156.897, -362.459, 397.92, -205.231, 39.5504, };
    double dySigmTopElecGBL_noL6_2016[] = {17.3192, -25.1257, -0.444389, 27.3374, -20.2921, 4.45335, };
    double dyMeanTopPosiGBL_noL6_2016[] = {-65.075, 441.159, -1138.55, 1411.9, -848.297, 198.577, };
    double dySigmTopPosiGBL_noL6_2016[] = {-5.41455, 114.663, -337.272, 428.686, -258.837, 61.0901, };
    double dxMeanBotElecGBL_noL6_2016[] = {55.3106, -245.841, 326.048, -81.3039, -118.178, 60.8224, };
    double dxSigmBotElecGBL_noL6_2016[] = {3.96533, 175.805, -779.098, 1326.95, -1008.98, 285.361, };
    double dxMeanBotPosiGBL_noL6_2016[] = {-29.3764, 46.0038, 228.393, -573.14, 439.687, -107.905, };
    double dxSigmBotPosiGBL_noL6_2016[] = {53.2854, -221.826, 443.864, -467.612, 247.697, -50.3484, };
    double dxMeanTopElecGBL_noL6_2016[] = {93.2172, -523.151, 1097.82, -1101.05, 528.294, -96.5476, };
    double dxSigmTopElecGBL_noL6_2016[] = {31.2558, -94.1807, 159.509, -159.903, 84.8279, -17.4858, };
    double dxMeanTopPosiGBL_noL6_2016[] = {-141.837, 842.901, -1933.3, 2217.03, -1261.14, 282.985, };
    double dxSigmTopPosiGBL_noL6_2016[] = {-64.576, 642.623, -1971.44, 2771.38, -1849.36, 475.228, };
    double dyMeanBotElecGBL_hasL6_2016[] = {14.3683, -75.6979, 161.427, -171.298, 88.4899, -17.6266, };
    double dySigmBotElecGBL_hasL6_2016[] = {12.5987, -22.2252, 12.4958, 6.54389, -8.9993, 2.4176, };
    double dyMeanBotPosiGBL_hasL6_2016[] = {9.3233, -46.712, 89.8483, -84.2193, 38.2755, -6.73631, };
    double dySigmBotPosiGBL_hasL6_2016[] = {7.7728, 3.83082, -34.4904, 43.846, -21.8778, 3.82387, };
    double dyMeanTopElecGBL_hasL6_2016[] = {10.9911, -72.3086, 162.106, -161.577, 74.176, -12.8401, };
    double dySigmTopElecGBL_hasL6_2016[] = {11.8146, -17.4191, 0.934459, 21.594, -18.7076, 4.74385, };
    double dyMeanTopPosiGBL_hasL6_2016[] = {-16.7227, 91.755, -181.775, 171.108, -77.4855, 13.6391, };
    double dySigmTopPosiGBL_hasL6_2016[] = {0.93244, 33.8579, -86.2885, 87.2523, -39.8184, 6.82038, };
    double dxMeanBotElecGBL_hasL6_2016[] = {-10.9873, 34.8779, -51.6262, 33.9128, -9.36276, 0.717712, };
    double dxSigmBotElecGBL_hasL6_2016[] = {9.6714, -17.2858, 12.1283, 2.47722, -6.10931, 1.83614, };
    double dxMeanBotPosiGBL_hasL6_2016[] = {5.53517, 6.22874, -15.0064, 6.02579, 1.7573, -0.940359, };
    double dxSigmBotPosiGBL_hasL6_2016[] = {2.94234, 14.3324, -40.5195, 41.579, -18.6294, 3.01598, };
    double dxMeanTopElecGBL_hasL6_2016[] = {-11.9406, 27.431, -19.012, -5.55942, 11.4834, -3.41388, };
    double dxSigmTopElecGBL_hasL6_2016[] = {-5.52831, 70.1653, -175.031, 192.23, -97.9497, 18.8932, };
    double dxMeanTopPosiGBL_hasL6_2016[] = {5.78764, -9.30485, 32.1211, -44.6924, 27.0623, -5.85591, };
    double dxSigmTopPosiGBL_hasL6_2016[] = {6.93754, 1.67445, -30.3761, 45.1041, -26.1743, 5.4303, };




    /**
     * Z position to start extrapolation from
     */
    double extStartPos = 700; // mm

    /**
     * The extrapolation step size
     */
    double stepSize = 5.; // mm

    /**
     * Constant denoting the index of the {@link TrackState} at the Ecal
     */
    private static final int ECAL_TRACK_STATE_INDEX = 1;

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

        // if the t position of the the track is less than 1/2 a crystal from the edge,
        // snap it to the edge.  
        tPos = snapToEdge(tPos, cluster);

        return Math.sqrt(Math.pow(cPos.x()-tPos.x(),2)+Math.pow(cPos.y()-tPos.y(),2));
    }


    private Map<Integer, Integer> innermostRow;
    int getInnerEdgeCrystalRow(int column){
        if(innermostRow == null){ //load the map with stuff from database
            innermostRow = new HashMap();
            for(EcalChannel channel : channels){
                if(channel.getY()< 0)
                    continue;
                if(!innermostRow.containsKey(channel.getX()))
                    innermostRow.put(channel.getX(),channel.getY());
                else if(innermostRow.get(channel.getX())> channel.getY())
                    innermostRow.put(channel.getX(), channel.getY());
            }
        }
        return innermostRow.get(column);
    }

    private Hep3Vector snapToEdge(Hep3Vector tPos, Cluster cluster) {
        double ty = tPos.y();
        double tx = tPos.x();

        double closestDist = 9999;
        if(positions == null || channels == null)
            return tPos;
        Double snapToY = null;
        for(EcalCrystalPosition pos : positions){
            EcalChannel chan = channels.findChannel(pos.getChannelId());
            int ix = chan.getX();
            int iy = chan.getY();
            //must be on same side
            if(ty*iy < 0)
                continue;
            //must be an edge crystal
            if(!( Math.abs(iy) == 5 || Math.abs(iy) == getInnerEdgeCrystalRow(ix)))
                continue;
            double cx = pos.getFrontX();
            double cy = pos.getFrontY();

            if(((iy == 5 || iy == -1 || iy == -2) && cy >ty) 
                    || (((iy == -5 || iy == 1 || iy == 2) && cy <ty)))
                continue;
            double dist = Math.hypot(tx-cx, ty-cy);

            if(dist>closestDist) continue;
            closestDist = dist;
            snapToY = cy;
        }
        if(snapToY == null)
            return tPos;
        return new BasicHep3Vector(tPos.x(), snapToY, tPos.z());
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
        return getNSigmaPosition(cluster, track);
    }


    public double getNSigmaPositionX(Cluster cluster, Track track) {


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
        Hep3Vector tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
        tPos = CoordinateTransformations.transformVectorToDetector(tPos);

        // whether it's a GBL track:
        final boolean isGBL = track.getType() >= 32;


        // choose which parameterization of mean and sigma to use:
        double dxMean[],dxSigm[];
        int charge = TrackUtils.getCharge(track);
        
        boolean hasL6 = false;
        for(TrackerHit hit : track.getTrackerHits()){
            if(TrackUtils.getLayer(hit) == 11)
                hasL6 = true;
        }
        
        if(Math.abs(beamEnergy-1.05)<.1){
            if (charge>0) {
                if (isTopTrack) {
                    dxMean = isGBL ? dxMeanTopPosiGBL : dxMeanTopPosiSeed;
                    dxSigm = isGBL ? dxSigmTopPosiGBL : dxSigmTopPosiSeed;
                }
                else {
                    dxMean = isGBL ? dxMeanBotPosiGBL : dxMeanBotPosiSeed;
                    dxSigm = isGBL ? dxSigmBotPosiGBL : dxSigmBotPosiSeed;
                }
            }
            else if (charge<0) {
                if (isTopTrack) {
                    dxMean = isGBL ? dxMeanTopElecGBL : dxMeanTopElecSeed;
                    dxSigm = isGBL ? dxSigmTopElecGBL : dxSigmTopElecSeed;
                }
                else {
                    dxMean = isGBL ? dxMeanBotElecGBL : dxMeanBotElecSeed;
                    dxSigm = isGBL ? dxSigmBotElecGBL : dxSigmBotElecSeed;
                }
            }
            else return Double.MAX_VALUE;
        } else if(Math.abs(beamEnergy-2.306)<.1){
           
            if (charge>0) {
                if (isTopTrack) {
                    dxMean = !hasL6 ? dxMeanTopPosiGBL_noL6_2016 : dxMeanTopPosiGBL_hasL6_2016;
                    dxSigm = !hasL6 ? dxSigmTopPosiGBL_noL6_2016 : dxSigmTopPosiGBL_hasL6_2016;
                }
                else {
                    dxMean = !hasL6 ? dxMeanBotPosiGBL_noL6_2016 : dxMeanBotPosiGBL_hasL6_2016;
                    dxSigm = !hasL6 ? dxSigmBotPosiGBL_noL6_2016 : dxSigmBotPosiGBL_hasL6_2016;
                }
            }
            else if (charge<0) {
                if (isTopTrack) {
                    dxMean = !hasL6 ? dxMeanTopElecGBL_noL6_2016 : dxMeanTopElecGBL_hasL6_2016;
                    dxSigm = !hasL6 ? dxSigmTopElecGBL_noL6_2016 : dxSigmTopElecGBL_hasL6_2016;
                }
                else {
                    dxMean = !hasL6 ? dxMeanBotElecGBL_noL6_2016 : dxMeanBotElecGBL_hasL6_2016;
                    dxSigm = !hasL6 ? dxSigmBotElecGBL_noL6_2016 : dxSigmBotElecGBL_hasL6_2016;
                }
            }
            else return Double.MAX_VALUE;
        }
        else return Double.MAX_VALUE;

        // get particle energy:
        Hep3Vector p3 = new BasicHep3Vector(track.getTrackStates().get(0).getMomentum());
        p3 = CoordinateTransformations.transformVectorToDetector(p3);
        double ee = p3.magnitude();

        if(Math.abs(beamEnergy-1.056)<.1){
            // Rafo's parameterization isn't measured above 650 MeV/c but expected to be constant:
            if (ee > 0.65) 
                ee=0.65;
        }
        if(Math.abs(beamEnergy-2.306)<.1){
            // Sebouh's parameterization for L6 matching isn't measured above 1700 MeV/c but expected to be constant:
            if (ee > 1.7 && hasL6) 
                ee=1.7;
            //similarly for L5 matching, it is not measured above 1000 MeV but expected to be constant;
            if (ee > 1.0 && !hasL6) 
                ee=1.0;
            
        }

        // calculate measured mean and sigma of deltaX for this energy:
        double aDxMean=0,aDxSigm=0;
        for (int ii=dxMean.length-1; ii>=0; ii--) aDxMean = dxMean[ii] + ee*aDxMean;
        for (int ii=dxSigm.length-1; ii>=0; ii--) aDxSigm = dxSigm[ii] + ee*aDxSigm;


        // calculate nSigma between track and cluster:
        return  (cPos.x() - tPos.x() - aDxMean) / aDxSigm;

    }

    public double getNSigmaPositionY(Cluster cluster, Track track) {


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
        Hep3Vector tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
        tPos = CoordinateTransformations.transformVectorToDetector(tPos);

        // whether it's a GBL track:
        final boolean isGBL = track.getType() >= 32;


        // choose which parameterization of mean and sigma to use:
        double dyMean[],dySigm[];
        int charge = TrackUtils.getCharge(track);
        
        boolean hasL6 = false;
        for(TrackerHit hit : track.getTrackerHits()){
            if(TrackUtils.getLayer(hit) == 11)
                hasL6 = true;
        }
        
        if(Math.abs(beamEnergy-1.05)<.1){
            if (charge>0) {
                if (isTopTrack) {
                    dyMean = isGBL ? dyMeanTopPosiGBL : dyMeanTopPosiSeed;
                    dySigm = isGBL ? dySigmTopPosiGBL : dySigmTopPosiSeed;
                }
                else {
                    dyMean = isGBL ? dyMeanBotPosiGBL : dyMeanBotPosiSeed;
                    dySigm = isGBL ? dySigmBotPosiGBL : dySigmBotPosiSeed;
                }
            }
            else if (charge<0) {
                if (isTopTrack) {
                    dyMean = isGBL ? dyMeanTopElecGBL : dyMeanTopElecSeed;
                    dySigm = isGBL ? dySigmTopElecGBL : dySigmTopElecSeed;
                }
                else {
                    dyMean = isGBL ? dyMeanBotElecGBL : dyMeanBotElecSeed;
                    dySigm = isGBL ? dySigmBotElecGBL : dySigmBotElecSeed;
                }
            }
            else return Double.MAX_VALUE;
        }
        else if(Math.abs(beamEnergy-2.3)<.1){
           
            if (charge>0) {
                if (isTopTrack) {
                    dyMean = !hasL6 ? dyMeanTopPosiGBL_noL6_2016 : dyMeanTopPosiGBL_hasL6_2016;
                    dySigm = !hasL6 ? dySigmTopPosiGBL_noL6_2016 : dySigmTopPosiGBL_hasL6_2016;
                }
                else {
                    dyMean = !hasL6 ? dyMeanBotPosiGBL_noL6_2016 : dyMeanBotPosiGBL_hasL6_2016;
                    dySigm = !hasL6 ? dySigmBotPosiGBL_noL6_2016 : dySigmBotPosiGBL_hasL6_2016;
                }
            }
            else if (charge<0) {
                if (isTopTrack) {
                    dyMean = !hasL6 ? dyMeanTopElecGBL_noL6_2016 : dyMeanTopElecGBL_hasL6_2016;
                    dySigm = !hasL6 ? dySigmTopElecGBL_noL6_2016 : dySigmTopElecGBL_hasL6_2016;
                }
                else {
                    dyMean = !hasL6 ? dyMeanBotElecGBL_noL6_2016 : dyMeanBotElecGBL_hasL6_2016;
                    dySigm = !hasL6 ? dySigmBotElecGBL_noL6_2016 : dySigmBotElecGBL_hasL6_2016;
                }
            }
            else return Double.MAX_VALUE;
        }
        else return Double.MAX_VALUE;
        // get particle energy:
        Hep3Vector p3 = new BasicHep3Vector(track.getTrackStates().get(0).getMomentum());
        p3 = CoordinateTransformations.transformVectorToDetector(p3);
        double ee = p3.magnitude();

        if(Math.abs(beamEnergy-1.056)<.1){
            // Rafo's parameterization isn't measured above 650 MeV/c but expected to be constant:
            if (ee > 0.65) 
                ee=0.65;
        }
        if(Math.abs(beamEnergy-2.306)<.1){
            // Sebouh's parameterization for L6 matching isn't measured above 1700 MeV/c but expected to be constant:
            if (ee > 1.7 && hasL6) 
                ee=1.7;
            //similarly for L5 matching, it is not measured above 1000 MeV but expected to be constant;
            if (ee > 1.0 && !hasL6) 
                ee=1.0;
            
        }

        // calculate measured mean and sigma of deltaX and deltaY for this energy:
        double aDyMean=0,aDySigm=0;
        for (int ii=dyMean.length-1; ii>=0; ii--) aDyMean = dyMean[ii] + ee*aDyMean;
        for (int ii=dySigm.length-1; ii>=0; ii--) aDySigm = dySigm[ii] + ee*aDySigm;

        //if the track's extrapolated position is within 1/2 a crystal width of the edge of 
        // the ecal edge, then move it to be 1/2 a crystal from the edge in y.  
        tPos= snapToEdge(tPos, cluster);

        return (cPos.y() - tPos.y() - aDyMean) / aDySigm;

    }

    public double getNSigmaPosition(Cluster cluster, Track track){

        /*
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
        Hep3Vector tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
        tPos = CoordinateTransformations.transformVectorToDetector(tPos);

        // whether it's a GBL track:
        final boolean isGBL = track.getType() >= 32;


        // choose which parameterization of mean and sigma to use:
        double dxMean[],dyMean[],dxSigm[],dySigm[];
        int charge = TrackUtils.getCharge(track);
        if (charge>0) {
            if (isTopTrack) {
                dxMean = isGBL ? dxMeanTopPosiGBL : dxMeanTopPosiSeed;
                dxSigm = isGBL ? dxSigmTopPosiGBL : dxSigmTopPosiSeed;
                dyMean = isGBL ? dyMeanTopPosiGBL : dyMeanTopPosiSeed;
                dySigm = isGBL ? dySigmTopPosiGBL : dySigmTopPosiSeed;
            }
            else {
                dxMean = isGBL ? dxMeanBotPosiGBL : dxMeanBotPosiSeed;
                dxSigm = isGBL ? dxSigmBotPosiGBL : dxSigmBotPosiSeed;
                dyMean = isGBL ? dyMeanBotPosiGBL : dyMeanBotPosiSeed;
                dySigm = isGBL ? dySigmBotPosiGBL : dySigmBotPosiSeed;
            }
        }
        else if (charge<0) {
            if (isTopTrack) {
                dxMean = isGBL ? dxMeanTopElecGBL : dxMeanTopElecSeed;
                dxSigm = isGBL ? dxSigmTopElecGBL : dxSigmTopElecSeed;
                dyMean = isGBL ? dyMeanTopElecGBL : dyMeanTopElecSeed;
                dySigm = isGBL ? dySigmTopElecGBL : dySigmTopElecSeed;
            }
            else {
                dxMean = isGBL ? dxMeanBotElecGBL : dxMeanBotElecSeed;
                dxSigm = isGBL ? dxSigmBotElecGBL : dxSigmBotElecSeed;
                dyMean = isGBL ? dyMeanBotElecGBL : dyMeanBotElecSeed;
                dySigm = isGBL ? dySigmBotElecGBL : dySigmBotElecSeed;
            }
        }
        else return Double.MAX_VALUE;

        // get particle energy:
        Hep3Vector p3 = new BasicHep3Vector(track.getTrackStates().get(0).getMomentum());
        p3 = CoordinateTransformations.transformVectorToDetector(p3);
        double ee = p3.magnitude();

        // Rafo's parameterization isn't measured above 650 MeV/c but expected to be constant:
        if (ee > 0.65) ee=0.65;

        // calculate measured mean and sigma of deltaX and deltaY for this energy:
        double aDxMean=0,aDxSigm=0,aDyMean=0,aDySigm=0;
        for (int ii=dxMean.length-1; ii>=0; ii--) aDxMean = dxMean[ii] + ee*aDxMean;
        for (int ii=dxSigm.length-1; ii>=0; ii--) aDxSigm = dxSigm[ii] + ee*aDxSigm;
        for (int ii=dyMean.length-1; ii>=0; ii--) aDyMean = dyMean[ii] + ee*aDyMean;
        for (int ii=dySigm.length-1; ii>=0; ii--) aDySigm = dySigm[ii] + ee*aDySigm;

        //if the track's extrapolated position is within 1/2 a crystal width of the edge of 
        // the ecal edge, then move it to be 1/2 a crystal from the edge in y.  
        tPos= snapToEdge(tPos, cluster);

        // calculate nSigma between track and cluster:
        final double nSigmaX = (cPos.x() - tPos.x() - aDxMean) / aDxSigm;
        final double nSigmaY = (cPos.y() - tPos.y() - aDyMean) / aDySigm;
         */
        double nSigmaX = getNSigmaPositionX(cluster, track);
        double nSigmaY = getNSigmaPositionY(cluster, track);
        return Math.sqrt(nSigmaX*nSigmaX + nSigmaY*nSigmaY);
        //return Math.sqrt( 1 / ( 1/nSigmaX/nSigmaX + 1/nSigmaY/nSigmaY ) );
    }


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

    public void setEcalCrystalPositions(EcalCrystalPositionCollection positions) {
       this.positions = positions;
    }

    public void setEcalChannels(EcalChannelCollection channels) {
        this.channels = channels;
    }

    public void setBeamEnergy(double beamEnergy) {
        this.beamEnergy = beamEnergy;
    }



}
