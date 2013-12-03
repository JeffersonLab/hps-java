/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.users.mgraham.jlabrotation;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.*;

import hep.physics.matrix.BasicMatrix;
import hep.physics.vec.VecOp;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.fit.helicaltrack.TrackDirection;
import org.lcsim.hps.recon.vertexing.BilliorTrack;
import org.lcsim.hps.recon.vertexing.BilliorVertex;
import org.lcsim.hps.recon.vertexing.StraightLineTrack;
import org.lcsim.hps.recon.tracking.FindableTrack;
import org.lcsim.hps.recon.tracking.TrackAnalysis;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**

 @author partridge
 */
public class DetailedAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    private IProfile1D phifake;
    private IProfile1D pfake;
    private IProfile1D cthfake;
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
    private IHistogram1D fakes;
    private IHistogram1D nfakes;
    private IProfile1D HitEffdEdX;
    private IProfile1D ClHitEffdEdX;
    private IProfile1D ClHitEffY;
    private IProfile1D ClHitEffZ;
    private IProfile1D STdEdXY;
    private IProfile1D STdEdXZ;
    private IProfile1D frdEdXY;
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
    double beamP = 2.2;
    public String outputTextName = "myevents.txt";
    FileWriter fw;
    PrintWriter pw;
    double[] beamsize = {0.001, 0.02, 0.02};

    public DetailedAnalysisDriver(int layers) {
        nlayers[0] = layers;

        //  Define the efficiency histograms
        IHistogramFactory hf = aida.histogramFactory();


        peffFindable = hf.createProfile1D("Findable Efficiency vs p", "", 20, 0., beamP);
        thetaeffFindable = hf.createProfile1D("Findable Efficiency vs theta", "", 20, 80, 100);
        phieffFindable = hf.createProfile1D("Findable Efficiency vs phi", "", 25, -0.25, 0.25);
        ctheffFindable = hf.createProfile1D("Findable Efficiency vs cos(theta)", "", 25, -0.25, 0.25);
        d0effFindable = hf.createProfile1D("Findable Efficiency vs d0", "", 50, -2., 2.);
        z0effFindable = hf.createProfile1D("Findable Efficiency vs z0", "", 50, -2., 2.);

        peffElectrons = hf.createProfile1D("Electrons Efficiency vs p", "", 20, 0., beamP);
        thetaeffElectrons = hf.createProfile1D("Electrons Efficiency vs theta", "", 20, 80, 100);
        phieffElectrons = hf.createProfile1D("Electrons Efficiency vs phi", "", 25, -0.25, 0.25);
        ctheffElectrons = hf.createProfile1D("Electrons Efficiency vs cos(theta)", "", 25, -0.25, 0.25);
        d0effElectrons = hf.createProfile1D("Electrons Efficiency vs d0", "", 20, -1., 1.);
        z0effElectrons = hf.createProfile1D("Electrons Efficiency vs z0", "", 20, -1., 1.);

        peffAxial = hf.createProfile1D("Axial Efficiency vs p", "", 20, 0., beamP);
        thetaeffAxial = hf.createProfile1D("Axial Efficiency vs theta", "", 20, 80, 100);
        phieffAxial = hf.createProfile1D("Axial Efficiency vs phi", "", 25, -0.25, 0.25);
        ctheffAxial = hf.createProfile1D("Axial Efficiency vs cos(theta)", "", 25, -0.25, 0.25);
        d0effAxial = hf.createProfile1D("Axial Efficiency vs d0", "", 20, -1., 1.);
        z0effAxial = hf.createProfile1D("Axial Efficiency vs z0", "", 20, -1., 1.);

        cthfake = hf.createProfile1D("Fake rate vs  cos(theta)", "", 25, -0.25, 0.25);
        phifake = hf.createProfile1D("Fake rate vs phi", "", 25, -0.25, 0.25);
        pfake = hf.createProfile1D("Fake rate vs p", "", 20, 0, 6);

        fakes = hf.createHistogram1D("Number of mis-matched hits (unnormalized)", "", 10, 0., 10.);
        nfakes = hf.createHistogram1D("Number of mis-matched hits (normalized)", "", 10, 0., 10.);

        HitEffdEdX = hf.createProfile1D("Strip Hit Efficiency vs dEdX", "", 50, 0, 0.3);
        ClHitEffdEdX = hf.createProfile1D("Cluster Hit Efficiency vs dEdX", "", 50, 0, 0.3);
        ClHitEffY = hf.createProfile1D("Cluster Hit Efficiency vs y", "", 50, -100, 100);
        ClHitEffZ = hf.createProfile1D("Cluster Hit Efficiency vs z", "", 50, -100, 100);
        STdEdXY = hf.createProfile1D("SimTHit dEdX vs y", "", 50, -100, 100);
        frdEdXY = hf.createProfile1D("fractional dEdX vs y", "", 50, -100, 100);
        STdEdXZ = hf.createProfile1D("SimTHit dEdX vs z", "", 50, -100, 100);

        VxEff = hf.createProfile1D("Aprime Efficiency vs Vx", "", 25, 0., 50.);
        VyEff = hf.createProfile1D("Aprime Efficiency vs Vy", "", 40, -0.2, 0.2);
        VzEff = hf.createProfile1D("Aprime Efficiency vs Vz", "", 40, -0.2, 0.2);

        VxEffFindable = hf.createProfile1D("Aprime Efficiency vs Vx: Findable", "", 25, 0., 50.);
        VyEffFindable = hf.createProfile1D("Aprime Efficiency vs Vy: Findable", "", 40, -0.2, 0.2);
        VzEffFindable = hf.createProfile1D("Aprime Efficiency vs Vz: Findable", "", 40, -0.2, 0.2);

        int i, j;
        for (i = 0; i < 1; i++)
            for (j = 0; j < nlayers[i]; j++) {
                int laynum = j + 1;
                String profname = detNames[i] + "_layer" + laynum + " cluster size vs y";
                String key = detNames[i] + "_layer" + laynum;
                clsizeMap.put(key, hf.createProfile1D(profname, 20, -15, 15));
            }
    }

    @Override
    public void process(
            EventHeader event) {
        if (nevt == 0)
            try {
//open things up
                fw = new FileWriter(outputTextName);
                pw = new PrintWriter(fw);
            } catch (IOException ex) {
                Logger.getLogger(DetailedAnalysisDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
        //  Increment the event counter
        nevt++;
        String resDir = "residualsPlots/";
        String resDirBar = "residualsBarrelPlots/";
        String resDirEC = "residualsEndcapPlots/";
        String simDir = "STHitPlots/";
        String debugDir = "debugPlots/";
        String occDir = "occupancyPlots/";
        //  Get the magnetic field
        Hep3Vector IP = new BasicHep3Vector(0., 0., 0.1);
//        double bfield = event.getDetector().getFieldMap().getField(IP).y();
        double bfield = 0.5;

        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");
        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
        // dump SThit information
        String[] input_hit_collections = {"TrackerHits"};
        for (String input : input_hit_collections) {
            List<SimTrackerHit> sthits = event.getSimTrackerHits(input);
            int[] nhits = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            for (SimTrackerHit st : sthits) {
                String detector = st.getDetectorElement().getName();
                int layer = st.getLayerNumber();
                double[] hp = st.getPoint();
                Hep3Vector hitPos = new BasicHep3Vector(hp[0], hp[1], hp[2]);
                double r = Math.sqrt(hp[0] * hp[0] + hp[1] * hp[1]);
                double theta = Math.atan2(r, hp[2]);
                double eta = -Math.log(Math.tan(theta / 2));
                double phi = Math.atan2(hp[1], hp[0]);
                nhits[layer]++;
                double hitwgt = 0;
                double clhitwgt = 0;
                for (RawTrackerHit rth : rawHits) {
                    List<SimTrackerHit> SthFromRth = rth.getSimTrackerHits();
                    if (SthFromRth.contains(st))
                        hitwgt = 1.0;
                }
                for (SiTrackerHitStrip1D cluster : stripHits) {
                    double measdedx = cluster.getdEdx() * 1000.0;

                    List<RawTrackerHit> RthFromSith = cluster.getRawHits();

                    for (RawTrackerHit rth : RthFromSith) {
                        List<SimTrackerHit> SthFromRth = rth.getSimTrackerHits();
                        if (SthFromRth.contains(st)) {
                            clhitwgt = 1.0;
                            double totdedx = 0;
                            for (SimTrackerHit sthtemp : SthFromRth)
                                totdedx = totdedx + sthtemp.getdEdx() * 1000.0;
                            aida.histogram1D(simDir + "delta dEdX", 50, -0.2, 0.2).fill(measdedx - totdedx);
                            aida.histogram1D(simDir + "fractional dEdX", 50, -1, 1.).fill((measdedx - totdedx) / totdedx);
                            aida.cloud1D(simDir + "fractional dEdX Cloud").fill((measdedx - totdedx) / totdedx);
                            //          if (Math.abs((measdedx - dedx) / dedx) < 1)
                            frdEdXY.fill(hp[1], (measdedx - totdedx) / totdedx);
                            //          if (dedx == 0)
                            //              System.out.println("*****************         dedx==0    ********");
                        }
                    }
                }
                //HitEffdEdX.fill(dedx, hitwgt);
                //ClHitEffdEdX.fill(dedx, clhitwgt);
                ClHitEffY.fill(hp[1], clhitwgt);
                ClHitEffZ.fill(hp[2], clhitwgt);
                //STdEdXY.fill(hp[1], dedx);
                //STdEdXZ.fill(hp[2], dedx);
                //aida.histogram1D(simDir + " dedx", 50, 0, 0.3).fill(dedx);
//                if (hitwgt == 0) {
//                    System.out.println("TrackAnalysis:  found an inefficiency hit:  " + dedx);
//                }

                //aida.cloud1D(simDir + input + " layer " + layer + " STHit p").fill(mom);
                aida.cloud1D(simDir + input + " layer " + layer + " STHit y").fill(hp[1]);
                aida.cloud1D(simDir + input + " layer " + layer + " STHit z").fill(hp[2]);
                aida.cloud2D(simDir + input + " layer " + layer + " STHit y vs z").fill(hp[2], hp[1]);
                aida.histogram2D(simDir + input + " layer " + layer + " STHit y vs z occupancy", 100, -15, 15, 500, -15, 15).fill(hp[2], hp[1]);

            }
            int i = 0;
            while (i < nlayers[0]) {
                if (nhits[i] > 0)
                    aida.cloud1D(simDir + input + "layer " + i + " number of ST hits").fill(nhits[i]);
                i++;
            }
        }


//        List<HelicalTrackHit> hthits = event.get(HelicalTrackHit.class, "MatchedHTHits");
        List<HelicalTrackHit> toththits = event.get(HelicalTrackHit.class, "HelicalTrackHits");
        if (event.hasCollection(HelicalTrackHit.class, "AxialTrackHits")) {
            List<HelicalTrackHit> axialhits = event.get(HelicalTrackHit.class, "AxialTrackHits");
            int nAxialHitsTotal = axialhits.size();
            int nL1Hits = 0;
            for (HelicalTrackHit hth : axialhits)
                if (hth.Layer() == 1)
                    nL1Hits++;
        }
        Map<String, Integer> occupancyMap = new HashMap<String, Integer>();
        for (RawTrackerHit rh : rawHits) {
            IDetectorElement rhDetE = rh.getDetectorElement();

            String rhDetName = rhDetE.getName();
            int rhLayer = rh.getLayerNumber();

            for (String myname : detNames)
                if (rhDetName.contains(myname)) {
                    String detlayer = myname + "_" + rhLayer;
                    Integer myint = occupancyMap.get(detlayer);
                    if (myint == null)
                        myint = 1;
                    myint++;
                    occupancyMap.put(detlayer, myint);
                }
        }
        Set<String> mykeyset = (Set<String>) occupancyMap.keySet();
        for (String keys : mykeyset)
            aida.cloud1D(occDir + keys + " # of hits").fill(occupancyMap.get(keys));

        for (SiTrackerHitStrip1D stripCluster : stripHits) {
            Hep3Vector strCluPos = stripCluster.getPositionAsVector();
            double yHit = strCluPos.y();
            Set<MCParticle> mcparts = stripCluster.getMCParticles();
            aida.cloud1D(occDir + "associated MC Particles").fill(mcparts.size());
            List<RawTrackerHit> rthList = stripCluster.getRawHits();
            int nhits = rthList.size();
            String detlayer = "Foobar";
            for (RawTrackerHit rth : rthList) {
                IDetectorElement rhDetE = rth.getDetectorElement();
                String rhDetName = rhDetE.getName();
                int rhLayer = rth.getLayerNumber();
                for (String myname : detNames)
                    if (rhDetName.contains(myname))
                        detlayer = myname + "_layer" + rhLayer;
            }
            clsizeMap.get(detlayer).fill(yHit, nhits);
            aida.cloud1D(occDir + detlayer + "associated MC Particles").fill(mcparts.size());
            aida.cloud1D(occDir + detlayer + " cluster size").fill(nhits);
        }

        //  Create a relational table that maps TrackerHits to MCParticles
        RelationalTable hittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
//        List<LCRelation> mcrelations = event.get(LCRelation.class, "HelicalTrackMCRelations");
        List<LCRelation> mcrelations = event.get(LCRelation.class, "RotatedMCRelations");

        for (LCRelation relation : mcrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittomc.add(relation.getFrom(), relation.getTo());

        RelationalTable hittomcAxial = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(Track.class, "AxialTracks")) {
//        List<LCRelation> mcrelations = event.get(LCRelation.class, "HelicalTrackMCRelations");
            List<LCRelation> mcrelationsAxial = event.get(LCRelation.class, "AxialTrackMCRelations");
            for (LCRelation relation : mcrelationsAxial)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    hittomcAxial.add(relation.getFrom(), relation.getTo());
        }
        //  Instantiate the class that determines if a track is "findable"
        FindableTrack findable = new FindableTrack(event);

        //  Create a map between tracks and the associated MCParticle
        List<Track> tracklist = event.get(Track.class, "MatchedTracks");
        RelationalTable trktomcAxial = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);

        if (event.hasCollection(Track.class, "AxialTracks")) {
            List<Track> axialtracklist = event.get(Track.class, "AxialTracks");
            aida.cloud1D("Axial Tracks per Event").fill(axialtracklist.size());
            String atrackdir = "TrackInfoAxial/";
            for (Track atrack : axialtracklist) {
                double apx = atrack.getPX();
                aida.cloud1D(atrackdir + "pX").fill(apx);
                TrackAnalysis tkanal = new TrackAnalysis(atrack, hittomcAxial);
                MCParticle mcp = tkanal.getMCParticle();
                if (mcp != null)
                    //  Create a map between the tracks found and the assigned MC particle
                    trktomcAxial.add(atrack, tkanal.getMCParticle());
            }

        }
        aida.cloud1D("Matched Tracks per Event").fill(tracklist.size());

        aida.cloud1D("HelicalTrackHits per Event").fill(toththits.size());
        RelationalTable trktomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);

        int _nchRec = 0;
        int _neleRec = 0;
        int _nposRec = 0;
        int _neleTru = 0;
        int _nposTru = 0;
        int _neleFake = 0;
        int _nposFake = 0;

        RelationalTable mcHittomcP = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);

        //  Get the collections of SimTrackerHits
        List<List<SimTrackerHit>> simcols = event.get(SimTrackerHit.class);

        //  Loop over the SimTrackerHits and fill in the relational table
        for (List<SimTrackerHit> simlist : simcols)
            for (SimTrackerHit simhit : simlist)
                if (simhit.getMCParticle() != null)
                    mcHittomcP.add(simhit, simhit.getMCParticle());

        Map<Track, TrackAnalysis> tkanalMap = new HashMap<Track, TrackAnalysis>();
        RelationalTable nearestHitToTrack = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        Map<Track, Double> l1Isolation = new HashMap<Track, Double>();
        Map<Track, Double> l1DeltaZ = new HashMap<Track, Double>();
        Map<Track, BilliorTrack> btMap = new HashMap<Track, BilliorTrack>();

        //  Analyze the tracks in the event

        String trackdir = "TrackInfo/";
        //  Analyze the tracks in the event
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
            _nchRec++;
            if (track.getCharge() < 0)
                _neleRec++;
            if (track.getCharge() > 0)
                _nposRec++;

            SeedTrack stEle = (SeedTrack) track;
            SeedCandidate seedEle = stEle.getSeedCandidate();
            HelicalTrackFit ht = seedEle.getHelix();
            TrackAnalysis tkanal = new TrackAnalysis(track, hittomc);

            tkanalMap.put(track, tkanal);
            BilliorTrack bt = new BilliorTrack(ht);

            btMap.put(track, bt);
            double xoca = ht.x0();
            double yoca = ht.y0();
            double[] poca = {xoca, yoca, z0};
            double mom[] = track.getMomentum();
            double px = mom[0];
            double py = mom[1];
            double pz = mom[2];
            double pperp = Math.sqrt(py * py + pz * pz);
            double pt = Math.sqrt(px * px + py * py);
            double p = Math.sqrt(pt * pt + pz * pz);
            double phi = Math.atan2(py, px);
            double cth = pz / Math.sqrt(pt * pt + pz * pz);
            double sth = pt / Math.sqrt(pt * pt + pz * pz);
            double th = Math.atan2(pt, pz);
            double eta = -Math.log(Math.tan(th / 2));



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
            Integer badLayerEle = encodeBadHitList(badLayers);
            if (badLayers.size() > 0) {
                System.out.println(badLayers.toString());
                System.out.println("Bad Layer code:  " + badLayerEle);
            }
            aida.cloud1D(trackdir + "Mis-matched hits for all tracks").fill(nbad);
            aida.cloud1D(trackdir + "purityNew for all tracks").fill(purity);
            aida.cloud1D(trackdir + "Bad Axial hits for all tracks").fill(nbadAxial);
            aida.cloud1D(trackdir + "Bad Z hits for all tracks").fill(nbadZ);
            aida.cloud1D(trackdir + "Number of Axial hits for all tracks").fill(nAxial);
            aida.cloud1D(trackdir + "Number of Z hits for all tracks").fill(nZ);

            for (Integer bhit : badLayers)
                aida.histogram1D(trackdir + "Layer of Bad Hit", nlayers[0], 1, nlayers[0] + 1).fill(bhit);

            //  Generate a normalized histogram after 1000 events
            trk_count++;
            if (nevt <= 1000)
                fakes.fill(nbad);

            //  Make plots for fake, non-fake, and all tracks
            if (purity < 0.5) {
                if (track.getCharge() < 0)
                    _neleFake++;
                if (track.getCharge() > 0)
                    _nposFake++;
                cthfake.fill(cth, 1.0);
                phifake.fill(phi, 1.0);
                pfake.fill(p, 1.0);

                fillTrackInfo(trackdir, "fake tracks", track.getChi2(), nhits, p, pperp, px, py, pz, phi, cth, d0, xoca, yoca, z0);

            } else {
                if (track.getCharge() < 0)
                    _neleTru++;
                if (track.getCharge() > 0)
                    _nposTru++;
                cthfake.fill(cth, 0.0);
                phifake.fill(phi, 0.0);
                pfake.fill(p, 0.0);

                fillTrackInfo(trackdir, "non-fake tracks", track.getChi2(), nhits, p, pperp, px, py, pz, phi, cth, d0, xoca, yoca, z0);



            }
            fillTrackInfo(trackdir, "all tracks", track.getChi2(), nhits, p, pperp, px, py, pz, phi, cth, d0, xoca, yoca, z0);
            if (nbadZ == 3)
                fillTrackInfo(trackdir, "3 Bad Z-hits", track.getChi2(), nhits, p, pperp, px, py, pz, phi, cth, d0, xoca, yoca, z0);


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




                BasicHep3Vector axial = new BasicHep3Vector();
