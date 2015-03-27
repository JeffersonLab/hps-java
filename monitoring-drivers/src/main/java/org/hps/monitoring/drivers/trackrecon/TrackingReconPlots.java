package org.hps.monitoring.drivers.trackrecon;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hps.monitoring.drivers.trackrecon.PlotAndFitUtilities.plot;
import org.hps.recon.tracking.HPSTrack;
import org.lcsim.event.Cluster;

import org.lcsim.event.EventHeader;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.Track;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class TrackingReconPlots extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String trackCollectionName = "MatchedTracks";
    String ecalSubdetectorName = "Ecal";
    String ecalCollectionName = "EcalClusters";
    IDDecoder dec;
    private String outputPlots = null;
    IPlotter plotter;
    IPlotter plotter22;
    IPlotter plotterECal;

    IHistogram1D nTracks;
    IHistogram1D nhits;
    IHistogram1D charge;
    IHistogram1D trkPx;
    IHistogram1D trkPy;
    IHistogram1D trkPz;
    IHistogram1D trkChi2;
    IHistogram1D trkd0;
    IHistogram1D trkphi;
    IHistogram1D trkomega;
    IHistogram1D trklam;
    IHistogram1D trkz0;
    IHistogram1D heOverP;
    IHistogram1D hdelXECal;
    IHistogram1D hdelYECal;
    IHistogram2D heVsP;

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");

        IAnalysisFactory fac = aida.analysisFactory();
        IPlotterFactory pfac = fac.createPlotterFactory("Track Recon");
        plotter = pfac.create("Momentum");

        plotter.createRegions(2, 3);
        //plotterFrame.addPlotter(plotter);
        nhits = aida.histogram1D("Hits per Track", 2, 5, 7);
        charge = aida.histogram1D("Track Charge", 3, -1, 2);
        trkPx = aida.histogram1D("Track Momentum (Px)", 50, -0.1, 0.2);
        trkPy = aida.histogram1D("Track Momentum (Py)", 50, -0.2, 0.2);
        trkPz = aida.histogram1D("Track Momentum (Pz)", 50, 0, 3);
        trkChi2 = aida.histogram1D("Track Chi2", 50, 0, 25.0);

        plot(plotter, nhits, null, 0);
        plot(plotter, charge, null, 1);
        plot(plotter, trkPx, null, 2);
        plot(plotter, trkPy, null, 3);
        plot(plotter, trkPz, null, 4);
        plot(plotter, trkChi2, null, 5);

        plotter.show();

