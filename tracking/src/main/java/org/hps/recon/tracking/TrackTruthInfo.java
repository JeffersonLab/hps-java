package org.hps.recon.tracking;

//import java.util.List;
//import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
//import org.lcsim.event.LCRelation;
//import org.lcsim.event.RelationalTable;
//import org.lcsim.event.Track;
//import org.lcsim.event.base.BaseRelationalTable;

/**
 *  * Generic object used to persist track truth data not available through a Track
 *   * object.
 *    */

public class TrackTruthInfo implements GenericObject {
    
    //int array holds two independent sets of data. Each set has 1 array entry
    //for each sensor layer. 
    //The second data set starts at index = number of sensor layers.
    public static int nLayers;
    public static final int GOOD_HITS_START_INDEX = 0;
    public static int NMCPS_START_INDEX;
    public static final int PURITY_INDEX = 0;
    

    private final int[] ints;
    private final double[] doubles;
    private final float[] floats; 

    public TrackTruthInfo(int NLAYERS){
        this.nLayers = NLAYERS;
        this.NMCPS_START_INDEX = NLAYERS;
        ints = new int[2*NLAYERS];
        doubles = new double[1];
        floats = new float[1];
    }

    public void setTrackGoodHits(int[] goodhits){
        for(int i= 0; i < nLayers; i++){
            ints[i+GOOD_HITS_START_INDEX] = goodhits[i];
        }
    }

    public void setTrackNMCPs(int[] nMCPsPerHit){
        for(int i= 0; i < nLayers; i++){
            ints[i+NMCPS_START_INDEX] = nMCPsPerHit[i];
        }
    }

    public void setTrackPurity(double purity){
        doubles[PURITY_INDEX] = purity;
    }


    @Override
    public boolean isFixedSize() {
        return true;
    }

    /**
     * Returns the double value for the given index.
     */
    @Override
    public double getDoubleVal(int index) {
        return doubles[index];
    }

    /**
     * Returns the float value for the given index.
     */
    @Override
    public float getFloatVal(int index) {
        return floats[index];
    }

    /**
     * Return the integer value for the given index.
     */
    @Override
    public int getIntVal(int index) {
        return ints[index];
    }

    /**
     * Number of double values stored in this object.
     */
    @Override
    public int getNDouble() {
        return doubles.length;
    }

    /**
     * Number of float values stored in this object.
     */
    @Override
    public int getNFloat() {
        return floats.length;
    }

    /**
     * Number of integer values stored in this object.
     */
    @Override
    public int getNInt() {
        return ints.length;
    }




}