//                axial.setV(0, 1, 0);
                axial.setV(1, 0, 0);
                String hitdir = "HitsOnTrack/";
                List<TrackerHit> hitsOnTrack = track.getTrackerHits();
                MCParticle bestmcp = tkanal.getMCParticleNew();

                String tkresid = "TrackResiduals/";

                int ndaug = 0;
                if (bestmcp != null)
                    ndaug = bestmcp.getDaughters().size();

                double mcmom = 0;
                double prevmom = 0;
                double mytotchi2 = 0;

                for (TrackerHit hit : hitsOnTrack) {

                    int iplane = 0;
                    HelicalTrackHit htc = (HelicalTrackHit) hit;
                    List<MCParticle> mcpsHTH = htc.getMCParticles();
                    int isbad = 0;
                    if (mcpsHTH.isEmpty() || mcpsHTH.size() > 1 || !mcpsHTH.contains(bestmcp))
                        isbad = 1;
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

                    mytotchi2 += Math.pow((y - yTr) / yerr, 2);
                    mytotchi2 += Math.pow((z - zTr) / zerr, 2);

                    int htlayer = htc.Layer();
                    if (htlayer == 1)
                        l1DeltaZ.put(track, z - zTr);

                    if (purity == 1 && track.getCharge() > 0 && nhits == 10) {
                        if (clusterlist.get(0).rawhits().size() == 1 && clusterlist.get(1).rawhits().size() == 1) {
                            aida.cloud1D(hitdir + tkresid + "SingleStrip--Track delta y:  Layer " + htlayer).fill(y - yTr);
                            aida.cloud1D(hitdir + tkresid + "SingleStrip--Track delta z:  Layer " + htlayer).fill(z - zTr);
                        }
                        aida.cloud1D(hitdir + tkresid + " Measured y:  Layer " + htlayer).fill(y);
                        aida.cloud1D(hitdir + tkresid + " Track y:  Layer " + htlayer).fill(yTr);
                        aida.cloud1D(hitdir + tkresid + " Measured z:  Layer " + htlayer).fill(z);
                        aida.cloud1D(hitdir + tkresid + " Track z:  Layer " + htlayer).fill(zTr);
                        aida.cloud1D(hitdir + tkresid + " Measured y ").fill(y);
                        aida.cloud1D(hitdir + tkresid + " Track delta y:  Layer " + htlayer + "; ndaug=" + ndaug).fill(y - yTr);
                        aida.cloud1D(hitdir + tkresid + " Track delta z:  Layer " + htlayer + "; ndaug=" + ndaug).fill(z - zTr);
                        aida.cloud2D(hitdir + tkresid + " Track deltay vs delta z:  Layer " + htlayer).fill(z - zTr, y - yTr);
                        if (htlayer == 1) {
                            aida.cloud2D(hitdir + tkresid + " Layer 1 deltay vs xoca").fill(poca[0], y - yTr);
                            aida.cloud2D(hitdir + tkresid + " Layer 1 deltay vs yoca").fill(poca[1], y - yTr);
                            aida.cloud2D(hitdir + tkresid + " Layer 1 deltay vs zoca").fill(poca[2], y - yTr);
                            aida.cloud2D(hitdir + tkresid + " Layer 1 deltaz vs xoca").fill(poca[0], z - zTr);
                            aida.cloud2D(hitdir + tkresid + " Layer 1 deltaz vs yoca").fill(poca[1], z - zTr);
                            aida.cloud2D(hitdir + tkresid + " Layer 1 deltaz vs zoca").fill(poca[2], z - zTr);
                        }
                        aida.cloud2D(hitdir + tkresid + " Track vs measured y:  Layer " + htlayer).fill(y, yTr);
                        aida.cloud2D(hitdir + tkresid + " Track vs measured z:  Layer " + htlayer).fill(z, zTr);
                        aida.cloud2D(hitdir + tkresid + " Track deltay vs S ").fill(sHit, y - yTr);
                        aida.cloud2D(hitdir + tkresid + " Track deltaz vs S ").fill(sHit, z - zTr);
                        aida.histogram1D(hitdir + tkresid + " Track pull y:  Layer " + htlayer, 200, -8, 8).fill((y - yTr) / yerr);
                        aida.histogram1D(hitdir + tkresid + " Track pull z:  Layer " + htlayer, 200, -8, 8).fill((z - zTr) / zerr);
                        aida.histogram2D(hitdir + tkresid + " Track pull y vs p:  Layer " + htlayer, 200, -8, 8, 200, 0, 5).fill((y - yTr) / yerr, pmc);
                        aida.histogram2D(hitdir + tkresid + " Track pull z vs p:  Layer " + htlayer, 200, -8, 8, 200, 0, 5).fill((z - zTr) / zerr, pmc);


                    }
                    for (HelicalTrackStrip cl : clusterlist) {
                        int ilayer = 0;
                        List<MCParticle> mcps = cl.MCParticles();

                        Hep3Vector corigin = cl.origin();
                        Hep3Vector u = cl.u();
                        double umeas = cl.umeas();
                        Hep3Vector uvec = VecOp.mult(umeas, u);
                        Hep3Vector clvec = VecOp.add(corigin, uvec);
                        int layer = cl.layer();
                        HelicalTrackStrip nearest = getNearestHit(cl, toththits);
                        if (layer == 1) {
                            Double l1Dist = getNearestDistance(cl, toththits);
                            if (l1Dist != null)
                                l1Isolation.put(track, l1Dist);
                        }
                        if (nearest != null)
                            nearestHitToTrack.add(track, nearest);

                        int badCl = 0;
                        if (mcps.isEmpty() || mcps.size() > 1 || !mcps.contains(bestmcp))
                            badCl = 1;
                        if (badCl == 1)
                            if (mcps.size() > 0 && mcps.get(0) != null) {
                                MCParticle tmpmc = mcps.get(0);
                                aida.cloud1D(hitdir + layer + " Momentum of bad hit ").fill(tmpmc.getMomentum().magnitude());
                                aida.cloud1D(hitdir + layer + " PDGID of bad hit ").fill(tmpmc.getPDGID());
                                for (MCParticle mymc : tmpmc.getParents())
                                    aida.cloud1D(hitdir + layer + " PDGID of bad hit mother ").fill(mymc.getPDGID());
                            }
                        String label = "False hit";
                        if (badCl == 0)
                            label = "True Hit ";

                        SimTrackerHit mcbesthit;
                        Set<SimTrackerHit> mchitlist = mcHittomcP.allTo(bestmcp);

                        double ymc = 0, zmc = 0;
                        for (SimTrackerHit sthbest : mchitlist) {
                            int slayer = sthbest.getLayer();
                            if (layer == slayer) {
                                mcbesthit = sthbest;
                                ymc = mcbesthit.getPoint()[1];
                                zmc = mcbesthit.getPoint()[2];
                                mcmom = getMag(mcbesthit.getMomentum());
                                if (prevmom > 0 && badCl == 0) {
                                    aida.histogram1D(hitdir + layer + " MC energy difference ", 100, -0.005, 0.0).fill(mcmom - prevmom);
                                    aida.histogram1D(hitdir + " MC energy difference ", 100, -0.005, 0.0).fill(mcmom - prevmom);
                                }
                                prevmom = mcmom;

                            }
                        }

                        double axdotu = VecOp.dot(cl.u(), axial);
                        boolean isAxial = false;
                        if (axdotu > 0.5)
                            isAxial = true;
//                    aida.cloud2D(hitdir + layer + " y vs z " + label).fill(z, y);
                        if (isAxial) {
                            aida.cloud1D(hitdir + layer + " y " + label).fill(clvec.y());
                            aida.cloud1D(hitdir + layer + " deltay " + label).fill(clvec.y() - ymc);
                            aida.cloud2D(hitdir + layer + " y vs yMC " + label).fill(ymc, clvec.y());
                        } else {
                            aida.cloud1D(hitdir + layer + " z " + label).fill(clvec.z());
                            aida.cloud1D(hitdir + layer + " deltaz " + label).fill(clvec.z() - zmc);
                            aida.cloud2D(hitdir + layer + " z vs zMC " + label).fill(zmc, clvec.z());
                        }
                        Set<MCParticle> mclist = hittomc.allFrom(hit);
                        aida.cloud1D(hitdir + layer + " Associated MC particles").fill(mclist.size());


                    }
                }
                aida.histogram2D(hitdir + "trkChi2 vs my chi2", 100, 0, 100, 100, 0, 100).fill(track.getChi2(), mytotchi2);

            }
        }

        //  Make the normalized fake plot after the specified number of events
        if (nevt == 1000) {
            double wgt = 1. / trk_count;
            for (int i = 0; i < 10; i++) {
                System.out.println(" Entries: " + fakes.binEntries(i) + " for mismatches: " + i);
                for (int j = 0; j < fakes.binHeight(i); j++)
                    nfakes.fill(i, wgt);
            }
            System.out.println("Normalization: " + nfakes.sumAllBinHeights() + " after ntrk = " + trk_count);
        }


        for (HelicalTrackHit hit : toththits) {

            int nAssHits = hit.getRawHits().size();
            aida.cloud1D(debugDir + hit.Detector() + " nAssHits").fill(nAssHits);
            Hep3Vector HTHPos = hit.getCorrectedPosition();
            double rHit = Math.sqrt(HTHPos.x() * HTHPos.x() + HTHPos.y() * HTHPos.y());
            double zHit = HTHPos.z();
            double etaHit = -Math.log(Math.tan(Math.atan2(rHit, zHit) / 2));
            double hitchisq = hit.chisq();

            if (hit instanceof HelicalTrackCross) {
                HelicalTrackCross cross = (HelicalTrackCross) hit;
                List<HelicalTrackStrip> clusterlist = cross.getStrips();
                double du_stereo = 0;
                double du_axial = 0;
                for (HelicalTrackStrip cluster : clusterlist) {

                    int nstrips = cluster.rawhits().size();
                    aida.cloud1D(debugDir + hit.Detector() + " nStrips-per-layer").fill(nstrips);
                    Hep3Vector corigin = cluster.origin();
                    Hep3Vector u = cluster.u();
                    List<RawTrackerHit> rawhits = cluster.rawhits();
                    double umc = -999999;
                    double stenergy = -999999;
                    String stripdir = "axial";
                    double umeas = cluster.umeas();
                    double charge = cluster.dEdx() * 1000.0;
                    for (RawTrackerHit rhit : rawhits) {

                        String deName = rhit.getDetectorElement().getName();
                        if (deName.contains("sensor1"))
                            stripdir = "stereo";
                        //                           System.out.println("Layer number  " + rhit.getLayerNumber() + "  " + deName);
                        List<SimTrackerHit> sthits = rhit.getSimTrackerHits();
                        int nsthits = sthits.size();
                        aida.cloud1D(debugDir + hit.Detector() + " associated ST hits").fill(nsthits);
                        aida.cloud1D(debugDir + hit.Detector() + " layer" + stripdir + " associated ST hits").fill(nsthits);
                        if (nsthits == 1) {
                            double[] sthitD = sthits.get(0).getPoint();
                            BasicHep3Vector sthit = new BasicHep3Vector(sthitD);
                            stenergy = sthits.get(0).getdEdx();
                            Hep3Vector vdiff = VecOp.sub(sthit, corigin);
                            umc = VecOp.dot(vdiff, u);
                        }
                    }


                    //                        System.out.println("filling...");
                    if (umc != -999999) {
                        aida.histogram1D(debugDir + hit.Detector() + "dedx", 50, 0, 0.3).fill(charge);
                        if (umc < 1 && umc > -1)
                            aida.cloud2D(debugDir + hit.Detector() + "cluster reco vs cluster mc").fill(umeas - umc, umc);
                        aida.cloud2D(debugDir + hit.Detector() + "cluster vs STHit dedx").fill(stenergy, charge);
                        aida.cloud2D(debugDir + hit.Detector() + "cluster dedx vs delte(u)").fill(umeas - umc, charge);
                        if (stripdir.contains("stereo"))
                            du_stereo = umeas - umc;
                        if (stripdir.contains("axial"))
                            du_axial = umeas - umc;
                        aida.cloud1D(debugDir + hit.Detector() + "layer=" + stripdir + " delta(u)").fill(umeas - umc);
                        aida.cloud1D(debugDir + hit.Detector() + " delta(u)").fill(umeas - umc);
                        if (nstrips == 1) {
                            aida.cloud1D(debugDir + hit.Detector() + "layer=" + stripdir + " delta(u)--1 strip").fill(umeas - umc);
                            aida.cloud1D(debugDir + hit.Detector() + " delta(u)--1 strip").fill(umeas - umc);
                        }
                        if (nstrips == 2) {
                            aida.cloud1D(debugDir + hit.Detector() + "layer=" + stripdir + " delta(u)--2 strip").fill(umeas - umc);
                            aida.cloud1D(debugDir + hit.Detector() + " delta(u)--2 strip").fill(umeas - umc);
                        }
                        if (nstrips == 3) {
                            aida.cloud1D(debugDir + hit.Detector() + "layer=" + stripdir + " delta(u)--3 strip").fill(umeas - umc);
                            aida.cloud1D(debugDir + hit.Detector() + " delta(u)--3 strip").fill(umeas - umc);
                        }
                    }

                }
                aida.cloud2D(debugDir + hit.Detector() + " delta(u) stereo v axial").fill(du_stereo, du_axial);
            }
        }

        //analyze the event
        int ApCand = 0;
        String apdir = "Aprime/";
        Track eleID = null;
        Track posID = null;
        MCParticle eleMC = null;
        MCParticle posMC = null;
        for (Track track : tracklist) {

            TrackAnalysis tkanal = tkanalMap.get(track);
            //  Calculate purity and make appropriate plots
            MCParticle mcp = tkanal.getMCParticle();
            if (mcp == null)
                continue;
            if (mcp.getParents().size() == 1 && mcp.getParents().get(0).getPDGID() == 622) {
                int nhits = tkanal.getNHitsNew();
                double px = track.getPX();
                double py = track.getPY();
                double pz = track.getPZ();
                double pt = Math.sqrt(px * px + py * py);
                double pperp = Math.sqrt(py * py + pz * pz);
                double p = Math.sqrt(pt * pt + pz * pz);
                double phi = Math.atan2(py, px);
                double cth = pz / Math.sqrt(pt * pt + pz * pz);

                SeedTrack stEle = (SeedTrack) track;
                SeedCandidate seedEle = stEle.getSeedCandidate();
                HelicalTrackFit ht = seedEle.getHelix();
                double doca = ht.dca();
                double[] poca = {ht.x0(), ht.y0(), ht.z0()};
                if (mcp.getCharge() > 0) {
                    posID = track;
                    posMC = mcp;
                    fillTrackInfo(apdir, "positron", track.getChi2(), nhits, p, pperp, px, py, pz, phi, cth, doca, poca[0], poca[1], poca[2]);

                } else {
                    eleID = track;
                    eleMC = mcp;
                    fillTrackInfo(apdir, "electron", track.getChi2(), nhits, p, pperp, px, py, pz, phi, cth, doca, poca[0], poca[1], poca[2]);
                }
            }

        }
        String vertex = "Vertexing/";
        String selected = "Selection/";
        String nhitsTotal = "NumberOfHits/";
        List<BilliorTrack> btlist = new ArrayList<BilliorTrack>();
        for (Track track1 : tracklist) {
            Track ele = null;
            Track pos = null;
            int ch1 = track1.getCharge();
            int index = tracklist.indexOf(track1);
            List<Track> subtracklist = tracklist.subList(index, tracklist.size());
            for (Track track2 : subtracklist) {
                int ch2 = track2.getCharge();
                if (track1 != track2 && ch1 == -ch2) {
                    ele = track1;
                    pos = track2;
//                    System.out.println("Found two oppositely charged tracks!  Lets look at them");
                    if (ch1 > 0) {
                        ele = track2;
                        pos = track1;
                    }
                    ApCand++;
                    int nElectron = ele.getTrackerHits().size();
                    int nPositron = pos.getTrackerHits().size();
                    BilliorTrack btEle = btMap.get(ele);
                    BilliorTrack btPos = btMap.get(pos);
                    btlist.clear();
                    btlist.add(btEle);
                    btlist.add(btPos);

                    /*


                     BilliorVertex bvertexUC = new BilliorVertex(bfield);
                     bvertexUC.doBeamSpotConstraint(false);
                     bvertexUC.tryNewFormalism(btlist);

                     BasicMatrix bvtxPosUC = (BasicMatrix)
                     bvertexUC.getVertexPosition(); BasicMatrix bvtxCovUC =
                     (BasicMatrix) bvertexUC.getVertexCovariance(); double
                     invMass = bvertexUC.getInvMass();

                     aida.histogram1D(vertex + "BilliorVertex X --
                     UnConstrained", 100, -10, 20).fill(bvtxPosUC.e(0, 0));
                     aida.histogram1D(vertex + "BilliorVertex Y --
                     UnConstrained", 100, -0.4, 0.4).fill(bvtxPosUC.e(1, 0));
                     aida.histogram1D(vertex + "BilliorVertex Z --
                     UnConstrained", 100, -0.4, 0.4).fill(bvtxPosUC.e(2, 0));
                     aida.histogram1D(vertex + "BilliorVertex ChiSq --
                     UnConstrained", 100, 0, 50).fill(bvertexUC.getChiSq());
                     aida.histogram1D(vertex + "BilliorVertex X Pull --
                     UnConstrained", 100, -4, 4).fill(bvtxPosUC.e(0, 0) /
                     Math.sqrt(bvtxCovUC.e(0, 0))); aida.histogram1D(vertex +
                     "BilliorVertex Y Pull-- UnConstrained", 100, -4,
                     4).fill(bvtxPosUC.e(1, 0) / Math.sqrt(bvtxCovUC.e(1, 1)));
                     aida.histogram1D(vertex + "BilliorVertex Z Pull--
                     UnConstrained", 100, -4, 4).fill(bvtxPosUC.e(2, 0) /
                     Math.sqrt(bvtxCovUC.e(2, 2))); aida.histogram1D(vertex +
                     "BilliorVertex Mass -- UnConstrained", 250, 0.0,
                     0.25).fill(bvertexUC.getInvMass());


                     aida.cloud1D(apdir + "e+e- Invariant Mass").fill(invMass);
                     if (eleMC != null && posMC != null && ele == eleID && pos
                     == posID) aida.cloud1D(apdir + "Matched A' Invariant
                     Mass").fill(invMass);


                     BilliorVertex bvertex = new BilliorVertex(bfield);
                     bvertex.doBeamSpotConstraint(true);
                     bvertex.tryNewFormalism(btlist); BasicMatrix bvtxPos =
                     (BasicMatrix) bvertex.getVertexPosition(); BasicMatrix
                     bvtxCov = (BasicMatrix) bvertex.getVertexCovariance();

                     aida.histogram1D(vertex + "BilliorVertex X -- Constrained",
                     100, -10, 20).fill(bvtxPos.e(0, 0));
                     aida.histogram1D(vertex + "BilliorVertex Y -- Constrained",
                     100, -0.4, 0.4).fill(bvtxPos.e(1, 0));
                     aida.histogram1D(vertex + "BilliorVertex Z -- Constrained",
                     100, -0.4, 0.4).fill(bvtxPos.e(2, 0));
                     aida.histogram1D(vertex + "BilliorVertex ChiSq --
                     Constrained", 100, -10, 50).fill(bvertex.getChiSq());
                     aida.histogram1D(vertex + "BilliorVertex X Pull --
                     Constrained", 100, -4, 4).fill(bvtxPos.e(0, 0) /
                     Math.sqrt(bvtxCov.e(0, 0))); aida.histogram1D(vertex +
                     "BilliorVertex Y Pull-- Constrained", 100, -4,
                     4).fill(bvtxPos.e(1, 0) / Math.sqrt(bvtxCov.e(1, 1)));
                     aida.histogram1D(vertex + "BilliorVertex Z Pull--
                     Constrained", 100, -4, 4).fill(bvtxPos.e(2, 0) /
                     Math.sqrt(bvtxCov.e(2, 2)));

                     aida.histogram1D(vertex + "BilliorVertex Mass --
                     Constrained", 250, 0.0, 0.25).fill(bvertex.getInvMass());



                     BilliorVertex bsconfit = new BilliorVertex(bfield);
                     bsconfit.setBeamSize(beamsize);
                     bsconfit.doBeamSpotConstraint(false);
                     bsconfit.constrainV0toBeamSpot(true);
                     bsconfit.tryNewFormalism(btlist); BasicMatrix bsconvtxPos =
                     (BasicMatrix) bsconfit.getVertexPosition(); BasicMatrix
                     bsconvtxCov = (BasicMatrix) bsconfit.getVertexCovariance();

                     aida.histogram1D(vertex + "BilliorVertex X -- BS
                     Constrained", 100, -10, 20).fill(bsconvtxPos.e(0, 0));
                     aida.histogram1D(vertex + "BilliorVertex Y -- BS
                     Constrained", 100, -0.4, 0.4).fill(bsconvtxPos.e(1, 0));
                     aida.histogram1D(vertex + "BilliorVertex Z -- BS
                     Constrained", 100, -0.4, 0.4).fill(bsconvtxPos.e(2, 0));
                     aida.histogram1D(vertex + "BilliorVertex ChiSq -- BS
                     Constrained", 100, -10, 50).fill(bsconfit.getChiSq());
                     aida.histogram1D(vertex + "BilliorVertex X Pull -- BS
                     Constrained", 100, -4, 4).fill(bsconvtxPos.e(0, 0) /
                     Math.sqrt(bvtxCov.e(0, 0))); aida.histogram1D(vertex +
                     "BilliorVertex Y Pull-- BS Constrained", 100, -4,
                     4).fill(bsconvtxPos.e(1, 0) / Math.sqrt(bvtxCov.e(1, 1)));
                     aida.histogram1D(vertex + "BilliorVertex Z Pull-- BS
                     Constrained", 100, -4, 4).fill(bsconvtxPos.e(2, 0) /
                     Math.sqrt(bvtxCov.e(2, 2)));

                     aida.histogram1D(vertex + "BilliorVertex Mass -- BS
                     Constrained", 100, 0.08, 0.12).fill(bsconfit.getInvMass());
                     */
                }
            }
        }

        aida.cloud1D(apdir + "Number of Aprime candidates found").fill(ApCand);
        aida.cloud1D(apdir + "Number of negative candidates found").fill(_neleRec);
        aida.cloud1D(apdir + "Number of true electrons").fill(_neleTru);
        aida.cloud1D(apdir + "Number of fake electrons").fill(_neleFake);
        aida.cloud1D(apdir + "Number of positive candidates found").fill(_nposRec);
        aida.cloud1D(apdir + "Number of true positrons").fill(_nposTru);
        aida.cloud1D(apdir + "Number of fake positrons").fill(_nposFake);

        //  Now loop over all MC Particles
        List<MCParticle> mclist = event.getMCParticles();
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
            //  Find the number of layers hit by this mc particle
