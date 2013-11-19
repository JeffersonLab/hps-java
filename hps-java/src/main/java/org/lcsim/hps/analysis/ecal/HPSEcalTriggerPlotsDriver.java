package org.lcsim.hps.analysis.ecal;

import hep.aida.IHistogram2D;

import java.util.List;

import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.hps.recon.ecal.ECalUtils;
import org.lcsim.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.hps.recon.ecal.HPSCalorimeterHit;
import org.lcsim.hps.readout.ecal.TriggerDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Diagnostic plots for HPS ECal.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: HPSEcalTriggerPlotsDriver.java,v 1.7 2013/02/25 22:39:26 meeg Exp $
 */
public class HPSEcalTriggerPlotsDriver extends Driver {

    String ecalCollectionName = "EcalHits";
    String clusterCollectionName = "EcalClusters";
    AIDA aida = AIDA.defaultInstance();
    IHistogram2D hitXYPlot;
    IHistogram2D hitXYPlot100;
    IHistogram2D hitXYPlot200;
    IHistogram2D hitXYPlot500;
    IHistogram2D hitXYPlot1000;
    IHistogram2D crystalDeadTime;
    IHistogram2D clusterHitXYPlot;
    IHistogram2D seedHitXYPlot;
    IHistogram2D triggerClusterHitXYPlot;
    IHistogram2D triggerSeedHitXYPlot;
    IDDecoder dec = null;
    private int coincidenceWindow = 2;
    private double tp = 14.0;
    private double threshold = 50 * 10 * 0.15 * ECalUtils.MeV;

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    @Override
    public void startOfData() {
        hitXYPlot = aida.histogram2D(
                "Trigger plots: " + ecalCollectionName + " : Hits",
                46, -23, 23, 11, -5.5, 5.5);
        hitXYPlot100 = aida.histogram2D(
                "Trigger plots: " + ecalCollectionName + " : Hits above 100 MeV",
                46, -23, 23, 11, -5.5, 5.5);
        hitXYPlot200 = aida.histogram2D(
                "Trigger plots: " + ecalCollectionName + " : Hits above 200 MeV",
                46, -23, 23, 11, -5.5, 5.5);
        hitXYPlot500 = aida.histogram2D(
                "Trigger plots: " + ecalCollectionName + " : Hits above 500 MeV",
                46, -23, 23, 11, -5.5, 5.5);
        hitXYPlot1000 = aida.histogram2D(
                "Trigger plots: " + ecalCollectionName + " : Hits above 1000 MeV",
                46, -23, 23, 11, -5.5, 5.5);
        crystalDeadTime = aida.histogram2D(
                "Trigger plots: " + ecalCollectionName + " : Crystal dead time",
                46, -23, 23, 11, -5.5, 5.5);
        clusterHitXYPlot = aida.histogram2D(
                "Trigger plots: " + clusterCollectionName + " : Crystals in clusters",
                47, -23.5, 23.5, 11, -5.5, 5.5);
        seedHitXYPlot = aida.histogram2D(
                "Trigger plots: " + clusterCollectionName + " : Seed hits",
                47, -23.5, 23.5, 11, -5.5, 5.5);
        triggerClusterHitXYPlot = aida.histogram2D(
                "Trigger plots: " + clusterCollectionName + " : Crystals in clusters, with trigger",
                47, -23.5, 23.5, 11, -5.5, 5.5);
        triggerSeedHitXYPlot = aida.histogram2D(
                "Trigger plots: " + clusterCollectionName + " : Seed hits, with trigger",
                47, -23.5, 23.5, 11, -5.5, 5.5);
    }

    @Override
    public void process(EventHeader event) {
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollectionName);
        if (clusters == null) {
            throw new RuntimeException("Missing cluster collection!");
        }

