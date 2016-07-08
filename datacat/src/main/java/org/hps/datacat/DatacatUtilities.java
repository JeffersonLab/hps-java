package org.hps.datacat;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.srs.datacat.client.Client;
import org.srs.datacat.client.ClientBuilder;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.DatasetView.VersionId;
import org.srs.datacat.model.dataset.DatasetWithViewModel;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.Provider;

/**
 * Data Catalog utility functions.
 * 
 * @author jeremym
 */
public class DatacatUtilities {
    
    private static final Logger LOGGER = Logger.getLogger(DatacatUtilities.class.getPackage().getName());
                  
    /**
     * Add datasets to the data catalog or patch existing ones.
     * 
     * @param datasets the list of datasets
     * @param folder the target folder
     * @param url the datacat URL
     * @param patch <code>true</code> to allow patching existing datasets
     */
    public static final void updateDatasets(List<DatasetModel> datasets, String folder, String url, boolean patch) {
        int nUpdated = 0;
        Client client = null;
        try {
            client = new ClientBuilder().setUrl(url).build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid datacat URL.", e);
        }
        for (DatasetModel dataset : datasets) {
            try {
                if (client.exists(folder + "/" + dataset.getName())) {
                    
                    // Throw an error if patching is not allowed.
                    if (!patch) {
                        throw new RuntimeException("Dataset " + folder + "/" + dataset.getName() + " already exists and patching is disabled.");
                    }
                    
                    LOGGER.info("patching existing dataset " + folder + "/" + dataset.getName());
                
                    String site = 
                            ((DatasetWithViewModel) dataset).getViewInfo().getLocations().iterator().next().getSite();                                                                               
                    client.patchDataset(folder + "/" + dataset.getName(), "current", site, dataset);
                    
                } else {
                    LOGGER.info("creating new dataset for " + folder + "/" + dataset.getName());
                    client.createDataset(folder, dataset);                                       
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            ++nUpdated;
        }
        LOGGER.info("Inserted or updated " + nUpdated + " datasets.");
    }
    
    /**
     * Create a dataset for insertion into the data catalog.
     * 
     * @param file the file on disk
     * @param metadata the metadata map 
     * @param folder the datacat folder
     * @param site the datacat site
     * @param dataType the data type 
     * @param fileFormat the file format
     * @return the created dataset
     */
    public static final DatasetModel createDataset(
            File file,
            Map<String, Object> metadata,
            String folder,
            String site,
            String dataType,
            String fileFormat) {
        
        LOGGER.info("creating dataset for " + file.getPath());
        
        Provider provider = new Provider();                                              
        Dataset.Builder datasetBuilder = provider.getDatasetBuilder();
        
        // Set basic info on new dataset.
        datasetBuilder.versionId(VersionId.valueOf("new"))
            .master(true)
            .name(file.getName())
            .resource(file.getPath())
            .dataType(dataType)
            .fileFormat(fileFormat)
            .site(site)
            .scanStatus("OK");
        
        // Set system metadata from the provided metadata map.
        if (metadata.get("eventCount") != null) {
            datasetBuilder.eventCount((Long) metadata.get("eventCount"));
        }
        if (metadata.get("checksum") != null) {
            datasetBuilder.checksum((String) metadata.get("checksum"));
        }
        if (metadata.get("runMin") != null) {                   
            datasetBuilder.runMin((Long) metadata.get("runMin"));
        }
        if (metadata.get("runMax") != null) {
            datasetBuilder.runMax((Long) metadata.get("runMax"));
        }
        if (metadata.get("size") != null) {
            datasetBuilder.size((Long) metadata.get("size"));
        }
                                
        // Create user metadata, leaving out system metadata fields.
        Map<String, Object> userMetadata = new HashMap<String, Object>();
        for (Entry<String, Object> metadataEntry : metadata.entrySet()) {
            if (!DatacatConstants.isSystemMetadata(metadataEntry.getKey())) {
                userMetadata.put(metadataEntry.getKey(), metadataEntry.getValue());
            }
        }
        datasetBuilder.versionMetadata(userMetadata);       
        return datasetBuilder.build();
    }
    
    public static Client createDefaultClient() {
        try {
            return new ClientBuilder().setUrl(DatacatConstants.DATACAT_URL).build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error initializing datacat client.", e);
        }
    }
    
    public static DatasetResultSetModel findEvioDatasets(Client client, String folder, Site site, String[] metadata, String[] sort, int run) {
        if (client == null) {
            client = createDefaultClient();
        }
        return client.searchForDatasets(
                folder,
                "current", /* dataset version */
                site.toString(),
                "fileFormat eq 'EVIO' AND dataType eq 'RAW' AND runMin eq " + run, /* basic query */
                sort, /* sort on file number */
                metadata /* metadata field values to return from query */
                );
    }
    
    public static DatasetResultSetModel findEvioDatasets(int run) {        
        return findEvioDatasets(
                null,
                DatacatConstants.RAW_DATA_FOLDER,
                DatacatConstants.DEFAULT_SITE,
                DatacatConstants.EVIO_METADATA,
                new String[] {"FILE"},
                run
                );
    }
}
