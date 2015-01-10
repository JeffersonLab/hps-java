package org.hps.recon.ecal.cluster;

/**
 * Type codes for different kinds of ECAL clusters.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public enum ClusterType {
    
    // Do not change the value assignments!
    RECON(1000),
    SIMPLE_RECON(1001),
    LEGACY(1002),
    GTP(1003),
    GTP_ONLINE(1004),
    CTP(1005),
    NN(1006),
    SIMPLE_COSMIC(1007),
    DUAL_THRESHOLD_COSMIC(1008);   
    // Any additional types should be added at end of the list.
                
    private int type;
    
    ClusterType(int type) {
        this.type = type;
    }
        
    public int getType() {
        return type;
    }
    
    public static ClusterType getClusterType(int type) {                
        if (type == RECON.getType()) {
            return RECON;
        } else if (type == SIMPLE_RECON.getType()) {
            return SIMPLE_RECON;
        } else if (type == LEGACY.getType()) {
            return LEGACY;
        } else if (type == GTP.getType()) {
            return GTP;
        } else if (type == GTP_ONLINE.getType()) {
            return GTP_ONLINE;
        } else if (type == CTP.getType()) {
            return CTP;
        } else if (type == NN.getType()) {
            return NN;
        } else if (type == SIMPLE_COSMIC.getType()) {
            return SIMPLE_COSMIC;
        } else if (type == DUAL_THRESHOLD_COSMIC.getType()) {
            return DUAL_THRESHOLD_COSMIC;
        } else {
            return null;
        }
    }
    
    public boolean equals(ClusterType clusterType) {
        return clusterType.getType() == getType();
    }
}
