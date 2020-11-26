package org.hps.analysis.trigger;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.analysis.trigger.util.SinglesTrigger2019;
import org.hps.analysis.trigger.util.PairTrigger2019;
import org.hps.analysis.trigger.util.Trigger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;

import org.hps.conditions.hodoscope.HodoscopeConditions;

import org.hps.record.daqconfig2019.ConfigurationManager2019;
import org.hps.record.daqconfig2019.DAQConfig2019;
import org.hps.record.daqconfig2019.SinglesTriggerConfig2019;
import org.hps.record.daqconfig2019.PairTriggerConfig2019;
import org.hps.record.triggerbank.VTPCluster;
import org.hps.record.triggerbank.VTPData;
import org.hps.record.triggerbank.TriggerModule2019;
import org.hps.readout.util.HodoscopePattern;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.event.CalorimeterHit;

import hep.aida.IHistogram1D;

/**
 * Class <code>DataTriggerSim2019Driver</code> takes in clusters of both class
 * <code>Cluster</code> and <code>VTPCluster</code> and hodoscope hits of class <code>CalorimeterHit</code> and performs a simulation of
 * the hardware trigger on them. The results of this simulation are then stored
 * in a <code>SimTriggerData2019</code> object which is placed on the data stream to
 * allow for other drivers to access the trigger results.<br/>
 * <br/>
 * <code>DataTriggerSim2019Driver</code> is intended exclusively as a hardware
 * simulation, and as such, requires that the DAQ configuration is read before
 * it can function. Thusly, <code>DAQConfig2019Driver</code> must exist upstream of
 * this driver for it to initialize. Additionally, to ensure consistency with
 * data, it is advised that online FADC driver be employed and the VTP
 * clustering driver be set to draw from the DAQ configuration as well.<br/>
 * <br/>
 * <code>DataTriggerSim2019Driver</code> requires two input collections. The first
 * is the bank collection, which contains the VTP banks. It uses the VTP
 * bank to obtain hardware clusters. It also requires the reconstructed cluster
 * bank to obtain the clusters that are simulated from FADC hits. The driver
 * outputs one collection, the simulated trigger collection, which contains
 * simulated triggers. This collection consists of one
 * <code>SimTriggerData2019</code> object that can be accessed to obtain all
 * simulated trigger types. This output object is not persisted into LCIO after
 * runtime.
 * 
 * Class <code>DataTriggerSim2019Driver</code> is developed based on Class
 * <code>DataTriggerSimDriver</code>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 * @see org.hps.record.daqconfig.DAQConfig2019Driver
 * @see org.hps.recon.ecal.EcalOnlineRawConverter2019Driver
 * @see org.hps.recon.ecal.cluster.VTPOnlineCluster2019Driver
 * @see SimTriggerData2019
 */
public class DataTriggerSim2019Driver extends Driver {
    
    private int runNumber = -1;
    
    private HodoscopeConditions hodoConditions = null;
    
    
    /** Stores all channel for the hodoscope. */
    private Map<Long, HodoscopeChannel> channelMap = new HashMap<Long, HodoscopeChannel>();    

    // Store the LCIO collection names for the needed objects.
    private boolean verbose = false;
    private boolean filterUnverifiable = false;
    private String bankCollectionName = "VTPBank";
    private String clusterCollectionName = "EcalClusters";
    private String hodoHitCollectionName = "HodoCalHits";
    private String simTriggerCollectionName = "SimTriggers";

    // Store the VTP bank.
    private VTPData vtpBank = null;

    // Store cluster verifiability parameters.
    private int nsaEcal = 0;
    private int nsbEcal = 0;
    private int windowWidthEcal = 0;
    private int offsetEcal = 0;

    // Store hodoscope hit verifiability parameters.
    // private int nsaHodo = 0;
    // private int nsbHodo = 0;
    // private int windowWidthHodo = 0;
    private int offsetHodo = 0;

    // Store VTP parameters.
    private int hodoFADCHitThr = 0;
    private int hodoThr = 0;
    private int hodoDT = 0;

    // layer list
    private List<Integer> layerList = new ArrayList<Integer>();

    // For a cluster, hodoscope hits for timeLower <= t_hodo - t_cluster <=
    // timeUpper are selected to build hodoscope hit patterns
    private int timeUpper = offsetHodo - offsetEcal;
    private int timeLower = timeUpper - hodoDT;

    // Define trigger simulation modules.
    private boolean[] pairTriggerEnabled = new boolean[4];
    private boolean[] singlesTriggerEnabled = new boolean[4];
    private boolean[][] pairCutsEnabled = new boolean[4][8];
    private boolean[][] singlesCutsEnabled = new boolean[4][9];
    private TriggerModule2019[] singlesTrigger = new TriggerModule2019[4];
    private TriggerModule2019[] pairsTrigger = new TriggerModule2019[4];

    // Reference variables.
    private static final int ENERGY_MIN = 0;
    private static final int ENERGY_MAX = 1;
    private static final int HIT_COUNT = 2;

    private static final int X_MIN = 0;
    private static final int PDE = 1;
    private static final int L1_MATCHING = 2;
    private static final int L2_MATCHING = 3;
    private static final int L1L2_GEO_MATCHING = 4;
    private static final int HODOECAL_GEO_MATCHING = 5;

    private static final int TIME_COINCIDENCE = 0;
    private static final int ENERGY_SUM = 1;
    private static final int ENERGY_DIFF = 2;
    private static final int ENERGY_SLOPE = 3;
    private static final int COPLANARITY = 4;

