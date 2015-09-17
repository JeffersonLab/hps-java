package org.hps.datacat.client;


/**
 * Dataset file formats for HPS.
 * 
 * @author Jeremy McCormick, SLAC
 */
public enum DatasetFileFormat {

    /**
     * EVIO data format.
     */
    EVIO(),
    /**
     * LCIO data format (note custom file extension).
     */
    LCIO("slcio"),
    /**
     * ROOT files.
     */
    ROOT(),
    /**
     * AIDA files.
     */
    AIDA(),
    /**
     * Testing only (do not use in production).
     */
    TEST(null);
                
    /**
     * The file extension of the format.
     */
    private String extension;
    
    /**
     * Create a file format with an extension.
     * 
     * @param extension the file's extension
     */
    private DatasetFileFormat(String extension) {
        this.extension = extension;
    }
    
    /**
     * Create a file format with default extension (lower case of enum name).
     */
    private DatasetFileFormat() {
        this.extension = this.name().toLowerCase();
    }
    
    /**
     * Get the format's file extension.
     * 
     * @return the format file extension
     */
    public String extension() {
        return extension;
    }        
}
