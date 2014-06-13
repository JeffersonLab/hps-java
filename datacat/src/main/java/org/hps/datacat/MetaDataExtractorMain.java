package org.hps.datacat;

import java.util.ArrayList;
import java.util.List;

/**
 * This class has a main method which will print out the meta data 
 * information for a file given as a command line argument.  The printed
 * information is suitable for passing directly as an argument to the Python script
 * that is used to register new data files.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class MetaDataExtractorMain {
    
    static List<MetaDataExtractor> metaDataExtractors = new ArrayList<MetaDataExtractor>();
    
    static {
        metaDataExtractors.add(new LcioMetaDataExtractor());
        metaDataExtractors.add(new EvioMetaDataExtractor());
    }
    
    static MetaDataExtractor findMetaDataExtractor(String path) {
        String extension = path.substring(path.lastIndexOf(".") + 1);
        //System.out.println("extension: " + extension);
        for (MetaDataExtractor extractor : metaDataExtractors) {
            if (extractor.handlesExtension().equals(extension))
                return extractor;
        }
        return null;
    }
    
    public static void main(String[] args) {
        if (args.length == 0)
            throw new IllegalArgumentException("Path to file is required.");
        String path = args[0];
        MetaDataExtractor e = findMetaDataExtractor(path);
        if (e == null) {
            System.err.println("Unknown extension on file: " + path);
            throw new IllegalArgumentException("No handler found for file.");
        }
        System.out.println(MetaDataUtil.toString(e.getMetaData(path)));
    }
}
