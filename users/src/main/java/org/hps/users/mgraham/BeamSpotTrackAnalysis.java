package org.hps.users.mgraham;

import hep.aida.IHistogram1D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.List;
import org.hps.analysis.examples.TrackAnalysis;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.units.SystemOfUnits;
import org.lcsim.units.clhep.PhysicalConstants;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * driver to compare regular tracks and beamspot tracks reconstructed track
 * quantities plots things like number of
 * tracks/event, chi^2, track parameters (d0/z0/theta/phi/curvature)
 */
public class BeamSpotTrackAnalysis extends Driver {

    protected AIDA aida = AIDA.defaultInstance();
    private final String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private final String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private String trackCollectionName = "MatchedTracks";

    private String bsTrackCollectionName = "BeamSpotTracks";
    private String bsTrackRelationName = "BeamSpotTracksRelation";

    private String bsUncV0CollectionName = "BSUnconstrainedV0Candidates";
    private String targetConV0CollectionName = "TargetConstrainedV0Candidates";

    private final String plotDir = "";

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        IHistogram1D trkChi2 = aida.histogram1D("BS Track Chi2", 25, 0, 25.0);
        IHistogram1D origtrkChi2 = aida.histogram1D("Orig Track Chi2", 25, 0, 25.0);
        IHistogram1D nTracks = aida.histogram1D("BS Tracks per Event", 6, 0, 6);
        IHistogram1D trkd0 = aida.histogram1D("BS d0 ", 25, -1, 1);
        IHistogram1D trkphi = aida.histogram1D("BS sinphi ", 25, -0.2, 0.2);
        IHistogram1D trkomega = aida.histogram1D("BS omega ", 25, -0.00025, 0.00025);
        IHistogram1D trklam = aida.histogram1D("BS tan(lambda) ", 25, -0.1, 0.1);
        IHistogram1D trkz0 = aida.histogram1D("BS z0 ", 25, -0.5, 0.5);
        IHistogram1D nHits = aida.histogram1D("Hits per Track", 3, 5, 8);
        IHistogram1D onHits = aida.histogram1D("Orig Hits per Track", 3, 5, 8);

        IHistogram1D otrkd0 = aida.histogram1D("Orig d0 ", 25, -1, 1);
        IHistogram1D otrkphi = aida.histogram1D("Orig sinphi ", 25, -0.2, 0.2);
        IHistogram1D otrkomega = aida.histogram1D("Orig omega ", 25, -0.00025, 0.00025);
        IHistogram1D otrklam = aida.histogram1D("Orig tan(lambda) ", 25, -0.1, 0.1);
        IHistogram1D otrkz0 = aida.histogram1D("Orig z0 ", 25, -0.5, 0.5);

        IHistogram1D dtrkd0 = aida.histogram1D("Delta d0 ", 25, -1, 1);
        IHistogram1D dtrkphi = aida.histogram1D("Delta sinphi ", 25, -0.05, 0.05);
        IHistogram1D dtrkomega = aida.histogram1D("Delta omega ", 25, -0.0001, 0.0001);
        IHistogram1D dtrklam = aida.histogram1D("Delta tan(lambda) ", 25, -0.02, 0.02);
        IHistogram1D dtrkz0 = aida.histogram1D("Delta z0 ", 25, -0.5, 0.5);

        IHistogram1D bsX = aida.histogram1D("BS V0 X", 25, -0.2, 0.2);
        IHistogram1D bsY = aida.histogram1D("BS V0 Y", 25, -0.2, 0.2);
        IHistogram1D bsZ = aida.histogram1D("BS V0 Z", 25, -1, 1);
        IHistogram1D tarX = aida.histogram1D("Target V0 X", 25, -0.2, 0.2);
        IHistogram1D tarY = aida.histogram1D("Target V0 Y", 25, -0.2, 0.2);
        IHistogram1D tarZ = aida.histogram1D("Target V0 Z", 25, -1, 1);

        IHistogram1D bsdelp = aida.histogram1D("BS MC Delta p", 25, -0.2, 0.2);
        IHistogram1D origdelp = aida.histogram1D("Orig MC Delta p", 25, -0.2, 0.2);

