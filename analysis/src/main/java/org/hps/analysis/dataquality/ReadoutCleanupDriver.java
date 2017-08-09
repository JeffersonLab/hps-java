package org.hps.analysis.dataquality;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.recon.tracking.digitization.sisim.config.CollectionHandler;

/**
 * This Driver clears the DetectorElement Readout
 * associated with a given collection in the event header.  
 * It accepts a list of collection names and ignores
 * others.
 * 
 * @author jeremym
 * @version $Id: ReadoutCleanupDriver.java,v 1.2 2012/04/25 14:27:00 jeremy Exp $
 */
public class ReadoutCleanupDriver
extends CollectionHandler
{    
    public ReadoutCleanupDriver() 
    {}
    
    public ReadoutCleanupDriver(List<String> collectionNames) {
        super(collectionNames);
    }
    
    public ReadoutCleanupDriver(String[] collectionNames) {
        super(collectionNames);
    }
    
    public void setCollectionNames(String[] collectionNames) {
        this.collections = new HashSet<String>(Arrays.asList(collectionNames));
    }
    
    protected void process(EventHeader event)
    {        
        Detector detector = event.getDetector();
        for(String collection: this.collections){
//            System.out.println("Removing "+collection);
            event.remove(collection);
        }
        for (Subdetector subdet : detector.getSubdetectors().values()) {
            if (subdet.getReadout() != null) {
                if (canHandle(subdet.getReadout().getName())) {
                    if (subdet.getDetectorElement() != null) {
                        subdet.getDetectorElement().clearReadouts();
                    }
                }
            }
        }        
    }     
}