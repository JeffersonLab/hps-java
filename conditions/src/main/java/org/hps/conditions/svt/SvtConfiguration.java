package org.hps.conditions.svt;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * This class is the conditions object model for an SVT configuration saved into the database.
 */
@Table(names = {"svt_configurations"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_UPDATED)
public final class SvtConfiguration extends BaseConditionsObject {

    /**
     * Collection implementation for {@link SvtConfiguration} objects.
     */
    @SuppressWarnings("serial")
    public static class SvtConfigurationCollection extends BaseConditionsObjectCollection<SvtConfiguration> {
    }

    /**
     * Convert the raw database content for the configuration into an XML document.
     *
     * @return The Document created from the raw data.
     * @throws IOException if there is an IO error
     * @throws JDOMException is there is an XML parsing error
     */
    public Document createDocument() throws IOException, JDOMException {
        final byte[] bytes = this.getFieldValue("content");
        final InputStream inputStream = new ByteArrayInputStream(bytes);
        final SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        return builder.build(inputStream);
    }

    /**
     * Get the content of the XML file as a byte array.
     *
     * @return the content of the XML file as a byte array
     */
    @Field(names = {"content"})
    public byte[] getContent() {
        return this.getFieldValue("content");
    }

    /**
     * Get the filename associated with this configuration.
     *
     * @return The filename associated with the configuration.
     */
    @Field(names = {"filename"})
    public String getFileName() {
        return this.getFieldValue("filename");
    }

    /**
     * Save this configuration to a local file on disk using its name from the database.
     */
    public void writeToFile() {
        final XMLOutputter out = new XMLOutputter();
        out.setFormat(Format.getPrettyFormat());
        try {
            out.output(this.createDocument(), new FileWriter(this.getFileName()));
        } catch (IOException | JDOMException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Save this configuration to a local file on disk.
     *
     * @param filename the name of the local file
     */
    public void writeToFile(final String filename) {
        final XMLOutputter out = new XMLOutputter();
        out.setFormat(Format.getPrettyFormat());
        try {
            out.output(this.createDocument(), new FileWriter(filename));
        } catch (IOException | JDOMException e) {
            throw new RuntimeException(e);
        }
    }
}
