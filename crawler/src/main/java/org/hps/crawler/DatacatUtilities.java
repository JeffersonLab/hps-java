package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hps.datacat.client.DatacatClient;
import org.hps.datacat.client.DatasetDataType;
import org.hps.datacat.client.DatasetFileFormat;
import org.hps.datacat.client.DatasetSite;

/**
 * Datacat utilities for the crawler.
 *
 * @author Jeremy McCormick, SLAC
 */
class DatacatUtilities {

    static Map<String, DatasetFileFormat> formatMap = new HashMap<String, DatasetFileFormat>();
    static {
        for (final DatasetFileFormat format : DatasetFileFormat.values()) {
            formatMap.put(format.extension(), format);
        }
    }

    static void addFile(final DatacatClient datacatClient, final String folder, final File file,
            final Map<String, Object> metadata) {
        final DatasetFileFormat fileFormat = DatacatUtilities.getFileFormat(file);
        final DatasetDataType dataType = DatacatUtilities.getDataType(file);
        DatacatUtilities.addFile(datacatClient, folder, file, metadata, fileFormat, dataType, DatasetSite.SLAC);
    }

    /**
     * Add a file to the data catalog.
     *
     * @param client the data catalog client
     * @param folder the folder name e.g. "data/raw"
     * @param fileMetadata the file's meta data including the path
     * @param fileFormat the file's format (EVIO, LCIO etc.)
     * @param dataType the file's data type (RAW, RECON, etc.)
     * @return the HTTP response code
     */
    static int addFile(final DatacatClient client, final String folder, final File file,
            final Map<String, Object> metadata, final DatasetFileFormat fileFormat, final DatasetDataType dataType,
            final DatasetSite site) {

        // Add the dataset to the data catalog using the REST API.
        final int response = client.addDataset(folder, dataType, file.getAbsolutePath(), site, fileFormat,
                file.getName(), metadata);

        return response;
    }

    static Map<String, Object> createMetadata(final File file) {
        final DatasetFileFormat fileFormat = DatacatUtilities.getFileFormat(file);
        final DatasetDataType dataType = DatacatUtilities.getDataType(file);
        final FileMetadataReader reader = DatacatUtilities.getFileMetaDataReader(fileFormat, dataType);
        if (reader == null) {
            throw new RuntimeException("No metadata reader found for format " + fileFormat.name() + " and type " + dataType.name() + ".");
        }
        Map<String, Object> metadata;
        try {
            metadata = reader.getMetadata(file);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return metadata;
    }

    static DatasetDataType getDataType(final File file) {
        final DatasetFileFormat fileFormat = getFileFormat(file);
        DatasetDataType dataType = null;
        if (fileFormat == null) {
            throw new IllegalArgumentException("File has unknown format: " + file.getAbsolutePath());
        }
        if (fileFormat.equals(DatasetFileFormat.EVIO)) {
            dataType = DatasetDataType.RAW;
        } else if (fileFormat.equals(DatasetFileFormat.LCIO)) {
            dataType = DatasetDataType.RECON;
        } else if (fileFormat.equals(DatasetFileFormat.ROOT)) {
            // FIXME: This should probably open the file and determine what it contains.
            if (file.getName().contains("_dqm")) {
                dataType = DatasetDataType.DQM;
            } else if (file.getName().contains("_dst")) {
                dataType = DatasetDataType.DST;
            }
        } else if (fileFormat.equals(DatasetFileFormat.AIDA)) {
            dataType = DatasetDataType.DQM;
        }
        if (dataType == null) {
            throw new IllegalArgumentException("Could not determine data type for format: " + fileFormat.name());
        }
        return dataType;
    }

    static DatasetFileFormat getFileFormat(final File pathname) {
        String name = pathname.getName();
        if (name.contains(DatasetFileFormat.EVIO.extension()) && !name.endsWith(DatasetFileFormat.EVIO.extension())) {
            name = stripEvioFileNumber(name);
        }
        final String extension = name.substring(name.lastIndexOf(".") + 1);
        return formatMap.get(extension);
    }

    static FileMetadataReader getFileMetaDataReader(final DatasetFileFormat fileFormat, final DatasetDataType dataType) {
        FileMetadataReader reader = null;
        if (fileFormat.equals(DatasetFileFormat.LCIO)) {
            reader = new LcioMetadataReader();
        } else if (fileFormat.equals(DatasetFileFormat.EVIO)) {
            reader = new EvioMetadataReader();
        } else if (fileFormat.equals(DatasetFileFormat.ROOT) && dataType.equals(DatasetDataType.DST)) {
            reader = new RootDstMetadataReader();
        }
        return reader;
    }

    static String getFolder(final String rootDir, final File file) {
        String stripDir = rootDir;
        if (!stripDir.endsWith("/")) {
            stripDir += "/";
        }
        final String folder = file.getParentFile().getPath().replace(stripDir, "");
        return folder;
    }

    static String stripEvioFileNumber(final String name) {
        String strippedName = name;
        if (!name.endsWith(DatasetFileFormat.EVIO.extension())) {
            strippedName = name.substring(0, name.lastIndexOf("."));
        }
        return strippedName;
    }
}
