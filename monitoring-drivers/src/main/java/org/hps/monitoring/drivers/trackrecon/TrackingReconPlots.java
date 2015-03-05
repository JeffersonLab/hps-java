package org.hps.monitoring.drivers.trackrecon;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    IHistogram1D trkPx;
    IHistogram1D nTracks;

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");

        IAnalysisFactory fac = aida.analysisFactory();
        plotter = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter.setTitle("Momentum");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 3);
        //plotterFrame.addPlotter(plotter);
        IHistogram1D nhits = aida.histogram1D("Hits per Track", 2, 5, 7);
        IHistogram1D charge = aida.histogram1D("Track Charge", 3, -1, 2);
        trkPx = aida.histogram1D("Track Momentum (Px)", 50, -0.1, 0.2);
        IHistogram1D trkPy = aida.histogram1D("Track Momentum (Py)", 50, -0.2, 0.2);
        IHistogram1D trkPz = aida.histogram1D("Track Momentum (Pz)", 50, 0, 3);
        IHistogram1D trkChi2 = aida.histogram1D("Track Chi2", 50, 0, 25.0);

        plotter.region(0).plot(nhits);
        plotter.region(1).plot(charge);
        plotter.region(2).plot(trkPx);
        plotter.region(3).plot(trkPy);
        plotter.region(4).plot(trkPz);
        plotter.region(5).plot(trkChi2);

//   ******************************************************************
        nTracks = aida.histogram1D("Number of Tracks ", 7, 0, 7.0);
        IHistogram1D trkd0 = aida.histogram1D("d0 ", 50, -5.0, 5.0);
        IHistogram1D trkphi = aida.histogram1D("sinphi ", 50, -0.1, 0.15);
        IHistogram1D trkomega = aida.histogram1D("omega ", 50, -0.0006, 0.0006);
        IHistogram1D trklam = aida.histogram1D("tan(lambda) ", 50, -0.1, 0.1);
        IHistogram1D trkz0 = aida.histogram1D("y0 ", 50, -1.0, 1.0);

        plotter22 = fac.createPlotterFactory().create("HPS Track Params");
        plotter22.setTitle("Track parameters");
        //plotterFrame.addPlotter(plotter22);
        IPlotterStyle style22 = plotter22.style();
        style22.dataStyle().fillStyle().setColor("yellow");
        style22.dataStyle().errorBarStyle().setVisible(false);
        plotter22.createRegions(2, 3);
        plotter22.region(0).plot(nTracks);
        plotter22.region(1).plot(trkd0);
        plotter22.region(2).plot(trkphi);
        plotter22.region(3).plot(trkomega);
        plotter22.region(4).plot(trklam);
        plotter22.region(5).plot(trkz0);

        plotter22.show();
        plotter.show();
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

            aida.histogram1D("Track Momentum (Px)").fill(trk.getTrackStates().get(0).getMomentum()[1]);
            aida.histogram1D("Track Momentum (Py)").fill(trk.getTrackStates().get(0).getMomentum()[2]);
            aida.histogram1D("Track Momentum (Pz)").fill(trk.getTrackStates().get(0).getMomentum()[0]);
            aida.histogram1D("Track Chi2").fill(trk.getChi2());

            aida.histogram1D("Hits per Track").fill(trk.getTrackerHits().size());
            aida.histogram1D("Track Charge").fill(-trk.getCharge());
            aida.histogram1D("d0 ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.d0.ordinal()));
            aida.histogram1D("sinphi ").fill(Math.sin(trk.getTrackStates().get(0).getParameter(ParameterName.phi0.ordinal())));
            aida.histogram1D("omega ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.omega.ordinal()));
            aida.histogram1D("tan(lambda) ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.tanLambda.ordinal()));
            aida.histogram1D("y0 ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.z0.ordinal()));

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
                plotter.writeToFile(outputPlots+"-mom.gif");
                 plotter22.writeToFile(outputPlots+"-trkparams.gif");
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconPlots.class.getName()).log(Level.SEVERE, null, ex);
            }

        //plotterFrame.dispose();
        //topFrame.dispose();
        //bottomFrame.dispose();
    }

}
