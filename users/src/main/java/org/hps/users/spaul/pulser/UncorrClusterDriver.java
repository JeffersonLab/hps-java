package org.hps.users.spaul.pulser;

import hep.aida.IHistogram2D;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class UncorrClusterDriver extends Driver{
	AIDA aida = AIDA.defaultInstance();
	public static boolean fid_ECal(double x, double y)
    {
        y = Math.abs(y);

        boolean in_fid = false;
        double x_edge_low = -262.74;
        double x_edge_high = 347.7;
        double y_edge_low = 33.54;
        double y_edge_high = 75.18;

        double x_gap_low = -106.66;
        double x_gap_high = 42.17;
        double y_gap_high = 47.18;

        y = Math.abs(y);

        if( x > x_edge_low && x < x_edge_high && y > y_edge_low && y < y_edge_high )
        {
            if( !(x > x_gap_low && x < x_gap_high && y > y_edge_low && y < y_gap_high) )
            {
                in_fid = true;
            }
        }

        return in_fid;
    }
	 private void processUncorrectedCluster(Cluster c){
	    	double time = 0;
	    	double seedEnergy = -100;
	    	if(Math.abs(c.getPosition()[1])< 47.18)
        		return;
	        for(CalorimeterHit hit : c.getCalorimeterHits()){
	        	//if(Math.abs(hit.getPositionVec().y())< 47.18)
	        		//continue;
	            if(hit.getCorrectedEnergy() > seedEnergy){
	                seedEnergy = hit.getCorrectedEnergy();
	                time = hit.getTime();
	                
	            }
	        }
	        double clusterEnergy = c.getEnergy();
	        //c.get
	        int size = c.getSize();
	        
	        
	        energyHist.fill(clusterEnergy, time);
	        
	        seedHist.fill(seedEnergy, time);
	    	sizeHist.fill(size, time);
	    	
	    	if(clusterEnergy>.6 && seedEnergy>.4 && size>=3){
	    	energyHistCut.fill(clusterEnergy, time);
	        
	        seedHistCut.fill(seedEnergy, time);
	    	sizeHistCut.fill(size, time);
	    	}
	 }
	 
	 IHistogram2D energyHist, seedHist, sizeHist, 
	 		energyHistCut, seedHistCut, sizeHistCut;
	 @Override
	 public void process(EventHeader event){
		 List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
		 for(Cluster c : clusters)
			 processUncorrectedCluster(c);
	 }
	 @Override
	 public void detectorChanged(Detector d){
		 energyHist = aida.histogram2D("uncorr/energy vs time", 100, 0, 2, 100, 0, 400);
		 seedHist = aida.histogram2D("uncorr/seed vs time", 100, 0, 2, 100, 0, 400);
		 sizeHist = aida.histogram2D("uncorr/size vs time", 10, 0, 10, 100, 0, 400);
		 energyHistCut = aida.histogram2D("uncorr/energy vs time with cuts", 100, 0, 2, 100, 0, 400);
		 seedHistCut = aida.histogram2D("uncorr/seed vs time with cuts", 100, 0, 2, 100, 0, 400);
		 sizeHistCut = aida.histogram2D("uncorr/size vs time with cuts", 10, 0, 10, 100, 0, 400);
	
	 }
}
