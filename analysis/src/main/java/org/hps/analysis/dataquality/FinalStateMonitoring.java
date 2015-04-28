package org.hps.analysis.dataquality;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.TIData;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
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
 * a problem with ReconParticle
 *
 * May 20, 2014: this was fixed by a) Omar's changes to ReconParticle and
 * b) making sure I run ECal clustering before this
 *
 *
 */
public class FinalStateMonitoring extends DataQualityMonitor {

    String finalStateParticlesColName = "FinalStateParticles";

    String[] fpQuantNames = {"nEle_per_Event", "nPos_per_Event", "nPhoton_per_Event", "nUnAssociatedTracks_per_Event", "avg_delX_at_ECal", "avg_delY_at_ECal", "avg_E_Over_P", "avg_mom_beam_elec", "sig_mom_beam_elec"};
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
    private String plotDir = "FinalStateParticles/";
    double beamEnergy = 1.05; //GeV
    double maxFactor = 1.5;
    double feeMomentumCut = 0.8; //GeV
   

    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("FinalStateMonitoring::detectorChanged  Setting up the plotter");
        aida.tree().cd("/");

        /*  Final State Particle Quantities   */
        /*  plot electron & positron momentum separately  */
        IHistogram1D elePx = aida.histogram1D(plotDir + triggerType + "/" + "Electron Px (GeV)", 50, -0.1, 0.200);
        IHistogram1D elePy = aida.histogram1D(plotDir + triggerType + "/" + "Electron Py (GeV)", 50, -0.1, 0.1);
        IHistogram1D elePz = aida.histogram1D(plotDir + triggerType + "/" + "Electron Pz (GeV)", 50, 0, beamEnergy * maxFactor);
        IHistogram1D elePzBeam = aida.histogram1D(plotDir + triggerType + "/" + "Beam Electrons Pz (GeV)", 50, feeMomentumCut, beamEnergy * maxFactor);

        IHistogram1D posPx = aida.histogram1D(plotDir + triggerType + "/" + "Positron Px (GeV)", 50, -0.1, 0.200);
        IHistogram1D posPy = aida.histogram1D(plotDir + triggerType + "/" + "Positron Py (GeV)", 50, -0.1, 0.1);
        IHistogram1D posPz = aida.histogram1D(plotDir + triggerType + "/" + "Positron Pz (GeV)", 50, 0, beamEnergy * maxFactor);
        /*  photon quanties (...right now, just unassociated clusters) */
        IHistogram1D nPhotonsHisto = aida.histogram1D(plotDir + triggerType + "/" + "Number of photons per event", 15, 0, 15);
        IHistogram1D enePhoton = aida.histogram1D(plotDir + triggerType + "/" + "Photon Energy (GeV)", 50, 0, 2.4);
        IHistogram1D xPhoton = aida.histogram1D(plotDir + triggerType + "/" + "Photon X position (mm)", 50, -200, 200);
        IHistogram1D yPhoton = aida.histogram1D(plotDir + triggerType + "/" + "Photon Y position (mm)", 50, -100, 100);

