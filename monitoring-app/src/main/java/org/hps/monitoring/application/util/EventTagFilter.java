package org.hps.monitoring.application.util;

interface EventTagFilter {

    boolean accept(int eventTag);
    
    public static final class AcceptAllFilter implements EventTagFilter {
        public boolean accept(int eventTag) {
            // Accept any event type.
            return true;
        }
    }
    
    public static final class SyncTagFilter implements EventTagFilter {
        public boolean accept(int eventTag) {
            // Physics event with sync information (DAQ config) has bit 6 set.
            return (((eventTag >> 6) & 1) == 1) && ((eventTag >> 7)== 1);
        }
    }
    
    public static final class PhysicsTagFilter implements EventTagFilter {
        public boolean accept(int eventTag) {
            // Physics event always has bit 7 set.
            return (((eventTag >> 7) & 1) == 1);
        }
    }
}