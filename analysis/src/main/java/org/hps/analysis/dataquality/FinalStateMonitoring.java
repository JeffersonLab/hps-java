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

import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
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
 * May 20, 2014: this was fixed by a) Omar's changes to ReconParticle and b)
 * making sure I run ECal clustering before this
 *
 *
 */
public class FinalStateMonitoring extends DataQualityMonitor {
    
    private static Logger LOGGER = Logger.getLogger(FinalStateMonitoring.class.getPackage().getName());

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
    private final String plotDir = "FinalStateParticles/";
   // double beamEnergy = 1.05; //GeV
    
    IHistogram1D elePx;
    IHistogram1D elePy;
    IHistogram1D elePz;
    IHistogram1D elePzBeam;
    IHistogram1D elePzBeamTop;
    IHistogram1D elePzBeamBottom;
    IHistogram1D elePTop;
    IHistogram1D elePBottom;

    IHistogram1D posPx;
    IHistogram1D posPy;
    IHistogram1D posPz;
    IHistogram1D posPTop;
    IHistogram1D posPBottom;

    /*  photon quanties (...right now, just unassociated clusters) */
    IHistogram1D nPhotonsHisto;
    IHistogram1D enePhoton;
    IHistogram1D xPhoton;
    IHistogram1D yPhoton;

    /*  tracks with associated clusters */
    IHistogram1D eneOverp;
    IHistogram1D deltaXAtCal;
    IHistogram1D deltaYAtCal;
//    IHistogram2D trackXvsECalX;
//    IHistogram2D trackYvsECalY;
    IHistogram2D trackPvsECalE;
    IHistogram2D trackTvsECalT;
    IHistogram1D timeMatchDeltaT;
    /* number of unassocaited tracks/event */
    IHistogram1D nUnAssTracksHisto;
    
    
   
    public void setFinalStateParticlesColName(String fsp) {
        this.finalStateParticlesColName = fsp;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        super.detectorChanged(detector);
        double maxFactor = 1.5;
        double feeMomentumCut = 0.75; //this number, multiplied by the beam energy, is the actual cut

        
        LOGGER.info("Setting up the plotter");
        aida.tree().cd("/");
          String trkType="SeedTrack/";
        if(isGBL)
            trkType="GBLTrack/";
       
        
        /*  Final State Particle Quantities   */
        /*  plot electron & positron momentum separately  */
        elePx = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Electron Px (GeV)", 100, -0.1*beamEnergy, 0.200*beamEnergy);
        elePy = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Electron Py (GeV)", 100, -0.1*beamEnergy, 0.1*beamEnergy);
        elePz = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Electron Pz (GeV)", 100, 0, beamEnergy * maxFactor);
        elePzBeam = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Beam Electrons Total P (GeV)", 100, feeMomentumCut*beamEnergy, beamEnergy * maxFactor);
        elePzBeamTop = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Beam Electrons Total P (GeV):  Top", 100, feeMomentumCut*beamEnergy, beamEnergy * maxFactor);
        elePzBeamBottom = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Beam Electrons Total P (GeV):  Bottom", 100, feeMomentumCut*beamEnergy, beamEnergy * maxFactor);
        elePTop = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Electron Total P (GeV):  Top", 100, 0, beamEnergy * maxFactor);
        elePBottom = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Electron Total P (GeV):  Bottom", 100, 0, beamEnergy * maxFactor);

        posPx = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Positron Px (GeV)", 50, -0.1*beamEnergy, 0.200*beamEnergy);
        posPy = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Positron Py (GeV)", 50, -0.1*beamEnergy, 0.1*beamEnergy);
        posPz = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Positron Pz (GeV)", 50, 0, beamEnergy * maxFactor);
        posPTop = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Positron Total P (GeV):  Top", 100, 0, beamEnergy * maxFactor);
        posPBottom = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Positron Total P (GeV):  Bottom", 100, 0, beamEnergy * maxFactor);

        /*  photon quanties (...right now, just unassociated clusters) */
        nPhotonsHisto = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Number of photons per event", 15, 0, 15);
        enePhoton = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Photon Energy (GeV)", 50, 0, 2.4*beamEnergy);
        xPhoton = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Photon X position (mm)", 50, -200, 200);
        yPhoton = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Photon Y position (mm)", 50, -100, 100);

        /*  tracks with associated clusters */
        eneOverp = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Cluster Energy Over TrackMomentum", 50, 0, 2.0);
        deltaXAtCal = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "delta X @ ECal (mm)", 50, -50, 50.0);
        deltaYAtCal = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "delta Y @ ECal (mm)", 50, -50, 50.0);
