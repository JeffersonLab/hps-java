package org.lcsim.hps.readout.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.evio.TriggerData;
import org.lcsim.hps.recon.ecal.ECalUtils;
import org.lcsim.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.hps.util.ClockSingleton;
import org.lcsim.util.aida.AIDA;

/**
 * Reads clusters and makes trigger decision using opposite quadrant criterion.
 * Prints triggers to file if file path specified.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: FADCTriggerDriver.java,v 1.4 2013/09/02 21:56:56 phansson Exp $
 */
public class FADCTriggerDriver extends TriggerDriver {

    // A list to contain all cluster pairs in an event
    List<HPSEcalCluster[]> clusterPairs;
    int nTriggers;
    int totalEvents;
    protected double beamEnergy = 2.2 * ECalUtils.GeV;
    private double clusterEnergyHigh = 1.85 / 2.2;
    private double clusterEnergyLow = .1 / 2.2;
    private double energySumThreshold = 1.0;
    private double energyDifferenceThreshold = 1.5 / 2.2;
    private double maxCoplanarityAngle = 35; // degrees
//    private double energyDistanceDistance = 250; // mm
//    private double energyDistanceThreshold = 0.8 / 2.2;
    private double energyDistanceDistance = 200; // mm
    private double energyDistanceThreshold = 0.5;
    int allPairs;
    int oppositeQuadrantCount;
    int clusterEnergyCount;
    int energySumCount;
    int energyDifferenceCount;
    int energyDistanceCount;
    int coplanarityCount;
    AIDA aida = AIDA.defaultInstance();
    IHistogram2D clusterEnergy2DAll, clusterSumDiff2DAll, energyDistance2DAll, clusterAngles2DAll, clusterCoplanarity2DAll;
    IHistogram2D clusterEnergy2D, clusterSumDiff2D, energyDistance2D, clusterAngles2D, clusterCoplanarity2D;
    IHistogram1D triggerBits1D, triggerTimes1D;
    int truthPeriod = 250;
    private boolean useQuadrants = false;
    protected String clusterCollectionName = "EcalClusters";

    private enum Flag {

        CLUSTER_ENERGY(3), ENERGY_SUM_DIFF(2), ENERGY_DISTANCE(1), COPLANARITY(0);
        private final int index;

        Flag(int i) {
            index = i;
        }

        static int bitmask(EnumSet<Flag> flags) {
            int mask = 0;
            for (Flag flag : flags) {
                mask |= 1 << flag.index;
            }
            return mask;
        }
    }

    public FADCTriggerDriver() {
        clusterPairs = new LinkedList<HPSEcalCluster[]>();
    }

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    public void setBeamEnergy(double beamEnergy) {
        if (beamEnergy == 1.1) {
            System.out.println(this.getClass().getSimpleName() + ": Setting trigger for 1.1 GeV beam");
            maxCoplanarityAngle = 90;
            clusterEnergyHigh = .7 / beamEnergy;
            clusterEnergyLow = .1 / beamEnergy;
            energySumThreshold = 0.8 / beamEnergy;
        } else if (beamEnergy == 2.2) {
            System.out.println(this.getClass().getSimpleName() + ": Setting trigger for 2.2 GeV beam");
            maxCoplanarityAngle = 45;
            clusterEnergyHigh = 1.6 / beamEnergy;
            clusterEnergyLow = .1 / beamEnergy;
            energySumThreshold = 1.7 / beamEnergy;
        } else if (beamEnergy == 6.6) {
            System.out.println(this.getClass().getSimpleName() + ": Setting trigger for 6.6 GeV beam");
            maxCoplanarityAngle = 60;
            clusterEnergyHigh = 5.0 / beamEnergy;
            clusterEnergyLow = .1 / beamEnergy;
            energySumThreshold = 5.5 / beamEnergy;
        }
        this.beamEnergy = beamEnergy * ECalUtils.GeV;
    }

    protected double getBeamEnergyFromDetector(Detector detector) {
        if (detector.getName().contains("1pt1")) {
            return 1.1;
        } else if (detector.getName().contains("2pt2")) {
            return 2.2;
        } else if (detector.getName().contains("6pt6")) {
            return 6.6;
        } else {
            return -1.0;
        }
    }

    public void setTruthPeriod(int truthPeriod) {
        this.truthPeriod = truthPeriod;
    }

