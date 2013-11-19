package org.lcsim.hps.users.mgraham;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IProfile1D;
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
import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.hps.recon.vertexing.BFitter;
import org.lcsim.hps.recon.vertexing.BilliorTrack;
import org.lcsim.hps.recon.vertexing.BilliorVertex;
import org.lcsim.hps.recon.vertexing.StraightLineTrack;
import org.lcsim.hps.recon.tracking.FindableTrack;
import org.lcsim.hps.recon.tracking.TrackAnalysis;
import org.lcsim.hps.recon.vertexing.*;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.spacegeom.SpacePoint;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**

 @author partridge
 */
public class JasAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    private IProfile1D peffFindable;
    private IProfile1D thetaeffFindable;
    private IProfile1D phieffFindable;
    private IProfile1D ctheffFindable;
    private IProfile1D d0effFindable;
    private IProfile1D z0effFindable;
    private IProfile1D peffElectrons;
    private IProfile1D thetaeffElectrons;
    private IProfile1D phieffElectrons;
    private IProfile1D ctheffElectrons;
    private IProfile1D d0effElectrons;
    private IProfile1D z0effElectrons;
    private IProfile1D peffAxial;
    private IProfile1D thetaeffAxial;
    private IProfile1D phieffAxial;
    private IProfile1D ctheffAxial;
    private IProfile1D d0effAxial;
    private IProfile1D z0effAxial;
    private IProfile1D VxEff;
    private IProfile1D VyEff;
    private IProfile1D VzEff;
    private IProfile1D VxEffFindable;
    private IProfile1D VyEffFindable;
    private IProfile1D VzEffFindable;
    public String outputPlots = "myplots.aida";
    Map<String, IProfile1D> clsizeMap = new HashMap<String, IProfile1D>();
    String[] detNames = {"Tracker"};
    Integer[] nlayers = {8};
    int trk_count = 0;
    int nevt = 0;
    int _nmcTrk = 0;
    double _nrecTrk = 0;
    double phiTrkCut = 0.3;
    double cosThTrkCutMax = 0.2;
    double cosThTrkCutMin = 0.05;
    double pTrkCut = 0.5; //GeV
    double d0TrkCut = 2.0; //mm
    double z0TrkCut = 2.0; //mm
    double etaTrkCut = 2.5;
    int totelectrons = 0;
    double foundelectrons = 0;
    int findableelectrons = 0;
    int findableTracks = 0;
    double foundTracks = 0;
    double xref = 0.0; //mm
    public String outputTextName = "myevents.txt";
    FileWriter fw;
    PrintWriter pw;
    boolean isBeamConstrain = false;
    double[] beamsize = {0.001, 0.02, 0.02};

    public JasAnalysisDriver(int layers) {
        //  Define the efficiency histograms
        IHistogramFactory hf = aida.histogramFactory();


        peffFindable = hf.createProfile1D("Findable Efficiency vs p", "", 50, 0., 2.2);
        thetaeffFindable = hf.createProfile1D("Findable Efficiency vs theta", "", 20, 80, 100);
        phieffFindable = hf.createProfile1D("Findable Efficiency vs phi", "", 25, -0.25, 0.25);
        ctheffFindable = hf.createProfile1D("Findable Efficiency vs cos(theta)", "", 25, -0.25, 0.25);
        d0effFindable = hf.createProfile1D("Findable Efficiency vs d0", "", 50, -2., 2.);
        z0effFindable = hf.createProfile1D("Findable Efficiency vs z0", "", 50, -2., 2.);

        peffElectrons = hf.createProfile1D("Electrons Efficiency vs p", "", 50, 0., 2.2);
        thetaeffElectrons = hf.createProfile1D("Electrons Efficiency vs theta", "", 20, 80, 100);
        phieffElectrons = hf.createProfile1D("Electrons Efficiency vs phi", "", 25, -0.25, 0.25);
        ctheffElectrons = hf.createProfile1D("Electrons Efficiency vs cos(theta)", "", 25, -0.25, 0.25);
        d0effElectrons = hf.createProfile1D("Electrons Efficiency vs d0", "", 20, -1., 1.);
        z0effElectrons = hf.createProfile1D("Electrons Efficiency vs z0", "", 20, -1., 1.);

        peffAxial = hf.createProfile1D("Axial Efficiency vs p", "", 50, 0., 2.2);
        thetaeffAxial = hf.createProfile1D("Axial Efficiency vs theta", "", 20, 80, 100);
        phieffAxial = hf.createProfile1D("Axial Efficiency vs phi", "", 25, -0.25, 0.25);
        ctheffAxial = hf.createProfile1D("Axial Efficiency vs cos(theta)", "", 25, -0.25, 0.25);
        d0effAxial = hf.createProfile1D("Axial Efficiency vs d0", "", 20, -1., 1.);
        z0effAxial = hf.createProfile1D("Axial Efficiency vs z0", "", 20, -1., 1.);

        VxEff = hf.createProfile1D("Aprime Efficiency vs Vx", "", 25, 0., 50.);
        VyEff = hf.createProfile1D("Aprime Efficiency vs Vy", "", 40, -0.2, 0.2);
        VzEff = hf.createProfile1D("Aprime Efficiency vs Vz", "", 40, -0.2, 0.2);

        VxEffFindable = hf.createProfile1D("Aprime Efficiency vs Vx: Findable", "", 25, 0., 50.);
        VyEffFindable = hf.createProfile1D("Aprime Efficiency vs Vy: Findable", "", 40, -0.2, 0.2);
        VzEffFindable = hf.createProfile1D("Aprime Efficiency vs Vz: Findable", "", 40, -0.2, 0.2);
        nlayers[0] = layers;
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
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");
        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");


        Hep3Vector IP = new BasicHep3Vector(0., 0., 0.);

        //  Get the magnetic field
//        double bfield = event.getDetector().getFieldMap().getField(IP).z();
        double bfield = 0.5;

//        List<HelicalTrackHit> hthits = event.get(HelicalTrackHit.class, "MatchedHTHits");
//        String sfile = StrategyXMLUtils.getDefaultStrategiesPrefix() + "DarkPhoton-Final.xml";
//        String strategyPrefix = "/nfs/sulky21/g.ec.u12/users/mgraham/AtlasUpgrade/hps-java/src/main/resources/";
//        String sfile = "DarkPhoton-Final.xml";
//        List<SeedStrategy> slist = StrategyXMLUtils.getStrategyListFromResource(sfile);
//        List<SeedStrategy> slist = StrategyXMLUtils.getStrategyListFromFile(new File(strategyPrefix + sfile));
        List<HelicalTrackHit> toththits = event.get(HelicalTrackHit.class, "HelicalTrackHits");
//        List<HelicalTrackHit> remaininghits = event.get(HelicalTrackHit.class, "RemainingHits");

        //  Create a relational table that maps TrackerHits to MCParticles
        RelationalTable hittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> mcrelations = event.get(LCRelation.class, "HelicalTrackMCRelations");

        for (LCRelation relation : mcrelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                hittomc.add(relation.getFrom(), relation.getTo());
            }
        }

