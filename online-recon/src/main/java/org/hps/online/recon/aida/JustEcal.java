package org.hps.online.recon.aida;

import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math.stat.StatUtils;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.monitoring.ecal.plots.EcalMonitoringUtilities;
import org.hps.recon.ecal.EcalUtils;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;
import org.hps.recon.tracking.SvtPlotUtils;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

/**
 * Make plots for remote AIDA display based on Kalman Filter track
 * reconstruction
 */
public class JustEcal extends RemoteAidaDriver {

    /*
     * AIDA paths
     */
    private static final String ECAL_HITS_DIR = "/ecalHits";
    private static final String ECAL_CLUSTERS_DIR = "/ecalClusters";
    private static final String ECAL_PAIRS_DIR = "/ecalPairs";
    private static final String TEMP_DIR = "/tmp";

    /*
     * Ecal plots
     */
    private IHistogram2D hitCountFillPlot;
    private IHistogram2D hitCountDrawPlot;

    private IHistogram2D occupancyDrawPlot;
    private double[] occupancyFill = new double[11 * 47];
    private int NoccupancyFill;
    private IHistogram2D clusterCountFillPlot;
    private IHistogram2D clusterCountDrawPlot;
    private IHistogram1D hitCountPlot;
    private IHistogram1D hitTimePlot;
    private IHistogram1D hitEnergyPlot;
    private IHistogram1D hitMaxEnergyPlot;
    private IHistogram1D topTimePlot, botTimePlot, orTimePlot;
    private IHistogram1D orTrigTimePlot;
    private IHistogram2D orTimePlot2D;

    private IHistogram1D clusterCountPlot;
    private IHistogram1D clusterSizePlot;
    private IHistogram1D clusterEnergyPlot;
    private IHistogram1D clusterMaxEnergyPlot;
    private IHistogram1D clusterTimes;
    private IHistogram1D clusterTimeSigma;
    private IHistogram2D edgePlot;

    private IHistogram1D pairEnergySum;
    private IHistogram1D pairEnergyDifference;
    private IHistogram1D pairCoplanarity;
//    private IHistogram1D pairEnergySlope;
    private IHistogram1D pairEnergyPositionMeanX;
    private IHistogram1D pairEnergyPositionMeanY;
    private IHistogram1D pairMassOppositeHalf;
    private IHistogram1D pairMassSameHalf;
    private IHistogram1D pairMassOppositeHalfFid;
    private IHistogram1D pairMassSameHalfFid;

    //private int eventn = 0;
    private int thisEventN, prevEventN;

    private long thisTime, prevTime;
    private double thisEventTime, prevEventTime;
    private double maxE = 5000 * EcalUtils.MeV;

    private static final String TRACKER_NAME = "Tracker";
    private boolean isSeedTracker = false;
    /*
     * Collection names
     */
    private static final String RAW_ECAL_HITS = "EcalCalHits";
    private static final String ECAL_CLUSTERS = "EcalClusters";

    private int eventRefreshRate = 10;

