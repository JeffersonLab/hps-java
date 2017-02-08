package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IProfile1D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.hps.analysis.examples.TrackAnalysis;
import org.hps.recon.tracking.FindableTrack;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.geometry.Detector;

/**
 * DQM driver for the monte carlo track efficiency; makes a bunch of efficiency
 * vs variable plots for all tracks and just electrons from trident/A' event, as
 * well as "findable" tracks use the debugTrackEfficiency flag to print out info
 * regarding individual failed events.
 */
// TODO:  Add some quantities for DQM monitoring:  e.g. <efficiency>, <eff>_findable
public class TrackMCEfficiency extends DataQualityMonitor {

    private static Logger LOGGER = Logger.getLogger(TrackMCEfficiency.class.getPackage().getName());
    
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String trackHitCollectionName = "RotatedHelicalTrackHits";
    private String fittedSVTHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "TrackerHits";
    private String siClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String trackHitMCRelationsCollectionName = "RotatedHelicalTrackMCRelations";
    private String detectorFrameHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String trackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private String trackCollectionName = "MatchedTracks";
    private IProfile1D peffFindable;
    private IProfile1D phieffFindable;
    private IProfile1D ctheffFindable;
    private IProfile1D peffElectrons;
    private IProfile1D phieffElectrons;
    private IProfile1D ctheffElectrons;
    double beamP = 2.2;
    int nlayers = 12;
    int totelectrons = 0;
    double foundelectrons = 0;
    int findableelectrons = 0;
    int findableTracks = 0;
    double foundTracks = 0;
    private boolean debugTrackEfficiency = false;
    private String plotDir = "TrackMCEfficiency/";
    private String resDir = "TrackMCResolution/";
    private String misidDir = "TrackMCMisId/";

    public void setTrackHitCollectionName(String trackHitCollectionName) {
        this.trackHitCollectionName = trackHitCollectionName;
    }

    public void setTrackHitMCRelationsCollectionName(String trackHitMCRelationsCollectionName) {
        this.trackHitMCRelationsCollectionName = trackHitMCRelationsCollectionName;
    }

    public void setDetectorFrameHitRelationsCollectionName(String detectorFrameHitRelationsCollectionName) {
        this.detectorFrameHitRelationsCollectionName = detectorFrameHitRelationsCollectionName;
    }

    public void setTrackHitRelationsCollectionName(String trackHitRelationsCollectionName) {
        this.trackHitRelationsCollectionName = trackHitRelationsCollectionName;
    }

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    public void setDebugTrackEfficiency(boolean debug) {
        this.debugTrackEfficiency = debug;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().mkdir(plotDir);
        aida.tree().mkdir(resDir);
        aida.tree().mkdir(misidDir);
        aida.tree().cd("/");
        IHistogramFactory hf = aida.histogramFactory();

        peffFindable = hf.createProfile1D(plotDir + "Findable Efficiency vs p", "", 20, 0., beamP);
        phieffFindable = hf.createProfile1D(plotDir + "Findable Efficiency vs phi", "", 25, -0.25, 0.25);
        ctheffFindable = hf.createProfile1D(plotDir + "Findable Efficiency vs cos(theta)", "", 25, -0.25, 0.25);
        peffElectrons = hf.createProfile1D(plotDir + "Electrons Efficiency vs p", "", 20, 0., beamP);
        phieffElectrons = hf.createProfile1D(plotDir + "Electrons Efficiency vs phi", "", 25, -0.25, 0.25);
        ctheffElectrons = hf.createProfile1D(plotDir + "Electrons Efficiency vs cos(theta)", "", 25, -0.25, 0.25);

        IHistogram1D pMCRes = hf.createHistogram1D(resDir + "Momentum Resolution", 50, -0.5, 0.5);
        IHistogram1D phi0MCRes = hf.createHistogram1D(resDir + "phi0 Resolution", 50, -0.1, 0.1);
        IHistogram1D d0MCRes = hf.createHistogram1D(resDir + "d0 Resolution", 50, -0.5, 0.5);
        IHistogram1D z0MCRes = hf.createHistogram1D(resDir + "z0 Resolution", 50, -1.0, 1.0);
        IHistogram1D tanLambdaMCRes = hf.createHistogram1D(resDir + "tanLambda Resolution", 50, -0.1, 0.1);
    }

