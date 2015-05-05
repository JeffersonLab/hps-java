package org.hps.users.omoreno;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotter;
import hep.aida.IHistogram1D;
import hep.aida.ITree;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

import org.hps.recon.tracking.TrackUtils;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 */
public class SvtTrackAnalysis extends Driver {

    // Use JFreeChart as the default plotting backend
    static { 
        hep.aida.jfree.AnalysisFactory.register();
    }

    // Plotting
    ITree tree; 
    IHistogramFactory histogramFactory; 
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();

    private Map<String, IHistogram1D> trackParameterPlots = new HashMap<String, IHistogram1D>();

    private String trackCollectionName = "MatchedTracks";
	
	int npositive = 0;
	int nnegative = 0;
	double ntracks = 0;
	double ntracksTop = 0;
	double ntracksBottom = 0;
	double nTwoTracks = 0;
	double nevents = 0;
	

    /**
     *  Default Constructor
     */    
	public SvtTrackAnalysis(){
	}

	protected void detectorChanged(Detector detector){
	
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        plotters.put("Track Parameters", plotterFactory.create("Track Parameters"));
        plotters.get("Track Parameters").createRegions(2, 3);

        trackParameterPlots.put("DOCA", histogramFactory.createHistogram1D("DOCA", 80, -80, 80));         
        plotters.get("Track Parameters").region(0).plot(trackParameterPlots.get("DOCA")); 
      
        trackParameterPlots.put("Z0", histogramFactory.createHistogram1D("Z0", 30, -30, 30));    
        plotters.get("Track Parameters").region(1).plot(trackParameterPlots.get("Z0"));

        trackParameterPlots.put("phi0", histogramFactory.createHistogram1D("phi0", 50, -0.5, 0.5));    
        plotters.get("Track Parameters").region(2).plot(trackParameterPlots.get("phi0"));
    
        trackParameterPlots.put("Curvature", histogramFactory.createHistogram1D("Curvature", 200, -1, 1));    
        plotters.get("Track Parameters").region(3).plot(trackParameterPlots.get("Curvature"));

        trackParameterPlots.put("Tan(Lambda)", histogramFactory.createHistogram1D("Tan(Lambda)", 100, -1, 1));    
        plotters.get("Track Parameters").region(4).plot(trackParameterPlots.get("Tan(Lambda)"));
	
        trackParameterPlots.put("Chi2", histogramFactory.createHistogram1D("Chi2", 100, 0, 100));    
        plotters.get("Track Parameters").region(5).plot(trackParameterPlots.get("Chi2"));


		//--- Track Extrapolation ---//
		//---------------------------//	
		/*plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal"));
		plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal", 200, -350, 350, 200, -100, 100));
		plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
    	plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style();
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp", 200, -200, 200, 100, -50, 50));
		plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
    	plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
		nPlotters++;

        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal: Curvature < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal: Curvature < 0",200, -350, 350, 200, -100, 100));
		plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
    	plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
		nPlotters++;
		
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp: Curvature < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp: Curvature < 0", 200, -200, 200, 100, -50, 50));
		plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
    	plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
		nPlotters++;
		
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal: Curvature > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal: Curvature > 0", 200, -350, 350, 200, -100, 100));
		plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
    	plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
		nPlotters++;
		
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp: Curvature > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp: Curvature > 0", 200, -200, 200, 100, -50, 50));
		plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
    	plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
		nPlotters++;
		
		plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal: Two Tracks"));
		plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal: Two Tracks", 200, -350, 350, 200, -100, 100));
		plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
    	plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style();
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp: Two Tracks"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp: Two Tracks", 200, -200, 200, 100, -50, 50));
		plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
    	plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
		nPlotters++;
		
        
        //--- Momentum ---//
        //----------------//
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Py"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Py", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Pz"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Pz", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
		
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px: C > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px: C > 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Py: C > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Py: C > 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Pz: C > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Pz: C > 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
		
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px: C < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px: C < 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Py: C < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Py: C < 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Pz: C < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Pz: C < 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
		
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px: Two Tracks"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px: Two Tracks", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("E over P"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("E over P", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
		   
		plotters.add(aida.analysisFactory().createPlotterFactory().create("E versus P"));
	    plotters.get(nPlotters).region(0).plot(aida.histogram2D("E versus P", 100, 0, 1500, 100, 0, 4000));
	    plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
	    plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
	    plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
	    nPlotters++;
        
        //--- Cluster Matching ---//
        //------------------------//        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("XY Difference between Ecal Cluster and Track Position"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("XY Difference between Ecal Cluster and Track Position", 200, -200, 200, 100, -50, 50));
		plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
    	plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
		nPlotters++;
		*/
		for (IPlotter plotter : plotters.values()) { 
			plotter.show();
		}
	}
	
