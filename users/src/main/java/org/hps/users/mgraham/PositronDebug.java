package org.hps.users.mgraham;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.util.ArrayList;
import java.util.List;
import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.TIData;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class PositronDebug extends Driver {

    static private AIDA aida = AIDA.defaultInstance();
    private final String helicalTrackHitCollectionName = "HelicalTrackHits";
    private final String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private final String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private final String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private final String trackCollectionName = "MatchedTracks";
    private final String trackerName = "Tracker";
    private static final String nameStrip = "Tracker_TestRunModule_";
    private String l1to3CollectionName = "L1to3Tracks";
    private String l4to6CollectionName = "L4to6Tracks";
    String ecalSubdetectorName = "Ecal";
    String ecalCollectionName = "EcalClusters";
    private Detector detector = null;
    private List<HpsSiSensor> sensors;
    private String triggerType = "pairs1";

    IHistogram1D nTracks46PosDebug;
    IHistogram1D nTracks13PosDebug;
    IHistogram1D deld0PosDebug;
    IHistogram1D delphiPosDebug;
    IHistogram1D delwPosDebug;
    IHistogram1D dellambdaPosDebug;
    IHistogram1D delz0PosDebug;

    IHistogram2D d0PosDebug;
    IHistogram2D phiPosDebug;
    IHistogram2D wPosDebug;
    IHistogram2D lambdaPosDebug;
    IHistogram2D z0PosDebug;

    double rangeD0 = 75;
    double rangePhi0 = 0.5;
    double rangeOmega = 0.00050;
    double rangeSlope = 0.06;
    double rangeZ0 = 15;

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        nTracks13PosDebug = aida.histogram1D("Number of L1-3 Tracks: PosDebug ", 7, 0, 7.0);
        nTracks46PosDebug = aida.histogram1D("Number of L4-6 Tracks: PosDebug ", 7, 0, 7.0);

        deld0PosDebug = aida.histogram1D("Delta d0: PosDebug", 50, -rangeD0, rangeD0);
        delphiPosDebug = aida.histogram1D("Delta sin(phi): PosDebug", 50, -rangePhi0, rangePhi0);
        delwPosDebug = aida.histogram1D("Delta curvature: PosDebug", 50, -rangeOmega, rangeOmega);
        dellambdaPosDebug = aida.histogram1D("Delta slope: PosDebug", 50, -rangeSlope, rangeSlope);
        delz0PosDebug = aida.histogram1D("Delta y0: PosDebug", 50, -rangeZ0, rangeZ0);

        d0PosDebug = aida.histogram2D("debug positrons d0: L46vs L13", 50, -rangeD0, rangeD0, 50, -rangeD0, rangeD0);
        phiPosDebug = aida.histogram2D("debug positrons sin(phi): L46vs L13", 50, -rangePhi0, rangePhi0, 50, -rangePhi0, rangePhi0);
        wPosDebug = aida.histogram2D("debug positrons curvature: L46vs L13", 50, -rangeOmega, rangeOmega, 50, -rangeOmega, rangeOmega);
        lambdaPosDebug = aida.histogram2D("debug positrons slope: L46vs L13", 50, -rangeSlope, rangeSlope, 50, -rangeSlope, rangeSlope);
        z0PosDebug = aida.histogram2D("debug positrons y0: L46vs L13", 50, -rangeZ0, rangeZ0, 50, -rangeZ0, rangeZ0);

    }

    @Override
    public void process(EventHeader event) {
        //select single trigger  type 
        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            for (GenericObject data : triggerList)
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG) {
                    TIData triggerData = new TIData(data);
                    if (!matchTriggerType(triggerData))//only process singles0 triggers...
                        return;
                }
        } else
            System.out.println(this.getClass().getSimpleName() + ":  No trigger bank found...running over all trigger types");

        if (!event.hasCollection(Track.class, trackCollectionName))
            return;

        boolean hasElectronTrack = false;
        boolean hasPositronTrack = false;
        boolean electronIsTop = false;
        Track eleTrack = null;
        Track posTrack = null;
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        for (Track trk : tracks)
            if (trk.getCharge() > 0) {
                hasElectronTrack = true;
                eleTrack = trk;
                if (trk.getTrackerHits().get(0).getPosition()[2] > 0)
                    electronIsTop = true;
            } else {
                System.out.println("Found a positron");
                System.out.println(trk.toString());
                posTrack = trk;
            }

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

        //if we have an electron, but no positron...why not?
        if (eleTrack != null && posTrack == null)
            if (electronIsTop)//positron should be on bottom
                fillPlots(l1to3tracksBot, l4to6tracksBot);
            else //positron in on top
                fillPlots(l1to3tracksTop, l4to6tracksTop);

    }

    public boolean matchTriggerType(TIData triggerData) {
        if (triggerType.contentEquals("") || triggerType.contentEquals("all"))
            return true;
        if (triggerData.isSingle0Trigger() && triggerType.contentEquals("singles0"))
            return true;
        if (triggerData.isSingle1Trigger() && triggerType.contentEquals("singles1"))
            return true;
        if (triggerData.isPair0Trigger() && triggerType.contentEquals("pairs0"))
            return true;
        return triggerData.isPair1Trigger() && triggerType.contentEquals("pairs1");
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

    private void fillPlots(List<Track> l1to3tracks, List<Track> l4to6tracks) {
        int ntrksL1to3Pass = 0;
        int ntrksL4to6Pass = 0;
        for (Track trk46 : l4to6tracks) {
            TrackState ts46 = trk46.getTrackStates().get(0);
            for (Track trk13 : l1to3tracks) {
                TrackState ts13 = trk13.getTrackStates().get(0);
                deld0PosDebug.fill(ts46.getD0() - ts13.getD0());
                delphiPosDebug.fill(Math.sin(ts46.getPhi()) - Math.sin(ts13.getPhi()));
                delwPosDebug.fill(ts46.getOmega() - ts13.getOmega());
                delz0PosDebug.fill(ts46.getZ0() - ts13.getZ0());
                dellambdaPosDebug.fill(ts46.getTanLambda() - ts13.getTanLambda());

                d0PosDebug.fill(ts46.getD0(), ts13.getD0());
                phiPosDebug.fill(Math.sin(ts46.getPhi()), Math.sin(ts13.getPhi()));
                wPosDebug.fill(ts46.getOmega(), ts13.getOmega());
                lambdaPosDebug.fill(ts46.getTanLambda(), ts13.getTanLambda());
                z0PosDebug.fill(ts46.getZ0(), ts13.getZ0());

            }
        }
    }

    private double trackTime(Track trk) {
        int nhits = trk.getTrackerHits().size();
        double sumTime = 0;
        for (TrackerHit trkHit : trk.getTrackerHits())
            sumTime += trkHit.getTime();
        return sumTime / nhits;
    }
}