    // Plots
    private AIDA aida = AIDA.defaultInstance();
    private IHistogram1D[][] triggerTime = {
            { aida.histogram1D("Trigger Sim/Sim Cluster/Singles 0 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Singles 1 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Singles 2 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Singles 3 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 0 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 1 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 2 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 3 Trigger Time", 51, -2, 202)},
            { aida.histogram1D("Trigger Sim/VTP Cluster/Singles 0 Trigger Time", 51, -2, 202),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Singles 1 Trigger Time", 101, -2, 402),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Singles 2 Trigger Time", 101, -2, 402),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Singles 3 Trigger Time", 101, -2, 402),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Pair 0 Trigger Time", 101, -2, 402),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Pair 1 Trigger Time", 101, -2, 402),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Pair 2 Trigger Time", 101, -2, 402),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Pair 3 Trigger Time", 101, -2, 402)} };
    private IHistogram1D[][] triggerCount = {
            { aida.histogram1D("Trigger Sim/Sim Cluster/Singles 0 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Singles 1 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Singles 2 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Singles 3 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 0 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 1 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 2 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/Sim Cluster/Pair 3 Trigger Count", 10, -0.5, 9.5)},
            { aida.histogram1D("Trigger Sim/VTP Cluster/Singles 0 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Singles 1 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Singles 2 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Singles 3 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Pair 0 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Pair 1 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Pair 2 Trigger Count", 10, -0.5, 9.5),
                    aida.histogram1D("Trigger Sim/VTP Cluster/Pair 3 Trigger Count", 10, -0.5, 9.5) } };
    private IHistogram1D vtpClusterTime = aida.histogram1D("Trigger Sim/VTP Cluster/Cluster Time", 101, -2, 402);
    private IHistogram1D simClusterAllTime = aida.histogram1D("Trigger Sim/Sim Cluster/Cluster Time", 51, -2, 202);
    private IHistogram1D simClusterVerifiedTime = aida.histogram1D("Trigger Sim/Sim Cluster/Verified Cluster Time", 51,
            -2, 202);

    /**
     * Connects the driver to the the <code>ConfigurationManager2019</code> in order to
     * obtain the correct trigger information. Trigger settings are stored in the
     * <code>TriggerModule2019</code> objects.
     */
    @Override
    public void startOfData() {
        // Initiate yLayerPointList
        layerList.add(SinglesTrigger2019.LAYER1);
        layerList.add(SinglesTrigger2019.LAYER2);

        // Listen for the configuration manager to provide the real
        // trigger settings.
        ConfigurationManager2019.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the DAQ configuration.
                DAQConfig2019 daq = ConfigurationManager2019.getInstance();

                // Get cluster parameters.
                nsaEcal = daq.getEcalFADCConfig().getNSA();
                nsbEcal = daq.getEcalFADCConfig().getNSB();
                windowWidthEcal = daq.getEcalFADCConfig().getWindowWidth();
                offsetEcal = daq.getEcalFADCConfig().getWindowOffset();

                // Get hodoscope hit parameters.
                // nsaHodo = daq.getHodoFADCConfig().getNSA();
                // nsbHodo = daq.getHodoFADCConfig().getNSB();
                // windowWidthHodo = daq.getHodoFADCConfig().getWindowWidth();
                offsetHodo = daq.getHodoFADCConfig().getWindowOffset();

                // Get VTP parameters.
                hodoFADCHitThr = daq.getVTPConfig().getHodoFADCHitThr();
                hodoThr = daq.getVTPConfig().getHodoThr();
                hodoDT = daq.getVTPConfig().getHodoDT() ; // Hodoscope hit coincidence between L1,L2, and also ECAL
                                                                           

                // For a cluster, hodoscope hits for timeLower <= t_hodo - t_cluster <=
                // timeUpper are selected to build hodoscope hit patterns
                timeUpper = offsetHodo - offsetEcal;
                timeLower = timeUpper - hodoDT;

                if(verbose) System.out.println("Limits for time differece between hodoscope hits and Ecal cluster: [" + Integer.toString(timeLower) + ", " + Integer.toString(timeUpper) + "] ns");
                
                // Get the trigger configurations from the DAQ.
                SinglesTriggerConfig2019[] singles = { daq.getVTPConfig().getSingles0Config(),
                        daq.getVTPConfig().getSingles1Config(), daq.getVTPConfig().getSingles2Config(),
                        daq.getVTPConfig().getSingles3Config() };
                PairTriggerConfig2019[] pairs = { daq.getVTPConfig().getPair0Config(),
                        daq.getVTPConfig().getPair1Config(), daq.getVTPConfig().getPair2Config(),
                        daq.getVTPConfig().getPair3Config() };                            
                
                // Update the enabled/disabled statuses.
                for (int i = 0; i < 4; i++) {                                        
                    singlesTrigger[i] = new TriggerModule2019();
                    pairsTrigger[i] = new TriggerModule2019();
                    // Load the DAQ settings from the configuration manager.
                    singlesTrigger[i].loadDAQConfiguration(singles[i]);
                    pairsTrigger[i].loadDAQConfiguration(pairs[i]);

                    // Set the trigger enabled status.
                    singlesTriggerEnabled[i] = singles[i].isEnabled();
                    pairTriggerEnabled[i] = pairs[i].isEnabled();

                    // Set the singles cut statuses.
                    singlesCutsEnabled[i][ENERGY_MIN] = singles[i].getEnergyMinCutConfig().isEnabled();
                    singlesCutsEnabled[i][ENERGY_MAX] = singles[i].getEnergyMaxCutConfig().isEnabled();
                    singlesCutsEnabled[i][HIT_COUNT] = singles[i].getHitCountCutConfig().isEnabled();

                    singlesCutsEnabled[i][3 + X_MIN] = singles[i].getXMinCutConfig().isEnabled();
                    singlesCutsEnabled[i][3 + PDE] = singles[i].getPDECutConfig().isEnabled();
                    singlesCutsEnabled[i][3 + L1_MATCHING] = singles[i].getL1MatchingConfig().isEnabled();
                    singlesCutsEnabled[i][3 + L2_MATCHING] = singles[i].getL2MatchingConfig().isEnabled();
                    singlesCutsEnabled[i][3 + L1L2_GEO_MATCHING] = singles[i].getL1L2GeoMatchingConfig().isEnabled();
                    singlesCutsEnabled[i][3 + HODOECAL_GEO_MATCHING] = singles[i].getHodoEcalGeoMatchingConfig()
                            .isEnabled();

                    // Set the pair cut statuses.
                    pairCutsEnabled[i][ENERGY_MIN] = pairs[i].getEnergyMinCutConfig().isEnabled();
                    pairCutsEnabled[i][ENERGY_MAX] = pairs[i].getEnergyMaxCutConfig().isEnabled();
                    pairCutsEnabled[i][HIT_COUNT] = pairs[i].getHitCountCutConfig().isEnabled();

                    pairCutsEnabled[i][3 + TIME_COINCIDENCE] = pairs[i].getEnergySumCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + ENERGY_SUM] = pairs[i].getEnergySumCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + ENERGY_DIFF] = pairs[i].getEnergyDifferenceCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + ENERGY_SLOPE] = pairs[i].getEnergySlopeCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + COPLANARITY] = pairs[i].getCoplanarityCutConfig().isEnabled();
                }

                // In evio, -31 for cluster xmin is written as 33 during DAQ since variable is not set as unsigned
                if((int)singlesTrigger[0].getCutValue(TriggerModule2019.CLUSTER_XMIN) == 33) singlesTrigger[0].setCutValue(TriggerModule2019.CLUSTER_XMIN, -31);
            }
        });
    }

    /**
     * Processes an LCIO event and simulates triggers in the same manner as the
     * hardware for both <code>VTPCluster</code> objects as well as
     * <code>Cluster</code> objects reconstructed from FADC hits. Triggers are then
     * output to the data stream.
     * 
     * @param event - The <code>EventHeader</code> object representing the current
     *              LCIO event.
     */
    @Override
    public void process(EventHeader event) {
        // If the DAQ configuration manager has not been initialized,
        // then no action can be performed.
        if (!ConfigurationManager2019.isInitialized()) {
            // Put an empty trigger results module into the data stream.
            SimTriggerData2019 triggerData = new SimTriggerData2019();
            List<SimTriggerData2019> dataList = new ArrayList<SimTriggerData2019>(1);
            dataList.add(triggerData);
            event.put(simTriggerCollectionName, dataList, SimTriggerData2019.class, 0);

            // Nothing further can be done, since trigger settings are
            // not yet defined.
            return;
        }

        // Get the VTP bank.
        if (event.hasCollection(GenericObject.class, bankCollectionName)) {
            // Get the bank list.
            List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
            vtpBank = new VTPData(bankList.get(0), bankList.get(1));

        }

        // Get a list of VTP clusters.
        List<VTPCluster> vtpClusters = null;
        if (vtpBank != null) {
            vtpClusters = vtpBank.getClusters();
        } else {
            vtpClusters = new ArrayList<VTPCluster>(0);
        }

        // Plot the VTP cluster time distribution.
        for (VTPCluster cluster : vtpClusters) {
            vtpClusterTime.fill(TriggerModule2019.getClusterTime(cluster));
        }

        // DEBUG :: Print the VTP clusters.
        if (verbose) {
            System.out.println("VTP Clusters:");
            for (VTPCluster cluster : vtpClusters) {
                System.out.printf("\t(%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n",
                        TriggerModule2019.getClusterXIndex(cluster), TriggerModule2019.getClusterYIndex(cluster),
                        TriggerModule2019.getValueClusterTotalEnergy(cluster),
                        TriggerModule2019.getClusterHitCount(cluster), TriggerModule2019.getClusterTime(cluster));
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
                simClusterAllTime.fill(TriggerModule2019.getClusterTime(cluster));

                // DEBUG :: Print the cluster.
                if (verbose) {
                    System.out.printf("\t(%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f;  ",
                            TriggerModule2019.getClusterXIndex(cluster), TriggerModule2019.getClusterYIndex(cluster),
                            TriggerModule2019.getValueClusterTotalEnergy(cluster),
                            TriggerModule2019.getClusterHitCount(cluster), TriggerModule2019.getClusterTime(cluster));
                }

                // Check if the cluster is at risk of pulse-clipping.
                if (TriggerDiagnosticUtil.isVerifiable(cluster, nsaEcal, nsbEcal, windowWidthEcal)) {
                    // Add the cluster to the good clusters list.
                    goodClusters.add(cluster);
                    if (verbose) {
                        System.out.println("[ passed ]");
                    }

                    // Plot the cluster time.
                    simClusterVerifiedTime.fill(TriggerModule2019.getClusterTime(cluster));
                } else if (verbose) {
                    System.out.println("[ failed ]");
                }
            }

            // Replace the old cluster list with the new one.
            reconClusters = goodClusters;
        }

        // Get reconstructed hodoscope hits.
        List<CalorimeterHit> reconHodoHits = null;
        if (event.hasCollection(CalorimeterHit.class, hodoHitCollectionName)) {
            reconHodoHits = event.get(CalorimeterHit.class, hodoHitCollectionName);
        } else {
            reconHodoHits = new ArrayList<CalorimeterHit>(0);
        }        
        
        if (verbose) {
            System.out.println("Hodoscope hits:");
            for (CalorimeterHit hit : reconHodoHits) {
                Long hodoChannelId = getHodoChannelID(hit);
                System.out.printf("\ty = %3d; layer = %3d; x = %3d; hole = %3d; E = %5.3f;  t = %3.0f\n",
                        channelMap.get(hodoChannelId).getIY(), channelMap.get(hodoChannelId).getLayer(), 
                        channelMap.get(hodoChannelId).getIX(), channelMap.get(hodoChannelId).getHole(), 
                        hit.getRawEnergy(), hit.getTime());
            } 
        }
        

        // Generate simulated triggers.
        if (verbose) {
            System.out.println("\nSim Cluster Triggers:");
        }
        SimTriggerModule2019<Cluster> reconModule = constructTriggers(reconClusters, Cluster.class, reconHodoHits);
        if (verbose) {
            System.out.println("\nVTP Cluster Triggers:");
        }
        SimTriggerModule2019<VTPCluster> vtpModule = constructTriggers(vtpClusters, VTPCluster.class, reconHodoHits);
        if (verbose) {
            System.out.println("\n\n\n\n");
        }

        // Plot trigger counts and trigger times.
        triggerCount[0][0].fill(reconModule.getSingles0Triggers().size());
        for (SinglesTrigger2019<Cluster> trigger : reconModule.getSingles0Triggers()) {
            triggerTime[0][0].fill(getTriggerTime(trigger));
        }
        triggerCount[0][1].fill(reconModule.getSingles1Triggers().size());
        for (SinglesTrigger2019<Cluster> trigger : reconModule.getSingles1Triggers()) {
            triggerTime[0][1].fill(getTriggerTime(trigger));
        }
        triggerCount[0][2].fill(reconModule.getSingles2Triggers().size());
        for (SinglesTrigger2019<Cluster> trigger : reconModule.getSingles2Triggers()) {
            triggerTime[0][2].fill(getTriggerTime(trigger));
        }
        triggerCount[0][3].fill(reconModule.getSingles3Triggers().size());
        for (SinglesTrigger2019<Cluster> trigger : reconModule.getSingles3Triggers()) {
            triggerTime[0][3].fill(getTriggerTime(trigger));
        }
        
        triggerCount[0][4].fill(reconModule.getPair0Triggers().size());
        for (PairTrigger2019<Cluster[]> trigger : reconModule.getPair0Triggers()) {
            triggerTime[0][4].fill(getTriggerTime(trigger));
        }
        triggerCount[0][5].fill(reconModule.getPair1Triggers().size());
        for (PairTrigger2019<Cluster[]> trigger : reconModule.getPair1Triggers()) {
            triggerTime[0][5].fill(getTriggerTime(trigger));
        }
        triggerCount[0][6].fill(reconModule.getPair2Triggers().size());
        for (PairTrigger2019<Cluster[]> trigger : reconModule.getPair2Triggers()) {
            triggerTime[0][6].fill(getTriggerTime(trigger));
        }
        triggerCount[0][7].fill(reconModule.getPair3Triggers().size());
        for (PairTrigger2019<Cluster[]> trigger : reconModule.getPair3Triggers()) {
            triggerTime[0][7].fill(getTriggerTime(trigger));
        }
        
        triggerCount[1][0].fill(vtpModule.getSingles0Triggers().size());
        for (SinglesTrigger2019<VTPCluster> trigger : vtpModule.getSingles0Triggers()) {
            triggerTime[1][0].fill(getTriggerTime(trigger));
        }
        triggerCount[1][1].fill(vtpModule.getSingles1Triggers().size());
        for (SinglesTrigger2019<VTPCluster> trigger : vtpModule.getSingles1Triggers()) {
            triggerTime[1][1].fill(getTriggerTime(trigger));
        }
        triggerCount[1][2].fill(vtpModule.getSingles2Triggers().size());
        for (SinglesTrigger2019<VTPCluster> trigger : vtpModule.getSingles2Triggers()) {
            triggerTime[1][2].fill(getTriggerTime(trigger));
        }
        triggerCount[1][3].fill(vtpModule.getSingles3Triggers().size());
        for (SinglesTrigger2019<VTPCluster> trigger : vtpModule.getSingles3Triggers()) {
            triggerTime[1][3].fill(getTriggerTime(trigger));
        }
        
        triggerCount[1][4].fill(vtpModule.getPair0Triggers().size());
        for (PairTrigger2019<VTPCluster[]> trigger : vtpModule.getPair0Triggers()) {
            triggerTime[1][4].fill(getTriggerTime(trigger));
        }
        triggerCount[1][5].fill(vtpModule.getPair1Triggers().size());
        for (PairTrigger2019<VTPCluster[]> trigger : vtpModule.getPair1Triggers()) {
            triggerTime[1][5].fill(getTriggerTime(trigger));
        }
        triggerCount[1][6].fill(vtpModule.getPair2Triggers().size());
        for (PairTrigger2019<VTPCluster[]> trigger : vtpModule.getPair2Triggers()) {
            triggerTime[1][6].fill(getTriggerTime(trigger));
        }
        triggerCount[1][7].fill(vtpModule.getPair3Triggers().size());
        for (PairTrigger2019<VTPCluster[]> trigger : vtpModule.getPair3Triggers()) {
            triggerTime[1][7].fill(getTriggerTime(trigger));
        }

        // Insert the trigger results in the data stream.
        SimTriggerData2019 triggerData = new SimTriggerData2019(reconModule, vtpModule);
        List<SimTriggerData2019> dataList = new ArrayList<SimTriggerData2019>(1);
        dataList.add(triggerData);
        
        event.put(simTriggerCollectionName, dataList, SimTriggerData2019.class, 0);
    }

    /**
     * Constructs simulated triggers in the same manner as the hardware. Method can
     * accept either <code>Cluster</code> objects, any object that is a subclass of
     * <code>Cluster</code>, or objects of type <code>VTPCluster</code>.
     * 
     * @param clusters    - A <code>List</code> collection of the cluster objects
     *                    from which triggers are to be derived.
     * @param clusterType - The class of the cluster objects from which triggers are
     *                    to be derived. This can be <code>Cluster</code>,
     *                    <code>VTPCluster</code>, or a subclass thereof.
     * @return Returns a <code>SimTriggerModule2019</code> object containing the
     *         simulated trigger results.
     * @throws IllegalArgumentException Occurs if the class of the cluster objects
     *                                  is not of a supported type.
     * 
     */
    private <E> SimTriggerModule2019<E> constructTriggers(List<E> clusters, Class<E> clusterType,
            List<CalorimeterHit> hodoHits) throws IllegalArgumentException {
        // Verify that the cluster type is supported.
        if (!clusterType.equals(Cluster.class) && !clusterType.equals(VTPCluster.class)) {
            throw new IllegalArgumentException(
                    "Class \"" + clusterType.getSimpleName() + "\" is not a supported cluster type.");
        }

        // Store the singles and pair triggers.
        List<List<PairTrigger2019<E[]>>> pairTriggers = new ArrayList<List<PairTrigger2019<E[]>>>(4);
        pairTriggers.add(new ArrayList<PairTrigger2019<E[]>>());
        pairTriggers.add(new ArrayList<PairTrigger2019<E[]>>());
        pairTriggers.add(new ArrayList<PairTrigger2019<E[]>>());
        pairTriggers.add(new ArrayList<PairTrigger2019<E[]>>());
        List<List<SinglesTrigger2019<E>>> singlesTriggers = new ArrayList<List<SinglesTrigger2019<E>>>(4);
        singlesTriggers.add(new ArrayList<SinglesTrigger2019<E>>());
        singlesTriggers.add(new ArrayList<SinglesTrigger2019<E>>());
        singlesTriggers.add(new ArrayList<SinglesTrigger2019<E>>());
        singlesTriggers.add(new ArrayList<SinglesTrigger2019<E>>());

        // Run the clusters through the singles trigger to determine
        // whether or not they pass it.
        for (E cluster : clusters) {
            // Simulate each of the cluster singles triggers.
            triggerLoop: for (int triggerNum = 0; triggerNum < 4; triggerNum++) {
                // Indicator to indicate the cluster is at top or bot
                // 1 for top
                // 0 for bot
                int topnbot = -1;

                // Track whether the cluster passed each singles cut.
                boolean passClusterLow = false;
                boolean passClusterHigh = false;
                boolean passHitCount = false;
                boolean passClusterXMin = false;
                boolean passClusterPDE = false;
                boolean passHodoL1Matching = false;
                boolean passHodoL2Matching = false;
                boolean passHodoL1L2Matching = false;
                boolean passHodoEcalMatching = false;

                List<CalorimeterHit> hodoHitList = new ArrayList<CalorimeterHit>();
                Map<Integer, HodoscopePattern> patternMap = new HashMap<Integer, HodoscopePattern>();

                // Perform the trigger cuts appropriately for the type
                // of cluster.
                if (cluster instanceof Cluster) {
                    // Cast the cluster to the appropriate type.
                    Cluster c = (Cluster) cluster;
                    if (TriggerModule2019.getClusterYIndex(c) > 0)
                        topnbot = 1;
                    else
                        topnbot = 0;

                    // Perform each trigger cut.
                    passClusterLow = singlesTrigger[triggerNum].clusterTotalEnergyCutLow(c);
                    passClusterHigh = singlesTrigger[triggerNum].clusterTotalEnergyCutHigh(c);
                    passHitCount = singlesTrigger[triggerNum].clusterHitCountCut(c);
                    passClusterXMin = singlesTrigger[triggerNum].clusterXMinCut(c);
                    passClusterPDE = singlesTrigger[triggerNum].clusterPDECut(c);
                    
                    if (topnbot == 1) {
                        // Save valid hodoscope hits into a list
                        for (CalorimeterHit hodoHit : hodoHits) {
                            if (channelMap.get(getHodoChannelID(hodoHit)).getIY() == 1 && isHodoHitValid(c, hodoHit))
                                hodoHitList.add(hodoHit);
                        }
                        // Build hodoscope patterns
                        patternMap = getHodoPatternMap(hodoHitList);

                        passHodoL1Matching = singlesTrigger[triggerNum]
                                .hodoLayerMatching(patternMap.get(SinglesTrigger2019.LAYER1));
                        passHodoL2Matching = singlesTrigger[triggerNum]
                                .hodoLayerMatching(patternMap.get(SinglesTrigger2019.LAYER2));
                        passHodoL1L2Matching = singlesTrigger[triggerNum].geometryHodoL1L2Matching(
                                patternMap.get(SinglesTrigger2019.LAYER1), patternMap.get(SinglesTrigger2019.LAYER2));
                        if (TriggerModule2019.getClusterXIndex(c) > 0) {
                            passHodoEcalMatching = geometryEcalHodoMatching(TriggerModule2019.getClusterXIndex(c),
                                    patternMap.get(SinglesTrigger2019.LAYER1), patternMap.get(SinglesTrigger2019.LAYER2), runNumber);
                        }
                    } else {
                        // Save valid hodoscope hits into a list
                        for (CalorimeterHit hodoHit : hodoHits) {
                            if (channelMap.get(getHodoChannelID(hodoHit)).getIY() == -1 && isHodoHitValid(c, hodoHit))
                                hodoHitList.add(hodoHit);
                        }
                        // Build hodoscope patterns
                        patternMap = getHodoPatternMap(hodoHitList);
 
                        passHodoL1Matching = singlesTrigger[triggerNum]
                                .hodoLayerMatching(patternMap.get(SinglesTrigger2019.LAYER1));
                        passHodoL2Matching = singlesTrigger[triggerNum]
                                .hodoLayerMatching(patternMap.get(SinglesTrigger2019.LAYER2));
                        passHodoL1L2Matching = singlesTrigger[triggerNum].geometryHodoL1L2Matching(
                                patternMap.get(SinglesTrigger2019.LAYER1), patternMap.get(SinglesTrigger2019.LAYER2));
                        if (TriggerModule2019.getClusterXIndex(c) > 0) {
                            passHodoEcalMatching = geometryEcalHodoMatching(TriggerModule2019.getClusterXIndex(c),
                                    patternMap.get(SinglesTrigger2019.LAYER1),
                                    patternMap.get(SinglesTrigger2019.LAYER2), runNumber);
                        }
                    }

                    if (verbose) {
                        System.out.printf("Singles %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerNum,
                                TriggerModule2019.getClusterXIndex(c), TriggerModule2019.getClusterYIndex(c),
                                TriggerModule2019.getValueClusterTotalEnergy(c),
                                TriggerModule2019.getClusterHitCount(c), TriggerModule2019.getClusterTime(c));
                        
                        System.out.println("Hodoscope hits:");
                        for (CalorimeterHit hit : hodoHitList) {
                            Long hodoChannelId = getHodoChannelID(hit);
                            System.out.printf("\ty = %3d; layer = %3d; x = %3d; hole = %3d; E = %5.3f;  t = %3.0f\n",
                                    channelMap.get(hodoChannelId).getIY(), channelMap.get(hodoChannelId).getLayer(), 
                                    channelMap.get(hodoChannelId).getIX(), channelMap.get(hodoChannelId).getHole(), 
                                    hit.getRawEnergy(), hit.getTime());
                        } 
                        
                        System.out.println("Hodoscope pattern: ");
                        System.out.printf("\tLayer %d: ", SinglesTrigger2019.LAYER1);
                        System.out.print(patternMap.get(SinglesTrigger2019.LAYER1));
                        System.out.printf("\tLayer %d: ", SinglesTrigger2019.LAYER2);
                        System.out.print(patternMap.get(SinglesTrigger2019.LAYER2));
                    }                    
                } 
                else if (cluster instanceof VTPCluster) {
                    // Cast the cluster to the appropriate type.
                    VTPCluster c = (VTPCluster) cluster;
                    if (TriggerModule2019.getClusterYIndex(c) > 0)
                        topnbot = 1;
                    else
                        topnbot = 0;

                    // Perform each trigger cut.
                    passClusterLow = singlesTrigger[triggerNum].clusterTotalEnergyCutLow(c);
                    passClusterHigh = singlesTrigger[triggerNum].clusterTotalEnergyCutHigh(c);
                    passHitCount = singlesTrigger[triggerNum].clusterHitCountCut(c);
                    passClusterXMin = singlesTrigger[triggerNum].clusterXMinCut(c);
                    passClusterPDE = singlesTrigger[triggerNum].clusterPDECut(c);
                    
                    if (topnbot == 1) {
                        // Save valid hodoscope hits into a list
                        for (CalorimeterHit hodoHit : hodoHits) {
                            if (channelMap.get(getHodoChannelID(hodoHit)).getIY() == 1 && isHodoHitValid(c, hodoHit))
                                hodoHitList.add(hodoHit);
                        }
                        // Build hodoscope patterns
                        patternMap = getHodoPatternMap(hodoHitList);

                        passHodoL1Matching = singlesTrigger[triggerNum]
                                .hodoLayerMatching(patternMap.get(SinglesTrigger2019.LAYER1));
                        passHodoL2Matching = singlesTrigger[triggerNum]
                                .hodoLayerMatching(patternMap.get(SinglesTrigger2019.LAYER2));
                        passHodoL1L2Matching = singlesTrigger[triggerNum].geometryHodoL1L2Matching(
                                patternMap.get(SinglesTrigger2019.LAYER1), patternMap.get(SinglesTrigger2019.LAYER2));
                        if (TriggerModule2019.getClusterXIndex(c) > 0) {
                            passHodoEcalMatching = geometryEcalHodoMatching(TriggerModule2019.getClusterXIndex(c),
                                    patternMap.get(SinglesTrigger2019.LAYER1),
                                    patternMap.get(SinglesTrigger2019.LAYER2), runNumber);
                        }
                    } else {
                        // Save valid hodoscope hits into a list
                        for (CalorimeterHit hodoHit : hodoHits) {
                            if (channelMap.get(getHodoChannelID(hodoHit)).getIY() == -1 && isHodoHitValid(c, hodoHit))
                                hodoHitList.add(hodoHit);
                        }
                        // Build hodoscope patterns
                        patternMap = getHodoPatternMap(hodoHitList);

                        passHodoL1Matching = singlesTrigger[triggerNum]
                                .hodoLayerMatching(patternMap.get(SinglesTrigger2019.LAYER1));
                        passHodoL2Matching = singlesTrigger[triggerNum]
                                .hodoLayerMatching(patternMap.get(SinglesTrigger2019.LAYER2));
                        passHodoL1L2Matching = singlesTrigger[triggerNum].geometryHodoL1L2Matching(
                                patternMap.get(SinglesTrigger2019.LAYER1), patternMap.get(SinglesTrigger2019.LAYER2));
                        if (TriggerModule2019.getClusterXIndex(c) > 0) {
                            passHodoEcalMatching = geometryEcalHodoMatching(TriggerModule2019.getClusterXIndex(c),
                                    patternMap.get(SinglesTrigger2019.LAYER1),
                                    patternMap.get(SinglesTrigger2019.LAYER2), runNumber);
                        }
                    }

                    if (verbose) {
                        System.out.printf("Singles %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerNum,
                                TriggerModule2019.getClusterXIndex(c), TriggerModule2019.getClusterYIndex(c),
                                TriggerModule2019.getValueClusterTotalEnergy(c),
                                TriggerModule2019.getClusterHitCount(c), TriggerModule2019.getClusterTime(c));
                        
                        System.out.println("Hodoscope hits:");
                        for (CalorimeterHit hit : hodoHitList) {
                            Long hodoChannelId = getHodoChannelID(hit);
                            System.out.printf("\ty = %3d; layer = %3d; x = %3d; hole = %3d; E = %5.3f;  t = %3.0f\n",
                                    channelMap.get(hodoChannelId).getIY(), channelMap.get(hodoChannelId).getLayer(), 
                                    channelMap.get(hodoChannelId).getIX(), channelMap.get(hodoChannelId).getHole(), 
                                    hit.getRawEnergy(), hit.getTime());
                        } 
                        
                        System.out.println("Hodoscope pattern: ");
                        System.out.printf("\tLayer %d: ", SinglesTrigger2019.LAYER1);
                        System.out.print(patternMap.get(SinglesTrigger2019.LAYER1));
                        System.out.printf("\tLayer %d: ", SinglesTrigger2019.LAYER2);
                        System.out.print(patternMap.get(SinglesTrigger2019.LAYER2));
                    }
                }

                // Make a trigger to store the results.
                SinglesTrigger2019<E> trigger = new SinglesTrigger2019<E>(cluster, hodoHitList, patternMap, triggerNum,
                        topnbot);
                trigger.setStateClusterEnergyLow(passClusterLow);
                trigger.setStateClusterEnergyHigh(passClusterHigh);
                trigger.setStateHitCount(passHitCount);
                trigger.setStateClusterXMin(passClusterXMin);
                trigger.setStateClusterPDE(passClusterPDE);
                trigger.setStateHodoL1Matching(passHodoL1Matching);
                trigger.setStateHodoL2Matching(passHodoL2Matching);
                trigger.setStateHodoL1L2Matching(passHodoL1L2Matching);
                trigger.setStateHodoEcalMatching(passHodoEcalMatching);

                if (verbose) {
                    System.out.printf("\t         N >= %1.0f     :: [ %5b ]%n",
                            singlesTrigger[triggerNum].getCutValue(TriggerModule2019.CLUSTER_HIT_COUNT_LOW),
                            passHitCount);
                    System.out.printf("\t%5.3f <= E          :: [ %5b ]%n",
                            singlesTrigger[triggerNum].getCutValue(TriggerModule2019.CLUSTER_TOTAL_ENERGY_LOW),
                            passClusterLow);
                    System.out.printf("\t%5.3f >= E          :: [ %5b ]%n",
                            singlesTrigger[triggerNum].getCutValue(TriggerModule2019.CLUSTER_TOTAL_ENERGY_HIGH),
                            passClusterHigh);
                    System.out.printf("\t%-5.0f <= X          :: [ %5b ]%n",
                            singlesTrigger[triggerNum].getCutValue(TriggerModule2019.CLUSTER_XMIN), passClusterXMin);
                    System.out.printf("\t  PDE <= E          :: [ %5b ]%n", passClusterPDE);
                    System.out.printf("\t HodoL1Matching     :: [ %5b ]%n", passHodoL1Matching);
                    System.out.printf("\t HodoL2Matching     :: [ %5b ]%n", passHodoL2Matching);
                    System.out.printf("\t HodoL1L2Matching   :: [ %5b ]%n", passHodoL1L2Matching);
                    System.out.printf("\t HodoEcalMatching   :: [ %5b ]%n", passHodoEcalMatching);
                }                

                // A trigger will only be reported by the VTP if it
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
                if (singlesCutsEnabled[triggerNum][3 + X_MIN] && !trigger.getStateClusterXMin()) {
                    continue triggerLoop;
                }
                if (singlesCutsEnabled[triggerNum][3 + PDE] && !trigger.getStateClusterPDE()) {
                    continue triggerLoop;
                }
                if (singlesCutsEnabled[triggerNum][3 + L1_MATCHING] && !trigger.getStateHodoL1Matching()) {
                    continue triggerLoop;
                }
                if (singlesCutsEnabled[triggerNum][3 + L2_MATCHING] && !trigger.getStateHodoL2Matching()) {
                    continue triggerLoop;
                }
                if (singlesCutsEnabled[triggerNum][3 + L1L2_GEO_MATCHING] && !trigger.getStateHodoL1L2Matching()) {
                    continue triggerLoop;
                }
                if (singlesCutsEnabled[triggerNum][3 + HODOECAL_GEO_MATCHING] && !trigger.getStateHodoEcalMatching()) {
                    continue triggerLoop;
                }

                // Store the trigger.
                singlesTriggers.get(triggerNum).add(trigger);
            }
        }

        // Store cluster pairs.
        List<E[]> pairs = TriggerModule2019.getTopBottomPairs(clusters, clusterType);

        // Simulate the pair triggers and record the results.
        for (E[] pair : pairs) {
            // Simulate each of the cluster pair triggers.
            pairTriggerLoop: for (int triggerIndex = 0; triggerIndex < 4; triggerIndex++) {
                // Track whether the cluster passed each singles cut.
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
                    Cluster[] reconPair = { (Cluster) pair[0], (Cluster) pair[1] };

                    // Check that the pair passes the time coincidence cut.
                    // If it does not, it is not a valid pair and should be
                    // destroyed.
                    if (!pairsTrigger[triggerIndex].pairTimeCoincidenceCut(reconPair)) {
                        continue pairTriggerLoop;
                    }

                    if (verbose) {
                        System.out.printf("Pair %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerIndex,
                                TriggerModule2019.getClusterXIndex(reconPair[0]),
                                TriggerModule2019.getClusterYIndex(reconPair[0]),
                                TriggerModule2019.getValueClusterTotalEnergy(reconPair[0]),
                                TriggerModule2019.getClusterHitCount(reconPair[0]),
                                TriggerModule2019.getClusterTime(reconPair[0]));
                        System.out.printf("Pair %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerIndex,
                                TriggerModule2019.getClusterXIndex(reconPair[1]),
                                TriggerModule2019.getClusterYIndex(reconPair[1]),
                                TriggerModule2019.getValueClusterTotalEnergy(reconPair[1]),
                                TriggerModule2019.getClusterHitCount(reconPair[1]),
                                TriggerModule2019.getClusterTime(reconPair[1]));
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
                } else if (clusterType.equals(VTPCluster.class)) {
                    // Cast the cluster object.
                    VTPCluster[] vtpPair = { (VTPCluster) pair[0], (VTPCluster) pair[1] };

                    // Check that the pair passes the time coincidence cut.
                    // If it does not, it is not a valid pair and should be
                    // destroyed.
                    if (!pairsTrigger[triggerIndex].pairTimeCoincidenceCut(vtpPair)) {
                        continue pairTriggerLoop;
                    }

                    if (verbose) {
                        System.out.printf("Pair %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerIndex,
                                TriggerModule2019.getClusterXIndex(vtpPair[0]),
                                TriggerModule2019.getClusterYIndex(vtpPair[0]),
                                TriggerModule2019.getValueClusterTotalEnergy(vtpPair[0]),
                                TriggerModule2019.getClusterHitCount(vtpPair[0]),
                                TriggerModule2019.getClusterTime(vtpPair[0]));
                        System.out.printf("Pair %d :: (%3d, %3d);  E = %5.3f;  N = %1.0f;  t = %3.0f%n", triggerIndex,
                                TriggerModule2019.getClusterXIndex(vtpPair[1]),
                                TriggerModule2019.getClusterYIndex(vtpPair[1]),
                                TriggerModule2019.getValueClusterTotalEnergy(vtpPair[1]),
                                TriggerModule2019.getClusterHitCount(vtpPair[1]),
                                TriggerModule2019.getClusterTime(vtpPair[1]));
                    }

                    // Perform each trigger cut.
                    passClusterLow = pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(vtpPair[0])
                            && pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(vtpPair[1]);
                    passClusterHigh = pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(vtpPair[0])
                            && pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(vtpPair[1]);
                    passHitCount = pairsTrigger[triggerIndex].clusterHitCountCut(vtpPair[0])
                            && pairsTrigger[triggerIndex].clusterHitCountCut(vtpPair[1]);
                    passPairEnergySumLow = pairsTrigger[triggerIndex].pairEnergySumCutLow(vtpPair);
                    passPairEnergySumHigh = pairsTrigger[triggerIndex].pairEnergySumCutHigh(vtpPair);
                    passPairEnergyDifference = pairsTrigger[triggerIndex].pairEnergyDifferenceCut(vtpPair);
                    passPairEnergySlope = pairsTrigger[triggerIndex].pairEnergySlopeCut(vtpPair);
                    passPairCoplanarity = pairsTrigger[triggerIndex].pairCoplanarityCut(vtpPair);
                    passTimeCoincidence = pairsTrigger[triggerIndex].pairTimeCoincidenceCut(vtpPair);
                }

                // Create a trigger from the results.
                PairTrigger2019<E[]> trigger = new PairTrigger2019<E[]>(pair, triggerIndex);
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
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule2019.CLUSTER_HIT_COUNT_LOW),
                            passHitCount);
                    System.out.printf("\t%5.3f <= E          :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule2019.CLUSTER_TOTAL_ENERGY_LOW),
                            passClusterLow);
                    System.out.printf("\t%5.3f >= E          :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule2019.CLUSTER_TOTAL_ENERGY_HIGH),
                            passClusterHigh);
                    System.out.printf("\t         t <= %-5.0f :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule2019.PAIR_TIME_COINCIDENCE),
                            passTimeCoincidence);
                    System.out.printf("\t%5.3f <= S <= %5.3f :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule2019.PAIR_ENERGY_SUM_LOW),
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule2019.PAIR_ENERGY_SUM_HIGH),
                            (passPairEnergySumLow && passPairEnergySumHigh));
                    System.out.printf("\t         D <= %5.3f :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule2019.PAIR_ENERGY_DIFFERENCE_HIGH),
                            passPairEnergyDifference);
                    System.out.printf("\t%5.3f <= L          :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule2019.PAIR_ENERGY_SLOPE_LOW),
                            passPairEnergySlope);
                    System.out.printf("\t         C <= %-5.0f :: [ %5b ]%n",
                            pairsTrigger[triggerIndex].getCutValue(TriggerModule2019.PAIR_COPLANARITY_HIGH),
                            passPairCoplanarity);
                }

                // A trigger will only be reported by the VTP if it
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
                if (pairCutsEnabled[triggerIndex][3 + TIME_COINCIDENCE] && !trigger.getStateTimeCoincidence()) {
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
        return new SimTriggerModule2019<E>(singlesTriggers.get(0), singlesTriggers.get(1), singlesTriggers.get(2),
                singlesTriggers.get(3), pairTriggers.get(0), pairTriggers.get(1), pairTriggers.get(2),
                pairTriggers.get(3));
    }

    /**
     * For a cluster, hodoscope hits, which time satisfies timeLower <= t_hodo -
     * t_cluster <= timeUpper and which energy is larger than hodoFADCHitThr are
     * selected to build hodoscope hit patterns.
     * 
     * @param cluster
     * @param hodoHit
     * @return
     */
    private boolean isHodoHitValid(Cluster cluster, CalorimeterHit hodoHit) {
        double timeCluster = TriggerModule2019.getClusterTime(cluster);
        double hodoHitTime = hodoHit.getTime();
        double timeDiff = hodoHitTime - timeCluster;
        double hodoEnergy = hodoHit.getRawEnergy();

        return hodoEnergy > hodoFADCHitThr && timeDiff >= timeLower && timeDiff <= timeUpper;
    }

    /**
     * For a cluster, hodoscope hits, which time satisfies timeLower <= t_hodo -
     * t_cluster <= timeUpper and which energy is larger than hodoFADCHitThr are
     * selected to build hodoscope hit patterns.
     * 
     * @param cluster
     * @param hodoHit
     * @return
     */
    private boolean isHodoHitValid(VTPCluster cluster, CalorimeterHit hodoHit) {
        double timeCluster = TriggerModule2019.getClusterTime(cluster);
        double hodoHitTime = hodoHit.getTime();
        double timeDiff = hodoHitTime - timeCluster;
        double hodoEnergy = hodoHit.getRawEnergy();

        return hodoEnergy > hodoFADCHitThr && timeDiff >= timeLower && timeDiff <= timeUpper;
    }
    
    /**
     * Get hodoscope pattern map for two layers at top or bot
     * @param hodoscope hit list
     * @return hodoscope pattern map
     */
    private Map<Integer, HodoscopePattern> getHodoPatternMap(List<CalorimeterHit> hodoHits) {
        Map<Integer, List<CalorimeterHit>> hodoHitListMap = new HashMap<Integer, List<CalorimeterHit>>();
        for (int layer : layerList) {
            hodoHitListMap.put(layer, new ArrayList<CalorimeterHit>());
        }

        for (CalorimeterHit hit : hodoHits) {
            hodoHitListMap.get(channelMap.get(getHodoChannelID(hit)).getLayer()).add(hit);
        }

        Map<Integer, HodoscopePattern> patternMap = new HashMap<Integer, HodoscopePattern>();

        for (int layer : hodoHitListMap.keySet()) {
            patternMap.put(layer, getHodoPattern(hodoHitListMap.get(layer)));
        }

        return patternMap;
    }

    /**
     * Get a hodoscope hit pattern at a layer
     * @param hodoscope hit list
     * @return a hodoscope pattern 
     */
    private HodoscopePattern getHodoPattern(List<CalorimeterHit> hodoHits) {
        // hodo x
        int HODO_X_0 = 0;
        int HODO_X_1 = 1;
        int HODO_X_2 = 2;
        int HODO_X_3 = 3;
        int HODO_X_4 = 4;

        // hodo hole
        int HODO_HOLE_0 = 0;
        int HODO_HOLE_M1 = -1;
        int HODO_HOLE_1 = 1;

        List<Point> pointList = new ArrayList<Point>();
        pointList.add(new Point(HODO_X_0, HODO_HOLE_0));
        pointList.add(new Point(HODO_X_1, HODO_HOLE_M1));
        pointList.add(new Point(HODO_X_1, HODO_HOLE_1));
        pointList.add(new Point(HODO_X_2, HODO_HOLE_M1));
        pointList.add(new Point(HODO_X_2, HODO_HOLE_1));
        pointList.add(new Point(HODO_X_3, HODO_HOLE_M1));
        pointList.add(new Point(HODO_X_3, HODO_HOLE_1));
        pointList.add(new Point(HODO_X_4, HODO_HOLE_0));

        Map<Point, List<CalorimeterHit>> hodoHitListMap = new HashMap<Point, List<CalorimeterHit>>();
        for (Point p : pointList) {
            hodoHitListMap.put(p, new ArrayList<CalorimeterHit>());
        }

        for (CalorimeterHit hit : hodoHits) {
            Point p = new Point(channelMap.get(getHodoChannelID(hit)).getIX(), channelMap.get(getHodoChannelID(hit)).getHole());
            hodoHitListMap.get(p).add(hit);
        }

        Map<Point, Double> maxEnergyMap = new HashMap<Point, Double>();
        ;
        for (Point p : hodoHitListMap.keySet()) {
            double maxEnergy = maxEnergyForHodoHitList(hodoHitListMap.get(p));
            if (p.getX() == HODO_X_0 || p.getX() == HODO_X_4)
                maxEnergyMap.put(p, maxEnergy);
            else
                maxEnergyMap.put(p, maxEnergy);
        }

        HodoscopePattern pattern = new HodoscopePattern();

        if (maxEnergyMap.get(new Point(HODO_X_0, HODO_HOLE_0)) > hodoThr)
            pattern.setHitStatus(HodoscopePattern.HODO_LX_1, true);

        if (maxEnergyMap.get(new Point(HODO_X_1, HODO_HOLE_M1))
                + maxEnergyMap.get(new Point(HODO_X_1, HODO_HOLE_1)) > hodoThr)
            pattern.setHitStatus(HodoscopePattern.HODO_LX_2, true);

        if (maxEnergyMap.get(new Point(HODO_X_2, HODO_HOLE_M1))
                + maxEnergyMap.get(new Point(HODO_X_2, HODO_HOLE_1)) > hodoThr)
            pattern.setHitStatus(HodoscopePattern.HODO_LX_3, true);

        if (maxEnergyMap.get(new Point(HODO_X_3, HODO_HOLE_M1))
                + maxEnergyMap.get(new Point(HODO_X_3, HODO_HOLE_1)) > hodoThr)
            pattern.setHitStatus(HodoscopePattern.HODO_LX_4, true);

        if (maxEnergyMap.get(new Point(HODO_X_4, HODO_HOLE_0)) > hodoThr)
            pattern.setHitStatus(HodoscopePattern.HODO_LX_5, true);

        if (maxEnergyMap.get(new Point(HODO_X_0, HODO_HOLE_0)) + maxEnergyMap.get(new Point(HODO_X_1, HODO_HOLE_M1))
                + maxEnergyMap.get(new Point(HODO_X_1, HODO_HOLE_1)) > hodoThr
                && maxEnergyMap.get(new Point(HODO_X_0, HODO_HOLE_0)) != 0
                && (maxEnergyMap.get(new Point(HODO_X_1, HODO_HOLE_M1)) != 0
                        || maxEnergyMap.get(new Point(HODO_X_1, HODO_HOLE_1)) != 0))
            pattern.setHitStatus(HodoscopePattern.HODO_LX_CL_12, true);

        if (maxEnergyMap.get(new Point(HODO_X_1, HODO_HOLE_M1)) + maxEnergyMap.get(new Point(HODO_X_1, HODO_HOLE_1))
                + maxEnergyMap.get(new Point(HODO_X_2, HODO_HOLE_M1))
                + maxEnergyMap.get(new Point(HODO_X_2, HODO_HOLE_1)) > hodoThr
                && (maxEnergyMap.get(new Point(HODO_X_1, HODO_HOLE_M1)) != 0
                        || maxEnergyMap.get(new Point(HODO_X_1, HODO_HOLE_1)) != 0)
                && (maxEnergyMap.get(new Point(HODO_X_2, HODO_HOLE_M1)) != 0
                        || maxEnergyMap.get(new Point(HODO_X_2, HODO_HOLE_1)) != 0))
            pattern.setHitStatus(HodoscopePattern.HODO_LX_CL_23, true);

        if (maxEnergyMap.get(new Point(HODO_X_2, HODO_HOLE_M1)) + maxEnergyMap.get(new Point(HODO_X_2, HODO_HOLE_1))
                + maxEnergyMap.get(new Point(HODO_X_3, HODO_HOLE_M1))
                + maxEnergyMap.get(new Point(HODO_X_3, HODO_HOLE_1)) > hodoThr
                && (maxEnergyMap.get(new Point(HODO_X_2, HODO_HOLE_M1)) != 0
                        || maxEnergyMap.get(new Point(HODO_X_2, HODO_HOLE_1)) != 0)
                && (maxEnergyMap.get(new Point(HODO_X_3, HODO_HOLE_M1)) != 0
                        || maxEnergyMap.get(new Point(HODO_X_3, HODO_HOLE_1)) != 0))
            pattern.setHitStatus(HodoscopePattern.HODO_LX_CL_34, true);

        if (maxEnergyMap.get(new Point(HODO_X_3, HODO_HOLE_M1)) + maxEnergyMap.get(new Point(HODO_X_3, HODO_HOLE_1))
                + maxEnergyMap.get(new Point(HODO_X_4, HODO_HOLE_0)) > hodoThr
                && (maxEnergyMap.get(new Point(HODO_X_3, HODO_HOLE_M1)) != 0
                        || maxEnergyMap.get(new Point(HODO_X_3, HODO_HOLE_1)) != 0)
                && maxEnergyMap.get(new Point(HODO_X_4, HODO_HOLE_0)) != 0)
            pattern.setHitStatus(HodoscopePattern.HODO_LX_CL_45, true);

        return pattern;
    }

    /**
     * Get max energy for hits at a channel
     * @param hodoHitList
     * @return max energy of hits at a channel
     */
    private double maxEnergyForHodoHitList(List<CalorimeterHit> hodoHitList) {
        double maxEnergy = 0;

        for (CalorimeterHit hit : hodoHitList) {
            double energy = hit.getRawEnergy();
            if (energy > maxEnergy)
                maxEnergy = energy;
        }

        return maxEnergy;
    }

    @Override
    public void detectorChanged(Detector detector) { 
        // Get a copy of the calorimeter conditions for the detector.
        hodoConditions = DatabaseConditionsManager.getInstance().getHodoConditions();  
        
        // Populate the channel ID collections.
        populateChannelCollections();
    }

    /**
     * Populates channel map
     */
    private void populateChannelCollections() {
        // Load the conditions database and get the hodoscope channel
        // collection data.
        final DatabaseConditionsManager conditions = DatabaseConditionsManager.getInstance();
        final HodoscopeChannelCollection channels = conditions.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();       
        
        // Store the set of all channel IDs.
        for(HodoscopeChannel channel : channels) {
            channelMap.put(Long.valueOf(channel.getChannelId().intValue()), channel);
        }
    }
    
    /**
     * Get hodoscope channel id
     * @param hit
     * @return
     */
    public Long getHodoChannelID(CalorimeterHit hit) {
        return Long.valueOf(hodoConditions.getChannels().findGeometric(hit.getCellID()).getChannelId().intValue());
    }    

    /**
     * Sets the name of the LCIO collection containing the TI and VTP banks.
     * 
     * @param bankCollectionName - The bank collection name.
     */
    public void setBankCollectionName(String bankCollectionName) {
        this.bankCollectionName = bankCollectionName;
    }

    /**
     * Sets the name of the LCIO collection containing the simulated reconstructed
     * clusters.
     * 
     * @param clusterCollectionName - The cluster collection name.
     */
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    /**
     * Sets the name of the LCIO collection containing the simulated reconstructed
     * hodoscope hits.
     * 
     * @param hodoHitCollectionName - The hodoscope hit collection name.
     */
    public void setHodoHitCollectionName(String hodoHitCollectionName) {
        this.hodoHitCollectionName = hodoHitCollectionName;
    }

    /**
     * Sets whether or not triggers should be formed using all clusters, or only
     * those that where the integration window for the cluster is completely within
     * the bounds of the event window.
     * 
     * @param state - <code>true</code> means that only clusters where the entire
     *              cluster integration window is within the event time window will
     *              be used, while <code>false</code> means that all clusters will
     *              be used.
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
     * Set verbose status
     * @param verbose
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Gets the trigger time of an arbitrary trigger, so long as its source is
     * either a <code>Cluster</code> or <code>VTPCluster</code> object. Method also
     * supports pairs of these objects as a size two array. For pairs trigger of
     * 2019 experiment, time of the earliest cluster is set as time of trigger
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
            return TriggerModule2019.getClusterTime((Cluster) triggerSource);
        }

        // If the trigger is a simulated VTP singles trigger...
        if (triggerSource instanceof VTPCluster) {
            return TriggerModule2019.getClusterTime((VTPCluster) triggerSource);
        }

        // If the trigger is a simulated sim pair trigger...
        if (triggerSource instanceof Cluster[]) {
            Cluster[] source = (Cluster[]) triggerSource;
            double timeCluster0 = TriggerModule2019.getClusterTime(source[0]);
            double timeCluster1 = TriggerModule2019.getClusterTime(source[1]);
            if (timeCluster0 <= timeCluster1)
                return timeCluster0;
            else
                return timeCluster1;
        }

        // If the trigger is a simulated VTP pair trigger...
        if (triggerSource instanceof VTPCluster[]) {
            VTPCluster[] source = (VTPCluster[]) triggerSource;
            double timeCluster0 = TriggerModule2019.getClusterTime(source[0]);
            double timeCluster1 = TriggerModule2019.getClusterTime(source[1]);
            if (timeCluster0 <= timeCluster1)
                return timeCluster0;
            else
                return timeCluster1;
        }

        // Otherwise, return negative MIN_VALUE to indicate an invalid
        // trigger type.
        return Double.MIN_VALUE;
    }
    
    /**
     * Check geometry matching between Hodo and Ecal
     * @param x
     * @param layer1
     * @param layer2
     * @param runNumber
     * @return
     */
    private boolean geometryEcalHodoMatching(int x, HodoscopePattern layer1, HodoscopePattern layer2, int runNumber) { 
        if(x < 1 || x > 23) throw new IllegalArgumentException(String.format("Parameter \"%d\" is out of X-coordinage range [1, 23].", x));
        
        boolean flagLayer1 = false;
        boolean flagLayer2 = false;
        
        // Before and after run 10189 for 2019 experiment, geometry mapping between hodoscope and Ecal changes.
        if(runNumber < 10189) {
            // Cluster X <-> Layer 1 Matching
            if((x >= 5) && (x <= 9) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_1) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_12))) flagLayer1 = true;
            if(flagLayer1 == false) {
                if((x >= 6) && (x <= 11) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_12) || layer1.getHitStatus(HodoscopePattern.HODO_LX_2) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_23))) flagLayer1 = true;
                if(flagLayer1 == false) {
                    if((x >= 10) && (x <= 16) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_23) || layer1.getHitStatus(HodoscopePattern.HODO_LX_3) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_34))) flagLayer1 = true;
                    if(flagLayer1 == false) {
                        if((x >= 15) && (x <= 21) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_34) || layer1.getHitStatus(HodoscopePattern.HODO_LX_4) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_45))) flagLayer1 = true;
                        if(flagLayer1 == false) {
                            if((x >= 19) && (x <= 23) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_45) || layer1.getHitStatus(HodoscopePattern.HODO_LX_5))) flagLayer1 = true;
                        }
                    }
                }
            }

            // Cluster X <-> Layer 2 Matching
            if((x >= 5) && (x <= 8) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_1) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_12))) flagLayer2 = true;
            if(flagLayer2 == false) {
                if((x >= 7) && (x <= 12) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_12) || layer2.getHitStatus(HodoscopePattern.HODO_LX_2) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_23))) flagLayer2 = true;
                if(flagLayer2 == false) {
                    if((x >= 12) && (x <= 17) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_23) || layer2.getHitStatus(HodoscopePattern.HODO_LX_3) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_34))) flagLayer2 = true;
                    if(flagLayer2 == false) {
                        if((x >= 16) && (x <= 23) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_34) || layer2.getHitStatus(HodoscopePattern.HODO_LX_4) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_45))) flagLayer2 = true;
                        if(flagLayer2 == false) {
                            if((x >= 20) && (x <= 23) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_45) || layer2.getHitStatus(HodoscopePattern.HODO_LX_5))) flagLayer2 = true;
                        }
                    }
                }
            }
        }        
        else {
            // Cluster X <-> Layer 1 Matching
            if((x >= 5) && (x <= 9) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_1) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_12))) flagLayer1 = true;
            if(flagLayer1 == false) {
                if((x >= 6) && (x <= 12) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_12) || layer1.getHitStatus(HodoscopePattern.HODO_LX_2) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_23))) flagLayer1 = true;
                if(flagLayer1 == false) {
                    if((x >= 10) && (x <= 17) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_23) || layer1.getHitStatus(HodoscopePattern.HODO_LX_3) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_34))) flagLayer1 = true;
                    if(flagLayer1 == false) {
                        if((x >= 15) && (x <= 21) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_34) || layer1.getHitStatus(HodoscopePattern.HODO_LX_4) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_45))) flagLayer1 = true;
                        if(flagLayer1 == false) {
                            if((x >= 18) && (x <= 23) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_45) || layer1.getHitStatus(HodoscopePattern.HODO_LX_5))) flagLayer1 = true;
                        }
                    }
                }
            }

            // Cluster X <-> Layer 2 Matching
            if((x >= 5) && (x <= 9) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_1) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_12))) flagLayer2 = true;
            if(flagLayer2 == false) {
                if((x >= 6) && (x <= 14) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_12) || layer2.getHitStatus(HodoscopePattern.HODO_LX_2) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_23))) flagLayer2 = true;
                if(flagLayer2 == false) {
                    if((x >= 12) && (x <= 18) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_23) || layer2.getHitStatus(HodoscopePattern.HODO_LX_3) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_34))) flagLayer2 = true;
                    if(flagLayer2 == false) {
                        if((x >= 16) && (x <= 22) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_34) || layer2.getHitStatus(HodoscopePattern.HODO_LX_4) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_45))) flagLayer2 = true;
                        if(flagLayer2 == false) {
                            if((x >= 20) && (x <= 23) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_45) || layer2.getHitStatus(HodoscopePattern.HODO_LX_5))) flagLayer2 = true;
                        }
                    }
                }
            }
        }

        return flagLayer1 && flagLayer2;    
    }
    
    /**
     * Sets the run number of the DAQ configuration being processed.
     * This is only used when reading from data files.
     * @param run - The run number of the data files to be used.
     */
    public void setRunNumber(int run) {
        runNumber = run;
    }
    
    /**
     * Gets the run number that the DAQConfigDriver is set to use. This
     * will be <code>-1</code> in the event that the driver reads from
     * an EvIO file.
     * @return Returns the run number as an <code>int</code> primitive.
     * Will return <code>-1</code> if the driver is set to read from an
     * EvIO file.
     */
    public final int getRunNumber() { return runNumber; }
    
    
}