//            System.out.println("MC pt=" + pt);
            int nhits = findable.LayersHit(mcp);
            boolean isFindable = findable.InnerTrackerIsFindable(mcp, nlayers[0] - 2);
            Set<SimTrackerHit> mchitlist = mcHittomcP.allTo(mcp);

            Set<HelicalTrackCross> hitlist = hittomc.allTo(mcp);

            //  Calculate the helix parameters for this MC particle
            HelixParamCalculator helix = new HelixParamCalculator(mcp, bfield);
            double d0 = helix.getDCA();
            double z0 = helix.getZ0();


            //  Check cases where we have multiple tracks associated with this MC particle
            Set<Track> trklist = trktomc.allTo(mcp);
            int ntrk = trklist.size();
            int ntrkAxial = 0;
            if (event.hasCollection(Track.class, "AxialTracks")) {
                Set<Track> trklistAxial = trktomcAxial.allTo(mcp);
                ntrkAxial = trklistAxial.size();
            }

            if (mcp.getPDGID() == 622) {
                boolean bothreco = true;
                boolean bothfindable = true;
                //it's the A'...let's see if we found both tracks.
                List<MCParticle> daughters = mcp.getDaughters();
                for (MCParticle d : daughters) {
                    if (trktomc.allTo(d).isEmpty())
                        bothreco = false;
//                    if (findable.LayersHit(d) != nlayers[0])
                    if (!findable.InnerTrackerIsFindable(d, nlayers[0] - 2))
                        bothfindable = false;
                }
                double vtxWgt = 0;
                if (bothreco)
                    vtxWgt = 1.0;
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
                    if (tkanal.getNBadHits() < tkanal.getNHits() - 1)
                        nmulthits++;
                }
                //  Flag any anomalous cases that we find
                if (nmulthits > 1)
                    System.out.println("2 tracks associated with a single MC Particle");
            }

