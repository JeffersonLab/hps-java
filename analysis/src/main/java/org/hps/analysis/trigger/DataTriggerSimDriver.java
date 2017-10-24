package org.hps.analysis.trigger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import org.hps.analysis.trigger.util.PairTrigger;
import org.hps.analysis.trigger.util.SinglesTrigger;
import org.hps.analysis.trigger.util.Trigger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.record.daqconfig.ConfigurationManager;
import org.hps.record.daqconfig.DAQConfig;
import org.hps.record.daqconfig.PairTriggerConfig;
import org.hps.record.daqconfig.SinglesTriggerConfig;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;

/**
 * Class <code>DataTriggerSimDriver</code> takes in clusters of both
 * class <code>Cluster</code> and <code>SSPCluster</code> and performs
 * a simulation of the hardware trigger on them. The results of this
 * simulation are then stored in a <code>SimTriggerData</code> object
 * which is placed on the data stream to allow for other drivers to
 * access the trigger results.<br/>
 * <br/>
 * <code>DataTriggerSimDriver</code> is intended exclusively as a
 * hardware simulation, and as such, requires that the DAQ configuration
 * is read before it can function. Thusly, <code>DAQConfigDriver</code> 
 * must exist upstream of this driver for it to initialize. Additionally,
 * to ensure consistency with data, it is advised that online FADC driver
 * be employed and the GTP clustering driver be set to draw from the
 * DAQ configuration as well.<br/>
 * <br/>
 * <code>DataTriggerSimDriver</code> requires two input collections.
 * The first is the bank collection, which contains the TI and SSP
 * banks. It uses the SSP bank to obtain hardware clusters. It also
 * requires the reconstructed cluster bank to obtain the clusters that
 * are simulated from FADC hits. The driver outputs one collection, the
 * simulated trigger collection, which contains simulated triggers.
 * This collection consists of one <code>SimTriggerData</code> object
 * that can be accessed to obtain all simulated trigger types. This
 * output object is not persisted into LCIO after runtime.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.record.daqconfig.DAQConfigDriver
 * @see org.hps.recon.ecal.EcalOnlineRawConverterDriver
 * @see org.hps.recon.ecal.cluster.GTPOnlineClusterDriver
 * @see SimTriggerData
 */
public class DataTriggerSimDriver extends Driver {

    // Store the LCIO collection names for the needed objects.
    private boolean verbose = false;
    private boolean filterUnverifiable = false;
    private String bankCollectionName = "TriggerBank";
    private String clusterCollectionName = "EcalClusters";
    private String simTriggerCollectionName = "SimTriggers";

    // Store the SSP bank.
    private SSPData sspBank = null;

    // Store cluster verifiability parameters.
    private int nsa = 0;
    private int nsb = 0;
    private int windowWidth = 0;

    // Define trigger simulation modules.
    private boolean[] pairTriggerEnabled = new boolean[2];
    private boolean[] singlesTriggerEnabled = new boolean[2];
    private boolean[][] pairCutsEnabled = new boolean[2][7];
    private boolean[][] singlesCutsEnabled = new boolean[2][3];
    private TriggerModule[] pairsTrigger = new TriggerModule[2];
    private TriggerModule[] singlesTrigger = new TriggerModule[2];

    // Reference variables.
    private static final int ENERGY_MIN = 0;
    private static final int ENERGY_MAX = 1;
    private static final int HIT_COUNT = 2;
    private static final int ENERGY_SUM = 0;
    private static final int ENERGY_DIFF = 1;
    private static final int ENERGY_SLOPE = 2;
    private static final int COPLANARITY = 3;

