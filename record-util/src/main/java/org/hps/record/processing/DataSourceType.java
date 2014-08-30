package org.hps.record.processing;

/**
 * The type of data source that will supply events to the app.
 */
public enum DataSourceType {

    ET_SERVER("ET Server"), 
    EVIO_FILE("EVIO File"), 
    LCIO_FILE("LCIO File");

    String description;

    private DataSourceType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
    
    public boolean isFile() {
        return this.ordinal() > ET_SERVER.ordinal();
    }
}
