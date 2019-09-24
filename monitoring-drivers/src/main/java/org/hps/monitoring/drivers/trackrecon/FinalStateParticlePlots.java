package org.hps.monitoring.drivers.trackrecon;

import static org.hps.monitoring.drivers.trackrecon.PlotAndFitUtilities.plot;
import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import static java.lang.Math.sqrt;
import java.util.List;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;

import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.TrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class FinalStateParticlePlots extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    String finalStateParticlesColName = "FinalStateParticles";
    String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    // some counters
    int nRecoEvents = 0;
    boolean debug = false;

    IPlotter plotterEle;
    IPlotter plotterPos;
    IPlotter plotterPhot;
    String outputPlots;

    IPlotterFactory plotterFactory;
    IFunctionFactory functionFactory;
    IFitFactory fitFactory;

    IHistogram1D nEle;
    IHistogram1D elePx;
    IHistogram1D elePy;
    IHistogram1D elePz;
    IHistogram2D eleProjXYEcalMatch;
    IHistogram2D eleProjXYEcalNoMatch;

    IHistogram1D nPos;
    IHistogram1D posPx;
    IHistogram1D posPy;
    IHistogram1D posPz;
    IHistogram2D posProjXYEcalMatch;
    IHistogram2D posProjXYEcalNoMatch;

    IHistogram1D nPhot;
    IHistogram1D photEne;
    IHistogram2D photXYECal;
    IHistogram1D pi0Ene;
    IHistogram1D pi0Diff;
    IHistogram1D pi0Mass;

    double ecalXRange = 500;
    double ecalYRange = 100;

    double pMax = 7.0;
    double pi0EsumCut = 3.0;//GeV
    double pi0EdifCut = 2.0;//GeV

    public void setPMax(double pmax) {
        this.pMax = pmax;
    }

    public void setPi0EsumCut(double cut) {
        this.pi0EsumCut = cut;
    }

    public void setPi0EdifCut(double cut) {
        this.pi0EdifCut = cut;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        // System.out.println("V0Monitoring::detectorChanged  Setting up the plotter");

        IAnalysisFactory fac = aida.analysisFactory();
        IPlotterFactory pfac = fac.createPlotterFactory("Final State Recon");
        functionFactory = aida.analysisFactory().createFunctionFactory(null);
        fitFactory = aida.analysisFactory().createFitFactory();

        aida.tree().cd("/");
        // resetOccupancyMap(); // this is for calculatin
        plotterEle = pfac.create("Electrons");
        plotterEle.createRegions(2, 3);

        plotterPos = pfac.create("Positrons");
        plotterPos.createRegions(2, 3);

        plotterPhot = pfac.create("Photons and Pi0");
        plotterPhot.createRegions(2, 3);

        /* V0 Quantities */
 /* Mass, vertex, chi^2 of fit */
 /* beamspot constrained */
        nEle = aida.histogram1D("Number of Electrons per event", 5, 0, 5);
        elePx = aida.histogram1D("Electron Px (GeV)", 50, -0.2, 0.2);
        elePy = aida.histogram1D("Electron Py (GeV)", 50, -0.2, 0.2);
        elePz = aida.histogram1D("Electron Pz (GeV)", 50, 0.0, pMax);
        eleProjXYEcalMatch = aida.histogram2D("Electron ECal Projection: Matched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);
        eleProjXYEcalNoMatch = aida.histogram2D("Electron ECal Projection: Unmatched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);
        plot(plotterEle, nEle, null, 0);
        plot(plotterEle, elePx, null, 1);
        plot(plotterEle, elePy, null, 2);
        plot(plotterEle, elePz, null, 3);
        plot(plotterEle, eleProjXYEcalMatch, null, 4);
        plot(plotterEle, eleProjXYEcalNoMatch, null, 5);

        nPos = aida.histogram1D("Number of Positrons per event", 5, 0, 5);
        posPx = aida.histogram1D("Positron Px (GeV)", 50, -0.2, 0.2);
        posPy = aida.histogram1D("Positron Py (GeV)", 50, -0.2, 0.2);
        posPz = aida.histogram1D("Positron Pz (GeV)", 50, 0.0, pMax);
        posProjXYEcalMatch = aida.histogram2D("Positron ECal Projection: Matched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);
        posProjXYEcalNoMatch = aida.histogram2D("Positron ECal Projection: Unmatched", 50, -ecalXRange, ecalXRange, 50, -ecalYRange, ecalYRange);
        plot(plotterPos, nPos, null, 0);
        plot(plotterPos, posPx, null, 1);
        plot(plotterPos, posPy, null, 2);
        plot(plotterPos, posPz, null, 3);
        plot(plotterPos, posProjXYEcalMatch, null, 4);
        plot(plotterPos, posProjXYEcalNoMatch, null, 5);

        nPhot = aida.histogram1D("Number of Photons per event", 5, 0, 5);
        photEne = aida.histogram1D("Photon Energy (GeV)", 50, 0.0, pMax);
        photXYECal = aida.histogram2D("ECal Position", 50, -300, 400, 50, -ecalYRange, ecalYRange);
        pi0Ene = aida.histogram1D("pi0 Energy (GeV)", 50, pi0EsumCut, pMax);
        pi0Diff = aida.histogram1D("pi0 E-Diff (GeV)", 50, 0, pi0EdifCut);
        pi0Mass = aida.histogram1D("pi0 Mass (GeV)", 50, 0.0, 0.3);

        plot(plotterPhot, nPhot, null, 0);
        plot(plotterPhot, photEne, null, 1);
        plot(plotterPhot, photXYECal, null, 2);
        plot(plotterPhot, pi0Ene, null, 3);
        plot(plotterPhot, pi0Diff, null, 4);
        plot(plotterPhot, pi0Mass, null, 5);

        plotterEle.show();
        plotterPos.show();
        plotterPhot.show();
    }

    @Override
    public void process(EventHeader event) {
        /* make sure everything is there */
        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName))
            return;
        if (!event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName))
            return;
        if (!event.hasCollection(ReconstructedParticle.class, beamConV0CandidatesColName))
            return;
        if (!event.hasCollection(ReconstructedParticle.class, targetV0ConCandidatesColName))
            return;
        nRecoEvents++;

        List<ReconstructedParticle> fspList = event.get(ReconstructedParticle.class,
                finalStateParticlesColName);

        int eleCnt = 0;
        int posCnt = 0;
        int photCnt = 0;
        for (ReconstructedParticle fsp : fspList) {
            double charge = fsp.getCharge();
            if (charge < 0) {
                eleCnt++;
                Hep3Vector mom = fsp.getMomentum();
                elePx.fill(mom.x());
                elePy.fill(mom.y());
                elePz.fill(mom.z());
                TrackState stateAtEcal = TrackUtils.getTrackStateAtECal((fsp.getTracks().get(0)));
                Hep3Vector tPos = new BasicHep3Vector(stateAtEcal.getReferencePoint());
                if (fsp.getClusters().size() != 0)
                    eleProjXYEcalMatch.fill(tPos.y(), tPos.z());
                else
                    eleProjXYEcalNoMatch.fill(tPos.y(), tPos.z());
            } else if (charge > 0) {
                posCnt++;
                Hep3Vector mom = fsp.getMomentum();
                posPx.fill(mom.x());
                posPy.fill(mom.y());
                posPz.fill(mom.z());
                TrackState stateAtEcal = TrackUtils.getTrackStateAtECal((fsp.getTracks().get(0)));
                Hep3Vector tPos = new BasicHep3Vector(stateAtEcal.getReferencePoint());
                if (fsp.getClusters().size() != 0)
                    posProjXYEcalMatch.fill(tPos.y(), tPos.z());// tracking frame!
                else
                    posProjXYEcalNoMatch.fill(tPos.y(), tPos.z());
            } else if (fsp.getClusters().size() != 0) {
                photCnt++;
                Cluster clu = fsp.getClusters().get(0);
                photEne.fill(clu.getEnergy());
                photXYECal.fill(clu.getPosition()[0], clu.getPosition()[1]);
            } else
                System.out.println("This FSP had no tracks or clusters???");
        }

        for (ReconstructedParticle fsp1 : fspList) {
            if (fsp1.getCharge() != 0)
                continue;
            for (ReconstructedParticle fsp2 : fspList) {
                if (fsp1 == fsp2)
                    continue;
                if (fsp2.getCharge() != 0)
                    continue;
//                if (fsp1.getClusters().get(0) == null || fsp2.getClusters().get(0) == null)
//                    continue;//this should never happen
                Cluster clu1 = fsp1.getClusters().get(0);
                Cluster clu2 = fsp2.getClusters().get(0);
                double pi0ene = clu1.getEnergy() + clu2.getEnergy();
                double pi0diff = Math.abs(clu1.getEnergy() - clu2.getEnergy());
                double pi0mass = getClusterPairMass(clu1, clu2);
                if (pi0diff > pi0EdifCut)
                    continue;
                if (pi0ene < pi0EsumCut)
                    continue;
                if (clu1.getPosition()[1] * clu2.getPosition()[1] < 0) {//top bottom
                    pi0Ene.fill(pi0ene);
                    pi0Diff.fill(pi0diff);
                    pi0Mass.fill(pi0mass);
                }
            }
        }
        nEle.fill(eleCnt);
        nPos.fill(posCnt);
        nPhot.fill(photCnt);
    }

    public double getClusterPairMass(Cluster clu1, Cluster clu2) {
        double x0 = clu1.getPosition()[0];
        double y0 = clu1.getPosition()[1];
        double z0 = clu1.getPosition()[2];
        double x1 = clu2.getPosition()[0];
        double y1 = clu2.getPosition()[1];
        double z1 = clu2.getPosition()[2];
        double e0 = clu1.getEnergy();
        double e1 = clu2.getEnergy();
        double xlen0 = sqrt(x0 * x0 + y0 * y0 + z0 * z0);
        double xlen1 = sqrt(x1 * x1 + y1 * y1 + z1 * z1);
        Hep3Vector p0 = new BasicHep3Vector(x0 / xlen0 * e0, y0 / xlen0 * e0, z0 / xlen0 * e0);
        Hep3Vector p1 = new BasicHep3Vector(x1 / xlen1 * e1, y1 / xlen1 * e1, z1 / xlen1 * e1);

        double esum = sqrt(p1.magnitudeSquared()) + sqrt(p0.magnitudeSquared());
        double pxsum = p1.x() + p0.x();
        double pysum = p1.y() + p0.y();
        double pzsum = p1.z() + p0.z();

        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = esum * esum - psum * psum;

        if (evtmass > 0)
            return Math.sqrt(evtmass);
        else
            return -99;
    }

}
