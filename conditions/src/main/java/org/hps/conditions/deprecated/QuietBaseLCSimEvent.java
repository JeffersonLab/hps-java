package org.hps.conditions.deprecated;

import hep.physics.event.BaseEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.Hit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.util.BaseIDDecoder;
import org.lcsim.geometry.util.IDDescriptor;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.lcio.LCIOUtil;

/**
 * Duplicates BaseLCSimEvent but doesn't call conditionsManager.setDetector in
 * its constructor. Used in TestRunTriggeredReconToLcio to avoid causing
 * spurious detectorChanged events. TODO: really should find a better way to do
 * this, this is awful, but I don't see a clean way to do this by just extending
 * BaseLCSimEvent.
 * @author Sho Uemura
 */
public class QuietBaseLCSimEvent extends BaseEvent implements EventHeader {
    private String detectorName;
    private final Map<List, LCMetaData> metaDataMap = new IdentityHashMap<List, LCMetaData>();
    private ConditionsManager conditionsManager = ConditionsManager.defaultInstance();
    private static final int NANO_SECONDS = 1000000;
    public static final String READOUT_NAME = "ReadoutName";

    /** Creates a new instance of BaseLCSimEvent */
    public QuietBaseLCSimEvent(int run, int event, String detectorName) {
        this(run, event, detectorName, System.currentTimeMillis() * NANO_SECONDS);
    }

    public QuietBaseLCSimEvent(int run, int event, String detectorName, long timeStamp) {
        super(run, event, timeStamp);
        this.detectorName = detectorName;
        // try
        // {
        // conditionsManager.setDetector(detectorName,run);
        // }
        // catch (ConditionsNotFoundException x)
        // {
        // throw new
        // RuntimeException("Please see http://confluence.slac.stanford.edu/display/ilc/Conditions+database",x);
        // }
    }

    // public String getInputFile()
    // {
    // return (String)get("INPUT_FILE");
    // }

    public String toString() {
        return "Run " + getRunNumber() + " Event " + getEventNumber() + " (" + new Date(getTimeStamp() / NANO_SECONDS) + ") Detector: " + detectorName;
    }

    public Detector getDetector() {
        return conditionsManager.getCachedConditions(Detector.class, "compact.xml").getCachedData();
    }

    public List<MCParticle> getMCParticles() {
        return get(MCParticle.class, MC_PARTICLES);
    }

    public List<Track> getTracks() {
        return get(Track.class, TRACKS);
    }

    public List<Cluster> getClusters() {
        return get(Cluster.class, CLUSTERS);
    }

    public List<SimCalorimeterHit> getSimCalorimeterHits(String name) {
        return get(SimCalorimeterHit.class, name);
    }

    public List<SimTrackerHit> getSimTrackerHits(String name) {
        return get(SimTrackerHit.class, name);
    }

    public <T> List<T> get(Class<T> type, String name) {
        return (List<T>) get(name);
    }

