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
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.ecal.cluster.ClusterCorrectionUtilities;
import org.hps.recon.particle.SimpleParticleID;
import org.lcsim.event.base.BaseCluster;

import org.lcsim.event.Cluster;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.geometry.FieldMap;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.hps.record.StandardCuts;

/**
 * Utility used to determine if a track and cluster are matched.
 */
public class TrackClusterMatcherNSigma extends AbstractTrackClusterMatcher {

    static private final Logger LOGGER = Logger.getLogger(TrackClusterMatcherNSigma.class.getPackage().getName());

    public double getMatchQC(Cluster cluster, ReconstructedParticle particle){

        double matchqc = this.getNSigmaPosition(cluster, particle);
        return matchqc;
    }

    public void initializeParameterizationFile(String fname){

        this.initializeParameterization(fname);
    }

    /**
     * The B field map
     */
    FieldMap bFieldMap = null;

    // Plotting
    protected ITree tree;
    protected IHistogramFactory histogramFactory;
    protected Map<String, IHistogram1D> plots1D;
    protected Map<String, IHistogram2D> plots2D;
    protected String rootFile = "track_cluster_matching_plots.root";

    // parameterization
    private Map<String, double[]> paramMap;

    //normalized cluster-track distance required for qualifying as a match
    private double MAXNSIGMAPOSITIONMATCH = 15.0;
    private void setMAXNSIGMAPOSITIONMATCH(double MAXNSIGMAPOSITIONMATCH){
        this.MAXNSIGMAPOSITIONMATCH = MAXNSIGMAPOSITIONMATCH;
    }

    //Map of Clusters w Tracks if applyClusterCorrections
    protected HashMap<Cluster, Track> clusterToTrack;

    //THIS IS SET IN RECONPARTICLEDRIVE. HOW SHOULD I PASS THIS VALUE TO THE
    //MATCHERS? SHOULD I CRATE A setDisablePID() METHOD IN THE ABSTRACT CLASS,
    //AND CALL IT THAT WAY???
    private boolean disablePID = false;

    /**
     * Flag used to determine if plots are enabled/disabled
     */
    protected boolean enablePlots = false;

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


    /**
     * Rafo's parameterization of cluster-seed x/y position residuals as function of energy.
     * 
     * Derived using GBL/seed tracks, non-analytic extrapolation, uncorrected cluster positions,
     * and EngRun2015-Nominal-v4-4-fieldmap detector.
     * 
     *  f = p0+e*(p1+e*(p2+e*(p3+e*(p4+e*p5))))
     */

    private boolean snapToEdge = true;

    public void setSnapToEdge(boolean val){
        this.snapToEdge = val;
    }

    public void setRootFileName(String filename) {                                                                                                                 rootFile = filename;                                                                                                                                  }

    @Override
    public void initializeParameterization(String fname) {

        java.io.InputStream fis = TrackClusterMatcherNSigma.class.getResourceAsStream(fname);
        //fis = new java.io.FileInputStream(fin);
        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(fis));

        paramMap = new HashMap<String, double[]>();
        String line = null;

        try {
            while ((line = br.readLine()) != null) {
                // test line length
                String[] arrOfStr = line.split(" ");
                if (arrOfStr.length < 2) {
                    System.out.printf("TrackClusterError reading paramaterization file");
                    continue;
                }
                double[] paramVals = new double[arrOfStr.length - 1];

                for (int i=1; i<arrOfStr.length; i++) {
                    paramVals[i-1] = new Double(arrOfStr[i]);
                }
                paramMap.put(arrOfStr[0], paramVals);
                //                Scanner s = new Scanner(line);
                //                String paramName = s.next();
                //                
                //                while (s.hasNext()) 
                //                float f = s.nextFloat();


            }
        } catch (IOException x) {
            System.err.format("TrackClusterMatcherNSigma error reading parameterization file: %s%n", x);
        }

