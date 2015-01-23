package org.hps.analysis.examples;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.FindableTrack;
import org.hps.recon.tracking.StraightLineTrack;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.fit.helicaltrack.TrackDirection;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;

/**

 @author mgraham
 */
public class FastTrackAnalysisDriver extends Driver {

    String[] detNames = {"Tracker"};
    Integer _minLayers = 8;
    Integer[] nlayers = {8};
    int nevt = 0;
    double xref = 50.0; //mm
    public String outputTextName = "myevents.txt";
    FileWriter fw;
    PrintWriter pw;
    double[] beamsize = {0.001, 0.02, 0.02};
    String _config = "3pt4";
// flipSign is a kludge...
//  HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
//  so we set the B-field in +iveZ and flip signs of fitted tracks
//  note:  this should be -1 for Test configurations and +1 for Full (v3.X and lower) configurations
//  this is set by the _config variable (detType in HeavyPhotonDriver)
    int flipSign = 1;

    public FastTrackAnalysisDriver(int trackerLayers, int mintrkLayers, String config) {
        nlayers[0] = trackerLayers;
        _minLayers = mintrkLayers;
        _config = config;
        if (_config.contains("Test"))
            flipSign = -1;
    }

    public void process(
            EventHeader event) {
        if (nevt == 0)
            try {
//open things up
                fw = new FileWriter(outputTextName);
                pw = new PrintWriter(fw);
            } catch (IOException ex) {
                Logger.getLogger(FastTrackAnalysisDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
        //  Increment the event counter
        nevt++;



        Hep3Vector IP = new BasicHep3Vector(0., 0., 0.);
        double bfield = event.getDetector().getFieldMap().getField(IP).z();

        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");
        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");

        List<HelicalTrackHit> toththits = event.get(HelicalTrackHit.class, "HelicalTrackHits");
        List<HelicalTrackHit> axialhits = event.get(HelicalTrackHit.class, "AxialTrackHits");

        int nAxialHitsTotal = axialhits.size();
        int nL1Hits = 0;
        for (HelicalTrackHit hth : axialhits)
            if (hth.Layer() == 1)
                nL1Hits++;


        //  Create a relational table that maps TrackerHits to MCParticles
        RelationalTable hittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> mcrelations = event.get(LCRelation.class, "HelicalTrackMCRelations");

        for (LCRelation relation : mcrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittomc.add(relation.getFrom(), relation.getTo());

        RelationalTable hittomcAxial = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
//        List<LCRelation> mcrelations = event.get(LCRelation.class, "HelicalTrackMCRelations");
        List<LCRelation> mcrelationsAxial = event.get(LCRelation.class, "AxialTrackMCRelations");
        for (LCRelation relation : mcrelationsAxial)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittomcAxial.add(relation.getFrom(), relation.getTo());

        //  Instantiate the class that determines if a track is "findable"
        FindableTrack findable = new FindableTrack(event);

        //  Create a map between tracks and the associated MCParticle
        List<Track> tracklist = event.get(Track.class, "MatchedTracks");
//        List<Track> lltracklist = event.get(Track.class, "LLTracks");
//        List<Track> axialtracklist = event.get(Track.class, "AxialTracks");
//        tracklist.addAll(lltracklist);

        RelationalTable trktomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);

        RelationalTable mcHittomcP = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);

        //  Get the collections of SimTrackerHits
        List<List<SimTrackerHit>> simcols = event.get(SimTrackerHit.class);

        //  Loop over the SimTrackerHits and fill in the relational table
        for (List<SimTrackerHit> simlist : simcols)
            for (SimTrackerHit simhit : simlist)
                if (simhit.getMCParticle() != null)
                    mcHittomcP.add(simhit, simhit.getMCParticle());

        Map<Track, TrackAnalysis> tkanalMap = new HashMap<Track, TrackAnalysis>();
        Map<Track, BilliorTrack> btMap = new HashMap<Track, BilliorTrack>();
        RelationalTable nearestHitToTrack = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        Map<Track, Double> l1Isolation = new HashMap<Track, Double>();
        Map<Track, Double> l1DeltaZ = new HashMap<Track, Double>();
        Map<Track, Double> l123KinkAngle = new HashMap<Track, Double>();
        int _neleRec = 0;
        int _nposRec = 0;
        //  Analyze the tracks in the event
        for (Track track : tracklist) {
            if (track.getCharge() < 0)
                _neleRec++;
            if (track.getCharge() > 0)
                _nposRec++;
            //extrapolate straight back...
            SeedTrack stEle = (SeedTrack) track;
            SeedCandidate seedEle = stEle.getSeedCandidate();
            HelicalTrackFit ht = seedEle.getHelix();

            BilliorTrack bt = new BilliorTrack(ht);

            TrackAnalysis tkanal = new TrackAnalysis(track, hittomc);

            tkanalMap.put(track, tkanal);
            btMap.put(track, bt);


            //  Now analyze MC Particles on this track
            MCParticle mcp = tkanal.getMCParticle();
            if (mcp != null)
                //  Create a map between the tracks found and the assigned MC particle
                trktomc.add(track, tkanal.getMCParticle());
            BasicHep3Vector axial = new BasicHep3Vector();
            axial.setV(0, 1, 0);
            List<TrackerHit> hitsOnTrack = track.getTrackerHits();
            double[] zlist = {0, 0, 0};
            for (TrackerHit hit : hitsOnTrack) {
                HelicalTrackHit htc = (HelicalTrackHit) hit;
                List<MCParticle> mcpsHTH = htc.getMCParticles();
                double sHit = ht.PathMap().get(htc);
                Hep3Vector posonhelix = HelixUtils.PointOnHelix(ht, sHit);
                double yTr = posonhelix.y();
                double zTr = posonhelix.z();
                HelicalTrackCross cross = (HelicalTrackCross) htc;
                List<HelicalTrackStrip> clusterlist = cross.getStrips();
                TrackDirection trkdir = HelixUtils.CalculateTrackDirection(ht, sHit);
                cross.setTrackDirection(trkdir, ht.covariance());
                double y = cross.y();
                double z = cross.z();
                double yerr = Math.sqrt(cross.getCorrectedCovMatrix().e(1, 1));
                double zerr = Math.sqrt(cross.getCorrectedCovMatrix().e(2, 2));

                int htlayer = htc.Layer();
                if (htlayer == 1)
                    zlist[0] = z;
                if (htlayer == 3)
                    zlist[1] = z;
                if (htlayer == 5)
                    zlist[2] = z;
                if (htlayer == 1)
                    l1DeltaZ.put(track, z - zTr);
                for (HelicalTrackStrip cl : clusterlist) {
                    int layer = cl.layer();
                    HelicalTrackStrip nearest = getNearestHit(cl, toththits);
                    if (layer == 1) {
                        Double l1Dist = getNearestDistance(cl, toththits);
                        if (l1Dist != null)
                            l1Isolation.put(track, l1Dist);
                    }
                    if (nearest != null)
                        nearestHitToTrack.add(track, nearest);
                }
            }
            double layerDist = 10;//cm
            double theta12 = Math.atan2(zlist[1] - zlist[0], layerDist);
            double theta13 = Math.atan2(zlist[2] - zlist[0], 2 * layerDist);
            double theta23 = Math.atan2(zlist[2] - zlist[1], layerDist);
            l123KinkAngle.put(track, theta23 - theta12);
        }




        //analyze the event
        int ApCand = 0;
        Track eleID = null;
        Track posID = null;
        MCParticle eleMC = null;
        MCParticle posMC = null;
        for (Track track : tracklist) {

            TrackAnalysis tkanal = tkanalMap.get(track);
            BilliorTrack bt = btMap.get(track);
            //  Calculate purity and make appropriate plots
            MCParticle mcp = tkanal.getMCParticle();
            if (mcp == null)
                continue;
            if (mcp.getParents().size() == 1 && mcp.getParents().get(0).getPDGID() == 622) {
                int nhits = tkanal.getNHitsNew();
                double px = track.getTrackStates().get(0).getMomentum()[0];
                double py = track.getTrackStates().get(0).getMomentum()[1];
                double pz = track.getTrackStates().get(0).getMomentum()[2];
                double pt = Math.sqrt(px * px + py * py);
                double pperp = Math.sqrt(py * py + pz * pz);
                double p = Math.sqrt(pt * pt + pz * pz);
                double phi = Math.atan2(py, px);
                double cth = pz / Math.sqrt(pt * pt + pz * pz);

//                double doca = slt.Doca();
//                double[] poca = slt.Poca();

                if (mcp.getCharge() > 0) {
                    posID = track;
                    posMC = mcp;

                } else {
                    eleID = track;
                    eleMC = mcp;
                }
            }

        }
        for (Track track1 : tracklist) {
            Track ele = null;
            Track pos = null;
            int ch1 = track1.getCharge() * flipSign;
            int index = tracklist.indexOf(track1);
            List<Track> subtracklist = tracklist.subList(index, tracklist.size());
            for (Track track2 : subtracklist) {
                int ch2 = track2.getCharge() * flipSign;
                if (track1 != track2 && ch1 == -ch2) {
                    ele = track1;
                    pos = track2;
//                    System.out.println("Found two oppositely charged tracks!  Lets look at them");
                    if (ch1 > 0) {
                        ele = track2;
                        pos = track1;
                    }
                    ApCand++;
                    // int nElectron = ele.getTrackerHits().size();
                    // int nPositron = pos.getTrackerHits().size();

                    SeedTrack stEle = (SeedTrack) ele;
                    SeedCandidate seedEle = stEle.getSeedCandidate();
                    HelicalTrackFit htEle = seedEle.getHelix();

                    SeedTrack stPos = (SeedTrack) pos;
                    SeedCandidate seedPos = stPos.getSeedCandidate();
                    HelicalTrackFit htPos = seedPos.getHelix();

                    double d0E = htEle.dca();
                    double z0E = htEle.z0();
                    double phi0E = htEle.phi0();
                    double RE = htEle.R();
                    double slopeE = htEle.slope();
                    double d0P = htPos.dca();
                    double z0P = htPos.z0();
                    double phi0P = htPos.phi0();
                    double RP = htPos.R();
                    double slopeP = htPos.slope();

                    double pxE = ele.getTrackStates().get(0).getMomentum()[0];
                    double pyE = ele.getTrackStates().get(0).getMomentum()[1];
                    double pzE = ele.getTrackStates().get(0).getMomentum()[2];
                    double pxP = pos.getTrackStates().get(0).getMomentum()[0];
                    double pyP = pos.getTrackStates().get(0).getMomentum()[1];
                    double pzP = pos.getTrackStates().get(0).getMomentum()[2];

                    List<BilliorTrack> btlist = new ArrayList<BilliorTrack>();

                    BilliorTrack btEle = btMap.get(ele);
                    BilliorTrack btPos = btMap.get(pos);

                    btlist.add(btEle);
                    btlist.add(btPos);



                    BilliorVertexer vtxfitter = new BilliorVertexer(bfield);
                    vtxfitter.doBeamSpotConstraint(false);
                    vtxfitter.setBeamSize(beamsize);

                    BilliorVertexer vtxfitterCon = new BilliorVertexer(bfield);
                    vtxfitterCon.doBeamSpotConstraint(true);
                    vtxfitterCon.setBeamSize(beamsize);

                    BilliorVertexer vtxfitterBSCon = new BilliorVertexer(bfield);
                    vtxfitterBSCon.doTargetConstraint(true);
                    vtxfitterBSCon.setBeamSize(beamsize);


                    BilliorVertex vtxfit = vtxfitter.fitVertex(btlist);
                    Map<String, Double> vtxMap = vtxfit.getParameters();

                    double vtxpxE = vtxMap.get("p1X");
                    double vtxpyE = vtxMap.get("p1Y");
                    double vtxpzE = vtxMap.get("p1Z");
                    double vtxpxP = vtxMap.get("p2X");
                    double vtxpyP = vtxMap.get("p2Y");
                    double vtxpzP = vtxMap.get("p2Z");
                    double chisq = vtxfit.getChi2();
                    BasicMatrix vtx = (BasicMatrix) vtxfit.getPosition();
                    SymmetricMatrix vtxcov = vtxfit.getCovMatrix();
                    double chisqE = ele.getChi2();
                    double chisqP = pos.getChi2();

                    BilliorVertex confit = vtxfitterCon.fitVertex(btlist);
                    Map<String, Double> conMap = confit.getParameters();
                    double conpxE = conMap.get("p1X");
                    double conpyE = conMap.get("p1Y");
                    double conpzE = conMap.get("p1Z");
                    double conpxP = conMap.get("p2X");
                    double conpyP = conMap.get("p2Y");
                    double conpzP = conMap.get("p2Z");
                    double conchisq = confit.getChi2();
                    BasicMatrix conVtx = (BasicMatrix) confit.getPosition();
                    SymmetricMatrix conVtxCov = confit.getCovMatrix();

                    BilliorVertex bsconfit = vtxfitterBSCon.fitVertex(btlist);
                    Map<String, Double> bsconMap = bsconfit.getParameters();
                    double bsconpxE = bsconMap.get("p1X");
                    double bsconpyE = bsconMap.get("p1Y");
                    double bsconpzE = bsconMap.get("p1Z");
                    double bsconpxP = bsconMap.get("p2X");
                    double bsconpyP = bsconMap.get("p2Y");
                    double bsconpzP = bsconMap.get("p2Z");
                    double bsconchisq = bsconfit.getChi2();
                    BasicMatrix bsconVtx = (BasicMatrix) bsconfit.getPosition();
                    SymmetricMatrix bsconVtxCov =  bsconfit.getCovMatrix();


                    double l1minE = -99;
                    double l1minP = -99;
                    if (l1Isolation.get(ele) != null)
                        l1minE = l1Isolation.get(ele);
                    if (l1Isolation.get(pos) != null)
                        l1minP = l1Isolation.get(pos);

                    TrackAnalysis tkanalEle = tkanalMap.get(ele);
                    TrackAnalysis tkanalPos = tkanalMap.get(pos);
                    Integer nElectron = tkanalEle.getNHitsNew();
                    Integer nPositron = tkanalPos.getNHitsNew();
                    List<Integer> badhitsEle = tkanalEle.getBadHitList();
                    List<Integer> badhitsPos = tkanalPos.getBadHitList();
                    Integer badLayerEle = encodeBadHitList(badhitsEle);
                    Integer badLayerPos = encodeBadHitList(badhitsPos);
//                        int nMCL1Ele = tkanalEle.getNumberOfMCParticles(1);
//                       int nMCL1Pos = tkanalPos.getNumberOfMCParticles(1);
                    List<Integer> sharedhitsEle = tkanalEle.getSharedHitList();
                    List<Integer> sharedhitsPos = tkanalPos.getSharedHitList();
                    Integer sharedLayerEle = encodeSharedHitList(sharedhitsEle);
                    Integer sharedLayerPos = encodeSharedHitList(sharedhitsPos);
                    int nStripsL1Ele = -99;
                    int nStripsL1Pos = -99;
                    Integer l1 = 1;
                    double zvalEle = -99;
                    double zvalPos = -99;
                    if (tkanalEle.hasLayerOne()) {
                        zvalEle = tkanalEle.getClusterPosition(l1).z();
                        nStripsL1Ele = tkanalEle.getNumberOfStripHits(1);
                    }
                    if (tkanalPos.hasLayerOne()) {
                        zvalPos = tkanalPos.getClusterPosition(l1).z();
                        nStripsL1Pos = tkanalPos.getNumberOfStripHits(1);
                    }

                    int eleFromAp = 0;
                    int posFromAp = 0;
                    if (eleMC != null && ele == eleID)
                        eleFromAp = 1;
                    if (posMC != null && pos == posID)
                        posFromAp = 1;
                    MCParticle mcEle = tkanalEle.getMCParticle();
                    MCParticle mcPos = tkanalPos.getMCParticle();
                    double[] pmcEle = {-99, -99, -99};
                    double[] pmcPos = {-99, -99, -99};
                    double[] pocamcE = {-99, -99, -99};
                    double[] pocamcP = {-99, -99, -99};
                    if (mcEle != null && mcPos != null) {
                        pmcEle[0] = mcEle.getPX();
                        pmcEle[1] = mcEle.getPY();
                        pmcEle[2] = mcEle.getPZ();
                        pmcPos[0] = mcPos.getPX();
                        pmcPos[1] = mcPos.getPY();
                        pmcPos[2] = mcPos.getPZ();
                    }
                    double[] ApVertexMC = {-99, -99, -99};
                    if (eleFromAp == 1 && posFromAp == 1) {
                        ApVertexMC[0] = eleMC.getOriginX();
                        ApVertexMC[1] = eleMC.getOriginY();
                        ApVertexMC[2] = eleMC.getOriginZ();
                    }


                    //print out the vertex;
                    double l1dzE = -99;
                    double l1dzP = -99;
                    if (l1DeltaZ.get(ele) != null)
                        l1dzE = l1DeltaZ.get(ele);
                    if (l1DeltaZ.get(pos) != null)
                        l1dzP = l1DeltaZ.get(pos);
                    double l123KinkE = -99;
                    double l123KinkP = -99;
                    if (l123KinkAngle.get(ele) != null)
                        l123KinkE = l123KinkAngle.get(ele);
                    if (l123KinkAngle.get(pos) != null)
                        l123KinkP = l123KinkAngle.get(pos);
                    if (!tkanalEle.hasLayerOne())
                        nElectron = -nElectron;
                    if (!tkanalPos.hasLayerOne())
                        nPositron = -nPositron;
                    pw.format("%d %5.5f %5.5f %5.5f ", nevt, pxE, pyE, pzE);
                    pw.format("%5.5f  %5.5f %5.5f %5.5f  %5.5f ", d0E, z0E, slopeE, phi0E, RE);
                    pw.format("%5.5f  %5.5f %5.5f %5.5f ", chisqE, l1minE, l1dzE, l123KinkE);
                    pw.format("%d %d  %5.5f ", sharedLayerEle, nStripsL1Ele, zvalEle);
                    pw.format("%d %d %d ", nElectron, badLayerEle, eleFromAp);
                    pw.format("%5.5f %5.5f %5.5f ", pxP, pyP, pzP);
                    pw.format("%5.5f  %5.5f %5.5f %5.5f  %5.5f ", d0P, z0P, slopeP, phi0P, RP);
                    pw.format("%5.5f  %5.5f %5.5f %5.5f ", chisqP, l1minP, l1dzP, l123KinkP);
                    pw.format("%d %d  %5.5f ", sharedLayerPos, nStripsL1Pos, zvalPos);
                    pw.format("%d %d %d ", nPositron, badLayerPos, posFromAp);
                    pw.format("%5.5f %5.5f %5.5f ", vtxpxE, vtxpyE, vtxpzE);
                    pw.format("%5.5f %5.5f %5.5f ", vtxpxP, vtxpyP, vtxpzP);
                    pw.format("%5.5f %5.5f %5.5f %5.5f %5.5f %5.5f ", vtx.e(0, 0), Math.sqrt(vtxcov.e(0, 0)), vtx.e(1, 0), Math.sqrt(vtxcov.e(1, 1)), vtx.e(2, 0), Math.sqrt(vtxcov.e(2, 2)));
                    pw.format("%5.5f ", chisq);
                    pw.format("%5.5f %5.5f %5.5f ", conpxE, conpyE, conpzE);
                    pw.format("%5.5f %5.5f %5.5f ", conpxP, conpyP, conpzP);

                    //   get the errors on the constrained vertex an make sure they aren't NaN
                    //   there must be something wrong in the constraint...hopefully just the error calc
                    double conErrX = getErr(conVtxCov.e(0, 0));
                    double conErrY = getErr(conVtxCov.e(1, 1));
                    double conErrZ = getErr(conVtxCov.e(2, 2));

                    pw.format("%5.5f %5.5f %5.5f %5.5f %5.5f %5.5f ", conVtx.e(0, 0), conErrX, conVtx.e(1, 0), conErrY, conVtx.e(2, 0), conErrZ);
                    pw.format("%5.5f ", conchisq);               
                    pw.format("%5.5f %5.5f %5.5f ", bsconpxE, bsconpyE, bsconpzE);
                    pw.format("%5.5f %5.5f %5.5f ", bsconpxP, bsconpyP, bsconpzP);

                    //   get the errors on the bsconstrained vertex an make sure they aren't NaN
                    //   there must be somethihhhhfang wrong in the bsconstraint...hopefully just the error calc
                    double bsconErrX = getErr(bsconVtxCov.e(0, 0));
                    double bsconErrY = getErr(bsconVtxCov.e(1, 1));
                    double bsconErrZ = getErr(bsconVtxCov.e(2, 2));

                    pw.format("%5.5f %5.5f %5.5f %5.5f %5.5f %5.5f ", bsconVtx.e(0, 0), bsconErrX, bsconVtx.e(1, 0), bsconErrY, bsconVtx.e(2, 0), bsconErrZ);
                    pw.format("%5.5f ", bsconchisq);


//  print out MC information
                    pw.format("%5.5f %5.5f %5.5f ", pmcEle[0], pmcEle[1], pmcEle[2]);
                    pw.format("%5.5f %5.5f %5.5f ", pocamcE[0], pocamcE[1], pocamcE[2]);
                    pw.format("%5.5f %5.5f %5.5f ", pmcPos[0], pmcPos[1], pmcPos[2]);
                    pw.format("%5.5f %5.5f %5.5f ", pocamcP[0], pocamcP[1], pocamcP[2]);
                    pw.format("%5.5f %5.5f %5.5f ", ApVertexMC[0], ApVertexMC[1], ApVertexMC[2]);
//  print out some event information
                    pw.format("%d %d %d %d", _neleRec, _nposRec, nAxialHitsTotal, nL1Hits);
                    pw.println();

                }
            }
        }

//ok, fill MC Aprime info
        List<MCParticle> mclist = event.getMCParticles();
        MCParticle mcEle = null;
        MCParticle mcPos = null;
        MCParticle mcApr = null;
        for (MCParticle mcp : mclist) {
//            System.out.print("PDG ID = "+mcp.getPDGID());
            if (mcp.getPDGID() == 622)
                mcApr = mcp;
            if (mcp.getParents().size() == 1 && mcp.getParents().get(0).getPDGID() == 622) {
                if (mcp.getPDGID() == -11)
                    mcPos = mcp;
                if (mcp.getPDGID() == 11)
                    mcEle = mcp;
            }
        }
//should probably check that each MC particles are found, but they should be...
        double pxMCE = -99;
        double pyMCE = -99;
        double pzMCE = -99;
        double pxMCP = -99;
        double pyMCP = -99;
        double pzMCP = -99;
        int findableE = 0;
        int findableP = 0;
        int foundE = 0;
        int foundP = 0;
        pxMCE = mcEle.getPX();
        pyMCE = mcEle.getPY();
        pzMCE = mcEle.getPZ();
        if (findable.InnerTrackerIsFindable(mcEle, _minLayers))
            findableE = 1;
        Set<Track> trklistE = trktomc.allTo(mcEle);
        foundE = trklistE.size();//can be greater than 1 if more than 1 track shares hits

        pxMCP = mcPos.getPX();
        pyMCP = mcPos.getPY();
        pzMCP = mcPos.getPZ();
        if (findable.InnerTrackerIsFindable(mcPos, _minLayers))
            findableP = 1;
        Set<Track> trklistP = trktomc.allTo(mcPos);
        foundP = trklistP.size();//can be greater than 1 if more than 1 track shares hits

        double pxMCA = mcApr.getPX();
        double pyMCA = mcApr.getPY();
        double pzMCA = mcApr.getPZ();
        double mMCA = mcApr.getMass();
        Hep3Vector decayMCA = mcApr.getEndPoint();

        pw.format(
                "%d %5.5f %5.5f %5.5f %d %d ", -666, pxMCE, pyMCE, pzMCE, findableE, foundE);
        pw.format(
                "%5.5f %5.5f %5.5f %d %d ", pxMCP, pyMCP, pzMCP, findableP, foundP);
        pw.format(
                "%5.5f %5.5f %5.5f %5.5f %5.5f %5.5f %5.5f", pxMCA, pyMCA, pzMCA, mMCA, decayMCA.x(), decayMCA.y(), decayMCA.z());
        pw.println();




        return;

    }

    public void endOfData() {

        pw.close();
        try {
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(FastTrackAnalysisDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setOutputPlots(String output) {
    }

    public void setOutputText(String output) {
        this.outputTextName = output;
    }

    private double getInvMass(Track track1, StraightLineTrack slt1, Track track2, StraightLineTrack slt2) {
        double esum = 0.;
        double pxsum = 0.;
        double pysum = 0.;
        double pzsum = 0.;
        double chargesum = 0.;
        double me = 0.000511;
        // Loop over jets

        double p1x = track1.getTrackStates().get(0).getMomentum()[0];
        double p1y = track1.getTrackStates().get(0).getMomentum()[1];
        double p1z = track1.getTrackStates().get(0).getMomentum()[2];
        double p1mag2 = p1x * p1x + p1y * p1y + p1z * p1z;
        double e1 = Math.sqrt(p1mag2 + me * me);
        double dydx1 = slt1.dydx();
        double dzdx1 = slt1.dzdx();
        double s1sq = 1 + 1 / (dydx1 * dydx1) + (dzdx1 * dzdx1) / (dydx1 * dydx1);
        double truep1y = Math.sqrt(p1mag2 / s1sq);
        if (dydx1 < 0)
            truep1y = -truep1y;
        double truep1x = truep1y / dydx1;
        double truep1z = dzdx1 * truep1x;

        double p2x = track2.getTrackStates().get(0).getMomentum()[0];
        double p2y = track2.getTrackStates().get(0).getMomentum()[1];
        double p2z = track2.getTrackStates().get(0).getMomentum()[2];
        double p2mag2 = p2x * p2x + p2y * p2y + p2z * p2z;
        double e2 = Math.sqrt(p2mag2 + me * me);

        double dydx2 = slt2.dydx();
        double dzdx2 = slt2.dzdx();
        double s2sq = 1 + 1 / (dydx2 * dydx2) + (dzdx2 * dzdx2) / (dydx2 * dydx2);
        double truep2y = Math.sqrt(p2mag2 / s2sq);
        if (dydx2 < 0)
            truep2y = -truep2y;
        double truep2x = truep2y / dydx2;
        double truep2z = dzdx2 * truep2x;

        pxsum =
                truep1x + truep2x;
        pysum =
                truep1y + truep2y;
        pzsum =
                truep1z + truep2z;

        esum =
                e1 + e2;
//        double p1dotp2 = p1x * p2x + p1y * p2y + p1z * p2z;
        double p1dotp2 = truep1x * truep2x + truep1y * truep2y + truep1z * truep2z;
        double e1e2 = e1 * e2;
        double invmass = Math.sqrt(2 * me * me + 2 * (e1e2 - p1dotp2));
        // Compute total momentum and hence event mass
        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = Math.sqrt(esum * esum - psum * psum);
//        System.out.println("invmass= " + invmass + "; evtmass=" + evtmass);
        return invmass;
    }

//find the DOCA to the beamline extrpolating linearly from the reference point
    private double findDoca(double y, double z, double px, double py, double pz) {
        double xoca = 0;
        double sy = py / px;
        double sz = pz / px;
        xoca =
                -(y * sy + z * sz) / (sy * sy + sz + sz);
        double doca = Math.sqrt(Math.pow(y + sy * xoca, 2) + Math.pow(z + sz * xoca, 2));
        return doca;
    }

//find the XOCA to the beamline extrpolating linearly from the reference point
    private double findXoca(double y, double z, double px, double py, double pz) {
        double xoca = 0;
        double sy = py / px;
        double sz = pz / px;
        xoca =
                -(y * sy + z * sz) / (sy * sy + sz + sz);
        return xoca;
    }

    private double[] findPoca(double y, double z, double px, double py, double pz) {
        double poca[] = {0, 0, 0};
        double sy = py / px;
        double sz = pz / px;
        poca[0] = -(y * sy + z * sz) / (sy * sy + sz * sz);
        poca[1] = y + sy * poca[0];
        poca[2] = z + sz * poca[0];
        return poca;
    }

    private Hep3Vector getV0Momentum(Track track1, StraightLineTrack slt1, Track track2, StraightLineTrack slt2) {


        Hep3Vector p1 = getTrueMomentum(track1, slt1);
        Hep3Vector p2 = getTrueMomentum(track2, slt2);
        Hep3Vector pV0 = VecOp.add(p1, p2);

        return pV0;

    }

    private double getV0OpeningAngle(Track track1, StraightLineTrack slt1, Track track2, StraightLineTrack slt2) {

        Hep3Vector p1 = getTrueMomentum(track1, slt1);
        Hep3Vector p2 = getTrueMomentum(track2, slt2);

        return VecOp.dot(p1, p2) / (p1.magnitude() * p2.magnitude());

    }

    private Hep3Vector getTrueMomentum(Track track1, StraightLineTrack slt1) {
        double[] truep = {0, 0, 0};
        double me = 0.000511;
        double p1x = track1.getTrackStates().get(0).getMomentum()[0];
        double p1y = track1.getTrackStates().get(0).getMomentum()[1];
        double p1z = track1.getTrackStates().get(0).getMomentum()[2];
        double p1mag2 = p1x * p1x + p1y * p1y + p1z * p1z;
        double e1 = Math.sqrt(p1mag2 + me * me);
        double dydx1 = slt1.dydx();
        double dzdx1 = slt1.dzdx();
        double s1sq = 1 + 1 / (dydx1 * dydx1) + (dzdx1 * dzdx1) / (dydx1 * dydx1);
        truep[1] = Math.sqrt(p1mag2 / s1sq);
        if (dydx1 < 0)
            truep[1] = -truep[1];
        truep[0] = truep[1] / dydx1;
        truep[2] = dzdx1 * truep[0];
        return new BasicHep3Vector(truep[0], truep[1], truep[2]);
    }

    private double getCosAlpha(Hep3Vector vertex, Hep3Vector pV0) {
        return VecOp.dot(vertex, pV0) / (vertex.magnitude() * pV0.magnitude());
    }

    private double getMag(double[] vec) {
        return Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
    }

    private HelicalTrackStrip getNearestHit(HelicalTrackStrip cl, List<HelicalTrackHit> toththits) {
        Hep3Vector corigin = cl.origin();
        Hep3Vector u = cl.u();
        double umeas = cl.umeas();
        Hep3Vector uvec = VecOp.mult(umeas, u);
        Hep3Vector clvec = VecOp.add(corigin, uvec);
        int layer = cl.layer();
        HelicalTrackStrip nearest = null;
        double mindist = 99999999;
        for (HelicalTrackHit hth : toththits) {
            HelicalTrackCross cross = (HelicalTrackCross) hth;
            for (HelicalTrackStrip str : cross.getStrips())
                if (str != cl) {
                    Hep3Vector strorigin = str.origin();
                    Hep3Vector stru = str.u();
                    double strumeas = str.umeas();
                    Hep3Vector struvec = VecOp.mult(strumeas, stru);
                    Hep3Vector strvec = VecOp.add(strorigin, struvec);
                    int strlayer = cl.layer();
                    if (layer == strlayer && VecOp.sub(clvec, strvec).magnitude() < mindist) {
                        mindist = VecOp.sub(clvec, strvec).magnitude();
                        nearest =
                                str;
                    }

                }
        }

        return nearest;
    }

    private Double getNearestDistance(HelicalTrackStrip cl, List<HelicalTrackHit> toththits) {
        Hep3Vector corigin = cl.origin();
        Hep3Vector u = cl.u();
        double umeas = cl.umeas();
        Hep3Vector uvec = VecOp.mult(umeas, u);
        Hep3Vector clvec = VecOp.add(corigin, uvec);
        int layer = cl.layer();
        HelicalTrackStrip nearest = null;
        Double mindist = 99999999.0;
        for (HelicalTrackHit hth : toththits) {
            HelicalTrackCross cross = (HelicalTrackCross) hth;
            for (HelicalTrackStrip str : cross.getStrips())
                if (str != cl) {
                    Hep3Vector strorigin = str.origin();
                    Hep3Vector stru = str.u();
                    double strumeas = str.umeas();
                    Hep3Vector struvec = VecOp.mult(strumeas, stru);
                    Hep3Vector strvec = VecOp.add(strorigin, struvec);
                    int strlayer = str.layer();
                    if (layer == strlayer && VecOp.sub(clvec, strvec).magnitude() < Math.abs(mindist)) {
                        mindist = VecOp.sub(clvec, strvec).magnitude();
                        if (Math.abs(clvec.z()) > Math.abs(strvec.z()))
                            mindist = -mindist;
                        nearest =
                                str;
                    }

                }
        }
        return mindist;
    }

    private Integer encodeBadHitList(List<Integer> badHits) {
        Integer badHitsEncoded = 0;
        for (Integer layer : badHits)
            badHitsEncoded += (int) Math.pow(2, layer - 1);
        return badHitsEncoded;
    }

    private Integer encodeSharedHitList(List<Integer> sharedHits) {
        Integer sharedHitsEncoded = 0;
        for (Integer layer : sharedHits)
            sharedHitsEncoded += (int) Math.pow(2, layer - 1);
        return sharedHitsEncoded;
    }

    private double getErr(double errSquared) {
        if (errSquared > 0)
            return Math.sqrt(errSquared);
        else
            return -99;
    }
}