    // Plots
    private AIDA aida = AIDA.defaultInstance();
    private IHistogram1D[][] triggerTime = {
            {aida.histogram1D("Trigger Sim/Sim Cluster/Singles 0 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Singles 1 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 0 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 1 Trigger Time", 51, -2, 202)},
            {aida.histogram1D("Trigger Sim/SSP Cluster/Singles 0 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/SSP Cluster/Singles 1 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/SSP Cluster/Pair 0 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/SSP Cluster/Pair 1 Trigger Time", 51, -2, 202)}};
    private IHistogram1D[][] triggerCount = {
            {aida.histogram1D("Trigger Sim/Sim Cluster/Singles 0 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Singles 1 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 0 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 1 Trigger Count", 10, -0.5, 9.5)},
            {aida.histogram1D("Trigger Sim/SSP Cluster/Singles 0 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/SSP Cluster/Singles 1 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/SSP Cluster/Pair 0 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/SSP Cluster/Pair 1 Trigger Count", 10, -0.5, 9.5)}};
    private IHistogram1D sspClusterTime = aida.histogram1D("Trigger Sim/SSP Cluster/Cluster Time", 51, -2, 202);
    private IHistogram1D simClusterAllTime = aida.histogram1D("Trigger Sim/Sim Cluster/Cluster Time", 51, -2, 202);
    private IHistogram1D simClusterVerifiedTime = aida.histogram1D("Trigger Sim/Sim Cluster/Verified Cluster Time", 51,
            -2, 202);

