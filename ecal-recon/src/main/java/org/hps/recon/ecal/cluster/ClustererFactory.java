package org.hps.recon.ecal.cluster;

public final class ClustererFactory {
    
    private ClustererFactory() {        
    }
    
    public static Clusterer create(String name) {
        if (LegacyClusterer.class.getSimpleName().equals(name)) {
            return new LegacyClusterer();
        } if (SimpleInnerCalClusterer.class.getSimpleName().equals(name)) {
            return new SimpleInnerCalClusterer();
        } else {
            throw new IllegalArgumentException("Unknown clusterer: " + name);
        }
    }

}