//            if (nhits == nlayers[0]) {
            if (isFindable) {
                _nchMCP++;
                findableTracks++;
                double wgt = 0.;
                if (ntrk > 0)
                    wgt = 1.;
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

                if (ntrkAxial > 0)
                    wgtAxial = 1.;
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
                    if (ntrk > 0)
                        wgt = 1.;
                    foundelectrons += wgt;
                    peffElectrons.fill(p, wgt);
                    phieffElectrons.fill(phi, wgt);
                    thetaeffElectrons.fill(theta, wgt);
                    ctheffElectrons.fill(cth, wgt);
                    d0effElectrons.fill(d0, wgt);
                    z0effElectrons.fill(z0, wgt);
//                    if (wgt == 0)
//                        System.out.println("Missed a findable ELECTRON!!!!!");


                    double wgtAxial = 0.;
                    if (ntrkAxial > 0)
                        wgtAxial = 1.;
                    peffAxial.fill(p, wgtAxial);
                    phieffAxial.fill(phi, wgtAxial);
                    thetaeffAxial.fill(theta, wgtAxial);
                    ctheffAxial.fill(cth, wgtAxial);
                    d0effAxial.fill(d0, wgtAxial);
                    z0effAxial.fill(z0, wgtAxial);

                }


            }

            if (mcp.getGeneratorStatus() == mcp.FINAL_STATE && mcp.getCharge() != 0)
                if (Math.abs(eta) < 6)
                    aida.cloud1D("findable/eta for final state particles").fill(eta);
            if (mcp.getGeneratorStatus() != mcp.FINAL_STATE && mcp.getGeneratorStatus() != mcp.INTERMEDIATE)
                if (Math.abs(eta) < 6)
                    aida.cloud1D("findable/eta for other particles").fill(eta);
            //  Select mcp that fail the final state requirement
            if (mcp.getGeneratorStatus() != mcp.FINAL_STATE) {
                aida.cloud1D("findable/Hits for non-final state particles").fill(nhits);
                aida.cloud1D("findable/pT for non-final state particles").fill(pt);
                aida.cloud1D("findable/cos(theta) for non-final state particles").fill(cth);
                if (Math.abs(eta) < 6)
                    aida.cloud1D("findable/eta for non-final state particles").fill(eta);
                aida.cloud1D("findable/d0 for non-final state particles").fill(d0);
                aida.cloud1D("findable/z0 for non-final state particles").fill(z0);
                aida.cloud2D("findable/Hits vs eta for non-final state particles").fill(eta, nhits);
                double zOrig = mcp.getOriginZ();
                double xOrig = mcp.getOriginX();
                double yOrig = mcp.getOriginY();
                double rOrig = Math.sqrt(mcp.getOriginX() * mcp.getOriginX() + mcp.getOriginY() * mcp.getOriginY());
                int mcid = mcp.getPDGID();

                if (Math.abs(mcid) != 310 && Math.abs(mcid) != 3122) {
                    aida.histogram2D("x vs y for non-final state particles", 1505, -5, 1500, 400, -200, 200).fill(xOrig, yOrig);
                    aida.histogram2D("x vs z for non-final state particles", 1505, -5, 1500, 400, -200, 200).fill(xOrig, zOrig);
                }
                continue;
            }

            //  Make plots for the base sample
            aida.cloud1D("findable/Hits for base MC selection").fill(nhits);
            aida.cloud1D("findable/pT for base MC selection").fill(pt);
            aida.cloud1D("findable/cos(theta) for base MC selection").fill(cth);
            if (Math.abs(eta) < 6)
                aida.cloud1D("findable/eta for base MC selection").fill(eta);
            aida.cloud1D("findable/d0 for base MC selection").fill(d0);
            aida.cloud1D("findable/z0 for base MC selection").fill(z0);
            if (Math.abs(eta) < 6)
                aida.cloud2D("findable/Hits vs eta for base MC selection").fill(eta, nhits);

        }

        aida.cloud1D("number of reconstructed tracks per Event").fill(_nchRec);
        aida.cloud1D(
                "number of generated charged tracks per Event").fill(_nchMCP);
        aida.cloud2D(
                "reco tracks vs MC tracks").fill(_nchMCP, _nchRec);




        return;
    }

    @Override
    public void endOfData() {
        try {
            aida.saveAs(outputPlots);
        } catch (IOException ex) {
            Logger.getLogger(DetailedAnalysisDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        pw.close();
        try {
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(DetailedAnalysisDriver.class.getName()).log(Level.SEVERE, null, ex);
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
        if (phi < 0.)
            phi += 2. * Math.PI;
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
        if (dydx1 < 0)
            truep1y = -truep1y;
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
        double p1x = track1.getPX();
        double p1y = track1.getPY();
        double p1z = track1.getPZ();
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
                        nearest = str;
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
                        nearest = str;
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
}
