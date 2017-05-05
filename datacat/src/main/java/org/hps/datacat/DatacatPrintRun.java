package org.hps.datacat;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.dataset.DatasetLocationModel;
import org.srs.datacat.model.dataset.DatasetWithViewModel;

/**
 * Example of printing information from all files for a given run in the datacat.
 */
public final class DatacatPrintRun {
            
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new RuntimeException("Missing run number argument.");
        }
        int run = Integer.parseInt(args[0]); 
        printRun(run);
    }
    
    private static void printRun(int run) throws Exception {
                
        DatasetResultSetModel results = new DatacatUtilities().findEvioDatasets(run);
                
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
