package org.hps.datacat;

import java.util.HashMap;
import java.util.Map;


/**
 * Dataset file formats for HPS.
 * 
 * @author jeremym
 */
public enum FileFormat {

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
    
    private static final Map<String, FileFormat> FORMAT_EXTENSIONS = new HashMap<String, FileFormat>();
    static {
        for (final FileFormat format : FileFormat.values()) {
            FORMAT_EXTENSIONS.put(format.extension(), format);
        }
    }
            
    /**
     * The file extension of the format.
     */
    private String extension;
    
    /**
     * Create a file format with an extension.
     * 
     * @param extension the file's extension
     */
    private FileFormat(String extension) {
        this.extension = extension;
    }
    
    /**
     * Create a file format with default extension (lower case of enum name).
     */
    private FileFormat() {
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
    
    /**
     * Find format by its extension.
     * @param extension
     * @return the format for the extension or <code>null</code> if does not exist
     */
    public static FileFormat findFormat(String extension) {
        return FORMAT_EXTENSIONS.get(extension);
    }
}
