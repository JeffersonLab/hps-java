package org.lcsim.hps.analysis.ecal;

import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.hps.recon.ecal.HPSCalorimeterHit;
import org.lcsim.hps.readout.ecal.TriggerDriver;
import org.hps.util.ClockSingleton;
import org.lcsim.units.SystemOfUnits;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Diagnostic plots for HPS ECal.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: HPSEcalPlotsDriver.java,v 1.16 2013/02/25 22:39:26 meeg Exp $
 */
public class HPSEcalPlotsDriver extends Driver {

    String ecalCollectionName = "EcalHits";
    String ecalClusterCollectionName = "EcalClusters";
    AIDA aida = AIDA.defaultInstance();
    // CalHit plots.
    IHistogram1D hitEPlot;
    ICloud1D ecalEPlot;
    ICloud2D hitXZPlot;
    ICloud2D hitYZPlot;
    ICloud1D hitUnder100MeVPlot;
    IHistogram1D hitOver100MeVPlot;
    ICloud1D maxHitEPlot;
    IHistogram1D maxTimePlot;
    ICloud1D timePlot;
    ICloud1D hitCountPlot;
    ICloud1D idCountPlot;
    ICloud1D crystalXPlot;
    ICloud1D crystalYPlot;
    //ICloud2D cellXYPlot;
    IHistogram2D crystalXYPlot;
    // Cluster plots.
    IHistogram1D nclusPlot;
    ICloud1D clusEPlot;
    ICloud1D clusTotEPlot;
    ICloud1D leadClusEPlot;
    ICloud1D leadClus2EPlot;
    //ICloud1D clusResTop3Plot;
    IHistogram1D clusHitPlot;
    ICloud1D clusSeedEPlot;
    ICloud1D clusSeedDistPlot;
    ICloud2D leadClusAndPrimaryPlot;
    IHistogram1D clusNHits;
    IHistogram2D hitXYPlot;
    int numTriggers = 0;

    class MCParticleEComparator implements Comparator<MCParticle> {

