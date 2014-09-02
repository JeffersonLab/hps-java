package org.hps.test_rafopar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

public class test1 extends Driver
{
    int clusterID;

    public void process(EventHeader event )
    {
	System.out.println("The cluster ID = " + clusterID);
    }
    
    public void setClusterID( int a_clusterID )
    {
	clusterID = a_clusterID;
    }
}



