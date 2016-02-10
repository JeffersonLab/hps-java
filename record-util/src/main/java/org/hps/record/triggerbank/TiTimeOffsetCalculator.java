package org.hps.record.triggerbank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculate TI time offset given lists of min and max offsets and the number of outliers.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class TiTimeOffsetCalculator {
    
    /* Constants from TiTimeOffsetEvioProcessor. */
    private final static int MAX_OUTLIERS = 10;
    private final static double MIN_RANGE = 0.99e9;
    
    private List<Long> minOffsets = new ArrayList<Long>();
    private List<Long> maxOffsets = new ArrayList<Long>();
    private int totalOutliers;
    
    public void addMinOffset(long minOffset) {
        minOffsets.add(minOffset);
    }
    
    public void addMaxOffset(long maxOffset) {
        maxOffsets.add(maxOffset);
    }
    
    public void addNumOutliers(int nOutliers) {
        totalOutliers += nOutliers;
    }
    
    public long calculateTimeOffset() {
        
        if (minOffsets.size() == 0) {
            throw new RuntimeException("The min offsets list has no data.");
        }
        if (maxOffsets.size() == 0) {
            throw new RuntimeException("The max offsets list has no data.");
        }
        
        Collections.sort(minOffsets);
        Collections.sort(maxOffsets);
        
        long minOffset = minOffsets.get(0);
        long maxOffset = maxOffsets.get(maxOffsets.size() - 1);
                
        final long offsetRange = maxOffset - minOffset;
        if (offsetRange > MIN_RANGE && totalOutliers < MAX_OUTLIERS) {
            return minOffset;
        } else {
            return 0L;
        }
    }
}
