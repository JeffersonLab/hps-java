package org.hps.datacat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.dataset.DatasetWithViewModel;

/**
 * Creates an index between an EVIO dataset and various metadata such as head timestamp range, file number, and event ID
 * range.
 * 
 * @author jeremym
 */
public final class EvioDatasetIndex {

    private DatasetResultSetModel datasets;
    private DatacatUtilities util;
    private Map<TimestampRange, DatasetModel> datasetTimestamps = new HashMap<TimestampRange, DatasetModel>();
    private Map<Long, DatasetModel> datasetFileNumbers = new HashMap<Long, DatasetModel>();
    private List<FileEventRange> eventRanges = new ArrayList<FileEventRange>();

    static class TimestampRange {

        private long startTimestamp;
        private long endTimestamp;

        TimestampRange(long startTimestamp, long endTimestamp) {
            this.startTimestamp = startTimestamp;
            this.endTimestamp = endTimestamp;
        }
    }

    public EvioDatasetIndex(int run) {
        this.util = new DatacatUtilities();
        load(run);
    }
   
    public EvioDatasetIndex(int run, DatacatUtilities util) {
        this.util = util;
        load(run);
    }
    
    public List<DatasetModel> findByTimestamp(long timestamp) {
        List<DatasetModel> datasets = new ArrayList<DatasetModel>();
        for (Entry<TimestampRange, DatasetModel> entry : datasetTimestamps.entrySet()) {
            if (timestamp >= entry.getKey().startTimestamp && timestamp <= entry.getKey().endTimestamp) {
                datasets.add(entry.getValue());
            }
        }
        return datasets;
    }

    public DatasetModel findByEventRange(long eventId) {
        return FileEventRange.findEventRange(eventRanges, eventId).getDataset();
    }

    public DatasetModel findByFileNumber(long fileNumber) {
        return datasetFileNumbers.get(fileNumber);
    }

    public DatasetResultSetModel getDatasets() {
        return datasets;
    }

    private void load(int run) {

        datasets = util.findEvioDatasets(run);

        // setup index of first and last timestamp
        for (DatasetModel dataset : datasets) {
            DatasetWithViewModel datasetView = (DatasetWithViewModel) dataset;
            Map<String, Object> metadata = datasetView.getMetadataMap();
            long firstTimestamp = (Long) metadata.get("FIRST_HEAD_TIMESTAMP");
            long lastTimestamp = (Long) metadata.get("LAST_HEAD_TIMESTAMP");
            datasetTimestamps.put(new TimestampRange(firstTimestamp, lastTimestamp), dataset);
        }

        // setup index by file number
        for (DatasetModel dataset : datasets) {
            DatasetWithViewModel datasetView = (DatasetWithViewModel) dataset;
            Map<String, Object> metadata = datasetView.getMetadataMap();
            long fileNumber = (Long) metadata.get("FILE");
            this.datasetFileNumbers.put(fileNumber, dataset);
        }
        
        // setup index by file number
        this.eventRanges = FileEventRange.createEventRanges(datasets);
    }
   
    // This is a test and not a command line interface!
    public static void main(String[] args) {

        EvioDatasetIndex datasetIndex = new EvioDatasetIndex(5772);
        DatasetResultSetModel datasets = datasetIndex.getDatasets();

        for (DatasetModel dataset : datasets) {

            System.out.println("checking dataset " + dataset.getName() + " ...");

            DatasetWithViewModel datasetView = (DatasetWithViewModel) dataset;
            Map<String, Object> metadata = datasetView.getMetadataMap();

            long firstTimestamp = (Long) metadata.get("FIRST_HEAD_TIMESTAMP");
            long lastTimestamp = (Long) metadata.get("LAST_HEAD_TIMESTAMP");
            long fileNumber = (Long) metadata.get("FILE");
            long firstPhysicsEvent = (Long) metadata.get("FIRST_PHYSICS_EVENT");
            long lastPhysicsEvent = (Long) metadata.get("LAST_PHYSICS_EVENT");
            
            System.out.println("FIRST_HEAD_TIMESTAMP = " + firstTimestamp);
            System.out.println("LAST_HEAD_TIMESTAMP = " + lastTimestamp);
            System.out.println("FILE = " + fileNumber);
            System.out.println("FIRST_PHYSICS_EVENT = " + firstPhysicsEvent);
            System.out.println("LAST_PHYSICS_EVENT = " + lastPhysicsEvent);
                        
            DatasetModel result = datasetIndex.findByEventRange(firstPhysicsEvent);
            System.out.println("found " + result.getName() + " for event ID " + firstPhysicsEvent);

            result = datasetIndex.findByEventRange(lastPhysicsEvent);
            System.out.println("found " + result.getName() + " for event ID " + lastPhysicsEvent);

            result = datasetIndex.findByFileNumber(fileNumber);
            System.out.println("found " + result.getName() + " for file " + fileNumber);

            List<DatasetModel> firstTimestampDatasets = datasetIndex.findByTimestamp(firstTimestamp);
            for (DatasetModel firstTimestampDataset : firstTimestampDatasets) {
                System.out.println("found " + firstTimestampDataset.getName() + " for timestamp = " 
                        + firstTimestamp);
            }

            List<DatasetModel> lastTimestampDatasets = datasetIndex.findByTimestamp(lastTimestamp);
            for (DatasetModel lastTimestampDataset : lastTimestampDatasets) {
                System.out.println("found " + lastTimestampDataset.getName() + " for timestamp = "
                        + lastTimestamp);
            }
            
            long midTimestamp = firstTimestamp + (lastTimestamp - firstTimestamp);
            List<DatasetModel> midTimestampDatasets = datasetIndex.findByTimestamp(midTimestamp);
            for (DatasetModel midTimestampDataset : midTimestampDatasets) {
                System.out.println("found " + midTimestampDataset.getName() + " for timestamp = " + firstTimestamp);
            }

            System.out.println();
        }
    }
}
