package org.hps.users.mgraham;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.deprecated.BeamlineConstants;
import org.hps.recon.tracking.HPSTrack;
import org.hps.recon.tracking.HelixConverter;
import org.hps.recon.tracking.StraightLineTrack;
import org.hps.util.Resettable;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**

 @author mgraham
 */
public class ExamplePlotter extends Driver implements Resettable {

    //private AIDAFrame plotterFrame;
    private AIDA aida = AIDA.defaultInstance();
    IPlotter plotter;
    IAnalysisFactory fac = aida.analysisFactory();
    private String trackCollectionName = "MatchedTracks";
    private String outputPlots = null;

    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        //plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("HPS Tracking Plots");

        plotter = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter.setTitle("Momentum");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 3);
        //plotterFrame.addPlotter(plotter);

        IHistogram1D trkPx = aida.histogram1D("Track Momentum (Px)", 25, -0.25, 0.25);
        IHistogram1D trkPy = aida.histogram1D("Track Momentum (Py)", 25, -0.1, 0.1);
        IHistogram1D trkPz = aida.histogram1D("Track Momentum (Pz)", 25, 0, 3.5);
        IHistogram1D trkChi2 = aida.histogram1D("Track Chi2", 25, 0, 25.0);
        IHistogram1D xAtConvert = aida.histogram1D("X (mm) @ Converter", 50, -50, 50);
        IHistogram1D yAtConvert = aida.histogram1D("Y (mm) @ Converter", 50, -20, 20);
        plotter.region(0).plot(trkPx);
        plotter.region(1).plot(trkPy);
        plotter.region(2).plot(trkPz);
        plotter.region(3).plot(trkChi2);
        plotter.region(4).plot(xAtConvert);
        plotter.region(5).plot(yAtConvert);

        //plotterFrame.pack();
        //plotterFrame.setVisible(true);
    }

    public void process(EventHeader event) {
        aida.tree().cd("/");
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        for (Track trk : tracks) {
            aida.histogram1D("Track Momentum (Px)").fill(trk.getPY());
            aida.histogram1D("Track Momentum (Py)").fill(trk.getPZ());
            aida.histogram1D("Track Momentum (Pz)").fill(trk.getPX());
            aida.histogram1D("Track Chi2").fill(trk.getChi2());

            SeedTrack stEle = (SeedTrack) trk;
            SeedCandidate seedEle = stEle.getSeedCandidate();
            HelicalTrackFit ht = seedEle.getHelix();
            HelixConverter converter = new HelixConverter(0);
            StraightLineTrack slt = converter.Convert(ht);
            HPSTrack hpstrack = new HPSTrack(ht);
            Hep3Vector[] trkatconver = hpstrack.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION_TESTRUN, 1);
            aida.histogram1D("X (mm) @ Converter").fill(trkatconver[0].x()); // y tracker frame?
            aida.histogram1D("Y (mm) @ Converter").fill(trkatconver[0].y()); // z tracker frame?

        }
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void endOfData() {
        System.out.println("Output");
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(ExamplePlotter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
