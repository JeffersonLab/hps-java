package org.hps.readout.ecal.updated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.hps.readout.util.collection.TriggeredLCIOData;
import org.hps.recon.ecal.cluster.ClusterType;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;

/**
 * Class <code>GTPClusterReadoutDriver</code> produces GTP cluster
 * objects for use in the readout trigger simulation. It takes in
 * {@link org.lcsim.event.CalorimeterHit CalorimeterHit} objects as
 * input and generates clusters from these using the GTP algorithm.
 * This algorithm works by selected all hits in the current
 * clock-cycle (4 ns period) and comparing them to adjacent hits. If
 * a given hit is an energy maximum compared to all adjacent hits in
 * both the current clock-cycle, and a number of clock-cycles before
 * and after the current cycle (defined through the variable {@link
 * org.hps.readout.ecal.updated.GTPClusterReadoutDriver#temporalWindow
 * temporalWindow} and set through the method {@link
 * org.hps.readout.ecal.updated.GTPClusterReadoutDriver#setClusterWindow(int)
 * setClusterWindow(int)}), then it is a seed hit so long as it also
 * exceeds a certain minimum energy (defined through the variable
 * {@link
 * org.hps.readout.ecal.updated.GTPClusterReadoutDriver#seedEnergyThreshold
 * seedEnergyThreshold} and set through the method {@link
 * org.hps.readout.ecal.updated.GTPClusterReadoutDriver#setSeedEnergyThreshold(double)
 * setSeedEnergyThreshold(double)}).<br/><br/>
 * Clusters are then output as objects of type {@link
 * org.lcsim.event.Cluster Cluster} to the specified output
 * collection. If the {@link
 * org.hps.readout.ecal.updated.GTPClusterReadoutDriver#setWriteClusterCollection(boolean)
 * setWriteClusterCollection(boolean)} is set to true, the clusters
 * will also be persisted into the output LCIO file.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class GTPClusterReadoutDriver extends ReadoutDriver {
    // ==============================================================
    // ==== LCIO Collections ========================================
    // ==============================================================
    
    /**
     * The name of the collection that contains the calorimeter hits
     * from which clusters should be generated.
     */
    private String inputCollectionName = "EcalCorrectedHits";
    /**
     * The name of the collection into which generated clusters should
     * be output.
     */
    private String outputCollectionName = "EcalClustersGTP";
    
    // ==============================================================
    // ==== Driver Options ==========================================
    // ==============================================================
    
    /**
     * The time window used for cluster verification. A seed hit must
     * be the highest energy hit within plus or minus this range in
     * order to be considered a valid cluster.
     */
    private int temporalWindow = 16;
    /**
     * The minimum energy needed for a hit to be considered as a seed
     * hit candidate.
     */
    private double seedEnergyThreshold = 0.050;
    /**
     * The local time for the driver. This starts at 2 ns due to a
     * quirk in the timing of the {@link
     * org.hps.readout.ecal.updated.EcalReadoutDriver
     * EcalReadoutDriver}.
     */
    private double localTime = 0.0;
    /**
     * The length of time by which objects produced by this driver
     * are shifted due to the need to buffer data from later events.
     * This is calculated automatically.
     */
    private double localTimeDisplacement = 0;
    
    // ==============================================================
    // ==== Driver Parameters =======================================
    // ==============================================================
    
    /**
     * An object which can provide, given an argument cell ID, a map
     * of cell IDs that are physically adjacent to the argument ID.
     * This is used to determine adjacency for energy comparisons in
     * the clustering algorithm.
     */
    private NeighborMap neighborMap;
    
    private HPSEcal3 calorimeterGeometry = null;
    
    @Override
    public void detectorChanged(Detector etector) {
        // Get the calorimeter data object.
        //HPSEcal3 ecal = (HPSEcal3) DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector("Ecal");
        calorimeterGeometry = (HPSEcal3) DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector("Ecal");
        if(calorimeterGeometry == null) {
            throw new IllegalStateException("Error: Calorimeter geometry data object not defined.");
        }
        
        // Get the calorimeter hit neighbor map.
        neighborMap = calorimeterGeometry.getNeighborMap();
        if(neighborMap == null) {
            throw new IllegalStateException("Error: Calorimeter hit neighbor map is not defined.");
        }
    }
    
    @Override
    public void process(EventHeader event) {
        // Check the data management driver to determine whether the
        // input collection is available or not.
        if(!ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime + temporalWindow + 4.0)) {
            return;
        }
        
        // Get the hits that occur during the present clock-cycle, as
        // well as the hits that occur in the verification window
        // both before and after the current clock-cycle.
        // TODO: Simplify this?
        Collection<CalorimeterHit> seedCandidates = ReadoutDataManager.getData(localTime, localTime + 4.0, inputCollectionName, CalorimeterHit.class);
        Collection<CalorimeterHit> foreHits = ReadoutDataManager.getData(localTime - temporalWindow, localTime, inputCollectionName, CalorimeterHit.class);
        Collection<CalorimeterHit> postHits = ReadoutDataManager.getData(localTime + 4.0, localTime + temporalWindow + 4.0, inputCollectionName, CalorimeterHit.class);
        
        // Increment the local time.
        localTime += 4.0;
        
        // DEBUG :: Print out the input hits.
        // TODO: Simplify this?
        List<CalorimeterHit> allHits = new ArrayList<CalorimeterHit>(seedCandidates.size() + foreHits.size() + postHits.size());
        allHits.addAll(foreHits);
        allHits.addAll(seedCandidates);
        allHits.addAll(postHits);
        
        // Store newly created clusters.
        List<Cluster> gtpClusters = new ArrayList<Cluster>();
        
        // Iterate over all seed hit candidates.
        seedLoop:
        for(CalorimeterHit seedCandidate : seedCandidates) {
            // A seed candidate must meet a minimum energy cut to be
            // considered for clustering.
            if(seedCandidate.getRawEnergy() < seedEnergyThreshold) {
                continue seedLoop;
            }
            
            // Collect other hits that are adjacent to the seed hit
            // and may be a part of the cluster.
            List<CalorimeterHit> clusterHits = new ArrayList<CalorimeterHit>();
            
            // Iterate over all other hits in the clustering window
            // and check that the seed conditions are met for the
            // seed candidate. Note that all hits are properly within
            // the clustering time window by definition, so the time
            // condition is not checked explicitly.
            hitLoop:
            for(CalorimeterHit hit : allHits) {
                // If the hit is not adjacent to the seed hit, it can
                // be ignored.
                if(!neighborMap.get(seedCandidate.getCellID()).contains(hit.getCellID())) {
                    continue hitLoop;
                }
                
                // A seed hit must have the highest energy in its
                // spatiotemporal window. If it is not, this is not a
                // valid seed hit.
                if(seedCandidate.getRawEnergy() < hit.getRawEnergy()) {
                    continue seedLoop;
                }
                
                // Add the hit to the list of cluster hits.
                clusterHits.add(hit);
            }
            
            // If no adjacent hit was found that invalidates the seed
            // condition, then the seed candidate is valid and a
            // cluster should be formed.
            gtpClusters.add(createBasicCluster(seedCandidate, clusterHits));
        }
        
        // Pass the clusters to the data management driver.
        ReadoutDataManager.addData(outputCollectionName, gtpClusters, Cluster.class);
    }
    
    @Override
    public void startOfData() {
        // Define the output LCSim collection parameters.
        LCIOCollectionFactory.setCollectionName(outputCollectionName);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags(1 << LCIOConstants.CLBIT_HITS);
        LCIOCollection<Cluster> clusterCollectionParams = LCIOCollectionFactory.produceLCIOCollection(Cluster.class);
        
        // Instantiate the GTP cluster collection with the readout
        // data manager.
        localTimeDisplacement = temporalWindow + 4.0;
        addDependency(inputCollectionName);
        ReadoutDataManager.registerCollection(clusterCollectionParams, false);
    }
    
    @Override
    protected Collection<TriggeredLCIOData<?>> getOnTriggerData(double triggerTime) {
        // If clusters are not to be output, return null.
        if(!isPersistent()) { return null; }
        
        // Create a list to store the on-trigger collections. There
        // are two collections outputs for this driver - the clusters
        // and the cluster hits. Unlike other drivers, the clusterer
        // must handle its own output because the manager does not
        // know that it must also specifically output the hits from
        // each cluster as well.
        List<TriggeredLCIOData<?>> collectionsList = new ArrayList<TriggeredLCIOData<?>>(2);
        
        // Define the LCIO collection settings for the clusters.
        LCIOCollectionFactory.setCollectionName(outputCollectionName);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags(1 << LCIOConstants.CLBIT_HITS);
        LCIOCollection<Cluster> clusterCollectionParams = LCIOCollectionFactory.produceLCIOCollection(Cluster.class);
        
        // Define the LCIO collection settings for the cluster hits.
        int hitFlags = 0;
        hitFlags += 1 << LCIOConstants.RCHBIT_TIME;
        hitFlags += 1 << LCIOConstants.RCHBIT_LONG;
        LCIOCollectionFactory.setCollectionName("EcalClustersGTPSimHits");
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags(hitFlags);
        LCIOCollectionFactory.setReadoutName(calorimeterGeometry.getReadout().getName());
        LCIOCollection<CalorimeterHit> clusterHitsCollectionParams = LCIOCollectionFactory.produceLCIOCollection(CalorimeterHit.class);
        
        // Get the output time range for clusters. This is either the
        // user defined output range, or the default readout window
        // that is defined by the readout data manager.
        double startTime;
        if(Double.isNaN(getReadoutWindowBefore())) { startTime = triggerTime - ReadoutDataManager.getTriggerOffset(); }
        else { startTime = triggerTime - getReadoutWindowBefore(); }
        
        double endTime;
        if(Double.isNaN(getReadoutWindowAfter())) { endTime = startTime + ReadoutDataManager.getReadoutWindow(); }
        else { endTime = triggerTime + getReadoutWindowAfter(); }
        
        // Get the cluster data and populate a list of cluster hits.
        Collection<Cluster> clusters = ReadoutDataManager.getData(startTime, endTime, outputCollectionName, Cluster.class);
        List<CalorimeterHit> clusterHits = new ArrayList<CalorimeterHit>();
        for(Cluster cluster : clusters) {
            clusterHits.addAll(cluster.getCalorimeterHits());
        }
        
        // Create the LCIO on-trigger data lists.
        TriggeredLCIOData<CalorimeterHit> clusterHitData = new TriggeredLCIOData<CalorimeterHit>(clusterHitsCollectionParams);
        clusterHitData.getData().addAll(clusterHits);
        collectionsList.add(clusterHitData);
        
        TriggeredLCIOData<Cluster> clusterData = new TriggeredLCIOData<Cluster>(clusterCollectionParams);
        clusterData.getData().addAll(clusters);
        collectionsList.add(clusterData);
        
        // Return the on-trigger data.
        return collectionsList;
    }
    
    @Override
    protected double getTimeDisplacement() {
        return localTimeDisplacement;
    }

    @Override
    protected double getTimeNeededForLocalOutput() {
        return 0;
    }
    
    /**
     * Creates a new cluster object from a seed hit and list of hits.
     * @param seedHit - The seed hit of the new cluster.
     * @param hits - The hits for the new cluster.
     * @return Returns a {@link org.lcsim.event.Cluster Cluster}
     * object with the specified properties.
     */
    private static final Cluster createBasicCluster(CalorimeterHit seedHit, List<CalorimeterHit> hits) {
        BaseCluster cluster = new BaseCluster();
        cluster.setType(ClusterType.GTP.getType());
        cluster.addHit(seedHit);
        cluster.setPosition(seedHit.getDetectorElement().getGeometry().getPosition().v());
        cluster.setNeedsPropertyCalculation(false);
        cluster.addHits(hits);
        return cluster;
    }
    
    /**
     * Sets the size of the hit verification temporal window. Note
     * that this defines the size of the window in one direction, so
     * the full time window will be <code>(2 * clusterWindow)+
     * 1</code> clock-cycles in length. (i.e., it will be a length of
     * <code>clusterWindow</code> before the seed hit, a length of
     * <code>clusterWindow</code> after the seed hit, plus the cycle
     * that includes the seed hit.) Time length is in clock-cycles.
     * @param value - The number of clock-cycles around the hit in
     * one direction.
     */
    public void setClusterWindow(int value) {
        temporalWindow = value * 4;
    }
    
    /**
     * Sets the minimum seed energy needed for a hit to be considered
     * for forming a cluster. This is the seed energy lower bound
     * trigger cut and is in units of GeV.
     * @param value - The minimum cluster seed energy in GeV.
     */
    public void setSeedEnergyThreshold(double value) {
        seedEnergyThreshold = value;
    }
}