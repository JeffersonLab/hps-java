package org.hps.crawler;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.datacat.client.DatacatClient;
import org.hps.datacat.client.DatacatClientFactory;
import org.hps.datacat.client.DatasetDataType;
import org.hps.datacat.client.DatasetFileFormat;
import org.hps.datacat.client.DatasetSite;
import org.hps.record.evio.EvioFileMetadata;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Utility for inserting EVIO files into the SRS data catalog.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EvioDatacatUtilities {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = LogUtil.create(EvioDatacatUtilities.class, new DefaultLogFormatter(), Level.FINE);

    /**
     * Add an EVIO to the data catalog.
     *
     * @param client the data catalog client
     * @param folder the folder name e.g. "data/raw"
     * @param evioFile the evio file to add to the data catalog
     * @param evioMetadata the EVIO file's meta data
     * @return the HTTP response code
     */
    static int addEvioFile(final DatacatClient client, final String folder, final EvioFileMetadata evioMetadata) {

        // Create metadata map for adding dataset.
        final Map<String, Object> metadataMap = createMetadataMap(evioMetadata);
        
        // Get the EVIO file.
        File evioFile = evioMetadata.getEvioFile();

        // Add the dataset to the data catalog using the REST API.
        final int response = client.addDataset(folder, DatasetDataType.RAW, evioFile.getPath(), DatasetSite.SLAC,
                DatasetFileFormat.EVIO, evioFile.getName(), metadataMap);

        return response;
    }

    /**
     * Add a list of EVIO files to the data catalog.
     *
     * @param evioFiles the list of EVIO files
     * @param folder the folder in the data catalog
     */
    static void addEvioFiles(List<EvioFileMetadata> metadataList, final String folder) {
        LOGGER.info("adding " + metadataList.size() + " EVIO files to data catalog in folder " + folder);
        final DatacatClientFactory datacatFactory = new DatacatClientFactory();
        final DatacatClient datacatClient = datacatFactory.createClient();
        for (EvioFileMetadata metadata : metadataList) {
            LOGGER.info("adding " + metadata.getEvioFile().getPath() + " to data catalog");
            EvioDatacatUtilities.addEvioFile(datacatClient, folder, metadata);
        }
    }
    
    /**
     * Create a map of metadata keys and values suitable for making a new dataset.
     * 
     * @param evioMetadata the EVIO metadata object
     * @return the metadata map
     */
    static Map<String, Object> createMetadataMap(EvioFileMetadata evioMetadata) {
        final Map<String, Object> metadataMap = new HashMap<String, Object>();
        metadataMap.put("runMin", evioMetadata.getRun());
        metadataMap.put("runMax", evioMetadata.getRun());
        metadataMap.put("eventCount", evioMetadata.getEventCount());
        metadataMap.put("size", evioMetadata.getByteCount());
        metadataMap.put("fileNumber", evioMetadata.getSequence());
        metadataMap.put("badEventCount", evioMetadata.getBadEventCount());
        metadataMap.put("endTimestamp", evioMetadata.getEndDate().getTime());
        metadataMap.put("startTimestamp", evioMetadata.getStartDate().getTime());
        metadataMap.put("startEvent", evioMetadata.getStartEvent());
        metadataMap.put("endEvent", evioMetadata.getEndEvent());
        metadataMap.put("hasEnd", evioMetadata.hasEnd() ? 1 : 0);
        metadataMap.put("hasPrestart", evioMetadata.hasPrestart() ? 1 : 0);
        return metadataMap;
    }

    /**
     * Class constructor which is private.
     */
    private EvioDatacatUtilities() {
        throw new RuntimeException("Do not instantiate this class.");
    }
}
