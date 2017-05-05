package org.lcsim.geometry.compact.converter;

/**
 * Interface to the LCDD converter geometry for the geometry definition. 
 * No volume is built but it can be used as reference in building the geometry.
 */
public class LCDDGhostSurveyVolume extends LCDDSurveyVolume {
       
    /**
     * Initialize with base and mother. This is typically for a reference geometry object 
     * that is used for referencing coordinate systems but that doesn't have a volume itself.
     * @param base - object used to get geometry definitions
     * @param mother - mother LCDD object
     */
    public LCDDGhostSurveyVolume(SurveyVolume base, LCDDSurveyVolume mother) {
        super(base);
        if(isDebug()) System.out.printf("%s: constructing LCDD ghost object %s with mother %s\n", this.getClass().getSimpleName(),base.getName(),mother==null?"null":mother.getName());
        setMother(mother);
        mother.addDaughter(this);
        if(isDebug()) System.out.printf("%s: DONE constructing LCDD object %s\n", this.getClass().getSimpleName(),base.getName());
    }
    
}