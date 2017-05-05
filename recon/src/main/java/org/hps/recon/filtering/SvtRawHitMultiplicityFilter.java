package org.hps.recon.filtering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.SvtPlotUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;

/**
 * Filter events based on max number of strip hits
 */
public class SvtRawHitMultiplicityFilter extends EventReconFilter {

    private Logger logger = Logger.getLogger(SvtRawHitMultiplicityFilter.class.getSimpleName());
    private int minHitsPerSensor = 1;
    private int maxHitsPerSensor = -1;
    private int minHitsPerHalf = 3;
    private int maxHitsPerHalf = -1;
    private final String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private final boolean dropSmallHitEvents = true;

    public SvtRawHitMultiplicityFilter() {
       logger.setLevel(Level.INFO);
    }
    
    @Override
    protected void process(EventHeader event) {
     
        incrementEventProcessed();
        
      
        if(!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName))
            skipEvent();
        
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

        if (dropSmallHitEvents && SvtPlotUtils.countSmallHits(rawHits) > 3) {
            return;
        }

        int nhits[] = {0,0};
        Map<String, List<RawTrackerHit> > sensorHitMap = new HashMap<String, List<RawTrackerHit>>();
        for (RawTrackerHit rawHit : rawHits) {
            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
            
            boolean isTop = sensor.isTopLayer();
            if(isTop)
                nhits[0]++;
            else
                nhits[1]++;
            
            String name = sensor.getName();
            List<RawTrackerHit> hits;
            if(sensorHitMap.containsKey(name))
                hits = sensorHitMap.get(name);
            else {
                hits = new ArrayList<RawTrackerHit>();
                sensorHitMap.put(name, hits);
            }
            hits.add(rawHit);
        }
        
        
        // check top and bottom hit multiplicity
        if(minHitsPerHalf >= 0 && nhits[0] < minHitsPerHalf)
            skipEvent();        
        if(minHitsPerHalf >= 0 && nhits[1] < minHitsPerHalf)
            skipEvent();
        if(maxHitsPerHalf >= 0 && nhits[0] > maxHitsPerHalf)
            skipEvent();        
        if(maxHitsPerHalf >= 0 && nhits[1] > maxHitsPerHalf)
            skipEvent();

        Map<Integer, List<RawTrackerHit>> trackCandL13Top = new HashMap<Integer, List<RawTrackerHit>>();
        Map<Integer, List<RawTrackerHit>> trackCandL13Bot = new HashMap<Integer, List<RawTrackerHit>>();
        Map<Integer, List<RawTrackerHit>> trackCandL46TopH = new HashMap<Integer, List<RawTrackerHit>>();
        Map<Integer, List<RawTrackerHit>> trackCandL46BotH = new HashMap<Integer, List<RawTrackerHit>>();
        Map<Integer, List<RawTrackerHit>> trackCandL46TopS = new HashMap<Integer, List<RawTrackerHit>>();
        Map<Integer, List<RawTrackerHit>> trackCandL46BotS = new HashMap<Integer, List<RawTrackerHit>>();
        
        for(Map.Entry<String, List<RawTrackerHit>> entry : sensorHitMap.entrySet()) {
            
            // check multiplicity on the sensor
            int n = entry.getValue().size();
            if(minHitsPerSensor >= 0 && n < minHitsPerSensor)
                continue;
            if(maxHitsPerSensor >= 0 && n > maxHitsPerSensor)
                continue;

            int layer = HPSTrackerBuilder.getLayerFromVolumeName(entry.getKey());
            boolean isTop = HPSTrackerBuilder.getHalfFromName(entry.getKey()).equalsIgnoreCase("top") ? true : false;
            
            if (layer < 4) {
                List<RawTrackerHit> list;
                if( isTop ) {   
                    if (trackCandL13Top.containsKey(layer)) {
                        list = trackCandL13Top.get(layer);
                    } else {
                        list =  new ArrayList<RawTrackerHit>();
                        trackCandL13Top.put(layer,list);
                    }
                } else {
                    if (trackCandL13Bot.containsKey(layer)) {
                        list = trackCandL13Bot.get(layer);
                    } else {
                        list =  new ArrayList<RawTrackerHit>();
                        trackCandL13Bot.put(layer,list);
                    }
                }
                list.addAll(entry.getValue());

            } else {
                List<RawTrackerHit> list;
                boolean isHole = HPSTrackerBuilder.isHoleFromName(entry.getKey());
                if( isTop ) {   
                    if( isHole ) {
                        if (trackCandL46TopH.containsKey(layer)) {
                            list = trackCandL46TopH.get(layer);
                        } else {
                            list =  new ArrayList<RawTrackerHit>();
                            trackCandL46TopH.put(layer,list);
                        }
                    } else {
                        if (trackCandL46TopS.containsKey(layer)) {
                            list = trackCandL46TopS.get(layer);
                        } else {
                            list =  new ArrayList<RawTrackerHit>();
                            trackCandL46TopS.put(layer,list);
                        }
                    }
                } else {
                    if( isHole ) {
                        if (trackCandL46BotH.containsKey(layer)) {
                            list = trackCandL46BotH.get(layer);
                        } else {
                            list =  new ArrayList<RawTrackerHit>();
                            trackCandL46BotH.put(layer,list);
                        }
                    } else {
                        if (trackCandL46BotS.containsKey(layer)) {
                            list = trackCandL46BotS.get(layer);
                        } else {
                            list =  new ArrayList<RawTrackerHit>();
                            trackCandL46BotS.put(layer,list);
                        }
                    }
                }
                list.addAll(entry.getValue());

            }
        }

        
        // require candidate tracks in L13 and L46 in the same half
        
        
        StringBuffer sb = new StringBuffer();
        sb.append("Event with " + rawHits.size() + " hits passed:\n");
        for(RawTrackerHit hit : rawHits) {
            sb.append(hit.getDetectorElement().getName() + "\n");
        }
        logger.info(sb.toString());
        
        incrementEventPassed();
        
    }

    public void setMinHitsPerSensor(int minHitsPerSensor) {
        this.minHitsPerSensor = minHitsPerSensor;
    }

    public void setMaxHitsPerSensor(int maxHitsPerSensor) {
        this.maxHitsPerSensor = maxHitsPerSensor;
    }

    public void setMinHitsPerHalf(int minHitsPerHalf) {
        this.minHitsPerHalf = minHitsPerHalf;
    }

    public void setMaxHitsPerHalf(int maxHitsPerHalf) {
        this.maxHitsPerHalf = maxHitsPerHalf;
    }

/*
    private static class Sensor {
        boolean isAxial;
        SiSensor
        List<RawTrackerHit> hits = new ArrayList<>();
        Sensor(boolean isAxial) {
            this.isAxial = isAxial;
        }
        
    }
    private static class Pair {
        HpsSiSensor axialSensor;
        HpsSiSensor stereoSensor;
        
    }
       */
}
