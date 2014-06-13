package org.hps.datacat;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

public class MetaDataUtil {

    static String toString(Map<String,Object> metaData) {        
        StringBuffer buffer = new StringBuffer();
        for (Entry<String,Object> entry : metaData.entrySet()) {
            buffer.append(entry.getKey() + "=");
            if (entry.getValue() instanceof String || entry.getValue() instanceof Date) {
                buffer.append("\"" + entry.getValue() + "\" ");                
            } else {
                buffer.append(entry.getValue() + " ");
            }
        }
        buffer.setLength(buffer.length() - 1);
        return buffer.toString();
    }
    
    static String toPythonDict(Map<String,Object> metaData) {
        StringBuffer buffer = new StringBuffer();
        buffer.append('{');
        for (Entry<String,Object> entry : metaData.entrySet()) {
            String key = entry.getKey();
            Object object = entry.getValue();
            buffer.append("'");
            buffer.append(key);
            buffer.append("'");
            buffer.append(": ");
            if (object instanceof String || object instanceof Date)
                buffer.append("'");
            buffer.append(object);
            if (object instanceof String || object instanceof Date)
                buffer.append("'");
            buffer.append(", ");
        }
        buffer.setLength(buffer.length() - 2);
        buffer.append('}');
        return buffer.toString();
    }
}
