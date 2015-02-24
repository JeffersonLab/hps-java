package org.hps;

import java.io.File;
import java.io.IOException;
import static java.lang.Math.sqrt;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

public class APrimeReconTest extends TestCase
{

    static final int nEvents = 1000;

    public void testSimpleMCReconTest() throws Exception
    {

        new TestOutputFile(this.getClass().getSimpleName()).mkdir();

        FileCache cache = new FileCache();
        String fileName = "ap2.2gev150mev_slic-3.1.5_geant4-v9r6p1_QGSP_BERT_HPS-Proposal2014-v8-2pt2-42-1.slcio";
//        fileName = "ap2.2gev150mev_slic-3.1.5_geant4-v9r6p1_QGSP_BERT_HPS-Proposal2014-v8-2pt2.slcio";
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/APrimeReconTest/" + fileName));
        // Run the reconstruction.
        JobManager job = new JobManager();
        DatabaseConditionsManager.getInstance().setLogLevel(Level.WARNING);
        File outputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName() + "_recon");
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addInputFile(inputFile);
        job.setup("/org/hps/steering/recon/SimpleMCRecon.lcsim");
        job.setNumberOfEvents(nEvents);
        job.run();

        // Read in the LCIO event file and print out summary information.
        System.out.println("Running ReconCheckDriver on output ...");
        LCSimLoop loop = new LCSimLoop();
        loop.add(new ReconCheckDriver());
        try {
            loop.setLCIORecordSource(new File(outputFile.getPath() + ".slcio"));
            loop.loop(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("Done!");

    }

    static class ReconCheckDriver extends Driver
    {

//        int nClusters;
//        int nTrackerHits;
//        int nSimCalorimeterHits;
//        int nCalorimeterHits;
//        int nReconstructedParticles;
//        int nEvents;
        public void process(EventHeader event)
        {
            int nTracks = 0;

            if (event.hasCollection(Track.class, "MatchedTracks")) {
                nTracks = event.get(Track.class, "MatchedTracks").size();
            }
            //           if(nTracks < 2) throw new Driver.NextEventException();
            assertEquals("Didn't find two tracks", 2, nTracks);
            int nClusters = event.get(Cluster.class, "EcalClusters").size();
            assertTrue("Didn't find at least two clusters", nClusters > 2);
            int nReconstructedParticles = event.get(ReconstructedParticle.class, "FinalStateParticles").size();
            System.out.println("nTracks = " + nTracks + " nClusters = " + nClusters + " NRP = " + nReconstructedParticles);

            List<ReconstructedParticle> rps = event.get(ReconstructedParticle.class, "FinalStateParticles");
            int nrp = rps.size();
            assertTrue("Didn't find at least two particles", nrp > 2);
            int nelectron = 0;
            int npositron = 0;

            ReconstructedParticle electron = null;
            ReconstructedParticle positron = null;
            for (ReconstructedParticle p : rps) {
                if (p.getParticleIDUsed().getPDG() == 11) {
                    electron = p;
                    ++nelectron;
                }
                if (p.getParticleIDUsed().getPDG() == -11) {
                    positron = p;
                    ++npositron;
                }
            }
            assertEquals("didn't find one electron", 1, nelectron);
            assertEquals("didn't find one positron", 1, npositron);

            //TODO fix this, as there may be more electrons in the generator final state list.
            //TODO get the track hits, check their MC parentage, check their purity.
            MCParticle electronMC = null;
            MCParticle positronMC = null;
            List<MCParticle> mcps = event.getMCParticles();
            for(MCParticle mcp : mcps)
            {
                if(mcp.getPDGID() == 11 && mcp.getGeneratorStatus()==MCParticle.FINAL_STATE) electronMC = mcp;
                if(mcp.getPDGID() == -11 && mcp.getGeneratorStatus()==MCParticle.FINAL_STATE) positronMC = mcp;
                
            }
            checkParticle(electron, electronMC);
            checkParticle(positron, positronMC);

//            if (event.hasCollection(Cluster.class, "EcalClusters")) {
//                nClusters += event.get(Cluster.class, "EcalClusters").size();
//            }
//            if (event.hasCollection(TrackerHit.class, "RotatedHelicalTrackHits")) {
//                nTrackerHits += event.get(TrackerHit.class, "RotatedHelicalTrackHits").size();
//            }
//            System.out.println("event: " + event.getEventNumber() + " : " + event.get(TrackerHit.class, "RotatedHelicalTrackHits").size() + " hits :" + event.get(Track.class, "MatchedTracks").size() + " tracks");
//
//            if (event.hasCollection(SimCalorimeterHit.class, "EcalHits")) {
//                nSimCalorimeterHits += event.get(SimCalorimeterHit.class, "EcalHits").size();
//            }
//            if (event.hasCollection(CalorimeterHit.class, "EcalCalHits")) {
//                nCalorimeterHits += event.get(CalorimeterHit.class, "EcalCalHits").size();
//            }
//            if (event.hasCollection(ReconstructedParticle.class, "FinalStateParticles")) {
//                nReconstructedParticles += event.get(ReconstructedParticle.class, "FinalStateParticles").size();
//            }
        }

        private void checkParticle(ReconstructedParticle p, MCParticle mcp)
        {
            assertEquals(1, p.getTracks().size());
            assertEquals(1, p.getClusters().size());
            Track t = p.getTracks().get(0);
            BaseTrackState state = (BaseTrackState) t.getTrackStates().get(0);
            //TODO fix this
            double[] mom = state.computeMomentum(-.5);
            double tmom = sqrt(mom[0] * mom[0] + mom[1] * mom[1] + mom[2] * mom[2]);
            Cluster c = p.getClusters().get(0);
            double energy = c.getEnergy();
            System.out.println("tracks momentum: " + tmom + " cluster energy " + energy +" E/p : " + (energy/tmom));
            //TODO assert e/p be within range

            double mcMomentum = sqrt(mcp.getPX()*mcp.getPX() + mcp.getPY()*mcp.getPY() +mcp.getPZ()*mcp.getPZ());
            double mcEnergy = mcp.getEnergy();
            System.out.println("mcMomentum "+mcMomentum + " mcEneregy "+mcEnergy);
            //TODO check that momentum and energy are within range
            assertEquals("Track momentum does not match MC particle momentum", mcMomentum, tmom, .5);
            assertEquals("Cluster energy does not match MC particle energy", mcEnergy, energy, .5);
            
        }

        public void endOfData()
        {
//            System.out.println("ReconCheckDriver results ...");
//            System.out.println("  nEvents: " + nEvents);
//            System.out.println("  nSimCalorimeterHits: " + nSimCalorimeterHits);
//            System.out.println("  nClusters: " + nClusters);
//            System.out.println("  nTrackerHits: " + nTrackerHits);
//            System.out.println("  nTracks: " + nTracks);
//            System.out.println("  nReconstructedParticles: " + nReconstructedParticles);
//            System.out.println("  < nTracks / nEvents > = " + (double) nTracks / (double) nEvents);
//            System.out.println("  < nClusters / nEvents > = " + (double) nClusters / (double) nEvents);
        }
    }
}