    @Override
    protected void detectorChanged(Detector detector) {

        /*
         * Ecal plots
         */
        tree.mkdirs(TEMP_DIR);
        tree.mkdirs(ECAL_HITS_DIR);
        tree.mkdirs(ECAL_CLUSTERS_DIR);
        tree.mkdirs(ECAL_PAIRS_DIR);
        tree.cd(ECAL_HITS_DIR);

        String hitCountDrawPlotTitle;
        hitCountDrawPlotTitle = RAW_ECAL_HITS + " : Hit Rate KHz";

        hitCountDrawPlot = aida.histogram2D(hitCountDrawPlotTitle, 47, -23.5, 23.5, 11, -5.5, 5.5);
        occupancyDrawPlot = aida.histogram2D(RAW_ECAL_HITS + " : Occupancy", 47,
                -23.5, 23.5, 11, -5.5, 5.5);

        NoccupancyFill = 1; // to avoid a "NaN" at beginning
        for (int ii = 0; ii < (11 * 47); ii++) {
            int row = EcalMonitoringUtilities.getRowFromHistoID(ii);
            int column = EcalMonitoringUtilities.getColumnFromHistoID(ii);
            occupancyFill[ii] = 0.;
        }
        prevTime = 0; // init the time
        thisTime = 0; // init the time

        thisEventN = 0;
        prevEventN = 0;

        thisEventTime = 0;
        prevEventTime = 0;

        hitCountPlot = aida.histogram1D(RAW_ECAL_HITS + " : Hit Count In Event",
                30, -0.5, 29.5);

        topTimePlot = aida.histogram1D(RAW_ECAL_HITS + " : First Hit Time, Top",
                100, 0, 100 * 4.0);
        botTimePlot = aida.histogram1D(RAW_ECAL_HITS
                + " : First Hit Time, Bottom", 100, 0, 100 * 4.0);
        orTimePlot = aida.histogram1D(RAW_ECAL_HITS + " : First Hit Time, Or",
                100, 0, 100 * 4.0);

        hitTimePlot = aida.histogram1D(RAW_ECAL_HITS + " : Hit Time", 100,
                0 * 4.0, 100 * 4.0);
        orTrigTimePlot = aida.histogram1D(RAW_ECAL_HITS + " : Trigger Time, Or",
                1024, 0, 4096);

        orTimePlot2D = aida.histogram2D(RAW_ECAL_HITS
                + " : Hit Time vs. Trig Time, Or", 100, 0, 100 * 4.0, 101, -2, 402);

        hitEnergyPlot = aida.histogram1D(RAW_ECAL_HITS + " : Hit Energy", 100,
                -0.1, maxE);
        hitMaxEnergyPlot = aida.histogram1D(RAW_ECAL_HITS
                + " : Maximum Hit Energy In Event", 100, -0.1, maxE);

        tree.cd(ECAL_CLUSTERS_DIR);
        clusterCountDrawPlot = aida.histogram2D(ECAL_CLUSTERS
                + " : Cluster Rate KHz", 47, -23.5, 23.5, 11, -5.5, 5.5);
        clusterCountPlot = aida.histogram1D(ECAL_CLUSTERS
                + " : Cluster Count per Event", 10, -0.5, 9.5);
        clusterSizePlot = aida.histogram1D(ECAL_CLUSTERS
                + " : Cluster Size", 15, -0.5, 15.5);
        clusterEnergyPlot = aida.histogram1D(ECAL_CLUSTERS
                + " : Cluster Energy", 100, -0.1, maxE);
        clusterMaxEnergyPlot = aida.histogram1D(ECAL_CLUSTERS
                + " : Maximum Cluster Energy in Event", 100, -0.1, maxE);
        edgePlot = aida.histogram2D(ECAL_CLUSTERS + " : Weighted Cluster",
                47, -23.25, 23.25, 11, -5.25, 5.25);
        clusterTimes = aida.histogram1D(ECAL_CLUSTERS
                + " : Cluster Time Mean", 400, 0, 4.0 * 100);
        clusterTimeSigma = aida.histogram1D(ECAL_CLUSTERS
                + " : Cluster Time Sigma", 100, 0, 40);

        tree.cd(ECAL_PAIRS_DIR);
        pairEnergySum = aida.histogram1D("Pair Energy Sum Distribution", 176, 0.0, maxE);
        pairEnergyDifference = aida.histogram1D("Pair Energy Difference Distribution", 176, 0.0, 2.2);
        pairCoplanarity = aida.histogram1D("Pair Coplanarity Distribution", 90, 0.0, 180.0);
//        pairEnergySlope = aida.histogram1D("Pair Energy Slope Distribution", 150, 0.0, 3.0);
        pairEnergyPositionMeanX = aida.histogram1D("Cluster Pair Weighted Energy Position (x-Index)", 100, -250, 250);
        pairEnergyPositionMeanY = aida.histogram1D("Cluster Pair Weighted Energy Position (y-Index)", 100, -100, 100);
        pairMassSameHalf = aida.histogram1D("Cluster Pair Mass: Same Half", 100, 0.0, 0.35);
        pairMassOppositeHalf = aida.histogram1D("Cluster Pair Mass: Top Bottom", 100, 0.0, 0.35);
        pairMassSameHalfFid = aida.histogram1D("Cluster Pair Mass: Same Half Fiducial", 100, 0.0, 0.35);
        pairMassOppositeHalfFid = aida.histogram1D("Cluster Pair Mass: Top Bottom Fiducial", 100, 0.0, 0.35);
        tree.cd(TEMP_DIR);
        hitCountFillPlot = makeCopy(hitCountDrawPlot);
        clusterCountFillPlot = makeCopy(clusterCountDrawPlot);
        /**
         * Turn off aggregation for occupancy/rate plots (JM)
         */
        clusterCountFillPlot.annotation().addItem("aggregate", "false");
        clusterCountDrawPlot.annotation().addItem("aggregate", "false");
        occupancyDrawPlot.annotation().addItem("aggregate", "false");
        hitCountFillPlot.annotation().addItem("aggregate", "false");
        hitCountDrawPlot.annotation().addItem("aggregate", "false");

    }

