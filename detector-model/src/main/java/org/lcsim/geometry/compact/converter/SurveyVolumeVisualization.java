package org.lcsim.geometry.compact.converter;

/**
 * 
 * LCDD geometry visualization information
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class SurveyVolumeVisualization {
    protected String visName = "";
    public SurveyVolumeVisualization() {}
    public String getVisName() {
        return visName;
    }
    protected void setVisName(String visName) {
        this.visName = visName;
    }
    
    
}