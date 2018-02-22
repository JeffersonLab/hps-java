package org.hps.analysis.MC;

import org.lcsim.event.GenericObject;
/**
 * Persistable info from TrackTruthMatching object
 *
 */
public class TrackTruthData implements GenericObject {
    public static final int N_LAYERS = 6;
    private final float[] floats;
    private final int[] ints;

    public TrackTruthData() {
        floats = new float[N_LAYERS];
        ints = new int[N_LAYERS];
    }
    
    public TrackTruthData(TrackTruthMatching ttm) {
        ints = new int[N_LAYERS];
        floats = new float[N_LAYERS];
        
        for (int i=0; i<N_LAYERS; i++) {
            Boolean temp = ttm.getHitList(i+1);
            if (temp == null || temp == false)
                ints[i] = 0;
            else
                ints[i] = 1;
            floats[i] = ttm.getNumberOfMCParticles(i+1);
        }
    }
    
    public TrackTruthData(int[] GoodHitsList, int[] MCHits) {
        floats = new float[N_LAYERS];
        if (GoodHitsList.length == N_LAYERS) {
            ints = GoodHitsList;
            for (int i=0; i<N_LAYERS; i++) {
                floats[i] = MCHits[i];
            }
        }
        else
            ints = new int[N_LAYERS];
    }

    public int getNGoodTruthHits() {
        int count=0;
        for (int i=0; i<N_LAYERS; i++) {
            if (ints[i] == 1)
                count++;
        }
        return count;
    }
    
    @Override
    public int getNInt() {
        return N_LAYERS;
    }

    @Override
    public int getNFloat() {
        return N_LAYERS;
    }

    @Override
    public int getNDouble() {
        return 0;
    }

    @Override
    public int getIntVal(int index) {
        if (index>=0 && index<N_LAYERS)
            return ints[index];
        return -1;
    }

    @Override
    public float getFloatVal(int index) {
        if (index>=0 && index<N_LAYERS)
            return floats[index];
        return -1;
    }

    @Override
    public double getDoubleVal(int index) {
        return -1;
    }

    public int getNumMChits(int layer) {
        if (layer>0 && layer<=N_LAYERS)
            return (int) floats[layer-1];
        return -1;
    }
    
    public boolean hasGoodHit(int layer) {
        if (layer>0 && layer<=N_LAYERS)
            return (ints[layer-1] > 0);
        return false;
    }
    
    @Override
    public boolean isFixedSize() {
        return true;
    }

}
