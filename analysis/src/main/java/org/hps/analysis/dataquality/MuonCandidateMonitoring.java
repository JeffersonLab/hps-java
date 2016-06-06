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

public class MuonCandidateMonitoring extends DataQualityMonitor{

    private static Logger LOGGER = Logger.getLogger(V0Monitoring.class.getPackage().getName());

    private String finalStateParticlesColName = "FinalStateParticles";
    private String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    private String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    private String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    private String clusterCollectionName = "EcalClustersCorr";

    private int nRecoEvents = 0;

    private String plotDir = "MuonCandidates/";

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
            }
            else if(trks.get(1).getCharge() > 0 && trks.get(0).getCharge() < 0){
                muPlus = trks.get(1);
                muMinus = trks.get(0);
            }





            Hep3Vector pPlus = muPlus.getMomentum();
            Hep3Vector pMinus = muMinus.getMomentum();

            //cut out pairs that have both particles on the same half of SVT.  
            if(pPlus.y()*pMinus.y()>0){
                continue;
            }


            double pp = pPlus.magnitude();
            double pm = pMinus.magnitude();

            double psum = pp + pm;
            double pdiff = pp - pm;
            if(psum > v0PSumMaxCut || psum < v0PSumMinCut)
                continue;
            if(pdiff > maxDiffCut || pdiff < -maxDiffCut)
                continue;
            if(pp < minP || pm < minP)
                continue;
            if(pp > maxP || pm > maxP)
                continue;


            Track trackPlus = muPlus.getTracks().get(0);
            Track trackMinus = muMinus.getTracks().get(0);

            double tPlus = TrackUtils.getTrackTime(trackPlus, hitToStrips, hitToRotated);
            double tMinus = TrackUtils.getTrackTime(trackMinus, hitToStrips, hitToRotated);



            {
                double ecalZPosition =  1395;
                Hep3Vector xyPlus = TrackUtils.extrapolateTrack(trackPlus, ecalZPosition);
                Hep3Vector xyMinus = TrackUtils.extrapolateTrack(trackMinus, ecalZPosition);

                xyAtEcalPlus.fill(xyPlus.x(), xyPlus.y());
                xyAtEcalMinus.fill(xyMinus.x(), xyMinus.y());
                if(!fid_ECal(xyPlus.x(), xyPlus.y()))
                    continue;
                if(!fid_ECal(xyMinus.x(), xyMinus.y()))
                    continue;
            }           


            //distance between the extrapolated track and the nearest cluster
            double iso1 = 1000, iso2 = 1000;

            Cluster nc = getNearestCluster(clusters, trackPlus);
            if(nc != null){
                Hep3Vector xyPlus = TrackUtils.extrapolateTrack(trackPlus, nc.getPosition()[2]);
                iso1 = Math.hypot(xyPlus.x()-nc.getPosition()[0], xyPlus.y()-nc.getPosition()[1]);
                dxyNearestClusterPlus.fill(xyPlus.x()-nc.getPosition()[0], xyPlus.y()-nc.getPosition()[1]);
            }
            nc = getNearestCluster(clusters, trackMinus);
            if(nc != null){
                Hep3Vector xyMinus = TrackUtils.extrapolateTrack(trackMinus, nc.getPosition()[2]);
                iso2 = Math.hypot(xyMinus.x()-nc.getPosition()[0], xyMinus.y()-nc.getPosition()[1]);
                dxyNearestClusterMinus.fill(xyMinus.x()-nc.getPosition()[0], xyMinus.y()-nc.getPosition()[1]);
            }

            if(iso1 < 20 || iso2 < 20)
                continue;

            tPlusVsTMinus.fill(tPlus, tMinus);
            timeDiff.fill(tPlus- tMinus);




            double mmu2 = .1057*.1057;

            double mass = Math.sqrt(2*mmu2 + 2*Math.sqrt(pPlus.magnitudeSquared()+mmu2)*Math.sqrt(pMinus.magnitudeSquared()+mmu2)
                    -2*(pPlus.x()*pMinus.x()+pPlus.y()*pMinus.y()+pPlus.z()*pMinus.z()));
            this.mass.fill(mass);

            sumPxPy.fill(pPlus.x()+pMinus.x(), pPlus.y()+pMinus.y());

            this.pPlus.fill(pp);
            this.pMinus.fill(pm);
            this.pPlusVsPMinus.fill(pp, pm);
            this.pTot.fill(pm+pp);

            for(Cluster c : clusters){
                clusterXY.fill(c.getPosition()[0], c.getPosition()[1]);
            }
        }
    }

    public static boolean fid_ECal(double x, double y)
    {
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

        if( x > x_edge_low && x < x_edge_high && y > y_edge_low && y < y_edge_high )
        {
            if( !(x > x_gap_low && x < x_gap_high && y > y_edge_low && y < y_gap_high) )
            {
                in_fid = true;
            }
        }

        return in_fid;
    }
    
    private Cluster getNearestCluster(List<Cluster> clusters, Track t) {
        double rbest = 1000;
        Cluster best = null;
        for(Cluster c : clusters){
            Hep3Vector xy = TrackUtils.extrapolateTrack(t, c.getPosition()[2]);
            double r = Math.hypot(xy.x()-c.getPosition()[0], xy.y()-c.getPosition()[1]);
            if(r<rbest){
                rbest = r;
                best= c;
            }
        }
        return best;
    }

    private double v0PSumMinCut, v0PSumMaxCut, maxDiffCut;

    private double minP;
    private double maxP;

    @Override
    protected void detectorChanged(Detector detector) {
        super.detectorChanged(detector);
    /*tab*///feeMomentumCut = 0.75*beamEnergy; //GeV

        v0PSumMinCut = 0.2 * beamEnergy;
        v0PSumMaxCut = 1.25 * beamEnergy;
        minP = .1 *beamEnergy;
        maxP = .85*beamEnergy;
        maxDiffCut = .8*beamEnergy;




        LOGGER.info("Setting up the plotter");
        aida.tree().cd("/");

        String trkType = "SeedTrack/";
        if (isGBL)
            trkType = "GBLTrack/";

        double maxMass = .4*beamEnergy;
        /*  V0 Quantities   */
        /*  Mass, vertex, chi^2 of fit */
        /*  unconstrained */
        mass = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Invariant Mass (GeV)", 100, 0, maxMass);
        pPlus = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "P mu+", 100, 0, 1.5*beamEnergy);
        pMinus = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "P mu-", 100, 0, 1.5*beamEnergy);

        timeDiff = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Time Difference", 100, -10, 10);

        tPlusVsTMinus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "t+ versus t-", 100, -10, 10, 100, -10, 10);

        pPlusVsPMinus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "P mu+ vs P mu-", 100, 0, 1.5*beamEnergy, 100, 0, 1.5*beamEnergy);




        pTot = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "P tot", 100, 0,  2*beamEnergy);

        xyAtEcalMinus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "XY at Ecal (mu-)", 200, -200.0, 200.0, 85, -85.0, 85.0);
        xyAtEcalPlus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "XY at Ecal (mu+)",  200, -200.0, 200.0, 85, -85.0, 85.0);

        clusterXY = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Cluster XY",  200, -200.0, 200.0, 85, -85.0, 85.0);


        dxyNearestClusterPlus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "dXY from nearest cluster (mu+)",  200, -30.0, 30.0, 85, -30.0, 30.0);
        dxyNearestClusterMinus = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "dXY from nearest cluster (mu-)",  200, -30.0, 30.0, 85, -30.0, 30.0);
        sumPxPy = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "pair px vs py",  100, -.1, .1, 100, -.1, .1);
    }



    private IHistogram1D mass, pPlus, pMinus, pTot, timeDiff;


    private IHistogram2D xyAtEcalPlus, xyAtEcalMinus, pPlusVsPMinus, tPlusVsTMinus;

    private IHistogram2D dxyNearestClusterPlus, dxyNearestClusterMinus;

    private IHistogram2D clusterXY;

    private IHistogram2D sumPxPy;

}
