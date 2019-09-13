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

    /**
     * The B field map
     */
    FieldMap bFieldMap = null;

    // Plotting
    private ITree tree;
    private IHistogramFactory histogramFactory;
    private Map<String, IHistogram1D> plots1D;
    private Map<String, IHistogram2D> plots2D;

    // parameterization
    private Map<String, double[]> paramMap;

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

    public void initializeParameterization(String fname) {

        java.io.InputStream fis = TrackClusterMatcher.class.getResourceAsStream(fname);
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
                //                while (s.hasNext()) {
                //                float f = s.nextFloat();


            }
        } catch (IOException x) {
            System.err.format("TrackClusterMatcher error reading parameterization file: %s%n", x);
        }

        try {
            br.close();
        } catch (IOException x) {
            System.err.format("TrackClusterMatcher error closing parameterization file: %s%n", x);
        }
    }

    /**
     * Constructor
     */
    public TrackClusterMatcher() {
        this("ClusterParameterization2015.dat");
    }

    public TrackClusterMatcher(String fname) {
        initializeParameterization(fname);
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
   
    @Deprecated
    public void setBeamEnergy(double beamEnergy) {
    //          this.beamEnergy = beamEnergy;
    }

}
