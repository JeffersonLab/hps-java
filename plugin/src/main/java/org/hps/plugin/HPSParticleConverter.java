package org.hps.plugin;

import hep.graphics.heprep.HepRepFactory;
import hep.graphics.heprep.HepRepInstance;
import hep.graphics.heprep.HepRepInstanceTree;
import hep.graphics.heprep.HepRepType;
import hep.graphics.heprep.HepRepTypeTree;
import hep.physics.vec.Hep3Vector;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.heprep.HepRepCollectionConverter;
import org.lcsim.util.heprep.LCSimHepRepConverter;
import org.lcsim.util.heprep.RandomColorMap;
import org.lcsim.util.swim.HelixSwimmer;
import org.lcsim.util.swim.HelixSwimmerYField;

/**
 * This class converts an LCIO ReconstructedParticle to the HepRep format for
 * display by Wired4.  Clusters and tracks from a RP are given the same randomly
 * generated color.  Charged Tracks, Neutral Tracks, and Calorimeter Clusters are
 * given their own sub-types, so that they can be easily switched on and off.
 * 
 * @author jeremym
 */
class HPSParticleConverter implements HepRepCollectionConverter
{
    
    private static final double[] ORIGIN = {0, 0, 0.00001};
    RandomColorMap rcolorMap;

    private static final double[] zero = {0, 0, 0};
    
    public HPSParticleConverter()
    {
        rcolorMap = new RandomColorMap(50);
    }

    public boolean canHandle(Class k)
    {
        return ReconstructedParticle.class.isAssignableFrom(k);
    }
    
    public void convert(EventHeader event, List collection, HepRepFactory factory, HepRepTypeTree typeTree, HepRepInstanceTree instanceTree)
    {               
        rcolorMap.reset(collection.size());
        
        String pfoName = event.getMetaData(collection).getName();        
        
        // RP top-level type.
        HepRepType rpType = factory.createHepRepType(typeTree, pfoName);
        rpType.addAttValue("layer", LCSimHepRepConverter.PARTICLES_LAYER);
        
        // Neutral Tracks type.
        HepRepType neutralTracksType = factory.createHepRepType(rpType, "NeutralTracks");
        neutralTracksType.addAttValue("layer", LCSimHepRepConverter.PARTICLES_LAYER);
        neutralTracksType.addAttValue("drawAs","Line");
        /*neutralTracksType.addAttValue("LineStyle", "Dashed"); // Doesn't work.
        neutralTracksType.addAttValue("color",Color.GREEN);*/
        
        // Charged Tracks type.
        HepRepType chargedTracksType = factory.createHepRepType(rpType, "ChargedTracks");
        chargedTracksType.addAttValue("layer", LCSimHepRepConverter.PARTICLES_LAYER);
        chargedTracksType.addAttValue("drawAs","Line");
        
        // Clusters type.
        HepRepType clustersType = factory.createHepRepType(rpType, "CalClusters");
        clustersType.addAttValue("color",Color.RED);
        clustersType.addAttValue("fill",true);
        clustersType.addAttValue("fillColor",Color.RED);
        clustersType.addAttValue("layer", LCSimHepRepConverter.HITS_LAYER);
        
        /*
        clustersType.addAttValue("layer", LCSimHepRepConverter.HITS_LAYER);
        clustersType.addAttValue("drawAs", "Point");
        clustersType.addAttValue("MarkName", "Box");
        clustersType.addAttDef("energy", "Hit Energy", "physics", "");
        clustersType.addAttDef("cluster", "Cluster Energy", "physics", "");
        */
        
        int rpCnt = 0;
        for (ReconstructedParticle rp : (List<ReconstructedParticle>) collection)
        {                      
            // Get the color for this RP.
            Color rpColor = rcolorMap.getColor(rpCnt % rcolorMap.size());
            
            List<Cluster> clusters = rp.getClusters();
            List<Track> tracks = rp.getTracks();
            
            convertClusters(event, clusters, factory, typeTree, instanceTree, rpColor, clustersType);                                                                    
            convertTracks(event, tracks, factory, typeTree, instanceTree, rpColor, chargedTracksType);
            
            if (rp.getCharge() == 0)
            {
                convertNeutralParticle(event.getDetector(), rp, instanceTree, factory, neutralTracksType, rpColor);
            }                      
            
            ++rpCnt;
        }
    }
    
