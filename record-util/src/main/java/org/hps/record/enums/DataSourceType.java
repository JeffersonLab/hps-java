package org.hps.record.enums;

import java.io.File;

/**
 * The type of data source that will supply events to the app.
 */
public enum DataSourceType {

    ET_SERVER("ET Server", null), 
    EVIO_FILE("EVIO File", "evio"), 
    LCIO_FILE("LCIO File", "slcio");

    String description;
    String extension;

    /**
     * Constructor which takes a description.
     * @param description The description of the data source type.
     */
    private DataSourceType(String description, String extension) {
        this.description = description;
    }

    /**
     * Get the description of the data source type.
     * @return The description of the data source type.
     */
    public String description() {
        return description;
    }
    
    /**
     * Get the extension associated with this data source type.
     * @return The file extension of the data source type.
     */
    public String getExtension() {
        return extension;
    }
    
    /**
     * True if the source is file-based 
     * (e.g. not an ET server).
     * @return
     */
    public boolean isFile() {
        return this.ordinal() > ET_SERVER.ordinal();
    }
    
    /**
     * Figure out a reasonable data source type for the path string.
     * This defaults to an ET source ((perhaps unreasonably!) if the 
     * file extension is unrecognized.
     * @param path The data source path.
     * @return The data source type.
     */
    public static DataSourceType getDataSourceType(String path) {
        if (path.endsWith("." + DataSourceType.LCIO_FILE.getExtension())) { 
            return DataSourceType.LCIO_FILE;
        } else if (path.contains("." + DataSourceType.EVIO_FILE.getExtension())) {
            // For EVIO files, only check that the extension appears someplace in the name
            // as typically the files from the DAQ look like "file.evio.0" etc.
            return DataSourceType.EVIO_FILE;
        } else {
            return DataSourceType.ET_SERVER;
        }
    }
    
}