//        RelationalTable hittomcRemaining = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
//        List<LCRelation> mcrelations = event.get(LCRelation.class, "HelicalTrackMCRelations");
//        List<LCRelation> mcrelationsRemaining = event.get(LCRelation.class, "RemainingMCRelations");
//        for (LCRelation relation : mcrelationsRemaining)
//            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
//                hittomcRemaining.add(relation.getFrom(), relation.getTo());

        //  Instantiate the class that determines if a track is "findable"
        FindableTrack findable = new FindableTrack(event);

        //  Create a map between tracks and the associated MCParticle
//        List<Track> tracklist = event.getTracks();
        List<Track> tracklist = event.get(Track.class, "MatchedTracks");
        //      List<Track> lltracklist = event.get(Track.class, "LLTracks");

        RelationalTable trktomcAxial = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);

        aida.cloud1D("Matched Tracks per Event").fill(tracklist.size());
//        aida.cloud1D("Long Lived Tracks per Event").fill(lltracklist.size());
        aida.cloud1D("HelicalTrackHits per Event").fill(toththits.size());
        RelationalTable trktomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
//        RelationalTable trktomcLL = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);

        //       tracklist.addAll(lltracklist);

        RelationalTable mcHittomcP = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);

        //  Get the collections of SimTrackerHits
        List<List<SimTrackerHit>> simcols = event.get(SimTrackerHit.class);

        //  Loop over the SimTrackerHits and fill in the relational table
        for (List<SimTrackerHit> simlist : simcols) {
            for (SimTrackerHit simhit : simlist) {
                if (simhit.getMCParticle() != null) {
                    mcHittomcP.add(simhit, simhit.getMCParticle());
                }
            }
        }


        String occDir = "occupancyPlots/";

        for (SiTrackerHitStrip1D stripCluster : stripHits) {


            Set<MCParticle> mcparts = stripCluster.getMCParticles();

            List<RawTrackerHit> rthList = stripCluster.getRawHits();
            int nhits = rthList.size();

            aida.cloud1D(occDir + "associated MC Particles").fill(mcparts.size());
            aida.cloud1D(occDir + " cluster size").fill(nhits);

            if (mcparts.size() == 1)
                aida.cloud1D(occDir + " cluster size MC Particles = 1").fill(nhits);
            if (mcparts.size() == 2)
                aida.cloud1D(occDir + " cluster size MC Particles = 2").fill(nhits);
        }

        Map<Track, TrackAnalysis> tkanalMap = new HashMap<Track, TrackAnalysis>();
        Map<Track, StraightLineTrack> sltMap = new HashMap<Track, StraightLineTrack>();
        Map<Track, BilliorTrack> btMap = new HashMap<Track, BilliorTrack>();
        String trackdir = "TrackInfo/";
        //  Analyze the tracks in the event
        aida.histogram1D("number of tracks", 11, 0, 10).fill(tracklist.size());
        for (Track track : tracklist) {

            //  Calculate the track pT and cos(theta)
            double d0 = track.getTrackParameter(HelicalTrackFit.dcaIndex);
            double z0 = track.getTrackParameter(HelicalTrackFit.z0Index);
            double phi0 = track.getTrackParameter(HelicalTrackFit.phi0Index);
            double slope = track.getTrackParameter(HelicalTrackFit.slopeIndex);
            double curve = track.getTrackParameter(HelicalTrackFit.curvatureIndex);
            double d0Err = Math.sqrt(track.getErrorMatrix().e(HelicalTrackFit.dcaIndex, HelicalTrackFit.dcaIndex));
            double z0Err = Math.sqrt(track.getErrorMatrix().e(HelicalTrackFit.z0Index, HelicalTrackFit.z0Index));
            double phi0Err = Math.sqrt(track.getErrorMatrix().e(HelicalTrackFit.phi0Index, HelicalTrackFit.phi0Index));
            double slopeErr = Math.sqrt(track.getErrorMatrix().e(HelicalTrackFit.slopeIndex, HelicalTrackFit.slopeIndex));
            double curveErr = Math.sqrt(track.getErrorMatrix().e(HelicalTrackFit.curvatureIndex, HelicalTrackFit.curvatureIndex));
            double chisq = track.getChi2();
            //plot the helix parameters
            aida.cloud1D("d0").fill(d0);
            aida.cloud1D("z0").fill(z0);
            aida.cloud1D("phi0").fill(phi0);
            aida.cloud1D("slope").fill(slope);
            aida.cloud1D("curve").fill(curve);
            aida.cloud1D("chi2").fill(chisq);

            double mom[] = track.getMomentum();

            SeedTrack stEle = (SeedTrack) track;
            SeedCandidate seedEle = stEle.getSeedCandidate();
            HelicalTrackFit ht = seedEle.getHelix();

            TrackAnalysis tkanal = new TrackAnalysis(track, hittomc);
            tkanalMap.put(track, tkanal);

            BilliorTrack bt = new BilliorTrack(ht);
//            System.out.println(bt.toString());
//            System.out.println(ht.toString());
            btMap.put(track, bt);

            double eps = bt.eps();
            double z0B = bt.z0();
            double phi0B = bt.phi0();
            double theta = bt.theta();
            double curveB = bt.curvature();
            double epsErr = bt.getEpsError();
            double z0BErr = bt.getZ0Error();
            double phi0BErr = bt.getPhi0Error();
            double thetaErr = bt.getThetaError();
            double curveBErr = bt.getCurveError();


            double s = HelixUtils.PathLength(ht, (HelicalTrackHit) track.getTrackerHits().get(0));
            double y1 = HelixUtils.PointOnHelix(ht, s).y();
            double z1 = HelixUtils.PointOnHelix(ht, s).z();

            int nhits = tkanal.getNHitsNew();
            double purity = tkanal.getPurityNew();
            int nbad = tkanal.getNBadHitsNew();
            int nbadAxial = tkanal.getNBadAxialHits();
            int nbadZ = tkanal.getNBadZHits();
            int nAxial = tkanal.getNAxialHits();
            int nZ = tkanal.getNZHits();
            List<Integer> badLayers = tkanal.getBadHitList();

            aida.cloud1D(trackdir + "Mis-matched hits for all tracks").fill(nbad);
            aida.cloud1D(trackdir + "purityNew for all tracks").fill(purity);
            aida.cloud1D(trackdir + "Bad Axial hits for all tracks").fill(nbadAxial);
            aida.cloud1D(trackdir + "Bad Z hits for all tracks").fill(nbadZ);
            aida.cloud1D(trackdir + "Number of Axial hits for all tracks").fill(nAxial);
            aida.cloud1D(trackdir + "Number of Z hits for all tracks").fill(nZ);

            for (Integer bhit : badLayers) {
                aida.histogram1D(trackdir + "Layer of Bad Hit", nlayers[0], 1, nlayers[0] + 1).fill(bhit);
            }

            //  Generate a normalized histogram after 1000 events
            trk_count++;


            //  Now analyze MC Particles on this track
            MCParticle mcp = tkanal.getMCParticle();
            if (mcp != null) {

                //  Create a map between the tracks found and the assigned MC particle
                trktomc.add(track, tkanal.getMCParticle());

                //  Calculate the MC momentum and polar angle
                Hep3Vector Pmc = mcp.getMomentum();
                double pxmc = Pmc.x();
                double pymc = Pmc.y();
                double pzmc = Pmc.z();
                double ptmc = Math.sqrt(pxmc * pxmc + pymc * pymc);
                double pmc = Math.sqrt(ptmc * ptmc + pzmc * pzmc);
                double pxtk = track.getPX();
                double pytk = track.getPY();
                double pztk = track.getPZ();
                double pttk = Math.sqrt(pxtk * pxtk + pytk * pytk);
                double ptk = Math.sqrt(pttk * pttk + pztk * pztk);



                //  Calculate the helix parameters for this MC particle and pulls in pT, d0
                HelixParamCalculator helix = new HelixParamCalculator(mcp, bfield);


                double d0mc = helix.getDCA();
                double z0mc = helix.getZ0();
                double phi0mc = helix.getPhi0();
                double slopemc = helix.getSlopeSZPlane();
                double curvemc = 1 / helix.getRadius();
                double pinvresid = (1 / ptk - 1 / pmc);
                double presid = (ptk - pmc);
                double z0newMC = z0mc;
                double y0newMC = d0mc;
                aida.histogram1D(trackdir + "d0 Pull", 200, -8, 8).fill((d0 - d0mc) / d0Err);
                aida.histogram1D(trackdir + "z0 Pull", 200, -8, 8).fill((z0 - z0mc) / z0Err);
                aida.histogram1D(trackdir + "phi0 Pull", 200, -8, 8).fill((phi0 - phi0mc) / phi0Err);
                aida.histogram1D(trackdir + "slope Pull", 200, -8, 8).fill((slope - slopemc) / slopeErr);
                aida.histogram1D(trackdir + "curvature Pull", 200, -8, 8).fill((curve - curvemc) / curveErr);


                double epsmc = -d0mc;
                double thetamc = Math.PI / 2 - Math.atan(slopemc);


                aida.histogram1D(trackdir + "Billior eps Pull", 200, -8, 8).fill((eps - epsmc) / epsErr);
                aida.histogram1D(trackdir + "Billior z0 Pull", 200, -8, 8).fill((z0B - z0mc) / z0BErr);
                aida.histogram1D(trackdir + "Billior phi0 Pull", 200, -8, 8).fill((phi0B - phi0mc) / phi0BErr);
                aida.histogram1D(trackdir + "Billior theta Pull", 200, -8, 8).fill((theta - thetamc) / thetaErr);
                aida.histogram1D(trackdir + "Billior curvature Pull", 200, -8, 8).fill((curveB - curvemc) / curveBErr);

                BasicHep3Vector axial = new BasicHep3Vector();
                axial.setV(0, 1, 0);
                String hitdir = "HitsOnTrack/";
                List<TrackerHit> hitsOnTrack = track.getTrackerHits();
                MCParticle bestmcp = tkanal.getMCParticleNew();

                String tkresid = "TrackResiduals/";

                int ndaug = 0;
                if (bestmcp != null) {
                    ndaug = bestmcp.getDaughters().size();
                }
                int imain = 0;
            }
        }


        // Fitting procedure: Creat BilloirFitter. Pass two matched tracks from track
        // list into the fitter with the Space Point set as the Interaction Point.
        // BilloirFitter returns a Vertex object

        BFitter bFit = new BFitter(bfield);
        SpacePoint SP = new SpacePoint(IP);
        List<Track> vlist = new ArrayList<Track>();
        List<BilliorTrack> btlist = new ArrayList<BilliorTrack>();
        for (Track track1 : tracklist) {
            for (Track track2 : tracklist) {
                if (track1 != track2 && track1.getCharge() > 0 && track2.getCharge() < 0) {
                    /*
                     vlist.clear(); vlist.add(track1); vlist.add(track2); Vertex
                     vtx = bFit.fit(vlist, SP, isBeamConstrain);
                     System.out.println(vtx.toString()); double vtxChi2 =
                     vtx._chi2; double[] vtxPos = vtx._xyzf;
                     aida.histogram1D("Vertex Chi2", 100, 0,
                     1000).fill(vtxChi2); aida.histogram1D("Vertex X", 100, -20,
                     50).fill(vtxPos[0]); aida.histogram1D("Vertex Y", 100, -1,
                     1).fill(vtxPos[1]); aida.histogram1D("Vertex Z", 100, -1,
                     1).fill(vtxPos[2]);
                     */


                    BilliorTrack bt1 = btMap.get(track1);
                    BilliorTrack bt2 = btMap.get(track2);
                    btlist.clear();
                    btlist.add(bt1);
                    btlist.add(bt2);
                    /*
                     BilliorVertex bvertex = new BilliorVertex(1.0);
                     bvertex.fitVertex(btlist); BasicMatrix bvtxPos =
                     (BasicMatrix) bvertex.getVertexPosition(); BasicMatrix
                     bvtxCov = (BasicMatrix) bvertex.getVertexCovariance();
                     System.out.println("Constrained");
                     System.out.println("Vertex Position: " +
                     bvtxPos.toString()); System.out.println("chisq : " +
                     bvertex.getChiSq());

                     aida.histogram1D("BilliorVertex X -- Constrained", 100,
                     -10, 20).fill(bvtxPos.e(0, 0));
                     aida.histogram1D("BilliorVertex Y -- Constrained", 100,
                     -0.4, 0.4).fill(bvtxPos.e(1, 0));
                     aida.histogram1D("BilliorVertex Z -- Constrained", 100,
                     -0.4, 0.4).fill(bvtxPos.e(2, 0));
                     aida.histogram1D("BilliorVertex ChiSq -- Constrained", 100,
                     0, 50).fill(bvertex.getChiSq());
                     aida.histogram1D("BilliorVertex X Pull -- Constrained",
                     100, -4, 4).fill(bvtxPos.e(0, 0) / Math.sqrt(bvtxCov.e(0,
                     0))); aida.histogram1D("BilliorVertex Y Pull--
                     Constrained", 100, -4, 4).fill(bvtxPos.e(1, 0) /
                     Math.sqrt(bvtxCov.e(1, 1)));
                     aida.histogram1D("BilliorVertex Z Pull-- Constrained", 100,
                     -4, 4).fill(bvtxPos.e(2, 0) / Math.sqrt(bvtxCov.e(2, 2)));
                     */
                    /*
                     BilliorVertex bvertexUC = new BilliorVertex(1.0);
                     bvertexUC.doBeamSpotConstraint(false);
                     bvertexUC.fitVertex(btlist); BasicMatrix bvtxPosUC =
                     (BasicMatrix) bvertexUC.getVertexPosition(); BasicMatrix
                     bvtxCovUC = (BasicMatrix) bvertexUC.getVertexCovariance();
                     System.out.println("UnConstrained");
                     System.out.println("Vertex Position: " +
                     bvtxPosUC.toString()); System.out.println("chisq : " +
                     bvertexUC.getChiSq()); aida.histogram1D("BilliorVertex X --
                     UnConstrained", 100, -10, 20).fill(bvtxPosUC.e(0, 0));
                     aida.histogram1D("BilliorVertex Y -- UnConstrained", 100,
                     -0.4, 0.4).fill(bvtxPosUC.e(1, 0));
                     aida.histogram1D("BilliorVertex Z -- UnConstrained", 100,
                     -0.4, 0.4).fill(bvtxPosUC.e(2, 0));
                     aida.histogram1D("BilliorVertex ChiSq -- UnConstrained",
                     100, 0, 50).fill(bvertexUC.getChiSq());
                     aida.histogram1D("BilliorVertex X Pull -- UnConstrained",
                     100, -4, 4).fill(bvtxPosUC.e(0, 0) /
                     Math.sqrt(bvtxCovUC.e(0, 0)));
                     aida.histogram1D("BilliorVertex Y Pull-- UnConstrained",
                     100, -4, 4).fill(bvtxPosUC.e(1, 0) /
                     Math.sqrt(bvtxCovUC.e(1, 1)));
                     aida.histogram1D("BilliorVertex Z Pull-- UnConstrained",
                     100, -4, 4).fill(bvtxPosUC.e(2, 0) /
                     Math.sqrt(bvtxCovUC.e(2, 2)));
                     */
                    BilliorVertexer bvertexerUC = new BilliorVertexer(bfield);
                    BilliorVertex bvertexUC = bvertexerUC.fitVertex(btlist);
//                    bvertexUC.fitVertex(btlist);
                    BasicMatrix bvtxPosUC = (BasicMatrix) bvertexUC.getPosition();
                    SymmetricMatrix bvtxCovUC = bvertexUC.getCovMatrix();
                    double invMassUC = bvertexUC.getParameters().get("invMass");
//                    System.out.println("UnConstrained");
//                    System.out.println("Vertex Position:  " + bvtxPosUC.toString());
//                    System.out.println("chisq :  " + bvertexUC.getChiSq());
                    aida.histogram1D("BilliorVertex X  -- UnConstrained", 100, -10, 20).fill(bvtxPosUC.e(0, 0));
                    aida.histogram1D("BilliorVertex Y -- UnConstrained", 100, -0.4, 0.4).fill(bvtxPosUC.e(1, 0));
                    aida.histogram1D("BilliorVertex Z -- UnConstrained", 100, -0.4, 0.4).fill(bvtxPosUC.e(2, 0));
                    aida.histogram1D("BilliorVertex ChiSq -- UnConstrained", 100, 0, 50).fill(bvertexUC.getChi2());
                    aida.histogram1D("BilliorVertex X Pull -- UnConstrained", 100, -4, 4).fill(bvtxPosUC.e(0, 0) / Math.sqrt(bvtxCovUC.e(0, 0)));
                    aida.histogram1D("BilliorVertex Y Pull-- UnConstrained", 100, -4, 4).fill(bvtxPosUC.e(1, 0) / Math.sqrt(bvtxCovUC.e(1, 1)));
                    aida.histogram1D("BilliorVertex Z Pull-- UnConstrained", 100, -4, 4).fill(bvtxPosUC.e(2, 0) / Math.sqrt(bvtxCovUC.e(2, 2)));


                    BilliorVertexer bvertexer = new BilliorVertexer(bfield);
                    bvertexer.setBeamSize(beamsize);
                    bvertexer.doBeamSpotConstraint(true);

                    BilliorVertex bvertex = bvertexer.fitVertex(btlist);
                    BasicMatrix bvtxPos = (BasicMatrix) bvertex.getPosition();
                    SymmetricMatrix bvtxCov = bvertex.getCovMatrix();
                    double invMass = bvertex.getParameters().get("invMass");

//                    System.out.println("Constrained");
//                    System.out.println("Vertex Position:  " + bvtxPos.toString());
//                    System.out.println("chisq :  " + bvertex.getChiSq());
                    aida.histogram1D("BilliorVertex X  -- Constrained", 100, -10, 20).fill(bvtxPos.e(0, 0));
                    aida.histogram1D("BilliorVertex Y -- Constrained", 100, -0.4, 0.4).fill(bvtxPos.e(1, 0));
                    aida.histogram1D("BilliorVertex Z -- Constrained", 100, -0.4, 0.4).fill(bvtxPos.e(2, 0));
                    aida.histogram1D("BilliorVertex ChiSq -- Constrained", 100, -10, 50).fill(bvertex.getChi2());
                    aida.histogram1D("BilliorVertex X Pull -- Constrained", 100, -4, 4).fill(bvtxPos.e(0, 0) / Math.sqrt(bvtxCov.e(0, 0)));
                    aida.histogram1D("BilliorVertex Y Pull-- Constrained", 100, -4, 4).fill(bvtxPos.e(1, 0) / Math.sqrt(bvtxCov.e(1, 1)));
                    aida.histogram1D("BilliorVertex Z Pull-- Constrained", 100, -4, 4).fill(bvtxPos.e(2, 0) / Math.sqrt(bvtxCov.e(2, 2)));

                    aida.histogram1D("BilliorVertex Mass  -- Constrained", 250, 0.0, 0.25).fill(invMass);
                    aida.histogram1D("BilliorVertex Mass  -- UnConstrained", 250, 0.0, 0.25).fill(invMassUC);


                    BilliorVertexer bconvertexer = new BilliorVertexer(bfield);
                    bconvertexer.setBeamSize(beamsize);
                    bconvertexer.doTargetConstraint(true);

                    BilliorVertex bsconfit = bconvertexer.fitVertex(btlist);
//                    bvertexUC.fitVertex(btlist);
                    BasicMatrix bsconvtxPos = (BasicMatrix) bsconfit.getPosition();
                    SymmetricMatrix bsconvtxCov = bsconfit.getCovMatrix();
                    double invMassBSCon = bsconfit.getParameters().get("invMass");


                    aida.histogram1D("BilliorVertex X  -- BS Constrained", 100, -10, 20).fill(bsconvtxPos.e(0, 0));
                    aida.histogram1D("BilliorVertex Y -- BS Constrained", 100, -0.4, 0.4).fill(bsconvtxPos.e(1, 0));
                    aida.histogram1D("BilliorVertex Z -- BS Constrained", 100, -0.4, 0.4).fill(bsconvtxPos.e(2, 0));
                    aida.histogram1D("BilliorVertex ChiSq -- BS Constrained", 100, -10, 50).fill(bsconfit.getChi2());
                    aida.histogram1D("BilliorVertex X Pull -- BS Constrained", 100, -4, 4).fill(bsconvtxPos.e(0, 0) / Math.sqrt(bsconvtxCov.e(0, 0)));
                    aida.histogram1D("BilliorVertex Y Pull-- BS Constrained", 100, -4, 4).fill(bsconvtxPos.e(1, 0) / Math.sqrt(bsconvtxCov.e(1, 1)));
                    aida.histogram1D("BilliorVertex Z Pull-- BS Constrained", 100, -4, 4).fill(bsconvtxPos.e(2, 0) / Math.sqrt(bsconvtxCov.e(2, 2)));

                    aida.histogram1D("BilliorVertex Mass  -- BS Constrained", 100, 0.08, 0.12).fill(invMassBSCon);
                }
            }
        }


        //  Now loop over all MC Particles
        List<MCParticle> mclist = event.getMCParticles();
        pw.format("%d ", nevt);

        for (MCParticle mcp : mclist) {
            if (mcp.getParents().size() > 0) {
                if (mcp.getParents().get(0).getPDGID() == 622) {
//                    boolean find= findable.InnerTrackerIsFindable(mcp, nlayers[0],true);
//                    System.out.println("A' Track Findable? "+find+";  nlayers = "+nlayers[0]);
                    boolean find = findable.InnerTrackerIsFindable(mcp, nlayers[0]);
                    int ifind = 0;
                    if (find)
                        ifind = 1;
                    double ch = mcp.getCharge();
                    pw.format("%d  %2.0f ", ifind, ch);
                    Set<SimTrackerHit> mchitlist = mcHittomcP.allTo(mcp);

                    for (int i = 0; i < 10; i++) {
                        if (mchitlist.size() > i + 1) {
                            SimTrackerHit sth = (SimTrackerHit) mchitlist.toArray()[i];

                            if (sth != null)
                                pw.format("%d %5.5f %5.5f %5.5f ", sth.getLayer(), sth.getPoint()[1], sth.getPoint()[2], sth.getPoint()[0]);
                            else
                                pw.format("%d %5.5f %5.5f %5.5f ", 99, -666, -666, -666);
                        } else {
                            pw.format("%d %5.5f %5.5f %5.5f ", 99, -666.6, -666.6, -666.6);
                        }
                    }
                }
            }

        }
        pw.println();
        int _nchMCP = 0;
        int _nchMCPBar = 0;
        for (MCParticle mcp : mclist) {

            //  Calculate the pT and polar angle of the MC particle
            double px = mcp.getPX();
            double py = mcp.getPY();
            double pz = mcp.getPZ();
            double pt = Math.sqrt(px * px + py * py);
            double p = Math.sqrt(pt * pt + pz * pz);
            double cth = pz / p;
            double theta = 180. * Math.acos(cth) / Math.PI;
            double eta = -Math.log(Math.tan(Math.atan2(pt, pz) / 2));
            double phi = Math.atan2(py, px);
            int pdgid = mcp.getPDGID();
            //  Find the number of layers hit by this mc particle
//            System.out.println("MC pt=" + pt);
            int nhits = findable.LayersHit(mcp);
            Set<SimTrackerHit> mchitlist = mcHittomcP.allTo(mcp);
            boolean isFindable = findable.InnerTrackerIsFindable(mcp, nlayers[0] - 2);
//            boolean isFindable = findable.InnerTrackerIsFindable(mcp, nlayers[0]);
            Set<HelicalTrackCross> hitlist = hittomc.allTo(mcp);

            //  Calculate the helix parameters for this MC particle
            HelixParamCalculator helix = new HelixParamCalculator(mcp, bfield);
            double d0 = helix.getDCA();
            double z0 = helix.getZ0();


            //  Check cases where we have multiple tracks associated with this MC particle
            Set<Track> trklist = trktomc.allTo(mcp);
            int ntrk = trklist.size();


            Set<Track> trklistAxial = trktomcAxial.allTo(mcp);
            int ntrkAxial = trklistAxial.size();

            if (mcp.getPDGID() == 622) {
                boolean bothreco = true;
                boolean bothfindable = true;
                //it's the A'...let's see if we found both tracks.
                List<MCParticle> daughters = mcp.getDaughters();
                for (MCParticle d : daughters) {
                    if (trktomc.allTo(d).size() == 0) {
                        bothreco = false;
                    }
                    if (!findable.InnerTrackerIsFindable(d, nlayers[0] - 2)) {
//                    if (!findable.InnerTrackerIsFindable(d, nlayers[0])) {
                        bothfindable = false;
                    }
                }
                double vtxWgt = 0;
                if (bothreco) {
                    vtxWgt = 1.0;
                }
                VxEff.fill(mcp.getOriginX(), vtxWgt);
                VyEff.fill(mcp.getOriginY(), vtxWgt);
                VzEff.fill(mcp.getOriginZ(), vtxWgt);
                if (bothfindable) {
                    VxEffFindable.fill(mcp.getOriginX(), vtxWgt);
                    VyEffFindable.fill(mcp.getOriginY(), vtxWgt);
                    VzEffFindable.fill(mcp.getOriginZ(), vtxWgt);
                }
            }

            if (ntrk > 1) {
                //  Count tracks where the assigned MC particle has more than 1 hit
                int nmulthits = 0;
                for (Track trk : trklist) {
                    TrackAnalysis tkanal = tkanalMap.get(trk);
                    if (tkanal.getNBadHits() < tkanal.getNHits() - 1) {
                        nmulthits++;
                    }
                }
                //  Flag any anomalous cases that we find
                if (nmulthits > 1) {
                    System.out.println("2 tracks associated with a single MC Particle");
                }
            }

            if (isFindable) {
                _nchMCP++;
                findableTracks++;
                double wgt = 0.;
                if (ntrk > 0) {
                    wgt = 1.;
                }
                foundTracks += wgt;
                peffFindable.fill(p, wgt);
                phieffFindable.fill(phi, wgt);
                thetaeffFindable.fill(theta, wgt);
                ctheffFindable.fill(cth, wgt);
                d0effFindable.fill(d0, wgt);
                z0effFindable.fill(z0, wgt);
                //              if (wgt == 0)
//                    System.out.println("Missed a findable track!");

                double wgtAxial = 0.;
                if (ntrkAxial > 0) {
                    wgtAxial = 1.;
                }
                peffAxial.fill(p, wgtAxial);
                phieffAxial.fill(phi, wgtAxial);
                thetaeffAxial.fill(theta, wgtAxial);
                ctheffAxial.fill(cth, wgtAxial);
                d0effAxial.fill(d0, wgtAxial);
                z0effAxial.fill(z0, wgtAxial);


            }

            if (mcp.getParents().size() == 1 && mcp.getParents().get(0).getPDGID() == 622) {
                totelectrons++;

                if (isFindable) {
                    findableelectrons++;
                    double wgt = 0.;
                    if (ntrk > 0) {
                        wgt = 1.;
                    }
                    foundelectrons += wgt;
                    peffElectrons.fill(p, wgt);
                    phieffElectrons.fill(phi, wgt);
                    thetaeffElectrons.fill(theta, wgt);
                    ctheffElectrons.fill(cth, wgt);
                    d0effElectrons.fill(d0, wgt);
                    z0effElectrons.fill(z0, wgt);
                    if (wgt == 0 && pdgid > 0) {
                        System.out.println("Missed a findable ELECTRON!!!!!");
                    }
                    if (wgt == 0 && pdgid < 0) {
                        System.out.println("Missed a findable POSITRON!!!!!");
                    }

                    double wgtAxial = 0.;
                    if (ntrkAxial > 0) {
                        wgtAxial = 1.;
                    }
                    peffAxial.fill(p, wgtAxial);
                    phieffAxial.fill(phi, wgtAxial);
                    thetaeffAxial.fill(theta, wgtAxial);
                    ctheffAxial.fill(cth, wgtAxial);
                    d0effAxial.fill(d0, wgtAxial);
                    z0effAxial.fill(z0, wgtAxial);
                }
            }


        }

        return;
    }

    public void endOfData() {
        try {
            aida.saveAs(outputPlots);
        } catch (IOException ex) {
            Logger.getLogger(JasAnalysisDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        pw.close();
        try {
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(JasAnalysisDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("# of reco tracks = " + foundTracks + "; # of MC tracks = " + findableTracks + "; Efficiency = " + foundTracks / findableTracks);
        System.out.println("# of reco ele/pos = " + foundelectrons + "; # of findable ele/pos = " + findableelectrons + "; Efficiency = " + foundelectrons / findableelectrons);
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setOutputText(String output) {
        this.outputTextName = output;
    }

    private double getr(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    protected double drcalc(Hep3Vector pos, SymmetricMatrix cov) {
        double x = pos.x();
        double y = pos.y();
        double r2 = x * x + y * y;
        return Math.sqrt((x * x * cov.e(0, 0) + y * y * cov.e(1, 1) + 2. * x * y * cov.e(0, 1)) / r2);
    }

    protected double drphicalc(Hep3Vector pos, SymmetricMatrix cov) {
        double x = pos.x();
        double y = pos.y();
        double r2 = x * x + y * y;
        return Math.sqrt((y * y * cov.e(0, 0) + x * x * cov.e(1, 1) - 2. * x * y * cov.e(0, 1)) / r2);
    }

    private double getphi(double x, double y) {
        double phi = Math.atan2(y, x);
        if (phi < 0.) {
            phi += 2. * Math.PI;
        }
        return phi;
    }

    private double getdxdy(Hep3Vector hitpos, Hep3Vector posonhelix) {
        return Math.sqrt(Math.pow(hitpos.x() - posonhelix.x(), 2) + Math.pow(hitpos.y() - posonhelix.y(), 2));
    }

    private double getdxdyErr(Hep3Vector hitpos, Hep3Vector posonhelix, SymmetricMatrix cov) {
        double dxdySq = Math.pow(hitpos.x() - posonhelix.x(), 2) + Math.pow(hitpos.y() - posonhelix.y(), 2);
        double ErrSqDxDySq = 4 * (cov.e(0, 0) * Math.pow(hitpos.x() - posonhelix.x(), 2) + cov.e(1, 1) * Math.pow(hitpos.y() - posonhelix.y(), 2));
        double error = Math.sqrt(ErrSqDxDySq / dxdySq) / 2;
        return error;
    }

    private void fillClouds(String dir, String species, int nhits, double p, double cth, double y0, double z0, double doca, double[] poca, double pinvresid, double presid, double y0resid, double z0resid, double docaresid, double[] pocaresid) {
        double nbins = 4;
        double[] pBins = {1, 2, 3, 4, 5};

        aida.cloud1D(dir + "Hits for all " + species).fill(nhits);
        aida.cloud1D(dir + "p for all " + species).fill(p);
        aida.cloud1D(dir + "cos(theta) for all " + species).fill(cth);
        aida.cloud1D(dir + "y0 for all " + species).fill(y0);
        aida.cloud1D(dir + "z0 for all " + species).fill(z0);
        aida.cloud1D(dir + "doca for all " + species).fill(doca);
        aida.cloud1D(dir + "xoca for all " + species).fill(poca[0]);
        aida.cloud1D(dir + "yoca for all " + species).fill(poca[1]);
        aida.cloud1D(dir + "zoca for all " + species).fill(poca[2]);
        aida.histogram1D(dir + "p^-1 Residual for all " + species, 50, -0.1, 0.1).fill(pinvresid);
        aida.cloud1D(dir + "p Residual for all " + species).fill(presid);
        aida.histogram1D(dir + "y0 Residual for all " + species, 50, -1, 1).fill(y0resid);
        aida.histogram1D(dir + "z0 Residual for all " + species, 50, -1, 1).fill(z0resid);

        if (p > pBins[0] && p < pBins[1]) {
            aida.histogram1D(dir + "doca Residual for 1<p<2 " + species, 50, -0.5, 0.5).fill(docaresid);
            aida.histogram1D(dir + "xoca Residual for 1<p<2 " + species, 50, -0.5, 0.5).fill(pocaresid[0]);
            aida.histogram1D(dir + "yoca Residual for 1<p<2 " + species, 50, -0.5, 0.5).fill(pocaresid[1]);
            aida.histogram1D(dir + "zoca Residual for 1<p<2 " + species, 50, -0.5, 0.5).fill(pocaresid[2]);
        } else if (p > pBins[1] && p < pBins[2]) {
            aida.histogram1D(dir + "doca Residual for 2<p<3 " + species, 50, -0.5, 0.5).fill(docaresid);
            aida.histogram1D(dir + "xoca Residual for 2<p<3 " + species, 50, -0.5, 0.5).fill(pocaresid[0]);
            aida.histogram1D(dir + "yoca Residual for 2<p<3 " + species, 50, -0.5, 0.5).fill(pocaresid[1]);
            aida.histogram1D(dir + "zoca Residual for 2<p<3 " + species, 50, -0.5, 0.5).fill(pocaresid[2]);
        } else if (p > pBins[2] && p < pBins[3]) {
            aida.histogram1D(dir + "doca Residual for 3<p<4 " + species, 50, -0.5, 0.5).fill(docaresid);
            aida.histogram1D(dir + "xoca Residual for 3<p<4 " + species, 50, -0.5, 0.5).fill(pocaresid[0]);
            aida.histogram1D(dir + "yoca Residual for 3<p<4 " + species, 50, -0.5, 0.5).fill(pocaresid[1]);
            aida.histogram1D(dir + "zoca Residual for 3<p<4 " + species, 50, -0.5, 0.5).fill(pocaresid[2]);
        } else if (p > pBins[3] && p < pBins[4]) {
            aida.histogram1D(dir + "doca Residual for 4<p<5 " + species, 50, -0.5, 0.5).fill(docaresid);
            aida.histogram1D(dir + "xoca Residual for 4<p<5 " + species, 50, -0.5, 0.5).fill(pocaresid[0]);
            aida.histogram1D(dir + "yoca Residual for 4<p<5 " + species, 50, -0.5, 0.5).fill(pocaresid[1]);
            aida.histogram1D(dir + "zoca Residual for 4<p<5 " + species, 50, -0.5, 0.5).fill(pocaresid[2]);
        }

        aida.histogram1D(dir + "doca Residual for all " + species, 50, -0.5, 0.5).fill(docaresid);
        aida.histogram1D(dir + "xoca Residual for all " + species, 50, -0.5, 0.5).fill(pocaresid[0]);
        aida.histogram1D(dir + "yoca Residual for all " + species, 50, -0.5, 0.5).fill(pocaresid[1]);
        aida.histogram1D(dir + "zoca Residual for all " + species, 50, -0.5, 0.5).fill(pocaresid[2]);
    }

    private void fillTrackInfo(String dir, String species, double chi2, int nhits, double p, double pperp, double px, double py, double pz, double phi, double cth, double doca, double xoca, double yoca, double zoca) {
        aida.cloud1D(dir + "total chi^2 for  " + species).fill(chi2);

//                aida.cloud1D(trackdir + "circle chi^2 for  " + species).fill(ht.chisq()[0]);
//                aida.cloud1D(trackdir + "linear chi^2 for  " + species).fill(ht.chisq()[1]
        aida.cloud1D(dir + "Hits for  " + species).fill(nhits);
        aida.cloud1D(dir + "p for  " + species).fill(p);
        aida.cloud1D(dir + "pperp for  " + species).fill(pperp);
        aida.cloud1D(dir + "px for  " + species).fill(px);
        aida.cloud1D(dir + "py for  " + species).fill(py);
        aida.cloud1D(dir + "pz for  " + species).fill(pz);
        aida.cloud1D(dir + "phi for  " + species).fill(phi);
        aida.cloud1D(dir + "cos(theta) for  " + species).fill(cth);
        aida.cloud1D(dir + "DOCA for  " + species).fill(doca);
        aida.cloud1D(dir + "XOCA for  " + species).fill(xoca);
        aida.cloud1D(dir + "YOCA for  " + species).fill(yoca);
        aida.cloud1D(dir + "ZOCA for  " + species).fill(zoca);
        aida.cloud2D(dir + "doca vs xoca for  " + species).fill(xoca, doca);
    }

    private void fillVertexInfo(String apdir, String vertex, double chisq, Hep3Vector vtx, SymmetricMatrix vtxcov, double invMass, double deltaPhi, double oAngle, double cosAlpha) {
        aida.histogram1D(apdir + vertex + " vertex chi^2", 50, 0, 1000).fill(chisq);
        aida.histogram1D(apdir + vertex + " vertex X", 50, -10, 10).fill(vtx.x());
        aida.histogram1D(apdir + vertex + " vertex X Wide", 100, -10, 50).fill(vtx.x());
        aida.histogram1D(apdir + vertex + " vertex sigma X", 50, 0, 1).fill(Math.sqrt(vtxcov.e(0, 0)));
        aida.histogram1D(apdir + vertex + " vertex signifigance X", 50, -100, 100).fill(vtx.x() / Math.sqrt(vtxcov.e(0, 0)));
        aida.histogram1D(apdir + vertex + " vertex X Positive Tail", 50, 10, 110).fill(vtx.x());
        aida.histogram1D(apdir + vertex + " vertex Y", 50, -1, 1).fill(vtx.y());
        aida.histogram1D(apdir + vertex + " vertex sigma Y", 50, 0, 0.1).fill(Math.sqrt(vtxcov.e(1, 1)));
        aida.histogram1D(apdir + vertex + " vertex Z", 50, -1, 1).fill(vtx.z());
        aida.histogram1D(apdir + vertex + " vertex sigma Z", 50, 0, 0.1).fill(Math.sqrt(vtxcov.e(2, 2)));
        aida.histogram1D(apdir + vertex + " e+e- Invariant Mass", 100, 0.05, 0.25).fill(invMass);
        aida.histogram1D(apdir + vertex + " vertex deltaPhi", 50, -0.2, 0.2).fill(deltaPhi);
        aida.histogram1D(apdir + vertex + " vertex cos(opening angle)", 50, 0.8, 1).fill(oAngle);
        aida.histogram1D(apdir + vertex + " vertex cos(Alpha)", 100, 0.5, 1).fill(cosAlpha);

    }

    private double getInvMass(Track track1, StraightLineTrack slt1, Track track2, StraightLineTrack slt2) {
        double esum = 0.;
        double pxsum = 0.;
        double pysum = 0.;
        double pzsum = 0.;
        double chargesum = 0.;
        double me = 0.000511;
        // Loop over jets

        double p1x = track1.getPX();
        double p1y = track1.getPY();
        double p1z = track1.getPZ();
        double p1mag2 = p1x * p1x + p1y * p1y + p1z * p1z;
        double e1 = Math.sqrt(p1mag2 + me * me);
        double dydx1 = slt1.dydx();
        double dzdx1 = slt1.dzdx();
        double s1sq = 1 + 1 / (dydx1 * dydx1) + (dzdx1 * dzdx1) / (dydx1 * dydx1);
        double truep1y = Math.sqrt(p1mag2 / s1sq);
        if (dydx1 < 0) {
            truep1y = -truep1y;
        }
        double truep1x = truep1y / dydx1;
        double truep1z = dzdx1 * truep1x;

        double p2x = track2.getPX();
        double p2y = track2.getPY();
        double p2z = track2.getPZ();
        double p2mag2 = p2x * p2x + p2y * p2y + p2z * p2z;
        double e2 = Math.sqrt(p2mag2 + me * me);

        double dydx2 = slt2.dydx();
        double dzdx2 = slt2.dzdx();
        double s2sq = 1 + 1 / (dydx2 * dydx2) + (dzdx2 * dzdx2) / (dydx2 * dydx2);
        double truep2y = Math.sqrt(p2mag2 / s2sq);
        if (dydx2 < 0) {
            truep2y = -truep2y;
        }
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
        double p1x = track1.getPX();
        double p1y = track1.getPY();
        double p1z = track1.getPZ();
        double p1mag2 = p1x * p1x + p1y * p1y + p1z * p1z;
        double e1 = Math.sqrt(p1mag2 + me * me);
        double dydx1 = slt1.dydx();
        double dzdx1 = slt1.dzdx();
        double s1sq = 1 + 1 / (dydx1 * dydx1) + (dzdx1 * dzdx1) / (dydx1 * dydx1);
        truep[1] = Math.sqrt(p1mag2 / s1sq);
        if (dydx1 < 0) {
            truep[1] = -truep[1];
        }
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
}
