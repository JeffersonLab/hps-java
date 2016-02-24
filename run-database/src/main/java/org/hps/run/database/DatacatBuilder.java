package org.hps.run.database;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.record.triggerbank.TiTimeOffsetCalculator;
import org.srs.datacat.client.Client;
import org.srs.datacat.client.exception.DcClientException;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.dataset.DatasetWithViewModel;
import org.srs.datacat.shared.DatasetLocation;

final class DatacatBuilder extends AbstractRunBuilder {
    
    private static final Logger LOGGER = Logger.getLogger(DatacatBuilder.class.getPackage().getName());
    
    private static final String[] METADATA_FIELDS = {
        "TI_TIME_MIN_OFFSET", 
        "TI_TIME_MAX_OFFSET", 
        "TI_TIME_N_OUTLIERS", 
        "END_TIMESTAMP",
        "GO_TIMESTAMP",
        "PRESTART_TIMESTAMP"
    };
    
    private Client datacatClient;
    private String site;
    private String folder;    
    private List<File> files;
                
    private static long calculateTiTimeOffset(DatasetResultSetModel results) {
        TiTimeOffsetCalculator calc = new TiTimeOffsetCalculator();
        for (DatasetModel ds : results) {
            DatasetWithViewModel view = (DatasetWithViewModel) ds;
            Map<String, Object> metadata = view.getMetadataMap();                        
            if (metadata.containsKey("TI_TIME_MIN_OFFSET")) {
                calc.addMinOffset(Long.parseLong((String) metadata.get("TI_TIME_MIN_OFFSET")));
            }
            if (metadata.containsKey("TI_TIME_MAX_OFFSET")) {
                calc.addMaxOffset(Long.parseLong((String) metadata.get("TI_TIME_MAX_OFFSET")));
            }
            if (metadata.containsKey("TI_TIME_N_OUTLIERS")) {
                calc.addNumOutliers((int) (long) metadata.get("TI_TIME_N_OUTLIERS"));
            }
        }
        return calc.calculateTimeOffset();
    }
    
    private static long getTotalEvents(DatasetResultSetModel results) {
        long totalEvents = 0;
        for (DatasetModel ds : results) {
            DatasetWithViewModel view = (DatasetWithViewModel) ds;
            DatasetLocation loc = (DatasetLocation) view.getViewInfo().getLocations().iterator().next();
            totalEvents += loc.getEventCount();
        }
        return totalEvents;
    }
    
    private static Integer getPrestartTimestamp(DatasetResultSetModel results) {
        DatasetWithViewModel ds = (DatasetWithViewModel) results.getResults().get(0);
        if (ds.getMetadataMap().containsKey("PRESTART_TIMESTAMP")) {
            return (int) (long) ds.getMetadataMap().get("PRESTART_TIMESTAMP");
        } else {
            return null;
        }
    }
    
    private static Integer getEndTimestamp(DatasetResultSetModel results) {        
        DatasetWithViewModel ds = (DatasetWithViewModel) results.getResults().get(results.getResults().size() - 1);
        if (ds.getMetadataMap().containsKey("END_TIMESTAMP")) {
            return (int) (long) ds.getMetadataMap().get("END_TIMESTAMP");
        } else {
            return null;
        }
    }
    
    
    private static Integer getGoTimestamp(DatasetResultSetModel results) {
        DatasetWithViewModel ds = (DatasetWithViewModel) results.getResults().get(0);
        if (ds.getMetadataMap().containsKey("GO_TIMESTAMP")) {
            return (int) (long) ds.getMetadataMap().get("GO_TIMESTAMP");
        } else {
            return null;
        }
    }
    
    private static double calculateTriggerRate(Integer startTimestamp, Integer endTimestamp, long nEvents) {
        if (startTimestamp == null) {
            throw new IllegalArgumentException("The start timestamp is null.");
        }
        if (endTimestamp == null) {
            throw new IllegalArgumentException("The end timestamp is null.");
        }
        if (endTimestamp - startTimestamp == 0) {
            throw new IllegalArgumentException("The start and end timestamp are the same.");
        }
        if (nEvents == 0) {
            throw new IllegalArgumentException("The number of events is zero.");
        }
        double triggerRate = (double) nEvents / ((double) endTimestamp - (double) startTimestamp);
        return triggerRate;
    }
        
    void build() {
        
        if (getRunSummary() == null) {
            throw new RuntimeException("The run summary was not set.");
        }        
        if (this.datacatClient == null) {
            throw new RuntimeException("The datacat client was not set.");
        }        
        if (this.folder == null) {
            throw new RuntimeException("The target folder was not set.");
        }
        if (this.site == null) {
            throw new RuntimeException("The site was not set.");
        }
        
        DatasetResultSetModel results = null;
        try {
            results = findDatasets();
        } catch (DcClientException e) {
            System.err.println("HTTP status: " + e.getStatusCode());
            throw new RuntimeException(e);
        }
        
        files = DatacatUtilities.toFileList(results);
        
        if (results.getResults().isEmpty()) {
            throw new RuntimeException("No results found for datacat search.");
        }
        
        long tiTimeOffset = calculateTiTimeOffset(results);
        getRunSummary().setTiTimeOffset(tiTimeOffset);
        
        long totalEvents = getTotalEvents(results);
        getRunSummary().setTotalEvents(totalEvents);
        
        int nFiles = results.getResults().size();
        getRunSummary().setTotalFiles(nFiles);
        
        int prestartTimestamp = getPrestartTimestamp(results);
        getRunSummary().setPrestartTimestamp(prestartTimestamp);
        
        int goTimestamp = getGoTimestamp(results);
        getRunSummary().setGoTimestamp(goTimestamp);
        
        int endTimestamp = getEndTimestamp(results);
        getRunSummary().setEndTimestamp(endTimestamp);
        
        double triggerRate = calculateTriggerRate(prestartTimestamp, endTimestamp, totalEvents);
        getRunSummary().setTriggerRate(triggerRate);        
    }
                         
    private DatasetResultSetModel findDatasets() {
        
        LOGGER.info("finding EVIO datasets for run " + getRun() + " in " + this.folder + " at " + this.site + " ...");
        
        DatasetResultSetModel results = datacatClient.searchForDatasets(
                this.folder,
                "current",
                this.site,
                "fileFormat eq 'EVIO' AND dataType eq 'RAW' AND runMin eq " + getRun(),
                new String[] {"FILE"},
                METADATA_FIELDS
                );
        
        LOGGER.info("found " + results.getResults().size() + " EVIO datasets for run " + getRun());
                               
        return results;
    }    
    
    void setSite(String site) {
        this.site = site;
    }
    
    void setDatacatClient(Client datacatClient) {
        this.datacatClient = datacatClient;
    }
    
    void setFolder(String folder) {
        this.folder = folder;
    }
    
    List<File> getFileList() {
        return files;
    }
}
