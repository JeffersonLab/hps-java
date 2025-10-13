package org.hps.recon.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import hep.physics.vec.BasicHep3Vector;

public class TrackTruthMatcher {

    private MCParticle _mcp = null;
    private int _nhits;
    private int _ngoodhits;
    private int _nbadhits;
    private double _purity;
    private Map<MCParticle, int[]> mcParticleStripHits = new HashMap<MCParticle, int[]>();
    private Map<MCParticle, Set<Integer>> mcParticleHitsByLayer = new HashMap<MCParticle, Set<Integer>>();
    private Map<Integer, List<RawTrackerHit>> _stripHitsOnLayerMap = new HashMap<Integer, List<RawTrackerHit>>();
    private Map<Integer, Set<MCParticle>> _mcpsOnLayerMap = new HashMap<Integer, Set<MCParticle>>();
    private Set<Integer> _layersOnTrack = new HashSet<Integer>();
    private Map<RawTrackerHit, List<MCParticle>> _stripHitsToMCPsMap = new HashMap<RawTrackerHit, List<MCParticle>>();
    private int _totalLayers=14;
    
    public TrackTruthMatcher(Track track, RelationalTable rawtomc){
        doAnalysis(track, rawtomc);
    }

    //Purity cut is a number less than 1
    //enforces minimum ratio of bestMCPLayerHits / totalNLayerHits
    //For example: 10 layer hits / 12 total layer hits is purity of 0.83
    public TrackTruthMatcher(Track track, RelationalTable rawtomc, double purityCut){
        doAnalysis(track, rawtomc, purityCut);
    }

    public TrackTruthMatcher(Track track, RelationalTable rawtomc, double purityCut, int nHitsRequired){
        doAnalysis(track, rawtomc, purityCut, nHitsRequired);
    }

    private void doAnalysis(Track track, RelationalTable rawtomc){
        getMCPsOnTrack(track, rawtomc);
        matchTrackToMCP(this.mcParticleHitsByLayer);
    }

    private void doAnalysis(Track track, RelationalTable rawtomc, double purityCut){
        getMCPsOnTrack(track, rawtomc);
        matchTrackToMCP(this.mcParticleHitsByLayer);
        if( _purity < purityCut)
            this._mcp = null;
    }

    private void doAnalysis(Track track, RelationalTable rawtomc, double purityCut, int nHitsRequired){
        getMCPsOnTrack(track, rawtomc);
        matchTrackToMCP(this.mcParticleHitsByLayer);
        if( _purity < purityCut ||  _ngoodhits < nHitsRequired)
            this._mcp = null;
    }

    //Return MCParticle matched to Track
    public MCParticle getMCParticle(){
        return this._mcp;
    }

    //Return number of Layers with hits on track 
    public int getNHits() {
        return _nhits;
    }
    
    //Return number of Layer hits for best MCP
    public int getNGoodHits() {
        return _ngoodhits;
    } 
    
    //Return number of Layer hits from other MCPs (not best MCP)
    public int getNBadHits() {
        return _nbadhits;
    }
    
    //Return _ngoodhits / _nhits
    public double getPurity() {
        return _purity;
    }

    //Return Map of MCParticles and the layers they leave hits
    public Map<MCParticle, Set<Integer>> getLayerHitsForAllMCPs(){
        return this.mcParticleHitsByLayer;
    }

    //Return Map of number of strip hits left by all MCParticles
    public Map<MCParticle, int[]> getNStripHitsForAllMCPs(){
        return this.mcParticleStripHits;
    }

    //Return number of Strip Hits on layer
    public List<RawTrackerHit> getStripHitsOnLayer(int layer){
        return this._stripHitsOnLayerMap.get(layer);
    }

    //Return number of MCPs that hit layer
    public Set<MCParticle> getMCPsOnLayer(int layer){
        return this._mcpsOnLayerMap.get(layer);
    }

    //Return layers that Track leaves hits on
    public Set<Integer> getLayersOnTrack(){
        return this._layersOnTrack;
    }

    //Return MCParticles that left RawTrackerHit
    public List<MCParticle> getMCPsOnRawTrackerHit(RawTrackerHit rawhit){
        return this._stripHitsToMCPsMap.get(rawhit);
    }

    // getGoodHitLayers and getCorrectlyMissingLayers
    // give layers where tracking is doing the "right thing"
    // ...following the MC particle track
    public Set<Integer> getGoodHitLayers(){
        return getLayerHitsForAllMCPs().get(this._mcp);
    }

    public Set<Integer> getCorrectlyMissingLayers(List<SimTrackerHit> simHits){
	Set<Integer> correctMissingLayers = new HashSet<Integer>();
	Set<Integer> trackLayers = getLayersOnTrack();
	Set<Integer> mcpLayers= getLayersHitByMCP(this._mcp, simHits);	
	for(Integer i=1; i<=_totalLayers; i++){
	    if(!trackLayers.contains(i) && !mcpLayers.contains(i))
		correctMissingLayers.add(i);
	}
	return correctMissingLayers;
    }
    
