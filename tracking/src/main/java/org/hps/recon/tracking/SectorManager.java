package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.constants.Constants;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.recon.tracking.seedtracker.Sector;
import org.lcsim.recon.tracking.seedtracker.SeedLayer;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;

/**
 *
 * @author Richard Partridge
 */
public class SectorManager {

    private List<Sector> _sectorlist;
    private Map<String, Sector> _sectormap;
    private Map<String, List<Sector>> _slistmap;
    protected int _nphi;
    protected double _dphi;
    protected double _dz;
    protected int _nphi0 = 4;
    protected double _dz0 = 100.;

    public SectorManager() {

        //  Set the default sector parameters
        setSectorParams(_nphi0, _dz0);

        //  Create the list of sectors with hits
        _sectorlist = new ArrayList<Sector>();

        //  Create a map to locate a sector using it's ID
        _sectormap = new HashMap<String, Sector>();

        //  Create a map to locate the list of sectors for a given layer
        _slistmap = new HashMap<String, List<Sector>>();
    }

    public void AddHit(HelicalTrackHit hit) {

        //  Get the sector identifier for this hit
        String identifier = FindSectorIdentifier(hit);

        //  Retrieve the sector - create a new sector if one doesn't already exist
        Sector sector;
        if (!_sectormap.containsKey(identifier)) {
            sector = CreateSector(hit);
            _sectorlist.add(sector);
            _sectormap.put(identifier, sector);

            //  See if we need to create a new list of Sensors for this detector layer
            String lyrid = sector.LayerID();
            if (!_slistmap.containsKey(lyrid)) {
                List<Sector> slist = new ArrayList<Sector>();
                _slistmap.put(lyrid, slist);
            }

            //  Update the list of sensors for this layer
            _slistmap.get(lyrid).add(sector);

        } else {
            sector = _sectormap.get(identifier);
        }

        //  Add the hit to the sector
        sector.addHit(hit);
    }

    public List<Sector> getAllSectors() {
        return _sectorlist;
    }

    public List<Sector> getSectors(SeedLayer layer) {
        String layerID = layer.LayerID();
        List<Sector> sectors;
        if (_slistmap.containsKey(layerID)) {
            sectors = _slistmap.get(layerID);
        } else {
            sectors = new ArrayList<Sector>();
        }
        return sectors;
    }

    public void Initialize() {
        _sectorlist.clear();
        _sectormap.clear();
        _slistmap.clear();
    }

    public void setSectorParams(int nphi, double dz) {
        _nphi = nphi;
        _dphi = 2. * Math.PI / _nphi;
        _dz = dz;
    }

    public void setSectorParams(List<SeedStrategy> slist, double bfield, double rtrk) {

        //  Default to the default sectoring
        int nphi = _nphi0;
        double dz = _dz0;

        //  See if we have defined strategies
        if (slist != null) {
            int nstrat = slist.size();
            if (nstrat > 0) {

                //  Find the average pTMin and MaxZ0
                double dzsum = 0.;
                double ptsum = 0.;
                for (SeedStrategy strategy : slist) {
                    ptsum += strategy.getMinPT();
                    dzsum += strategy.getMaxZ0();
                }
                double ptave = ptsum / nstrat;
                double dzave = dzsum / nstrat;

                //  If there is a bfield defined, set the size of a phi
                //  segmentation slice to half the change in angle for a
                //  the average minimum momentum particle
                if (bfield > 0.) {
                    double RMin = ptave / (Constants.fieldConversion * bfield);
                    double dphi = Math.atan(rtrk / (2. * RMin));
                    nphi = (int) Math.floor(2. * Math.PI / dphi);
                }

                //  Set the z sectoring to match the average MaxZ0
                dz = dzave;
            }
        }

        //  Save the sector parameters
        setSectorParams(nphi, dz);

        return;
    }

    private Sector CreateSector(HelicalTrackHit hit) {
        String identifier = FindSectorIdentifier(hit);
        String lyrid = hit.getLayerIdentifier();
        int phibin = PhiBin(hit);
        int zbin = ZBin(hit);
        double phimin = PhiMin(phibin);
        double phimax = PhiMax(phibin);
        double zmin = ZMin(zbin);
        double zmax = ZMax(zbin);
        return new Sector(identifier, lyrid, phibin, zbin, phimin, phimax, zmin, zmax);
    }

    private String FindSectorIdentifier(HelicalTrackHit hit) {
        String layerID = hit.getLayerIdentifier();
        int phibin = PhiBin(hit);
        int zbin = ZBin(hit);
        return SectorID(layerID, phibin, zbin);
    }

    private String SectorID(String layerID, int phibin, int zbin) {
        return layerID + "phi" + phibin + "z" + zbin;
    }

    private int PhiBin(HelicalTrackHit hit) {
        return (int) Math.floor(hit.phi() / _dphi);
    }

    protected int ZBin(HelicalTrackHit hit) {
        return (int) Math.floor(hit.z() / _dz);
    }

    private double PhiMin(int phibin) {
        return phibin * _dphi;
    }

    private double PhiMax(int phibin) {
        return (phibin + 1) * _dphi;
    }

    private double ZMin(int zbin) {
        return zbin * _dz;
    }

    private double ZMax(int zbin) {
        return (zbin + 1) * _dz;
    }

}
