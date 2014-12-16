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
    
    protected ClusterDriver() {
        logger.config("initializing");
    }
    
    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }
    
    public void setOutputClusterCollectionName(String outputClusterCollectionName) {        
        this.outputClusterCollectionName = outputClusterCollectionName;
        this.getLogger().config("outputClusterCollectionName = " + this.outputClusterCollectionName);
    }
    
    public void setInputHitCollectionName(String inputHitCollectionName) {
        this.inputHitCollectionName = inputHitCollectionName;
        this.getLogger().config("inputClusterCollectionName = " + this.inputHitCollectionName);
    }
    
    public void setSkipNoClusterEvents(boolean skipNoClusterEvents) {
        this.skipNoClusterEvents = skipNoClusterEvents;       
        this.getLogger().config("skipNoClusterEvents = " + this.skipNoClusterEvents);
    }
    
    public void setWriteClusterCollection(boolean writeClusterCollection) {
        this.writeClusterCollection = writeClusterCollection;
        this.getLogger().config("writeClusterCollection = " + this.writeClusterCollection);
    }
    
    public void setRaiseErrorNoHitCollection(boolean raiseErrorNoHitCollection) {
        this.raiseErrorNoHitCollection = raiseErrorNoHitCollection;
    }
    
    public void setStoreHits(boolean storeHits) {
        this.storeHits = storeHits;
    }
    
    public void setClusterer(String name) {
        clusterer = ClustererFactory.create(name);
    }
    
    public void setClusterer(Clusterer clusterer) {
        this.clusterer = clusterer;
    }
    
    public void setCreateEmptyClusterCollection(boolean createEmptyClusterCollection) {
        this.createEmptyClusterCollection = createEmptyClusterCollection;
    }
    
    public void setCuts(double[] cuts) {
        this.cuts = cuts;
    }
    
    public void detectorChanged(Detector detector) {
        logger.fine("detectorChanged");
        Subdetector subdetector = detector.getSubdetector(ecalName);
        if (subdetector == null) {
            throw new RuntimeException("There is no subdetector called " + ecalName + " in the detector.");
        }
        if (!(subdetector instanceof HPSEcal3)) {
            throw new RuntimeException("Ther subdetector " + ecalName + " does not have the right type.");
        }
        ecal = (HPSEcal3) subdetector;
    }
    
    public void startOfData() {
        logger.fine("startOfData");
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
    
    public Logger getLogger() {
       return logger;
    }
}