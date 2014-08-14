package org.hps.conditions.svt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * This is a simple class for getting an SVT XML configuration file.
 */
public class SvtConfiguration extends AbstractConditionsObject {
            
    public static class SvtConfigurationCollection extends ConditionsObjectCollection<SvtConfiguration> {
    }

    /**
     * Get the filename associated with this configuration.
     * @return The filename associated with the configuration.
     */
    public String getFileName() {
        return getFieldValue("filename");
    }
        
    /**
     * Convert the raw database field value for the configuration into an XML document.
     * @return The Document created from the raw data.
     * @throws IOException 
     * @throws JDOMException
     */
    public Document createDocument() throws IOException, JDOMException {
        byte[] bytes = getFieldValue("content");
        InputStream inputStream = new ByteArrayInputStream(bytes);
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        return builder.build(inputStream);
    }
}
