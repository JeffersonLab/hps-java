package org.hps.crawler;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.srs.datacat.client.Client;
import org.srs.datacat.client.ClientBuilder;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.dataset.DatasetLocationModel;
import org.srs.datacat.model.dataset.DatasetWithViewModel;

/**
 * Example of printing information from all files for a given run in the datacat.
 * @author jeremym
 */
public class DatacatPrintRun {
    
    /* metadata fields that should be included in search results */
    private static final String[] METADATA_FIELDS = {
        "BAD_EVENTS",
        "BLINDED",
        "END_EVENT_COUNT",
        "END_TIMESTAMP",
        "FILE",
        "FIRST_HEAD_TIMESTAMP",
        "FIRST_PHYSICS_EVENT",
        "LAST_HEAD_TIMESTAMP",
        "LAST_PHYSICS_EVENT",
        "LED_COSMIC",
        "PAIRS0",
        "PAIRS1",
        "PHYSICS_EVENTS",
        "PULSER",
        "SINGLES0",
        "SINGLES1",
        "TI_TIME_MAX_OFFSET",
        "TI_TIME_MIN_OFFSET",
        "TI_TIME_N_OUTLIERS",
        "TRIGGER_RATE"
    };
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new RuntimeException("Missing run number argument.");
        }
        int run = Integer.parseInt(args[0]); 
        printRun(run);
    }
    
    private static void printRun(int run) throws Exception {
        
        /* initialize datacat client */
        Client client = new ClientBuilder().setUrl("http://hpsweb.jlab.org/datacat/r").build();
        
        /* perform dataset search */
        DatasetResultSetModel results = client.searchForDatasets(
                "/HPS/data/raw",
                "current", /* dataset version */
                "JLAB",
                "fileFormat eq 'EVIO' AND dataType eq 'RAW' AND runMin eq " + run, /* basic query */
                new String[] {"FILE"}, /* sort on file number */
                METADATA_FIELDS /* metadata field values to return from query */
                );
        
        /* print results including metadata */
        for (DatasetModel dataset : results) {            
            DatasetWithViewModel datasetView = (DatasetWithViewModel) dataset;
            DatasetLocationModel loc = datasetView.getViewInfo().getLocations().iterator().next();
            System.out.println("name: " + datasetView.getName() + "; path: " + datasetView.getPath() + "; resource: " + loc.getResource());
            Map<String, Object> metadata = datasetView.getMetadataMap();
            SortedSet<String> keys = new TreeSet<String>(metadata.keySet());
            for (String key : keys) {
                Object value = metadata.get(key);
                System.out.println("  " + key + " = " + value);
            }
            System.out.println();
        }
    }
}
