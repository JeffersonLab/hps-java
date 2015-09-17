package org.hps.datacat.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Dataset utilities for the crawler.
 * 
 * @author Jeremy McCormick, SLAC
 */
final class DatasetUtilities {
    
    /**
     * Create {@link Dataset} objects from JSON search results.
     * 
     * @param searchResults the JSON search results
     * @return the list of {@link Dataset} objects
     */
    static List<Dataset> getDatasetsFromSearch(JSONObject searchResults) {
        List<Dataset> datasets = new ArrayList<Dataset>();
        JSONArray resultsArray = searchResults.getJSONArray("results");
        for (int i = 0; i < resultsArray.length(); i++) {
            datasets.add(new DatasetImpl(resultsArray.getJSONObject(i)));
        }
        return datasets;
    }
    
    /**
     * No class instantiation.
     */
    private DatasetUtilities() {
        throw new RuntimeException("Do not instantiate this class.");
    }
}
