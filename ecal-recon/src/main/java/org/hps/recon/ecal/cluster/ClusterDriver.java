package org.hps.recon.ecal.cluster;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * This is a basic Driver that creates Cluster collections through the
 * Clusterer interface.
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
    
    protected ClusterDriver() {
    }
    
    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }
    
    public void setOutputClusterCollectionName(String outputClusterCollectionName) {
        this.outputClusterCollectionName = outputClusterCollectionName;
    }
    
    public void setInputHitCollectionName(String inputHitCollectionName) {
        this.inputHitCollectionName = inputHitCollectionName;
    }
    
    public void setSkipNoClusterEvents(boolean skipNoClusterEvents) {
        this.skipNoClusterEvents = skipNoClusterEvents;
    }
    
    public void setWriteClusterCollection(boolean writeClusterCollection) {
        this.writeClusterCollection = writeClusterCollection;
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
    
    public void detectorChanged(Detector detector) {
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
        if (this.clusterer == null) {
            throw new RuntimeException("The clusterer was never initialized.");
        }
        if (this.clusterer instanceof AbstractClusterer) {
            ((AbstractClusterer)clusterer).setEcalSubdetector(ecal);
        }
    }
    
    /**
     * This method implements the default clustering procedure based on input parameters.
     */
    public void process(EventHeader event) {
        if (event.hasCollection(CalorimeterHit.class, inputHitCollectionName)) {       
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputHitCollectionName);
            List<Cluster> clusters = clusterer.createClusters(hits);
            if (clusters == null) {
                throw new RuntimeException("The clusterer returned a null pointer.");
            }
            if (clusters.isEmpty() && this.skipNoClusterEvents) {
                throw new NextEventException();
            }
            if (event.hasCollection(Cluster.class, this.outputClusterCollectionName)) {
                throw new RuntimeException("There is already a cluster collection called " + this.outputClusterCollectionName);
            }
            int flags = 0;
            if (this.storeHits) {
                flags = 1 << LCIOConstants.CLBIT_HITS;
            }
            if (!clusters.isEmpty() || this.createEmptyClusterCollection) {
                event.put(outputClusterCollectionName, clusters, Cluster.class, flags);
                if (!this.writeClusterCollection) {
                    event.getMetaData(clusters).setTransient(true);
                }
            }
        } else {
            this.getLogger().warning("The input hit collection " + this.inputHitCollectionName + " is missing from the event.");
            if (this.raiseErrorNoHitCollection) {
                throw new RuntimeException("The expected hit collection " + this.inputHitCollectionName + " is missing from the event.");
            }
        }
    }
}