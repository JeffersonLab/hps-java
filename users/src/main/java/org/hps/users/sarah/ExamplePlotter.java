package org.hps.users.sarah;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.util.List;

import org.hps.util.Resettable;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
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

    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        //plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("HPS Tracking Plots");

        plotter = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter.setTitle("Momentum");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 2);
        //plotterFrame.addPlotter(plotter);

        IHistogram1D trkPx = aida.histogram1D("Track Momentum (Px)", 25, -0.25, 0.25);
        IHistogram1D trkPy = aida.histogram1D("Track Momentum (Py)", 25, -0.1, 0.1);
        IHistogram1D trkPz = aida.histogram1D("Track Momentum (Pz)", 25, 0, 3.5);
        IHistogram1D trkChi2 = aida.histogram1D("Track Chi2", 25, 0, 25.0);

        plotter.region(0).plot(trkPx);
        plotter.region(1).plot(trkPy);
        plotter.region(2).plot(trkPz);
        plotter.region(3).plot(trkChi2);

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
        }
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