        IHistogram1D bsdelm = aida.histogram1D("BS MC Delta m", 25, -0.2, 0.2);
        IHistogram1D origdelm = aida.histogram1D("Orig MC Delta m", 25, -0.2, 0.2);

        //      aida.tree().cd("/");
    }

    @Override
    public void process(EventHeader event) {
        RelationalTable hittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, rotatedHelicalTrackHitRelationsCollectionName)) {
            List<LCRelation> mcrelations = event.get(LCRelation.class, rotatedHelicalTrackHitRelationsCollectionName);
            for (LCRelation relation : mcrelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    hittomc.add(relation.getFrom(), relation.getTo());
//            System.out.println("Filling hittomc");
        }

//        aida.tree().cd("/");
        if (!event.hasCollection(Track.class, bsTrackCollectionName)) {
            aida.histogram1D("BS Tracks per Event").fill(0);
            return;
        }

        List<Track> bstracks = event.get(Track.class, bsTrackCollectionName);
        aida.histogram1D("BS Tracks per Event").fill(bstracks.size());
        List<LCRelation> bsRelation = event.get(LCRelation.class, bsTrackRelationName);
        RelationalTable bsToTrk = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        for (LCRelation relation : bsRelation)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                bsToTrk.add(relation.getFrom(), relation.getTo());
        for (Track trk : bstracks) {
            Track origTrk = (Track) bsToTrk.to(trk);

            aida.histogram1D("BS Track Chi2").fill(trk.getChi2());
            aida.histogram1D("Orig Track Chi2").fill(origTrk.getChi2());
            aida.histogram1D("Hits per Track").fill(trk.getTrackerHits().size());
            aida.histogram1D("Orig Hits per Track").fill(origTrk.getTrackerHits().size());
            aida.histogram1D("BS d0 ").fill(trk.getTrackStates().get(0).getD0());
            aida.histogram1D("BS sinphi ").fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
            aida.histogram1D("BS omega ").fill(trk.getTrackStates().get(0).getOmega());
            aida.histogram1D("BS tan(lambda) ").fill(trk.getTrackStates().get(0).getTanLambda());
            aida.histogram1D("BS z0 ").fill(trk.getTrackStates().get(0).getZ0());
            System.out.println(" BS chi^2 = " + trk.getChi2());

            aida.histogram1D("Orig d0 ").fill(origTrk.getTrackStates().get(0).getD0());
            aida.histogram1D("Orig sinphi ").fill(Math.sin(origTrk.getTrackStates().get(0).getPhi()));
            aida.histogram1D("Orig omega ").fill(origTrk.getTrackStates().get(0).getOmega());
            aida.histogram1D("Orig tan(lambda) ").fill(origTrk.getTrackStates().get(0).getTanLambda());
            aida.histogram1D("Orig z0 ").fill(origTrk.getTrackStates().get(0).getZ0());

            aida.histogram1D("Delta d0 ").fill(origTrk.getTrackStates().get(0).getD0() - trk.getTrackStates().get(0).getD0());
            aida.histogram1D("Delta sinphi ").fill(Math.sin(origTrk.getTrackStates().get(0).getPhi()) - Math.sin(trk.getTrackStates().get(0).getPhi()));
            aida.histogram1D("Delta omega ").fill(origTrk.getTrackStates().get(0).getOmega() - trk.getTrackStates().get(0).getOmega());
            aida.histogram1D("Delta tan(lambda) ").fill(origTrk.getTrackStates().get(0).getTanLambda() - trk.getTrackStates().get(0).getTanLambda());
            aida.histogram1D("Delta z0 ").fill(origTrk.getTrackStates().get(0).getZ0() - trk.getTrackStates().get(0).getZ0());

            TrackAnalysis tkanal = new TrackAnalysis(origTrk, hittomc);
            if (tkanal.getMCParticleNew() != null) {
                double mcMom = tkanal.getMCParticleNew().getMomentum().magnitude();
                double trkMom = getTotMomentum(trk.getTrackStates().get(0).getMomentum());
                double origtrkMom = getTotMomentum(origTrk.getTrackStates().get(0).getMomentum());
                aida.histogram1D("BS MC Delta p").fill(trkMom - mcMom);
                aida.histogram1D("Orig MC Delta p").fill(origtrkMom - mcMom);
//                System.out.println("mcMom = " + mcMom + "; trkMom = " + trkMom);
            }
        }

        if (!event.hasCollection(ReconstructedParticle.class, targetConV0CollectionName))
            return;

        List<ReconstructedParticle> targetRP = event.get(ReconstructedParticle.class, targetConV0CollectionName);

        for (ReconstructedParticle tarV0 : targetRP) {
            aida.histogram1D("Target V0 X").fill(tarV0.getStartVertex().getPosition().x());
            aida.histogram1D("Target V0 Y").fill(tarV0.getStartVertex().getPosition().y());
            aida.histogram1D("Target V0 Z").fill(tarV0.getStartVertex().getPosition().z());
//            System.out.println(" number of hits in tarV0(0) = " + tarV0.getParticles().get(0).getTracks().get(0).getTrackerHits().size());
            TrackAnalysis t1anal = new TrackAnalysis(tarV0.getParticles().get(0).getTracks().get(0), hittomc);
            TrackAnalysis t2anal = new TrackAnalysis(tarV0.getParticles().get(1).getTracks().get(0), hittomc);
            if (t1anal.getMCParticleNew() != null && t2anal.getMCParticleNew() != null) {
                double mMC = getInvariantMass(t1anal.getMCParticleNew().getMomentum(), t2anal.getMCParticleNew().getMomentum());
                double mReco = tarV0.getMass();
                aida.histogram1D("Orig MC Delta m").fill(mReco - mMC);
            }
        }
        if (!event.hasCollection(ReconstructedParticle.class, bsUncV0CollectionName))
            return;
        List<ReconstructedParticle> bsRP = event.get(ReconstructedParticle.class, bsUncV0CollectionName);

        for (ReconstructedParticle bsV0 : bsRP) {
            aida.histogram1D("BS V0 X").fill(bsV0.getStartVertex().getPosition().x());
            aida.histogram1D("BS V0 Y").fill(bsV0.getStartVertex().getPosition().y());
            aida.histogram1D("BS V0 Z").fill(bsV0.getStartVertex().getPosition().z());
            TrackAnalysis t1anal = new TrackAnalysis(bsV0.getParticles().get(0).getTracks().get(0), hittomc);
            TrackAnalysis t2anal = new TrackAnalysis(bsV0.getParticles().get(1).getTracks().get(0), hittomc);
            if (t1anal.getMCParticleNew() != null && t2anal.getMCParticleNew() != null) {
                double mMC = getInvariantMass(t1anal.getMCParticleNew().getMomentum(), t2anal.getMCParticleNew().getMomentum());
                double mReco = bsV0.getMass();
                aida.histogram1D("BS MC Delta m").fill(mReco - mMC);
            }
        }

//
//        if (!event.hasCollection(MCParticle.class, "MCParticle"))
//            return;
//        List<MCParticle> mcpList = event.get(MCParticle.class, "MCParticle");
//        for (MCParticle mcp : mcpList) {
//            System.out.println("MCP PDG ID = " + mcp.getPDGID() + "; time = " + mcp.getProductionTime());
//            if (mcp.getParents().size() > 0)
//                System.out.println("\t\tParent = " + mcp.getParents().get(0).getPDGID());
//
//            if (mcp.getDaughters().size() > 0)
//                System.out.println("\t\t\t\t Daughter = " + mcp.getDaughters().get(0).getPDGID());
//        }
    }

    double getTotMomentum(double[] mom) {
        return (new BasicHep3Vector(mom)).magnitude();
    }

    double getInvariantMass(Hep3Vector pvec1, Hep3Vector pvec2) {

        double e1 = pvec1.magnitude();
        double e2 = pvec2.magnitude();
        double me = PhysicalConstants.electron_mass_c2 / SystemOfUnits.GeV; //mass of electron in GeV
        double dotprod = VecOp.dot(pvec1, pvec2);
        double m2 = 2 * me * me + 2 * (e1 * e2 - dotprod);
        if (m2 > 0)
            return Math.sqrt(m2);
        return -99;
    }

}