	public void process(EventHeader event){
		nevents++;
	
        /*    
		if(event.hasCollection(SiTrackerHitStrip1D.class, stripHitCollectionName)){
			
			System.out.println("Found Strip Hits!");
			List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, stripHitCollectionName);
			short[] samples = new short[6];
			for(SiTrackerHitStrip1D stripHit : stripHits){
				for(RawTrackerHit rawHit : stripHit.getRawHits()){
					for(int index = 0; index < samples.length; index++){
						samples[index] += rawHit.getADCValues()[index];
					}
				}
			}
		}*/
		
        // If the event doesn't have any tracks, skip it    
		if(!event.hasCollection(Track.class, trackCollectionName)) return;
    	
        // Get the collection of tracks from the event
        List<Track> tracks = event.get(Track.class, trackCollectionName);
    
        /*    
    	Map<Hep3Vector,Track> trackToEcalPosition = new HashMap<Hep3Vector, Track>();
     	Map<Track, Cluster> trackToCluster = new HashMap<Track, Cluster>();
    	List<Hep3Vector> ecalPos = new ArrayList<Hep3Vector>();
    	*/

    	for(Track track : tracks){
    
            trackParameterPlots.get("DOCA").fill(TrackUtils.getDoca(track));
            trackParameterPlots.get("Z0").fill(TrackUtils.getZ0(track));
            trackParameterPlots.get("phi0").fill(TrackUtils.getPhi0(track));
            trackParameterPlots.get("Curvature").fill(TrackUtils.getR(track));
            trackParameterPlots.get("Tan(Lambda)").fill(TrackUtils.getTanLambda(track));
            trackParameterPlots.get("Chi2").fill(track.getChi2());

        }
    }
}

            /*
            ntracks++;
    		Hep3Vector positionEcal = TrackUtils.getTrackPositionAtEcal(track);
    		System.out.println("Position at Ecal: " + positionEcal);
    		Hep3Vector positionConverter = TrackUtils.extrapolateTrack(track,-700);
    	
    		aida.histogram2D("Track Position at Ecal").fill(positionEcal.y(), positionEcal.z());
    		aida.histogram2D("Track Position at Harp").fill(positionConverter.y(), positionConverter.z());

    		if(positionEcal.z() > 0 ) ntracksTop++;
    		else if(positionEcal.z() < 0) ntracksBottom++;
            */
    		
    	
            /*    
    		aida.histogram1D("Px").fill(track.getTrackStates().get(0).getMomentum()[0]);
    		aida.histogram1D("Py").fill(track.getTrackStates().get(0).getMomentum()[1]);
    		aida.histogram1D("Pz").fill(track.getTrackStates().get(0).getMomentum()[2]);
    		aida.histogram1D("ChiSquared").fill(track.getChi2());
    		
    		if(Math.signum(TrackUtils.getR(track)) < 0){
    			aida.histogram2D("Track Position at Ecal: Curvature < 0").fill(positionEcal.y(), positionEcal.z());
    			aida.histogram2D("Track Position at Harp: Curvature < 0").fill(positionConverter.y(), positionConverter.z());
        		aida.histogram1D("Px: C < 0").fill(track.getTrackStates().get(0).getMomentum()[0]);
        		aida.histogram1D("Py: C < 0").fill(track.getTrackStates().get(0).getMomentum()[1]);
        		aida.histogram1D("Pz: C < 0").fill(track.getTrackStates().get(0).getMomentum()[2]);
        		nnegative++;
    		} else if(Math.signum(TrackUtils.getR(track)) > 0){
    			aida.histogram2D("Track Position at Ecal: Curvature > 0").fill(positionEcal.y(), positionEcal.z());
    			aida.histogram2D("Track Position at Harp: Curvature > 0").fill(positionConverter.y(), positionConverter.z());
        		aida.histogram1D("Px: C > 0").fill(track.getTrackStates().get(0).getMomentum()[0]);
        		aida.histogram1D("Px: C > 0").fill(track.getTrackStates().get(0).getMomentum()[1]);
        		aida.histogram1D("Px: C > 0").fill(track.getTrackStates().get(0).getMomentum()[2]);
        		npositive++;
    		}
    		
    		if(tracks.size() > 1){
    			aida.histogram2D("Track Position at Ecal: Two Tracks").fill(positionEcal.y(), positionEcal.z());
    			aida.histogram2D("Track Position at Harp: Two Tracks").fill(positionConverter.y(), positionConverter.z()); 
    			aida.histogram1D("Px: Two Tracks").fill(track.getTrackStates().get(0).getMomentum()[0]);
    			if(tracks.size() == 2) nTwoTracks++;
    		}
    		
    		trackToEcalPosition.put(positionEcal, track);
    		ecalPos.add(positionEcal);  		
    	}
    	
    	if(!event.hasCollection(Cluster.class, "EcalClusters")) return;
    	List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
    	

    	for(Hep3Vector ecalP : ecalPos){
        	double xdiff = 1000; 
        	double ydiff = 1000;
    		for(Cluster cluster : clusters){
    			double xd = ecalP.y() - cluster.getPosition()[0];
    			double yd = ecalP.z() - cluster.getPosition()[1];  
    			if(yd < ydiff){
    				xdiff = xd;
    				ydiff = yd;
    				trackToCluster.put(trackToEcalPosition.get(ecalP),cluster);
    			}
    		}
    		clusters.remove(trackToCluster.get(trackToEcalPosition.get(ecalP)));
    		aida.histogram2D("XY Difference between Ecal Cluster and Track Position").fill(xdiff, ydiff);
    	}
    	
    	for(Map.Entry<Track, Cluster> entry : trackToCluster.entrySet()){
    		double Energy = entry.getValue().getEnergy();
    		Track track = entry.getKey();
    		double pTotal = Math.sqrt(track.getTrackStates().get(0).getMomentum()[0]*track.getTrackStates().get(0).getMomentum()[0] + track.getTrackStates().get(0).getMomentum()[1]*track.getTrackStates().get(0).getMomentum()[1] + track.getTrackStates().get(0).getMomentum()[2]*track.getTrackStates().get(0).getMomentum()[2]);
    		
    		double ep = Energy/(pTotal*1000);
    		
    		System.out.println("Energy: " + Energy + "P: " + pTotal + " E over P: " + ep);
    		
    		aida.histogram1D("E over P").fill(ep);
    		aida.histogram2D("E versus P").fill(Energy, pTotal*1000);
    	}
    	
    	for(Cluster cluster : clusters){
    		double[] clusterPosition = cluster.getPosition();
    		
    		System.out.println("Cluster Position: [" + clusterPosition[0] + ", " + clusterPosition[1] + ", " + clusterPosition[2]+ "]");
    	}
    	
    	double ratio = nnegative/npositive;
    	System.out.println("Ratio of Negative to Position Tracks: " + ratio);
	
    	double tracksRatio = ntracks/nevents;
    	double tracksTopRatio = ntracksTop/nevents;
    	double tracksBottomRatio = ntracksBottom/nevents;
    	double twoTrackRatio = nTwoTracks/nevents;
    	System.out.println("Number of tracks per event: " + tracksRatio);
    	System.out.println("Number of top tracks per event: " + tracksTopRatio);
    	System.out.println("Number of bottom tracks per event: " + tracksBottomRatio);
    	System.out.println("Number of two track events: " + twoTrackRatio);
	}*/
