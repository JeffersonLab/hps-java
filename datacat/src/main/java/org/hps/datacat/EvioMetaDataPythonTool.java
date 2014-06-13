package org.hps.datacat;

import java.util.Map;

/**
 * This is a simple wrapper of the {@link EvioMetaDataExtractor} to
 * print out a string that can be imported into Python as a dictionary.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EvioMetaDataPythonTool {
    
    public static void main(String[] args) {
        if (args.length == 0)
            throw new IllegalArgumentException("Path to file is required.");
        String path = args[0];        
        EvioMetaDataExtractor extractor = new EvioMetaDataExtractor();
        Map<String,Object> metaData = extractor.getMetaData(path);
        System.out.println(MetaDataUtil.toPythonDict(metaData));
    }
    
}
