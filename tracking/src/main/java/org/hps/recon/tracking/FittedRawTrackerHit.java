package org.hps.recon.tracking;

import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseLCRelation;

/**
 * 
 * @author meeg
 * @version $Id: HPSFittedRawTrackerHit.java,v 1.3 2013/04/16 22:05:43 phansson Exp $
 */
// TODO: Add class documentation.
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

    @Override
    public String toString() {
        return String.format("HPSFittedRawTrackerHit: hit cell id %d on sensor %s with fit %s\n", this.getRawTrackerHit().getCellID(), getRawTrackerHit().getDetectorElement().getName(), this.getShapeFitParameters().toString());
    }
}