        /*  tracks with associated clusters */
        IHistogram1D eneOverp = aida.histogram1D(plotDir + triggerType + "/" + "Cluster Energy Over TrackMomentum", 50, 0, 2.0);
        IHistogram1D deltaXAtCal = aida.histogram1D(plotDir + triggerType + "/" + "delta X @ ECal (mm)", 50, -50, 50.0);
        IHistogram1D deltaYAtCal = aida.histogram1D(plotDir + triggerType + "/" + "delta Y @ ECal (mm)", 50, -50, 50.0);
        //IHistogram2D trackXvsECalX = aida.histogram2D(plotDir + triggerType + "/" + "track X vs ECal X", 50, -300, 300.0, 50, -300, 300.0);
        //IHistogram2D trackYvsECalY = aida.histogram2D(plotDir + triggerType + "/" + "track Y vs ECal Y", 50, -100, 100.0, 50, -100, 100.0);
        IHistogram2D trackPvsECalE = aida.histogram2D(plotDir + triggerType + "/" + "track mom vs ECal E", 50, 0.1, beamEnergy * maxFactor, 50, 0.1, beamEnergy * maxFactor);
        /* number of unassocaited tracks/event */
        IHistogram1D nUnAssTracksHisto = aida.histogram1D(plotDir + triggerType + "/" + "Number of unassociated tracks per event", 5, 0, 5);
    }

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */

        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName)) {
            if (debug)
                System.out.println(finalStateParticlesColName + " collection not found???");
            return;
        }

        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            for (GenericObject data : triggerList)
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG) {
                    TIData triggerData = new TIData(data);
                    if (!matchTriggerType(triggerData))//only process singles0 triggers...
                        return;
                }
        } else
            System.out.println(this.getClass().getSimpleName() + ":  No trigger bank found...running over all trigger types");

        nRecoEvents++;
        int nPhotons = 0;  //number of photons 
        int nUnAssTracks = 0; //number of tracks w/o clusters
        List<ReconstructedParticle> finalStateParticles = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        if (debug)
            System.out.println("This events has " + finalStateParticles.size() + " final state particles");
        for (ReconstructedParticle fsPart : finalStateParticles) {
            if (debug)
                System.out.println("PDGID = " + fsPart.getParticleIDUsed() + "; charge = " + fsPart.getCharge() + "; pz = " + fsPart.getMomentum().x());

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
                    aida.histogram1D(plotDir + triggerType + "/" + "Electron Px (GeV)").fill(mom.x());
                    aida.histogram1D(plotDir + triggerType + "/" + "Electron Py (GeV)").fill(mom.y());
                    aida.histogram1D(plotDir + triggerType + "/" + "Electron Pz (GeV)").fill(mom.z());
                    aida.histogram1D(plotDir + triggerType + "/" + "Beam Electrons Pz (GeV)").fill(mom.z());
                } else {
                    nTotPos++;
                    aida.histogram1D(plotDir + triggerType + "/" + "Positron Px (GeV)").fill(mom.x());
                    aida.histogram1D(plotDir + triggerType + "/" + "Positron Py (GeV)").fill(mom.y());
                    aida.histogram1D(plotDir + triggerType + "/" + "Positron Pz (GeV)").fill(mom.z());
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
                aida.histogram1D(plotDir + triggerType + "/" + "Photon Energy (GeV)").fill(ene);
                aida.histogram1D(plotDir + triggerType + "/" + "Photon X position (mm)").fill(xpos);
                aida.histogram1D(plotDir + triggerType + "/" + "Photon Y position (mm)").fill(ypos);
            }

            if (hasCluster && !isPhoton) {
                nTotAss++;
                Hep3Vector mom = fsPart.getMomentum();
                double ene = fsPart.getEnergy();
                double eOverP = ene / mom.magnitude();
                Hep3Vector clusterPosition = new BasicHep3Vector(fsCluster.getPosition());//this gets position at shower max assuming it's an electron/positron
                Hep3Vector trackPosAtEcal = TrackUtils.extrapolateTrack(fsTrack, clusterPosition.z());
                double dx = trackPosAtEcal.x() - clusterPosition.x();//remember track vs detector coords
                double dy = trackPosAtEcal.y() - clusterPosition.y();//remember track vs detector coords

                sumdelX += dx;
                sumdelY += dy;
                sumEoverP += eOverP;

                aida.histogram1D(plotDir + triggerType + "/" + "Cluster Energy Over TrackMomentum").fill(eOverP);
                aida.histogram1D(plotDir + triggerType + "/" + "delta X @ ECal (mm)").fill(dx);
                aida.histogram1D(plotDir + triggerType + "/" + "delta Y @ ECal (mm)").fill(dy);
                /* here are some plots for debugging track-cluster matching */
                // aida.histogram2D(plotDir + triggerType + "/" + "track X vs ECal X").fill(trackPosAtEcal.x(), clusterPosition.x());
                // aida.histogram2D(plotDir + triggerType + "/" + "track Y vs ECal Y").fill(trackPosAtEcal.y(), clusterPosition.y());
                aida.histogram2D(plotDir + triggerType + "/" + "track mom vs ECal E").fill(fsPart.getMomentum().magnitude(), fsPart.getEnergy());
                //          if(dy<-20)
                //              System.out.println("Big deltaY...")

            }
            if (!hasCluster) {//if there is no cluster, can't be a track or else it wouldn't be in list
                nUnAssTracks++; //count per event
                nTotUnAss++; //and keep a running total for averaging
            }
        }
        aida.histogram1D(plotDir + triggerType + "/" + "Number of unassociated tracks per event").fill(nUnAssTracks);
        aida.histogram1D(plotDir + triggerType + "/" + "Number of photons per event").fill(nPhotons);
    }

    @Override
    public void printDQMData() {
        System.out.println("FinalStateMonitoring::printDQMData");
        for (Entry<String, Double> entry : monitoredQuantityMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        System.out.println("*******************************");
    }

    /**
     * Calculate the averages here and fill the map
     */
    @Override
    public void calculateEndOfRunQuantities() {
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter fitter = fitFactory.createFitter("chi2");
        IHistogram1D beamE = aida.histogram1D(plotDir + triggerType + "/" + "Beam Electrons Pz (GeV)");
        IFitResult result = fitBeamEnergyPeak(beamE, fitter, "range=\"(-10.0,10.0)\"");
        double[] pars = result.fittedParameters();
        for (int i = 0; i < 5; i++)
            System.out.println("Beam Energy Peak:  " + result.fittedParameterNames()[i] + " = " + pars[i]);

        monitoredQuantityMap.put(fpQuantNames[0], (double) nTotEle / nRecoEvents);
        monitoredQuantityMap.put(fpQuantNames[1], (double) nTotPos / nRecoEvents);
        monitoredQuantityMap.put(fpQuantNames[2], (double) nTotPhotons / nRecoEvents);
        monitoredQuantityMap.put(fpQuantNames[3], (double) nTotUnAss / nRecoEvents);
        monitoredQuantityMap.put(fpQuantNames[4], (double) sumdelX / nTotAss);
        monitoredQuantityMap.put(fpQuantNames[5], (double) sumdelY / nTotAss);
        monitoredQuantityMap.put(fpQuantNames[6], (double) sumEoverP / nTotAss);
        monitoredQuantityMap.put(fpQuantNames[7], (double) pars[1]);
        monitoredQuantityMap.put(fpQuantNames[8], (double) pars[2]);
        IPlotter plotter = analysisFactory.createPlotterFactory().create("Beam Energy Electrons");

        IPlotterStyle pstyle = plotter.style();
        pstyle.legendBoxStyle().setVisible(false);
        pstyle.dataStyle().fillStyle().setColor("green");
        pstyle.dataStyle().lineStyle().setColor("black");
        plotter.region(0).plot(beamE);
        plotter.region(0).plot(result.fittedFunction());
        if (outputPlots)
            try {
                plotter.writeToFile(outputPlotDir + "beamEnergyElectrons.png");
            } catch (IOException ex) {
                Logger.getLogger(FinalStateMonitoring.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    @Override
    public void printDQMStrings() {
        for (int i = 0; i < 9; i++)//TODO:  do this in a smarter way...loop over the map
            System.out.println("ALTER TABLE dqm ADD " + fpQuantNames[i] + " double;");
    }

    IFitResult fitBeamEnergyPeak(IHistogram1D h1d, IFitter fitter, String range) {
//        return fitter.fit(h1d, "g", range);

//        return fitter.fit(h1d, "g+p1", init, range);
        double[] init = {20.0, 2.2, 0.12, 10, 0.0};
//        double[] init = {20.0, 2.2, 0.1};
        return fitter.fit(h1d, "g+p1", init);
    }

  

}
