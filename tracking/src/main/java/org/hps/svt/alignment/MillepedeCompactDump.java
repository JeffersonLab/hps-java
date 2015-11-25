package org.hps.svt.alignment;

/**
 * Class building a new compact.xml detector based on MillepedeII input corrections
 * @author phansson
 * created on 1/15/2014
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


public class MillepedeCompactDump {

	private static String detectorName = "Tracker";



    private static Options createCmdLineOpts() {
		Options options = new Options();
		options.addOption(new Option("c",true,"The path to the compact xml file."));
		options.addOption(new Option("o",true,"The name of the output text file."));
		return options;
	}
	
	private static void printHelpAndExit(Options options) {
		HelpFormatter help = new HelpFormatter();
		help.printHelp(" ", options);
		System.exit(1);
	}
	
		
	public static void main(String[] args) {

		// Setup command line input
		Options options = createCmdLineOpts();
		if (args.length == 0) {
			printHelpAndExit(options);
		}

		CommandLineParser parser = new DefaultParser();
		CommandLine cl = null;
		try {
			cl = parser.parse(options, args);
		} catch (ParseException e) {
			throw new RuntimeException("Problem parsing command line options.",e);
		}
		
		String compactFilename = null;  
		if(cl.hasOption("c")) {
			compactFilename = cl.getOptionValue("c");
		} else {
			printHelpAndExit(options);
		}
		
		String outputFilename = "millepede_dump.txt";// + compactFilename.replace(".xml", ".txt");
		if(cl.hasOption("o")) {
			outputFilename = cl.getOptionValue("o");
		}
		
        PrintWriter outputPrintWriter = null;
		try {
             outputPrintWriter
               = new PrintWriter(new BufferedWriter(new FileWriter(outputFilename)));
        } catch (IOException e) {
            e.printStackTrace();
        }
		 
		
		
		
		File compactFile = new File(compactFilename);
		
		// read XML
		SAXBuilder builder = new SAXBuilder();
		Document compact_document = null;
		try {
			compact_document = (Document) builder.build(compactFile);
		} catch (JDOMException | IOException e1) {
			throw new RuntimeException("problem with JDOM ", e1);
		}
		
		

		Element rootNode = compact_document.getRootElement();
		
		// find the constants needed to calculate the final millepede parameters
		List<Element> definitions = rootNode.getChildren("define");
		for(Element definition : definitions) {
		    List<Element> constants = definition.getChildren("constant");
		    
		}
        
		
		
		// find the millepede constants
		List<Element> mpConstants = null;
		List<Element> detectors = rootNode.getChildren("detectors");
		for(Element detectorsNode : detectors) {
		    List<Element> detectorNode = detectorsNode.getChildren("detector");
		    if(detectorNode!=null) {
		        System.out.println(detectorNode.size() + " detectors");
		        for(Element detector : detectorNode) {
		            if(detector.getAttribute("name")!=null) {
		                if(detector.getAttributeValue("name").compareTo(detectorName)==0 ) {
		                    System.out.println("Found " + detectorName);
		                    
		                    Element element_constants = detector.getChild("millepede_constants");
		                    if(element_constants==null) {
		                        throw new RuntimeException("no alignment constants in this compact file.");
		                    }
		                    mpConstants = element_constants.getChildren("millepede_constant");

		                           
		                }
		            } else {
		                throw new RuntimeException("this detector node element is not formatted correctly");
		            }
		        }
		    } else {
		        throw new RuntimeException("this detector node element is not formatted correctly");
		    }
		}
        System.out.println("Found " + mpConstants.size() + " constants" );
        for(Element element : mpConstants) {
            String name = element.getAttributeValue("name");
            String value = element.getAttributeValue("value");
            String s = name + "," + value;
            System.out.println(s);
            outputPrintWriter.println(s);
        }
        

		outputPrintWriter.close();
		
		
		
		
	}



	

	
	
	

}