    private void convertClusters(EventHeader event, List<Cluster> collection, HepRepFactory factory, HepRepTypeTree typeTree, HepRepInstanceTree instanceTree, Color clusterColor, HepRepType type)
    {       
        List<CalorimeterHit> hits = new ArrayList<CalorimeterHit>();
        
        HepRepInstance instanceC = factory.createHepRepInstance(instanceTree, type);
        
        for (Cluster cluster : collection)
        {
            if (cluster.getCalorimeterHits().size() != 0) {
                hits.addAll(cluster.getCalorimeterHits());
            }
        }
           
        if (hits != null)
        {
            for (CalorimeterHit hit : hits)
            {
                if (hit != null) {
                    double[] pos = hit.getPosition();
                    HepRepInstance instanceX = factory.createHepRepInstance(instanceC, type);
                    instanceX.addAttValue("MarkSize", 5);
                    instanceX.addAttValue("color", clusterColor);
                    instanceX.addAttValue("showparentattributes", true);
                    instanceX.addAttValue("pickparent", true);
                    //HepRepPoint pp = 
                    factory.createHepRepPoint(instanceX, pos[0], pos[1], pos[2]);
                }
            }
        }
    }   

    private void convertTracks(EventHeader event, List<Track> collection, HepRepFactory factory, HepRepTypeTree typeTree, HepRepInstanceTree instanceTree, Color trackColor, HepRepType type)
    {   
        if (collection.size() == 0)
            return;
        
        Detector detector = event.getDetector();

        double trackingRMax = detector.getConstants().get("tracking_region_radius").getValue();
        double trackingZMax = detector.getConstants().get("tracking_region_zmax").getValue();

        double[] field = detector.getFieldMap().getField(ORIGIN);
        HelixSwimmer helix = new HelixSwimmerYField(field[1]);
             
        for (Track t : (List<Track>) collection)
        {                        
            helix.setTrack(t);
            double distanceToCylinder = helix.getDistanceToCylinder(trackingRMax, trackingZMax);

            HepRepInstance instanceX = factory.createHepRepInstance(instanceTree, type);
            instanceX.addAttValue("color", trackColor);

            double dAlpha = 10; // 1cm
            for (int k = 0; k < 200; k++)
            {
                double d = k * dAlpha;
                if (d > distanceToCylinder)
                    break;
                Hep3Vector point = helix.getPointAtDistance(d);
                factory.createHepRepPoint(instanceX, point.x(), point.y(), point.z());
            }
        }
    }
    
    // TODO: Connect to cluster center instead of swimming.
    public void convertNeutralParticle(Detector detector, ReconstructedParticle p, HepRepInstanceTree tree, HepRepFactory factory, HepRepType type, Color rpColor)
    {  
        HepRepInstance instanceX = factory.createHepRepInstance(tree, type);
        
        double trackingRMax = detector.getConstants().get("tracking_region_radius").getValue();
        double trackingZMax = detector.getConstants().get("tracking_region_zmax").getValue();

        double[] field = detector.getFieldMap().getField(zero);
        HelixSwimmer helix = new HelixSwimmer(field[2]);
        
        double charge = p.getCharge();
        Hep3Vector start =  p.getReferencePoint();
        Hep3Vector momentum = p.getMomentum();
        helix.setTrack(momentum, start, (int) charge);
        double distanceToCylinder = helix.getDistanceToCylinder(trackingRMax,trackingZMax);
        
        Hep3Vector stop = helix.getPointAtDistance(distanceToCylinder);
           
        factory.createHepRepPoint(instanceX,start.x(),start.y(),start.z());
        factory.createHepRepPoint(instanceX,stop.x(),stop.y(),stop.z());
        instanceX.addAttValue("color", rpColor);
        instanceX.addAttValue("momentum",p.getEnergy());
        instanceX.addAttValue("type",p.getType());                   
    }
}