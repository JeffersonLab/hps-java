package org.hps.recon.tracking;

/*
 * HitManager.java
 *
 * Created on August 4, 2007, 4:03 PM
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.recon.tracking.seedtracker.Sector;
import org.lcsim.recon.tracking.seedtracker.SeedLayer;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;

/**
 * Organize tracker hits into lists of hits sorted by detector name, layer number, and barrel-endcap flag
 * @author Richard Partridge
 * @version 1.0
 */
public class HitManager {

    private SectorManager _smanager;
    private boolean _doSectoring = true;

    /** Creates a new instance of HitManager */
    public HitManager() {

        // Instantiate a default sector manager with coarse sectoring
        _smanager = new SectorManager();
    }

    /**
     * Sort the hits into distinct lists where each list has a unique detector name, layer number, and barrel endcap flag.
     * Also calculate the minimum and maximum hit radius and z coordinate for each list.
     * @param event EventHeader for the event to be organized
     * @deprecated use OrganizeHits(List<HelicalTrackHit>) instead
     */
    public void OrganizeHits(EventHeader event) {

        //  Retrieve the HelicalTrackHits
        List<HelicalTrackHit> hitcol = event.get(HelicalTrackHit.class, "HelicalTrackHits");

        OrganizeHits(hitcol);
    }

    /**
     * Sort the hits into distinct lists where each list has a unique detector name, layer number, and barrel endcap flag.
     * Also calculate the minimum and maximum hit radius and z coordinate for each list.
     * @param hitCol List of <code>HelicalTrackHits</code> to be organized
     */
    public void OrganizeHits(List<HelicalTrackHit> hitCol) {

        //  Initialize the sector manager
        _smanager.Initialize();

        //  Loop over the hits and let the SectorManager keep track of them
        for (HelicalTrackHit hit : hitCol) {
            //  Tell the sector manager about this hit
            _smanager.AddHit(hit);
        }
    }

    public SectorManager getSectorManager() {
        return _smanager;
    }

    public void setSectorParams(int nphi, double dz) {
        _smanager.setSectorParams(nphi, dz);
    }

    public void setSectorParams(List<SeedStrategy> slist, double bfield, double rtrk) {
        _smanager.setSectorParams(slist, bfield, rtrk);
    }

    public List<Sector> getSectors(SeedLayer seedlayer) {
        return _smanager.getSectors(seedlayer);
    }

    public void setDoSectoring(boolean doSectoring) {
        _doSectoring = doSectoring;
    }

    public boolean getDoSectoring() {
        return _doSectoring;
    }

    /**
     * Return the list of tracker hits associated with a specified SeedLayer
     * @param seedlayer Seedlayer to look at
     * @return List of TrackerHits
     */
    public List<HelicalTrackHit> getTrackerHits(SeedLayer seedlayer) {

        //  Get the list of sectors for this SeedLayer
        List<Sector> sectorlist = getSectors(seedlayer);

        //  Make a list of all the hits in these sectors
        List<HelicalTrackHit> hitlist = new ArrayList<HelicalTrackHit>();
        for (Sector sector : sectorlist) {
            hitlist.addAll(sector.Hits());
        }

        return hitlist;
    }
}