    @Override
    public void detectorChanged(Detector detector) {
        setBeamEnergy(this.getBeamEnergyFromDetector(detector));

        clusterSumDiff2DAll = aida.histogram2D("All cluster pairs: energy difference vs. sum", 100, 0.0, 2 * beamEnergy, 100, 0.0, beamEnergy);
        clusterEnergy2DAll = aida.histogram2D("All cluster pairs: energy (less energetic vs. more energetic)", 100, 0.0, 2 * beamEnergy, 100, 0.0, beamEnergy);
        energyDistance2DAll = aida.histogram2D("All cluster pairs: distance vs. energy (less energetic cluster)", 100, 0.0, 0.5 * beamEnergy, 25, 0.0, 400.0);
        clusterCoplanarity2DAll = aida.histogram2D("All cluster pairs: cluster angle uncoplanarity vs. less energetic cluster angle", 100, -180.0, 180.0, 100, -180.0, 180.0);
        clusterAngles2DAll = aida.histogram2D("All cluster pairs: cluster angle (less energetic vs. more energetic)", 100, -180.0, 180.0, 100, -180.0, 180.0);

        clusterSumDiff2D = aida.histogram2D("Passed other cuts: energy difference vs. sum", 100, 0.0, 2 * beamEnergy, 100, 0.0, beamEnergy);
        clusterEnergy2D = aida.histogram2D("Passed other cuts: energy (less energetic vs. more energetic)", 100, 0.0, 2 * beamEnergy, 100, 0.0, beamEnergy);
        energyDistance2D = aida.histogram2D("Passed other cuts: distance vs. energy (less energetic cluster)", 100, 0.0, 0.5 * beamEnergy, 25, 0.0, 400.0);
        clusterCoplanarity2D = aida.histogram2D("Passed other cuts: cluster angle uncoplanarity vs. less energetic cluster angle", 100, -180.0, 180.0, 100, -180.0, 180.0);
        clusterAngles2D = aida.histogram2D("Passed other cuts: cluster angle (less energetic vs. more energetic)", 100, -180.0, 180.0, 100, -180.0, 180.0);

        triggerBits1D = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollectionName + " : trigger bits", 17, -1.5, 15.5);
        triggerTimes1D = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollectionName + " : trigger times", truthPeriod, -0.5, truthPeriod - 0.5);
    }

    @Override
    public void startOfData() {
        super.startOfData();
        if (clusterCollectionName == null) {
            throw new RuntimeException("The parameter clusterCollectionName was not set!");
        }

        allPairs = 0;
        oppositeQuadrantCount = 0;
        clusterEnergyCount = 0;
        energySumCount = 0;
        energyDifferenceCount = 0;
        energyDistanceCount = 0;
        coplanarityCount = 0;
    }

    @Override
    protected boolean triggerDecision(EventHeader event) {
        // Get the list of raw ECal hits.
        if (event.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
            return testTrigger(event.get(HPSEcalCluster.class, clusterCollectionName));
        } else {
            return false;
        }
    }

    public boolean testTrigger(List<HPSEcalCluster> clusters) {
        boolean trigger = false;

        if (useQuadrants) {
            getClusterPairs(clusters);
        } else {
            getClusterPairsTopBot(clusters);
        }

        //--- Apply Trigger Cuts ---//

        // Iterate through all cluster pairs present in the event.  If at least
        // one of the cluster pairs satisfies all of the trigger conditions,
        // a trigger signal is sent to all other detectors.
        for (HPSEcalCluster[] clusterPair : clusterPairs) {

            EnumSet<Flag> bits = EnumSet.noneOf(Flag.class);

            if (outputStream != null) {
                outputStream.printf("Event %d: cluster pair (energy %f in quadrant %d (%s), energy %f in quadrant %d (%s))\n",
                        ClockSingleton.getClock(),
                        clusterPair[0].getEnergy(), ECalUtils.getQuadrant(clusterPair[0]), clusterPair[0].getSeedHit().getPositionVec().toString(),
                        clusterPair[1].getEnergy(), ECalUtils.getQuadrant(clusterPair[1]), clusterPair[1].getSeedHit().getPositionVec().toString());
            }

            allPairs++;

            if (useQuadrants) {
                // Require that the event have at least two clusters in opposite
                // quadrants
                if (!oppositeQuadrantsCut(clusterPair)) {
                    if (outputStream != null) {
                        outputStream.println("Failed opposite quadrant cut");
                    }
                    continue;
                }
                oppositeQuadrantCount++;
            }

            // Require the components of a cluster pair to have an energy in
            // the range of 100 MeV to 1.85 GeV
            if (clusterECut(clusterPair)) {
                bits.add(Flag.CLUSTER_ENERGY);
            }

            bits.add(Flag.ENERGY_SUM_DIFF);
            // Require the sum of the energies of the components of the
            // cluster pair to be less than the
            // (Beam Energy)*(Sampling Fraction) ( 2 GeV for the Test Run )
            if (!energySum(clusterPair)) {
                bits.remove(Flag.ENERGY_SUM_DIFF);
            }

            // Require the difference in energy of the components of the
            // cluster pair to be less than 1.5 GeV
            if (!energyDifference(clusterPair)) {
                bits.remove(Flag.ENERGY_SUM_DIFF);
            }

            // Apply a low energy cluster vs. distance cut of the form
            // E_low + .0032 GeV/mm < .8 GeV
            if (energyDistanceCut(clusterPair)) {
                bits.add(Flag.ENERGY_DISTANCE);
            }

            // Require that the two clusters are coplanar with the beam within
            // 35 degrees
            if (coplanarityCut(clusterPair)) {
                bits.add(Flag.COPLANARITY);
            }

            if (bits.contains(Flag.CLUSTER_ENERGY)) {
                clusterEnergyCount++;
                if (energySum(clusterPair)) {
                    energySumCount++;
                    if (energyDifference(clusterPair)) {
                        energyDifferenceCount++;
                        if (bits.contains(Flag.ENERGY_DISTANCE)) {
                            energyDistanceCount++;
                            if (bits.contains(Flag.COPLANARITY)) {
                                coplanarityCount++;
                            } else if (outputStream != null) {
                                outputStream.println("Failed coplanarity cut");
                            }
                        } else if (outputStream != null) {
                            outputStream.println("Failed energy-distance cut");
                        }
                    } else if (outputStream != null) {
                        outputStream.println("Failed energy difference cut");
                    }
                } else if (outputStream != null) {
                    outputStream.println("Failed energy sum cut");
                }
            } else if (outputStream != null) {
                outputStream.println("Failed cluster energy cut");
            }

            clusterSumDiff2DAll.fill(clusterPair[0].getEnergy() + clusterPair[1].getEnergy(), clusterPair[0].getEnergy() - clusterPair[1].getEnergy());
            clusterEnergy2DAll.fill(clusterPair[0].getEnergy(), clusterPair[1].getEnergy());
            energyDistance2DAll.fill(clusterPair[1].getEnergy(), getClusterDistance(clusterPair[1]));
            clusterCoplanarity2DAll.fill(getClusterAngle(clusterPair[1]), pairUncoplanarity(clusterPair));
            clusterAngles2DAll.fill(getClusterAngle(clusterPair[0]), getClusterAngle(clusterPair[1]));

            if (bits.containsAll(EnumSet.complementOf(EnumSet.of(Flag.ENERGY_SUM_DIFF, Flag.CLUSTER_ENERGY)))) { //cluster energy, energy-distance, coplanarity
                clusterSumDiff2D.fill(clusterPair[0].getEnergy() + clusterPair[1].getEnergy(), clusterPair[0].getEnergy() - clusterPair[1].getEnergy());
                clusterEnergy2D.fill(clusterPair[0].getEnergy(), clusterPair[1].getEnergy());
            }
            if (bits.containsAll(EnumSet.complementOf(EnumSet.of(Flag.ENERGY_DISTANCE)))) {
                energyDistance2D.fill(clusterPair[1].getEnergy(), getClusterDistance(clusterPair[1]));
            }
            if (bits.containsAll(EnumSet.complementOf(EnumSet.of(Flag.COPLANARITY)))) {
                clusterCoplanarity2D.fill(getClusterAngle(clusterPair[1]), pairUncoplanarity(clusterPair));
                clusterAngles2D.fill(getClusterAngle(clusterPair[0]), getClusterAngle(clusterPair[1]));
            }

            triggerBits1D.fill(Flag.bitmask(bits));

            if (bits.containsAll(EnumSet.allOf(Flag.class))) {
                // If all cuts are pased, we have a trigger
                if (outputStream != null) {
                    outputStream.println("Passed all cuts");
                }
                trigger = true;
            }
        }
        if (trigger) {
            triggerBits1D.fill(-1);
            triggerTimes1D.fill(ClockSingleton.getClock() % truthPeriod);
        }
        return trigger;
    }

    @Override
    public void endOfData() {
        if (outputStream != null) {
            printCounts(outputStream);
        }
        printCounts(new PrintWriter(System.out));
        super.endOfData();
    }

    private void printCounts(PrintWriter writer) {
        writer.printf("Number of pairs: %d\n", allPairs);
        writer.printf("Number of cluster pairs after successive trigger conditions:\n");
        if (useQuadrants) {
            writer.printf("Opposite quadrants: %d\n", oppositeQuadrantCount);
        }
        writer.printf("Cluster energy: %d\n", clusterEnergyCount);
        writer.printf("Energy sum: %d\n", energySumCount);
        writer.printf("Energy difference: %d\n", energyDifferenceCount);
        writer.printf("Energy-distance cut: %d\n", energyDistanceCount);
        writer.printf("Coplanarity: %d\n", coplanarityCount);
        writer.printf("Trigger count: %d\n", numTriggers);
        writer.close();
    }

    /**
     * Get a list of all unique cluster pairs in the event
     *
     * @param ecalClusters : List of ECal clusters
     * @return true if there are any cluster pairs
     */
    protected boolean getClusterPairs(List<HPSEcalCluster> ecalClusters) {
        // Create a list which will hold all neighboring cluster to the cluster
        // of interest
        List< HPSEcalCluster> ecalClusterNeighbors = new LinkedList< HPSEcalCluster>();
        ecalClusterNeighbors.addAll(ecalClusters);

        // Clear the list of cluster pairs
        clusterPairs.clear();

        for (HPSEcalCluster ecalCluster : ecalClusters) {
            // Create a list of neighbors to the cluster of interest
            ecalClusterNeighbors.remove(ecalCluster);

            // Loop over all neigboring clusters and check to see if there is
            // any which lie in opposing quadrants to the cluster of interest.
            // If so, add them to the list of cluster pairs
            for (HPSEcalCluster ecalClusterNeighbor : ecalClusterNeighbors) {
                if (ecalCluster.getEnergy() > ecalClusterNeighbor.getEnergy()) {
                    HPSEcalCluster[] clusterPair = {ecalCluster, ecalClusterNeighbor};
                    clusterPairs.add(clusterPair);
                } else {
                    HPSEcalCluster[] clusterPair = {ecalClusterNeighbor, ecalCluster};
                    clusterPairs.add(clusterPair);
                }
            }
        }

        return !clusterPairs.isEmpty();
    }

    protected boolean getClusterPairsTopBot(List<HPSEcalCluster> ecalClusters) {
        // Create a list which will hold all neighboring cluster to the cluster
        // of interest
        List< HPSEcalCluster> topClusters = new ArrayList< HPSEcalCluster>();
        List< HPSEcalCluster> botClusters = new ArrayList< HPSEcalCluster>();
        for (HPSEcalCluster ecalCluster : ecalClusters) {
            if (ecalCluster.getSeedHit().getIdentifierFieldValue("iy") > 0) {
                topClusters.add(ecalCluster);
            } else {
                botClusters.add(ecalCluster);
            }
        }
        // Clear the list of cluster pairs
        clusterPairs.clear();

        // Loop over all top-bottom pairs of clusters; higher-energy cluster goes first in the pair
        for (HPSEcalCluster topCluster : topClusters) {
            for (HPSEcalCluster botCluster : botClusters) {
                if (topCluster.getEnergy() > botCluster.getEnergy()) {
                    HPSEcalCluster[] clusterPair = {topCluster, botCluster};
                    clusterPairs.add(clusterPair);
                } else {
                    HPSEcalCluster[] clusterPair = {botCluster, topCluster};
                    clusterPairs.add(clusterPair);
                }
            }
        }
        return !clusterPairs.isEmpty();
    }

    /**
     * Checks if the ECal clusters making up a cluster pair lie in opposite
     * quadrants
     *
     * @param clusterPair : pair of clusters
     * @return true if opposite quadrants, false otherwise
     */
    protected boolean oppositeQuadrantsCut(HPSEcalCluster[] clusterPair) {
        int quad1 = ECalUtils.getQuadrant(clusterPair[0]);
        int quad2 = ECalUtils.getQuadrant(clusterPair[1]);

        //if clusters are in the same quadrant, they're not opposite quadrants
        if (quad1 == quad2) {
            return false;
        } //opposite pairs of quadrants are either both even (2 and 4) or both odd (1 and 3)
        else {
            return ((quad1 & 1) == (quad2 & 1));
        }
    }

    /**
     * Checks if the ECal clusters making up a cluster pair lie above the low
     * energy threshold and below the high energy threshold
     *
     * @param clusterPair : pair of clusters
     * @return true if a pair is found, false otherwise
     */
    protected boolean clusterECut(HPSEcalCluster[] clusterPair) {
        return (clusterPair[0].getEnergy() < beamEnergy * clusterEnergyHigh
                && clusterPair[1].getEnergy() < beamEnergy * clusterEnergyHigh
                && clusterPair[0].getEnergy() > beamEnergy * clusterEnergyLow
                && clusterPair[1].getEnergy() > beamEnergy * clusterEnergyLow);
    }

    /**
     * Checks if the sum of the energies of ECal clusters making up a cluster
     * pair is below an energy sum threshold
     *
     * @param clusterPair : pair of clusters
     * @return true if a pair is found, false otherwise
     */
    protected boolean energySum(Cluster[] clusterPair) {
        double clusterESum = clusterPair[0].getEnergy() + clusterPair[1].getEnergy();
        return (clusterESum < beamEnergy * energySumThreshold);
    }

    /**
     * Checks if the energy difference between the ECal clusters making up a
     * cluster pair is below an energy difference threshold
     *
     * @param clusterPair : pair of clusters
     * @return true if pair is found, false otherwise
     */
    protected boolean energyDifference(HPSEcalCluster[] clusterPair) {
        double clusterEDifference = clusterPair[0].getEnergy() - clusterPair[1].getEnergy();

        return (clusterEDifference < beamEnergy * energyDifferenceThreshold);
    }

    /**
     * Require that the distance from the beam of the lowest energy cluster in a
     * cluster pair satisfies the following E_low + d_b*.0032 GeV/mm < .8 GeV
     *
     * @param clusterPair : pair of clusters
     * @return true if pair is found, false otherwise
     */
    protected boolean energyDistanceCut(HPSEcalCluster[] clusterPair) {
        HPSEcalCluster lowEnergyCluster = clusterPair[1];

        // Calculate its position
        double lowEClusterDistance = getClusterDistance(clusterPair[1]);
        // event passes cut if above the line with X- and Y-intercepts defined by energyDistanceDistance and beamEnergy*energyDistanceThreshold
        double clusterDistvsE = lowEnergyCluster.getEnergy() + lowEClusterDistance * beamEnergy * energyDistanceThreshold / energyDistanceDistance;

        return (clusterDistvsE > beamEnergy * energyDistanceThreshold);
    }

    /**
     * Checks if a cluster pair is coplanar to the beam within a given angle
     *
     * @param clusterPair : pair of clusters
     * @return true if pair is found, false otherwise
     */
    protected boolean coplanarityCut(HPSEcalCluster[] clusterPair) {
        return (Math.abs(pairUncoplanarity(clusterPair)) < maxCoplanarityAngle);
    }

    protected double pairUncoplanarity(HPSEcalCluster[] clusterPair) { // Find the angle between clusters in the pair
        double cluster1Angle = (getClusterAngle(clusterPair[0]) + 180.0) % 180.0;
        double cluster2Angle = (getClusterAngle(clusterPair[1]) + 180.0) % 180.0;

        return cluster2Angle - cluster1Angle;
    }

    protected double getClusterAngle(HPSEcalCluster cluster) { //returns angle in range of -180 to 180
        double position[] = cluster.getSeedHit().getPosition();
        return Math.toDegrees(Math.atan2(position[1], position[0]));
    }

    protected double getClusterDistance(HPSEcalCluster cluster) {
        return Math.hypot(cluster.getSeedHit().getPosition()[0], cluster.getSeedHit().getPosition()[1]);
    }
}