        List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);
        if (hits == null) {
            throw new RuntimeException("Missing hit collection!");
        }

        // Get ID helper.
        IIdentifierHelper helper =
                event.getMetaData(hits).getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper();

        for (CalorimeterHit hit : hits) {
            int ix = hit.getIdentifierFieldValue("ix");
            int iy = hit.getIdentifierFieldValue("iy");
            hitXYPlot.fill(ix-0.5*Math.signum(ix), iy, 1.0 / coincidenceWindow);
            if (hit.getRawEnergy() > 100.0 * ECalUtils.MeV) {
                hitXYPlot100.fill(ix-0.5*Math.signum(ix), iy, 1.0 / coincidenceWindow);
                if (hit.getRawEnergy() > 200.0 * ECalUtils.MeV) {
                    hitXYPlot200.fill(ix-0.5*Math.signum(ix), iy, 1.0 / coincidenceWindow);
                    if (hit.getRawEnergy() > 500.0 * ECalUtils.MeV) {
                        hitXYPlot500.fill(ix-0.5*Math.signum(ix), iy, 1.0 / coincidenceWindow);
                        if (hit.getRawEnergy() > 1000.0 * ECalUtils.MeV) {
                            hitXYPlot1000.fill(ix-0.5*Math.signum(ix), iy, 1.0 / coincidenceWindow);
                        }
                    }
                }
            }
            double deadTime = 0;
            for (int time = 0; time < 500; time++) {
                if (hit.getRawEnergy() * pulseAmplitude(time) > threshold) {
                    deadTime += 1e-6; //units of milliseconds
                } else if (time > 2*tp || deadTime != 0) {
                    break;
                }
            }
            crystalDeadTime.fill(ix-0.5*Math.signum(ix), iy, deadTime / coincidenceWindow);
        }

        for (HPSEcalCluster clus : clusters) {
            HPSCalorimeterHit seedHit = (HPSCalorimeterHit) clus.getSeedHit();
            IIdentifier id = seedHit.getIdentifier();
            int ix = helper.unpack(id).getValue(helper.getFieldIndex("ix"));
            int iy = helper.unpack(id).getValue(helper.getFieldIndex("iy"));
//                dec = seedHit.getIDDecoder();
//                dec.setID(seedHit.getCellID());
            seedHitXYPlot.fill(ix, iy);
            for (CalorimeterHit hit : clus.getCalorimeterHits()) {
                id = hit.getIdentifier();
                ix = helper.unpack(id).getValue(helper.getFieldIndex("ix"));
                iy = helper.unpack(id).getValue(helper.getFieldIndex("iy"));
//                    dec = hit.getIDDecoder();
//                    dec.setID(hit.getCellID());
                clusterHitXYPlot.fill(ix, iy);
            }
        }

        if (TriggerDriver.triggerBit()) {
            for (HPSEcalCluster clus : clusters) {
                HPSCalorimeterHit seedHit = (HPSCalorimeterHit) clus.getSeedHit();
                IIdentifier id = seedHit.getIdentifier();
                int ix = helper.unpack(id).getValue(helper.getFieldIndex("ix"));
                int iy = helper.unpack(id).getValue(helper.getFieldIndex("iy"));
//                dec = seedHit.getIDDecoder();
//                dec.setID(seedHit.getCellID());
                triggerSeedHitXYPlot.fill(ix, iy);
                for (CalorimeterHit hit : clus.getCalorimeterHits()) {
                    id = hit.getIdentifier();
                    ix = helper.unpack(id).getValue(helper.getFieldIndex("ix"));
                    iy = helper.unpack(id).getValue(helper.getFieldIndex("iy"));
//                    dec = hit.getIDDecoder();
//                    dec.setID(hit.getCellID());
                    triggerClusterHitXYPlot.fill(ix, iy);
                }
            }
        }
    }

    private double pulseAmplitude(double time) {
        if (time <= 0.0) {
            return 0.0;
        }
        return (time / tp) * Math.exp(1.0 - time / tp);
    }
}