    @Override
    public void process(EventHeader event) {

        aida.tree().cd("/");

        //make sure the required collections exist
        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            if (debug)
                LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + rawTrackerHitCollectionName);
            return;
        }
        if (!event.hasCollection(LCRelation.class, fittedSVTHitCollectionName))
            if (debug)
                LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + fittedSVTHitCollectionName); //mg...2/1/2015...don't return if the fitted collection isn't there...
        //allow us to run if we simulated in "simple" mode (i.e. no time evolution)
        //            return;
        if (!event.hasCollection(Track.class, trackCollectionName)) {
            if (debug)
                LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + trackCollectionName);
            return;
        }
        if (!event.hasCollection(LCRelation.class, trackHitMCRelationsCollectionName)) {
            if (debug)
                LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + trackHitMCRelationsCollectionName);
            return;
        }
        if (!event.hasCollection(TrackerHit.class, siClusterCollectionName)) {
            if (debug)
                LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + siClusterCollectionName);
            return;
        }

        if (!event.hasCollection(SimTrackerHit.class, trackerHitCollectionName)) {
            if (debug)
                LOGGER.info(this.getClass().getSimpleName() + ": no collection found " + trackerHitCollectionName);
            return;
        }
        //
        //get the b-field
        Hep3Vector IP = new BasicHep3Vector(0., 0., 1.);
        double bfield = event.getDetector().getFieldMap().getField(IP).y();
        //make some maps and relation tables        
        Map<Track, TrackAnalysis> tkanalMap = new HashMap<Track, TrackAnalysis>();
        RelationalTable hittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> mcrelations = event.get(LCRelation.class, trackHitMCRelationsCollectionName);
        if (debugTrackEfficiency)
            LOGGER.info(this.getClass().getSimpleName() + ": number of MC relations = " + mcrelations.size());
        for (LCRelation relation : mcrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittomc.add(relation.getFrom(), relation.getTo());
        if (debugTrackEfficiency)
            LOGGER.info(this.getClass().getSimpleName() + ": number of hittomc relations = " + hittomc.size());
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
        // make relational table for strip clusters to mc particle
        List<TrackerHit> siClusters = event.get(TrackerHit.class, siClusterCollectionName);
        RelationalTable clustertosimhit = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        for (TrackerHit cluster : siClusters) {
            List<RawTrackerHit> rawHits = cluster.getRawHits();
            for (RawTrackerHit rth : rawHits) {
                Set<SimTrackerHit> simTrackerHits = rawtomc.allFrom(rth);
                if (simTrackerHits != null)
                    for (SimTrackerHit simhit : simTrackerHits)
                        clustertosimhit.add(cluster, simhit);
            }
        }
        //relational tables from mc particle to raw and fitted tracker hits
        RelationalTable fittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, fittedSVTHitCollectionName)) {
            List<LCRelation> fittedTrackerHits = event.get(LCRelation.class, fittedSVTHitCollectionName);
            for (LCRelation hit : fittedTrackerHits) {
                RawTrackerHit rth = FittedRawTrackerHit.getRawTrackerHit(hit);
                Set<SimTrackerHit> simTrackerHits = rawtomc.allFrom(rth);
                if (simTrackerHits != null)
                    for (SimTrackerHit simhit : simTrackerHits)
                        if (simhit.getMCParticle() != null)
                            fittomc.add(hit, simhit.getMCParticle());
            }
        }
        RelationalTable hittostrip = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> hitrelations = event.get(LCRelation.class, detectorFrameHitRelationsCollectionName);
        for (LCRelation relation : hitrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittostrip.add(relation.getFrom(), relation.getTo());

        RelationalTable hittorotated = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> rotaterelations = event.get(LCRelation.class, trackHitRelationsCollectionName);
        for (LCRelation relation : rotaterelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittorotated.add(relation.getFrom(), relation.getTo());

        //  Instantiate the class that determines if a track is "findable"
        FindableTrack findable = new FindableTrack(event);

        List<Track> tracks = event.get(Track.class, trackCollectionName);
        if (debugTrackEfficiency)
            LOGGER.info(this.getClass().getSimpleName() + ": nTracks = " + tracks.size());
        for (Track trk : tracks) {
            TrackAnalysis tkanal = new TrackAnalysis(trk, hittomc, rawtomc, hittostrip, hittorotated);
            tkanalMap.put(trk, tkanal);
            MCParticle mcp = tkanal.getMCParticleNew();
            if (mcp != null) {//  Create a map between the tracks found and the assigned MC particle            
                if (debugTrackEfficiency)
                    LOGGER.info(this.getClass().getSimpleName() + ": found MCP match");
                trktomc.add(trk, tkanal.getMCParticleNew());
            }
        }

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
            double cth = py / p;
            double theta = 180. * Math.acos(cth) / Math.PI;
            double eta = -Math.log(Math.tan(Math.atan2(pt, pz) / 2));
            double phi = Math.atan2(px, pz);
            //  Find the number of layers hit by this mc particle
            if (debugTrackEfficiency)
                LOGGER.info("MC pt=" + pt);
            int nhits = findable.LayersHit(mcp);
            boolean isFindable = findable.InnerTrackerIsFindable(mcp, nlayers - 2);
            if (debugTrackEfficiency)
                LOGGER.info("nhits Findable =" + nhits + "; is findable? " + isFindable);
            //  Calculate the helix parameters for this MC particle
            HelixParamCalculator helix = new HelixParamCalculator(mcp, bfield);
            double d0 = helix.getDCA();
            double z0 = helix.getZ0();

            //  Check cases where we have multiple tracks associated with this MC particle
            Set<Track> trklist = trktomc.allTo(mcp);
            int ntrk = trklist.size();

//            Set<Track> trklistAxial = trktomcAxial.allTo(mcp);
//            int ntrkAxial = trklistAxial.size();
            if (mcp.getPDGID() == 622) {
                boolean bothreco = true;
                boolean bothfindable = true;
                //it's the A'...let's see if we found both tracks.
                List<MCParticle> daughters = mcp.getDaughters();
                for (MCParticle d : daughters) {
                    if (trktomc.allTo(d).isEmpty())
                        bothreco = false;
                    if (!findable.InnerTrackerIsFindable(d, nlayers - 2))
                        bothfindable = false;
                }
                double vtxWgt = 0;
                if (bothreco)
                    vtxWgt = 1.0;
//                VxEff.fill(mcp.getOriginX(), vtxWgt);
//                VyEff.fill(mcp.getOriginY(), vtxWgt);
//                VzEff.fill(mcp.getOriginZ(), vtxWgt);
                if (bothfindable) {
//                    VxEffFindable.fill(mcp.getOriginX(), vtxWgt);
//                    VyEffFindable.fill(mcp.getOriginY(), vtxWgt);
//                    VzEffFindable.fill(mcp.getOriginZ(), vtxWgt);
                }
            }

//            if (nhits == nlayers[0]) {
            if (isFindable) {
                _nchMCP++;
                findableTracks++;
                double wgt = 0.;
                if (ntrk > 0)
                    wgt = 1.;
                foundTracks += wgt;
                if (debugTrackEfficiency)
                    LOGGER.info("...is findable; filling plots with weight " + wgt);
                peffFindable.fill(p, wgt);
                phieffFindable.fill(phi, wgt);
                ctheffFindable.fill(cth, wgt);

                if (wgt == 0) {
                    Set<SimTrackerHit> mchitlist = mcHittomcP.allTo(mcp);
                    Set<HelicalTrackCross> hitlist = hittomc.allTo(mcp);
                    if (debugTrackEfficiency)
                        if (fittomc != null) {
                            Set<FittedRawTrackerHit> fitlist = fittomc.allTo(mcp);
                            LOGGER.info(this.getClass().getSimpleName() + ":  Missed a findable track with MC p = " + p);
                            if (!hasHTHInEachLayer(hitlist, fitlist))
                                LOGGER.info("\t\tThis track failed becasue it's missing a helical track hit");
                        }
                }

            }
            if (debugTrackEfficiency){
                LOGGER.info("# of mc parents " + mcp.getParents().size());
                if(mcp.getParents().size() > 0 )
                    LOGGER.info("PDG ID of parent 0 is " + mcp.getParents().get(0).getPDGID());
            }
            if (mcp.getParents().size() == 1 && mcp.getParents().get(0).getPDGID() == 622) {
                totelectrons++;
//                    findableelectrons++;
                double wgt = 0.;
                if (ntrk > 0)
                    wgt = 1.;
                if (debugTrackEfficiency)
                    LOGGER.info("...is from A'; filling plots with weight " + wgt);
                foundelectrons += wgt;
                peffElectrons.fill(p, wgt);
                phieffElectrons.fill(phi, wgt);
                ctheffElectrons.fill(cth, wgt);
                if (ntrk == 1) {
                    Track trk = (Track) trklist.toArray()[0];
                    TrackState ts = trk.getTrackStates().get(0);
                    double deld0 = d0 - ts.getD0();
                    double delz0 = z0 - ts.getZ0();
                    double delp = p - calcMagnitude(ts.getMomentum());
                    double delphi0 = phi - ts.getPhi();
                    double deltanlam = cth - ts.getTanLambda();
                    aida.histogram1D(resDir + "Momentum Resolution").fill(delp);
                    aida.histogram1D(resDir + "z0 Resolution").fill(delz0);
                    aida.histogram1D(resDir + "d0 Resolution").fill(deld0);
                    aida.histogram1D(resDir + "phi0 Resolution").fill(delphi0);
                    aida.histogram1D(resDir + "tanLambda Resolution").fill(deltanlam);
                }

                //               }
            }
        }
    }

    @Override
    public void fillEndOfRunPlots() {
    }

    @Override
    public void dumpDQMData() {
    }

    private IProfile1D getLayerPlot(String prefix, int layer) {
        return aida.profile1D(prefix + "_layer" + layer);
    }

    private IProfile1D createLayerPlot(String prefix, int layer, int nchan, double min, double max) {
        IProfile1D hist = aida.profile1D(prefix + "_layer" + layer, nchan, min, max);
        return hist;
    }

    private boolean hasHTHInEachLayer(Set<HelicalTrackCross> list, Set<FittedRawTrackerHit> fitlist) {
        if (list.isEmpty())
            return false;
        if (!(list.toArray()[0] instanceof HelicalTrackCross))
            return false;
        for (int layer = 1; layer < nlayers - 2; layer += 2) {
            boolean hasThisLayer = false;
            for (HelicalTrackCross hit : list)
                if (hit.Layer() == layer)
                    hasThisLayer = true;
            if (!hasThisLayer) {
//                LOGGER.info("Missing reconstructed hit in layer = " + layer);
                boolean hasFitHitSL1 = false;
                boolean hasFitHitSL2 = false;
                FittedRawTrackerHit fitSL1 = null;
                FittedRawTrackerHit fitSL2 = null;
//                LOGGER.info("fitted hit list size = " + fitlist.size());
                for (FittedRawTrackerHit fit : fitlist) {
//                    LOGGER.info("fitted hit layer number = " + fit.getRawTrackerHit().getLayerNumber());
                    if (fit.getRawTrackerHit().getLayerNumber() == layer) {
                        hasFitHitSL1 = true;
                        fitSL1 = fit;
//                        LOGGER.info("Found a hit in SL1 with t0 = " + fitSL1.getT0() + "; amp = " + fitSL1.getAmp() + "; chi^2 = " + fitSL1.getShapeFitParameters().getChiProb() + "; strip = " + fitSL1.getRawTrackerHit().getCellID());
                    }
                    if (fit.getRawTrackerHit().getLayerNumber() == layer + 1) {
                        hasFitHitSL2 = true;
                        fitSL2 = fit;
//                        LOGGER.info("Found a hit in SL2 with t0 = " + fitSL2.getT0() + "; amp = " + fitSL2.getAmp() + "; chi^2 = " + fitSL2.getShapeFitParameters().getChiProb() + "; strip = " + fitSL2.getRawTrackerHit().getCellID());

                    }
                }
//                if (!hasFitHitSL1)
//                    LOGGER.info("MISSING a hit in SL1!!!");
//                if (!hasFitHitSL2)
//                    LOGGER.info("MISSING a hit in SL2!!!");

                return false;
            }
        }
        return true;
    }

    private double calcMagnitude(double[] vec) {
        return Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
    }

}
