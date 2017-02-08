package org.hps.recon.tracking;

import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.NearestNeighborRMS;
import org.lcsim.recon.tracking.digitization.sisim.RawTrackerHitMaker;
import org.lcsim.recon.tracking.digitization.sisim.StripHitMaker;

/**
 * This Driver runs the tracker digitization to create raw hits and strip hits from simulated data.
 * The output can be used by a track reconstruction algorithm like Seed Tracker.
 * 
 * Copied from org.lcsim.hps.recon.tracking.TrackerDigiDriver, with the difference that this driver
 * does not make noise hits or add noise to hits, and drops bad channels.
 */
public class SimpleTrackerDigiDriver extends TrackerDigiDriver {

    private boolean dropBadChannels = false;

    public void setDropBadChannels(boolean dropBadChannels) {
        this.dropBadChannels = dropBadChannels;
    }

    /**
     * Initializes this Driver's objects with the job parameters.
     */
    @Override
    protected void initialize() {

        // Create the sensor simulation.
        CDFSiSensorSim stripSim = new CDFSiSensorSim();

        // Create the readout chips and set the noise parameters.
        NoiselessReadoutChip stripReadout = new NoiselessReadoutChip();
        stripReadout.setDropBadChannels(dropBadChannels);
        stripReadout.setNoiseIntercept(readoutNoiseIntercept);
        stripReadout.setNoiseSlope(readoutNoiseSlope);
        stripReadout.setNbits(readoutNBits);
        stripReadout.setDynamicRange(readoutDynamicRange);

        // Create the digitizer that produces the raw hits
        stripDigitizer = new RawTrackerHitMaker(stripSim, stripReadout);

        // Create Strip clustering algorithm.
        NearestNeighborRMS stripClusteringAlgo = new NearestNeighborRMS();
        stripClusteringAlgo.setSeedThreshold(clusterSeedThreshold);
        stripClusteringAlgo.setNeighborThreshold(clusterNeighborThreshold);
        stripClusteringAlgo.setClusterThreshold(clusterThreshold);

        // Create the clusterers and set hit-making parameters.
        stripClusterer = new StripHitMaker(stripSim, stripReadout, stripClusteringAlgo);
        stripClusterer.setMaxClusterSize(clusterMaxSize);
        stripClusterer.setCentralStripAveragingThreshold(clusterCentralStripAveragingThreshold);

        // Set the cluster errors.
        stripClusterer.SetOneClusterErr(oneClusterErr);
        stripClusterer.SetTwoClusterErr(twoClusterErr);
        stripClusterer.SetThreeClusterErr(threeClusterErr);
        stripClusterer.SetFourClusterErr(fourClusterErr);
        stripClusterer.SetFiveClusterErr(fiveClusterErr);

        // Set the readout to process.
        readouts.add(readoutCollectionName);

        // Set the detector to process.
        processPaths.add(subdetectorName);
    }
}