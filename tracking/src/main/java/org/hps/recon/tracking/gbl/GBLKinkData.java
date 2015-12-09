package org.hps.recon.tracking.gbl;

import java.util.List;
import org.apache.commons.math3.util.Pair;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseRelationalTable;

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

    private static Pair<EventHeader, RelationalTable> kinkDataToTrackCache = null;

    public static RelationalTable getKinkDataToTrackTable(EventHeader event) {
        if (kinkDataToTrackCache == null || kinkDataToTrackCache.getFirst() != event) {
            RelationalTable kinkDataToTrack = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            if (event.hasCollection(LCRelation.class, DATA_RELATION_COLLECTION)) {
                List<LCRelation> relations = event.get(LCRelation.class, DATA_RELATION_COLLECTION);
                for (LCRelation relation : relations) {
                    if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                        kinkDataToTrack.add(relation.getFrom(), relation.getTo());
                    }
                }
            }
            kinkDataToTrackCache = new Pair<EventHeader, RelationalTable>(event, kinkDataToTrack);
        }
        return kinkDataToTrackCache.getSecond();
    }

    public static GenericObject getKinkData(EventHeader event, Track track) {
        return (GenericObject) getKinkDataToTrackTable(event).from(track);
    }

}
