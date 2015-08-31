package org.hps.datacat.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 
 * @author Jeremy McCormick, SLAC
 */
final class DatasetUtilities {
    
    static List<Dataset> getDatasetsFromSearch(JSONObject searchResults) {
        List<Dataset> datasets = new ArrayList<Dataset>();
        JSONArray resultsArray = searchResults.getJSONArray("results");
        for (int i = 0; i < resultsArray.length(); i++) {
            datasets.add(new DatasetImpl(resultsArray.getJSONObject(i)));
        }
        return datasets;
    }
}
