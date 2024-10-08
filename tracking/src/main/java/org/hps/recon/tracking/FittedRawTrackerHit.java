package org.hps.recon.tracking;

import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseLCRelation;

public class FittedRawTrackerHit extends BaseLCRelation {

    public FittedRawTrackerHit(RawTrackerHit hit, ShapeFitParameters fit) {
        super(hit, fit);
    }

    public RawTrackerHit getRawTrackerHit() {
        return (RawTrackerHit) getFrom();
    }

    public ShapeFitParameters getShapeFitParameters() {
        return (ShapeFitParameters) getTo();
    }

    public double getT0() {
        return getShapeFitParameters().getT0();
    }

    public double getAmp() {
        return getShapeFitParameters().getAmp();
    }
    public double getT0Err() {
        return getShapeFitParameters().getT0Err();
    }

    public static RawTrackerHit getRawTrackerHit(LCRelation rel) {
        return (RawTrackerHit) rel.getFrom();
    }

    public static GenericObject getShapeFitParameters(LCRelation rel) {
        return (GenericObject) rel.getTo();
    }

    public static double getT0(LCRelation rel) {
        return ShapeFitParameters.getT0(getShapeFitParameters(rel));
    }

    public static double getAmp(LCRelation rel) {
        return ShapeFitParameters.getAmp(getShapeFitParameters(rel));
    }
    public static double getT0Err(LCRelation rel) {
        return ShapeFitParameters.getT0Err(getShapeFitParameters(rel));
    }

    /**
     * Get the fit chi2 probability. 
     *
     * @param relation The relation between a RawTrackerHit and 
     *                 ShapeFitParameter.
     */
    public static double getChi2Prob(LCRelation relation) {
        return ShapeFitParameters.getChiProb(getShapeFitParameters(relation)); 
    }

    @Override
    public String toString() {
        return String.format("HPSFittedRawTrackerHit: hit cell id %d on sensor %s with fit %s\n", this.getRawTrackerHit().getCellID(), getRawTrackerHit().getDetectorElement().getName(), this.getShapeFitParameters().toString());
    }
}
