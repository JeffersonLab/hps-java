package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.datacat.DataType;
import org.hps.datacat.DatacatUtilities;
import org.hps.datacat.FileFormat;
import org.srs.datacat.model.DatasetModel;

/**
 * Datacat helper functions for the crawler.
 *
 * @author Jeremy McCormick, SLAC
 */
class DatacatHelper {
    
    private static final Logger LOGGER = Logger.getLogger(DatacatHelper.class.getPackage().getName());
                 
    /**
     * Create metadata for a file using its {@link FileMetadataReader}.
     *
     * @param file the file
     * @return the metadata for the file
     */
    static Map<String, Object> createMetadata(final File file) {
        LOGGER.fine("creating metadata for " + file.getPath() + " ...");
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
        metadata.put("scanStatus", "OK");
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
        return FileFormat.findFormat(extension);
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
     * Create datasets from a list of files.
     * 
     * @param files the list of files
     * @return the list of datasets
     */
    static List<DatasetModel> createDatasets(List<File> files, String folder, String site) {
        List<DatasetModel> datasets = new ArrayList<DatasetModel>();
        DatacatUtilities util = new DatacatUtilities();
        for (File file : files) {
            Map<String, Object> metadata = createMetadata(file);
            DataType dataType = DatacatHelper.getDataType(file);
            FileFormat fileFormat = DatacatHelper.getFileFormat(file);
            DatasetModel dataset = util.createDataset(
                    file,
                    metadata,
                    folder,
                    dataType.toString(),
                    fileFormat.toString());
            datasets.add(dataset);
        }
        return datasets;
    }    
}