    //getBadLayerHits, combination of "wrongHit" and "noMCP" ... see below
    public Set<Integer> getBadHitLayers(){
        Set<Integer> badLayers = new HashSet<Integer>();
        Set<Integer> trackLayers = getLayersOnTrack();
        Set<Integer> goodLayers = getLayerHitsForAllMCPs().get(this._mcp);
        for(Integer layer : trackLayers){
            if(!goodLayers.contains(layer)){
                badLayers.add(layer);
            }
        }
        return badLayers;
    }
    
    //two ways to get a "bad" hit on track:
    //getWrongHitLayers:  MC particle left hit on the layer but we picked a different one
    //getNonMCPHitLayers:  no MC particle hit on layer but we found a hit anyway    
    public Set<Integer> getWrongHitLayers(List<SimTrackerHit> simHits){
	Set<Integer> wrongLayers = new HashSet<Integer>();
	Set<Integer> trackLayers = getLayersOnTrack();
	Set<Integer> goodLayers = getGoodHitLayers(); //this checks if the hit on track is from main mcp
	Set<Integer> mcpLayers= getLayersHitByMCP(this._mcp, simHits);
	for(Integer i=1; i<=_totalLayers; i++){
	    if(trackLayers.contains(i) && !goodLayers.contains(i) && mcpLayers.contains(i))
		wrongLayers.add(i);
	}
	return wrongLayers;
    }

    public Set<Integer> getNonMCPHitLayers(List<SimTrackerHit> simHits){
	Set<Integer> extraLayers = new HashSet<Integer>();
	Set<Integer> trackLayers = getLayersOnTrack();
	Set<Integer> goodLayers = getGoodHitLayers(); //this checks if the hit on track is from main mcp
	Set<Integer> mcpLayers= getLayersHitByMCP(this._mcp, simHits);
	//regarding (!goodLayers.contains(i) && !mcpLayers.contains(i))):	
	//if the mcp hit isn't in a layer, it also can't be in goodLayers
	//but keep it this way just to make obvious 
	for(Integer i=1; i<=_totalLayers; i++){
	    if(trackLayers.contains(i) && !goodLayers.contains(i) && !mcpLayers.contains(i))
		extraLayers.add(i);
	}
	return extraLayers;
    }

    //getMissedHitLayers account for layers we have an MC particle hit, but no hit on track
    //this doesn't show up in either "goodLayers" or "badLayers" because that loops over hits-on-track
    public Set<Integer> getMissedHitLayers(List<SimTrackerHit> simHits){
	Set<Integer> missedLayers = new HashSet<Integer>();
	Set<Integer> trackLayers = getLayersOnTrack();
	Set<Integer> goodLayers = getGoodHitLayers(); //this checks if the hit on track is from main mcp
	Set<Integer> mcpLayers= getLayersHitByMCP(this._mcp, simHits);
	for(Integer i=1; i<=_totalLayers; i++){
	    if(!trackLayers.contains(i) && mcpLayers.contains(i))
		missedLayers.add(i);
	}
	return missedLayers;
    }

    
    public Set<Integer> getMCPHitLayers(List<SimTrackerHit> simHits){
	return getLayersHitByMCP(this._mcp, simHits);
    }


    public void getMCPsOnTrack(Track track, RelationalTable rawtomc){

        double trackPmag = new BasicHep3Vector(track.getTrackStates().get(0).getMomentum()).magnitude();
        int _nhits = track.getTrackerHits().size();

        //loop over tracker hits
        for (TrackerHit hit: track.getTrackerHits()) {
            //Get layer by layer truth information
            Map<Integer, Map<RawTrackerHit, List<MCParticle>>> hitTruth_byLayer = getMCParticlesOnTrackerHit(hit, rawtomc);
            //loop over each layer that this trackerhit exists on
            for(Map.Entry<Integer, Map<RawTrackerHit, List<MCParticle>>> entry : hitTruth_byLayer.entrySet()){
                int layer = entry.getKey();
                this._layersOnTrack.add(layer);
                //loop over each strip hit that was clustered into this layer
                //hit
                List<RawTrackerHit> rawhitsOnLayer = new ArrayList<RawTrackerHit>(); 
                Set<MCParticle> mcpsOnLayer = new HashSet<MCParticle>();
                for(Map.Entry<RawTrackerHit, List<MCParticle>> subentry : entry.getValue().entrySet()){
                    rawhitsOnLayer.add(subentry.getKey());
                    //loop over each unique MCParticle that contributed to this
                    //strip hit
                    for(MCParticle particle : subentry.getValue()){
                        //Only count a MCParticle hit once per layer
                        Set<Integer> hitsOnLayer = new HashSet<Integer>();
                        hitsOnLayer.add(entry.getKey());
                        mcpsOnLayer.add(particle);
                        if(!mcParticleHitsByLayer.containsKey(particle)){
                            mcParticleHitsByLayer.put(particle, hitsOnLayer);
                        }
                        else{
                            Set<Integer> tmp = mcParticleHitsByLayer.get(particle);
                            tmp.add(entry.getKey());
                            mcParticleHitsByLayer.put(particle, tmp);
                        }
                        if(!mcParticleStripHits.containsKey(particle)){
                            mcParticleStripHits.put(particle, new int[1]);
                            mcParticleStripHits.get(particle)[0] = 0;
                        }
                        mcParticleStripHits.get(particle)[0]++;
                    }
                }
                this._mcpsOnLayerMap.put(layer, mcpsOnLayer);
                this._stripHitsOnLayerMap.put(layer, rawhitsOnLayer);
            }
        }
    }

