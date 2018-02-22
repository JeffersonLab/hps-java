package org.hps.analysis.MC;

import org.lcsim.event.GenericObject;
/**
 * Persistable info from TrackTruthMatching object
 *
 */
public class TrackTruthData implements GenericObject {
    public static final int N_INT = 3;
    public static final int NTRUTHHITS_INDEX=0;
    public static final int NGOODTRUTHHITS_INDEX=1;
    public static final int NBADTRUTHHITS_INDEX=2;
    private final double[] doubles;
    private final int[] ints;

    public TrackTruthData() {
        doubles = new double[1];
        ints = new int[N_INT];
    }
    
    public TrackTruthData(int[] hitInfo, double purity) {
        this.doubles = new double[]{purity};
        this.ints = hitInfo;
    }
    
    public int getNTruthHits() {
        return ints[NTRUTHHITS_INDEX];
    }
    
    public int getNBadTruthHits() {
        return ints[NBADTRUTHHITS_INDEX];
    }
    
    public int getNGoodTruthHits() {
        return ints[NGOODTRUTHHITS_INDEX];
    }
    
    @Override
    public int getNInt() {
        // TODO Auto-generated method stub
        return N_INT;
    }

    @Override
    public int getNFloat() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getNDouble() {
        // TODO Auto-generated method stub
        return 1;
    }

    @Override
    public int getIntVal(int index) {
        // TODO Auto-generated method stub
        if (index>=0 && index<N_INT)
            return ints[index];
        return 0;
    }

    @Override
    public float getFloatVal(int index) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getDoubleVal(int index) {
        // TODO Auto-generated method stub
        if (index==0)
            return doubles[0];
        return 0;
    }

    @Override
    public boolean isFixedSize() {
        // TODO Auto-generated method stub
        return true;
    }

}
