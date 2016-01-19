package org.hps.users.mgraham;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.base.BaseMCParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class MCParticleAnalysis extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private String collectionName = "MCParticle";
    IPlotter plotterMC;
    IHistogram1D px;
    IHistogram1D py;
    IHistogram1D pz;
    IHistogram1D pTot;
    IHistogram2D EposEele;
    IHistogram1D ePos;
    IHistogram1D eEle1;
    IHistogram1D eEle2;
    IHistogram1D EepSum;
    IHistogram1D EepSumEndpoint;
    IHistogram1D EeepSum;
    IHistogram1D EepDiff;

    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("MCParicleAnalysis::detectorChanged  Setting up the plotter");
        IAnalysisFactory fac = aida.analysisFactory();
        IPlotterFactory pfac = fac.createPlotterFactory("MC Particles");

        aida.tree().cd("/");
//        resetOccupancyMap(); // this is for calculatin
        plotterMC = pfac.create("MC Particle Momentum");
        plotterMC.createRegions(2, 2);
        px = aida.histogram1D("px (GeV)", 50, -0.05, 0.14);
        py = aida.histogram1D("py (GeV)", 50, -0.1, 0.1);
        pz = aida.histogram1D("pz (GeV)", 50, -0.25, 2.5);
        pTot = aida.histogram1D("angle X", 50, -0.01, 0.04);
        plotterMC.region(0).plot(px);
        plotterMC.region(1).plot(py);
        plotterMC.region(2).plot(pz);
        plotterMC.region(3).plot(pTot);
        plotterMC.show();

        EposEele = aida.histogram2D("Epos vs Eele", 50, 0, 2.0, 50, 0, 2.0);

        ePos = aida.histogram1D("Epos", 50, 0, 2.0);
        eEle1 = aida.histogram1D("Eele1", 50, 0, 2.0);
        eEle2 = aida.histogram1D("Eele2", 50, 0, 2.0);
        EepSumEndpoint = aida.histogram1D("Pair energy:  Endpoint", 50, 1.5, 2.0);
        EepSum = aida.histogram1D("Pair energy", 50, 0, 2.0);
        EeepSum = aida.histogram1D("Trident energy", 50, 0, 2.0);

        // pairs1
        EepDiff = aida.histogram1D("Energy Diff", 50, 0, 2.0);


    }

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */
        if (!event.hasCollection(MCParticle.class, collectionName))
            return;

        List<MCParticle> mcpList = event.get(MCParticle.class, collectionName);

        MCParticle pos = null;
        MCParticle ele1 = null;
        MCParticle ele2 = null;

        for (MCParticle mcp : mcpList) {
            if (mcp.getCharge() < 0 && mcp.getMomentum().magnitude() > 0.025) {
                px.fill(mcp.getPX());
                py.fill(mcp.getPY());
                pz.fill(mcp.getPZ());
                pTot.fill(mcp.getPX() / mcp.getMomentum().magnitude());
            }

            if (!mcp.getParents().isEmpty())
                if (mcp.getParents().get(0).getPDGID() == 622)
                    if (mcp.getCharge() > 0)
                        pos = mcp;
                    else if (ele1 == null)
                        ele1 = mcp;
                    else
                        ele2 = mcp;
        }
        if (ele1 != null && ele2 != null && pos != null) {
            EposEele.fill(ele1.getEnergy(), pos.getEnergy());
            EposEele.fill(ele2.getEnergy(), pos.getEnergy());
            //EposEele.fill(Math.max(ele2.getEnergy(),ele1.getEnergy()), pos.getEnergy());

            eEle1.fill(ele1.getEnergy());
            eEle2.fill(ele2.getEnergy());
            ePos.fill(pos.getEnergy());

            EepSum.fill(ele1.getEnergy() + pos.getEnergy());
            EepSum.fill(ele2.getEnergy() + pos.getEnergy());
            EeepSum.fill(ele1.getEnergy() + pos.getEnergy() + ele2.getEnergy());

            EepSumEndpoint.fill(ele1.getEnergy() + pos.getEnergy());
            EepSumEndpoint.fill(ele2.getEnergy() + pos.getEnergy());
            
            EepDiff.fill(ele1.getEnergy() - pos.getEnergy());
            EepDiff.fill(ele2.getEnergy() - pos.getEnergy());

        }

    }
}
