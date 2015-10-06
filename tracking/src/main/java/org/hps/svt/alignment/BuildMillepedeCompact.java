package org.hps.svt.alignment;

/**
 * Class building a new compact.xml detector based on MillepedeII input corrections
 * @author phansson
 * created on 1/15/2014
 */

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.recon.tracking.CoordinateTransformations;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.GeometryReader;
import org.lcsim.geometry.compact.converter.MilleParameter;
import org.lcsim.util.xml.ElementFactory.ElementCreationException;


public class BuildMillepedeCompact {

	private static String detectorName = "Tracker";
	private static boolean replaceConstant = false;
    private static boolean calcNewValue = true;



    private static Options createCmdLineOpts() {
		Options options = new Options();
		options.addOption(new Option("c",true,"The path to the compact xml file."));
		options.addOption(new Option("o",true,"The name of the new compact xml file."));
		options.addOption(new Option("r", false, "Replace correction instead of adding to it."));
        options.addOption(new Option("t", false, "Add a text string as a new value instead of adding to it."));
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

		CommandLineParser parser = new PosixParser();
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
		
		String compactFilenameNew = "compact_new.xml";
		if(cl.hasOption("o")) {
			compactFilenameNew = cl.getOptionValue("o");
		}
		
		
		
		if(cl.hasOption("r")) {
		    replaceConstant = true;
		}

		if(cl.hasOption("t")) {
            calcNewValue = false;
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
		
		
		
		// Loop over all millepede input files and build a list of parameters
		
		List<MilleParameter> params = new ArrayList<MilleParameter>();
		
		FileInputStream inMille = null;
		BufferedReader brMille = null;
		try {
			for(String milleFilename : cl.getArgs()) {
				inMille = new FileInputStream(milleFilename);
				brMille = new BufferedReader(new InputStreamReader(inMille));
				String line;
				while((line = brMille.readLine()) != null) {
					//System.out.printf("%s\n",line);
					if(!line.contains("Parameter") && !line.contains("!")) {
						
						MilleParameter par = new MilleParameter(line);
						//System.out.println(par.getXMLName() + " " + par.getValue());
						
						//add the parameter
						params.add(par);
						
					}
				}
				brMille.close();
			}
		}
		catch (IOException e) {
			throw new RuntimeException("problem reading mille file",e);
		}
		
		System.out.printf("Found %d millepede parameters\n ", params.size());
		
		

		Element rootNode = compact_document.getRootElement();
		List<Element> detectors = rootNode.getChildren("detectors");
		for(Element detectorsNode : detectors) {
		    List<Element> detectorNode = detectorsNode.getChildren("detector");
		    if(detectorNode!=null) {
		        System.out.println(detectorNode.size() + " detectors");
		        for(Element detector : detectorNode) {
		            if(detector.getAttribute("name")!=null) {
		                if(detector.getAttributeValue("name").compareTo(detectorName)==0 ) {
		                    System.out.println("Found " + detectorName);
		                    for(MilleParameter p : params) {
		                        Element node = findMillepedeConstantNode(detector,Integer.toString(p.getId()));
		                        if(node!=null) {

                                    double correction = p.getValue();

		                            // have the option of adding a text value to the compact instead of actually computing the new value
		                            if(calcNewValue) {

		                                double oldValue = 0;
		                                try {
		                                    oldValue = node.getAttribute("value").getDoubleValue();
		                                } catch (DataConversionException e) {
		                                    e.printStackTrace();
		                                }
		                                double newValue;
		                                if(replaceConstant) {
		                                    newValue = correction;
		                                } else {
		                                    if (p.getType() == MilleParameter.Type.ROTATION.getType()) {
		                                        newValue = oldValue - correction;
		                                    } else {
		                                        newValue = oldValue + correction;
		                                    }
		                                }
		                                System.out.println("Update " + p.getId() + ": " + oldValue + " (corr. " + correction + ") ->  "  + newValue );
		                                node.setAttribute("value", String.format("%.6f",newValue));
		                            
		                            } else {
		                                
		                                String oldValue = node.getAttribute("value").getValue();
		                                
		                                if(replaceConstant)
		                                    throw new RuntimeException("Doesn't make sense to try and replace with the string option?");

		                                if( correction != 0.0) {
		                                    String newValue = oldValue + " + " + String.format("%.6f",correction);
		                                    System.out.println("Update " + p.getId() + ": " + oldValue + " (corr. " + correction + ") ->  "  + newValue );
		                                    node.setAttribute("value", newValue);
		                                }		                                
		                            }
		                            
		                        } else {
		                            throw new RuntimeException("no element found for " + p.getId() + " check format of compact file");
		                        }
		                    }       
		                }
		            } else {
		                throw new RuntimeException("this detector node element is not formatted correctly");
		            }
		        }
		    } else {
		        throw new RuntimeException("this detector node element is not formatted correctly");
		    }
		}


		// Save new XML file
		
		XMLOutputter xmlOutput = new XMLOutputter();
		// display nice 
		//xmlOutput.setFormat(Format.getPrettyFormat());
		try {
			xmlOutput.output(compact_document, new FileWriter(compactFilenameNew));
		} catch (IOException e) {
			throw new RuntimeException("problem with xml output",e);
		}
			
		
		
		
		
	}


	private static Element findMillepedeConstantNode(Element detector, String name) {
	    Element element_constants = detector.getChild("millepede_constants");
	    if(element_constants==null) {
	        throw new RuntimeException("no alignment constants in this xml file.");
	    }
	    List<Element> list = element_constants.getChildren("millepede_constant");
	    for(Element element : list) {
	        if(element.getAttribute("name")!=null) {
	            if(element.getAttributeValue("name").compareTo(name) == 0) {
	                return element;
	            } 
	        } else {
	            throw new RuntimeException("this element is not formatted correctly");
	        }
	    }
	    return null;
	}

	

	
	
	

}
