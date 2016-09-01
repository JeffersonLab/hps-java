package org.hps.users.spaul;

import java.util.List;
import java.util.Map;

import org.hps.datacat.DatacatUtilities;
import org.hps.datacat.EvioDatasetIndex;

import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.dataset.DatasetWithViewModel;

public class PrintStuff {
	public static void main(String arg[]){
		/*for(int run = 7000; run <= 8100; run++){
			EvioDatasetIndex edi;
			try{
				edi = new EvioDatasetIndex(new DatacatUtilities(), run);
			}catch(Exception e){
				continue;
			}
			for(int fileNumber = 0; fileNumber < edi.getDatasets().getCount(); fileNumber++){
				DatasetModel dataset = edi.findByFileNumber(fileNumber);
				DatasetWithViewModel datasetView = (DatasetWithViewModel) dataset;
				if(datasetView == null)
					continue;
				Map<String, Object> metadata = datasetView.getMetadataMap();

				long firstTimestamp = (Long) metadata.get("FIRST_HEAD_TIMESTAMP");
				long lastTimestamp = (Long) metadata.get("LAST_HEAD_TIMESTAMP");
				
				long tiTimeOffset = Long.parseLong((String) metadata.get("TI_TIME_MIN_OFFSET"));
				//long fileNumber = (Long) metadata.get("FILE");
				long firstPhysicsEvent = (Long) metadata.get("FIRST_PHYSICS_EVENT");
				long lastPhysicsEvent = (Long) metadata.get("LAST_PHYSICS_EVENT");
				long nEvents = lastPhysicsEvent- firstPhysicsEvent+1; 
				
				int firstTI = 0;
				int lastTI = 0;
				System.out.printf("%d,%d,%d,0,%d,%d,%d,%d,%d\n", run, fileNumber, nEvents, firstTimestamp, lastTimestamp, firstTI, lastTI, tiTimeOffset);
				
			}
		}*/
	}
}
