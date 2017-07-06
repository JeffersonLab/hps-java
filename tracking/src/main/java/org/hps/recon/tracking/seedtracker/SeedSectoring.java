package org.hps.recon.tracking.seedtracker;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.recon.tracking.seedtracker.Sector;
import org.lcsim.recon.tracking.seedtracker.SeedLayer;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;

/**
 * HPS version of LCSim class
 * @author Richard Partridge
* @author Miriam Diamond <mdiamond@slac.stanford.edu>
* @version $Id: 2.0 07/07/17$
*/

public class SeedSectoring {

    private List<List<Sector>> _seedsectors;

    public SeedSectoring(HitManager hmanager, SeedStrategy strategy, double bfield, boolean doSectorBinCheck) {

        _seedsectors = new ArrayList<List<Sector>>();

        FastCheck checker = new FastCheck(strategy, bfield);
        if (doSectorBinCheck)
            checker.setDoSectorBinCheck(hmanager.getSectorManager());

        //  Get the SeedLayers for this strategy
        List<SeedLayer> layers = strategy.getLayers(SeedLayer.SeedType.Seed);
        if (layers.size() != 3)
            throw new RuntimeException("Illegal Strategy " + strategy.getName() + ": Number of Seed Layers is not 3");

        List<Sector> slist0 = hmanager.getSectors(layers.get(0));
        List<Sector> slist1 = hmanager.getSectors(layers.get(1));
        List<Sector> slist2 = hmanager.getSectors(layers.get(2));

        for (Sector s0 : slist0) {
            for (Sector s1 : slist1) {
                if (!checker.CheckSectorPair(s0, s1))
                    continue;
                for (Sector s2 : slist2) {
                    if (!checker.CheckSectorPair(s0, s2))
                        continue;
                    if (!checker.CheckSectorPair(s1, s2))
                        continue;
                    List<Sector> slist = new ArrayList<Sector>();
                    slist.add(s0);
                    slist.add(s1);
                    slist.add(s2);
                    _seedsectors.add(slist);
                }
            }
        }
    }

    public List<List<Sector>> SeedSectors() {
        return _seedsectors;
    }
}