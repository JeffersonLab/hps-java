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
import org.hps.conditions.deprecated.HPSSVTSensorSetup;
import org.hps.conditions.deprecated.SvtUtils;
import org.hps.recon.tracking.CoordinateTransformations;
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


public class BuildCompact {

	private static int runNumber = -1; //1351;
	private static String detectorName = ""; //"HPS-TestRun-v7";
	private static ConditionsManager conditionsManager = null;
	
	private static Options createCmdLineOpts() {
		Options options = new Options();
		options.addOption(new Option("c",true,"The path to the compact xml file."));
		options.addOption(new Option("o",true,"The name of the new compact xml file."));
		options.addOption(new Option("d",true,"Detector name."));
		options.addOption(new Option("r",true,"Run number."));
		
		return options;
	}
	
	private static void printHelpAndExit(Options options) {
		HelpFormatter help = new HelpFormatter();
		help.printHelp(" ", options);
		System.exit(1);
	}
	
	//private static buildDetector()

	
	
	
	
	
	private static class MilleParameterSet {
		private IDetectorElement _det = null;
		 List<MilleParameter> params = new ArrayList<MilleParameter>();
		public MilleParameterSet(IDetectorElement d) {
			setDetector(d);
		}
		public void setDetector(IDetectorElement d) {
			_det = d;
		}
		public IDetectorElement getDetector() {
			return _det;
		}
		public void add(MilleParameter par) {
			params.add(par);
		}
		public Hep3Vector getLocalTranslation() {
			Map<String,Double> m = new HashMap<String,Double>();
			for(MilleParameter p : params) {
				if (p.getType() == 1) {
					if (p.getDim() == 1) m.put("u", p.getValue());
					else if(p.getDim() == 2) m.put("v", p.getValue());
					else m.put("w", p.getValue());
				}
			}
			if(m.size() != 3) {
				System.out.println("bad trans!!");
				System.exit(1);
			}
			return new BasicHep3Vector(m.get("u"),m.get("v"),m.get("w"));
			 
		}
		public Hep3Vector getLocalRotation() {
			Map<String,Double> m = new HashMap<String,Double>();
			for(MilleParameter p : params) {
				if (p.getType() == 2) {
					if (p.getDim() == 1) m.put("alpha",p.getValue());
					else if(p.getDim() == 2) m.put("beta", p.getValue());
					else m.put("gamma", p.getValue());
				}
			}
			if(m.size() != 3) {
				System.out.println("bad rot!!");
				System.exit(1);
			}
			return new BasicHep3Vector(m.get("alpha"),m.get("beta"),m.get("gamma"));
		}
		public Hep3Vector getGlobalTranslation() {
			ITransform3D localToGlobal = getLocalToGlobal();
			return localToGlobal.getRotation().rotated(getLocalTranslation());
		}
		public double getGlobalTranslation(int d) {
			return getGlobalTranslation().v()[d-1];
		}
		public Hep3Vector getGlobalRotation() {
			ITransform3D localToGlobal = getLocalToGlobal();
			return localToGlobal.getRotation().rotated(getLocalRotation());
		}
		public double getGlobalRotation(int d) {
			return getGlobalRotation().v()[d-1];
		}
		public ITransform3D getLocalToGlobal() {
			ITransform3D localToGlobal = ( (SiSensor) _det).getReadoutElectrodes(ChargeCarrier.HOLE).getLocalToGlobal();
			return localToGlobal;
		}		
		
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
		
		if(cl.hasOption("d")) {
			detectorName = cl.getOptionValue("d");
		} else {
			printHelpAndExit(options);
		}

		if(cl.hasOption("r")) {
			runNumber = Integer.parseInt(cl.getOptionValue("r"));
		} else {
			printHelpAndExit(options);
		}