        public int compare(MCParticle p1, MCParticle p2) {
            double e1 = p1.getEnergy();
            double e2 = p2.getEnergy();
            if (e1 < e2) {
                return -1;
            } else if (e1 == e2) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    class ClusterEComparator implements Comparator<Cluster> {

        public int compare(Cluster o1, Cluster o2) {
            if (o1.getEnergy() < o2.getEnergy()) {
                return -1;
            } else if (o1.getEnergy() > o2.getEnergy()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setEcalClusterCollectionName(String ecalClusterCollectionName) {
        this.ecalClusterCollectionName = ecalClusterCollectionName;
    }

    public void startOfData() {
        timePlot = aida.cloud1D(ecalCollectionName + " : Hit Time");
        timePlot.annotation().addItem("yAxisScale", "log");
        timePlot.annotation().addItem("xAxisLabel", "Time [ns]");

        maxTimePlot = aida.histogram1D(ecalCollectionName + " : Max Time", 200, 0, 1000);
        maxTimePlot.annotation().addItem("yAxisScale", "log");
        maxTimePlot.annotation().addItem("xAxisLabel", "Time [ns]");

        maxHitEPlot = aida.cloud1D(ecalCollectionName + " : Max Hit E in Event");
        maxHitEPlot.annotation().addItem("xAxisLabel", "E [GeV]");

        hitEPlot = aida.histogram1D(ecalCollectionName + " : Hit Energy", 200, 0, 3500);
        hitEPlot.annotation().addItem("yAxisScale", "log");
        hitEPlot.annotation().addItem("xAxisLabel", "E [GeV]");

        hitCountPlot = aida.cloud1D(ecalCollectionName + " : Hit Count");
        hitCountPlot.annotation().addItem("xAxisLabel", "Number of Hits");

        idCountPlot = aida.cloud1D(ecalCollectionName + " : Uniq Hit IDs");
        idCountPlot.annotation().addItem("xAxisLabel", "Number of Unique IDs in Event");

        ecalEPlot = aida.cloud1D(ecalCollectionName + " : Total E in Event");
        ecalEPlot.annotation().addItem("xAxisLabel", "E [GeV]");

        hitYZPlot = aida.cloud2D(ecalCollectionName + " : Y vs Z");
        hitYZPlot.annotation().addItem("xAxisLabel", "Y [mm]");
        hitYZPlot.annotation().addItem("yAxisLabel", "Z [mm]");

        hitXZPlot = aida.cloud2D(ecalCollectionName + " : X vs Z");
        hitXZPlot.annotation().addItem("xAxisLabel", "X [mm]");
        hitXZPlot.annotation().addItem("yAxisLabel", "Z [mm]");

        crystalXPlot = aida.cloud1D(ecalCollectionName + " : X Field Value");
        crystalXPlot.annotation().addItem("xAxisLabel", "Number of Entries");

        crystalYPlot = aida.cloud1D(ecalCollectionName + " : Y Field Value");
        crystalYPlot.annotation().addItem("xAxisLabel", "Number of Entries");

        crystalXYPlot = aida.histogram2D(
                ecalCollectionName + " : X & Y ID Values",
                46, -23., 23., 5, 1., 6.);

        clusEPlot = aida.cloud1D(ecalClusterCollectionName + " : Cluster E");
        clusEPlot.annotation().addItem("xAxisLabel", "E [GeV]");

        nclusPlot = aida.histogram1D(ecalClusterCollectionName + " : Number of Clusters", 20, 0, 20);
        nclusPlot.annotation().addItem("xAxisLabel", "Number of Clusters");

        hitUnder100MeVPlot = aida.cloud1D(ecalCollectionName + " : Hits with E < 100 MeV");
        hitUnder100MeVPlot.annotation().addItem("xAxisLabel", "Number of Hits");

        hitOver100MeVPlot = aida.histogram1D(ecalCollectionName + " : Hits with E >= 100 MeV", 20, 0, 20);
        hitUnder100MeVPlot.annotation().addItem("xAxisLabel", "Number of Hits");

        leadClusEPlot = aida.cloud1D(ecalClusterCollectionName + " : Leading Cluster E");
        leadClusEPlot.annotation().addItem("xAxisLabel", "E [GeV]");

        leadClus2EPlot = aida.cloud1D(ecalClusterCollectionName + " : Second Leading Cluster E");
        leadClus2EPlot.annotation().addItem("xAxisLabel", "E [GeV]");

        clusTotEPlot = aida.cloud1D(ecalClusterCollectionName + " : Total Clus E in Event");
        clusTotEPlot.annotation().addItem("xAxisLabel", "E [GeV]");

        //clusResTop3Plot = aida.cloud1D(ecalClusterCollectionName + " : Total Clus E Residual with Top 3 Particles");
        //clusResTop3Plot.annotation().addItem("xAxisLabel", "E [GeV]");

        clusHitPlot = aida.histogram1D(ecalClusterCollectionName + " : Number of Clusters per Hit", 5, 1, 6);
        clusHitPlot.annotation().addItem("xAxisLabel", "Number of Clusters");

        clusSeedDistPlot = aida.cloud1D(ecalClusterCollectionName + " : Cluster Seed Hit Distance from Beam Axis");
        clusSeedDistPlot.annotation().addItem("xAxisLabel", "Distance [mm]");
        clusSeedDistPlot.annotation().addItem("yAxisScale", "log");

        clusSeedEPlot = aida.cloud1D(ecalClusterCollectionName + " : Cluster Seed Energy");
        clusSeedEPlot.annotation().addItem("xAxisLabel", "E [GeV]");

        clusNHits = aida.histogram1D(ecalClusterCollectionName + " : Number of Hits per Cluster", 13, 0, 13);
        clusNHits.annotation().addItem("xAxisLabel", "Number of Hits");

        leadClusAndPrimaryPlot = aida.cloud2D(ecalClusterCollectionName + " : Lead Cluster E vs Highest Primary Particle E");
    }

    public void process(EventHeader event) {
        if (TriggerDriver.triggerBit()) numTriggers++;

        // MCParticles
        List<MCParticle> mcparticles = event.get(MCParticle.class).get(0);

        // primary particle with most E
        MCParticle primary = getPrimary(mcparticles);
        double primaryE = primary.getEnergy();

        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, ecalClusterCollectionName);
        if (clusters == null)
            throw new RuntimeException("Missing cluster collection!");

        List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

        Collections.sort(clusters, new ClusterEComparator());

        nclusPlot.fill(clusters.size());

        // Leading cluster E.
        if (clusters.size() > 0) {
            Cluster leadClus = clusters.get(clusters.size() - 1);
            leadClusEPlot.fill(leadClus.getEnergy());

            leadClusAndPrimaryPlot.fill(primaryE, leadClus.getEnergy());
        }

        // Second leading cluster E.
        if (clusters.size() > 1) {
            leadClus2EPlot.fill(clusters.get(clusters.size() - 2).getEnergy());
        }

        Map<CalorimeterHit, Integer> hitClusMap = new HashMap<CalorimeterHit, Integer>();

        double clusE = 0;

        // Get ID helper.
        IIdentifierHelper helper =
                event.getMetaData(hits).getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper();

        for (HPSEcalCluster clus : clusters) {

            clusNHits.fill(clus.getCalorimeterHits().size());

            double e = clus.getEnergy();
            clusEPlot.fill(e);
            clusE += e;
            HPSCalorimeterHit seedHit = (HPSCalorimeterHit) clus.getSeedHit();
            //double maxe = 0;
            for (CalorimeterHit hit : clus.getCalorimeterHits()) {
                if (hitClusMap.containsKey(hit)) {
                    int nshared = hitClusMap.get(hit);
                    ++nshared;

                    hitClusMap.put(hit, nshared);
                } else {
                    hitClusMap.put(hit, 1);
                }

//                if (hit.getRawEnergy() > maxe) {
//                    seedHit = hit;
//                }
            }

            // Seed distance from X axis.
            Hep3Vector pos = seedHit.getPositionVec();
            double y = pos.y();
            double z = pos.z();
            clusSeedDistPlot.fill(Math.sqrt(y * y + z * z));

            // Seed E.
            clusSeedEPlot.fill(seedHit.getRawEnergy());
        }
        // Total E in all clusters.
        clusTotEPlot.fill(clusE);

        // Residual of cluster total E and E from top 3 primary particles.
        //clusResTop3Plot.fill(clusE - e3);

        for (Entry<CalorimeterHit, Integer> clusHit : hitClusMap.entrySet()) {
            clusHitPlot.fill(clusHit.getValue());
        }

        // sum and max vars
        double esum = 0;
        double emax = 0;
        double tmax = 0;

        // Loop over hits from ECal collection.
        int nhits = hits.size();



        // Check unique IDs.
        Set<Long> ids = new HashSet<Long>();

        // n hits
        hitCountPlot.fill(nhits);

        int nhits100MeV = 0;
        int nhitsOver100MeV = 0;

        if (TriggerDriver.triggerBit() && numTriggers <= 100) {
            hitXYPlot = aida.histogram2D(
                    ecalCollectionName + " : hit E, event " + String.format("%07d", ClockSingleton.getClock()),
                    47, -23.5, 23.5, 11, -5.5, 5.5);
        }

        // Loop over ECal hits.
        for (CalorimeterHit hit : hits) {
            // get raw E
            double eraw = hit.getRawEnergy();

            if (eraw >= 0.1) {
                nhitsOver100MeV++;
            }

            if (eraw < 0.1) {
                nhits100MeV++;
            }

            // time
            timePlot.fill(hit.getTime());

            // YZ
            hitYZPlot.fill(hit.getPosition()[1], hit.getPosition()[2]);

            // XZ
            hitXZPlot.fill(hit.getPosition()[0], hit.getPosition()[2]);

            // hit E
            hitEPlot.fill(eraw / SystemOfUnits.MeV);

            // check E max
            if (eraw > emax)
                emax = eraw;

            // check T max
            if (hit.getTime() > tmax)
                tmax = hit.getTime();

            if (ids.contains(hit.getCellID()))
                throw new RuntimeException("Duplicate cell ID: " + hit.getCellID());

            ids.add(hit.getCellID());

            // Add to ECal energy sum.
            esum += eraw;

            // X and Y identifier values.
            IIdentifier id = hit.getIdentifier();
            int ix = helper.unpack(id).getValue(helper.getFieldIndex("ix"));
            int iy = helper.unpack(id).getValue(helper.getFieldIndex("iy"));
            crystalXPlot.fill(ix);
            crystalYPlot.fill(iy);
            crystalXYPlot.fill(ix, iy);
            if (TriggerDriver.triggerBit() && numTriggers <= 100)
                hitXYPlot.fill(ix, iy, eraw);
        }

        hitUnder100MeVPlot.fill(nhits100MeV);
        hitOver100MeVPlot.fill(nhitsOver100MeV);

        // total E in Cal
        ecalEPlot.fill(esum);

        // max hit E in event
        maxHitEPlot.fill(emax);

        // max hit time
        maxTimePlot.fill(tmax);

        // number of unique hit ids
        idCountPlot.fill(ids.size());
    }


    private MCParticle getPrimary(List<MCParticle> particles) {
        double maxE = 0;
        MCParticle primary = null;
        for (MCParticle particle : particles) {
            if (particle.getGeneratorStatus() == MCParticle.FINAL_STATE
                    && particle.getEnergy() > maxE) {
                maxE = particle.getEnergy();
                primary = particle;
            }
        }
        return primary;
    }
}
