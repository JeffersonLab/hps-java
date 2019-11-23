package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;

import java.util.List;
import java.util.logging.Logger;

// import org.hps.UnusedImportCheckstyleViolation
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;

/*
    Simple driver for looking at di-muon candidates
    Requires top/bottom track-matched clusters
    ECal Cluster size<3 and energy<0.5 GeV
*/
public class MuonCandidateMonitoring extends DataQualityMonitor {

    private static Logger LOGGER = Logger.getLogger(V0Monitoring.class.getPackage().getName());

    private String finalStateParticlesColName = "FinalStateParticles";
    private String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    private String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    private String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    private String clusterCollectionName = "EcalClustersCorr";

    private int nRecoEvents = 0;

    private String plotDir = "MuonCandidates/";

    double ecalXRange = 500;
    double ecalYRange = 100;

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */

        //check to see if this event is from the correct trigger (or "all");
        if (!matchTrigger(event))
            return;

        nRecoEvents++;

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        List<ReconstructedParticle> v0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);
        for (ReconstructedParticle uncV0 : v0List) {
            if (isGBL != TrackType.isGBL(uncV0.getType()))
                continue;
            Vertex uncVert = uncV0.getStartVertex();

            //this always has 2 tracks. 
            List<ReconstructedParticle> trks = uncV0.getParticles();
            //            Track ele = trks.get(0).getTracks().get(0);
            //            Track pos = trks.get(1).getTracks().get(0);
            //            //if track #0 has charge>0 it's the electron!  This seems mixed up, but remember the track 
            //            //charge is assigned assuming a positive B-field, while ours is negative
            //            if (trks.get(0).getCharge() > 0) {
            //                pos = trks.get(0).getTracks().get(0);
            //                ele = trks.get(1).getTracks().get(0);
            //            }
            //            aida.histogram2D(plotDir + trkType + triggerType + "/" + "P(e) vs P(p)").fill(getMomentum(ele), getMomentum(pos));
            //            aida.histogram2D(plotDir + trkType + triggerType + "/" + "Px(e) vs Px(p)").fill(ele.getTrackStates().get(0).getMomentum()[1], pos.getTrackStates().get(0).getMomentum()[1]);
            //            aida.histogram2D(plotDir + trkType + triggerType + "/" + "Py(e) vs Py(p)").fill(ele.getTrackStates().get(0).getMomentum()[2], pos.getTrackStates().get(0).getMomentum()[2]);
            ReconstructedParticle muMinus = null;
            ReconstructedParticle muPlus = null;
            //ReconParticles have the charge correct. 
            if (trks.get(0).getCharge() > 0 && trks.get(1).getCharge() < 0) {
                muPlus = trks.get(0);
                muMinus = trks.get(1);
            } else if (trks.get(1).getCharge() > 0 && trks.get(0).getCharge() < 0) {
                muPlus = trks.get(1);
                muMinus = trks.get(0);
            }

            Hep3Vector pPlus = muPlus.getMomentum();
            Hep3Vector pMinus = muMinus.getMomentum();

            //cut out pairs that have both particles on the same half of SVT.  
            if (pPlus.y() * pMinus.y() > 0)
                continue;

            double pp = pPlus.magnitude();
            double pm = pMinus.magnitude();

            double psum = pp + pm;
            double pdiff = pp - pm;
            if (psum > v0PSumMaxCut || psum < v0PSumMinCut)
                continue;
            if (pdiff > maxDiffCut || pdiff < -maxDiffCut)
                continue;
            if (pp < minP || pm < minP)
                continue;
            if (pp > maxP || pm > maxP)
                continue;

            Track trackPlus = muPlus.getTracks().get(0);
            Track trackMinus = muMinus.getTracks().get(0);
            double[] trkAtEcalPlus = TrackUtils.getTrackStateAtECal(trackPlus).getReferencePoint();
            double[] trkAtEcalMinus = TrackUtils.getTrackStateAtECal(trackMinus).getReferencePoint();
            double tPlus = TrackUtils.getTrackTime(trackPlus, hitToStrips, hitToRotated);
            double tMinus = TrackUtils.getTrackTime(trackMinus, hitToStrips, hitToRotated);
//            System.out.println("track Plus AtEcal = (" + trkAtEcalPlus[0] + ", " + trkAtEcalPlus[1] + ", " + trkAtEcalPlus[2] + ")");
//            System.out.println("track Minus AtEcal = (" + trkAtEcalMinus[0] + ", " + trkAtEcalMinus[1] + ", " + trkAtEcalMinus[2] + ")");

            //distance between the extrapolated track and the nearest cluster
            double iso1 = 1000, iso2 = 1000;

            Cluster ncPlus = getNearestCluster(clusters, trackPlus);
            if (ncPlus != null) {
                iso1 = Math.hypot(trkAtEcalPlus[2] - ncPlus.getPosition()[1], trkAtEcalPlus[1] - ncPlus.getPosition()[0]);
//                System.out.println("isoPlus = " + iso1);
                dxyNearestClusterPlus.fill(trkAtEcalPlus[1] - ncPlus.getPosition()[0], trkAtEcalPlus[2] - ncPlus.getPosition()[1]);
            }
            Cluster ncMinus = getNearestCluster(clusters, trackMinus);
            if (ncMinus != null) {
                iso2 = Math.hypot(trkAtEcalMinus[2] - ncMinus.getPosition()[1], trkAtEcalMinus[1] - ncMinus.getPosition()[0]);
//                System.out.println("isoMinus = " + iso2);
                dxyNearestClusterMinus.fill(trkAtEcalMinus[1] - ncMinus.getPosition()[0], trkAtEcalMinus[2] - ncMinus.getPosition()[1]);
            }
//            System.out.println("isoPlus = " + iso1 + "; isoMinus = " + iso2);
            if (iso1 > 40 || iso2 > 40)
                continue;

            if (ncPlus.getSize() > maxClSize || ncMinus.getSize() > maxClSize)
                continue;
            if (ncPlus.getEnergy() > maxClEnergy || ncMinus.getEnergy() > maxClEnergy)
                continue;

            xyAtEcalPlus.fill(trkAtEcalPlus[1], trkAtEcalPlus[2]);
            xyAtEcalMinus.fill(trkAtEcalMinus[1], trkAtEcalMinus[2]);
//                if (!fid_ECal(trkAtEcalPlus[1], trkAtEcalPlus[2]))
//                    continue;
//                if (!fid_ECal(trkAtEcalMinus[1], trkAtEcalMinus[2]))
//                    continue;

//            System.out.println("cluster energy Plus " + ncPlus.getEnergy());
//            System.out.println("cluster energy Minus " + ncMinus.getEnergy());
            clSizePlus.fill(ncPlus.getSize());
            clSizeMinus.fill(ncMinus.getSize());
            clEnergyPlus.fill(ncPlus.getEnergy());
            clEnergyMinus.fill(ncMinus.getEnergy());

            tPlusVsTMinus.fill(tPlus, tMinus);
            timeDiff.fill(tPlus - tMinus);

            double mmu2 = .1057 * .1057;

            double mass = Math.sqrt(2 * mmu2 + 2 * Math.sqrt(pPlus.magnitudeSquared() + mmu2) * Math.sqrt(pMinus.magnitudeSquared() + mmu2)
                    - 2 * (pPlus.x() * pMinus.x() + pPlus.y() * pMinus.y() + pPlus.z() * pMinus.z()));
            this.mass.fill(mass);

            sumPxPy.fill(pPlus.x() + pMinus.x(), pPlus.y() + pMinus.y());

            this.pPlus.fill(pp);
            this.pMinus.fill(pm);
            this.pPlusVsPMinus.fill(pp, pm);
            this.pTot.fill(pm + pp);

            for (Cluster c : clusters)
                clusterXY.fill(c.getPosition()[0], c.getPosition()[1]);
        }
    }

    public static boolean fid_ECal(double x, double y) {
        y = Math.abs(y);

        boolean in_fid = false;
        double x_edge_low = -262.74;
        double x_edge_high = 347.7;
        double y_edge_low = 33.54;
        double y_edge_high = 75.18;

        double x_gap_low = -106.66;
        double x_gap_high = 42.17;
        double y_gap_high = 47.18;

        y = Math.abs(y);

        if (x > x_edge_low && x < x_edge_high && y > y_edge_low && y < y_edge_high)
            if (!(x > x_gap_low && x < x_gap_high && y > y_edge_low && y < y_gap_high))
                in_fid = true;

        return in_fid;
    }

    private Cluster getNearestCluster(List<Cluster> clusters, Track t) {
        double rbest = 1000;
        Cluster best = null;
        for (Cluster c : clusters) {
//            Hep3Vector xy = TrackUtils.extrapolateTrack(t, c.getPosition()[2]);
            double[] trkAtEcal = TrackUtils.getTrackStateAtECal(t).getReferencePoint();
            double r = Math.hypot(trkAtEcal[2] - c.getPosition()[1], trkAtEcal[1] - c.getPosition()[0]);
//            if (t.getCharge() > 0) {
//                System.out.println("cluster position = (" + c.getPosition()[0] + ", " + c.getPosition()[1] + ", " + c.getPosition()[2] + ")");
//                System.out.println("track     AtEcal = (" + trkAtEcal[1] + ", " + trkAtEcal[2] + ", " + trkAtEcal[0] + ")");
//            }
            if (r < rbest) {
                rbest = r;
                best = c;
            }
        }
        return best;
    }

    private double v0PSumMinCut, v0PSumMaxCut, maxDiffCut;

    private double minP;
    private double maxP;
    private double maxClSize = 3;
    private double maxClEnergy = 0.5;

    @Override
    protected void detectorChanged(Detector detector) {
        super.detectorChanged(detector);
        /*tab*///feeMomentumCut = 0.75*beamEnergy; //GeV
        beamEnergy = 4.5;
        v0PSumMinCut = 0.2 * beamEnergy;
        v0PSumMaxCut = 1.25 * beamEnergy;
        minP = .1 * beamEnergy;
        maxP = .85 * beamEnergy;
        maxDiffCut = .8 * beamEnergy;

        LOGGER.info("Setting up the plotter");
        aida.tree().cd("/");

        String trkType = "SeedTrack/";
        if (isGBL)
            trkType = "GBLTrack/";
        double minMass = 0.2;
        double maxMass = .1 * beamEnergy;
        /*  V0 Quantities   */
 /*  Mass, vertex, chi^2 of fit */
 /*  unconstrained */
        mass = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Invariant Mass (GeV)", 100, minMass, maxMass);
        pPlus = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "P mu+", 100, 0, 1.2 * beamEnergy);
        pMinus = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "P mu-", 100, 0, 1.2 * beamEnergy);

        timeDiff = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Time Difference", 100, -10, 10);

        tPlusVsTMinus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "t+ versus t-", 100, -10, 10, 100, -10, 10);

        pPlusVsPMinus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "P mu+ vs P mu-", 100, 0, 1.2 * beamEnergy, 100, 0, 1.2 * beamEnergy);

        pTot = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "P tot", 100, 0, 1.2 * beamEnergy);

        xyAtEcalMinus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "XY at Ecal (mu-)", 100, -ecalXRange, ecalXRange, 100, -ecalYRange, ecalYRange);
        xyAtEcalPlus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "XY at Ecal (mu+)", 100, -ecalXRange, ecalXRange, 100, -ecalYRange, ecalYRange);

        clusterXY = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Cluster XY", 100, -ecalXRange, ecalXRange, 85, -ecalYRange, ecalYRange);

        dxyNearestClusterPlus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "dXY from nearest cluster (mu+)", 100, -30.0, 30.0, 100, -10.0, 10.0);
        dxyNearestClusterMinus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "dXY from nearest cluster (mu-)", 100, -30.0, 30.0, 100, -10.0, 10.0);
        sumPxPy = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "pair px vs py", 100, -.1, .1, 100, -.1, .1);
        clSizePlus = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Cluster Size mu+", 10, 0, maxClSize);
        clSizeMinus = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Cluster Size mu-", 10, 0, maxClSize);
        clEnergyPlus = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Cluster Energy mu+", 50, 0, maxClEnergy);
        clEnergyMinus = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Cluster Energy mu-", 50, 0, maxClEnergy);

    }

    private IHistogram1D mass, pPlus, pMinus, pTot, timeDiff;

    private IHistogram2D xyAtEcalPlus, xyAtEcalMinus, pPlusVsPMinus, tPlusVsTMinus;

    private IHistogram2D dxyNearestClusterPlus, dxyNearestClusterMinus;

    private IHistogram2D clusterXY;

    private IHistogram2D sumPxPy;

    private IHistogram1D clSizePlus, clSizeMinus;
    private IHistogram1D clEnergyPlus, clEnergyMinus;

}
