package org.lcsim.geometry.compact.converter;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;

public abstract class HPSTrackerLCDDBuilder  implements IHPSTrackerLCDDBuilder {

	public boolean _debug = false;
	protected LCDD lcdd = null;
	protected LCDDSurveyVolume baseSurveyVolume;
	protected List<LCDDSurveyVolume> lcddSurveyVolumes = new ArrayList<LCDDSurveyVolume>();
	private SensitiveDetector sensitiveDetector;
	public HPSTrackerBuilder _builder = null;
    protected Element node;

	
	
	public HPSTrackerLCDDBuilder(boolean debugFlag, Element node, LCDD lcdd2, SensitiveDetector sens) {
		setDebug(debugFlag);
		setLCDD(lcdd2);
		setSensitiveDetector(sens);
		setNode(node);
	}
	
	/**
     * Build the LCDD geometry objects.
     * @param worldVolume - the reference volume.
     */
    public abstract void build(Volume worldVolume);

    
    public abstract void setBuilder();
    
    public abstract HPSTrackerGeometryDefinition createGeometryDefinition(boolean debug, Element node);
	

	public void setNode(Element node) {
        this.node = node;
	}
	
    public void setSensitiveDetector(SensitiveDetector sens) {
		this.sensitiveDetector = sens;
	}

	public SensitiveDetector getSensitiveDetector() {
		return this.sensitiveDetector;
	}

	public void setBuilder(HPSTrackerBuilder b) {
		_builder = b;
	}
	
	public HPSTrackerBuilder getBuilder() {
	    return _builder;
	}
	
	public void build() {
		_builder.build();
	}
	
	public void setDebug(boolean debug) {
		_debug = debug;
	}
	
	public boolean isDebug() {
		return _debug;
	}
	
	/**
	 * Add to list of objects.
	 * @param geom - object to add.
	 */
	public void add(LCDDSurveyVolume geom) {
		lcddSurveyVolumes.add(geom);
	}

	
	

	public void setLCDD(LCDD lcdd) {
		this.lcdd = lcdd;
	}

	public LCDD getLCDD() {
		return lcdd;
	}

	public LCDDSurveyVolume getBaseLCDD() {
		return baseSurveyVolume;
	}

	public void setVisualization() {
	
		if(isDebug()) System.out.printf("%s: Set LCDD visualization for %d LCDD geometry objects \n", getClass().getSimpleName(), lcddSurveyVolumes.size());
		for(SurveyVolumeImpl g : lcddSurveyVolumes) {
		    String name = g.getName();
			if(isDebug()) System.out.printf("%s: Set LCDD vis for %s \n", getClass().getSimpleName(), name);			
			if(name.contains("base_plate")) g.setVisName("BasePlateVis");
            else if(name.equals("base")) g.setVisName("SvtBoxVis");
			else if(name.contains("chamber")) g.setVisName("ChamberVis");
			else if(name.contains("support_bottom") || name.contains("support_top")) g.setVisName("SupportVolumeVis");
			else if(name.contains("support_plate")) g.setVisName("SupportPlateVis");
			else if(name.startsWith("module_")) {
			    if(name.endsWith("halfmodule_axial") || name.endsWith("halfmodule_stereo")) g.setVisName("HalfModuleVis");
			    else if(name.endsWith("cold")) g.setVisName("ColdBlockVis");
			    else if(name.endsWith("lamination")) g.setVisName("KaptonVis");
			    else if(name.endsWith("sensor")) g.setVisName("SensorVis");
			    else if(name.endsWith("sensor_active")) g.setVisName("SensorVis");
			    else if(name.endsWith("cf")) g.setVisName("CarbonFiberVis");
			    else if(name.endsWith("hybrid")) g.setVisName("HybridVis");
			    else {
			        //this must be a module then?
			        g.setVisName("ModuleVis");
			    }
			}
			else {
				if(isDebug()) System.out.printf("%s: No LCDD vis for %s \n", getClass().getSimpleName(), name);
			}
		}
		if(isDebug()) System.out.printf("%s: DONE Set LCDD vis \n", getClass().getSimpleName());
	}
	

}