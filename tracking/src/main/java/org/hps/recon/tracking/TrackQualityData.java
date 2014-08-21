package org.hps.recon.tracking;

import org.lcsim.event.GenericObject;

/**
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 *
 */
public class TrackQualityData implements GenericObject {

    public static final int L1_ISOLATION = 0;
    public static final int L2_ISOLATION = 1;
    public static final String QUALITY_COLLECTION = "TrackQualityData";
    public static final String QUALITY_RELATION_COLLECTION = "TrackQualityDataRelations";
    private final double[] doubles;

    /**
     * Default Ctor
     *
     * @param trackerVolume : The SVT volume to which the track used to
     * calculate the residuals corresponds to.
     */
    public TrackQualityData() {
        doubles = new double[2];
    }

    public TrackQualityData(double[] doubles) {
        this.doubles = doubles;
    }

    public double getL1Isolation() {
        return doubles[L1_ISOLATION];
    }

    public double getL2Isolation() {
        return doubles[L2_ISOLATION];
    }

    public static double getL1Isolation(GenericObject object) {
        return object.getDoubleVal(L1_ISOLATION);
    }

    public static double getL2Isolation(GenericObject object) {
        return object.getDoubleVal(L2_ISOLATION);
    }

    /**
     *
     */
    @Override
    public double getDoubleVal(int index) {
        return doubles[index];
    }

    /**
     *
     */
    @Override
    public float getFloatVal(int index) {
        throw new UnsupportedOperationException("No float values in " + this.getClass().getSimpleName());
    }

    /**
     *
     */
    @Override
    public int getIntVal(int index) {
        throw new UnsupportedOperationException("No int values in " + this.getClass().getSimpleName());
    }

    /**
     *
     */
    @Override
    public int getNDouble() {
        return doubles.length;
    }

    /**
     *
     */
    @Override
    public int getNFloat() {
        return 0;
    }

    /**
     *
     */
    @Override
    public int getNInt() {
        return 0;
    }

    /**
     *
     */
    @Override
    public boolean isFixedSize() {
        return true;
    }
}
