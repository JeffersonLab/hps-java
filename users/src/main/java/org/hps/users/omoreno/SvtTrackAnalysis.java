package org.hps.users.omoreno;

//--- java ---//
//--- hep ---//
import hep.aida.IPlotter;
import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
//--- org.lcsim ---//
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
//--- hps-java ---//

/**
 * 
 * @author Omar Moreno
 * @version $Id: SvtTrackAnalysis.java,v 1.7 2013/11/06 19:19:55 jeremy Exp $
 *
 */

public class SvtTrackAnalysis extends Driver {
	
    private String trackCollectionName = "MatchedTracks";
    private String stripHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";
	private AIDA aida;
	private List<IPlotter> plotters = new ArrayList<IPlotter>();
    
	
	int npositive = 0;
	int nnegative = 0;
	double ntracks = 0;
	double ntracksTop = 0;
	double ntracksBottom = 0;
	double nTwoTracks = 0;
	double nevents = 0;
	
	
	public SvtTrackAnalysis(){
	}

	protected void detectorChanged(Detector detector){
		
		aida = AIDA.defaultInstance();
		aida.tree().cd("/");
		
		int nPlotters = 0;
		
		//--- Track Extrapolation ---//
		//---------------------------//	
		plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal"));
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
		
        
        //--- Track Parameters ---//
        //------------------------//
        plotters.add(aida.analysisFactory().createPlotterFactory().create("DOCA"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("DOCA", 120, 0, 120));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Z0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Z0", 120, 0, 120));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("phi0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("phi0", 50, -Math.PI, Math.PI));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Curvature"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("R", 200, -10, 10));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Tan(Lambda)"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Tan(Lambda)", 100, 0, 1));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
		nPlotters++;
		
        plotters.add(aida.analysisFactory().createPlotterFactory().create("ChiSquared"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("ChiSquared", 100, 0, 100));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
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
		
		for(IPlotter plotter : plotters) plotter.show();
	}
	
	public void process(EventHeader event){
		nevents++;
		
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
		}
				
		if(!event.hasCollection(Track.class, trackCollectionName)) return;
    	List<SeedTrack> tracks = event.get(SeedTrack.class, trackCollectionName);
    	
    	Map<Hep3Vector,SeedTrack> trackToEcalPosition = new HashMap<Hep3Vector, SeedTrack>();
     	Map<SeedTrack, Cluster> trackToCluster = new HashMap<SeedTrack, Cluster>();
    	List<Hep3Vector> ecalPos = new ArrayList<Hep3Vector>();
    	
    	for(SeedTrack track : tracks){
    		ntracks++;
    		Hep3Vector positionEcal = TrackUtils.getTrackPositionAtEcal(track);
    		System.out.println("Position at Ecal: " + positionEcal);
    		Hep3Vector positionConverter = TrackUtils.extrapolateTrack(track,-700);
    	
    		aida.histogram2D("Track Position at Ecal").fill(positionEcal.y(), positionEcal.z());
    		aida.histogram2D("Track Position at Harp").fill(positionConverter.y(), positionConverter.z());

    		if(positionEcal.z() > 0 ) ntracksTop++;
    		else if(positionEcal.z() < 0) ntracksBottom++;
    		
    		
    		aida.histogram1D("DOCA").fill(TrackUtils.getDoca(track));
    		aida.histogram1D("Z0").fill(TrackUtils.getZ0(track));
    		aida.histogram1D("phi0").fill(TrackUtils.getPhi0(track));
    		aida.histogram1D("R").fill((1/TrackUtils.getR(track))*1000);
    		aida.histogram1D("Tan(Lambda)").fill(TrackUtils.getTanLambda(track));
    		
    		aida.histogram1D("Px").fill(track.getPX());
    		aida.histogram1D("Py").fill(track.getPY());
    		aida.histogram1D("Pz").fill(track.getPZ());
    		aida.histogram1D("ChiSquared").fill(track.getChi2());
    		
    		if(Math.signum(TrackUtils.getR(track)) < 0){
    			aida.histogram2D("Track Position at Ecal: Curvature < 0").fill(positionEcal.y(), positionEcal.z());
    			aida.histogram2D("Track Position at Harp: Curvature < 0").fill(positionConverter.y(), positionConverter.z());
        		aida.histogram1D("Px: C < 0").fill(track.getPX());
        		aida.histogram1D("Py: C < 0").fill(track.getPY());
        		aida.histogram1D("Pz: C < 0").fill(track.getPZ());
        		nnegative++;
    		} else if(Math.signum(TrackUtils.getR(track)) > 0){
    			aida.histogram2D("Track Position at Ecal: Curvature > 0").fill(positionEcal.y(), positionEcal.z());
    			aida.histogram2D("Track Position at Harp: Curvature > 0").fill(positionConverter.y(), positionConverter.z());
        		aida.histogram1D("Px: C > 0").fill(track.getPX());
        		aida.histogram1D("Px: C > 0").fill(track.getPY());
        		aida.histogram1D("Px: C > 0").fill(track.getPZ());
        		npositive++;
    		}
    		
    		if(tracks.size() > 1){
    			aida.histogram2D("Track Position at Ecal: Two Tracks").fill(positionEcal.y(), positionEcal.z());
    			aida.histogram2D("Track Position at Harp: Two Tracks").fill(positionConverter.y(), positionConverter.z()); 
    			aida.histogram1D("Px: Two Tracks").fill(track.getPX());
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
    	
    	for(Map.Entry<SeedTrack, Cluster> entry : trackToCluster.entrySet()){
    		double Energy = entry.getValue().getEnergy();
    		SeedTrack track = entry.getKey();
    		double pTotal = Math.sqrt(track.getPX()*track.getPX() + track.getPY()*track.getPY() + track.getPZ()*track.getPZ());
    		
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
	}
}
