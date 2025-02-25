package org.hps.recon.filtering;

import static java.lang.Math.abs;
import java.util.List;
import java.util.ArrayList;
import org.lcsim.event.Cluster;
import org.lcsim.event.base.BaseCluster;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;

/**
 * Class to strip off 3-prong trident candidates, using only the ECal cluster energy sum, 
 * requires exactly 3 in-time clusters, with at least 1 top and 1 bottom and 
 * the 3-cluster energy sum be within eMin<3-Esum<eMax
 */
public class ThreeProngECalFilter extends EventReconFilter {
    
    private String _ECalClusterCollectionName =  "EcalClustersCorr";
    private double _triggerTimeOffset = 38.0; //good for 2021 data
    private double _triggerWindowSize = 10.0;  //+/- ns
    private double _clusterRelativeTimingCut = 5.0;
    private double _eMin = 3.0;
    private double _eMax = 4.0;
    
    @Override
    protected void process(EventHeader event) {
        incrementEventProcessed();

        if (!event.hasCollection(Cluster.class, _ECalClusterCollectionName)) {
            skipEvent();
        }
        List<Cluster> clusters = event.get(Cluster.class, _ECalClusterCollectionName);
	if(clusters.size()<3) //have to have three clusters for this
	    skipEvent(); 

	
	boolean hasTop=false; 
	boolean hasBottom=false; 
	List<Cluster> inTimeClusters=new ArrayList<Cluster>();
	for (Cluster clu : clusters){
	    double clTime= ClusterUtilities.getSeedHitTime(clu);
	    if( clTime>(_triggerTimeOffset-_triggerWindowSize)
		&& clTime<(_triggerTimeOffset+_triggerWindowSize)){
		if(clu.getPosition()[1]>0)
		    hasTop=true;
		else
		    hasBottom=true; 
		inTimeClusters.add(clu);
	    }
	}
	//	System.out.println("Found "+inTimeClusters.size()+" in time clusters"); 
	if(inTimeClusters.size()!=3) //have to have exactly three in time clusters for this
	    skipEvent(); 

	if(! (hasTop && hasBottom))
	    skipEvent();
	
	double t0= ClusterUtilities.getSeedHitTime(inTimeClusters.get(0));
	double t1= ClusterUtilities.getSeedHitTime(inTimeClusters.get(1));
	double t2= ClusterUtilities.getSeedHitTime(inTimeClusters.get(2));

	// require that all 3 clusters are within _clusterRelativeTimingCut 
	if(abs(t0-t1)>_clusterRelativeTimingCut)
	    skipEvent();
	if(abs(t1-t2)>_clusterRelativeTimingCut)
	    skipEvent();
	if(abs(t2-t0)>_clusterRelativeTimingCut)
	    skipEvent();
	// forget to add energy cut!
	double clEneSum=inTimeClusters.get(0).getEnergy()
	    +inTimeClusters.get(1).getEnergy()
	    +inTimeClusters.get(2).getEnergy(); 
	if(clEneSum<_eMin || clEneSum>_eMax)
	    skipEvent();
	
        incrementEventPassed();
    }
    
    protected void detectorChanged(Detector detector) {
        super.detectorChanged(detector);
    }
}
