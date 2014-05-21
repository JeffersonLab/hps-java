package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;

/**
 * DQM driver reconstructed particles (i.e. electrons, positrons, photons) plots
 * things like number of electrons (or positrons)/event, photons/event, e+/e-
 * momentum, and track-cluster matching stuff
 *
 * @author mgraham on Mar 28, 2014 big update on May 14, 2014...right now the
 * output is crap; no charge<0 tracks & the track momentum isn't filled; likely
 * a problem with ReconParticle TODO: may want to break out the V0 DQM (not
 * written) into it's own class
 */
public class V0Monitoring extends DataQualityMonitor {

    String finalStateParticlesColName = "FinalStateParticles";
    String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    private Map<String, Double> monitoredQuantityMap = new HashMap<>();
    String[] fpQuantNames = {"nEle_per_Event", "nPos_per_Event", "nPhoton_per_Event", "nUnAssociatedTracks_per_Event", "avg_delX_at_ECal", "avg_delY_at_ECal", "avg_E_Over_P"};
    //some counters
    int nRecoEvents = 0;
    int nTotEle = 0;
    int nTotPos = 0;
    int nTotPhotons = 0;
    int nTotUnAss = 0;
    int nTotAss = 0;
    //some summers
    double sumdelX = 0.0;
    double sumdelY = 0.0;
    double sumEoverP = 0.0;
    boolean debug = false;
    private String plotDir = "V0Monitoring/";

    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("V0Monitoring::detectorChanged  Setting up the plotter");
        aida.tree().cd("/");

        /*  V0 Quantities   */
        /*  Mass, vertex, chi^2 of fit */
        /* beamspot constrained */
        IHistogram1D bsconMass = aida.histogram1D(plotDir + "BS Constrained Mass (GeV)", 100, 0, 0.200);
        IHistogram1D bsconVx = aida.histogram1D(plotDir + "BS Constrained Vx (mm)", 50, -1, 1);
        IHistogram1D bsconVy = aida.histogram1D(plotDir + "BS Constrained Vy (mm)", 50, -1, 1);
        IHistogram1D bsconVz = aida.histogram1D(plotDir + "BS Constrained Vz (mm)", 50, -10, 10);
        IHistogram1D bsconChi2 = aida.histogram1D(plotDir + "BS Constrained Chi2", 25, 0, 25);
        /* target constrained */
        IHistogram1D tarconMass = aida.histogram1D(plotDir + "Target Constrained Mass (GeV)", 100, 0, 0.200);
        IHistogram1D tarconVx = aida.histogram1D(plotDir + "Target Constrained Vx (mm)", 50, -1, 1);
        IHistogram1D tarconVy = aida.histogram1D(plotDir + "Target Constrained Vy (mm)", 50, -1, 1);
        IHistogram1D tarconVz = aida.histogram1D(plotDir + "Target Constrained Vz (mm)", 50, -10, 10);
        IHistogram1D tarconChi2 = aida.histogram1D(plotDir + "Target Constrained Chi2", 25, 0, 25);

    }

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */
        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName))
            return;
        if (!event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName))
            return;
        if (!event.hasCollection(ReconstructedParticle.class, beamConV0CandidatesColName))
            return;
        if (!event.hasCollection(ReconstructedParticle.class, targetV0ConCandidatesColName))
            return;
        nRecoEvents++;

        List<ReconstructedParticle> beamConstrainedV0List = event.get(ReconstructedParticle.class, beamConV0CandidatesColName);
        for (ReconstructedParticle bsV0 : beamConstrainedV0List) {
            Vertex bsVert = bsV0.getStartVertex();
            aida.histogram1D(plotDir + "BS Constrained Vx (mm)").fill(bsVert.getPosition().x());
            aida.histogram1D(plotDir + "BS Constrained Vy (mm)").fill(bsVert.getPosition().y());
            aida.histogram1D(plotDir + "BS Constrained Vz (mm)").fill(bsVert.getPosition().z());
            aida.histogram1D(plotDir + "BS Constrained Mass (GeV)").fill(bsV0.getMass());
            aida.histogram1D(plotDir + "BS Constrained Chi2").fill(bsVert.getChi2());            
        }
        
          List<ReconstructedParticle> targetConstrainedV0List = event.get(ReconstructedParticle.class, targetV0ConCandidatesColName);
          System.out.println("Number of V0s = " + targetConstrainedV0List.size());
          for (ReconstructedParticle tarV0 : targetConstrainedV0List) {
            Vertex tarVert = tarV0.getStartVertex();
            System.out.println(tarVert.toString());
            aida.histogram1D(plotDir + "Target Constrained Vx (mm)").fill(tarVert.getPosition().x());
            aida.histogram1D(plotDir + "Target Constrained Vy (mm)").fill(tarVert.getPosition().y());
            aida.histogram1D(plotDir + "Target Constrained Vz (mm)").fill(tarVert.getPosition().z());
            aida.histogram1D(plotDir + "Target Constrained Mass (GeV)").fill(tarV0.getMass());
            aida.histogram1D(plotDir + "Target Constrained Chi2").fill(tarVert.getChi2());            
            System.out.println("Target Constrained chi^2 = "+tarVert.getChi2());
        }
    }

    @Override
    public void dumpDQMData() {
        System.out.println("ReconMonitoring::endOfData filling DQM database");
    }

    @Override
    public void printDQMData() {
        System.out.println("V0Monitoring::printDQMData");
        for (Entry<String, Double> entry : monitoredQuantityMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        System.out.println("*******************************");
    }

    /**
     * Calculate the averages here and fill the map
     */
    @Override
    public void calculateEndOfRunQuantities() {
        monitoredQuantityMap.put(fpQuantNames[0], (double) nTotEle / nRecoEvents);
        monitoredQuantityMap.put(fpQuantNames[1], (double) nTotPos / nRecoEvents);
        monitoredQuantityMap.put(fpQuantNames[2], (double) nTotPhotons / nRecoEvents);
        monitoredQuantityMap.put(fpQuantNames[3], (double) nTotUnAss / nRecoEvents);
        monitoredQuantityMap.put(fpQuantNames[4], (double) sumdelX / nTotAss);
        monitoredQuantityMap.put(fpQuantNames[5], (double) sumdelY / nTotAss);
        monitoredQuantityMap.put(fpQuantNames[6], (double) sumEoverP / nTotAss);
    }

    @Override
    public void printDQMStrings() {
        for (int i = 0; i < 7; i++)//TODO:  do this in a smarter way...loop over the map
            System.out.println(fpQuantNames[i]);
    }

}
