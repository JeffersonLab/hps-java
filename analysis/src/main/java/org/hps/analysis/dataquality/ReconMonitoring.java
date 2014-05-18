package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
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
public class ReconMonitoring extends DataQualityMonitor {

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

    protected void detectorChanged(Detector detector) {
        System.out.println("ReconMonitoring::detectorChanged  Setting up the plotter");
        aida.tree().cd("/");

        /*  Final State Particle Quantities   */
        /*  plot electron & positron momentum separately  */
        IHistogram1D elePx = aida.histogram1D("Electron Px (GeV)", 25, -0.1, 0.200);
        IHistogram1D elePy = aida.histogram1D("Electron Py (GeV)", 25, -0.1, 0.1);
        IHistogram1D elePz = aida.histogram1D("Electron Pz (GeV)", 25, 0, 2.4);

        IHistogram1D posPx = aida.histogram1D("Positron Px (GeV)", 25, -0.1, 0.200);
        IHistogram1D posPy = aida.histogram1D("Positron Py (GeV)", 25, -0.1, 0.1);
        IHistogram1D posPz = aida.histogram1D("Positron Pz (GeV)", 25, 0, 2.4);
        /*  photon quanties (...right now, just unassociated clusters) */
        IHistogram1D nPhotonsHisto = aida.histogram1D("Number of photons per event", 10, 0, 10);
        IHistogram1D enePhoton = aida.histogram1D("Photon Energy (GeV)", 25, 0, 2.4);
        IHistogram1D xPhoton = aida.histogram1D("Photon X position (mm)", 25, -100, 100);
        IHistogram1D yPhoton = aida.histogram1D("Photon Y position (mm)", 25, -100, 100);

        /*  tracks with associated clusters */
        IHistogram1D eneOverp = aida.histogram1D("Cluster Energy Over TrackMomentum", 25, 0, 2.0);
        IHistogram1D deltaXAtCal = aida.histogram1D("delta X @ ECal (mm)", 25, -10, 10.0);
        IHistogram1D deltaYAtCal = aida.histogram1D("delta Y @ ECal (mm)", 25, -10, 10.0);
        /* number of unassocaited tracks */
        IHistogram1D nUnAssTracksHisto = aida.histogram1D("Number of unassociated tracks per event", 10, 0, 10);
    }

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */
        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName))
            return;
//        if (!event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName))
//            return;
//        if (!event.hasCollection(ReconstructedParticle.class, beamConV0CandidatesColName))
//            return;
//        if (!event.hasCollection(ReconstructedParticle.class, targetV0ConCandidatesColName))
//            return;
        nRecoEvents++;
        int nPhotons = 0;  //number of photons 
        int nUnAssTracks = 0; //number of tracks w/o clusters
        List<ReconstructedParticle> finalStateParticles = event.get(ReconstructedParticle.class, finalStateParticlesColName);
//        System.out.println("This events has " + finalStateParticles.size() + " final state particles");
        for (ReconstructedParticle fsPart : finalStateParticles) {
            // Extrapolate the track to the Ecal cluster position
            boolean isPhoton = false;
            boolean hasCluster = true;
            Track fsTrack = null;
            Cluster fsCluster = null;
            //TODO:  mg-May 14, 2014 use PID to do this instead...not sure if that's implemented yet
            if (fsPart.getTracks().size() == 1)//should always be 1 or zero for final state particles
                fsTrack = fsPart.getTracks().get(0);
            else
                isPhoton = true;
            //get the cluster
            if (fsPart.getClusters().size() == 1)
                fsCluster = fsPart.getClusters().get(0);
            else
                hasCluster = false;

            //deal with electrons & positrons first
            if (!isPhoton) {
                double charge = fsPart.getCharge();
                Hep3Vector mom = fsPart.getMomentum();
                if (charge < 0) {
                    nTotEle++;
                    aida.histogram1D("Electron Px (GeV)").fill(mom.x());
                    aida.histogram1D("Electron Py (GeV)").fill(mom.y());
                    aida.histogram1D("Electron Pz (GeV)").fill(mom.z());
                } else {
                    nTotPos++;
                    aida.histogram1D("Positron Px (GeV)").fill(mom.x());
                    aida.histogram1D("Positron Py (GeV)").fill(mom.y());
                    aida.histogram1D("Positron Pz (GeV)").fill(mom.z());
                }

            }
            //now, the photons
            if (isPhoton) {
                double ene = fsPart.getEnergy();
                //TODO:  mg-May 14, 2014....I would like to do this!!!!
                //double xpos = fsCluster.getPositionAtShowerMax(false)[0];// false-->assume a photon instead of electron from calculating shower depth
                //double ypos = fsCluster.getPositionAtShowerMax(false)[1];
                //but I can't because ReconParticles don't know about HPSEcalClusters, and casting it as one doesn't seem to work
                Hep3Vector clusterPosition = new BasicHep3Vector(fsCluster.getPosition());
                double xpos = clusterPosition.x();
                double ypos = clusterPosition.y();
                nPhotons++;
                nTotPhotons++;
                aida.histogram1D("Photon Energy (GeV)").fill(ene);
                aida.histogram1D("Photon X position (mm)").fill(xpos);
                aida.histogram1D("Photon Y position (mm)").fill(ypos);
            }

            if (hasCluster && !isPhoton) {
                nTotAss++;
                Hep3Vector mom = fsPart.getMomentum();
                double ene = fsPart.getEnergy();
                double eOverP = ene / mom.magnitude();
                Hep3Vector clusterPosition = new BasicHep3Vector(fsCluster.getPosition());//this gets position at shower max assuming it's an electron/positron
                Hep3Vector trackPosAtEcal = TrackUtils.extrapolateTrack(fsTrack, clusterPosition.z());
                double dx = trackPosAtEcal.y() - clusterPosition.x();//remember track vs detector coords
                double dy = trackPosAtEcal.z() - clusterPosition.y();//remember track vs detector coords
                sumdelX += dx;
                sumdelY += dy;
                sumEoverP += eOverP;

                aida.histogram1D("Cluster Energy Over TrackMomentum").fill(eOverP);
                aida.histogram1D("delta X @ ECal (mm)").fill(dx);
                aida.histogram1D("delta Y @ ECal (mm)").fill(dy);
            }
            if (!hasCluster) {//if there is no cluster, can't be a track or else it wouldn't be in list
                nUnAssTracks++; //count per event
                nTotUnAss++; //and keep a running total for averaging
            }
        }
        aida.histogram1D("Number of unassociated tracks per event").fill(nUnAssTracks);
        aida.histogram1D("Number of photons per event").fill(nPhotons);     
    }

    @Override
    public void dumpDQMData() {
        System.out.println("ReconMonitoring::endOfData filling DQM database");
    }

    @Override
    public void printDQMData() {
         System.out.println("ReconMonitoring::printDQMData");      
        for(Entry<String,Double> entry:  monitoredQuantityMap.entrySet()){
            System.out.println(entry.getKey()+" = "+entry.getValue());
        }      
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
        for (int i = 0; i < 7; i++) {//TODO:  do this in a smarter way...loop over the map
            System.out.println(fpQuantNames[i]);
        }
    }

}
