package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hep.physics.vec.Hep3Vector;

import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;

/**
 * Full per-hit dump of a handful of named events, for debugging a specific case where a
 * real track lost its truth link under pulser overlay.
 *
 * SvtHitProvenanceDriver answers the aggregate question. This one answers the forensic
 * question for one event, and in particular separates the two ways a real particle's
 * track can end up with zero truth relations:
 *
 *   A. The particle's SimTrackerHits never produced a truthed raw hit. The digitization
 *      dropped the relation -- e.g. the MC contribution failed the G1 threshold on a
 *      channel that a pulser hit kept alive (MERGED_SUBTHRESH).
 *
 *   B. The particle's SimTrackerHits DID produce truthed raw hits, but pattern
 *      recognition built the track out of different raw hits instead.
 *
 * The distinguishing measurement is per SimTrackerHit: walk SVTTrueHitRelations backwards
 * to the raw hits that carry it, then ask whether those raw hits are on the track. A is
 * "no raw hit carries this SimTrackerHit"; B is "one does, but the track did not use it".
 *
 * Requires the readout to have been run with writeHitOriginCollections=true.
 *
 * Configure with one or more <eventNumber> entries; with none, every event is dumped, so
 * always set at least one.
 */
public class SvtEventForensicsDriver extends Driver {

    private String trackCollectionName = "KalmanFullTracks";
    private String truthRelationCollectionName = "SVTTrueHitRelations";
    private String pulserOriginCollectionName = "SVTHitOriginPulser";
    private String mcContribCollectionName = "SVTHitOriginMCContrib";
    private String simHitCollectionName = "TrackerHits";
    private String rawHitCollectionName = "SVTRawTrackerHits";
    private String clusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String[] vertexCollectionNames = { "UnconstrainedV0Vertices_KF" };

    /** Half-width, in strips, of the neighbourhood listed around a lost hit. */
    private int neighbourWindow = 12;

    /** Emit one "RAWHIT event sensor channel category" line per raw hit, for diffing arms. */
    private boolean dumpAllRawHits = false;

    /** Only dump MCParticles above this momentum, to keep the listing readable. [GeV] */
    private double mcpMinMomentum = 0.05;

    private final Set<Integer> eventNumbers = new HashSet<Integer>();

    private static final int NOISE = 0, MC_PURE = 1, MC_PURE_SUBTHRESH = 2,
                             PULSER_PURE = 3, MERGED = 4, MERGED_SUBTHRESH = 5;
    private static final String[] CAT_NAME = {
        "NOISE", "MC_PURE", "MC_PURE_SUBTHRESH", "PULSER_PURE", "MERGED", "MERGED_SUBTHRESH"
    };

    public void setTrackCollectionName(String val) { this.trackCollectionName = val; }
    public void setTruthRelationCollectionName(String val) { this.truthRelationCollectionName = val; }
    public void setPulserOriginCollectionName(String val) { this.pulserOriginCollectionName = val; }
    public void setMcContribCollectionName(String val) { this.mcContribCollectionName = val; }
    public void setSimHitCollectionName(String val) { this.simHitCollectionName = val; }
    public void setRawHitCollectionName(String val) { this.rawHitCollectionName = val; }
    public void setClusterCollectionName(String val) { this.clusterCollectionName = val; }
    public void setNeighbourWindow(int val) { this.neighbourWindow = val; }
    public void setDumpAllRawHits(boolean val) { this.dumpAllRawHits = val; }
    public void setVertexCollectionNames(String val) { this.vertexCollectionNames = val.split(" +"); }
    public void setMcpMinMomentum(double val) { this.mcpMinMomentum = val; }

    /** Add one event number to dump. Repeat the element to dump several. */
    public void setEventNumber(int val) { this.eventNumbers.add(val); }

    private Set<RawTrackerHit> fromSide(EventHeader event, String name) {
        Set<RawTrackerHit> out = new HashSet<RawTrackerHit>();
        if(!event.hasCollection(LCRelation.class, name)) { return out; }
        for(LCRelation rel : event.get(LCRelation.class, name)) {
            if(rel.getFrom() instanceof RawTrackerHit) { out.add((RawTrackerHit) rel.getFrom()); }
        }
        return out;
    }

