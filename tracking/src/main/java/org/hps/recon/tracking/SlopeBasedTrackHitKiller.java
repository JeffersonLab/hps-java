package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.event.Track;
import org.lcsim.lcio.LCIOUtil;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.util.Driver;
import org.lcsim.event.ReconstructedParticle;
/**
 *
 * @author mgraham created 2/2/20
 * Based on TrackHitKiller.java,
 * This driver will remove 1d strip clusters from the
 * "StripClusterer_SiTrackerHitStrip1D" (default)
 * collection based on a track-slope efficiency file (obtained from L1/no L1 WAB events)
 * ...this class removed BOTH strip clusters in the 3d helicaltrackhit
 * FOR HITS-ON-TRACKS ONLY ... occupancies will be ~ unchanged
 *
 * mg...this only works for L1 module at the moment
 */
public class SlopeBasedTrackHitKiller extends Driver {

    Set<String> ratioFiles = new HashSet<String>();
    private List<ModuleSlopeMap> _modulesToKill = new ArrayList<ModuleSlopeMap>();
    //List of Sensors
    private List<HpsSiSensor> sensors = null;
    private static final String SUBDETECTOR_NAME = "Tracker";
    private static Pattern layerPattern = Pattern.compile("L(\\d+)");
    private String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private final String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private final String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private final String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    //    private String trackCollectionName="MatchedTracks";
    private String trackCollectionName="GBLTracks";
    private boolean _debug = false;
    private double _scaleKillFactor=1.0;
    private List<TrackerHit> siClusters=new ArrayList<TrackerHit>();
    
    private Map<TrackerHit, Boolean> _siClustersAcceptMap = new HashMap<TrackerHit, Boolean>();
    private Map<TrackerHit, Boolean> _finalSiClustersAcceptMap = new HashMap<TrackerHit, Boolean>();

    private boolean _useSqrtKillFactor=true;
    private boolean _correctForDisplacement=true;

    public void setRatioFiles(String[] ratioNames) {
        System.out.println("Setting ratio files!!!  " + ratioNames[0]);
        this.ratioFiles = new HashSet<String>(Arrays.asList(ratioNames));
    }
    
    public void setDebug(boolean debug) {
        this._debug = debug;
    }

    public void setScaleKillFactor(double kill){
        this._scaleKillFactor=kill;
    }

    public void setUseSqrtKillFactor(boolean use){
        this._useSqrtKillFactor=use;
    }
      
    public void setCorrectForDisplacement(boolean setme){
        this._correctForDisplacement=setme;
    }

    public void setTrackCollectionName(String name){
        this.trackCollectionName=name;
    }

    public SlopeBasedTrackHitKiller() {
    }

    @Override
    public void detectorChanged(Detector detector) {
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                .getDetectorElement().findDescendants(HpsSiSensor.class);
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0)
            throw new RuntimeException("No sensors were found in this detector.");

