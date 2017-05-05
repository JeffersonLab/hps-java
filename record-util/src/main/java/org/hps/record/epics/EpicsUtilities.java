package org.hps.record.epics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for {@link EpicsData}.
 */
public final class EpicsUtilities {

    private EpicsUtilities() {
    }
    
    public static Map<String, Double> computeAverageValues(List<EpicsData> epicsDataList) {
        
        HashMap<String, Double> values = new HashMap<String, Double>();
        HashMap<String, Integer> counts = new HashMap<String, Integer>();
        
        for (EpicsData data : epicsDataList) {
            for (String key : data.getKeys()) {
                if (!values.containsKey(key)) {
                    values.put(key, 0.);
                }
                if (!counts.containsKey(key)) {
                    counts.put(key, 0);
                }
                values.put(key, values.get(key) + data.getValue(key));
                counts.put(key, counts.get(key) + 1);
            }
        }
        
        for (String key : values.keySet()) {
            double averageValue = values.get(key) / counts.get(key);
            values.put(key, averageValue);
        }
        
        return values;
    }
    
    public static int computeTimeInterval(List<EpicsData> epicsDataList) {
        if (epicsDataList.size() == 0) {
            throw new IllegalArgumentException("The EPICS data list is empty.");
        }
        ArrayList<EpicsData> sortedList = new ArrayList<EpicsData>(epicsDataList);
        Collections.sort(sortedList, new SequenceComparator());
        return sortedList.get(sortedList.size() - 1).getEpicsHeader().getTimestamp() 
                - sortedList.get(0).getEpicsHeader().getTimestamp();
    }
            
    public static class SequenceComparator implements Comparator<EpicsData> {

        @Override
        public int compare(EpicsData o1, EpicsData o2) {
            if (o1 == o2) {
                return 0;
            } else if (o1.getEpicsHeader().getSequence() < o2.getEpicsHeader().getSequence()) {
                return -1;
            } else {
                return 1;
            }
        }
    }
    
    public static class TimestampComparator implements Comparator<EpicsData> {
        @Override
        public int compare(EpicsData o1, EpicsData o2) {
            if (o1 == o2) {
                return 0;
            } else if (o1.getEpicsHeader().getTimestamp() < o2.getEpicsHeader().getTimestamp()) {
                return -1;
            } else {
                return 1;
            }
        }
    }

}