    public void endOfData() {
        super.endOfData();
    }

    public void process(EventHeader event) {

        super.process(event);

        int nhits = 0;
        int chits[] = new int[11 * 47];

        int orTrigTime = 4097;
        int topTrigTime = 4097;
        int botTrigTime = 4097;

        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            if (!triggerList.isEmpty()) {
                GenericObject triggerData = triggerList.get(0);

                if (triggerData instanceof SSPData) {
                    // TODO: TOP, BOTTOM, OR, and AND triggers are test
                    // run specific parameters and are not supported
                    // by SSPData.
                    orTrigTime = 0; // ((SSPData)triggerData).getOrTrig();
                    topTrigTime = 0; // ((SSPData)triggerData).getTopTrig();
                    botTrigTime = 0; // ((SSPData)triggerData).getBotTrig();

                    orTrigTimePlot.fill(orTrigTime);

                }

            }// end if triggerList isEmpty
        }

        if (event.hasCollection(CalorimeterHit.class, RAW_ECAL_HITS)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, RAW_ECAL_HITS);
            hitCountPlot.fill(hits.size());

            double maxEnergy = 0;
            double topTime = Double.POSITIVE_INFINITY;
            double botTime = Double.POSITIVE_INFINITY;
            double orTime = Double.POSITIVE_INFINITY;
            for (CalorimeterHit hit : hits) {

                int column = hit.getIdentifierFieldValue("ix");
                int row = hit.getIdentifierFieldValue("iy");
                int id = EcalMonitoringUtilities.getHistoIDFromRowColumn(row, column);
                hitCountFillPlot.fill(column, row);
                {
                    chits[id]++;
                    nhits++;
                }
                hitEnergyPlot.fill(hit.getRawEnergy());
                hitTimePlot.fill(hit.getTime());

                if (hit.getTime() < orTime) {
                    orTime = hit.getTime();
                }
                if (hit.getIdentifierFieldValue("iy") > 0 && hit.getTime() < topTime) {
                    topTime = hit.getTime();
                }
                if (hit.getIdentifierFieldValue("iy") < 0 && hit.getTime() < botTime) {
                    botTime = hit.getTime();
                }
                if (hit.getRawEnergy() > maxEnergy) {
                    maxEnergy = hit.getRawEnergy();
                }
            }

