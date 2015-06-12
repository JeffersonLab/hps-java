package org.hps.recon.tracking;

import org.lcsim.event.GenericObject;

/**
 * Generic object used to persist track data not available through 
 * a Track object.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 *
 */
public class TrackData implements GenericObject {
    
    public static final int L1_ISOLATION_INDEX = 0;
    public static final int L2_ISOLATION_INDEX = 1;
    public static final int TRACK_TIME_INDEX = 0;
    public static final int TRACK_VOLUME_INDEX = 0;
    public static final String TRACK_DATA_COLLECTION = "TrackData";
    public static final String TRACK_DATA_RELATION_COLLECTION = "TrackDataRelations";
    
    private final double[] doubles;
    private final float[] floats;
    private final int[] ints;
    
    /**
     * Default constructor
     */
    public TrackData() {
        doubles = new double[2];
        floats = new float[1];
        ints = new int[1];
    }
        
    /**
     * Constructor
     * 
     * @param trackVolume : SVT volume associated with the track
     * @param trackTime : The track time 
     * @param doubles : an array of doubles containing track data
     */
    public TrackData(int trackVolume, float trackTime, double[] doubles) {
    
        this.doubles = doubles;
        this.floats = new float[]{trackTime};
        this.ints = new int[]{trackVolume};
    }

    /**
     * @return The layer 1 isolation value
     */
    public double getL1Isolation() {
        return doubles[L1_ISOLATION_INDEX];
    }

    /**
     * @return The layer 2 isolation value
     */
    public double getL2Isolation() {
        return doubles[L2_ISOLATION_INDEX];
    }

    /**
     * @return The track time
     */
    public float getTrackTime() { 
        return floats[TRACK_TIME_INDEX];
    }
    
    /**
     * @return The SVT volume associated with the track
     */
    public int getTrackVolume() {
        return ints[TRACK_VOLUME_INDEX];
    }

    /**
     * @param object : The generic object containing the data.
     * @return The layer 1 isolation value
     */
    public static double getL1Isolation(GenericObject object) {
        return object.getDoubleVal(L1_ISOLATION_INDEX);
    }

    /**
     * @param object : The generic object containing the data.
     * @return The layer 2 isolation value
     */
    public static double getL2Isolation(GenericObject object) {
        return object.getDoubleVal(L2_ISOLATION_INDEX);
    }
    
    /**
     * @param object : The generic object containing the data.
     * @return The track time
     */
    public static float getTrackTime(GenericObject object) { 
        return object.getFloatVal(TRACK_TIME_INDEX);
    }
    
    /**
     * @param object : The generic object containing the data.
     * @return The SVT volume associated with the track
     */
    public static int getTrackVolume(GenericObject object) { 
        return object.getIntVal(TRACK_VOLUME_INDEX);
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

    /**
     * True if objects of the implementation class have a fixed size.
     */
    @Override
    public boolean isFixedSize() {
        return true;
    }
}
