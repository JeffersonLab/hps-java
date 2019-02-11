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
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;

/**
 *
 * @author mrsolt
 * This driver keeps all truth hits that correspond to a track's 1D strip hits
 */
public class KeepTruthRawTrackerHits extends Driver {

    private String simHitCollectionName = "TrackerHits_truth";
    private String rawHitRelationsCollectionName = "SVTTrueHitRelations";
    
    private String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String stripHitOutputCollectionName = "StripClusterer_SiTrackerHitStrip1D_truth";

    protected Set<String> collections = new HashSet<String>();

    public KeepTruthRawTrackerHits() {
    
    }
    
    public void setSimHitCollectionName(String simHitCollectionName) {
        this.simHitCollectionName = simHitCollectionName;
    }

    public void setRawHitRelationsCollectionName(String rawHitRelationsCollectionName) {
        this.rawHitRelationsCollectionName = rawHitRelationsCollectionName;
    }
    
    public static RelationalTable getRawToTruthTable(EventHeader event,String RawHitRelationsCollectionName) {
        RelationalTable hitToTruth = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> hitrelations = event.get(LCRelation.class, RawHitRelationsCollectionName);
        List<LCRelation> hitrelations_truth = new ArrayList<LCRelation>();
        for (LCRelation relation : hitrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null){
                hitToTruth.add(relation.getFrom(), relation.getTo());
                hitrelations_truth.add(relation);
            }
        Pair<EventHeader, RelationalTable> hitToTruthCache = new Pair<EventHeader, RelationalTable>(event, hitToTruth);
        return hitToTruthCache.getSecond();
    }
    
    protected void process(EventHeader event) {
        List<SiTrackerHitStrip1D> striphits = event.get(SiTrackerHitStrip1D.class, stripHitInputCollectionName);
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, simHitCollectionName);
        List<SiTrackerHitStrip1D> truthStripHits = new ArrayList<SiTrackerHitStrip1D>();
        RelationalTable rawToTruth = getRawToTruthTable(event,rawHitRelationsCollectionName);
        for(SiTrackerHitStrip1D hit:striphits){
            boolean keepHit = false;
            List<RawTrackerHit> rawhits = hit.getRawHits();
            for(RawTrackerHit rawhit:rawhits){
                for (SimTrackerHit simhit : simHits) {
                    if(rawToTruth.allFrom(rawhit).contains(simhit)){
                        keepHit = true;
                        break;
                    }
                }
                if(keepHit)
                    break;
            }
            if(keepHit){
                truthStripHits.add(hit);
            }
        }
        
        event.put(stripHitOutputCollectionName, truthStripHits, SiTrackerHitStrip1D.class, 0);
    }
}
