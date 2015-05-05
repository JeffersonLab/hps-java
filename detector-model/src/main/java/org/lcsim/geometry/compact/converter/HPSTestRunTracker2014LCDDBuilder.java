package org.lcsim.geometry.compact.converter;

import org.jdom.Element;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.BasePlate;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.CSupport;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.SupportBottom;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.SupportPlateBottom;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.SupportPlateTop;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.SupportTop;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.TestRunHalfModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder.BaseModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder.HalfModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition.TestRunModuleBundle;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;


/**
 * Class used by LCDD converter to build detector for SLIC. 
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class HPSTestRunTracker2014LCDDBuilder extends HPSTrackerLCDDBuilder {


	public HPSTestRunTracker2014LCDDBuilder(boolean debugFlag, Element node, LCDD lcdd, SensitiveDetector sens) {
		super(debugFlag, node, lcdd, sens);
	}

	
	public void setBuilder() {
	    setBuilder();
	}
	
	public void build(Volume worldVolume) {
		
		// set and build geometry
        setBuilder(createGeometryDefinition(_debug, node));
        
		if(_builder==null) throw new RuntimeException("need to set builder class before calling build!");

		if(isDebug()) System.out.printf("%s: build the base geometry objects\n", getClass().getSimpleName());

		_builder.build();

		if(isDebug()) System.out.printf("%s: DONE build the base geometry objects\n", getClass().getSimpleName());


		if(isDebug()) System.out.printf("%s: build the LCDD geometry objects\n", getClass().getSimpleName());

		
		// Go through the list of volumes to build that is created in the generic builder class
		// TODO this is manual now since I don't have a way of knowing in the generic builder class what is a ghost volume at this point.
		LCDDSurveyVolume trackingGeometry = new LCDDSurveyVolume(_builder.getSurveyVolume(org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition.TrackingVolume.class), worldVolume);
		add(trackingGeometry);
		baseSurveyVolume = new LCDDSurveyVolume(_builder.getSurveyVolume(TrackerEnvelope.class), lcdd, trackingGeometry);
		add(baseSurveyVolume);
		LCDDSurveyVolume basePlateGeometry = new LCDDSurveyVolume(_builder.getSurveyVolume(BasePlate.class), lcdd, baseSurveyVolume);
		add(basePlateGeometry);
		// TODO I don't think this c-support has any use at all since the coordinates of it has been already used in the builder. Should remove?
		LCDDSurveyVolume cSupportGeometry = new LCDDGhostSurveyVolume(_builder.getSurveyVolume(CSupport.class), baseSurveyVolume);
		add(cSupportGeometry);
		LCDDSurveyVolume supportBottomGeometry = new LCDDGhostSurveyVolume(_builder.getSurveyVolume(SupportBottom.class), baseSurveyVolume);
		add(supportBottomGeometry);
		LCDDSurveyVolume supportPlateBottomGeometry = new LCDDSurveyVolume(_builder.getSurveyVolume(SupportPlateBottom.class), lcdd, baseSurveyVolume);
		add(supportPlateBottomGeometry);
		LCDDSurveyVolume supportTopGeometry = new LCDDGhostSurveyVolume(_builder.getSurveyVolume(SupportTop.class), baseSurveyVolume);
		add(supportTopGeometry);
		LCDDSurveyVolume supportPlateTopGeometry = new LCDDSurveyVolume(_builder.getSurveyVolume(SupportPlateTop.class), lcdd, baseSurveyVolume);
		add(supportPlateTopGeometry);

		// build modules	

		if(isDebug()) System.out.printf("%s: build modules\n", getClass().getSimpleName());

		// Loop over all modules created
		for(BaseModuleBundle mod : _builder.modules) {
		    TestRunModuleBundle m = (TestRunModuleBundle) mod;
			if(isDebug()) { 
				System.out.printf("%s: module layer %d half %s\n", getClass().getSimpleName(),m.getLayer(),m.getHalf());
				m.print();
			}

			// Find the mother among the LCDD objects using its name, should probably have a better way...
			String name_mother = m.getMother().getName();
			LCDDSurveyVolume mother = null;
			for(LCDDSurveyVolume g : lcddSurveyVolumes) {
				if(g.getName().equals(name_mother)) {
					mother = g;
					break;
				}
			}
			// Check that it had a mother
			if(mother==null) throw new RuntimeException("Cound't find mother to module layer " + m.getLayer() + " half "+ m.getHalf());

			if(isDebug()) System.out.printf("%s: found mother %s for module layer %d half %s\n", getClass().getSimpleName(),mother.getName(),m.getLayer(),m.getHalf());

			// add the module to the list of objects that will be added to LCDD
			addTestRunModule(m, mother);

		}



		if(isDebug()) {
		    System.out.printf("%s: DONE building the LCDD geometry objects\n", getClass().getSimpleName());
		    System.out.printf("%s: List of all %d LCDD geometry objects built\n", this.getClass().getSimpleName(), lcddSurveyVolumes.size());
		    for(SurveyVolumeImpl bg : lcddSurveyVolumes) {
		        System.out.printf("-------\n%s\n", bg.toString());
		    }
		}



		// Set visualization features
		setVisualization();


	}


	/**
	 * Rules for adding the LCDD module geometry.
	 * @param bundle - module to be added
	 * @param mother - mother LCDD geometry object
	 */
	protected void addTestRunModule(TestRunModuleBundle bundle, LCDDSurveyVolume mother) {
		// This could perhaps be fixed if there is a relation with daughters in geometry definition?
		// create the module
		LCDDSurveyVolume lcddM = new LCDDSurveyVolume(bundle.module, lcdd, mother);
		add(lcddM);
		if(bundle.halfModuleAxial!=null)  addTestRunHalfModule(bundle.halfModuleAxial,lcddM);
		if(bundle.coldBlock!=null)        add(new LCDDSurveyVolume(bundle.coldBlock, lcdd, lcddM));		
		if(bundle.halfModuleStereo!=null) addTestRunHalfModule((TestRunHalfModuleBundle)bundle.halfModuleStereo,lcddM);
//        if(bundle.halfModuleAxial!=null)  addHalfModule((TestRunHalfModuleBundle)bundle.halfModuleAxial,lcddM);
//        if(bundle.coldBlock!=null)        add(new LCDDSurveyVolume(bundle.coldBlock, lcdd, lcddM));     
//        if(bundle.halfModuleStereo!=null) addHalfModule((TestRunHalfModuleBundle)bundle.halfModuleStereo,lcddM);

	}

	/**
	 * Rules for adding the LCDD half module geometry.
	 * @param bundle - module to be added
	 * @param mother - mother LCDD geometry object
	 */
	protected void addTestRunHalfModule(HalfModuleBundle bundle2, LCDDSurveyVolume mother) {
		// This could perhaps be fixed if there is a relation with daughters in geometry definition?
	    TestRunHalfModuleBundle bundle = (TestRunHalfModuleBundle) bundle2;
	    
	    // create the half-module
		LCDDSurveyVolume lcddHM = new LCDDSurveyVolume(bundle.halfModule, lcdd, mother);
		add(lcddHM);
		// create the sensor
		LCDDSurveyVolume lcddS = new LCDDSurveyVolume(bundle.sensor, lcdd, lcddHM);
		add(lcddS);
		// create the active sensor
		LCDDSurveyVolume lcddAS = new LCDDSurveyVolume(bundle.activeSensor, lcdd, lcddS);
		add(lcddAS);
		// create the lamination
		LCDDSurveyVolume lcddL = new LCDDSurveyVolume(bundle.lamination, lcdd, lcddHM);
		add(lcddL);
		// create the carbon fiber frame
		LCDDSurveyVolume lcddCF = new LCDDSurveyVolume(bundle.carbonFiber, lcdd, lcddHM);
		add(lcddCF);
		// create the hybrid frame
		LCDDSurveyVolume lcddH = new LCDDSurveyVolume(bundle.hybrid, lcdd, lcddHM);
		add(lcddH);

	}


    @Override
    public HPSTrackerGeometryDefinition createGeometryDefinition(boolean debug,
            Element node) {
        return new HPSTestRunTracker2014GeometryDefinition(_debug, node);
    }	


}