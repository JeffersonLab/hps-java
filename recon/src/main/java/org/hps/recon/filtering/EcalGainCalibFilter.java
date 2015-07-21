package org.hps.recon.filtering;

import java.util.List;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.hps.recon.ecal.cluster.ClusterUtilities;

public class EcalGainCalibFilter extends EventReconFilter {

	private double feeCut = 0.6;
	private double molCut = 0.6;
	private double tMin = 16.0;
	private double tMax = 80.0;
	private double dtMax = 12.0;
    private String clusterCollectionName = "EcalClusters";

    public void setFeeCut(double feeCut) { this.feeCut = feeCut; }
    public void setMolCut(double molCut) { this.molCut = molCut; }
    public void setTMin(double tMin) { this.tMin = tMin; }
    public void setTMax(double tMax) { this.tMax = tMax; }
    public void setDTMax(double dTMax) { this.dtMax = dTMax; }
    
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    @Override
    public void process(EventHeader event) 
    {
    	incrementEventProcessed();
        if (!event.hasCollection(Cluster.class, clusterCollectionName)) skipEvent();
        List<Cluster> cc = event.get(Cluster.class, clusterCollectionName);
        if (cc.size() < 1) skipEvent();
        boolean keepEvent = false;
        for (Cluster c1 : cc) 
        {
        	final double t1 = ClusterUtilities.getSeedHitTime(c1);
        	if (t1<tMin || t1>tMax) continue;
        	if (c1.getEnergy() > feeCut)
        	{
        		keepEvent = true;
        		break;
        	}
        	for (Cluster c2 : cc)
        	{
        		final double t2 = ClusterUtilities.getSeedHitTime(c2);
        		if (c1 == c2) continue;
        		if (t2<tMin || t2>tMax) continue;
        		if (Math.abs(t1-t2) > dtMax) continue;
        		if (c1.getEnergy() + c2.getEnergy() > molCut)
        		{
        			keepEvent = true;
        			break;
        		}
        	}
        }
        if (!keepEvent) skipEvent();
        incrementEventPassed();
    }
}
