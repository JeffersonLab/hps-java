package org.hps.datacat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.dataset.DatasetWithViewModel;

/**
 * Utility class for assocating a file in the datacat to its event ID range.
 * 
 * @author jeremym
 */
public final class FileEventRange {
    
    private long startEvent;
    private long endEvent;
    private DatasetModel dataset;
    
    FileEventRange(DatasetModel dataset, long startEvent, long endEvent) {
        this.startEvent = startEvent;
        this.endEvent = endEvent;
        this.dataset = dataset;
    }
    
    public DatasetModel getDataset() {
        return dataset;
    }
    
    public long getStartEvent() {
        return startEvent;
    }
    
    public long getEndEvent() {
        return endEvent;
    }
    
    public boolean matches(long eventId) {
        return eventId >= startEvent && eventId <= endEvent;
    }
    
    public static List<FileEventRange> createEventRanges(DatasetResultSetModel results) {
        List<FileEventRange> ranges = new ArrayList<FileEventRange>();
        for (DatasetModel ds : results) {
            DatasetWithViewModel view = (DatasetWithViewModel) ds;
            Map<String, Object> metadata = view.getMetadataMap();
            long firstPhysicsEvent = (Long) metadata.get("FIRST_PHYSICS_EVENT");
            long lastPhysicsEvent = (Long) metadata.get("LAST_PHYSICS_EVENT");
            ranges.add(new FileEventRange(ds, firstPhysicsEvent, lastPhysicsEvent));
        }
        return ranges;
    }
    
    public static FileEventRange findEventRange(List<FileEventRange> ranges, long eventId) {
        FileEventRange match = null;
        for (FileEventRange range : ranges) {
            if (range.matches(eventId)) {
                match = range;
                break;
            }
        }
        return match;
    }
}
