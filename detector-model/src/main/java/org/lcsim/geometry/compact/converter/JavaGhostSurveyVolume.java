package org.lcsim.geometry.compact.converter;

/**
 * 
 *  Interface to the JAVA converter geometry for the geometry definition.   
 *  In this case no volume is built but can be used as reference in building the geometry.
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class JavaGhostSurveyVolume extends JavaSurveyVolume {
    
    /**
     * Initialize with base and mother. This is typically for a reference geometry object 
     * that is used for referencing coordinate systems but that doesn't have a volume itself.
     * @param surveyVolume - object used to get geometry definitions
     * @param mother - mother object
     */
    public JavaGhostSurveyVolume(SurveyVolume surveyVolume, JavaSurveyVolume mother) {
        super(surveyVolume);
        if(isDebug()) System.out.printf("%s: constructing JAVA ghost object %s with mother %s\n", this.getClass().getSimpleName(),surveyVolume.getName(),mother==null?"null":mother.getName());
        setMother(mother);
        mother.addDaughter(this);
        setPositionAndRotation(surveyVolume);
        if(isDebug()) System.out.printf("%s: DONE constructing JAVA object %s\n", this.getClass().getSimpleName(),surveyVolume.getName());
    }
    


}