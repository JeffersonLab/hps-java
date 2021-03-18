package org.hps.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Prints a summary of collections seen in the file, and their average sizes.
 */
public class CollectionSizeDriver extends Driver {

    int eventCount = 0;
    private Map<String, CollectionStats> collections = new HashMap<String, CollectionStats>();

    @Override
    public void process(EventHeader event) {
        List<List<Object>> listOfLists = event.get(Object.class);
        for (List<Object> list : listOfLists) {
            String name = event.getMetaData(list).getName();
            Class type = event.getMetaData(list).getType();

            CollectionStats stats = collections.get(name);
            if (stats == null) {
                stats = new CollectionStats(name, type);
                collections.put(name, stats);
            }

            stats.addCount(list.size());
        }

        eventCount++;
    }

    @Override
    public void endOfData() {
        System.out.format("Read %d events\n", eventCount);
        List<String> names = new ArrayList<String>(collections.keySet());
        java.util.Collections.sort(names);
        for (String name:names){
            collections.get(name).printStats(eventCount);
        }
    }

    private class CollectionStats {

        String name;
        Class type;
        boolean hasHits;
        int nHits;
        double eventsWithCollection = 0;
        double totalCount = 0;

        public CollectionStats(String name, Class type) {
            this.name = name;
            this.type = type;
        }

        public void addCount(int count) {
            eventsWithCollection++;
            totalCount += count;
        }

        public void printStats(int eventCount) {
            double fractionWithCollection = eventsWithCollection / eventCount;
            double averageCollectionSize = totalCount / eventsWithCollection;

            System.out.format("%s (%s): %f of events had this collection, with an average of %f elements\n", name, type.getSimpleName(), fractionWithCollection, averageCollectionSize);

        }
    }
}
