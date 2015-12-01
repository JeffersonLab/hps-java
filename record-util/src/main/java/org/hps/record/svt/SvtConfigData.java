package org.hps.record.svt;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Represents the four SVT status banks from EVIO sync events containing XML config data.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class SvtConfigData {
        
    /**
     * Helper for ROC bank tag information and data array indices.
     */
    public enum RocTag {
        
        /** Data bank. */
        DATA(51, 0, 1),
        /** Control bank. */
        CONTROL(66, 2, 3);
        
        private int tag;
        private int configIndex;
        private int statusIndex;
                
        RocTag(int tag, int configIndex, int statusIndex) {
            this.tag = tag;
            this.configIndex = configIndex;
            this.statusIndex = statusIndex;
        }
        
        int configIndex() {
            return configIndex;
        }
        
        int statusIndex() {
            return statusIndex;
        }
        
        /**
         * Get the ROC tag from an int value.
         * 
         * @param tag the tag's int value
         * @return the matching <code>RocTag</code>
         * @throws IllegalArgumentException if <code>tag</code> is not valid
         */
        static RocTag fromTag(int tag) {
            if (tag == DATA.tag) {
                return DATA;
            } else if (tag == CONTROL.tag) {
                return CONTROL;
            } else {
                throw new IllegalArgumentException("Unknown tag " + tag + " for ROC.");
            }
        }
    }
    
    // Unix timestamp from the closest head bank.
    private int timestamp;
    
    // The config data strings.
    private String[] data = new String[4];
        
    public SvtConfigData(int timestamp) {
        this.timestamp = timestamp;
    }
    
    public void setData(RocTag rocTag, String data) {
        if (data.contains("<config>")) {
            setConfigData(rocTag, data);
        }
        if (data.contains("<status>")) {
            setStatusData(rocTag, data);
        }
    }
    
    public void setConfigData(RocTag rocTag, String configData) {
        if (rocTag.equals(RocTag.DATA)) {
            data[RocTag.DATA.configIndex()] = configData;
        } else {
            data[RocTag.CONTROL.configIndex()] = configData;
        }
    }
    
    public void setStatusData(RocTag rocTag, String statusData) {
        if (rocTag.equals(RocTag.DATA)) {
            data[RocTag.DATA.statusIndex()] = statusData;
        } else {
            data[RocTag.CONTROL.statusIndex()] = statusData;
        }
    }
    
    public String getConfigData(RocTag roc) {
        if (roc.equals(RocTag.DATA)) {
            return data[RocTag.DATA.configIndex()];
        } else {
            return data[RocTag.CONTROL.configIndex()];
        }
    }
    
    public String getStatusData(RocTag roc) {
        if (roc.equals(RocTag.DATA)) {
            return data[RocTag.DATA.statusIndex()];
        } else {
            return data[RocTag.CONTROL.statusIndex()];
        }
    }
    
    public Document toXmlDocument() {
        StringBuffer sb = new StringBuffer();
        sb.append("<svt>" + '\n');
        if (getConfigData(RocTag.DATA) != null) {
            sb.append(getConfigData(RocTag.DATA));
        } else {
            sb.append("<config/>" + '\n');
        }
        if (getStatusData(RocTag.DATA) != null) {
            sb.append(getStatusData(RocTag.DATA));
        } else {
            sb.append("<status/>" + '\n');
        }
        if (getConfigData(RocTag.CONTROL) != null) {
            sb.append(getConfigData(RocTag.CONTROL));                
        } else {
            sb.append("<config/>" + '\n');
        }
        if (getStatusData(RocTag.CONTROL) != null) {
            sb.append(getStatusData(RocTag.CONTROL));   
        } else {
            sb.append("<status/>" + '\n');
        }
        sb.append("</svt> + '\n'");
        Document document = null;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(sb.toString())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return document;
    }
    
    public String toXmlString() {
        Document document = toXmlDocument();
        String output = null;
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        } catch (Exception e) {
            throw new RuntimeException();
        }
        return output;
    }
    
    public int getTimestamp() {
        return timestamp;
    }
}
