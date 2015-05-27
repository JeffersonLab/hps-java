package org.lcsim.geometry.compact.converter;

import hep.physics.vec.Hep3Vector;

public abstract class SurveyVolumeImpl extends SurveyVolumeVisualization {

    private boolean debug = false;
    protected SurveyVolume surveyVolume = null;

    public SurveyVolumeImpl(SurveyVolume surveyVolume) {
        super();
        this.surveyVolume = surveyVolume;
    }

    public abstract void buildPhysVolume();

    public abstract void buildBox();

    public abstract void buildVolume();

    public abstract void setPositionAndRotation(SurveyVolume base);

    public String getName() {
    	return surveyVolume.getName();
    }
    
    protected Hep3Vector getBoxDim() {
        return surveyVolume.getBoxDim();
    }
    
    protected String getMaterial() {
        return surveyVolume.getMaterial();
    }

    public boolean isDebug() {
    	return debug;
    }

    public abstract String toString();

}