        try {
            br.close();
        } catch (IOException x) {
            System.err.format("TrackClusterMatcherNSigma error closing parameterization file: %s%n", x);
        }
    }

    /**
     * Constructor
     */
    public TrackClusterMatcherNSigma() {
        this("ClusterParameterization2015.dat");
    }

    public TrackClusterMatcherNSigma(String fname) {
        initializeParameterization(fname);
    }

    /**
     * Enable/disable booking, filling of Ecal cluster and extrapolated track 
     * position plots.
     * 
     * @param enablePlots true to enable, false to disable
     */
    @Override
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
    @Override
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

        boolean hasL6 = false;
        for(TrackerHit hit : track.getTrackerHits()){
            if(TrackUtils.getLayer(hit) == 11)
                hasL6 = true;
        }

        // choose which parameterization of mean and sigma to use:
        double dxMean[],dyMean[],dxSigm[],dySigm[];
        int charge = TrackUtils.getCharge(track);

        if (charge>0) {
            if (isTopTrack) {
                dxMean = !hasL6 ? paramMap.get("dxMeanTopPosiGBL_noL6") : paramMap.get("dxMeanTopPosiGBL_hasL6");
                dxSigm = !hasL6 ? paramMap.get("dxSigmTopPosiGBL_noL6") : paramMap.get("dxSigmTopPosiGBL_hasL6");
                dyMean = !hasL6 ? paramMap.get("dyMeanTopPosiGBL_noL6") : paramMap.get("dyMeanTopPosiGBL_hasL6");
                dySigm = !hasL6 ? paramMap.get("dySigmTopPosiGBL_noL6") : paramMap.get("dySigmTopPosiGBL_hasL6");
            }
            else {
                dxMean = !hasL6 ? paramMap.get("dxMeanBotPosiGBL_noL6") : paramMap.get("dxMeanBotPosiGBL_hasL6");
                dxSigm = !hasL6 ? paramMap.get("dxSigmBotPosiGBL_noL6") : paramMap.get("dxSigmBotPosiGBL_hasL6");
                dyMean = !hasL6 ? paramMap.get("dyMeanBotPosiGBL_noL6") : paramMap.get("dyMeanBotPosiGBL_hasL6");
                dySigm = !hasL6 ? paramMap.get("dySigmBotPosiGBL_noL6") : paramMap.get("dySigmBotPosiGBL_hasL6");
            }
        }
        else if (charge<0) {
            if (isTopTrack) {
                dxMean = !hasL6 ? paramMap.get("dxMeanTopElecGBL_noL6") : paramMap.get("dxMeanTopElecGBL_hasL6");
                dxSigm = !hasL6 ? paramMap.get("dxSigmTopElecGBL_noL6") : paramMap.get("dxSigmTopElecGBL_hasL6");
                dyMean = !hasL6 ? paramMap.get("dyMeanTopElecGBL_noL6") : paramMap.get("dyMeanTopElecGBL_hasL6");
                dySigm = !hasL6 ? paramMap.get("dySigmTopElecGBL_noL6") : paramMap.get("dySigmTopElecGBL_hasL6");
            }
            else {
                dxMean = !hasL6 ? paramMap.get("dxMeanBotElecGBL_noL6") : paramMap.get("dxMeanBotElecGBL_hasL6");
                dxSigm = !hasL6 ? paramMap.get("dxSigmBotElecGBL_noL6") : paramMap.get("dxSigmBotElecGBL_hasL6");
                dyMean = !hasL6 ? paramMap.get("dyMeanBotElecGBL_noL6") : paramMap.get("dyMeanBotElecGBL_hasL6");
                dySigm = !hasL6 ? paramMap.get("dySigmBotElecGBL_noL6") : paramMap.get("dySigmBotElecGBL_hasL6");
            }
        }
        else return Double.MAX_VALUE;



        // Beyond the edges of the fits in momentum, assume that the parameters are constant:
        if ((p > paramMap.get("pHigh_hasL6")[0]) && hasL6) 
            p = paramMap.get("pHigh_hasL6")[0];
        else if ((p > paramMap.get("pHigh_noL6")[0]) && !hasL6) 
            p= paramMap.get("pHigh_noL6")[0];
        else if ((p < paramMap.get("pLow_hasL6")[0]) && hasL6)
            p = paramMap.get("pLow_hasL6")[0];
        else if ((p < paramMap.get("pLow_noL6")[0]) && !hasL6)
            p = paramMap.get("pLow_noL6")[0];

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

        if(Math.abs(cPos.x()-tPos.x())<50 &&  Math.abs(cPos.y()-tPos.y())<50){
            LOGGER.fine("TC MATCH RESULTS:");
            LOGGER.fine("isTop:  " + isTopTrack);
            LOGGER.fine("charge: " + charge);
            LOGGER.fine("hasL6:  " + hasL6);
            LOGGER.fine("p: " + p);
            LOGGER.fine("cx: " + cPos.x());
            LOGGER.fine("cy: " + cPos.y());
            LOGGER.fine("tx: " + originalTPos.x());
            LOGGER.fine("ty: " + originalTPos.y());

            LOGGER.fine("nSigmaX: " + nSigmaX);
            LOGGER.fine("nSigmaY: " + nSigmaY);
            LOGGER.fine("nSigma: " + nSigma);
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

        // Extrapolate the track to the Ecal cluster position
        Hep3Vector trackPosAtEcal = null;
        if (this.useAnalyticExtrapolator) {
            trackPosAtEcal = TrackUtils.extrapolateTrack(track, clusterPosition.z());
        } else {
            TrackState trackStateAtEcal = TrackUtils.getTrackStateAtECal(track);
            trackPosAtEcal = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
            trackPosAtEcal = CoordinateTransformations.transformVectorToDetector(trackPosAtEcal);
        }

        // Calculate the difference between the cluster position at the Ecal and
        // the track in both the x and y directions
        double deltaX = clusterPosition.x() - trackPosAtEcal.x();
        double deltaY = clusterPosition.y() - trackPosAtEcal.y();

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
        if ((track.getTrackStates().get(0).getTanLambda() > 0 && (deltaX > paramMap.get("topClusterTrackMatchDeltaXHigh")[0]
                        || deltaX < paramMap.get("topClusterTrackMatchDeltaXLow")[0]))
                || (track.getTrackStates().get(0).getTanLambda() < 0 && (deltaX > paramMap.get("bottomClusterTrackMatchDeltaXHigh")[0]
                        || deltaX < paramMap.get("bottomClusterTrackMatchDeltaXLow")[0]))) {
            return false;
        }

        if ((track.getTrackStates().get(0).getTanLambda() > 0 && (deltaY > paramMap.get("topClusterTrackMatchDeltaYHigh")[0]
                        || deltaY < paramMap.get("topClusterTrackMatchDeltaYLow")[0]))
                || (track.getTrackStates().get(0).getTanLambda() < 0 && (deltaY > paramMap.get("bottomClusterTrackMatchDeltaYHigh")[0]
                        || deltaY < paramMap.get("bottomClusterTrackMatchDeltaYLow")[0]))) {
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
    @Override
    public void bookHistograms() {

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
     * Applies EcalCluster corrections, with option to use Track position for
     * corrections.
     */
    @Override
    public void applyClusterCorrections(boolean useTrackPositionForClusterCorrection, List<Cluster> clusters, double beamEnergy, HPSEcal3 ecal, boolean isMC){
        // Apply the corrections to the Ecal clusters using track information, if available
        for (Cluster cluster : clusters) {
            if (cluster.getParticleId() != 0) {
                if (useTrackPositionForClusterCorrection && this.clusterToTrack.containsKey(cluster)) {
                    Track matchedT = clusterToTrack.get(cluster);
                    double ypos = TrackUtils.getTrackStateAtECal(matchedT).getReferencePoint()[2];
                    ClusterCorrectionUtilities.applyCorrections(beamEnergy, ecal, cluster, ypos, isMC);
                } else {
                    ClusterCorrectionUtilities.applyCorrections(beamEnergy, ecal, cluster, isMC);
                }
            }
        }

    }

    /**
     * Return Map of Tracks with matched EcalCluster. 
     * Tracks are not matched to unique Clusters, and the same Cluster may be
     * matched to different Tracks.
     * If Track is not matched to a Cluster, Map value set to null
     */
    @Override
    public HashMap<Track,Cluster> matchTracksToClusters(EventHeader event, List<List<Track>> trackCollections, List<Cluster> clusters, StandardCuts cuts, int flipSign, boolean useCorrectedClusterPositionsForMatching, boolean isMC, HPSEcal3 ecal, double beamEnergy){

        //Relational Tables used to get Track data
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);

        // Create a list in which to store reconstructed particles.
        List<ReconstructedParticle> particles = new ArrayList<ReconstructedParticle>();

        // Create a mapping of matched clusters to corresponding tracks.
        this.clusterToTrack = new HashMap<Cluster, Track>();

        // Create a mapping of Tracks and their corresponding Clusters.
        HashMap<Track, Cluster> trackClusterPairs = new HashMap<Track, Cluster>();

        // Loop through all of the track collections and try to match every
        // track to a cluster. Allow a cluster to be matched to multiple
        // tracks and use a probability (to be coded later) to determine what
        // the best match is.
        for (List<Track> tracks : trackCollections) {

            for (Track track : tracks){

                //create a mapping of smallestNSigma Clusters and their sigma values
                HashMap<Cluster,Double> clusterNSigma = new HashMap<Cluster,Double>();

                //Create a reconstructed particle to represent the track.
                //ReconstructedParticle particle = super.addTrackToParticle(track, flipSign);
                ReconstructedParticle particle = new BaseReconstructedParticle();
                // Store the track in the particle.
                particle.addTrack(track);

                // Set the type of the particle. This is used to identify
                // the tracking strategy used in finding the track associated with
                // this particle.
                ((BaseReconstructedParticle) particle).setType(track.getType());

                // Derive the charge of the particle from the track.
                int charge = (int) Math.signum(track.getTrackStates().get(0).getOmega());
                ((BaseReconstructedParticle) particle).setCharge(charge * flipSign);

                // initialize PID quality to a junk value:
                ((BaseReconstructedParticle) particle).setGoodnessOfPid(9999);

                // Extrapolate the particle ID from the track. Positively
                // charged particles are assumed to be positrons and those
                // with negative charges are assumed to be electrons.
                if (particle.getCharge() > 0) {
                    ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(-11, 0, 0, 0));
                } else if (particle.getCharge() < 0) {
                    ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(11, 0, 0, 0));
                }

                double smallestNSigma = Double.MAX_VALUE;
                // try to find a matching cluster:
                Cluster matchedCluster = null;
                for(Cluster cluster : clusters) {
                    double clusTime = ClusterUtilities.getSeedHitTime(cluster);
                    double trkT = TrackUtils.getTrackTime(track, hitToStrips, hitToRotated);

                    if (Math.abs(clusTime - trkT - cuts.getTrackClusterTimeOffset()) > cuts.getMaxMatchDt()) {
                        LOGGER.fine("Failed cluster-track deltaT!");
                        LOGGER.fine(clusTime + "  " + trkT + "  " + cuts.getTrackClusterTimeOffset() + ">" + cuts.getMaxMatchDt());
                        continue;
                    }

                    //if the option to use corrected cluster positions is selected, then
                    //create a copy of the current cluster, and apply corrections to it
                    //before calculating nsigma.  Default is don't use corrections.  
                    Cluster originalCluster = cluster;
                    if (useCorrectedClusterPositionsForMatching) {
                        BaseCluster clusterBase = new BaseCluster(cluster);
                        clusterBase.setNeedsPropertyCalculation(false);
                        cluster = clusterBase;
                        double ypos = TrackUtils.getTrackStateAtECal(particle.getTracks().get(0)).getReferencePoint()[2];
                        ClusterCorrectionUtilities.applyCorrections(beamEnergy, ecal, cluster, ypos, isMC);
                    }                    

                    // normalized distance between this cluster and track:
                    final double thisNSigma = this.getNSigmaPosition(cluster, particle);

                    if (enablePlots) {
                        if (TrackUtils.getTrackStateAtECal(track) != null) {
                            this.isMatch(cluster, track);
                        }
                    }

                    // ignore if matching quality doesn't make the cut:
                    if (thisNSigma > MAXNSIGMAPOSITIONMATCH) {
                        LOGGER.fine("Failed cluster-track NSigma Cut!");
                        LOGGER.fine("match NSigma = " + thisNSigma + "; Max NSigma =  " + MAXNSIGMAPOSITIONMATCH);
                        continue;
                    }

                    // ignore if we already found a cluster that's a better match:
                    if (thisNSigma > smallestNSigma) {
                        LOGGER.fine("Already found a better match than this!");
                        LOGGER.fine("match NSigma = " + thisNSigma + "; smallest NSigma =  " + smallestNSigma);
                        continue;
                    }
                    // we found a new best cluster candidate for this track:
                    smallestNSigma = thisNSigma;
                    matchedCluster = originalCluster;

                    // prefer using GBL tracks to correct (later) the clusters, for some consistency:
                    if (track.getType() >= 32 || !this.clusterToTrack.containsKey(matchedCluster)) {
                        this.clusterToTrack.put(matchedCluster, track);
                    }
                }
                trackClusterPairs.put(track, matchedCluster);
            }
        }
        return trackClusterPairs;
    }

    /**
     * Save the histograms to a ROO file
     */
    @Override
    public void saveHistograms() {

        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    @Override
    public void setBeamEnergy(double beamEnergy) {
        //          this.beamEnergy = beamEnergy;
    }

}