//   ******************************************************************
        nTracks = aida.histogram1D("Number of Tracks ", 7, 0, 7.0);
        trkd0 = aida.histogram1D("d0 ", 50, -5.0, 5.0);
        trkphi = aida.histogram1D("sinphi ", 50, -0.1, 0.15);
        trkomega = aida.histogram1D("omega ", 50, -0.0006, 0.0006);
        trklam = aida.histogram1D("tan(lambda) ", 50, -0.1, 0.1);
        trkz0 = aida.histogram1D("y0 ", 50, -1.0, 1.0);

        plotter22 = pfac.create("Track parameters");
        plotter22.createRegions(2, 3);
        plot(plotter22, nTracks, null, 0);
        plot(plotter22, trkd0, null, 1);
        plot(plotter22, trkphi, null, 2);
        plot(plotter22, trkomega, null, 3);
        plot(plotter22, trklam, null, 4);
        plot(plotter22, trkz0, null, 5);

        plotter22.show();
        
        //   ******************************************************************
        heOverP = aida.histogram1D("Cluster Energy over Track Momentum ", 50, 0, 2.0);
        hdelXECal = aida.histogram1D("delta X @ ECal (mm) ", 50, -15.0, 15.0);
        hdelYECal = aida.histogram1D("delta Y @ ECal (mm) ", 50, -15.0, 15.0);
        heVsP = aida.histogram2D("Momentum vs ECal E ", 50, 0, 2.5 ,50, 0, 2.5);
      

        plotterECal = pfac.create("Cluster Matching");
        plotterECal.createRegions(2, 2);
        plot(plotterECal, heOverP, null, 0);
        plot(plotterECal, hdelXECal, null, 1);
        plot(plotterECal, hdelYECal, null, 2);
        plot(plotterECal, heVsP, null, 3);
      
        plotterECal.show();

    }

    public TrackingReconPlots() {
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
    }

    public void setFittedTrackerHitCollectionName(String fittedTrackerHitCollectionName) {
    }

    public void setTrackerHitCollectionName(String trackerHitCollectionName) {
    }

    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");
        if (!event.hasCollection(HelicalTrackHit.class, helicalTrackHitCollectionName))
            return;

        if (!event.hasCollection(Track.class, trackCollectionName)) {
            nTracks.fill(0);
            return;
        }

        List<Track> tracks = event.get(Track.class, trackCollectionName);
        nTracks.fill(tracks.size());

        for (Track trk : tracks) {
            Hep3Vector momentum =new BasicHep3Vector( trk.getTrackStates().get(0).getMomentum());
            trkPx.fill(momentum.y());
            trkPy.fill(momentum.z());
            trkPz.fill(momentum.x());
            trkChi2.fill(trk.getChi2());

            nhits.fill(trk.getTrackerHits().size());
            charge.fill(-trk.getCharge());
            trkd0.fill(trk.getTrackStates().get(0).getParameter(ParameterName.d0.ordinal()));
            trkphi.fill(Math.sin(trk.getTrackStates().get(0).getParameter(ParameterName.phi0.ordinal())));
            trkomega.fill(trk.getTrackStates().get(0).getParameter(ParameterName.omega.ordinal()));
            trklam.fill(trk.getTrackStates().get(0).getParameter(ParameterName.tanLambda.ordinal()));
            trkz0.fill(trk.getTrackStates().get(0).getParameter(ParameterName.z0.ordinal()));

            SeedTrack stEle = (SeedTrack) trk;
            SeedCandidate seedEle = stEle.getSeedCandidate();
            HelicalTrackFit ht = seedEle.getHelix();
            HPSTrack hpstrk = new HPSTrack(ht);
            double svt_l12 = 900.00;//mm ~approximately...this doesn't matter much
            double ecal_face = 1393.00;//mm ~approximately ... this matters!  Should use typical shower depth...or, once have cluster match, use that value of Z
            Hep3Vector posAtEcal = hpstrk.getPositionAtZMap(svt_l12, ecal_face, 5.0,event.getDetector().getFieldMap())[0];
            List<Cluster> clusters = event.get(Cluster.class, ecalCollectionName);
            if (clusters != null) {  
                System.out.println("Found "+clusters.size()+ " clusters");
                Cluster clust = findClosestCluster(posAtEcal, clusters);
                if (clust != null) {
                    System.out.println("\t\t\t Found the best clusters");
                    Hep3Vector clusterPos=new BasicHep3Vector(clust.getPosition());
                    double zCluster = clusterPos.z();
                    //improve the extrapolation...use the reconstructed cluster z-position
                    posAtEcal = hpstrk.getPositionAtZMap(svt_l12, zCluster, 5.0,event.getDetector().getFieldMap())[0];
                    double eOverP=clust.getEnergy()/momentum.magnitude();
                    double dx= posAtEcal.x() - clusterPos.x();
                      double dy= posAtEcal.y() - clusterPos.y();
                    heOverP.fill(eOverP);
                    hdelXECal.fill(dx);
                    hdelYECal.fill(dy);                  
                    heVsP.fill(momentum.magnitude(), clust.getEnergy());
                }
            }
        }

    }

    @Override
    public void endOfData() {
        if (outputPlots != null)
            try {
                plotter.writeToFile(outputPlots + "-mom.gif");
                plotter22.writeToFile(outputPlots + "-trkparams.gif");
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconPlots.class.getName()).log(Level.SEVERE, null, ex);
            }

        //plotterFrame.dispose();
        //topFrame.dispose();
        //bottomFrame.dispose();
    }

    /*
     *   mg...3/26/15...use this for now; eventually use the FinalStateParticle matching
     */
    private Cluster findClosestCluster(Hep3Vector posonhelix, List<Cluster> clusters) {
        Cluster closest = null;
        double minDist = 9999;
        for (Cluster cluster : clusters) {
            double[] clPos = cluster.getPosition();
            double clEne = cluster.getEnergy();
//            double dist = Math.sqrt(Math.pow(clPos[0] - posonhelix.y(), 2) + Math.pow(clPos[1] - posonhelix.z(), 2)); //coordinates!!!
            double dist = Math.sqrt(Math.pow(clPos[1] - posonhelix.z(), 2)); //coordinates!!!
            if (dist < minDist && clEne < 3.0) {
                closest = cluster;
                minDist = dist;
            }
        }
        return closest;
    }

}
