package org.hps.recon.tracking.gbl;

import org.lcsim.event.GenericObject;

/**
 * Generic object used to persist GBL kink data.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 *
 */
public class GBLKinkData implements GenericObject {

    public static final String DATA_COLLECTION = "GBLKinkData";
    public static final String DATA_RELATION_COLLECTION = "GBLKinkDataRelations";

    private final double[] phiKinks;
    private final float[] lambdaKinks;

    public GBLKinkData(float[] lambdaKinks, double[] phiKinks) {

        this.lambdaKinks = lambdaKinks;
        this.phiKinks = phiKinks;
    }

    public double getPhiKink(int layer) {
        return phiKinks[layer];
    }

    public double getLambdaKink(int layer) {
        return lambdaKinks[layer];
    }

    public static double getPhiKink(GenericObject object, int layer) {
        return object.getDoubleVal(layer);
    }

    public static double getLambdaKink(GenericObject object, int layer) {
        return object.getFloatVal(layer);
    }

    /**
     * Returns the double value for the given index.
     */
    @Override
    public double getDoubleVal(int index) {
        return phiKinks[index];
    }

    /**
     * Returns the float value for the given index.
     */
    @Override
    public float getFloatVal(int index) {
        return lambdaKinks[index];
    }

    /**
     * Return the integer value for the given index.
     */
    @Override
    public int getIntVal(int index) {
        throw new ArrayIndexOutOfBoundsException();
    }

    /**
     * Number of double values stored in this object.
     */
    @Override
    public int getNDouble() {
        return phiKinks.length;
    }

    /**
     * Number of float values stored in this object.
     */
    @Override
    public int getNFloat() {
        return lambdaKinks.length;
    }

    /**
     * Number of integer values stored in this object.
     */
    @Override
    public int getNInt() {
        return 0;
    }

    /**
     * True if objects of the implementation class have a fixed size.
     */
    @Override
    public boolean isFixedSize() {
        return true;
    }
}
