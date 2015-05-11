package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.util.ArrayList;
import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class SVTOpeningStudies extends DataQualityMonitor {

    static private AIDA aida = AIDA.defaultInstance();
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String l1to3CollectionName = "L1to3Tracks";
    private String l4to6CollectionName = "L4to6Tracks";
    private String outputPlots = null;

    IHistogram1D nTracks46Top;
    IHistogram1D nTracks13Top;
    IHistogram1D nTracks46Bot;
    IHistogram1D nTracks13Bot;
    IHistogram1D deld0Top;
    IHistogram1D delphiTop;
    IHistogram1D delwTop;
    IHistogram1D dellambdaTop;
    IHistogram1D delz0Top;
    IHistogram1D deld0Bot;
    IHistogram1D delphiBot;
    IHistogram1D delwBot;
    IHistogram1D dellambdaBot;
    IHistogram1D delz0Bot;

    IHistogram1D nTracks46Pos;
    IHistogram1D nTracks13Pos;
    IHistogram1D nTracks46Ele;
    IHistogram1D nTracks13Ele;
    IHistogram1D deld0Pos;
    IHistogram1D delphiPos;
    IHistogram1D delwPos;
    IHistogram1D dellambdaPos;
    IHistogram1D delz0Pos;
    IHistogram1D deld0Ele;
    IHistogram1D delphiEle;
    IHistogram1D delwEle;
    IHistogram1D dellambdaEle;
    IHistogram1D delz0Ele;

    IHistogram2D d0Ele;
    IHistogram2D phiEle;
    IHistogram2D wEle;
    IHistogram2D lambdaEle;
    IHistogram2D z0Ele;

    IHistogram2D d0Pos;
    IHistogram2D phiPos;
    IHistogram2D wPos;
    IHistogram2D lambdaPos;
    IHistogram2D z0Pos;

    IHistogram1D nCombosTop;
    IHistogram1D nCombosBot;

    double rangeD0 = 50;
    double rangePhi0 = 0.25;
    double rangeOmega = 0.00025;
    double rangeSlope = 0.01;
    double rangeZ0 = 10;

    double pcut = 0.8;

    private final String plotDir = "SVTOpening/";

    public SVTOpeningStudies() {
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }

    public void setL1to3CollectionName(String trackCollectionName) {
        this.l1to3CollectionName = trackCollectionName;
    }

    public void setL4to6CollectionName(String trackCollectionName) {
        this.l4to6CollectionName = trackCollectionName;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");

        nTracks13Top = aida.histogram1D(plotDir + "Number of L1-3 Tracks: Top ", 7, 0, 7.0);
        nTracks46Top = aida.histogram1D(plotDir + "Number of L4-6 Tracks: Top ", 7, 0, 7.0);

        deld0Top = aida.histogram1D(plotDir + "Delta d0: Top", 50, -rangeD0, rangeD0);
        delphiTop = aida.histogram1D(plotDir + "Delta sin(phi): Top", 50, -rangePhi0, rangePhi0);
        delwTop = aida.histogram1D(plotDir + "Delta curvature: Top", 50, -rangeOmega, rangeOmega);
        dellambdaTop = aida.histogram1D(plotDir + "Delta slope: Top", 50, -rangeSlope, rangeSlope);
        delz0Top = aida.histogram1D(plotDir + "Delta y0: Top", 50, -rangeZ0, rangeZ0);

        nTracks13Bot = aida.histogram1D(plotDir + "Number of L1-3 Tracks: Bot ", 7, 0, 7.0);
        nTracks46Bot = aida.histogram1D(plotDir + "Number of L4-6 Tracks: Bot ", 7, 0, 7.0);

        deld0Bot = aida.histogram1D(plotDir + "Delta d0: Bot", 50, -rangeD0, rangeD0);
        delphiBot = aida.histogram1D(plotDir + "Delta sin(phi): Bot", 50, -rangePhi0, rangePhi0);
        delwBot = aida.histogram1D(plotDir + "Delta curvature: Bot", 50, -rangeOmega, rangeOmega);
        dellambdaBot = aida.histogram1D(plotDir + "Delta slope: Bot", 50, -rangeSlope, rangeSlope);
        delz0Bot = aida.histogram1D(plotDir + "Delta y0: Bot", 50, -rangeZ0, rangeZ0);

        nTracks13Ele = aida.histogram1D(plotDir + "Number of L1-3 Tracks: Ele ", 7, 0, 7.0);
        nTracks46Ele = aida.histogram1D(plotDir + "Number of L4-6 Tracks: Ele ", 7, 0, 7.0);

        deld0Ele = aida.histogram1D(plotDir + "Delta d0: Ele", 50, -rangeD0, rangeD0);
        delphiEle = aida.histogram1D(plotDir + "Delta sin(phi): Ele", 50, -rangePhi0, rangePhi0);
        delwEle = aida.histogram1D(plotDir + "Delta curvature: Ele", 50, -rangeOmega, rangeOmega);
        dellambdaEle = aida.histogram1D(plotDir + "Delta slope: Ele", 50, -rangeSlope, rangeSlope);
        delz0Ele = aida.histogram1D(plotDir + "Delta y0: Ele", 50, -rangeZ0, rangeZ0);

        nTracks13Pos = aida.histogram1D(plotDir + "Number of L1-3 Tracks: Pos ", 7, 0, 7.0);
        nTracks46Pos = aida.histogram1D(plotDir + "Number of L4-6 Tracks: Pos ", 7, 0, 7.0);

        deld0Pos = aida.histogram1D(plotDir + "Delta d0: Pos", 50, -rangeD0, rangeD0);
        delphiPos = aida.histogram1D(plotDir + "Delta sin(phi): Pos", 50, -rangePhi0, rangePhi0);
        delwPos = aida.histogram1D(plotDir + "Delta curvature: Pos", 50, -rangeOmega, rangeOmega);
        dellambdaPos = aida.histogram1D(plotDir + "Delta slope: Pos", 50, -rangeSlope, rangeSlope);
        delz0Pos = aida.histogram1D(plotDir + "Delta y0: Pos", 50, -rangeZ0, rangeZ0);

        d0Ele = aida.histogram2D(plotDir + "electrons d0: L46vs L13", 50, -rangeD0, rangeD0, 50, -rangeD0, rangeD0);
        phiEle = aida.histogram2D(plotDir + "electrons sin(phi): L46vs L13", 50, -rangePhi0, rangePhi0, 50, -rangePhi0, rangePhi0);
        wEle = aida.histogram2D(plotDir + "electrons curvature: L46vs L13", 50, -rangeOmega, rangeOmega, 50, -rangeOmega, rangeOmega);
        lambdaEle = aida.histogram2D(plotDir + "electrons slope: L46vs L13", 50, -10 * rangeSlope, 10 * rangeSlope, 50, -10 * rangeSlope, 10 * rangeSlope);
        z0Ele = aida.histogram2D(plotDir + "electrons y0: L46vs L13", 50, -rangeZ0, rangeZ0, 50, -rangeZ0, rangeZ0);

        d0Pos = aida.histogram2D(plotDir + "positrons d0: L46vs L13", 50, -rangeD0, rangeD0, 50, -rangeD0, rangeD0);
        phiPos = aida.histogram2D(plotDir + "positrons sin(phi): L46vs L13", 50, -rangePhi0, rangePhi0, 50, -rangePhi0, rangePhi0);
        wPos = aida.histogram2D(plotDir + "positrons curvature: L46vs L13", 50, -rangeOmega, rangeOmega, 50, -rangeOmega, rangeOmega);
        lambdaPos = aida.histogram2D(plotDir + "positrons slope: L46vs L13", 50, -10 * rangeSlope, 10 * rangeSlope, 50, -10 * rangeSlope, 10 * rangeSlope);
        z0Pos = aida.histogram2D(plotDir + "positrons y0: L46vs L13", 50, -rangeZ0, rangeZ0, 50, -rangeZ0, rangeZ0);

        nCombosTop = aida.histogram1D(plotDir + "Number of Combinations: Top", 7, 0, 7.0);
        nCombosBot = aida.histogram1D(plotDir + "Number of Combinations: Bot", 7, 0, 7.0);

    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");
        if (!event.hasCollection(HelicalTrackHit.class, helicalTrackHitCollectionName))
            return;

        if (!event.hasCollection(Track.class, l1to3CollectionName))
            return;

        if (!event.hasCollection(Track.class, l4to6CollectionName))
            return;

        List<Track> l1to3tracks = event.get(Track.class, l1to3CollectionName);
        List<Track> l4to6tracks = event.get(Track.class, l4to6CollectionName);

        List<Track> l1to3tracksTop = splitTrackList(l1to3tracks, true);
        List<Track> l1to3tracksBot = splitTrackList(l1to3tracks, false);
        List<Track> l4to6tracksTop = splitTrackList(l4to6tracks, true);
        List<Track> l4to6tracksBot = splitTrackList(l4to6tracks, false);

        List<Track> l1to3tracksEle = splitByCharge(l1to3tracks, -1);
        List<Track> l1to3tracksPos = splitByCharge(l1to3tracks, 1);
        List<Track> l4to6tracksEle = splitByCharge(l4to6tracks, -1);
        List<Track> l4to6tracksPos = splitByCharge(l4to6tracks, 1);

        nTracks13Top.fill(l1to3tracksTop.size());
        nTracks13Bot.fill(l1to3tracksBot.size());
        nTracks46Top.fill(l4to6tracksTop.size());
        nTracks46Bot.fill(l4to6tracksBot.size());

        nTracks13Ele.fill(l1to3tracksEle.size());
        nTracks13Pos.fill(l1to3tracksPos.size());
        nTracks46Ele.fill(l4to6tracksEle.size());
        nTracks46Pos.fill(l4to6tracksPos.size());

        int ncombotop = 0;
        int ncombobot = 0;

        for (Track trk46 : l4to6tracksTop) {
            TrackState ts46 = trk46.getTrackStates().get(0);
            for (Track trk13 : l1to3tracksTop)
                if (trk46.getMomentum()[0] > pcut && trk13.getMomentum()[0] > pcut) {
                    TrackState ts13 = trk13.getTrackStates().get(0);
                    deld0Top.fill(ts46.getD0() - ts13.getD0());
                    delphiTop.fill(Math.sin(ts46.getPhi()) - Math.sin(ts13.getPhi()));
                    delwTop.fill(ts46.getOmega() - ts13.getOmega());
                    delz0Top.fill(ts46.getZ0() - ts13.getZ0());
                    dellambdaTop.fill(ts46.getTanLambda() - ts13.getTanLambda());
                    ncombotop++;
                }
        }

        for (Track trk46 : l4to6tracksBot) {
            TrackState ts46 = trk46.getTrackStates().get(0);
            for (Track trk13 : l1to3tracksBot)
                if (trk46.getMomentum()[0] > pcut && trk13.getMomentum()[0] > pcut) {
                    TrackState ts13 = trk13.getTrackStates().get(0);
                    deld0Bot.fill(ts46.getD0() - ts13.getD0());
                    delphiBot.fill(Math.sin(ts46.getPhi()) - Math.sin(ts13.getPhi()));
                    delwBot.fill(ts46.getOmega() - ts13.getOmega());
                    delz0Bot.fill(ts46.getZ0() - ts13.getZ0());
                    dellambdaBot.fill(ts46.getTanLambda() - ts13.getTanLambda());
                    ncombobot++;
                }
        }

        nCombosTop.fill(ncombotop);
        nCombosBot.fill(ncombobot);

        for (Track trk46 : l4to6tracksEle) {
            TrackState ts46 = trk46.getTrackStates().get(0);
            for (Track trk13 : l1to3tracksEle) {
                TrackState ts13 = trk13.getTrackStates().get(0);
                deld0Ele.fill(ts46.getD0() - ts13.getD0());
                delphiEle.fill(Math.sin(ts46.getPhi()) - Math.sin(ts13.getPhi()));
                delwEle.fill(ts46.getOmega() - ts13.getOmega());
                delz0Ele.fill(ts46.getZ0() - ts13.getZ0());
                dellambdaEle.fill(ts46.getTanLambda() - ts13.getTanLambda());
                d0Ele.fill(ts46.getD0(), ts13.getD0());
                phiEle.fill(Math.sin(ts46.getPhi()), Math.sin(ts13.getPhi()));
                wEle.fill(ts46.getOmega(), ts13.getOmega());
                lambdaEle.fill(ts46.getTanLambda(), ts13.getTanLambda());
                z0Ele.fill(ts46.getZ0(), ts13.getZ0());

            }
        }

        for (Track trk46 : l4to6tracksPos) {
            TrackState ts46 = trk46.getTrackStates().get(0);
            for (Track trk13 : l1to3tracksPos) {
                TrackState ts13 = trk13.getTrackStates().get(0);
                deld0Pos.fill(ts46.getD0() - ts13.getD0());
                delphiPos.fill(Math.sin(ts46.getPhi()) - Math.sin(ts13.getPhi()));
                delwPos.fill(ts46.getOmega() - ts13.getOmega());
                delz0Pos.fill(ts46.getZ0() - ts13.getZ0());
                dellambdaPos.fill(ts46.getTanLambda() - ts13.getTanLambda());
                d0Pos.fill(ts46.getD0(), ts13.getD0());
                phiPos.fill(Math.sin(ts46.getPhi()), Math.sin(ts13.getPhi()));
                wPos.fill(ts46.getOmega(), ts13.getOmega());
                lambdaPos.fill(ts46.getTanLambda(), ts13.getTanLambda());
                z0Pos.fill(ts46.getZ0(), ts13.getZ0());
            }
        }

        /*
         l1to3tracksPos = null;
         l1to3tracksEle = null;
         l1to3tracksTop = null;
         l1to3tracksBot = null;

         l4to6tracksPos = null;
         l4to6tracksEle = null;
         l4to6tracksTop = null;
         l4to6tracksBot = null;
         */
    }

    @Override
    public void endOfData() {

    }

    private List<Track> splitTrackList(List<Track> trks, boolean doTop) {
        List<Track> tracksHalf = new ArrayList<Track>();
        boolean isTop = false;
        boolean isBot = false;
        for (Track trk : trks) {
            isTop = false;
            isBot = false;
            for (TrackerHit hit : trk.getTrackerHits())
                if (hit.getPosition()[2] > 0)//remember, non-bend in tracking frame is z-direction
                    isTop = true;
                else
                    isBot = true;
            if (isTop == true && isBot != true && doTop == true)  //if we want top tracks and all hits are in top
                tracksHalf.add(trk);
            if (isBot == true && isTop != true && doTop == false) //if we want bottom tracks and all hits are in bottom
                tracksHalf.add(trk);
        }
        return tracksHalf;
    }

    private List<Track> splitByCharge(List<Track> trks, int charge) {
        List<Track> tracksHalf = new ArrayList<>();
        boolean isTop = false;
        boolean isBot = false;
        for (Track trk : trks) {
            isTop = false;
            isBot = false;
            if (!(trk.getCharge() == charge)) { //XNOR but remember that the track charge is opposite because of B-field definition...
                for (TrackerHit hit : trk.getTrackerHits())
                    if (hit.getPosition()[2] > 0)//remember, non-bend in tracking frame is z-direction
                        isTop = true;
                    else
                        isBot = true;
                if (isTop == true && isBot != true)  //if  all hits are in top
                    tracksHalf.add(trk);
                if (isBot == true && isTop != true) //if all hits are in bottom
                    tracksHalf.add(trk);
            }
        }
        return tracksHalf;
    }
}