        //parse the ratio names and register sensors to kill
        String delims = "-";// this will split strings between  "-"
        for (String ratioFile : ratioFiles) {
            System.out.println("SlopeBasedTrackHitKiller::Using this ratioFile:  " + ratioFile);
            int layer = -1;
            boolean top = false;
            boolean stereo = false;
            boolean slot = false;
            System.out.println("Parsing ratioFile = " + ratioFile);
            String[] tokens = ratioFile.split(delims);
            Matcher m = layerPattern.matcher(tokens[1]);
            if (m.find()) {
                layer = Integer.parseInt(m.group(1));
            } else {
                System.out.println("Couldn't find layer number!!!  " + ratioFile);
                continue;
            }
 
            System.out.println("SlopeBasedTrackHitKiller::Killing this:  "
                    + "layer = " + layer);
            this.registerModule(layer, ratioFile);            
        }
    }

    @Override
    public void process(EventHeader event) {
        //    System.out.println("In process of SVTHitKiller");
        //        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        
        if (event.hasItem(stripHitInputCollectionName))
            siClusters = (List<TrackerHit>) event.get(stripHitInputCollectionName);
        else {
            System.out.println("SlopeBasedTrackHitKiller::process No Input Collection Found?? " + stripHitInputCollectionName);
            return;
        }
        
        if (!event.hasCollection(LCRelation.class, helicalTrackHitRelationsCollectionName) || !event.hasCollection(LCRelation.class, rotatedHelicalTrackHitRelationsCollectionName))
            return;
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        List<Track> tracks = event.get(Track.class, trackCollectionName);                
        Map<Track,Double> trkNewSlopeMap=new HashMap<Track, Double>();

        if(_correctForDisplacement){
            if(!event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName)) {
                if (_debug)
                    System.out.println("SlopeBasedTrackHitKiller::process No Input Collection Found?? " + unconstrainedV0CandidatesColName);
                return;
            }
            List<ReconstructedParticle> unconstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
            if (_debug)
                System.out.println("This events has " + unconstrainedV0List.size() + " unconstrained V0s");
            trkNewSlopeMap=getUniqueTracksFromV0List(unconstrainedV0List);
            System.out.println("# of tracks in map = "+trkNewSlopeMap.size());

        }

        List<TrackerHit> tmpClusterList=new ArrayList<TrackerHit>(siClusters);
        int oldClusterListSize = siClusters.size();

        for (TrackerHit siCluster : siClusters) {
            for (ModuleSlopeMap modToKill : _modulesToKill){
                if(modToKill.getLayer() != layerToModule(((RawTrackerHit) siCluster.getRawHits().get(0)).getLayerNumber()))
                    continue;
                double lambda=-666;
                if(_correctForDisplacement){
                    lambda=adjustedSlopeFromMap(trkNewSlopeMap,siCluster,hitToStrips,hitToRotated);
                    //                    System.out.println("corrected lambda= "+lambda);
                }else{
                    Track trk=getTrackWithHit(tracks,siCluster,hitToStrips,hitToRotated);
                    if (trk==null)
                        continue;
                    lambda = trk.getTrackStates().get(0).getTanLambda(); 
                }

               
                double ratio = modToKill.getInefficiency(lambda);
                if(ratio == -666)
                    continue;
                if(_useSqrtKillFactor){
                    double eff=Math.sqrt(1-ratio);
                    ratio=1-eff;
                }
                double killFactor=ratio*modToKill.getScaleKillFactor();
                double random = Math.random(); //throw a random number to see if this hit should be rejected
                if(_debug)
                    System.out.println("ratio = "+ratio+"; killFactor = "+killFactor+"; random # = "+random);
                if (random < killFactor) {
                    //                    List<TrackerHit> stripsToKill=getTrackHitsPerModule(trk,hitToStrips,hitToRotated,modToKill.getLayer());
                    //                    boolean removed=tmpClusterList.removeAll(stripsToKill);
                    //if(_debug){
                    //    System.out.println("Removed hits? "+removed);
                    // }
                    //for(TrackerHit hit: stripsToKill){
                    //    if(_debug){
                    //       System.out.println("Removing clusters...");
                    //        System.out.println(hit.toString());
                    //        if(!tmpClusterList.contains(hit))
                    //            System.out.println("....Cluster not in tmpList");
                    //        if(!siClusters.contains(hit))
                    //           System.out.println("....Cluster not in siClusters");
                    //    }
                    //    tmpClusterList.remove(hit);                        
                    tmpClusterList.remove(siCluster);
                }
            }
        }
        

        if (_debug){
            System.out.println("New Cluster List Has " + tmpClusterList.size() + "; old List had " + oldClusterListSize);
            System.out.println("");
        }
        int flag = LCIOUtil.bitSet(0, 31, true); // Turn on 64-bit cell ID.        
        event.put(this.stripHitInputCollectionName, tmpClusterList, SiTrackerHitStrip1D.class, 0, toString());
        if(_debug)
            System.out.println("Clearing hit relational table caches");
        TrackUtils.clearCaches();

    }

    @Override
    public void endOfData() {
      
    }

    public void registerModule(int layer,  String ratioFile) {
        ModuleSlopeMap newModule = new ModuleSlopeMap(layer, ratioFile);
        newModule.setScaleKillFactor(this._scaleKillFactor);
        _modulesToKill.add(newModule);
    }

    //Return the HpsSiSensor for a given top/bottom track, layer, axial/stereo, and slot/hole
    private HpsSiSensor getSensor(int layer, boolean isTop, boolean isAxial, boolean isHole) {
        for (HpsSiSensor sensor : sensors) {
            int senselayer = (sensor.getLayerNumber() + 1) / 2;
            if (senselayer != layer)
                continue;
            if ((isTop && !sensor.isTopLayer()) || (!isTop && sensor.isTopLayer()))
                continue;
            if ((isAxial && !sensor.isAxial()) || (!isAxial && sensor.isAxial()))
                continue;
            if (layer < 4 && layer > 0)
                return sensor;
            else {
                if ((!sensor.getSide().matches("ELECTRON") && isHole) || (sensor.getSide().matches("ELECTRON") && !isHole))
                    continue;
                return sensor;
            }
        }
        return null;
    }

  
   

    public class ModuleSlopeMap {

        private List<Double> _lowEdge=new ArrayList<Double>();
        private List<Double> _upEdge=new ArrayList<Double>(); 
        private List<Double>  _inefficiency=new ArrayList<Double>();
        private int _layer=-666;
        private String _ratioFile="foo";
        private double _scaleKillFactor=0.5; // since we loop over all 1D clusters and there are 2 1D clusters per 3D hit, don't want to double the chance that the 3D hit is killed
        public ModuleSlopeMap(int layer, String ratioFile){
            _layer=layer; 
            _ratioFile=ratioFile;
            readRatioFile();
        }

        private void readRatioFile() {
            String infile = "/org/hps/recon/tracking/efficiencyCorrections/" + _ratioFile;
            InputStream inRatios = this.getClass().getResourceAsStream(infile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inRatios));
            String line;
            String delims = "[ ]+";// this will split strings between one or more spaces
            try {
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(delims);
                    System.out.println("loweredge = " + tokens[0] + "; upperedge = " + tokens[1]+"; ratio = " + tokens[1]);
                    _lowEdge.add(Double.parseDouble(tokens[0]));
                    _upEdge.add(Double.parseDouble(tokens[1]));
                    _inefficiency.add(Double.parseDouble(tokens[2]));
                }
            } catch (IOException ex) {
                Logger.getLogger(SlopeBasedTrackHitKiller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void setScaleKillFactor(double kill){
            System.out.println("Setting scaleKillFactor to "+kill);
            _scaleKillFactor=kill;
        }
        public double getScaleKillFactor(){
            return _scaleKillFactor;
        }
        public int getLayer(){
            return _layer;
        }

        public double getInefficiency(double slope) {
            for(int i=0;i<_lowEdge.size();i++){
                if(slope<_upEdge.get(i) && slope>_lowEdge.get(i))
                    return _inefficiency.get(i);
            }
            return -666;
        }
        
    }
    

    //Converts double array into Hep3Vector
    private Hep3Vector toHep3(double[] arr) {
        return new BasicHep3Vector(arr[0], arr[1], arr[2]);
    }
    
    private List<TrackerHit> getTrackHitsPerModule(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated, int layer){
        List<TrackerHit> lhits=new ArrayList<TrackerHit>();
        List<TrackerHit> hitsontrack=TrackUtils.getStripHits(track,hitToStrips,hitToRotated);
        System.out.println("hitsontrack size = " + hitsontrack.size());
        for (TrackerHit hit: hitsontrack) {
            int thislayer = ((RawTrackerHit) hit.getRawHits().get(0)).getLayerNumber();            
            int module = (thislayer-1) / 2 + 1;
            System.out.println("this layer ="+thislayer+"; module ="+module);
            if(module == layer && !(lhits.contains(hit))){// if it's layer of interest and it's not in list yet
                System.out.println("add hit to remove");
                lhits.add(hit);
            }            
        }                
        return lhits;
    }
    
    private List<TrackerHit> getTrackHitsPerLayer(List<Track> tracks, RelationalTable hitToStrips, RelationalTable hitToRotated, int layer){
        List<TrackerHit> lhits=new ArrayList<TrackerHit>();
        for(Track trk: tracks){
            List<TrackerHit> hitsontrack=TrackUtils.getStripHits(trk,hitToStrips,hitToRotated);
            for (TrackerHit hit: hitsontrack) {
                int thislayer = ((RawTrackerHit) hit.getRawHits().get(0)).getLayerNumber();
                int module = (thislayer-1) / 2 + 1;
                if(module == layer && !(lhits.contains(hit))){// if it's layer of interest and it's not in list yet
                    lhits.add(hit);
                }
            }
        }                
        return lhits;
    }

    /*
     * mg...7/5/20...
     * the strips in the SiCluster list are type: org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D
     * while from the track list (and presumably linked in HTH) are: org.hps.recon.tracking.SiTrackerHitStrip1D
     * and they apparently are actually separate in memory...
     * ...just checking if the SiCluster TrackerHit "hit" is in the hitsontrack collection doesn't work..
     * that's why I go back to the raw hits and compare these lists.  
     */
    private Track getTrackWithHit(List<Track> tracks, TrackerHit hit, RelationalTable hitToStrips, RelationalTable hitToRotated){
        for(Track trk: tracks){
            List<TrackerHit> hitsontrack=TrackUtils.getStripHits(trk,hitToStrips,hitToRotated);
            /*
              if(hitsontrack.contains(hit)){
                System.out.println("found a track with this hit "+hit.toString());
                return trk;
                } */
            for (TrackerHit hot: hitsontrack) {
                //                System.out.println(hit.toString()+" "+hot.toString());
                List<RawTrackerHit> rawTrkHits= (List<RawTrackerHit>)( hot.getRawHits());
                List<RawTrackerHit> rawHitHits= (List<RawTrackerHit>)( hit.getRawHits());
                if(rawHitHits.equals(rawTrkHits)){
                    if(_debug)
                        System.out.println("found match to SiCluster in track");
                    return trk;
                }
            }
        }                
        return null;
    }
    private int layerToModule(int layer){
        return (layer-1)/2+1;
    }
    
    private double correctSlope(double z,double slp){
        double d = 100;
        return (1 - z/d) * slp;
    }

    private Map<Track,Double> getUniqueTracksFromV0List(List<ReconstructedParticle> unconstrainedV0List){
        Map<Track,Double> trkmap=new HashMap<Track,Double>();
        for(ReconstructedParticle uncV0:unconstrainedV0List){
            double vz=uncV0.getStartVertex().getPosition().z();
            if(_debug)System.out.println("vertex z = "+vz);
            if(_debug)System.out.println("number of tracks = "+uncV0.getTracks().size());
            for(ReconstructedParticle  part: uncV0.getParticles()){
                Track trk=part.getTracks().get(0);
                double slope=trk.getTrackStates().get(0).getTanLambda();
                double newslope=correctSlope(vz,slope);
                System.out.println(slope+" ; "+newslope);
                if(trkmap.containsKey(trk)){
                    if(Math.abs(trkmap.get(trk))<Math.abs(newslope)){
                        if(_debug)System.out.println("Replacing new trk/slope pair "+newslope+"; old slope = "+slope);
                        //                        trkmap.replace(trk,newslope);  this only works in java 8
                        trkmap.remove(trk);
                        trkmap.put(trk,newslope);
                    }
                }else{
                    if(_debug)System.out.println("Putting new trk/slope pair "+newslope+"; old slope = "+slope);
                    trkmap.put(trk,newslope);
                }
            }
        }
        return trkmap;
    }

    private double  adjustedSlopeFromMap(Map<Track,Double> trkmap, TrackerHit hit, RelationalTable hitToStrips, RelationalTable hitToRotated){
        
        for (Map.Entry<Track,Double> entry : trkmap.entrySet())  {
            Track trk=entry.getKey();
            double newSlope=entry.getValue();
            List<TrackerHit> hitsontrack=TrackUtils.getStripHits(trk,hitToStrips,hitToRotated);
            /*
              if(hitsontrack.contains(hit)){
                System.out.println("found a track with this hit "+hit.toString());
                return trk;
                } */
            for (TrackerHit hot: hitsontrack) {
                //                System.out.println(hit.toString()+" "+hot.toString());
                List<RawTrackerHit> rawTrkHits= (List<RawTrackerHit>)( hot.getRawHits());
                List<RawTrackerHit> rawHitHits= (List<RawTrackerHit>)( hit.getRawHits());
                if(rawHitHits.equals(rawTrkHits)){
                    if(_debug)
                        System.out.println("found match to SiCluster in track");
                    return newSlope;
                }
            }
        }                
        return -666;
    }
    
}
