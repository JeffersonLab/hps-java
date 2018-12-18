package org.hps.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.util.Driver;

/**
 *
 * @author mrsolt
 * This driver removes all raw tracker hits except those that are truth hits
 */
public class KeepTruthRawTrackerHits extends Driver {

    private String rawHitCollectionName = "SVTRawTrackerHits";
    private String simHitCollectionName = "TrackerHits_truth";
    private String rawHitTruthCollectionName = "SVTRawTrackesrHits_truth";
    private String rawHitRelationsCollectionName = "SVTTrueHitRelations";

    protected Set<String> collections = new HashSet<String>();

    public KeepTruthRawTrackerHits() {
    }
    
    public void setRawHitCollectionName(String rawHitCollectionName) {
        this.rawHitCollectionName = rawHitCollectionName;
    }
    
    public void setSimHitCollectionName(String simHitCollectionName) {
        this.simHitCollectionName = simHitCollectionName;
    }
    
    public void setRawHitTruthCollectionName(String rawHitTruthCollectionName) {
        this.rawHitTruthCollectionName = rawHitTruthCollectionName;
    }

    public void setRawHitRelationsCollectionName(String rawHitRelationsCollectionName) {
        this.rawHitRelationsCollectionName = rawHitRelationsCollectionName;
    }
    
    public static RelationalTable getRawToTruthTable(EventHeader event,String RawHitRelationsCollectionName) {
        RelationalTable hitToTruth = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> hitrelations = event.get(LCRelation.class, RawHitRelationsCollectionName);
        for (LCRelation relation : hitrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hitToTruth.add(relation.getFrom(), relation.getTo());
        Pair<EventHeader, RelationalTable> hitToTruthCache = new Pair<EventHeader, RelationalTable>(event, hitToTruth);
        return hitToTruthCache.getSecond();
    }
    
    protected void process(EventHeader event) {
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawHitCollectionName);
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, simHitCollectionName);
        RelationalTable rawToTruth = getRawToTruthTable(event,rawHitRelationsCollectionName);
        List<RawTrackerHit> truthRawhits = new ArrayList<RawTrackerHit>();
        for (SimTrackerHit hit : simHits) {
            System.out.println(rawToTruth.allTo(hit));
            truthRawhits.addAll(rawToTruth.allTo(hit));
        }
        /*for (RawTrackerHit hit : rawHits) {
            System.out.println(rawToTruth.allFrom(hit));
            truthRawhits.addAll(rawToTruth.allFrom(hit));
        }*/
        System.out.println(truthRawhits);
        event.put(rawHitTruthCollectionName, truthRawhits, RawTrackerHit.class, 0);
        if (event.hasItem(rawHitCollectionName))
            event.remove(rawHitCollectionName);
    }
}