    public <T> List<List<T>> get(Class<T> type) {
        List<List<T>> result = new ArrayList<List<T>>();
        for (Map.Entry<List, LCMetaData> entry : metaDataMap.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getType())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public Collection<LCMetaData> getMetaData() {
        return metaDataMap.values();
    }

    public Set<List> getLists() {
        return metaDataMap.keySet();
    }

    public LCMetaData getMetaData(List x) {
        return metaDataMap.get(x);
    }

    public void put(String name, Object component) {
        super.put(name, component);
        if (component instanceof List) {
            List list = (List) component;
            Class type = list.isEmpty() ? Object.class : list.get(0).getClass();
            for (Object o : list) {
                if (!type.isAssignableFrom(o.getClass()))
                    type = Object.class;
            }
            metaDataMap.put(list, new MetaData(name, type, 0, null));
        }
    }

    public void put(String name, List collection, Class type, int flags) {
        put(name, collection, type, flags, null);
    }

    public void put(String name, List collection, Class type, int flags, String readoutName) {
        super.put(name, collection);

        LCMetaData meta = new MetaData(name, type, flags, readoutName);
        metaDataMap.put(collection, meta);

        setCollectionMetaData(collection, type, meta);
    }

    private void setCollectionMetaData(List collection, Class type, LCMetaData meta) {
        // Set MetaData on collection objects.
        if (Hit.class.isAssignableFrom(type)) {
            for (Object o : collection) {
                if (o instanceof Hit) {
                    Hit hit = (Hit) o;
                    if (hit.getMetaData() == null) {
                       hit.setMetaData(meta);
                    }
                }
            }
        }
    }

    protected void put(String name, List collection, Class type, int flags, Map intMap, Map floatMap, Map stringMap) {
        super.put(name, collection);
        LCMetaData meta = new MetaData(name, type, flags, intMap, floatMap, stringMap);
        metaDataMap.put(collection, meta);
    }

    /**
     * Removes a collection from the event.
     */
    public void remove(String name) {
        Object collection = get(name);
        if (collection instanceof List)
            metaDataMap.remove((List) collection);
        super.keys().remove(name);
    }

    public String getDetectorName() {
        return detectorName;
    }

    public boolean hasCollection(Class type, String name) {
        if (!hasItem(name))
            return false;
        Object collection = get(name);
        if (!(collection instanceof List))
            return false;
        return type.isAssignableFrom(metaDataMap.get(collection).getType());
    }

    public boolean hasCollection(Class type) {
        for (LCMetaData meta : metaDataMap.values()) {
            if (type.isAssignableFrom(meta.getType())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasItem(String name) {
        return super.keys().contains(name);
    }

    public Map<String, int[]> getIntegerParameters() {
        return Collections.<String, int[]> emptyMap();

    }

    public Map<String, float[]> getFloatParameters() {
        return Collections.<String, float[]> emptyMap();

    }

    public Map<String, String[]> getStringParameters() {
        return Collections.<String, String[]> emptyMap();
    }

    public float getWeight() {
        return 1.0f;
    }

    private class MetaData implements LCMetaData {
        private int flags;
        private final String name;
        private final Class type;
        private Map<String, int[]> intMap;
        private Map<String, float[]> floatMap;
        private Map<String, String[]> stringMap;
        private transient IDDecoder dec;

        MetaData(String name, Class type, int flags, String readoutName) {
            this.name = name;
            this.type = type;
            this.flags = flags;
            if (readoutName != null)
                getStringParameters().put(READOUT_NAME, new String[] { readoutName });
        }

        MetaData(String name, Class type, int flags, Map intMap, Map floatMap, Map stringMap) {
            this.name = name;
            this.type = type;
            this.flags = flags;
            this.intMap = intMap;
            this.floatMap = floatMap;
            this.stringMap = stringMap;
        }

        public int getFlags() {
            return flags;
        }

        public String getName() {
            return name;
        }

        public Class getType() {
            return type;
        }

        public org.lcsim.geometry.IDDecoder getIDDecoder() {
            if (dec == null)
                dec = findIDDecoder();
            return dec;
        }

        public org.lcsim.geometry.IDDecoder findIDDecoder() {
            // If the IDDecoder name is explicitly set then use it, otherwise
            // use the name of the collection itself.
            String readoutName = name;
            if (stringMap != null) {
                String[] names = stringMap.get(READOUT_NAME);
                if (names != null && names.length >= 1)
                    readoutName = names[0];
            }

            // Find the IDDecoder using the Detector.
            org.lcsim.geometry.IDDecoder result = null;
            try {
                result = getDetector().getDecoder(readoutName);
            } catch (RuntimeException x) {
            }

            // Detector lookup failed. Attempt to use the CellIDEncoding
            // collection parameter.
            if (result == null)
                result = createIDDecoderFromCellIDEncoding();

            // If both methods failed, then there is a problem.
            if (result == null)
                throw new RuntimeException("Could not find or create an IDDecoder for the collection: " + name + ", readout: " + readoutName);

            return result;
        }

        /**
         * Make an IDDecoder for this MetaData using the CellIDEncoding
         * parameter.
         * @return An IDDecoder made built from the CellIDEncoding.
         */
        private IDDecoder createIDDecoderFromCellIDEncoding() {
            String[] cellIdEncoding = getStringParameters().get("CellIDEncoding");
            IDDecoder result = null;
            if (cellIdEncoding != null) {
                result = new BaseIDDecoder();
                try {
                    IDDescriptor desc = new IDDescriptor(cellIdEncoding[0]);
                    result.setIDDescription(desc);
                } catch (IDDescriptor.IDException x) {
                    throw new RuntimeException(x);
                }
            }
            return result;
        }

        public Map<String, int[]> getIntegerParameters() {
            if (intMap == null)
                intMap = new HashMap<String, int[]>();
            return intMap;
        }

        public Map<String, float[]> getFloatParameters() {
            if (floatMap == null)
                floatMap = new HashMap<String, float[]>();
            return floatMap;
        }

        public Map<String, String[]> getStringParameters() {
            if (stringMap == null)
                stringMap = new HashMap<String, String[]>();
            return stringMap;
        }

        public EventHeader getEvent() {
            return QuietBaseLCSimEvent.this;
        }

        public void setSubset(boolean isSubset) {
            flags = LCIOUtil.bitSet(flags, LCIOConstants.BITSubset, isSubset);
        }

        public boolean isSubset() {
            return LCIOUtil.bitTest(flags, LCIOConstants.BITSubset);
        }

        public void setTransient(boolean isTransient) {
            flags = LCIOUtil.bitSet(flags, LCIOConstants.BITTransient, isTransient);
        }

        public boolean isTransient() {
            return LCIOUtil.bitTest(flags, LCIOConstants.BITTransient);
        }
    }
}