    private static String sensorOf(RawTrackerHit hit) {
        return hit.getDetectorElement().getName();
    }

    private static int channelOf(RawTrackerHit hit) {
        return hit.getIdentifierFieldValue("strip");
    }

    private static int categorise(RawTrackerHit hit, Set<RawTrackerHit> pulser,
                                  Set<RawTrackerHit> mcContrib, Set<RawTrackerHit> truthed) {
        boolean p = pulser.contains(hit);
        boolean m = mcContrib.contains(hit);
        boolean t = truthed.contains(hit);
        if(!m) { return p ? PULSER_PURE : NOISE; }
        if(p)  { return t ? MERGED : MERGED_SUBTHRESH; }
        return t ? MC_PURE : MC_PURE_SUBTHRESH;
    }

    private static double momentum(MCParticle p) {
        return p.getMomentum().magnitude();
    }

    /** tan(lambda) for an MCParticle, matching the track-parameter convention. */
    private static double tanLambda(MCParticle p) {
        Hep3Vector m = p.getMomentum();
        double pt = Math.hypot(m.x(), m.z());
        return pt > 0 ? m.y() / pt : Double.NaN;
    }

    @Override
    protected void process(EventHeader event) {
        if(!eventNumbers.isEmpty() && !eventNumbers.contains(event.getEventNumber())) { return; }

        Set<RawTrackerHit> pulser = fromSide(event, pulserOriginCollectionName);
        Set<RawTrackerHit> mcContrib = fromSide(event, mcContribCollectionName);
        Set<RawTrackerHit> truthed = fromSide(event, truthRelationCollectionName);

        // raw hit -> the SimTrackerHits it carries, and the reverse
        Map<RawTrackerHit, List<SimTrackerHit>> rawToSim = new HashMap<RawTrackerHit, List<SimTrackerHit>>();
        Map<SimTrackerHit, List<RawTrackerHit>> simToRaw = new HashMap<SimTrackerHit, List<RawTrackerHit>>();
        if(event.hasCollection(LCRelation.class, truthRelationCollectionName)) {
            for(LCRelation rel : event.get(LCRelation.class, truthRelationCollectionName)) {
                if(!(rel.getFrom() instanceof RawTrackerHit)) { continue; }
                if(!(rel.getTo() instanceof SimTrackerHit)) { continue; }
                RawTrackerHit r = (RawTrackerHit) rel.getFrom();
                SimTrackerHit s = (SimTrackerHit) rel.getTo();
                if(!rawToSim.containsKey(r)) { rawToSim.put(r, new ArrayList<SimTrackerHit>()); }
                rawToSim.get(r).add(s);
                if(!simToRaw.containsKey(s)) { simToRaw.put(s, new ArrayList<RawTrackerHit>()); }
                simToRaw.get(s).add(r);
            }
        }

        int nRaw = event.hasCollection(RawTrackerHit.class, rawHitCollectionName)
                 ? event.get(RawTrackerHit.class, rawHitCollectionName).size() : -1;

        System.out.println();
        System.out.println("################ FORENSICS run " + event.getRunNumber()
                           + " event " + event.getEventNumber() + " ################");
        System.out.println("  raw hits in event      : " + nRaw);
        System.out.println("  with truth relation    : " + truthed.size());
        System.out.println("  with pulser origin     : " + pulser.size());
        System.out.println("  with MC contribution   : " + mcContrib.size());

        // ---- machine-readable dump of every raw hit ----
        // Emitted so the two arms can be diffed channel by channel. The question it
        // answers: are the hits labelled PULSER_PURE really independent data, or are
        // they a second copy of the MC deposit at shifted channels? A PULSER_PURE
        // channel that also fires in the no-pulser arm cannot have come from the
        // pulser file, since that arm has no pulser file.
        if(dumpAllRawHits && event.hasCollection(RawTrackerHit.class, rawHitCollectionName)) {
            for(RawTrackerHit r : event.get(RawTrackerHit.class, rawHitCollectionName)) {
                System.out.printf("RAWHIT %d %s %d %s%n", event.getEventNumber(), sensorOf(r),
                                  channelOf(r), CAT_NAME[categorise(r, pulser, mcContrib, truthed)]);
            }
        }

        // ---- MCParticles of interest ----
        System.out.println("  -- MCParticles (|pdg|=11, p > " + mcpMinMomentum + ") --");
        List<MCParticle> mcps = event.getMCParticles();
        for(MCParticle p : mcps) {
            if(Math.abs(p.getPDGID()) != 11 || momentum(p) < mcpMinMomentum) { continue; }
            System.out.printf("     pdg=%+3d p=%.4f tanL=%+.5f vtx_z=%+.3f%n",
                              p.getPDGID(), momentum(p), tanLambda(p), p.getOriginZ());
        }

        // ---- SimTrackerHits grouped by particle ----
        Map<MCParticle, List<SimTrackerHit>> simByParticle = new HashMap<MCParticle, List<SimTrackerHit>>();
        if(event.hasCollection(SimTrackerHit.class, simHitCollectionName)) {
            for(SimTrackerHit s : event.get(SimTrackerHit.class, simHitCollectionName)) {
                MCParticle p = s.getMCParticle();
                if(p == null) { continue; }
                if(!simByParticle.containsKey(p)) { simByParticle.put(p, new ArrayList<SimTrackerHit>()); }
                simByParticle.get(p).add(s);
            }
        }

        // ---- raw hit -> the cluster containing it ----
        // Tracking never sees raw hits; it sees the clusters TrackerHitDriver builds. A
        // raw hit that is present and truthed but is in no cluster, or in a cluster no
        // track used, has been removed from tracking's view before any pattern
        // recognition happens -- which is a different failure from losing a
        // combinatorial competition.
        Map<RawTrackerHit, TrackerHit> rawToCluster = new HashMap<RawTrackerHit, TrackerHit>();
        List<TrackerHit> clusters = event.hasCollection(TrackerHit.class, clusterCollectionName)
                                  ? event.get(TrackerHit.class, clusterCollectionName)
                                  : new ArrayList<TrackerHit>();
        for(TrackerHit c : clusters) {
            for(Object o : c.getRawHits()) {
                if(o instanceof RawTrackerHit) { rawToCluster.put((RawTrackerHit) o, c); }
            }
        }

        // sensor -> every raw hit on it, so the neighbourhood of a lost hit can be shown
        Map<String, List<RawTrackerHit>> bySensor = new HashMap<String, List<RawTrackerHit>>();
        if(event.hasCollection(RawTrackerHit.class, rawHitCollectionName)) {
            for(RawTrackerHit r : event.get(RawTrackerHit.class, rawHitCollectionName)) {
                String s = sensorOf(r);
                if(!bySensor.containsKey(s)) { bySensor.put(s, new ArrayList<RawTrackerHit>()); }
                bySensor.get(s).add(r);
            }
        }

        // ---- raw hits used by each track ----
        Map<RawTrackerHit, Integer> hitToTrack = new HashMap<RawTrackerHit, Integer>();
        List<Track> tracks = event.hasCollection(Track.class, trackCollectionName)
                           ? event.get(Track.class, trackCollectionName) : new ArrayList<Track>();
        for(int it = 0; it < tracks.size(); it++) {
            for(TrackerHit th : tracks.get(it).getTrackerHits()) {
                for(Object o : th.getRawHits()) {
                    if(o instanceof RawTrackerHit) { hitToTrack.put((RawTrackerHit) o, it); }
                }
            }
        }

        // ---- per-track hit listing ----
        System.out.println("  -- tracks in " + trackCollectionName + ": " + tracks.size() + " --");
        for(int it = 0; it < tracks.size(); it++) {
            Track t = tracks.get(it);
            TrackState ts = t.getTrackStates().isEmpty() ? null : t.getTrackStates().get(0);
            double[] mom = ts != null ? ts.getMomentum() : new double[]{0, 0, 0};
            double pmag = Math.sqrt(mom[0]*mom[0] + mom[1]*mom[1] + mom[2]*mom[2]);
            double tanL = ts != null ? ts.getTanLambda() : Double.NaN;
            System.out.printf("   track %d: charge=%+d p=%.4f tanL=%+.5f chi2=%.2f ndf=%d nClusters=%d%n",
                              it, (int) t.getCharge(), pmag, tanL, t.getChi2(), t.getNDF(),
                              t.getTrackerHits().size());
            int nTruthed = 0, nHits = 0;
            int[] byCat = new int[6];
            for(TrackerHit th : t.getTrackerHits()) {
                for(Object o : th.getRawHits()) {
                    if(!(o instanceof RawTrackerHit)) { continue; }
                    RawTrackerHit r = (RawTrackerHit) o;
                    nHits++;
                    int cat = categorise(r, pulser, mcContrib, truthed);
                    byCat[cat]++;
                    StringBuilder who = new StringBuilder();
                    if(rawToSim.containsKey(r)) {
                        nTruthed++;
                        for(SimTrackerHit s : rawToSim.get(r)) {
                            MCParticle p = s.getMCParticle();
                            who.append(" <- pdg=").append(p == null ? "null" : p.getPDGID())
                               .append(" p=").append(p == null ? "?" : String.format("%.3f", momentum(p)));
                        }
                    }
                    System.out.printf("      %-28s ch=%4d  %-17s%s%n",
                                      sensorOf(r), channelOf(r), CAT_NAME[cat], who);
                }
            }
            System.out.printf("      -> %d raw hits, %d truthed;", nHits, nTruthed);
            for(int c = 0; c < 6; c++) { if(byCat[c] > 0) { System.out.print(" " + CAT_NAME[c] + "=" + byCat[c]); } }
            System.out.println();
        }

        // ---- vertices, with the provenance of the tracks they actually reference ----
        // Identifying a vertex's track by matching momenta against the track collection
        // is unreliable: the momentum a vertex reports is the fitted one, and hpstr reads
        // a different track state again. Walk the references instead.
        for(String vtxColl : vertexCollectionNames) {
            if(!event.hasCollection(Vertex.class, vtxColl)) { continue; }
            List<Vertex> vertices = event.get(Vertex.class, vtxColl);
            System.out.println("  -- " + vtxColl + ": " + vertices.size() + " --");
            int iv = 0;
            for(Vertex v : vertices) {
                System.out.printf("   vertex %d: chi2=%.2f  pos=(%.3f, %.3f, %.3f)  invM=%.4f%n",
                                  iv++, v.getChi2(), v.getPosition().x(), v.getPosition().y(),
                                  v.getPosition().z(),
                                  v.getAssociatedParticle() == null ? Double.NaN
                                      : v.getAssociatedParticle().getMass());
                if(v.getAssociatedParticle() == null) { continue; }
                for(ReconstructedParticle rp : v.getAssociatedParticle().getParticles()) {
                    for(Track t : rp.getTracks()) {
                        int nHits = 0, nTruthed = 0;
                        int[] byCat = new int[6];
                        Set<MCParticle> carried = new HashSet<MCParticle>();
                        for(TrackerHit th : t.getTrackerHits()) {
                            for(Object o : th.getRawHits()) {
                                if(!(o instanceof RawTrackerHit)) { continue; }
                                RawTrackerHit r = (RawTrackerHit) o;
                                nHits++;
                                byCat[categorise(r, pulser, mcContrib, truthed)]++;
                                if(rawToSim.containsKey(r)) {
                                    nTruthed++;
                                    for(SimTrackerHit s : rawToSim.get(r)) {
                                        if(s.getMCParticle() != null) { carried.add(s.getMCParticle()); }
                                    }
                                }
                            }
                        }
                        TrackState ts = t.getTrackStates().isEmpty() ? null : t.getTrackStates().get(0);
                        double[] m = ts != null ? ts.getMomentum() : new double[]{0, 0, 0};
                        Integer which = null;
                        for(int it = 0; it < tracks.size(); it++) {
                            if(tracks.get(it) == t) { which = it; }
                        }
                        System.out.printf("      rp charge=%+d rp_p=%.4f | track#%s p=%.4f tanL=%+.5f "
                                          + "chi2=%.2f nRawHits=%d nTruthed=%d",
                                          (int) rp.getCharge(), rp.getMomentum().magnitude(),
                                          which == null ? "NOT-IN-" + trackCollectionName : which.toString(),
                                          Math.sqrt(m[0]*m[0] + m[1]*m[1] + m[2]*m[2]),
                                          ts != null ? ts.getTanLambda() : Double.NaN,
                                          t.getChi2(), nHits, nTruthed);
                        for(int c = 0; c < 6; c++) {
                            if(byCat[c] > 0) { System.out.print(" " + CAT_NAME[c] + "=" + byCat[c]); }
                        }
                        for(MCParticle p : carried) {
                            System.out.printf(" <- pdg=%+d p=%.3f", p.getPDGID(), momentum(p));
                        }
                        System.out.println();
                    }
                }
            }
        }

        // ---- the decisive part: what became of each MC hit ----
        System.out.println("  -- fate of every SimTrackerHit of each e+- --");
        List<MCParticle> ordered = new ArrayList<MCParticle>(simByParticle.keySet());
        Collections.sort(ordered, (a, b) -> Double.compare(momentum(b), momentum(a)));
        for(MCParticle p : ordered) {
            if(Math.abs(p.getPDGID()) != 11 || momentum(p) < mcpMinMomentum) { continue; }
            List<SimTrackerHit> sims = simByParticle.get(p);
            System.out.printf("   particle pdg=%+d p=%.4f tanL=%+.5f : %d SimTrackerHits%n",
                              p.getPDGID(), momentum(p), tanLambda(p), sims.size());
            int lost = 0, onTrack = 0, orphan = 0, noCluster = 0;
            for(SimTrackerHit s : sims) {
                List<RawTrackerHit> raws = simToRaw.get(s);
                if(raws == null || raws.isEmpty()) {
                    lost++;
                    System.out.printf("      %-28s  NO RAW HIT CARRIES THIS -- truth dropped in digitization%n",
                                      s.getDetectorElement().getName());
                    continue;
                }
                for(RawTrackerHit r : raws) {
                    Integer trk = hitToTrack.get(r);
                    if(trk == null) { orphan++; } else { onTrack++; }
                    TrackerHit c = rawToCluster.get(r);
                    String clus;
                    if(c == null) {
                        clus = "NOT IN ANY CLUSTER";
                        noCluster++;
                    } else {
                        boolean used = false;
                        for(Object o : c.getRawHits()) {
                            if(o instanceof RawTrackerHit && hitToTrack.containsKey(o)) { used = true; }
                        }
                        clus = String.format("cluster size=%d t=%.2f %s",
                                             c.getRawHits().size(), c.getTime(),
                                             used ? "USED by a track" : "used by NO track");
                    }
                    System.out.printf("      %-28s ch=%4d  %-17s  %-16s  %s%n",
                                      sensorOf(r), channelOf(r),
                                      CAT_NAME[categorise(r, pulser, mcContrib, truthed)],
                                      trk == null ? "NOT on any track" : ("on track " + trk),
                                      clus);
                    // What else is on this sensor nearby? A pulser hit on an adjacent
                    // channel merges into the same cluster and can move or spoil it.
                    List<RawTrackerHit> near = bySensor.get(sensorOf(r));
                    if(near != null) {
                        StringBuilder nb = new StringBuilder();
                        for(RawTrackerHit o : near) {
                            int d = Math.abs(channelOf(o) - channelOf(r));
                            if(o != r && d <= neighbourWindow) {
                                nb.append(String.format(" ch=%d(%s,d=%d)", channelOf(o),
                                          CAT_NAME[categorise(o, pulser, mcContrib, truthed)], d));
                            }
                        }
                        if(nb.length() > 0) { System.out.println("           neighbours:" + nb); }
                    }
                }
            }
            System.out.printf("      -> %d SimTrackerHits with no raw hit (case A), "
                              + "%d raw hits on a track, %d truthed raw hits used by no track (case B), "
                              + "%d of those in NO cluster at all (case C)%n",
                              lost, onTrack, orphan, noCluster);
        }
        System.out.println("################ end event " + event.getEventNumber() + " ################");
    }
}
