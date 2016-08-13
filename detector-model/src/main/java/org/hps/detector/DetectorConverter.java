package org.hps.detector;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.spi.ServiceRegistry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManagerImplementation;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.geometry.compact.converter.Converter;
import org.lcsim.util.loop.DummyConditionsConverter;
import org.lcsim.util.loop.DummyDetector;

/**
 * A rewrite of the LCSim detector converter that supports setting of the run number
 * for conditions system initialization so that database conditions are available.
 * 
 * @author jeremym
 */
public class DetectorConverter {
    
    private static Options OPTIONS = new Options();
    static {
        OPTIONS.addOption(new Option("i", "input-file", true, "input compact.xml file"));
        OPTIONS.getOption("i").setRequired(true);
        OPTIONS.addOption(new Option("o", "ouput-file", true, "output file"));
        OPTIONS.getOption("o").setRequired(true);
        OPTIONS.addOption(new Option("f", "format", true, "output format (lcdd, etc.)"));
        OPTIONS.addOption(new Option("r", "run-number", true, "run number for conditions initialization"));
    }
    
    private DefaultParser parser = new DefaultParser();
    private File inputFile = null;
    private File outputFile = null;
    private String outputFormat = null;
    private Integer runNumber = null;
    
    public static void main(String args[]) throws Exception {
        DetectorConverter cnv = new DetectorConverter();
        cnv.run(args);
    }
    
    private static List<Converter> getConverters() {
        Iterator<Converter> iter = getServices(Converter.class);
        List<Converter> result = new ArrayList<Converter>();
        while (iter.hasNext()) result.add(iter.next());
        return result;
    }
    
    private static <T> Iterator<T> getServices(Class<T> providerClass) {
       return ServiceRegistry.lookupProviders(providerClass, DetectorConverter.class.getClassLoader());
    }
        
    private void run(String args[]) throws Exception {
        CommandLine cl = parser.parse(OPTIONS, args);
        if (!cl.hasOption("i")) {
            throw new RuntimeException("Missing required -i arg.");
        }
        if (!cl.hasOption("o")) {
            throw new RuntimeException("Missing required -o arg.");
        }        
        inputFile = new File(cl.getOptionValue("i"));
        outputFile = new File(cl.getOptionValue("o"));
        if (cl.hasOption("f")) {
            outputFormat = cl.getOptionValue("f");
        }
        if (cl.hasOption("r")) {
            runNumber = Integer.parseInt(cl.getOptionValue("r"));
        } else {
            runNumber = 0;
        }
        
        Converter cnv = null;
        
        if (outputFormat != null) {
            for (Converter c : getConverters()) {
                if (c.getOutputFormat().equalsIgnoreCase(outputFormat)) {
                    cnv = c;
                    break;
                }
            }
        } else {
            for (Converter c : getConverters()) {
               if (c.getFileFilter().accept(outputFile))
               {
                  cnv = c;
                  break;
               }
            }
        }
        if (cnv == null) throw new IllegalArgumentException("No converter found for format: " + outputFormat);
        
        if (runNumber != null) {
            String name = "DUMMY";            
            DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
            ConditionsReader dummyReader = ConditionsReader.createDummy();
            ((ConditionsManagerImplementation) mgr).setConditionsReader(dummyReader, name);
            DummyDetector detector = new DummyDetector(name);
            mgr.registerConditionsConverter(new DummyConditionsConverter(detector));
            mgr.setDetector(name, runNumber);
        }
        
        InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
        cnv.convert(inputFile.getCanonicalPath(), in, out);
        in.close();
        out.close();
    }                  
}
