package org.hps.monitoring.drivers.trackrecon;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hps.monitoring.drivers.trackrecon.PlotAndFitUtilities.plot;

import org.lcsim.event.EventHeader;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.Track;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
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
//        IPlotterStyle style22 = plotter22.style();
//        style22.dataStyle().fillStyle().setColor("yellow");
//        style22.dataStyle().errorBarStyle().setVisible(false);
        plotter22.createRegions(2, 3);
        plot(plotter22, nTracks, null, 0);
        plot(plotter22, trkd0, null, 1);
        plot(plotter22, trkphi, null, 2);
        plot(plotter22, trkomega, null, 3);
        plot(plotter22, trklam, null, 4);
        plot(plotter22, trkz0, null, 5);
        
        plotter22.show();

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

            trkPx.fill(trk.getTrackStates().get(0).getMomentum()[1]);
            trkPy.fill(trk.getTrackStates().get(0).getMomentum()[2]);
            trkPz.fill(trk.getTrackStates().get(0).getMomentum()[0]);
            trkChi2.fill(trk.getChi2());

            nhits.fill(trk.getTrackerHits().size());
            charge.fill(-trk.getCharge());
            trkd0.fill(trk.getTrackStates().get(0).getParameter(ParameterName.d0.ordinal()));
            trkphi.fill(Math.sin(trk.getTrackStates().get(0).getParameter(ParameterName.phi0.ordinal())));
            trkomega.fill(trk.getTrackStates().get(0).getParameter(ParameterName.omega.ordinal()));
            trklam.fill(trk.getTrackStates().get(0).getParameter(ParameterName.tanLambda.ordinal()));
            trkz0.fill(trk.getTrackStates().get(0).getParameter(ParameterName.z0.ordinal()));

//            SeedTrack stEle = (SeedTrack) trk;
//            SeedCandidate seedEle = stEle.getSeedCandidate();
//            HelicalTrackFit ht = seedEle.getHelix();
//            HelixConverter converter = new HelixConverter(0);
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

}
