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
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This makes a bunch of plots of the FEE peak per crystal (seed).
 */
public class FEEClusterPlotter extends Driver {

    // private AIDAFrame plotterFrame;
    private AIDA aida = AIDA.defaultInstance();
    IPlotter plotter;
    IAnalysisFactory fac = aida.analysisFactory();
    private String inputCollection = "EcalClusters";
    protected boolean isMC = false;

    public void setInputCollection(final String inputCollection) {
        this.inputCollection = inputCollection;
    }

    private DatabaseConditionsManager conditionsManager = null;
    private EcalConditions ecalConditions = null;
    private String histoNameFormat = "%3d";

    private String outputPlots = null;

    // Set min energy in histo
    private double minHistoE = 0.5;
    private double minHistoEseed = 0.5;

    // Set max energy in histo
    private double maxHistoE = 1.3;
    private double maxHistoEseed = 1.3;
    /**
     * Set the minimum histogram energy
     * 
     * @param minHistoE
     */
    public void setMinHistoE(double minHistoE) {
        this.minHistoE = minHistoE;
    }

    /**
     * Set the maximum histogram energy
     * 
     * @param maxHistoE
     */
    public void setMaxHistoE(double maxHistoE) {
        this.maxHistoE = maxHistoE;
    }

    /**
     * Set the minimum histogram energy
     * 
     * @param minHistoE
     */
    public void setMinHistoEseed(double minHistoEseed) {
        this.minHistoEseed = minHistoEseed;
    }

    /**
     * Set the maximum histogram energy
     * 
     * @param maxHistoE
     */
    public void setMaxHistoEseed(double maxHistoEseed) {
        this.maxHistoEseed = maxHistoEseed;
    }

    
    
    /**
     * Sets the condition of whether the data is Monte Carlo or not. False by
     * default.
     * 
     * @param isMC
     */
    public void setIsMC(boolean state) {
        isMC = state;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        conditionsManager = DatabaseConditionsManager.getInstance();
        ecalConditions = conditionsManager.getEcalConditions();

        aida.tree().cd("/");
        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            aida.histogram1D(getHistoName(cc), 200, minHistoE, maxHistoE);
            aida.histogram1D(getHistoName(cc)+"_seed", 200, minHistoEseed, maxHistoEseed);
        }

        // Create a 1D histo to hold the time
        aida.histogram1D("seedTime", 400, 0, 200);
        aida.histogram1D("seedTimeTop", 400, 0, 200);
        aida.histogram1D("seedTimeBot", 400, 0, 200);
        // Create a 2D histo to hold the number of entries in each 1D histogram
        aida.histogram2D("numberOfHits", 47, -23.5, 23.5, 11, -5.5, 5.5);

    }

    private String getHistoName(EcalChannel cc) {
        return String.format(histoNameFormat, cc.getChannelId());
    }

    // Set min seed energy value, default to 2015 run
    private double seedCut = 0.4;

    // set min cluster time in window, default to 2015 run
    private double minTime = 30;

    // set max cluster time in window, default to 2015 run
    private double maxTime = 70;

    // set min number of hits in a cluster in row 1, default to 2015 run
    private int hitCut = 5;

    // hit cut is only used in 2016 data, not 2015
    private boolean useHitCut = false;

    /**
     * Set the cut value for seed energy in GeV
     * 
     * @param seedCut
     */
    public void setSeedCut(double seedCut) {
        this.seedCut = seedCut;
    }

    /**
     * Set the min time in window to look for cluster
     * 
     * @param minTime
     */
    public void setMinTime(double minTime) {
        this.minTime = minTime;
    }

    /**
     * Set the max time in window to look for cluster
     * 
     * @param maxTime
     */
    public void setMaxTime(double maxTime) {
        this.maxTime = maxTime;
    }

    /**
     * Set the hit cut value for hits in cluster This cut is used in 2016 running
     * (not 2015)
     * 
     * @param hitCut
     */
    public void setHitCut(int hitCut) {
        this.hitCut = hitCut;
    }

    /**
     * Set the hit cut value for hits in cluster This cut is used in 2016 running
     * (not 2015)
     * 
     * @param hitCut
     */
    public void setUseHitCut(boolean useHitCut) {
        this.useHitCut = useHitCut;
    }

    public void process(EventHeader event) {
        aida.tree().cd("/");
        // only keep singles triggers:
        boolean isSingles = false;
        if (isMC) {
            isSingles = true;
        } else {
            if (!event.hasCollection(GenericObject.class, "TriggerBank")) {
                throw new Driver.NextEventException();
            }

            /*
             * for (GenericObject gob : event.get(GenericObject.class, "TriggerBank")) { if
             * (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue; TIData tid =
             * new TIData(gob); if (tid.isSingle0Trigger() || tid.isSingle1Trigger()) {
             * isSingles = true; break; } }
             */
            isSingles = true;
        }
        if (isSingles) {
            List<Cluster> clusters = event.get(Cluster.class, inputCollection);
            for (Cluster clus : clusters) {
                List<CalorimeterHit> hits = clus.getCalorimeterHits();
                CalorimeterHit seed = hits.get(0);

                double seedE = seed.getCorrectedEnergy();
                double clusE = clus.getEnergy();
                double time = seed.getTime();

               

                aida.histogram1D("seedTime").fill(time);
                if (findChannel(seed).getY() > 0)
                    aida.histogram1D("seedTimeTop").fill(time);
                else
                    aida.histogram1D("seedTimeBot").fill(time);

                // in 2015, not hit count cut used at all
                if (useHitCut) {
                    if (Math.abs(seed.getIdentifierFieldValue("iy")) == 1 && (seedE / clusE > 0.6) && seedE > seedCut
                            && time > minTime && time < maxTime && hits.size() > (hitCut + 2)) {

                        EcalChannel cc = findChannel(seed);
                        aida.histogram1D(getHistoName(cc)).fill(clusE);
                    } else if (Math.abs(seed.getIdentifierFieldValue("iy")) > 1 && (seedE / clusE > 0.6)
                            && seedE > seedCut && time > minTime && time < maxTime && hits.size() > (hitCut)) {

                        EcalChannel cc = findChannel(seed);
                        aida.histogram2D("numberOfHits").fill(cc.getX(), cc.getY());
                        aida.histogram1D(getHistoName(cc)).fill(clusE);
                    }
                } else {
                    if ((seedE / clusE > 0.6) && seedE > seedCut && time > minTime && time < maxTime) {
                        EcalChannel cc = findChannel(seed);
                        aida.histogram2D("numberOfHits").fill(cc.getX(), cc.getY());
                        aida.histogram1D(getHistoName(cc)).fill(clusE);
                        aida.histogram1D(getHistoName(cc)+"_seed").fill(seedE);
                    }
                }
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
                aida.saveAs("outputFEEPlots.root");
            } catch (IOException ex) {
                Logger.getLogger(FEEClusterPlotter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
