package org.hps.readout.ecal.updated;

import org.lcsim.event.Cluster;

public class GTPClusterReadoutDriver extends ReadoutDriver {
	private String ecalHitCollectionName = "EcalRawHits";
	private String outputClusterCollectionName = "EcalClustersGTP";
	
	@Override
	public void startOfData() {
		addDependency(ecalHitCollectionName);
		ReadoutDataManager.registerCollection(outputClusterCollectionName, this, Cluster.class);
	}
	
	@Override
	protected double getTimeDisplacement() {
		return 16;
	}
}
