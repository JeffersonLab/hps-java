package org.hps.datacat.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.datacat.client.DatacatClient;
import org.hps.datacat.client.DatacatClientImpl;
import org.hps.datacat.client.Dataset;
import org.hps.datacat.client.DatasetDataType;
import org.hps.datacat.client.DatasetFileFormat;
import org.hps.datacat.client.DatasetSite;

import junit.framework.TestCase;

public class DatacatTest extends TestCase {
    
    private static final String DATASET_NAME = "dummyDataset";
    private static final String FOLDER = "dummyFolder";
    private static final String RESOURCE = "/path/to/dummyDataset.ds";
    
    public void testDatacat() throws Exception {

        // Datacat client with default parameters
        DatacatClient client = new DatacatClientImpl();
        
        // Stores response from HTTP operations
        int response = -1;
        
        // Create dummy folder
        response = client.makeFolder("dummyFolder");
        System.out.println("makeFolder: " + response);
        System.out.println();
                        
        // Add dummy dataset
        // TODO: should add some meta data here too
        response = client.addDataset(FOLDER, DatasetDataType.TEST, RESOURCE, DatasetSite.SLAC, DatasetFileFormat.TEST, DATASET_NAME);
        System.out.println("addDataset: " + response);
        System.out.println();
        
        // Patch the dataset with some meta data
        Map<String, Object> metaData = new HashMap<String, Object>();
        //metaData.put("someStringVar", "aStringValue");
        metaData.put("someIntVar", 1234);
        //metaData.put("someFloatVar", 1.234);
        response = client.addMetadata(FOLDER, DATASET_NAME, metaData);
        System.out.println("patchDataset: " + response);
        System.out.println();
        
        // TODO: check that folder exists
        
        // TODO: check that dataset exists
        
        // TODO: get the full folder info
                
        // TODO: get the full dataset info       
                       
        // Find the dataset with a simple query
        List<Dataset> datasets = client.findDatasets(FOLDER, "someIntVar == 1234");
        for (Dataset dataset : datasets) {
            System.out.println("found dataset: " + dataset.getName());
        }
        
        // Delete the dataset
        response = client.deleteDataset("/" + FOLDER + "/" + DATASET_NAME);
        System.out.println("deleteDataset: " + response);
        System.out.println();
        
        // Remove the folder
        client.removeFolder("/" + FOLDER);
        System.out.println("removeFolder: " + response);
        System.out.println();
    }         
}
