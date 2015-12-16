package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.srs.datacat.client.Client;
import org.srs.datacat.client.ClientBuilder;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.Provider;

/**
 * Datacat helper functions for the crawler.
 *
 * @author Jeremy McCormick, SLAC
 */
class DatacatHelper {
    
    /*
     * Default base URL for datacat.
     */
    static final String DATACAT_URL = "http://hpsweb.jlab.org/datacat/r";

    /*
     * Static map of strings to file formats.
     */
    private static final Map<String, FileFormat> formatMap = new HashMap<String, FileFormat>();
    static {
        for (final FileFormat format : FileFormat.values()) {
            formatMap.put(format.extension(), format);
        }
    }
    
    /* 
     * System metadata fields. 
     */
    private static final Set<String> SYSTEM_METADATA = new HashSet<String>();
    static {
        SYSTEM_METADATA.add("eventCount");
        SYSTEM_METADATA.add("size");
        SYSTEM_METADATA.add("runMin");
        SYSTEM_METADATA.add("runMax");
        SYSTEM_METADATA.add("checksum");
        SYSTEM_METADATA.add("scanStatus");
    }
   
    /**
     * Create metadata for a file using its specific reader.
     *
     * @param file the file
     * @return the metadata for the file
     */
    static Map<String, Object> createMetadata(final File file) {
        File actualFile = file;
        if (FileUtilities.isMssFile(file)) {
            actualFile = FileUtilities.getCachedFile(file);
        }
        final FileFormat fileFormat = DatacatHelper.getFileFormat(file);
        final DataType dataType = DatacatHelper.getDataType(file);
        final FileMetadataReader reader = DatacatHelper.getFileMetaDataReader(fileFormat, dataType);
        if (reader == null) {
            throw new RuntimeException("No metadata reader found for format " + fileFormat.name() + " and type "
                    + dataType.name() + ".");
        }
        Map<String, Object> metadata;
        try {
            metadata = reader.getMetadata(actualFile);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return metadata;
    }

    /**
     * Get the data type for a file.
     *
     * @param file the file
     * @return the file's data type
     */
    static DataType getDataType(final File file) {
        final FileFormat fileFormat = getFileFormat(file);
        DataType dataType = null;
        if (fileFormat == null) {
            throw new IllegalArgumentException("File has unknown format: " + file.getAbsolutePath());
        }
        if (fileFormat.equals(FileFormat.EVIO)) {
            dataType = DataType.RAW;
        } else if (fileFormat.equals(FileFormat.LCIO)) {
            dataType = DataType.RECON;
        } else if (fileFormat.equals(FileFormat.ROOT)) {
            if (file.getName().contains("_dqm")) {
                dataType = DataType.DQM;
            } else if (file.getName().contains("_dst")) {
                dataType = DataType.DST;
            }
        } else if (fileFormat.equals(FileFormat.AIDA)) {
            dataType = DataType.DQM;
        }
        if (dataType == null) {
            throw new IllegalArgumentException("Could not determine data type for format: " + fileFormat.name());
        }
        return dataType;
    }

    /**
     * Get the file format of a file.
     *
     * @param pathname the file
     * @return the file format of the file
     */
    static FileFormat getFileFormat(final File pathname) {
        String name = pathname.getName();
        if (name.contains(FileFormat.EVIO.extension()) && !name.endsWith(FileFormat.EVIO.extension())) {
            name = stripEvioFileNumber(name);
        }
        final String extension = name.substring(name.lastIndexOf(".") + 1);
        return formatMap.get(extension);
    }

    /**
     * Get a metadata reader for a given combination of file format and data type.
     *
     * @param fileFormat the file format
     * @param dataType the data type
     * @return the file metadata reader
     */
    static FileMetadataReader getFileMetaDataReader(final FileFormat fileFormat, final DataType dataType) {
        FileMetadataReader reader = null;
        if (fileFormat.equals(FileFormat.LCIO)) {
            reader = new LcioReconMetadataReader();
        } else if (fileFormat.equals(FileFormat.EVIO)) {
            reader = new EvioMetadataReader();
        } else if (fileFormat.equals(FileFormat.ROOT) && dataType.equals(DataType.DST)) {
            reader = new RootDstMetadataReader();
        } else if (fileFormat.equals(FileFormat.ROOT) && dataType.equals(DataType.DQM)) {
            reader = new RootDqmMetadataReader();
        } else if (fileFormat.equals(FileFormat.AIDA)) {
            reader = new AidaMetadataReader();
        }
        return reader;
    }

    /**
     * Strip the file number from an EVIO file name.
     *
     * @param name the EVIO file name
     * @return the file name stripped of the file number
     */
    static String stripEvioFileNumber(final String name) {
        String strippedName = name;
        if (!name.endsWith(FileFormat.EVIO.extension())) {
            strippedName = name.substring(0, name.lastIndexOf("."));
        }
        return strippedName;
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
    static DatasetModel createDataset(
            File file,
            Map<String, Object> metadata,
            String folder,
            String site,
            String dataType,
            String fileFormat) {
        
        Provider provider = new Provider();        
        List<DatasetModel> datasets = new ArrayList<DatasetModel>();
                              
        Dataset.Builder datasetBuilder = provider.getDatasetBuilder();
        datasetBuilder.versionId(1);
        datasetBuilder.master(true);
        datasetBuilder.name(file.getName());
        datasetBuilder.resource(file.getPath());
        datasetBuilder.size((Long) metadata.get("size"));
        datasetBuilder.scanStatus("OK");
        datasetBuilder.dataType(dataType);
        datasetBuilder.fileFormat(fileFormat);
        datasetBuilder.site(site);
        if (metadata.get("eventCount") != null) {
            datasetBuilder.eventCount((Long) metadata.get("eventCount"));
        }
        datasetBuilder.runMin((Long) metadata.get("runMin"));
        datasetBuilder.runMax((Long) metadata.get("runMax"));
        datasetBuilder.checksum((String) metadata.get("checksum"));
                        
        // Create user metadata leaving out system metadata fields.
        Map<String, Object> userMetadata = new HashMap<String, Object>();
        for (Entry<String, Object> metadataEntry : metadata.entrySet()) {
            if (!SYSTEM_METADATA.contains(metadataEntry.getKey())) {
                userMetadata.put(metadataEntry.getKey(), metadataEntry.getValue());
            }
        }
        datasetBuilder.versionMetadata(userMetadata);
        
        // Build dataset and add to list.
        DatasetModel dataset = datasetBuilder.build();
        datasets.add(dataset);
        
        return dataset;
    }
    
    /**
     * Create datasets from a list of files.
     * 
     * @param files the list of files
     * @return the list of datasets
     */
    static List<DatasetModel> createDatasets(List<File> files, String folder, String site) {
        List<DatasetModel> datasets = new ArrayList<DatasetModel>();
        for (File file : files) {
            Map<String, Object> metadata = createMetadata(file);
            DataType dataType = DatacatHelper.getDataType(file);
            FileFormat fileFormat = DatacatHelper.getFileFormat(file);
            DatasetModel dataset = DatacatHelper.createDataset(
                    file,
                    metadata, 
                    folder, 
                    site,
                    dataType.toString(),
                    fileFormat.toString());
            datasets.add(dataset);
        }
        return datasets;
    }
    
    /**
     * Add datasets to the data catalog.
     * 
     * @param datasets the list of datasets
     * @param folder the target folder
     * @param url the datacat URL
     */
    static void addDatasets(List<DatasetModel> datasets, String folder, String url) {
        Client client = null;
        try {
            client = new ClientBuilder().setUrl(url).build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Bad datacat URL.", e);
        }
        for (DatasetModel dataset : datasets) {
            client.createDataset(folder, dataset);
        }
    }
}
