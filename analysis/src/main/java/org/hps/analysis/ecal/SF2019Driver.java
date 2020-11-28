package org.hps.analysis.ecal;

import hep.aida.IAnalysisFactory;
import hep.aida.IPlotter;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.hps.detector.ecal.EcalCrystal;
import org.hps.detector.ecal.HPSEcalDetectorElement;

/**
 * This driver is used to check the Sampling Fraction correction for the 2019 run,
 * using MC
 *
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 */

public class SF2019Driver extends Driver {

    // private AIDAFrame plotterFrame;
    private AIDA aida = AIDA.defaultInstance();
    IPlotter plotter;
    IAnalysisFactory fac = aida.analysisFactory();
    private String inputCollection = "EcalClusters";

    public void setInputCollection(final String inputCollection) {
        this.inputCollection = inputCollection;
    }

    private DatabaseConditionsManager conditionsManager = null;
    private EcalConditions ecalConditions = null;
    private String histoNameFormat = "%3d";

    private String outputPlots = null;

    // Set min energy in histo
    private double minHistoE;

    // Set max energy in histo
    private double maxHistoE;

    private double E0 = 4.5560;

    double BEAMGAPTOP = 20.0;
    double BEAMGAPBOT = -20.0;
    double BEAMGAPTOPC;
    double BEAMGAPBOTC;

    static int MC_GENERATED_PARTICLE = 1;

    HPSEcal3 ecal;

