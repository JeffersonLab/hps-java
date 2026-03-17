package org.hps.recon.tracking;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

import java.util.List;

/**
 * Driver wrapper for {@link TrackSmearingTool}.
 *
 * Applies Gaussian smearing (MC mode) or mean corrections (data mode) to
 * track helix parameters after pattern recognition and before particle
 * reconstruction.  By default loads smearing constants from the built-in
 * classpath resource {@link TrackSmearingTool#DEFAULT_RESOURCE}.
 *
 * Steering-file parameters (all optional):
 * <pre>
 *   smearingFile      — path to an external JSON config (overrides default resource)
 *   smearingVariable  — binned-lookup variable: "tanLambda" (default), "phi0", "nHits", "flat"
 *   smearOmega        — if true, run updateWithSmearOmega() instead of updateWithSmearP()
 *   smearZ0           — if true, also run updateWithSmearZ0() (default true)
 *   isData            — if true, apply mean corrections instead of Gaussian smearing
 *   applyMeanCorr     — enable mean corrections in data mode (default false)
 *   smearingFactor    — scale factor applied to all sigma values (default 1.0)
 *   seed              — random seed (default 42)
 *   trackCollectionName — input track collection (default "KalmanFullTracks")
 *   debug             — verbose per-track printout (default false)
 * </pre>
 *
 * @author mgraham
 */
public class TrackSmearingDriver extends Driver {

    private String  trackCollectionName = "KalmanFullTracks";
    private String  smearingFile        = null;   // null → use DEFAULT_RESOURCE
    private String  smearingVariable    = null;   // null → use JSON default
    private boolean smearOmega          = false;
    private boolean smearZ0             = true;
    private boolean isData              = false;
    private boolean applyMeanCorr       = false;
    private double  smearingFactor      = 1.0;
    private int     seed                = 42;
    private boolean debug               = false;

    private TrackSmearingTool smearingTool;

    // =========================================================
    // Steering-file setters
    // =========================================================

    public void setTrackCollectionName(String name)  { this.trackCollectionName = name; }
    public void setSmearingFile(String file)         { this.smearingFile        = file; }
    public void setSmearingVariable(String var)      { this.smearingVariable    = var; }
    public void setSmearOmega(boolean smearOmega)    { this.smearOmega          = smearOmega; }
    public void setSmearZ0(boolean smearZ0)          { this.smearZ0             = smearZ0; }
    public void setIsData(boolean isData)            { this.isData              = isData; }
    public void setApplyMeanCorr(boolean apply)      { this.applyMeanCorr       = apply; }
    public void setSmearingFactor(double factor)     { this.smearingFactor      = factor; }
    public void setSeed(int seed)                    { this.seed                = seed; }
    public void setDebug(boolean debug)              { this.debug               = debug; }

    // =========================================================
    // Driver lifecycle
    // =========================================================

    @Override
    public void detectorChanged(Detector detector) {
        double bfield = Math.abs(TrackUtils.getBField(detector).magnitude());

        if (smearingFile != null && !smearingFile.isEmpty()) {
            smearingTool = new TrackSmearingTool(smearingFile, seed, smearingFactor);
        } else {
            smearingTool = new TrackSmearingTool(seed, smearingFactor);
        }

        smearingTool.setBField(bfield);
        smearingTool.setIsData(isData);
        smearingTool.setApplyMeanCorr(applyMeanCorr);
        smearingTool.setDebug(debug);

        if (smearingVariable != null && !smearingVariable.isEmpty())
            smearingTool.setForcedVariable(smearingVariable);

        smearingTool.printConfig();
    }

    @Override
    public void process(EventHeader event) {
        if (!event.hasItem(trackCollectionName)) {
            if (debug)
                System.out.println("TrackSmearingDriver: collection not found: " + trackCollectionName);
            return;
        }

        List<Track> tracks = event.get(Track.class, trackCollectionName);

        for (Track track : tracks) {
            if (smearOmega) {
                smearingTool.updateWithSmearOmega(track);
            } else {
                smearingTool.updateWithSmearP(track);
            }
            if (smearZ0) {
                smearingTool.updateWithSmearZ0(track);
            }
        }
    }
}