    //match track to MCP by matching to MCP that leaves hits on the most layers
    private void matchTrackToMCP(Map<MCParticle, Set<Integer>> mcParticleHitsByLayer){

        MCParticle bestMCP = null;
        int bestNhits = 0;

        for(Map.Entry<MCParticle, Set<Integer>> entry : mcParticleHitsByLayer.entrySet()){
            int nhits = entry.getValue().size();
            if(nhits > bestNhits){
                bestNhits = nhits;
                bestMCP = entry.getKey();
            }
        }

        this._purity = (double) bestNhits/ (double) _layersOnTrack.size();
        this._mcp = bestMCP;
        this._ngoodhits = bestNhits;
        this._nbadhits = _layersOnTrack.size() - bestNhits;
        this._nhits = _layersOnTrack.size();
    }

    public Map<Integer, Map<RawTrackerHit, List<MCParticle>>> getMCParticlesOnTrackerHit(TrackerHit hit, RelationalTable rawtomc){

        //Get all 1d Strip Hits on TrackerHit
        List<RawTrackerHit> rawhits = hit.getRawHits();
        //GBL TrackerHits are 3d clusters of 2d hits from axial and stereo pair
        //Separate these 3d clusters into their independent layer 2d constituents
        Set<Integer> layers = new HashSet<Integer>();
        //Define which layers this TrackerHit is composed of
        for(RawTrackerHit rawhit : rawhits){
            int layer = rawhit.getLayerNumber();
            layers.add(layer);
        }

        //Map StripHits to their respective layers
        Map<Integer, Map<RawTrackerHit, List<MCParticle>>> rawHitsMCPMap_byLayer = new HashMap<Integer,Map<RawTrackerHit, List<MCParticle>>>();
        //Loop over Striphits, layer by layer
        for(Integer layer : layers){
            Map<RawTrackerHit, List<MCParticle>> rawHitsMCPMap = new HashMap<RawTrackerHit, List<MCParticle>>();
            for(RawTrackerHit rawhit : rawhits){
                if(rawhit.getLayerNumber() != layer)
                    continue;
                //Get list of unique MCPs that make up this Strip hit
                List<MCParticle> rawhitMCPs = getMCParticlesOnRawTrackerHit(rawhit, rawtomc);
                rawHitsMCPMap.put(rawhit,rawhitMCPs);
            }
            rawHitsMCPMap_byLayer.put(layer, rawHitsMCPMap);
        }

        return rawHitsMCPMap_byLayer;
    }

    public List<MCParticle> getMCParticlesOnRawTrackerHit(RawTrackerHit rawhit, RelationalTable rawtomc){

        Set<MCParticle> mcps = new HashSet<MCParticle>();
        List<MCParticle> mcpList = new ArrayList<MCParticle>();
        Set<SimTrackerHit> simhits = rawtomc.allFrom(rawhit);
        //loop over simhits on rawhit
        for(SimTrackerHit simhit : simhits){
            //get mcp that left simhit
            MCParticle particle = simhit.getMCParticle();
            int pdgid = particle.getPDGID();
            //only add electron or positron mcps
            if(Math.abs(pdgid) != 11)
                continue;
            /*
            if(particle.getOriginZ() > 0)
                continue;
                */
            mcps.add(particle);
        }

        //Convert set to list, so that list contains only 1 entry per MCP
        mcpList.addAll(mcps);
        this._stripHitsToMCPsMap.put(rawhit, mcpList);

        return mcpList;
    }

    public Set<Integer> getLayersHitByMCP(MCParticle mcp, List<SimTrackerHit> simhits){
	Set<Integer> layerhitsMap = new HashSet<Integer>();
	for(SimTrackerHit simhit : simhits){
	    MCParticle simhitmcp = simhit.getMCParticle();
	    if(simhitmcp == mcp){
		int layer = simhit.getLayer();
		if(!layerhitsMap.contains(layer))
		    layerhitsMap.add(layer); 	       
	    }
	}		
	return  layerhitsMap;
    }
    
}



