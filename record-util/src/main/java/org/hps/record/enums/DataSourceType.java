package org.hps.record.enums;

/**
 * The type of data source that will supply events to the app.
 */
public enum DataSourceType {

    ET_SERVER("ET Server"), 
    EVIO_FILE("EVIO File"), 
    LCIO_FILE("LCIO File");

    String description;

    /**
     * Constructor which takes a description.
     * @param description The description of the data source type.
     */
    private DataSourceType(String description) {
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
     * True if the source is file-based 
     * (e.g. not an ET server).
     * @return
     */
    public boolean isFile() {
        return this.ordinal() > ET_SERVER.ordinal();
    }
}
