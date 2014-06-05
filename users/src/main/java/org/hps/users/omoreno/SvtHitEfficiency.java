package org.hps.users.omoreno;


//--- java ---//
import hep.aida.ICloud2D;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
import org.hps.conditions.deprecated.SvtUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackerHitUtils;
import org.hps.util.AIDAFrame;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
//--- lcsim ---//
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
//--- hps-java ---//

public class SvtHitEfficiency extends Driver {

	private AIDA aida;
    private List<IHistogram1D> histos1D = new ArrayList<IHistogram1D>();
    private List<IHistogram2D> histos2D = new ArrayList<IHistogram2D>();
    private List<IPlotter> plotters = new ArrayList<IPlotter>();
    private Map<SiSensor, Map<Integer, Hep3Vector>> stripPositions = new HashMap<SiSensor, Map<Integer, Hep3Vector>>(); 
    TrackerHitUtils trackerHitUtils = new TrackerHitUtils();
    
    boolean debug = false;
    boolean ecalClusterTrackMatch = false;
    
    // Plot flags
    boolean enableMomentumPlots = true;
    boolean enableChiSquaredPlots = true;
    boolean enableTrackPositionPlots = true;
    boolean maskBadChannels = false;
    
    int plotterIndex = 0;
    
    double numberOfTopTracks = 0;
    double numberOfBottomTracks = 0;
    double numberOfTopTracksWithHitOnMissingLayer = 0; 
    double numberOfBottomTracksWithHitOnMissingLayer = 0;
    double[] topTracksPerMissingLayer = new double[5];
    double[] topTracksWithHitOnMissingLayer = new double[5];
    double[] bottomTracksPerMissingLayer = new double[5];
    double[] bottomTracksWithHitOnMissingLayer = new double[5];
    
    Hep3Vector trackPos = null;
    Hep3Vector frontTrackPos = null;
    Hep3Vector rearTrackPos = null;
    
    // Collections
    private String trackCollectionName = "MatchedTracks";
    private String stereoHitCollectionName = "HelicalTrackHits";
    private String ecalClustersCollectionName = "EcalClusters";
   
    // Constants
    public static final double SENSOR_LENGTH = 98.33; // mm
    public static final double SENSOR_WIDTH = 38.3399; // mm

    /**
     * Default Ctor
     */
    public SvtHitEfficiency(){
    }
    
    /**
     *  Enable/Disable debug 
     */
    public void  setDebug(boolean debug){
        this.debug = debug;
    }
    
    /**
     * Enable/Disable masking of bad channels
     */
    public void setMaskBadChannels(boolean maskBadChannels){
    	this.maskBadChannels = maskBadChannels; 
    }
    
    public void detectorChanged(Detector detector){
    	
        // setup AIDA
        aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        
        String title = null;
        IHistogram2D histo2D = null;
        IHistogram1D histo1D = null;
        ICloud2D cloud2D = null;

        // Create a Map from sensor to bad channels and from bad channels to
        // strip position
        for(ChargeCarrier carrier : ChargeCarrier.values()){
            for(SiSensor sensor : SvtUtils.getInstance().getSensors()){ 
                if(sensor.hasElectrodesOnSide(carrier)){ 
                    stripPositions.put(sensor, new HashMap<Integer, Hep3Vector>());
                    SiStrips strips = (SiStrips) sensor.getReadoutElectrodes(carrier);     
                    ITransform3D parentToLocal = sensor.getReadoutElectrodes(carrier).getParentToLocal();
                    ITransform3D localToGlobal = sensor.getReadoutElectrodes(carrier).getLocalToGlobal();
                    for(int physicalChannel = 0; physicalChannel < 640; physicalChannel++){
                        Hep3Vector localStripPosition = strips.getCellPosition(physicalChannel);
                        Hep3Vector stripPosition = parentToLocal.transformed(localStripPosition);
                        Hep3Vector globalStripPosition = localToGlobal.transformed(stripPosition);
                        //this.printDebug(SvtUtils.getInstance().getDescription(sensor) + " : Channel " + physicalChannel + " : Local Strip Position " + localStripPosition.toString());
                        //this.printDebug(SvtUtils.getInstance().getDescription(sensor) + " : Channel " + physicalChannel + " : Strip Position " + stripPosition.toString());
                        //this.printDebug(SvtUtils.getInstance().getDescription(sensor) + " : Channel " + physicalChannel + " : Strip Position " + globalStripPosition.toString());
                        stripPositions.get(sensor).put(physicalChannel, stripPosition);
                    }
                }
            }
        }
        
        //--- Momentum Plots ---//
        //----------------------//
        if(enableMomentumPlots){
        	plotters.add(PlotUtils.setupPlotter("Track Momentum", 0, 0));
        	title = "Track Momentum - All Tracks";
        	histo1D = aida.histogram1D(title, 50, 0, 5);
        	PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 0, "Momentum [GeV]", histo1D);
        	title = "Track Momentum - Tracks Within Acceptance";
        	histo1D = aida.histogram1D(title, 50, 0, 5);
        	plotters.get(plotterIndex).region(0).plot(histo1D);
        	title = "Track Momentum - Tracks With All Layers Hit";
        	histo1D = aida.histogram1D(title, 50, 0, 5);
        	plotters.get(plotterIndex).region(0).plot(histo1D);
        	plotterIndex++;
        }
        
