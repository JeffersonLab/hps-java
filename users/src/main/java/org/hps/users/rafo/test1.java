package org.hps.users.rafo;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

public class test1 extends Driver {
    private int clusterID;
    
    public void process(EventHeader event) {
        System.out.println("The cluster ID = " + clusterID);
    }
    
    public void setClusterID(int clusterID) {
        this.clusterID = clusterID;
    }
}



