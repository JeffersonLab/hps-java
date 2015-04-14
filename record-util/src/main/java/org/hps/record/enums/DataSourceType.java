package org.hps.record.enums;

/**
 * The type of data source that will supply events to the app.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public enum DataSourceType {

    /**
     * An ET server data source (hence no file extension).
     */
    ET_SERVER("ET Server", null),
    /**
     * An EVIO data source with the "evio" file extension.
     */
    EVIO_FILE("EVIO File", "evio"),
    /**
     * An LCIO data source with the "slcio" file extension.
     */
    LCIO_FILE("LCIO File", "slcio");

    /**
     * Figure out a reasonable data source type for the path string.
     * <p>
     * This defaults to an ET source ((perhaps unreasonably!) if the file extension is unrecognized.
     *
     * @param path The data source path.
     * @return The data source type.
     */
    // FIXME: Probably this should throw an error if the extension is unrecognized.
    public static DataSourceType getDataSourceType(final String path) {
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

    /**
     * The description of the data source.
     */
    private String description;

    /**
     * The extension associated with the type.
     */
    private String extension;

    /**
     * Class constructor, which takes a description and file extension.
     *
     * @param description the description of the data source type
     * @param extension the associated file extension
     */
    private DataSourceType(final String description, final String extension) {
        this.description = description;
    }

    /**
     * Get the description of the data source type.
     *
     * @return the description of the data source type
     */
    public String description() {
        return this.description;
    }

    /**
     * Get the extension associated with this data source type.
     *
     * @return the file extension of the data source type
     */
    public String getExtension() {
        return this.extension;
    }

    /**
     * Return <code>true</code> if the source is file-based (e.g. not an ET server).
     *
     * @return <code>true</code> if the source is file-based
     */
    public boolean isFile() {
        return this.ordinal() > ET_SERVER.ordinal();
    }
}
