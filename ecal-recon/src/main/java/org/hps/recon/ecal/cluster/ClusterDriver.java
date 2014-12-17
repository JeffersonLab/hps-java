package org.hps.recon.ecal.cluster;

import java.util.List;
import java.util.logging.Logger;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import org.lcsim.util.log.LogUtil;
import org.lcsim.util.log.BasicFormatter;

/**
 * <p>
 * This is a basic Driver that creates ECAL <code>Cluster</code> collections 
 * through the {@link Clusterer} interface.
 * <p>
 * A specific clustering engine can be created with the {@link #setClusterer(String)} method
 * which will use a factory to create it by name.  The cuts of the {@link Clusterer}
 * can be set generically with the {@link #setCuts(double[])} method.  
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ClusterDriver extends Driver {
    
    protected String ecalName = "Ecal";
    protected HPSEcal3 ecal;
    protected String outputClusterCollectionName = "EcalClusters";
    protected String inputHitCollectionName = "EcalCalHits";
    protected Clusterer clusterer;
    protected boolean createEmptyClusterCollection = true;
    protected boolean raiseErrorNoHitCollection = false;
    protected boolean skipNoClusterEvents = false;
    protected boolean writeClusterCollection = true;
    protected boolean storeHits = true;
    protected double[] cuts;
    protected Logger logger = LogUtil.create(ClusterDriver.class, new BasicFormatter(ClusterDriver.class.getSimpleName()));
    
    /**
     * No arg constructor.
     */
    public ClusterDriver() {
        logger.config("initializing");
    }
    
    /**
     * Set the name of the ECAL in the detector framework.
     * This is kind of dangerous, so set this argument at your own peril!
     * @param ecalName The name of the ECAL.
     */
    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }
    
    /**
     * Set the name of the output Cluster collection.
     * @param outputClusterCollectionName The name of the output Cluster collection.
     */
    public void setOutputClusterCollectionName(String outputClusterCollectionName) {        
        this.outputClusterCollectionName = outputClusterCollectionName;
        this.getLogger().config("outputClusterCollectionName = " + this.outputClusterCollectionName);
    }
    
    /**
     * Set the name of the input CalorimeterHit collection to use for clustering.
     * @param inputHitcollectionName The name of the input hit collection.
     */
    public void setInputHitCollectionName(String inputHitCollectionName) {
        this.inputHitCollectionName = inputHitCollectionName;
        this.getLogger().config("inputClusterCollectionName = " + this.inputHitCollectionName);
    }
    
    /**
     * True to raise a <code>NextEventException</code> if no Clusters are created by the Clusterer.
     * @param skipNoClusterEvents True to skip events with no clusters.
     */
    public void setSkipNoClusterEvents(boolean skipNoClusterEvents) {
        this.skipNoClusterEvents = skipNoClusterEvents;       
        this.getLogger().config("skipNoClusterEvents = " + this.skipNoClusterEvents);
    }
    
    /**
     * True to write the Cluster collection to the output LCIO file.
     * @param writeClusterCollection True to write the Cluster to the event; false to mark as transient.
     */
    public void setWriteClusterCollection(boolean writeClusterCollection) {
        this.writeClusterCollection = writeClusterCollection;
        this.getLogger().config("writeClusterCollection = " + this.writeClusterCollection);
    }
    
    /**
     * True to raise an exception if the input hit collection is not found in the event.
     * @param raiseErrorNoHitCollection True to raise an exception if hit collection is not in event.
     */
    public void setRaiseErrorNoHitCollection(boolean raiseErrorNoHitCollection) {
        this.raiseErrorNoHitCollection = raiseErrorNoHitCollection;
    }
    
    /**
     * True to store hit references into the output clusters.
     * This will set <code>LCIOConstants.CLBIT_HITS</code> on the collection flags.
     * @param storeHits True to store hits.
     */
    public void setStoreHits(boolean storeHits) {
        this.storeHits = storeHits;
    }
    
    /**
     * Set the Clusterer by name.  
     * This will use a factory method which first tries to use some hard-coded names from 
     * the cluster package.  As a last resort, it will interpret the name as a canonical 
     * class name and try to instantiate it using the Class API.
     * @param The name or canonical class name of the Clusterer.
     */
    public void setClusterer(String name) {
        clusterer = ClustererFactory.create(name);
        this.getLogger().config("Clusterer was set to " + this.clusterer.getClass().getSimpleName());
    }
    
    /**
     * Set the Clusterer which implements the clustering algorithm.
     * @param clusterer The Clusterer.
     */
    public void setClusterer(Clusterer clusterer) {
        this.clusterer = clusterer;
        this.getLogger().config("Clusterer was set to " + this.clusterer.getClass().getSimpleName());
    }
    
    /**
     * Set whether an empty collection should be created if there are no clusters made by the Clusterer.
     * @param createEmptyClusterCollection True to write an empty collection to the event.
     */
    public void setCreateEmptyClusterCollection(boolean createEmptyClusterCollection) {
        this.createEmptyClusterCollection = createEmptyClusterCollection;
    }
    
    /**
     * Set the numerical cuts of the Clusterer.
     * @param cuts The numerical cuts.
     */
    public void setCuts(double[] cuts) {
        this.cuts = cuts;
    }
    
    /**
     * Setup conditions specific configuration.
     */
    public void detectorChanged(Detector detector) {
        logger.finer("detectorChanged - " + detector.getDetectorName());
        Subdetector subdetector = detector.getSubdetector("Ecal");
        if (subdetector == null) {
            throw new RuntimeException("There is no subdetector called " + ecalName + " in the detector.");
        }
        if (!(subdetector instanceof HPSEcal3)) {
            throw new RuntimeException("Ther subdetector " + ecalName + " does not have the right type.");
        }
        ecal = (HPSEcal3) subdetector;
    }
    
    /**
     * Perform start of job initialization.
     */
    public void startOfData() {
        logger.finer("startOfData");
        if (this.clusterer == null) {
            throw new RuntimeException("The clusterer was never initialized.");
        }
        if (this.cuts != null) {
            logger.config("setting cuts on clusterer");
            this.clusterer.setCuts(cuts);
            for (int cutIndex = 0; cutIndex < clusterer.getCuts().length; cutIndex++) {
                logger.config("  " + this.clusterer.getCutNames()[cutIndex] + " = " + this.clusterer.getCut(cutIndex));
            }            
        } 
        logger.config("initializing clusterer");
        this.clusterer.initialize();
    }
    
    /**
     * This method implements the default clustering procedure based on input parameters.
     */
    public void process(EventHeader event) {
        this.getLogger().fine("processing event #" + event.getEventNumber());
        if (event.hasCollection(CalorimeterHit.class, inputHitCollectionName)) {       
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputHitCollectionName);
            this.getLogger().fine("Input hit collection " + inputHitCollectionName + " has " + hits.size() + " hits.");
            List<Cluster> clusters = clusterer.createClusters(event, hits);
            if (clusters == null) {
                throw new RuntimeException("The clusterer returned null from its createClusters method.");
            }
            if (clusters.isEmpty() && this.skipNoClusterEvents) {
                logger.finer("Skipping event with no clusters.");
                throw new NextEventException();
            }
            if (event.hasCollection(Cluster.class, this.outputClusterCollectionName)) {
                this.getLogger().severe("There is already a cluster collection called " + this.outputClusterCollectionName);
                throw new RuntimeException("Cluster collection already exists in event.");
            }
            int flags = 0;
            if (this.storeHits) {
                flags = 1 << LCIOConstants.CLBIT_HITS;
            }
            if (!clusters.isEmpty() || this.createEmptyClusterCollection) {
                logger.finer("writing " + clusters.size() + " clusters to collection " + outputClusterCollectionName);
                event.put(outputClusterCollectionName, clusters, Cluster.class, flags);
                if (!this.writeClusterCollection) {
                    logger.finer("Collection is set to transient and will not be persisted.");
                    event.getMetaData(clusters).setTransient(true);
                }
            }
        } else {
            this.getLogger().severe("The input hit collection " + this.inputHitCollectionName + " is missing from the event.");
            if (this.raiseErrorNoHitCollection) {
                throw new RuntimeException("The expected input hit collection is missing from the event.");
            }
        }
    }
    
    /**
     * Get the logger for this Driver.
     * @return The logger.
     */
    public Logger getLogger() {
       return logger;
    }
    
    /**
     * Get a Clusterer using type inference for the concrete type.
     * @return The Clusterer object.
     */
    @SuppressWarnings("unchecked")
    <ClustererType extends Clusterer> ClustererType getClusterer() {
        // Return the Clusterer casting to the right type, which should always work because ClustererType must extend Clusterer.
        return (ClustererType) clusterer;
    }
}