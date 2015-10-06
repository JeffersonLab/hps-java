package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.PhysicalVolumeNavigator;
import org.lcsim.geometry.Subdetector;
import org.lcsim.geometry.subdetector.HPSTracker;
import org.lcsim.geometry.subdetector.HPSTracker2;
import org.lcsim.recon.tracking.seedtracker.MaterialXPlane;

/**
 * Extension to lcsim MaterialManager to allow more flexibility in track reconstruction
 *
 * @author Per Hansson <phansson@slac.stanford.edu>
 */
public class MaterialManager extends org.lcsim.recon.tracking.seedtracker.MaterialManager {

    /**
     * Get the path groups for SiTrackerEndcap2, which has modules placed directly in the tracking volume.
     */
    static private class HPSTracker2VolumeGrouper implements SubdetectorVolumeGrouper {

        @Override
        public List<List<String>> getPathGroups(final Subdetector subdet, final IPhysicalVolume topVol) {
            // System.out.println(this.getClass().getSimpleName() + ".getPathGroups()");
            final List<List<String>> pathGroups = new ArrayList<List<String>>();
            // Layer loop.
            for (final IDetectorElement layer : subdet.getDetectorElement().getChildren()) {
                final List<String> modulePaths = new ArrayList<String>();

                // Module loop.
                for (final IDetectorElement module : layer.getChildren()) {
                    final String path = "";
                    PhysicalVolumeNavigator.getLeafPaths(modulePaths, module.getGeometry().getPhysicalVolume(), path);
                }

                // Add module paths to this layer.
                pathGroups.add(modulePaths);
            }
            return pathGroups;
        }
    }

    private final static List<MaterialXPlane> _emptyMaterialXPlaneList = new ArrayList<MaterialXPlane>();

    protected boolean _includeMS = true;

    public MaterialManager() {
        super();

        // Add volume groupers for HPS tracker types.
        final SubdetectorVolumeGrouper endcap2Grouper = new SiTrackerEndap2VolumeGrouper();
        subdetGroups.put(HPSTracker.class, endcap2Grouper);
        subdetGroups.put(HPSTracker2.class, new HPSTracker2VolumeGrouper());
    }

    public MaterialManager(final boolean includeMS) {
        super();
        this._includeMS = includeMS;
    }

    @Override
    public List<MaterialXPlane> getMaterialXPlanes() {
        return this._includeMS ? super.getMaterialXPlanes() : _emptyMaterialXPlaneList;
    }

    @Override
    public void setDebug(final boolean debug) {
        super.setDebug(debug);
    }

}