    public void setBeamEnergy(double beamE) {
        System.out.println("SF2019Driver, setBeamEnergy: " + beamE + " GeV ");
        this.E0 = beamE;
        minHistoE = E0 * 0.1;
        maxHistoE = E0 * 1.2;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        conditionsManager = DatabaseConditionsManager.getInstance();
        ecalConditions = conditionsManager.getEcalConditions();

        aida.histogram2D("hitPositionEcalImpinging_all", 100, -350., 350., 100, -100., 100.);
        aida.histogram2D("hitPositionEcalImpinging", 100, -350., 350., 100, -100., 100.);

        aida.histogram2D("hitPositionEcal_ScoringPlane", 100, -350., 350., 100, -100., 100.);
        aida.histogram2D("hitPositionEcal_Cluster", 100, -350., 350., 100, -100., 100.);

        /*aida.tree().cd("/");
        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            aida.histogram1D(getHistoName(cc), 200, minHistoE, maxHistoE);
        }*/
        aida.histogram1D("clusterEnergy", 700, 0.5 * E0, 1.2 * E0);
        aida.histogram1D("clusterEnergy_cutE0", 700, 0.5 * E0, 1.2 * E0);
        aida.histogram1D("clusterEnergy_cutE0_fidcut", 700, 0.5 * E0, 1.2 * E0);

        aida.histogram2D("hitPositionYvsE_yScoringPlane", 100, -100., 100., 700, 0.5 * E0, 1.2 * E0);
        aida.histogram2D("hitPositionYvsE_yCluster", 100, -100., 100., 700, 0.5 * E0, 1.2 * E0);

        aida.histogram2D("xScoringPlane_vs_xCluster", 100, -350., 350., 100, -350., 350.);
        aida.histogram2D("yScoringPlane_vs_yCluster", 100, -100., 100., 100, -100., 100.);
        aida.histogram2D("edgeScoringPlane_vs_edgeCluster", 100, 0, 100, 100, 0, 100);

        aida.histogram2D("hitPositionDistancevsE_ScoringPlane", 200, -100., 100., 700, 0.5 * E0, 1.2 * E0);
        aida.histogram2D("hitPositionDistancevsE_Cluster", 200, -100., 100., 700, 0.5 * E0, 1.2 * E0);

        
        
        aida.histogram2D("dx_vs_xCluster", 100, -350., 350.,200,-50,50);
        aida.histogram2D("dx_vs_yCluster", 100, -100., 100.,200,-50,50);
        aida.histogram2D("dy_vs_xCluster", 100, -350., 350.,200,-50,50);
        aida.histogram2D("dy_vs_yCluster", 100, -100., 100.,200,-50,50);
        
        aida.histogram2D("dx_vs_xScoringPlane", 100, -350., 350.,200,-50,50);
        aida.histogram2D("dx_vs_yScoringPlane", 100, -100., 100.,200,-50,50);
        aida.histogram2D("dy_vs_xScoringPlane", 100, -350., 350.,200,-50,50);
        aida.histogram2D("dy_vs_yScoringPlane", 100, -100., 100.,200,-50,50);
        

        
        aida.histogram2D("xScoringPlane_vs_xCluster", 100, -350., 350., 100, -350., 350.);
        aida.histogram2D("yScoringPlane_vs_yCluster", 100, -100., 100., 100, -100., 100.);
        
        ecal = (HPSEcal3) detector.getSubdetector("Ecal");

        // distance to beam gap edge

        // Get these values from the Ecal geometry - I AM USING SAME code as in ClusterEnergyCorrection.java

        BEAMGAPTOP = 20.0;
        try {
            BEAMGAPTOP = ecal.getNode().getChild("layout").getAttribute("beamgapTop").getDoubleValue();
        } catch (Exception e) {
            try {
                BEAMGAPTOP = ecal.getNode().getChild("layout").getAttribute("beamgap").getDoubleValue();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        BEAMGAPBOT = -20.0;
        try {
            BEAMGAPBOT = -ecal.getNode().getChild("layout").getAttribute("beamgapBottom").getDoubleValue();
        } catch (Exception e) {
            try {
                BEAMGAPBOT = -ecal.getNode().getChild("layout").getAttribute("beamgap").getDoubleValue();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        BEAMGAPTOPC = BEAMGAPTOP + 13.0;// mm
        BEAMGAPBOTC = BEAMGAPBOT - 13.0;// mm

        System.out.println("SF2019Driver: BEAMGAPTOP= " + BEAMGAPTOP + " BEAMGAPBOT= " + BEAMGAPBOT);
        System.out.println("SF2019Driver: BEAMGAPTOBC= " + BEAMGAPTOPC + " BEAMGAPBOTC= " + BEAMGAPBOTC);

    }

    /**Return the y position relative to the beam gap edge, according to the 2015 analysis note convention.
     * @param xpos: cluster position X coordinate mm
     * @param ypos: cluster position Y coordinate mm
     * @return the distance wrt the beam gap, in mm
     */
    private double getYpositionFromBeamGapEdge(double xpos, double ypos) {

        double r;

        HPSEcalDetectorElement detElement = (HPSEcalDetectorElement) ecal.getDetectorElement();
        // x-coordinates of crystals on either side of row 1 cut out
        EcalCrystal crystalM = detElement.getCrystal(-11, 1);
        Hep3Vector posM = crystalM.getPositionFront();
        EcalCrystal crystalP = detElement.getCrystal(-1, 1);
        Hep3Vector posP = crystalP.getPositionFront();

        double matchingPoint = 35;

       
        if ((xpos < posM.x()) || (xpos > posP.x())) {
            if (ypos > 0) {
                r = Math.abs(ypos - BEAMGAPTOP);
            } else {
                r = Math.abs(ypos - BEAMGAPBOT);
            }
        }
        // crystals above row 1 cut out
        else {
            if (ypos > 0) {
                if (ypos > (matchingPoint + BEAMGAPTOP)) {
                    r = Math.abs(ypos - BEAMGAPTOP);
                } else {
                    r = Math.abs(ypos - BEAMGAPTOPC);
                }
            } else {
                if (ypos > (-matchingPoint + BEAMGAPBOT)) {
                    r = Math.abs(ypos - BEAMGAPBOTC);
                } else {
                    r = Math.abs(ypos - BEAMGAPBOT);
                }
            }
        }

        return r;
    }

    private String getHistoName(EcalChannel cc) {
        return String.format(histoNameFormat, cc.getChannelId());
    }

    public void process(EventHeader event) {
        aida.tree().cd("/");

        int decCalo = 0;
        int decTracker = 0;
        double scoringE = 0;

        double xpos, ypos;
        double xClus, yClus;
        double dX,dY;

        double edgeDistance_clus;
        double edgeDistance_pos;

        int iX, iY;
        boolean flagMCgenerator = false;
        Hep3Vector scoringP = new BasicHep3Vector(0, 0, 0);
        Hep3Vector scoringX = new BasicHep3Vector(0, 0, 0);
        
        

      
        

        /*Determine the generated particle*/
        List<MCParticle> mcParticles = event.get(MCParticle.class, "MCParticle");
        if (mcParticles.size() < 1) return;
        MCParticle fee=mcParticles.get(0); //just to init
        for (MCParticle particle : mcParticles) {
            if (particle.getGeneratorStatus() == SF2019Driver.MC_GENERATED_PARTICLE) {
                fee = particle;
                flagMCgenerator = true;
                break;
            }
        }
        if (!flagMCgenerator) {
            return;
        }

        
        
        MCParticle.SimulatorStatus simstat = fee.getSimulatorStatus();

        if (simstat.isDecayedInCalorimeter()) {
            decCalo = 1;
        }
        if (simstat.isDecayedInTracker()) {
            decTracker = 1;
        }
        // System.out.println("FEE: "+fee.getPZ()+" "+decCalo+" "+decTracker);
        if ((decCalo != 1) || (decTracker != 0)) return;

        List<SimTrackerHit> simTrackerHitList = event.get(SimTrackerHit.class, "TrackerHitsECal");
        for (SimTrackerHit hit : simTrackerHitList) {
            Hep3Vector simTrackerHitPos = hit.getPositionVec();
            Hep3Vector simTrackerHitMomentum = new BasicHep3Vector(hit.getMomentum());
            double simTrackerHitTime = hit.getTime();
            if (hit.getMCParticle() == fee) {
                if (simTrackerHitMomentum.magnitude() > scoringE) {
                    scoringX = simTrackerHitPos;
                    scoringP = simTrackerHitMomentum;
                    scoringE = simTrackerHitMomentum.magnitude();
                }
            }
        }

        xpos = scoringX.x();
        ypos = scoringX.y();
        aida.histogram2D("hitPositionEcalImpinging_all").fill(xpos, ypos);

        if (scoringE > 0.85 * E0) {
            xpos = scoringX.x();
            ypos = scoringX.y();
            aida.histogram2D("hitPositionEcalImpinging").fill(xpos, ypos);
        }

        List<Cluster> rawClusters = event.get(Cluster.class,inputCollection);
        for (Cluster clus : rawClusters) {
            List<CalorimeterHit> hits = clus.getCalorimeterHits();
            CalorimeterHit seed = hits.get(0);
            double seedE = seed.getCorrectedEnergy();
            double clusE = clus.getEnergy();
            double time = seed.getTime();
            aida.histogram1D("clusterEnergy").fill(clusE);
            if (scoringE > 0.85 * E0) {
                aida.histogram1D("clusterEnergy_cutE0").fill(clusE);
                iX = seed.getIdentifierFieldValue("ix");
                iY = seed.getIdentifierFieldValue("iy");

                if ((iY == -3) || (iY == -4) || (iY == 3) || (iY == 4)) {
                    aida.histogram1D("clusterEnergy_cutE0_fidcut").fill(clusE);
                }

                xpos = scoringX.x();
                ypos = scoringX.y();

                xClus = clus.getPosition()[0];
                yClus = clus.getPosition()[1];
                
                dX=xClus-xpos;
                dY=yClus-ypos;

                aida.histogram2D("hitPositionEcal_ScoringPlane").fill(xpos, ypos);
                aida.histogram2D("hitPositionEcal_Cluster").fill(xClus, yClus);

                edgeDistance_pos = getYpositionFromBeamGapEdge(xpos, ypos);
                edgeDistance_clus = getYpositionFromBeamGapEdge(xClus, yClus);

                aida.histogram2D("yScoringPlane_vs_yCluster").fill(ypos, yClus);
                aida.histogram2D("xScoringPlane_vs_xCluster").fill(xpos, xClus);
                aida.histogram2D("edgeScoringPlane_vs_edgeCluster").fill(edgeDistance_pos, edgeDistance_clus);

                aida.histogram2D("hitPositionYvsE_yScoringPlane").fill(ypos, clusE);
                aida.histogram2D("hitPositionYvsE_yCluster").fill(yClus, clusE);

                aida.histogram2D("hitPositionDistancevsE_ScoringPlane").fill(edgeDistance_pos, clusE);
                aida.histogram2D("hitPositionDistancevsE_Cluster").fill(edgeDistance_clus, clusE);
                
                
                
                aida.histogram2D("dx_vs_xCluster").fill(dX,xClus);
                aida.histogram2D("dx_vs_yCluster").fill(dX,yClus);
                aida.histogram2D("dy_vs_xCluster").fill(dY,xClus);
                aida.histogram2D("dy_vs_yCluster").fill(dY,yClus);
                
                aida.histogram2D("dx_vs_xScoringPlane").fill(dX,xpos);
                aida.histogram2D("dx_vs_yScoringPlane").fill(dX,ypos);
                aida.histogram2D("dy_vs_xScoringPlane").fill(dY,xpos);
                aida.histogram2D("dy_vs_yScoringPlane").fill(dY,ypos);
                
            

            }
        }
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public EcalChannel findChannel(int channel_id) {
        return ecalConditions.getChannelCollection().findChannel(channel_id);
    }

    public EcalChannel findChannel(CalorimeterHit hit) {
        return ecalConditions.getChannelCollection().findGeometric(hit.getCellID());
    }

    public void endOfData() {
        System.out.println("OutputFile");
        if (outputPlots != null) {
            try {
                aida.saveAs("outputSF2019MC.root");
            } catch (IOException ex) {
                Logger.getLogger(SF2019Driver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}