            if (orTime != Double.POSITIVE_INFINITY) {
                orTimePlot.fill(orTime);
                orTimePlot2D.fill(orTime, orTrigTime);
            }
            if (topTime != Double.POSITIVE_INFINITY) {
                topTimePlot.fill(topTime);
            }
            if (botTime != Double.POSITIVE_INFINITY) {
                botTimePlot.fill(botTime);
            }
            hitMaxEnergyPlot.fill(maxEnergy);
        }

        if (nhits > 0) {
            for (int ii = 0;
                    ii < (11 * 47); ii++) {
                occupancyFill[ii] += (1. * chits[ii]) / nhits;
            }
        }

        // calorimeter and the bottom.
        List<Cluster> topList = new ArrayList<Cluster>();
        List<Cluster> bottomList = new ArrayList<Cluster>();

        // Track the highest energy cluster in the event.
        double maxEnergy = 0.0;
        if (event.hasCollection(Cluster.class, ECAL_CLUSTERS)) {
            List<Cluster> clusters = event.get(Cluster.class, ECAL_CLUSTERS);
            for (Cluster cluster : clusters) {
                clusterCountFillPlot.fill(cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"), cluster
                        .getCalorimeterHits().get(0).getIdentifierFieldValue("iy"));

                if (cluster.getEnergy() > maxEnergy) {
                    maxEnergy = cluster.getEnergy();
                }

                // Get the list of calorimeter hits and its size.
                List<CalorimeterHit> hitList = cluster.getCalorimeterHits();
                int hitCount = hitList.size();

                // Track cluster statistics.
                double xEnergyWeight = 0.0;
                double yEnergyWeight = 0.0;
                double[] hitTimes = new double[hitCount];
                double totalHitEnergy = 0.0;

                // Iterate over the hits and extract statistics from them.
                for (int hitIndex = 0; hitIndex < hitCount; hitIndex++) {
                    hitTimes[hitIndex] = hitList.get(hitIndex).getTime();
                    totalHitEnergy += hitList.get(hitIndex).getRawEnergy();
                    xEnergyWeight += (hitList.get(hitIndex).getRawEnergy() * hitList.get(hitIndex)
                            .getIdentifierFieldValue("ix"));
                    yEnergyWeight += (hitList.get(hitIndex).getRawEnergy() * hitList.get(hitIndex)
                            .getIdentifierFieldValue("iy"));
                }

                // If the cluster energy exceeds zero, plot the cluster
                // statistics.
                if (cluster.getEnergy() > 0) {
                    clusterSizePlot.fill(hitCount);
                    clusterTimes.fill(StatUtils.mean(hitTimes, 0, hitCount));
                    clusterTimeSigma.fill(Math.sqrt(StatUtils.variance(hitTimes, 0, hitCount)));
                    edgePlot.fill(xEnergyWeight / totalHitEnergy, yEnergyWeight / totalHitEnergy);
                }

                // Fill the single cluster plots.
                clusterEnergyPlot.fill(cluster.getEnergy());

                // Cluster pairs are formed from all top/bottom cluster
                // combinations. To create these pairs, separate the
                // clusters into two lists based on their y-indices.
                if (cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix") > 0) {
                    topList.add(cluster);
                } else {
                    bottomList.add(cluster);
                }
            }
            // Populate the event plots.
            clusterCountPlot.fill(clusters.size());
            if (maxEnergy > 0) {
                clusterMaxEnergyPlot.fill(maxEnergy);
            }

            // Create a list to store cluster pairs.
            List<Cluster[]> pairList = new ArrayList<Cluster[]>(topList.size() * bottomList.size());

            // Form pairs from all possible combinations of clusters
            // from the top and bottom lists.
            for (Cluster topCluster : topList) {
                for (Cluster bottomCluster : bottomList) {
                    // Make a cluster pair array.
                    Cluster[] pair = new Cluster[2];

                    // The lower energy cluster goes in the second slot.
                    if (topCluster.getEnergy() > bottomCluster.getEnergy()) {
                        pair[0] = topCluster;
                        pair[1] = bottomCluster;
                    } else {
                        pair[0] = bottomCluster;
                        pair[1] = topCluster;
                    }

                    // Add the pair to the pair list.
                    pairList.add(pair);
                }
            }

            // Iterate over each pair and calculate the pair cut values.
            for (Cluster[] pair : pairList) {
                // Get the energy slope value.
                double energySumValue = TriggerModule.getValueEnergySum(pair);
                double energyDifferenceValue = TriggerModule.getValueEnergyDifference(pair);
                double energySlopeValue = TriggerModule.getValueEnergySlope(pair, 0.005500);
                double coplanarityValue = TriggerModule.getValueCoplanarity(pair);
                double xMean = ((pair[0].getEnergy() * pair[0].getPosition()[0]) + (pair[1].getEnergy() * pair[1]
                        .getPosition()[0])) / energySumValue;
                double yMean = ((pair[0].getEnergy() * pair[0].getPosition()[1]) + (pair[1].getEnergy() * pair[1]
                        .getPosition()[1])) / energySumValue;

                // Populate the cluster pair plots.
                pairEnergySum.fill(energySumValue, 1);
                ;
                pairEnergyDifference.fill(energyDifferenceValue, 1);
//                pairEnergySlope.fill(energySlopeValue, 1);
                pairCoplanarity.fill(coplanarityValue, 1);
                pairEnergyPositionMeanX.fill(xMean);
                pairEnergyPositionMeanY.fill(yMean);
                pairMassOppositeHalf.fill(getClusterPairMass(pair));

                int idx0 = pair[0].getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                int idx1 = pair[1].getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                if (Math.abs(idx0) > 2 && Math.abs(idx1) > 2) {
                    pairMassOppositeHalfFid.fill(getClusterPairMass(pair));
                }

            }
            // Create a list to store cluster pairs.
            List<Cluster[]> pairListSameHalf = new ArrayList<Cluster[]>();

            // Form pairs from all possible combinations of clusters
            // from the top and bottom lists.
            for (Cluster topCluster : topList) {
                for (Cluster topCluster2 : topList) {
                    // Make a cluster pair array.
                    Cluster[] pair = new Cluster[2];

                    // The lower energy cluster goes in the second slot.
                    if (topCluster.getEnergy() > topCluster2.getEnergy()) {
                        pair[0] = topCluster;
                        pair[1] = topCluster2;
                    } else {
                        pair[0] = topCluster2;
                        pair[1] = topCluster;
                    }

                    // Add the pair to the pair list.
                    pairListSameHalf.add(pair);
                }
            }

            for (Cluster cluster : bottomList) {
                for (Cluster cluster2 : bottomList) {
                    // Make a cluster pair array.
                    Cluster[] pair = new Cluster[2];

                    // The lower energy cluster goes in the second slot.
                    if (cluster.getEnergy() > cluster2.getEnergy()) {
                        pair[0] = cluster;
                        pair[1] = cluster2;
                    } else {
                        pair[0] = cluster2;
                        pair[1] = cluster;
                    }

                    // Add the pair to the pair list.
                    pairListSameHalf.add(pair);
                }
            }
            // Iterate over each pair and calculate the pair cut values.
            for (Cluster[] pair : pairListSameHalf) {
                // Get the energy slope value.
                pairMassSameHalf.fill(getClusterPairMass(pair));
                int idx0 = pair[0].getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                int idx1 = pair[1].getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                if (Math.abs(idx0) > 2 && Math.abs(idx1) > 2) {
                    pairMassSameHalfFid.fill(getClusterPairMass(pair));
                }
            }

        } else {
            clusterCountPlot.fill(0);
        }

        thisTime = System.currentTimeMillis() / 1000;
        thisEventN = event.getEventNumber();
        thisEventTime = event.getTimeStamp() / 1E9;
        if ((thisTime - prevTime) > eventRefreshRate) {
            double scale = 1.;

            if (NoccupancyFill > 0) {
                scale = (thisEventN - prevEventN) / NoccupancyFill;
                scale = scale / (thisEventTime - prevEventTime);
                scale /= 1000.; // do KHz
            }
            // System.out.println("Event: "+thisEventN+" "+prevEventN);
            // System.out.println("Time: "+thisEventTime+" "+prevEventTime);
            // System.out.println("Monitor: "+thisTime+" "+prevTime+" "+NoccupancyFill);
            if (scale > 0) {
                hitCountFillPlot.scale(scale);
                clusterCountFillPlot.scale(scale);
                redraw();
            }
            prevTime = thisTime;
            prevEventN = thisEventN;
            prevEventTime = thisEventTime;
            NoccupancyFill = 0;
        } else {
            NoccupancyFill++;
        }

    }

    private static Map<Integer, Hep3Vector> createStripPositionMap(HpsSiSensor sensor) {
        Map<Integer, Hep3Vector> positionMap = new HashMap<Integer, Hep3Vector>();
        for (ChargeCarrier carrier : ChargeCarrier.values()) {
            if (sensor.hasElectrodesOnSide(carrier)) {
                // SiSensorElectrodes electrodes = sensor.getReadoutElectrodes();
                SiSensorElectrodes strips = (SiSensorElectrodes) sensor.getReadoutElectrodes(carrier);
                ITransform3D parentToLocal = sensor.getReadoutElectrodes(carrier).getParentToLocal();
                ITransform3D localToGlobal = sensor.getReadoutElectrodes(carrier).getLocalToGlobal();
                for (int physicalChannel = 0; physicalChannel < 640; physicalChannel++) {
                    Hep3Vector localStripPosition = strips.getCellPosition(physicalChannel);
                    Hep3Vector stripPosition = parentToLocal.transformed(localStripPosition);
                    Hep3Vector globalStripPosition = localToGlobal.transformed(stripPosition);
                    positionMap.put(physicalChannel, globalStripPosition);
                }
            }
        }
        return positionMap;
    }

    private double getClusterPairMass(Cluster clu1, Cluster clu2) {
        double x0 = clu1.getPosition()[0];
        double y0 = clu1.getPosition()[1];
        double z0 = clu1.getPosition()[2];
        double x1 = clu2.getPosition()[0];
        double y1 = clu2.getPosition()[1];
        double z1 = clu2.getPosition()[2];
        double e0 = clu1.getEnergy();
        double e1 = clu2.getEnergy();
        double xlen0 = sqrt(x0 * x0 + y0 * y0 + z0 * z0);
        double xlen1 = sqrt(x1 * x1 + y1 * y1 + z1 * z1);
        Hep3Vector p0 = new BasicHep3Vector(x0 / xlen0 * e0, y0 / xlen0 * e0, z0 / xlen0 * e0);
        Hep3Vector p1 = new BasicHep3Vector(x1 / xlen1 * e1, y1 / xlen1 * e1, z1 / xlen1 * e1);

        double esum = sqrt(p1.magnitudeSquared()) + sqrt(p0.magnitudeSquared());
        double pxsum = p1.x() + p0.x();
        double pysum = p1.y() + p0.y();
        double pzsum = p1.z() + p0.z();

        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = esum * esum - psum * psum;

        if (evtmass > 0) {
            return Math.sqrt(evtmass);
        } else {
            return -99;
        }
    }

    private double getMomentum(Track trk) {

        double px = trk.getTrackStates().get(0).getMomentum()[0];
        double py = trk.getTrackStates().get(0).getMomentum()[1];
        double pz = trk.getTrackStates().get(0).getMomentum()[2];
        return Math.sqrt(px * px + py * py + pz * pz);
    }

    private IHistogram2D makeCopy(IHistogram2D hist) {
        return aida.histogram2D(hist.title() + "_copy", hist.xAxis().bins(), hist.xAxis().lowerEdge(), hist.xAxis()
                .upperEdge(), hist.yAxis().bins(), hist.yAxis().lowerEdge(), hist.yAxis().upperEdge());
    }

    void redraw() {
        hitCountDrawPlot.reset();
        hitCountDrawPlot.add(hitCountFillPlot);

        hitCountFillPlot.reset();

        clusterCountDrawPlot.reset();
        clusterCountDrawPlot.add(clusterCountFillPlot);

        clusterCountFillPlot.reset();

        occupancyDrawPlot.reset();
        for (int id = 0; id < (47 * 11); id++) {
            int row = EcalMonitoringUtilities.getRowFromHistoID(id);
            int column = EcalMonitoringUtilities.getColumnFromHistoID(id);
            double mean = occupancyFill[id] / NoccupancyFill;

            occupancyFill[id] = 0;
            if ((row != 0) && (column != 0) && (!EcalMonitoringUtilities.isInHole(row, column))) {
                occupancyDrawPlot.fill(column, row, mean);
            }
        }

    }

    public double getClusterPairMass(Cluster[] pair) {
        double x0 = pair[0].getPosition()[0];
        double y0 = pair[0].getPosition()[1];
        double z0 = pair[0].getPosition()[2];
        double x1 = pair[1].getPosition()[0];
        double y1 = pair[1].getPosition()[1];
        double z1 = pair[1].getPosition()[2];
        double e0 = pair[0].getEnergy();
        double e1 = pair[1].getEnergy();
        double xlen0 = sqrt(x0 * x0 + y0 * y0 + z0 * z0);
        double xlen1 = sqrt(x1 * x1 + y1 * y1 + z1 * z1);
        Hep3Vector p0 = new BasicHep3Vector(x0 / xlen0 * e0, y0 / xlen0 * e0, z0 / xlen0 * e0);
        Hep3Vector p1 = new BasicHep3Vector(x1 / xlen1 * e1, y1 / xlen1 * e1, z1 / xlen1 * e1);

        double esum = sqrt(p1.magnitudeSquared()) + sqrt(p0.magnitudeSquared());
        double pxsum = p1.x() + p0.x();
        double pysum = p1.y() + p0.y();
        double pzsum = p1.z() + p0.z();

        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = esum * esum - psum * psum;

        if (evtmass > 0) {
            return Math.sqrt(evtmass);
        } else {
            return -99;
        }
    }
}