//        trackXvsECalX = aida.histogram2D(plotDir +trkType+ triggerType + "/" + "track X vs ECal X", 50, -300, 300.0, 50, -300, 300.0);
//        trackYvsECalY = aida.histogram2D(plotDir +trkType+ triggerType + "/" + "track Y vs ECal Y", 50, -100, 100.0, 50, -100, 100.0);
        trackPvsECalE = aida.histogram2D(plotDir +trkType+ triggerType + "/" + "track mom vs ECal E", 50, 0.1, beamEnergy * maxFactor, 50, 0.1, beamEnergy * maxFactor);
        trackTvsECalT = aida.histogram2D(plotDir +trkType+ triggerType + "/" + "track T vs ECal T", 200, 0.0, 200.0, 100, -25.0, 25.0);
        timeMatchDeltaT = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "ECal T minus track T", 200, -25, 175);
        /* number of unassocaited tracks/event */
        nUnAssTracksHisto = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Number of unassociated tracks per event", 5, 0, 5);
    }

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */

        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName)) {
            if (debug) {
                LOGGER.info(finalStateParticlesColName + " collection not found???");
            }
            return;
        }

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        //check to see if this event is from the correct trigger (or "all");
        if (!matchTrigger(event)) {
            return;
        }

        nRecoEvents++;
        int nPhotons = 0;  //number of photons 
        int nUnAssTracks = 0; //number of tracks w/o clusters
        List<ReconstructedParticle> finalStateParticles = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        if (debug) {
            LOGGER.info("This events has " + finalStateParticles.size() + " final state particles");
        }
        for (ReconstructedParticle fsPart : finalStateParticles) {
            if (debug) {
                LOGGER.info("PDGID = " + fsPart.getParticleIDUsed() + "; charge = " + fsPart.getCharge() + "; pz = " + fsPart.getMomentum().x());
            }
          if (isGBL != TrackType.isGBL(fsPart.getType()))
                continue;
            // Extrapolate the track to the Ecal cluster position
            boolean isPhoton = false;
            boolean hasCluster = true;
            Track fsTrack = null;
            Cluster fsCluster = null;
            //TODO:  mg-May 14, 2014 use PID to do this instead...not sure if that's implemented yet
            if (fsPart.getTracks().size() == 1)//should always be 1 or zero for final state particles
            {
                fsTrack = fsPart.getTracks().get(0);
            } else {
                isPhoton = true;
            }
            //get the cluster
            if (fsPart.getClusters().size() == 1) {
                fsCluster = fsPart.getClusters().get(0);
            } else {
                hasCluster = false;
            }

            //deal with electrons & positrons first
            if (!isPhoton) {
                double charge = fsPart.getCharge();
                Hep3Vector mom = fsPart.getMomentum();
                if (charge < 0) {
                    nTotEle++;
                    elePx.fill(mom.x());
                    elePy.fill(mom.y());
                    elePz.fill(mom.z());
                    elePzBeam.fill(mom.magnitude());
                    if (mom.y() > 0) {
                        elePzBeamTop.fill(mom.magnitude());
                        elePTop.fill(mom.magnitude());
                    } else {
                        elePzBeamBottom.fill(mom.magnitude());
                        elePBottom.fill(mom.magnitude());
                    }
                } else {
                    nTotPos++;
                    posPx.fill(mom.x());
                    posPy.fill(mom.y());
                    posPz.fill(mom.z());
                    if (mom.y() > 0) {
                        posPTop.fill(mom.magnitude());
                    } else {
                        posPBottom.fill(mom.magnitude());
                    }
                }

            }
            //now, the photons
            if (isPhoton) {
                if (fsCluster == null) {
                    throw new RuntimeException("isPhoton==true but no cluster found: should never happen");
                }
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
                enePhoton.fill(ene);
                xPhoton.fill(xpos);
                yPhoton.fill(ypos);
            }

            if (hasCluster && !isPhoton) {
                if (fsCluster == null) {
                    throw new RuntimeException("hasCluster==true but no cluster found: should never happen");
                }
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

                eneOverp.fill(eOverP);
                deltaXAtCal.fill(dx);
                deltaYAtCal.fill(dy);
                /* here are some plots for debugging track-cluster matching */
//                trackXvsECalX.fill(trackPosAtEcal.x(), clusterPosition.x());
//                trackYvsECalY.fill(trackPosAtEcal.y(), clusterPosition.y());
                trackPvsECalE.fill(fsPart.getMomentum().magnitude(), fsPart.getEnergy());
                trackTvsECalT.fill(ClusterUtilities.getSeedHitTime(fsCluster), TrackUtils.getTrackTime(fsTrack, hitToStrips, hitToRotated));
                timeMatchDeltaT.fill(ClusterUtilities.getSeedHitTime(fsCluster) - TrackUtils.getTrackTime(fsTrack, hitToStrips, hitToRotated));
                //          if(dy<-20)
                //              LOGGER.info("Big deltaY...")

            }
            if (!hasCluster) {//if there is no cluster, can't be a track or else it wouldn't be in list
                nUnAssTracks++; //count per event
                nTotUnAss++; //and keep a running total for averaging
            }
        }
        nUnAssTracksHisto.fill(nUnAssTracks);
        nPhotonsHisto.fill(nPhotons);
    }

    @Override
    public void printDQMData() {
        LOGGER.info("FinalStateMonitoring::printDQMData");
        for (Entry<String, Double> entry : monitoredQuantityMap.entrySet()) {
            LOGGER.info(entry.getKey() + " = " + entry.getValue());
        }
        LOGGER.info("*******************************");
    }

    /**
     * Calculate the averages here and fill the map
     */
    @Override
    public void calculateEndOfRunQuantities() {
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter fitter = fitFactory.createFitter("chi2");
        IFitResult result = fitBeamEnergyPeak(elePzBeam, fitter, "range=\"(-10.0,10.0)\"");
        if (result != null) {
            double[] pars = result.fittedParameters();
            for (int i = 0; i < 5; i++) {
                LOGGER.info("Beam Energy Peak:  " + result.fittedParameterNames()[i] + " = " + pars[i]);
            }
            monitoredQuantityMap.put(finalStateParticlesColName + " " + triggerType + " " + fpQuantNames[7], (double) pars[1]);
            monitoredQuantityMap.put(finalStateParticlesColName + " " + triggerType + " " + fpQuantNames[8], (double) pars[2]);
        }
        monitoredQuantityMap.put(finalStateParticlesColName + " " + triggerType + " " + fpQuantNames[0], (double) nTotEle / nRecoEvents);
        monitoredQuantityMap.put(finalStateParticlesColName + " " + triggerType + " " + fpQuantNames[1], (double) nTotPos / nRecoEvents);
        monitoredQuantityMap.put(finalStateParticlesColName + " " + triggerType + " " + fpQuantNames[2], (double) nTotPhotons / nRecoEvents);
        monitoredQuantityMap.put(finalStateParticlesColName + " " + triggerType + " " + fpQuantNames[3], (double) nTotUnAss / nRecoEvents);
        monitoredQuantityMap.put(finalStateParticlesColName + " " + triggerType + " " + fpQuantNames[4], (double) sumdelX / nTotAss);
        monitoredQuantityMap.put(finalStateParticlesColName + " " + triggerType + " " + fpQuantNames[5], (double) sumdelY / nTotAss);
        monitoredQuantityMap.put(finalStateParticlesColName + " " + triggerType + " " + fpQuantNames[6], (double) sumEoverP / nTotAss);

        IPlotter plotter = analysisFactory.createPlotterFactory().create("Beam Energy Electrons");

        IPlotterStyle pstyle = plotter.style();
        pstyle.legendBoxStyle().setVisible(false);
        pstyle.dataStyle().fillStyle().setColor("green");
        pstyle.dataStyle().lineStyle().setColor("black");
        plotter.region(0).plot(elePzBeam);
//        plotter.region(0).plot(result.fittedFunction());
        if (outputPlots) {
            try {
                plotter.writeToFile(outputPlotDir + "beamEnergyElectrons.png");
            } catch (IOException ex) {
                Logger.getLogger(FinalStateMonitoring.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void printDQMStrings() {
        for (int i = 0; i < 9; i++)//TODO:  do this in a smarter way...loop over the map
        {
            LOGGER.info("ALTER TABLE dqm ADD " + fpQuantNames[i] + " double;");
        }
    }

    IFitResult fitBeamEnergyPeak(IHistogram1D h1d, IFitter fitter, String range) {
//        return fitter.fit(h1d, "g", range);

//        return fitter.fit(h1d, "g+p1", init, range);
        double[] init = {20.0, 2.2, 0.12, 10, 0.0};
//        double[] init = {20.0, 2.2, 0.1};
        IFitResult ifr = null;
        try {
            ifr = fitter.fit(h1d, "g+p1", init);
        } catch (RuntimeException ex) {
            LOGGER.info(this.getClass().getSimpleName() + ":  caught exception in fitGaussian");
        }

        return ifr;
    }

}
