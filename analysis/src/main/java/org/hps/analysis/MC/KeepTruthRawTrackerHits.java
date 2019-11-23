package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;

/**
 *
 * @author mrsolt
 * This driver keeps all truth hits that correspond to a track's 1D strip hits
 */
public class KeepTruthRawTrackerHits extends Driver {

    //Collection Names
    private String simHitCollectionName = "TrackerHits_truth";
    private String rawHitRelationsCollectionName = "SVTTrueHitRelations";
    
    private String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String stripHitOutputCollectionName = "StripClusterer_SiTrackerHitStrip1D_truth";

    public KeepTruthRawTrackerHits() {
    
    }
    
    public void setSimHitCollectionName(String simHitCollectionName) {
        this.simHitCollectionName = simHitCollectionName;
    }

    public void setRawHitRelationsCollectionName(String rawHitRelationsCollectionName) {
        this.rawHitRelationsCollectionName = rawHitRelationsCollectionName;
    }
    
    
    //Build relation table for truth track hits
    public static RelationalTable getRawToTruthTable(EventHeader event,String RawHitRelationsCollectionName) {
        RelationalTable hitToTruth = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> hitrelations = event.get(LCRelation.class, RawHitRelationsCollectionName);
        for (LCRelation relation : hitrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null){
                hitToTruth.add(relation.getFrom(), relation.getTo());
            }
        return hitToTruth;
    }
    
    protected void process(EventHeader event) {
        List<TrackerHit> striphits = event.get(TrackerHit.class, stripHitInputCollectionName);
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, simHitCollectionName);
        List<SiTrackerHitStrip1D> truthStripHits = new ArrayList<SiTrackerHitStrip1D>();
        RelationalTable rawToTruth = getRawToTruthTable(event,rawHitRelationsCollectionName);
        //Loop over all 1D strip hits
        for(TrackerHit hit:striphits){
            boolean keepHit = false;
            List<RawTrackerHit> rawhits = hit.getRawHits();
            //Loop over raw hits on strip hits
            for(RawTrackerHit rawhit:rawhits){
                //Loop over all sim tracker hits on raw hit
                for (SimTrackerHit simhit : simHits) {
                    //Check to see if the raw hit contains a sim hit from the list of truth hits
                    //If so, keep the hit and break the loop
                    if(rawToTruth.allFrom(rawhit).contains(simhit)){
                        keepHit = true;
                        break;
                    }
                }
                if(keepHit)
                    break;
            }
            if(keepHit){
                truthStripHits.add(new SiTrackerHitStrip1D(hit));
            }
        }
        event.put(stripHitOutputCollectionName, truthStripHits, SiTrackerHitStrip1D.class, 0);
    }
}