        //--- Track Fit Chi Squared ---//
        //-----------------------------//
        if(enableChiSquaredPlots){
        	plotters.add(PlotUtils.setupPlotter("Track Chi Squared", 0, 0));
        	title = "Chi Squared - All Tracks";
        	histo1D = aida.histogram1D(title, 50, 0, 50);
        	PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 0, "Chi Squared", histo1D);
        	title = "Chi Squared - Tracks Within Acceptance";
        	histo1D = aida.histogram1D(title, 50, 0, 50);
        	plotters.get(plotterIndex).region(0).plot(histo1D);
        	title = "Chi Squared - Tracks With All Layers Hit";
        	histo1D = aida.histogram1D(title, 50, 0, 50);
        	plotters.get(plotterIndex).region(0).plot(histo1D);
            plotterIndex++;
        }
                
        //--- Track Position Plots ---//
        //----------------------------//
        if(enableTrackPositionPlots){
        	int layerNumber = 1; 
        	SiSensor sensor = null;
        	IPlotterStyle style = aida.analysisFactory().createPlotterFactory().createPlotterStyle();
            for(int index = 1; index < 6; index++){
                plotters.add(PlotUtils.setupPlotter("Track Position - Layer " + index, 2, 3));
            	title = "Track Position - Layer " + index + " - Tracks Within Acceptance";
                cloud2D = aida.cloud2D(title);
                PlotUtils.setup2DRegion(plotters.get(plotterIndex), title, 0, "x [mm]", "y [mm]", cloud2D, style);
                title = "Track Position - Layer " + index + " - Tracks With All Layers Hit";
                cloud2D = aida.cloud2D(title);
                plotters.get(plotterIndex).region(0).plot(cloud2D, style);
                title = "Track Position - Layer " + index + " - Difference";
                cloud2D = aida.cloud2D(title);
                PlotUtils.setup2DRegion(plotters.get(plotterIndex), title, 1, "x [mm]", "y [mm]", cloud2D, style);
                sensor = SvtUtils.getInstance().getBottomSensor(layerNumber, 0);
                title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
                histo1D = aida.histogram1D(title, 640, 0, 639);
                histos1D.add(histo1D);
                PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 2, "Channel #", histo1D);
                sensor = SvtUtils.getInstance().getTopSensor(layerNumber, 0);
                title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
                histo1D = aida.histogram1D(title, 640, 0, 639);
                histos1D.add(histo1D);
                PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 4, "Channel #", histo1D);
                layerNumber++;
                sensor = SvtUtils.getInstance().getBottomSensor(layerNumber, 0);
                title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
                histo1D = aida.histogram1D(title, 640, 0, 639);
                histos1D.add(histo1D);
                PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 3, "Channel #", histo1D);
                sensor = SvtUtils.getInstance().getTopSensor(layerNumber, 0);
                title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
                histo1D = aida.histogram1D(title, 640, 0, 639);
                histos1D.add(histo1D);
                PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 5, "Channel #", histo1D);
                layerNumber++;
                plotterIndex++;
            }
        }
        
        for (IPlotter plotter : plotters) {
            plotter.show();
        }
    }

    /**
     * .
     */
    private Hep3Vector getStripPosition(SiSensor sensor, int physicalChannel){ 
        return stripPositions.get(sensor).get(physicalChannel);
    }

    /**
     * Print a debug message if they are enabled.
     * 
     * @param debugMessage : message to be printed
     */ 
    private void printDebug(String debugMessage){
        if(debug){
            System.out.println(this.getClass().getSimpleName() + ": " + debugMessage);
        }
    }
    
    public void process(EventHeader event){
        
        // If the event does not have tracks, skip it
        if(!event.hasCollection(Track.class, trackCollectionName)) return;
    
        // Get the list of tracks from the event
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        
        if(tracks.size() >= 2 ) return;
        
        // Get the list of Ecal clusters from the event
        List<HPSEcalCluster> ecalClusters = event.get(HPSEcalCluster.class, ecalClustersCollectionName);

        for(Track track : tracks){
        	
            ecalClusterTrackMatch = false;
        	
            // Check if there is an Ecal cluster in the same detector volume as the track
        	/*for(HPSEcalCluster ecalCluster : ecalClusters){
        		if(ecalCluster.getPosition()[1] > 0 && trkUtil.getZ0() > 0){
        			ecalClusterTrackMatch = true;
        			break;
        		}
        		else if(ecalCluster.getPosition()[1] < 0 && trkUtil.getZ0() < 0){
        			ecalClusterTrackMatch = true;
        			break;
        		}
        	}*/
        	
        	/*
        	if(!ecalClusterTrackMatch){
        		if(debug) System.out.println(this.getClass().getSimpleName() + ": No matching Ecal cluster found");
        		continue;
        	}*/
            
            // Check that the track is associated with four hits only. This should
            // be the case since the strategy is only requiring four hits to fit
            // a track and is not requiring an extending layer
            if(track.getTrackerHits().size() != 4){
                System.out.println(this.getClass().getSimpleName() + ": This track is composed of " + track.getTrackerHits().size() + ". Skipping event..." );
                continue;
            }
            
        	// Apply a momentum cut? Probably ...
        	// Calculate the track momentum
        	double momentum = Math.sqrt(track.getPX()*track.getPX() + track.getPY()*track.getPY() + track.getPZ()*track.getPZ());
        	if(momentum < 0.5 /* GeV */) continue;
        	if(enableMomentumPlots)
        		aida.histogram1D("Track Momentum - All Tracks").fill(momentum);
            
        	if(enableChiSquaredPlots)
        		aida.histogram1D("Chi Squared - All Tracks").fill(track.getChi2());
        	
            // Find which layer is not being used to fit the track
            int layer = this.findMissingFitLayer(track.getTrackerHits());
        	int arrayPosition = (layer - 1)/2;
            
        	// Find if the track is within the acceptance of the layer not being used in
            // the fit
            if(!isWithinAcceptance(track, layer)) continue;
            if(TrackUtils.getZ0(track) > 0){
            	numberOfTopTracks++;
            	topTracksPerMissingLayer[arrayPosition]++;
            } else {
            	numberOfBottomTracks++;
            	bottomTracksPerMissingLayer[arrayPosition]++;
            }

            if(enableMomentumPlots)
    			aida.histogram1D("Track Momentum - Tracks Within Acceptance").fill(momentum);
    		if(enableChiSquaredPlots)
    			aida.histogram1D("Chi Squared - Tracks Within Acceptance").fill(track.getChi2());
    		
            // Find if there is a stereo hit within that layer
            List<HelicalTrackHit> stereoHits = event.get(HelicalTrackHit.class, stereoHitCollectionName);
            for(HelicalTrackHit stereoHit : stereoHits){
            	if(layer == stereoHit.Layer()){
            		if(debug) System.out.println(this.getClass().getSimpleName() + ": Track has five layers hit");
            		if(TrackUtils.getZ0(track) > 0){
            			numberOfTopTracksWithHitOnMissingLayer++;
            			topTracksWithHitOnMissingLayer[arrayPosition]++;
            		} else {
            			numberOfBottomTracksWithHitOnMissingLayer++;
            			bottomTracksWithHitOnMissingLayer[arrayPosition]++;
            		}
            		if(enableMomentumPlots)
            			aida.histogram1D("Track Momentum - Tracks With All Layers Hit").fill(momentum);
            		if(enableChiSquaredPlots)
            			aida.histogram1D("Chi Squared - Tracks With All Layers Hit").fill(track.getChi2());
            		
            		return;
            	}
            }
            
	      	int layerNumber = (layer - 1)/2 + 1;
    		if(enableTrackPositionPlots){
            	String title = "Track Position - Layer " + layerNumber + " - Difference";
            	//aida.histogram2D(title).fill(trackPos.y(), trackPos.z());
                aida.cloud2D(title).fill(frontTrackPos.y(), frontTrackPos.z());

                title = "Track Position - Layer " + layerNumber + " - Tracks With All Layers Hit";
                //aida.histogram2D(title).fill(trackPos.y(), trackPos.z());
                aida.cloud2D(title).fill(frontTrackPos.y(), frontTrackPos.z());
            }
    		
    		List<SiSensor> sensors = new ArrayList<SiSensor>();
    		if(TrackUtils.getZ0(track) > 0){
    			sensors.add(SvtUtils.getInstance().getTopSensor(layer, 0));
        		sensors.add(SvtUtils.getInstance().getTopSensor(layer+1, 0));
    		} else { 
    			sensors.add(SvtUtils.getInstance().getBottomSensor(layer, 0));
        		sensors.add(SvtUtils.getInstance().getBottomSensor(layer+1, 0));
    		}
    		aida.histogram1D(SvtUtils.getInstance().getDescription(sensors.get(0)) + " - Occupancy").fill(this.findIntersectingChannel(frontTrackPos, sensors.get(0)));
            aida.histogram1D(SvtUtils.getInstance().getDescription(sensors.get(1)) + " - Occupancy").fill(this.findIntersectingChannel(rearTrackPos, sensors.get(1)));
    		
           if(debug)
        	   System.out.println(this.getClass().getSimpleName() + ": Stereo hit was not found.");
        }
    }
    
    private int findMissingFitLayer(List<TrackerHit> trkHits){
        int[] layer = new int[5];
        for(TrackerHit trkHit : trkHits){
            HelicalTrackHit stereoHit = (HelicalTrackHit) trkHit;
            int arrayPosition = (stereoHit.Layer() - 1)/2;
            layer[arrayPosition]++;
        }
        
        for(int index = 0; index < layer.length; index++){
            if(layer[index] == 0) return (2*index + 1);
        }
        
        return -1;
    }
    
    private boolean isWithinAcceptance(Track track, int layer){
        
        
        
        List<SiSensor> sensors = new ArrayList<SiSensor>();
        if(TrackUtils.getZ0(track) > 0){
           sensors.add(SvtUtils.getInstance().getTopSensor(layer, 0));
           sensors.add(SvtUtils.getInstance().getTopSensor(layer + 1, 0));
        } else {
            sensors.add(SvtUtils.getInstance().getBottomSensor(layer, 0));
            sensors.add(SvtUtils.getInstance().getBottomSensor(layer + 1, 0));
        }
        
        Hep3Vector frontSensorPos = sensors.get(0).getGeometry().getPosition();
        Hep3Vector rearSensorPos = sensors.get(1).getGeometry().getPosition();
        
        this.frontTrackPos = TrackUtils.extrapolateTrack(track,frontSensorPos.z());
        this.rearTrackPos = TrackUtils.extrapolateTrack(track,rearSensorPos.z());
        
        if(this.sensorContainsTrack(frontTrackPos, sensors.get(0)) && this.sensorContainsTrack(rearTrackPos, sensors.get(1))){
//        	if(this.sensorContainsTrack(trackPos, sensor))
	      	if(enableTrackPositionPlots){
	      		int layerNumber = (layer - 1)/2 + 1;
	      		String title = "Track Position - Layer " + layerNumber + " - Tracks Within Acceptance";
	      		//aida.histogram2D(title).fill(trackPos.y(), trackPos.z());
	      		aida.cloud2D(title).fill(frontTrackPos.y(), frontTrackPos.z());
	      	}
        	return true;
        } 
        
        
        return false;
    }
    
    /**
     * 
     */
    public int findIntersectingChannel(Hep3Vector trackPosition, SiSensor sensor){
		
    	//--- Check that the track doesn't pass through a region of bad channels ---//
		//--------------------------------------------------------------------------//
    
		//Rotate the track position to the JLab coordinate system
		this.printDebug("Track position in tracking frame: " + trackPosition.toString());
		Hep3Vector trackPositionDet = VecOp.mult(VecOp.inverse(this.trackerHitUtils.detToTrackRotationMatrix()), trackPosition);
		this.printDebug("Track position in JLab frame " + trackPositionDet.toString());
		// Rotate the track to the sensor coordinates
		ITransform3D globalToLocal = sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getGlobalToLocal();
		globalToLocal.transform(trackPositionDet);
		this.printDebug("Track position in sensor electrodes frame " + trackPositionDet.toString());

		// Find the closest channel to the track position
		double deltaY = Double.MAX_VALUE;
		int intersectingChannel = 0;
		for(int physicalChannel= 0; physicalChannel < 639; physicalChannel++){ 
			/*this.printDebug(SvtUtils.getInstance().getDescription(sensor) + " : Channel " + physicalChannel 
                	+ " : Strip Position " + stripPositions.get(sensor).get(physicalChannel));
        	this.printDebug(SvtUtils.getInstance().getDescription(sensor) + ": Channel " + physicalChannel
                	+ " : Delta Y: " + Math.abs(trackPositionDet.x() - stripPositions.get(sensor).get(physicalChannel).x()));*/
			if(Math.abs(trackPositionDet.x() - stripPositions.get(sensor).get(physicalChannel).x()) < deltaY ){
				deltaY = Math.abs(trackPositionDet.x() - stripPositions.get(sensor).get(physicalChannel).x()); 
				intersectingChannel = physicalChannel;
			}
		}
    
		this.printDebug(SvtUtils.getInstance().getDescription(sensor) + ": Track intersects physical channel " + intersectingChannel);
		
		return intersectingChannel;
    }

    /**
     *
     */
    public boolean sensorContainsTrack(Hep3Vector trackPosition, SiSensor sensor){

    	
    	if(maskBadChannels){
    		int intersectingChannel = this.findIntersectingChannel(trackPosition, sensor);
    		if(intersectingChannel == 0 || intersectingChannel == 638) return false;
    	    
    		if(HPSSVTCalibrationConstants.isBadChannel(sensor, intersectingChannel) 
    				|| HPSSVTCalibrationConstants.isBadChannel(sensor, intersectingChannel+1) 
    				|| HPSSVTCalibrationConstants.isBadChannel(sensor, intersectingChannel-1)){
    			this.printDebug("Track intersects a bad channel!");
    			return false;
    		}
    	}
        
        ITransform3D localToGlobal = sensor.getGeometry().getLocalToGlobal();
    	
        Hep3Vector sensorPos = sensor.getGeometry().getPosition();   
        Box sensorSolid = (Box) sensor.getGeometry().getLogicalVolume().getSolid();
        Polygon3D sensorFace = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, 1)).get(0);
        if(debug){
        	System.out.println(this.getClass().getSimpleName() + ": Sensor: " + SvtUtils.getInstance().getDescription(sensor));
        	System.out.println(this.getClass().getSimpleName() + ": Track Position: " + trackPosition.toString());
        }
        
        List<Point3D> vertices = new ArrayList<Point3D>();
        for(int index = 0; index < 4; index++){
        	vertices.add(new Point3D());
        }
        for(Point3D vertex : sensorFace.getVertices()){
            if(vertex.y() < 0 && vertex.x() > 0){
            	localToGlobal.transform(vertex);
                //vertices.set(0, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
            	vertices.set(0, new Point3D(vertex.x(), vertex.y(), vertex.z()));
                if(debug){
                	System.out.println(this.getClass().getSimpleName() + ": Vertex 1 Position: " + vertices.get(0).toString());
                	//System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 1 Position: " + localToGlobal.transformed(vertex).toString());
                }
            } 
            else if(vertex.y() > 0 && vertex.x() > 0){
            	localToGlobal.transform(vertex);
                //vertices.set(1, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
                vertices.set(1, new Point3D(vertex.x(), vertex.y(), vertex.z()));
                if(debug){
                System.out.println(this.getClass().getSimpleName() + ": Vertex 2 Position: " + vertices.get(1).toString());
            	//System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 2 Position: " + localToGlobal.transformed(vertex).toString());
                }
            } 
            else if(vertex.y() > 0 && vertex.x() < 0){
            	localToGlobal.transform(vertex);
                //vertices.set(2, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
                vertices.set(2, new Point3D(vertex.x(), vertex.y(), vertex.z()));
                if(debug){
                System.out.println(this.getClass().getSimpleName() + ": Vertex 3 Position: " + vertices.get(2).toString());
            	//System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 3 Position: " + localToGlobal.transformed(vertex).toString());
            	}
            }             
            else if(vertex.y() < 0 && vertex.x() < 0){
            	localToGlobal.transform(vertex);
                //vertices.set(3, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
                vertices.set(3, new Point3D(vertex.x(), vertex.y(), vertex.z()));
                if(debug){
                System.out.println(this.getClass().getSimpleName() + ": Vertex 4 Position: " + vertices.get(3).toString());
            	//System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 4 Position: " + localToGlobal.transformed(vertex).toString());
                }
            } 
        }

        double area1 = this.findTriangleArea(vertices.get(0).x(), vertices.get(0).y(), vertices.get(1).x(), vertices.get(1).y(), trackPosition.y(), trackPosition.z()); 
        double area2 = this.findTriangleArea(vertices.get(1).x(), vertices.get(1).y(), vertices.get(2).x(), vertices.get(2).y(), trackPosition.y(), trackPosition.z()); 
        double area3 = this.findTriangleArea(vertices.get(2).x(), vertices.get(2).y(), vertices.get(3).x(), vertices.get(3).y(), trackPosition.y(), trackPosition.z()); 
        double area4 = this.findTriangleArea(vertices.get(3).x(), vertices.get(3).y(), vertices.get(0).x(), vertices.get(0).y(), trackPosition.y(), trackPosition.z()); 
        
        if((area1 > 0 && area2 > 0 && area3 > 0 && area4 > 0) || (area1 < 0 && area2 < 0 && area3 < 0 && area4 < 0)) return true;
        return false;
    } 

    /**
     *
     */
   public double findTriangleArea(double x0, double y0, double x1, double y1, double x2, double y2){
       return .5*(x1*y2 - y1*x2 -x0*y2 + y0*x2 + x0*y1 - y0*x1); 
    }
   
    @Override
    public void endOfData(){
        System.out.println("%===================================================================%");
        System.out.println("%======================  Hit Efficiencies ==========================%");
        System.out.println("%===================================================================% \n%");
        if(numberOfTopTracks > 0){
        	double topEfficiency = numberOfTopTracksWithHitOnMissingLayer/numberOfTopTracks;
        	System.out.println("% Top Hit Efficiency: " + numberOfTopTracksWithHitOnMissingLayer + "/" + 
        						numberOfTopTracks + " = " + topEfficiency*100 + "%");
        	System.out.println("% Top Hit Efficiency Error: sigma poisson = " 
        						+ topEfficiency*Math.sqrt((1/numberOfTopTracksWithHitOnMissingLayer) + (1/numberOfTopTracks))*100 + "%");
        	System.out.println("% Top Hit Efficiency Error: sigma binomial = " 
        						+ (1/numberOfTopTracks)*Math.sqrt(numberOfTopTracksWithHitOnMissingLayer*(1-topEfficiency))*100 + "%");
        }
        if(numberOfBottomTracks > 0){
        	double bottomEfficiency = numberOfBottomTracksWithHitOnMissingLayer/numberOfBottomTracks;
        	System.out.println("% Bottom Hit Efficiency: " + numberOfBottomTracksWithHitOnMissingLayer + "/" 
        						+ numberOfBottomTracks + " = " + bottomEfficiency*100 + "%");
        	System.out.println("% Bottom Hit Efficiency Error: sigma poisson= " 
        						+ bottomEfficiency*Math.sqrt((1/numberOfBottomTracksWithHitOnMissingLayer) + (1/numberOfBottomTracks))*100 + "%");
        	System.out.println("% Top Hit Efficiency Error: sigma binomial = " 
								+ (1/numberOfBottomTracks)*Math.sqrt(numberOfBottomTracksWithHitOnMissingLayer*(1-bottomEfficiency))*100 + "%");
        }
/*        for(int index = 0; index < topTracksWithHitOnMissingLayer.length; index++){
        	if(topTracksPerMissingLayer[index] > 0)
        		System.out.println("% Top Layer " + (index+1) + ": " + (topTracksWithHitOnMissingLayer[index]/topTracksPerMissingLayer[index])*100 + "%");
        }
        for(int index = 0; index < bottomTracksWithHitOnMissingLayer.length; index++){
        	if(bottomTracksPerMissingLayer[index] > 0)
        		System.out.println("% Bottom Layer " + (index+1) + ": " + (bottomTracksWithHitOnMissingLayer[index]/bottomTracksPerMissingLayer[index])*100 + "%");
        }*/
        System.out.println("% \n%===================================================================%");
    }
}
