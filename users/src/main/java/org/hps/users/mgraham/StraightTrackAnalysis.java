package org.hps.users.mgraham;

import hep.aida.IHistogram1D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.analysis.examples.TrackAnalysis;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Analysis driver for testing straight track (i.e. 0 b-field) development
 *
 * @author mgraham
 */
public class StraightTrackAnalysis extends Driver {

    protected AIDA aida = AIDA.defaultInstance();
    private String mcSvtHitsName = "TrackerHits";
//    private String rawHitsName = "RawTrackerHitMaker_RawTrackerHits";
      private String rawHitsName = "SVTRawTrackerHits";
    private String clustersName = "StripClusterer_SiTrackerHitStrip1D";
    private final String helicalTrackMCRelationsCollectionName = "HelicalTrackMCRelations";
    private final String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private final String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private String hthName = "HelicalTrackHits";
    private String trackCollectionName = "StraightTracks";

    int nevents = 0;

    public void setTrackCollectionName(String name){
        this.trackCollectionName=name;
    }
    
    public void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        IHistogram1D nSimHits = aida.histogram1D("Number of SVT Sim Hits", 25, 0, 25.0);
        IHistogram1D nRawHits = aida.histogram1D("Number of Raw Hits", 25, 0, 25.0);
        IHistogram1D nClusters = aida.histogram1D("Number of 1D Clusters", 25, 0, 25.0);
        IHistogram1D nHTH = aida.histogram1D("Number of HelicalTrackHits", 25, 0, 25.0);
        IHistogram1D nLayers = aida.histogram1D("Number of Layers Hit", 7, 0, 7);
        IHistogram1D nTracks = aida.histogram1D("Number of Tracks found", 5, 0, 5);
    }

    public void process(EventHeader event) {
        nevents++;
//        if (!event.hasCollection(SimTrackerHit.class, mcSvtHitsName))
//            return;
//        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, mcSvtHitsName);
//        aida.histogram1D("Number of SVT Sim Hits").fill(simHits.size());
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawHitsName);
        List<TrackerHit> clusters = event.get(TrackerHit.class, clustersName);
        List<TrackerHit> hths = event.get(TrackerHit.class, hthName);
        aida.histogram1D("Number of Raw Hits").fill(rawHits.size());
        aida.histogram1D("Number of 1D Clusters").fill(clusters.size());
        aida.histogram1D("Number of HelicalTrackHits").fill(hths.size());
        int[] hitInLayer = {0, 0, 0, 0, 0, 0};
        for (TrackerHit hit : hths) {
            int layer = ((RawTrackerHit) (hit.getRawHits().get(0))).getLayerNumber();
            if (layer == 1 || layer == 2)
                hitInLayer[0] = 1;
            if (layer == 3 || layer == 4)
                hitInLayer[1] = 1;
            if (layer == 5 || layer == 6)
                hitInLayer[2] = 1;
            if (layer == 7 || layer == 8)
                hitInLayer[3] = 1;
            if (layer == 9 || layer == 10)
                hitInLayer[4] = 1;
            if (layer == 11 || layer == 12)
                hitInLayer[5] = 1;
        }
        int totLayers = 0;
        for (int i = 0; i < 6; i++)
            totLayers += hitInLayer[i];
        aida.histogram1D("Number of Layers Hit").fill(totLayers);
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        aida.histogram1D("Number of Tracks found").fill(tracks.size());

        //make some maps and relation tables        
        Map<Track, TrackAnalysis> tkanalMap = new HashMap<Track, TrackAnalysis>();
        RelationalTable hittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> mcrelations = event.get(LCRelation.class, helicalTrackMCRelationsCollectionName);
        for (LCRelation relation : mcrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittomc.add(relation.getFrom(), relation.getTo());

        System.out.println("Size of hittomc collection " + hittomc.size());
        RelationalTable mcHittomcP = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        //  Get the collections of SimTrackerHits
        List<List<SimTrackerHit>> simcols = event.get(SimTrackerHit.class);
        //  Loop over the SimTrackerHits and fill in the relational table
        for (List<SimTrackerHit> simlist : simcols)
            for (SimTrackerHit simhit : simlist)
                if (simhit.getMCParticle() != null)
                    mcHittomcP.add(simhit, simhit.getMCParticle());

        RelationalTable trktomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    rawtomc.add(relation.getFrom(), relation.getTo());
        }

        RelationalTable hittostrip = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> hitrelations = event.get(LCRelation.class, helicalTrackHitRelationsCollectionName);
        for (LCRelation relation : hitrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittostrip.add(relation.getFrom(), relation.getTo());

        RelationalTable hittorotated = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> rotaterelations = event.get(LCRelation.class, rotatedHelicalTrackHitRelationsCollectionName);
        for (LCRelation relation : rotaterelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittorotated.add(relation.getFrom(), relation.getTo());

        for (Track trk : tracks) {
//            StraightTrack stght = (StraightTrack) trk;
            aida.histogram1D("d0", 50, -50, 50).fill(trk.getTrackStates().get(0).getParameters()[0]);
            aida.histogram1D("z0", 50, -50, 50).fill(trk.getTrackStates().get(0).getParameters()[3]);
            aida.histogram1D("xy slope", 50, -0.2, 0.25).fill(trk.getTrackStates().get(0).getParameters()[1]);
            aida.histogram1D("sz slope", 50, -0.25, 0.25).fill(trk.getTrackStates().get(0).getParameters()[4]);
            aida.histogram1D("track chi2 per ndf", 50, 0, 2).fill(trk.getChi2() / trk.getNDF());
            aida.histogram1D("track nhits", 50, 0, 10).fill(trk.getTrackerHits().size());
            TrackAnalysis tkanal = new TrackAnalysis(trk, hittomc);
        }

    }

}