    /**
     * Connects the driver to the the <code>ConfigurationManager</code> in order to obtain the correct trigger
     * information. Trigger
     * settings are stored in the <code>TriggerModule</code> objects.
     */
    @Override
    public void startOfData() {
        // Define the first singles trigger.
        singlesTrigger[0] = new TriggerModule();
        singlesTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.500);
        singlesTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
        singlesTrigger[0].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);

        // Define the second singles trigger.
        singlesTrigger[1] = new TriggerModule();
        singlesTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.000);
        singlesTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
        singlesTrigger[1].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);

        // Define the first pairs trigger.
        pairsTrigger[0] = new TriggerModule();
        pairsTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.000);
        pairsTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
        pairsTrigger[0].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW, 0.000);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH, 8.191);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 8.191);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW, 0.000);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F, 0.001);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH, 180);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE, 8);

        // Define the second pairs trigger.
        pairsTrigger[1] = new TriggerModule();
        pairsTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.000);
        pairsTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
        pairsTrigger[1].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW, 0.000);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH, 8.191);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 8.191);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW, 0.000);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F, 0.001);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH, 180);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE, 8);

        // Listen for the configuration manager to provide the real
        // trigger settings.
        ConfigurationManager.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the DAQ configuration.
                DAQConfig daq = ConfigurationManager.getInstance();

                // Get cluster verifiability parameters.
                nsa = daq.getFADCConfig().getNSA();
                nsb = daq.getFADCConfig().getNSB();
                windowWidth = daq.getFADCConfig().getWindowWidth();

                // Load the DAQ settings from the configuration manager.
                singlesTrigger[0].loadDAQConfiguration(daq.getSSPConfig().getSingles1Config());
                singlesTrigger[1].loadDAQConfiguration(daq.getSSPConfig().getSingles2Config());
                pairsTrigger[0].loadDAQConfiguration(daq.getSSPConfig().getPair1Config());
                pairsTrigger[1].loadDAQConfiguration(daq.getSSPConfig().getPair2Config());

                // Get the trigger configurations from the DAQ.
                SinglesTriggerConfig[] singles = {daq.getSSPConfig().getSingles1Config(),
                        daq.getSSPConfig().getSingles2Config()};
                PairTriggerConfig[] pairs = {daq.getSSPConfig().getPair1Config(), daq.getSSPConfig().getPair2Config()};

                // Update the enabled/disabled statuses.
                for (int i = 0; i < 2; i++) {
                    // Set the trigger enabled status.
                    pairTriggerEnabled[i] = pairs[i].isEnabled();
                    singlesTriggerEnabled[i] = singles[i].isEnabled();

                    // Set the singles cut statuses.
                    singlesCutsEnabled[i][ENERGY_MIN] = singles[i].getEnergyMinCutConfig().isEnabled();
                    singlesCutsEnabled[i][ENERGY_MAX] = singles[i].getEnergyMaxCutConfig().isEnabled();
                    singlesCutsEnabled[i][HIT_COUNT] = singles[i].getHitCountCutConfig().isEnabled();

                    // Set the pair cut statuses.
                    pairCutsEnabled[i][ENERGY_MIN] = pairs[i].getEnergyMinCutConfig().isEnabled();
                    pairCutsEnabled[i][ENERGY_MAX] = pairs[i].getEnergyMaxCutConfig().isEnabled();
                    pairCutsEnabled[i][HIT_COUNT] = pairs[i].getHitCountCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + ENERGY_SUM] = pairs[i].getEnergySumCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + ENERGY_DIFF] = pairs[i].getEnergyDifferenceCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + ENERGY_SLOPE] = pairs[i].getEnergySlopeCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + COPLANARITY] = pairs[i].getCoplanarityCutConfig().isEnabled();
                }
            }
        });
    }

    /**
     * Processes an LCIO event and simulates triggers in the same manner
     * as the hardware for both <code>SSPCluster</code> objects as well
     * as <code>Cluster</code> objects reconstructed from FADC hits.
     * Triggers are then output to the data stream.
     * 
     * @param event - The <code>EventHeader</code> object representing
     * the current LCIO event.
     */
    @Override
    public void process(EventHeader event) {
        // If the DAQ configuration manager has not been initialized,
        // then no action can be performed.
        if (!ConfigurationManager.isInitialized()) {
            // Put an empty trigger results module into the data stream.
            SimTriggerData triggerData = new SimTriggerData();
            List<SimTriggerData> dataList = new ArrayList<SimTriggerData>(1);
            dataList.add(triggerData);
            event.put(simTriggerCollectionName, dataList, SimTriggerData.class, 0);

            // Nothing further can be done, since trigger settings are
            // not yet defined.
            return;
        }

        // Get the SSP bank.
        if (event.hasCollection(GenericObject.class, bankCollectionName)) {
            // Get the bank list.
            List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);

            // Search through the banks and get the SSP and TI banks.
            for (GenericObject obj : bankList) {
                // If this is an SSP bank, parse it.
                if (AbstractIntData.getTag(obj) == SSPData.BANK_TAG) {
                    sspBank = new SSPData(obj);
                }
            }
        }

        // Get a list of SSPClusters.
        /**
         * List<SSPCluster> tempSSPClusters = null;
         * List<SSPCluster> sspClusters = null;
         * if(sspBank != null) { tempSSPClusters = sspBank.getClusters(); }
         * else { tempSSPClusters = new ArrayList<SSPCluster>(0); }
         * 
         * sspClusters = new ArrayList<SSPCluster>();
         * for(SSPCluster cluster : tempSSPClusters) {
         * if(TriggerDiagnosticUtil.isVerifiable(cluster, nsa, nsb, windowWidth)) { sspClusters.add(cluster); }
         * }
         **/
        List<SSPCluster> sspClusters = null;
        if (sspBank != null) {
            sspClusters = sspBank.getClusters();
        } else {
            sspClusters = new ArrayList<SSPCluster>(0);
        }

        // Plot the SSP cluster time distribution.
        for (SSPCluster cluster : sspClusters) {
            sspClusterTime.fill(TriggerModule.getClusterTime(cluster));
        }

        // DEBUG :: Print the SSP clusters.
        if (verbose) {
            System.out.println("SSP Clusters:");
            for (SSPCluster cluster : sspClusters) {
                System.out.printf("\t(%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n",
                        TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster),
                        TriggerModule.getValueClusterTotalEnergy(cluster), TriggerModule.getClusterHitCount(cluster),
                        TriggerModule.getClusterTime(cluster));
            }
        }

        // Get reconstructed clusters.
        List<Cluster> reconClusters = null;
        if (event.hasCollection(Cluster.class, clusterCollectionName)) {
            reconClusters = event.get(Cluster.class, clusterCollectionName);
        } else {
            reconClusters = new ArrayList<Cluster>(0);
        }

        // If only "verifiable" clusters should be used, test all the
        // reconstructed clusters for verifiability.
        if (filterUnverifiable) {
            // Create a list to store the verifiable clusters.
            List<Cluster> goodClusters = new ArrayList<Cluster>();

            // Iterate over all the clusters and test them to see if
            // they are verifiable.
            if (verbose) {
                System.out.println("Sim Clusters:");
            }
            for (Cluster cluster : reconClusters) {
                // Plot the cluster time.
                simClusterAllTime.fill(TriggerModule.getClusterTime(cluster));

                // DEBUG :: Print the cluster.
                if (verbose) {
                    System.out.printf("\t(%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f;  ",
                            TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster),
                            TriggerModule.getValueClusterTotalEnergy(cluster),
                            TriggerModule.getClusterHitCount(cluster), TriggerModule.getClusterTime(cluster));
                }

                // Check if the cluster is at risk of pulse-clipping.
                if (TriggerDiagnosticUtil.isVerifiable(cluster, nsa, nsb, windowWidth)) {
                    // Add the cluster to the good clusters list.
                    goodClusters.add(cluster);
                    if (verbose) {
                        System.out.println("[ passed ]");
                    }

                    // Plot the cluster time.
                    simClusterVerifiedTime.fill(TriggerModule.getClusterTime(cluster));
                } else if (verbose) {
                    System.out.println("[ failed ]");
                }
            }

            // Replace the old cluster list with the new one.
            reconClusters = goodClusters;
        }

        // Generate simulated triggers.
        if (verbose) {
            System.out.println("\nSim Cluster Triggers:");
        }
        SimTriggerModule<Cluster> reconModule = constructTriggers(reconClusters, Cluster.class);
        if (verbose) {
            System.out.println("\nSSP Cluster Triggers:");
        }
        SimTriggerModule<SSPCluster> sspModule = constructTriggers(sspClusters, SSPCluster.class);
        if (verbose) {
            System.out.println("\n\n\n\n");
        }

        // Plot trigger counts and trigger times.
        triggerCount[0][0].fill(reconModule.getSingles0Triggers().size());
        for (SinglesTrigger<Cluster> trigger : reconModule.getSingles0Triggers()) {
            triggerTime[0][0].fill(getTriggerTime(trigger));
        }
        triggerCount[0][1].fill(reconModule.getSingles1Triggers().size());
        for (SinglesTrigger<Cluster> trigger : reconModule.getSingles1Triggers()) {
            triggerTime[0][1].fill(getTriggerTime(trigger));
        }
        triggerCount[0][2].fill(reconModule.getPair0Triggers().size());
        for (PairTrigger<Cluster[]> trigger : reconModule.getPair0Triggers()) {
            triggerTime[0][2].fill(getTriggerTime(trigger));
        }
        triggerCount[0][3].fill(reconModule.getPair1Triggers().size());
        for (PairTrigger<Cluster[]> trigger : reconModule.getPair1Triggers()) {
            triggerTime[0][3].fill(getTriggerTime(trigger));
        }
        triggerCount[1][0].fill(sspModule.getSingles0Triggers().size());
        for (SinglesTrigger<SSPCluster> trigger : sspModule.getSingles0Triggers()) {
            triggerTime[1][0].fill(getTriggerTime(trigger));
        }
        triggerCount[1][1].fill(sspModule.getSingles1Triggers().size());
        for (SinglesTrigger<SSPCluster> trigger : sspModule.getSingles1Triggers()) {
            triggerTime[1][1].fill(getTriggerTime(trigger));
        }
        triggerCount[1][2].fill(sspModule.getPair0Triggers().size());
        for (PairTrigger<SSPCluster[]> trigger : sspModule.getPair0Triggers()) {
            triggerTime[1][2].fill(getTriggerTime(trigger));
        }
        triggerCount[1][3].fill(sspModule.getPair1Triggers().size());
        for (PairTrigger<SSPCluster[]> trigger : sspModule.getPair1Triggers()) {
            triggerTime[1][3].fill(getTriggerTime(trigger));
        }

        // Insert the trigger results in the data stream.
        SimTriggerData triggerData = new SimTriggerData(reconModule, sspModule);
        List<SimTriggerData> dataList = new ArrayList<SimTriggerData>(1);
        dataList.add(triggerData);
        event.put(simTriggerCollectionName, dataList, SimTriggerData.class, 0);
    }

    /**
     * Constructs simulated triggers in the same manner as the hardware.
     * Method can accept either <code>Cluster</code> objects, any object
     * that is a subclass of <code>Cluster</code>, or objects of type <code>SSPCluster</code>.
     * 
     * @param clusters - A <code>List</code> collection of the cluster
     * objects from which triggers are to be derived.
     * @param clusterType - The class of the cluster objects from which
     * triggers are to be derived. This can be <code>Cluster</code>, <code>SSPCluster</code>, or a subclass thereof.
     * @return Returns a <code>SimTriggerModule</code> object containing
     * the simulated trigger results.
     * @throws IllegalArgumentException Occurs if the class of the
     * cluster objects is not of a supported type.
     * 
     */
    private <E> SimTriggerModule<E> constructTriggers(List<E> clusters, Class<E> clusterType)
            throws IllegalArgumentException {
        // Verify that the cluster type is supported.
        if (!clusterType.equals(Cluster.class) && !clusterType.equals(SSPCluster.class)) {
            throw new IllegalArgumentException("Class \"" + clusterType.getSimpleName()
                    + "\" is not a supported cluster type.");
        }

        // Store the singles and pair triggers.
        List<List<PairTrigger<E[]>>> pairTriggers = new ArrayList<List<PairTrigger<E[]>>>(2);
        pairTriggers.add(new ArrayList<PairTrigger<E[]>>());
        pairTriggers.add(new ArrayList<PairTrigger<E[]>>());
        List<List<SinglesTrigger<E>>> singlesTriggers = new ArrayList<List<SinglesTrigger<E>>>(2);
        singlesTriggers.add(new ArrayList<SinglesTrigger<E>>());
        singlesTriggers.add(new ArrayList<SinglesTrigger<E>>());

        // Run the clusters through the singles trigger to determine
        // whether or not they pass it.
        for (E cluster : clusters) {
            // Simulate each of the cluster singles triggers.
            triggerLoop: for (int triggerNum = 0; triggerNum < 2; triggerNum++) {
                // Track whether the cluster passed each singles cut.
                boolean passSeedLow = true;
                boolean passSeedHigh = true;
                boolean passClusterLow = false;
                boolean passClusterHigh = false;
                boolean passHitCount = false;

                // Perform the trigger cuts appropriately for the type
                // of cluster.
                if (cluster instanceof Cluster) {
                    // Cast the cluster to the appropriate type.
                    Cluster c = (Cluster) cluster;

                    // Perform each trigger cut.
                    passClusterLow = singlesTrigger[triggerNum].clusterTotalEnergyCutLow(c);
                    passClusterHigh = singlesTrigger[triggerNum].clusterTotalEnergyCutHigh(c);
                    passHitCount = singlesTrigger[triggerNum].clusterHitCountCut(c);

                    if (verbose) {
                        System.out.printf("Singles %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerNum,
                                TriggerModule.getClusterXIndex(c), TriggerModule.getClusterYIndex(c),
                                TriggerModule.getValueClusterTotalEnergy(c), TriggerModule.getClusterHitCount(c),
                                TriggerModule.getClusterTime(c));
                    }
                } else if (cluster instanceof SSPCluster) {
                    // Cast the cluster to the appropriate type.
                    SSPCluster c = (SSPCluster) cluster;

                    // Perform each trigger cut.
                    passClusterLow = singlesTrigger[triggerNum].clusterTotalEnergyCutLow(c);
                    passClusterHigh = singlesTrigger[triggerNum].clusterTotalEnergyCutHigh(c);
                    passHitCount = singlesTrigger[triggerNum].clusterHitCountCut(c);

                    if (verbose) {
                        System.out.printf("Singles %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerNum,
                                TriggerModule.getClusterXIndex(c), TriggerModule.getClusterYIndex(c),
                                TriggerModule.getValueClusterTotalEnergy(c), TriggerModule.getClusterHitCount(c),
                                TriggerModule.getClusterTime(c));
                    }
                }

                // Make a trigger to store the results.
                SinglesTrigger<E> trigger = new SinglesTrigger<E>(cluster, triggerNum);
                trigger.setStateSeedEnergyLow(passSeedLow);
                trigger.setStateSeedEnergyHigh(passSeedHigh);
                trigger.setStateClusterEnergyLow(passClusterLow);
                trigger.setStateClusterEnergyHigh(passClusterHigh);
                trigger.setStateHitCount(passHitCount);

                if (verbose) {
                    System.out.printf("\t         N >= %1.0f     :: [ %5b ]%n",
                            singlesTrigger[triggerNum].getCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW), passHitCount);
                    System.out.printf("\t%5.3f <= E <= %5.3f :: [ %5b ]%n",
                            singlesTrigger[triggerNum].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW),
                            singlesTrigger[triggerNum].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH),
                            (passClusterLow && passClusterHigh));
                }

                // A trigger will only be reported by the SSP if it
                // passes all of the enabled cuts for that trigger.
                // Check whether this trigger meets these conditions.
                if (singlesCutsEnabled[triggerNum][ENERGY_MIN] && !trigger.getStateClusterEnergyLow()) {
                    continue triggerLoop;
                }
                if (singlesCutsEnabled[triggerNum][ENERGY_MAX] && !trigger.getStateClusterEnergyHigh()) {
                    continue triggerLoop;
                }
                if (singlesCutsEnabled[triggerNum][HIT_COUNT] && !trigger.getStateHitCount()) {
                    continue triggerLoop;
                }

                // Store the trigger.
                singlesTriggers.get(triggerNum).add(trigger);
            }
        }

        // Store cluster pairs.
        List<E[]> pairs = TriggerModule.getTopBottomPairs(clusters, clusterType);

        // Simulate the pair triggers and record the results.
        for (E[] pair : pairs) {
            // Simulate each of the cluster pair triggers.
            pairTriggerLoop: for (int triggerIndex = 0; triggerIndex < 2; triggerIndex++) {
                // Track whether the cluster passed each singles cut.
                boolean passSeedLow = true;
                boolean passSeedHigh = true;
                boolean passClusterLow = false;
                boolean passClusterHigh = false;
                boolean passHitCount = false;
                boolean passPairEnergySumLow = false;
                boolean passPairEnergySumHigh = false;
                boolean passPairEnergyDifference = false;
                boolean passPairEnergySlope = false;
                boolean passPairCoplanarity = false;
                boolean passTimeCoincidence = false;

                // Apply the trigger cuts appropriately according to the
                // cluster type.
                if (clusterType.equals(Cluster.class)) {
                    // Cast the cluster object.
                    Cluster[] reconPair = {(Cluster) pair[0], (Cluster) pair[1]};

                    // Check that the pair passes the time coincidence cut.
                    // If it does not, it is not a valid pair and should be
                    // destroyed.
                    if (!pairsTrigger[triggerIndex].pairTimeCoincidenceCut(reconPair)) {
                        continue pairTriggerLoop;
                    }

                    if (verbose) {
                        System.out.printf("Pair %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerIndex,
                                TriggerModule.getClusterXIndex(reconPair[0]),
                                TriggerModule.getClusterYIndex(reconPair[0]),
                                TriggerModule.getValueClusterTotalEnergy(reconPair[0]),
                                TriggerModule.getClusterHitCount(reconPair[0]),
                                TriggerModule.getClusterTime(reconPair[0]));
                        System.out.printf("Pair %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerIndex,
                                TriggerModule.getClusterXIndex(reconPair[1]),
                                TriggerModule.getClusterYIndex(reconPair[1]),
                                TriggerModule.getValueClusterTotalEnergy(reconPair[1]),
                                TriggerModule.getClusterHitCount(reconPair[1]),
                                TriggerModule.getClusterTime(reconPair[1]));
                    }

                    // Perform each trigger cut.
                    passClusterLow = pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(reconPair[0])
                            && pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(reconPair[1]);
                    passClusterHigh = pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(reconPair[0])
                            && pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(reconPair[1]);
                    passHitCount = pairsTrigger[triggerIndex].clusterHitCountCut(reconPair[0])
                            && pairsTrigger[triggerIndex].clusterHitCountCut(reconPair[1]);
                    passPairEnergySumLow = pairsTrigger[triggerIndex].pairEnergySumCutLow(reconPair);
                    passPairEnergySumHigh = pairsTrigger[triggerIndex].pairEnergySumCutHigh(reconPair);
                    passPairEnergyDifference = pairsTrigger[triggerIndex].pairEnergyDifferenceCut(reconPair);
                    passPairEnergySlope = pairsTrigger[triggerIndex].pairEnergySlopeCut(reconPair);
                    passPairCoplanarity = pairsTrigger[triggerIndex].pairCoplanarityCut(reconPair);
                    passTimeCoincidence = pairsTrigger[triggerIndex].pairTimeCoincidenceCut(reconPair);
                } else if (clusterType.equals(SSPCluster.class)) {
                    // Cast the cluster object.
                    SSPCluster[] sspPair = {(SSPCluster) pair[0], (SSPCluster) pair[1]};

                    // Check that the pair passes the time coincidence cut.
                    // If it does not, it is not a valid pair and should be
                    // destroyed.
                    if (!pairsTrigger[triggerIndex].pairTimeCoincidenceCut(sspPair)) {
                        continue pairTriggerLoop;
                    }

                    if (verbose) {
                        System.out.printf("Pair %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerIndex,
                                TriggerModule.getClusterXIndex(sspPair[0]), TriggerModule.getClusterYIndex(sspPair[0]),
                                TriggerModule.getValueClusterTotalEnergy(sspPair[0]),
                                TriggerModule.getClusterHitCount(sspPair[0]), TriggerModule.getClusterTime(sspPair[0]));
                        System.out.printf("Pair %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerIndex,
                                TriggerModule.getClusterXIndex(sspPair[1]), TriggerModule.getClusterYIndex(sspPair[1]),
                                TriggerModule.getValueClusterTotalEnergy(sspPair[1]),
                                TriggerModule.getClusterHitCount(sspPair[1]), TriggerModule.getClusterTime(sspPair[1]));
                    }

                    // Perform each trigger cut.
                    passClusterLow = pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(sspPair[0])
                            && pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(sspPair[1]);
                    passClusterHigh = pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(sspPair[0])
                            && pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(sspPair[1]);
                    passHitCount = pairsTrigger[triggerIndex].clusterHitCountCut(sspPair[0])
                            && pairsTrigger[triggerIndex].clusterHitCountCut(sspPair[1]);
                    passPairEnergySumLow = pairsTrigger[triggerIndex].pairEnergySumCutLow(sspPair);
                    passPairEnergySumHigh = pairsTrigger[triggerIndex].pairEnergySumCutHigh(sspPair);
                    passPairEnergyDifference = pairsTrigger[triggerIndex].pairEnergyDifferenceCut(sspPair);
                    passPairEnergySlope = pairsTrigger[triggerIndex].pairEnergySlopeCut(sspPair);
                    passPairCoplanarity = pairsTrigger[triggerIndex].pairCoplanarityCut(sspPair);
                    passTimeCoincidence = pairsTrigger[triggerIndex].pairTimeCoincidenceCut(sspPair);
                }

                // Create a trigger from the results.
                PairTrigger<E[]> trigger = new PairTrigger<E[]>(pair, triggerIndex);
                trigger.setStateSeedEnergyLow(passSeedLow);
                trigger.setStateSeedEnergyHigh(passSeedHigh);
                trigger.setStateClusterEnergyLow(passClusterLow);
                trigger.setStateClusterEnergyHigh(passClusterHigh);
                trigger.setStateHitCount(passHitCount);
                trigger.setStateEnergySumLow(passPairEnergySumLow);
                trigger.setStateEnergySumHigh(passPairEnergySumHigh);
                trigger.setStateEnergyDifference(passPairEnergyDifference);
                trigger.setStateEnergySlope(passPairEnergySlope);
                trigger.setStateCoplanarity(passPairCoplanarity);
                trigger.setStateTimeCoincidence(passTimeCoincidence);

                if (verbose) {
                    System.out.printf("\t%-5.0f >= N          :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW), passHitCount);
                    System.out.printf("\t%5.3f <= E <= %5.3f :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW),
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH),
                            (passClusterLow && passClusterHigh));
                    System.out.printf("\t%5.3f <= S <= %5.3f :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW),
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH),
                            (passPairEnergySumLow && passPairEnergySumHigh));
                    System.out.printf("\t         D <= %5.3f :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH),
                            passPairEnergyDifference);
                    System.out.printf("\t%5.3f <= L          :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW),
                            passPairEnergySlope);
                    System.out.printf("\t         C <= %-5.0f :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule.PAIR_COPLANARITY_HIGH),
                            passPairCoplanarity);
                    System.out.printf("\t         t <= %-5.0f :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule.PAIR_TIME_COINCIDENCE),
                            passTimeCoincidence);
                }

                // A trigger will only be reported by the SSP if it
                // passes all of the enabled cuts for that trigger.
                // Check whether this trigger meets these conditions.
                if (pairCutsEnabled[triggerIndex][ENERGY_MIN] && !trigger.getStateClusterEnergyLow()) {
                    continue pairTriggerLoop;
                }
                if (pairCutsEnabled[triggerIndex][ENERGY_MAX] && !trigger.getStateClusterEnergyHigh()) {
                    continue pairTriggerLoop;
                }
                if (pairCutsEnabled[triggerIndex][HIT_COUNT] && !trigger.getStateHitCount()) {
                    continue pairTriggerLoop;
                }
                if (pairCutsEnabled[triggerIndex][3 + ENERGY_SUM] && !trigger.getStateEnergySum()) {
                    continue pairTriggerLoop;
                }
                if (pairCutsEnabled[triggerIndex][3 + ENERGY_DIFF] && !trigger.getStateEnergyDifference()) {
                    continue pairTriggerLoop;
                }
                if (pairCutsEnabled[triggerIndex][3 + ENERGY_SLOPE] && !trigger.getStateEnergySlope()) {
                    continue pairTriggerLoop;
                }
                if (pairCutsEnabled[triggerIndex][3 + COPLANARITY] && !trigger.getStateCoplanarity()) {
                    continue pairTriggerLoop;
                }

                // Add the trigger to the list.
                pairTriggers.get(triggerIndex).add(trigger);
            }
        }

        // Create a new simulated trigger module to contain the results.
        return new SimTriggerModule<E>(singlesTriggers.get(0), singlesTriggers.get(1), pairTriggers.get(0),
                pairTriggers.get(1));
    }

    /**
     * Sets the name of the LCIO collection containing the TI and SSP
     * banks.
     * 
     * @param bankCollectionName - The bank collection name.
     */
    public void setBankCollectionName(String bankCollectionName) {
        this.bankCollectionName = bankCollectionName;
    }

    /**
     * Sets the name of the LCIO collection containing the simulated
     * reconstructed clusters.
     * 
     * @param clusterCollectionName - The cluster collection name.
     */
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    /**
     * Sets whether or not triggers should be formed using all clusters,
     * or only those that where the integration window for the cluster
     * is completely within the bounds of the event window.
     * 
     * @param state - <code>true</code> means that only clusters where
     * the entire cluster integration window is within the event time
     * window will be used, while <code>false</code> means that all
     * clusters will be used.
     */
    public void setFilterUnverifiableClusters(boolean state) {
        this.filterUnverifiable = state;
    }

    /**
     * Sets the name of the LCIO collection containing simulated triggers.
     * 
     * @param triggerCollection - The trigger collection name.
     */
    public void setTriggerCollectionName(String triggerCollection) {
        this.simTriggerCollectionName = triggerCollection;
    }

    /**
     * Gets the trigger time of an arbitrary trigger, so long as its
     * source is either a <code>Cluster</code> or <code>SSPCluster</code> object. Method also supports pairs of these
     * objects as a size
     * two array.
     * 
     * @param trigger - The trigger.
     * @return Returns the trigger time of the trigger as a <code>double</code>.
     */
    private static final double getTriggerTime(Trigger<?> trigger) {
        // Get the trigger source and calculate the trigger time based
        // on its object type.
        Object triggerSource = trigger.getTriggerSource();

        // If the trigger is simulated sim singles trigger...
        if (triggerSource instanceof Cluster) {
            return TriggerModule.getClusterTime((Cluster) triggerSource);
        }

        // If the trigger is a simulated SSP singles trigger...
        if (triggerSource instanceof SSPCluster) {
            return TriggerModule.getClusterTime((SSPCluster) triggerSource);
        }

        // If the trigger is a simulated sim pair trigger...
        if (triggerSource instanceof Cluster[]) {
            // Get the bottom cluster.
            Cluster bottomCluster = null;
            Cluster[] source = (Cluster[]) triggerSource;
            if (TriggerModule.getClusterYIndex(source[0]) < 0) {
                bottomCluster = source[0];
            } else {
                bottomCluster = source[1];
            }

            // Return the time of the bottom cluster.
            return TriggerModule.getClusterTime(bottomCluster);
        }

        // If the trigger is a simulated SSP pair trigger...
        if (triggerSource instanceof SSPCluster[]) {
            // Get the bottom cluster.
            SSPCluster bottomCluster = null;
            SSPCluster[] source = (SSPCluster[]) triggerSource;
            if (TriggerModule.getClusterYIndex(source[0]) < 0) {
                bottomCluster = source[0];
            } else {
                bottomCluster = source[1];
            }

            // Return the time of the bottom cluster.
            return TriggerModule.getClusterTime(bottomCluster);
        }

        // Otherwise, return negative MIN_VALUE to indicate an invalid
        // trigger type.
        return Double.MIN_VALUE;
    }
}