		String compactFilenameNew = "compact_new.xml";
		if(cl.hasOption("o")) {
			compactFilenameNew = cl.getOptionValue("o");
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
				
		// read detector geometry
		FileInputStream inCompact;
		try {
			inCompact = new FileInputStream(compactFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("cannot open compact file",e);
		}

		GeometryReader reader = new GeometryReader();
		Detector det;
		try {
			det = reader.read(inCompact);
		} catch (IOException | JDOMException | ElementCreationException e) {
			throw new RuntimeException("problem reading compact file",e);
		}

		
		// set conditions in order to be able to determine which sensors are where in the geometry
		setConditions(detectorName,runNumber);
		
		
		// setup sensors
		HPSSVTSensorSetup sensorSetup = new HPSSVTSensorSetup();
		sensorSetup.detectorChanged(det);
		
		// Loop over all millepede input files and match parameters with detectors
		
		List<MilleParameterSet> list_det = new ArrayList<MilleParameterSet>();
		
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
						
						SiSensor detEle = getTrackerDetElement( det,  par);
						if(detEle == null) {
							System.out.println("Couldn't find detector for param " + par.getId());
							System.exit(1);
						}
						System.out.println("Found detector  " + detEle.getName());
						if(detEle.getClass().isInstance(SiSensor.class)) {
							System.out.println("yeah");
						}

						// do we have it already?
						MilleParameterSet useSet = null;
						for(MilleParameterSet set : list_det) {
							if(set.getDetector() == detEle) {
								useSet = set;
							}
						}
						
						if (useSet == null) {
							useSet = new MilleParameterSet(detEle);
							list_det.add(useSet);
						}
						
						//add the parameter
						useSet.add(par);
						
						
					}
				}
				brMille.close();
			}
		}
		catch (IOException e) {
			throw new RuntimeException("problem reading mille file",e);
		}
		
		for(MilleParameterSet set : list_det) {
			System.out.println("Detector " + set.getDetector().getName());
			List<MilleParameter> pars = set.params;
			for(MilleParameter p : pars) {
				System.out.println(p.getXMLName() + " " + p.getValue());
				Element node = findXMLNode(compact_document,p.getXMLName());
				double value = 0.0;
				if(p.getType() == 1){
					value = set.getGlobalTranslation(p.getDim());
				} else if(p.getType() == 2){
					value = set.getGlobalRotation(p.getDim());
				} else {
					System.out.println("This type is illdefnied " + p.getType());
					System.exit(1);
				}
				node.setAttribute("value", String.format("%.6f",value));
			}
			Hep3Vector u = getMeasuredCoordinate( (SiSensor )set.getDetector());
			System.out.println("u  " + u.toString());
			System.out.println("t (local)  " + set.getLocalTranslation().toString());
			System.out.println("t (global) " + set.getGlobalTranslation().toString());
			System.out.println("r (local)  " + set.getLocalRotation().toString());
			System.out.println("r (global) " + set.getGlobalRotation().toString());
			
			
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

	private static Element findXMLNode(Document document, String name) {
		Element rootNode = document.getRootElement();
		List list = rootNode.getChildren("define");
		for(int i = 0; i < list.size(); ++i ) {
			Element node = (Element) list.get(i);
			List llist = node.getChildren("constant");
			//System.out.println("length of list " + llist.size());
			for(int ii = 0; ii < llist.size(); ++ii ) {
				Element nnode = (Element) llist.get(ii);
				//System.out.println("node name " + nnode.getAttributeValue("name") + " " + nnode.getAttributeValue("value") );
				//if(nnode.getAttributeValue("name").contains(name)) {
				if(nnode.getAttributeValue("name").compareTo(name) ==0 ) {
					return nnode;
				}
			}
		}	
		
		return null;
	}
	
	private static void setConditions(String detectorName, int run) {

		try {
			if(conditionsManager == null) {
				conditionsManager = ConditionsManager.defaultInstance();				
			}
			conditionsManager.setDetector(detectorName, run);
			
		} catch (ConditionsNotFoundException e1) {
			throw new RuntimeException("problem setting conditions",e1);
		}
		
	}

	private static SiSensor getTrackerDetElement(Detector det, MilleParameter par) {
		List<SiSensor> sensors = det.getSubdetector("Tracker").getDetectorElement().findDescendants(SiSensor.class);
        //List<SiTrackerModule> modules = det.getDetectorElement().findDescendants(SiTrackerModule.class);
        //System.out.printf("%d sensors\n",sensors.size());
        for (SiSensor module: sensors) {
            // Create DAQ Maps
            if (!SvtUtils.getInstance().isSetup()) {
                SvtUtils.getInstance().setup(det);
            }
        	boolean isTop = SvtUtils.getInstance().isTopLayer(module);
        	int h = par.getHalf();
        	if ((isTop && h == 1) || (!isTop && h == 2)) {
        		int layer = SvtUtils.getInstance().getLayerNumber(module);
        		if (layer == par.getSensor()) {
        			//found match
        			return module;
        		}
        	}
        }
        return null;
		
	}
	

	
	private static class DetectorList<K> extends ArrayList<DetAlignConstants> {
		//List<DetAlignConstants> _detectors = new ArrayList<DetAlignConstants>();
		public DetectorList() {
		}
		
		public boolean contains(IDetectorElement detEle) {
			return this.get(detEle) == null ? false : true;
		}
		
		public DetAlignConstants get(IDetectorElement detEle) {
			for(DetAlignConstants d : this) {
				if (d == detEle) {
					return d;
				}
			}
			return null;
		}
		public void print() {
			System.out.println("==== " + this.size() + " detectors has alignment corrections ====");
			for(DetAlignConstants det : this) {
				det.print();
			}
		}
		
	}
	
	
	
	private static Hep3Vector getTrackingMeasuredCoordinate(SiSensor sensor)
    {
        // p-side unit vector
        ITransform3D electrodes_to_global = sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getLocalToGlobal();
        Hep3Vector measuredCoordinate = sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getMeasuredCoordinate(0);
        measuredCoordinate = VecOp.mult(VecOp.mult(CoordinateTransformations.getMatrix(),electrodes_to_global.getRotation().getRotationMatrix()), measuredCoordinate);
        return measuredCoordinate;
    }
	
	private static Hep3Vector getMeasuredCoordinate(SiSensor sensor)
    {
        // p-side unit vector
        ITransform3D electrodes_to_global = sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getLocalToGlobal();
        Hep3Vector measuredCoordinate = sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getMeasuredCoordinate(0);
        measuredCoordinate = VecOp.mult(electrodes_to_global.getRotation().getRotationMatrix(), measuredCoordinate);
        return measuredCoordinate;
    }
	
	
	
	private static class SiSensorDetAlignConstants extends DetAlignConstants {
		public SiSensorDetAlignConstants(IDetectorElement det) {
			super(det);
		}
		public void transform() {
			ITransform3D localToGlobal = null;
			if(_det.getClass().isInstance(SiSensor.class)) {
				localToGlobal = ( (SiSensor) _det).getReadoutElectrodes(ChargeCarrier.HOLE).getLocalToGlobal();
			}
			//Translation
			Hep3Vector t_local = _constants.getTranslationVector();
			Hep3Vector t_global = localToGlobal.getRotation().rotated(t_local);
			_constants.addGlobalTranslation(t_global);
			//Rotation
			Hep3Vector r_local = _constants.getRotationVector();
			Hep3Vector r_global = localToGlobal.getRotation().rotated(r_local);
			_constants.addGlobalRotation(r_global);
		}
		
	}
	
	private static abstract class DetAlignConstants {
		protected IDetectorElement _det = null;
		protected AlignConstants<String,Double> _constants = new AlignConstants<String,Double>();
		public DetAlignConstants(IDetectorElement det) {
			_det = det;
		}
		public abstract void transform();
		public void add(MilleParameter par) {
			this._constants.add(par);
		}
		public void print() {
			System.out.println(_det.getName());
			for(Entry<String, Double>  c : this._constants.entrySet()) {
				System.out.println(c.getKey() + " " + c.getValue());
			}
			System.out.println("Local translation  " + _constants.getTranslationVector().toString());
			System.out.println("Global translation " + _constants.getTranslationVectorGlobal().toString());
			System.out.println("Local rotation     " + _constants.getRotationVector().toString());
			System.out.println("Global rotation    " + _constants.getRotationVectorGlobal().toString());
			

		}
			
		
	}
	
	
	
	private static class AlignConstants<K,V> extends HashMap<String,Double> {
		List<MilleParameter> _pars = new ArrayList<MilleParameter>();
		public AlignConstants() {
			super();
		}
		public void add(MilleParameter p) {
			_pars.add(p);
			if (p.getType() == 1) {
				if (p.getDim() == 1) this.put("u", p.getValue());
				else if(p.getDim() == 2) this.put("v", p.getValue());
				else this.put("w", p.getValue());
			}
			else {
				if (p.getDim() == 1) this.put("alpha", p.getValue());
				else if(p.getDim() == 2) this.put("beta", p.getValue());
				else this.put("gamma", p.getValue());
			}
		}
		public void print() {
			for(Entry<String,Double> e : this.entrySet()) {
				System.out.println(e.getKey() + " " + e.getValue());
			}
		}
		public Hep3Vector getTranslationVector() {
			if(!this.containsKey("u") || !this.containsKey("v") || !this.containsKey("w")) {
				System.out.println("missing pars for translation");
				print();
				System.exit(1);
			}
			return new BasicHep3Vector(this.get("u"),this.get("v"),this.get("w"));
		}
		public Hep3Vector getTranslationVectorGlobal() {
			if(!this.containsKey("x") || !this.containsKey("y") || !this.containsKey("z")) {
				System.out.println("missing pars for global translation");
				print();
				System.exit(1);
			}
			return new BasicHep3Vector(this.get("x"),this.get("y"),this.get("z"));
		}
		public Hep3Vector getRotationVector() {
			if(!this.containsKey("alpha") || !this.containsKey("beta") || !this.containsKey("gamma")) {
				System.out.println("missing pars for rotation");
				print();
				System.exit(1);
			}
			return new BasicHep3Vector(this.get("alpha"),this.get("beta"),this.get("gamma"));
		}
		public Hep3Vector getRotationVectorGlobal() {
			if(!this.containsKey("rx") || !this.containsKey("ry") || !this.containsKey("rz")) {
				System.out.println("missing pars for global rotation");
				print();
				System.exit(1);
			}
			return new BasicHep3Vector(this.get("rx"),this.get("ry"),this.get("rz"));
		}
		private void addGlobalTranslation(Hep3Vector t) {
			this.put("x", t.x()); 
			this.put("y", t.y());
			this.put("z", t.z());
		}
		private void addGlobalRotation(Hep3Vector t) {
			this.put("rx", t.x()); 
			this.put("ry", t.y());
			this.put("rz", t.z());
		}
		
		
	}
	
	

}
