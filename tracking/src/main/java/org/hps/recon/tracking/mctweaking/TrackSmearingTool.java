package org.hps.recon.tracking;

import org.json.JSONArray;
import org.json.JSONObject;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseTrack;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Port of hpstr TrackSmearingTool to hps-java.
 *
 * Applies Gaussian smearing (MC mode) or mean corrections (data mode) to
 * track helix parameters: momentum magnitude (via omega), z0, or omega
 * directly.  Configuration is loaded from a JSON file using the same format
 * as the hpstr C++ tool.
 *
 * Typical usage inside a Driver:
 * <pre>
 *   // in detectorChanged:
 *   smearingTool = new TrackSmearingTool("smearing.json");
 *   smearingTool.setBField(Math.abs(TrackUtils.getBField(detector).magnitude()));
 *
 *   // in process, per track:
 *   smearingTool.updateWithSmearP(track);
 *   smearingTool.updateWithSmearZ0(track);
 * </pre>
 *
 * Notes:
 *  - ROOT-histogram-based smearing (from .root files) is not supported; use a JSON config.
 *  - Truth-matching support is omitted; apply at the caller level if needed.
 *  - Top half is defined by tanLambda > 0.
 *
 * @author ported from hpstr/utils/src/TrackSmearingTool.cxx
 */
public class TrackSmearingTool {

    /** Classpath location of the default smearing configuration. */
    public static final String DEFAULT_RESOURCE =
            "/org/hps/recon/tracking/trackSmearing/tool_smearing.json";

    // =========================================================
    // Inner class for binned smearing tables
    // =========================================================

    private static class BinnedParam {
        double[] edgesTop = new double[0], edgesBot = new double[0];
        double[] valTop   = new double[0], valBot   = new double[0];   // smearing sigmas
        double[] muDatTop = new double[0], muDatBot = new double[0];   // data means
        double[] muMcTop  = new double[0], muMcBot  = new double[0];   // MC means
    }

    // =========================================================
    // Configuration
    // =========================================================

    private double  smearingFactor = 1.0;
    private boolean relSmearingP   = false;
    private boolean relSmearingZ0  = false;
    private boolean isData         = false;
    private boolean applyMeanCorr  = false;
    private boolean smearOmega     = false;
    private boolean debug          = false;

    // Flat smearing sigmas — separate top/bottom
    private double pSmearingValueTop     = 0., pSmearingValueBot     = 0.;
    private double z0SmearingValueTop    = 0., z0SmearingValueBot    = 0.;
    private double omegaSmearingValueTop = 0., omegaSmearingValueBot = 0.;

    // Single-value fallback (fixed-value constructor path)
    private double  pSmearingValue    = 0.;
    private double  z0SmearingValue   = 0.;
    private boolean useFixedSmearing  = false;
    private boolean useSeparateTopBot = false;

    // Scalar mean corrections
    private double pMeanDataTop  = 0., pMeanMcTop  = 0.;
    private double pMeanDataBot  = 0., pMeanMcBot  = 0.;
    private double z0MeanDataTop = 0., z0MeanMcTop = 0.;
    private double z0MeanDataBot = 0., z0MeanMcBot = 0.;
    private double omMeanDataTop = 0., omMeanMcTop = 0.;
    private double omMeanDataBot = 0., omMeanMcBot = 0.;

    private boolean hasMeanCorrP     = false;
    private boolean hasMeanCorrZ0    = false;
    private boolean hasMeanCorrOmega = false;

    // Binned smearing tables (from JSON sections named {section}_binned_{var})
    private String binnedLookupVariable = "tanLambda";
    private final Map<String, BinnedParam> pBinned     = new HashMap<>();
    private final Map<String, BinnedParam> omegaBinned = new HashMap<>();
    private final Map<String, BinnedParam> z0Binned    = new HashMap<>();

    private String smearingFile = "";

    // Random number generator
    private final Random random;

    // B-field (T) needed for BaseTrack.setTrackParameters — set via setBField()
    private double bfield = 0.52;

    // =========================================================
    // Constructors
    // =========================================================

    /**
     * Load smearing configuration from the default classpath resource
     * ({@link #DEFAULT_RESOURCE}).
     *
     * @param seed           random seed
     * @param smearingFactor multiplier applied to all sigma values
     */
    public TrackSmearingTool(int seed, double smearingFactor) {
        this.smearingFactor = smearingFactor;
        this.smearingFile   = DEFAULT_RESOURCE;
        this.random         = new Random(seed);
        loadJsonFromResource(DEFAULT_RESOURCE);
    }

    /** No-arg constructor: loads the default resource with seed=42 and smearingFactor=1.0. */
    public TrackSmearingTool() {
        this(42, 1.0);
    }

    /**
     * Load smearing configuration from an external JSON file on disk.
     *
     * @param smearingFile   path to the JSON smearing config
     * @param seed           random seed
     * @param smearingFactor multiplier applied to all sigma values (default 1.0)
     */
    public TrackSmearingTool(String smearingFile, int seed, double smearingFactor) {
        this.smearingFactor = smearingFactor;
        this.smearingFile   = smearingFile;
        this.random         = new Random(seed);
        loadJsonFromFile(smearingFile);
    }

    /** Convenience constructor: loads an external file with seed=42 and smearingFactor=1.0. */
    public TrackSmearingTool(String smearingFile) {
        this(smearingFile, 42, 1.0);
    }

    /**
     * Construct with fixed (flat) smearing values — no JSON file needed.
     *
     * @param pSmearingValue  sigma for p smearing
     * @param z0SmearingValue sigma for z0 smearing
     * @param relSmearingP    if true, p smearing is relative (multiplicative)
     * @param relSmearingZ0   if true, z0 smearing is relative
     * @param seed            random seed
     * @param smearingFactor  multiplier applied to both sigma values
     */
    public TrackSmearingTool(double pSmearingValue, double z0SmearingValue,
                             boolean relSmearingP, boolean relSmearingZ0,
                             int seed, double smearingFactor) {
        this.smearingFactor  = smearingFactor;
        this.relSmearingP    = relSmearingP;
        this.relSmearingZ0   = relSmearingZ0;
        this.pSmearingValue  = pSmearingValue  * smearingFactor;
        this.z0SmearingValue = z0SmearingValue * smearingFactor;
        this.useFixedSmearing = true;
        this.random           = new Random(seed);
        if (debug) {
            System.out.println("TrackSmearingTool: fixed p sigma="  + this.pSmearingValue);
            System.out.println("TrackSmearingTool: fixed z0 sigma=" + this.z0SmearingValue);
        }
    }

    // =========================================================
    // Setters
    // =========================================================

    /** Set the B-field magnitude (T).  Must be called before any update methods. */
    public void setBField(double bfield)       { this.bfield = bfield; }

    /** Declare whether this instance processes data (true) or MC (false, default). */
    public void setIsData(boolean isData)      { this.isData = isData; }

    /** Enable mean correction (data mode only; no effect in MC mode). */
    public void setApplyMeanCorr(boolean apply){ this.applyMeanCorr = apply; }

    public void setDebug(boolean debug)        { this.debug = debug; }

    /**
     * Explicitly select the variable used for binned-smearing lookup.
     *   "flat"      — use scalar top/bot values, disable all binned lookup.
     *   "tanLambda" — default.
     *   "phi0"/"phi", "nHits"/"nhits" — require a matching binned section in the JSON.
     */
    public void setForcedVariable(String var) {
        if (var == null || var.isEmpty()) return;
        if (var.equals("flat")) {
            pBinned.clear(); omegaBinned.clear(); z0Binned.clear();
            System.out.println("TrackSmearingTool: forced to flat (scalar) smearing.");
            return;
        }
        boolean found = pBinned.containsKey(var)
                     || omegaBinned.containsKey(var)
                     || z0Binned.containsKey(var);
        if (!found)
            throw new RuntimeException(
                "TrackSmearingTool: smearingVariable='" + var +
                "' requested but no binned data found for this variable in the JSON.");
        binnedLookupVariable = var;
        System.out.println("TrackSmearingTool: using binned smearing, variable='" + var + "'.");
    }

    // =========================================================
    // Public smearing methods
    // =========================================================

    /**
     * Smear (or correct) the momentum magnitude by modifying omega in-place.
     *
     * @param track  the track to modify (must be a BaseTrack instance)
     * @return scale factor p_smeared / p_original  (1.0 if nothing applied)
     */
    public double updateWithSmearP(Track track) {
        double[] params = track.getTrackParameters();
        double   omega  = params[BaseTrack.OMEGA];
        boolean  isTop  = params[BaseTrack.TANLAMBDA] > 0.;

        // --- Data mode: mean correction only, no Gaussian smearing ---
        if (isData) {
            if (!applyMeanCorr) return 1.0;

            double mu_data = 0., mu_mc = 0.;
            boolean hasMu = false;
            if (pBinned.containsKey(binnedLookupVariable)) {
                BinnedParam bp = pBinned.get(binnedLookupVariable);
                if (bp.muDatTop.length > 0) {
                    double lv = getLookupValue(track);
                    mu_data = isTop ? lookupBinnedValue(bp.edgesTop, bp.muDatTop, lv)
                                    : lookupBinnedValue(bp.edgesBot, bp.muDatBot, lv);
                    mu_mc   = isTop ? lookupBinnedValue(bp.edgesTop, bp.muMcTop,  lv)
                                    : lookupBinnedValue(bp.edgesBot, bp.muMcBot,  lv);
                    hasMu = true;
                }
            }
            if (!hasMu) {
                if (!hasMeanCorrP) return 1.0;
                mu_data = isTop ? pMeanDataTop : pMeanDataBot;
                mu_mc   = isTop ? pMeanMcTop   : pMeanMcBot;
            }

            double scale;
            if (relSmearingP) {
                scale = (mu_data > 0.) ? mu_mc / mu_data : 1.0;
            } else {
                double p = getP(track);
                scale = (p > 0.) ? (p + (mu_mc - mu_data)) / p : 1.0;
            }
            double omegaNew = (scale != 0.) ? omega / scale : omega;
            params[BaseTrack.OMEGA] = omegaNew;
            ((BaseTrack) track).setTrackParameters(params, bfield);
            if (debug)
                System.out.printf("Data p corr: isTop=%b  p=%.5f  scale=%.5f  omega %.5f -> %.5f%n",
                                  isTop, getP(track), scale, omega, omegaNew);
            return scale;
        }

        // --- MC mode: Gaussian smearing ---
        double smearingValue;
        if (pBinned.containsKey(binnedLookupVariable)) {
            BinnedParam bp = pBinned.get(binnedLookupVariable);
            double lv = getLookupValue(track);
            smearingValue = isTop ? lookupBinnedValue(bp.edgesTop, bp.valTop, lv)
                                  : lookupBinnedValue(bp.edgesBot, bp.valBot, lv);
        } else if (useSeparateTopBot) {
            smearingValue = isTop ? pSmearingValueTop : pSmearingValueBot;
        } else {
            smearingValue = pSmearingValue;
        }

        double g = random.nextGaussian();
        double scale;
        if (relSmearingP) {
            // p' = p * (1 + g * sigma_rel)  =>  scale = 1 + g * sigma_rel
            scale = 1.0 + g * smearingValue;
        } else {
            // p' = p + g * sigma_abs  =>  scale = p' / p
            double p = getP(track);
            scale = (p > 0.) ? (p + g * smearingValue) / p : 1.0;
        }
        double omegaNew = (scale != 0.) ? omega / scale : omega;
        params[BaseTrack.OMEGA] = omegaNew;
        ((BaseTrack) track).setTrackParameters(params, bfield);
        if (debug)
            System.out.printf("MC p smear: isTop=%b  sigma=%.5e  scale=%.5f  omega %.5f -> %.5f%n",
                              isTop, smearingValue, scale, omega, omegaNew);
        return scale;
    }

    /**
     * Smear (or correct) z0 in-place.
     *
     * @param track  the track to modify (must be a BaseTrack instance)
     */
    public void updateWithSmearZ0(Track track) {
        double[] params = track.getTrackParameters();
        double   z0     = params[BaseTrack.Z0];
        boolean  isTop  = params[BaseTrack.TANLAMBDA] > 0.;

        // --- Data mode: mean correction only ---
        if (isData) {
            if (!applyMeanCorr) return;
            double mu_data = 0., mu_mc = 0.;
            boolean hasMu = false;
            if (z0Binned.containsKey(binnedLookupVariable)) {
                BinnedParam bp = z0Binned.get(binnedLookupVariable);
                if (bp.muDatTop.length > 0) {
                    double lv = getLookupValue(track);
                    mu_data = isTop ? lookupBinnedValue(bp.edgesTop, bp.muDatTop, lv)
                                    : lookupBinnedValue(bp.edgesBot, bp.muDatBot, lv);
                    mu_mc   = isTop ? lookupBinnedValue(bp.edgesTop, bp.muMcTop,  lv)
                                    : lookupBinnedValue(bp.edgesBot, bp.muMcBot,  lv);
                    hasMu = true;
                }
            }
            if (!hasMu) {
                if (!hasMeanCorrZ0) return;
                mu_data = isTop ? z0MeanDataTop : z0MeanDataBot;
                mu_mc   = isTop ? z0MeanMcTop   : z0MeanMcBot;
            }
            double z0New = z0 + (mu_mc - mu_data);
            params[BaseTrack.Z0] = z0New;
            ((BaseTrack) track).setTrackParameters(params, bfield);
            if (debug)
                System.out.printf("Data z0 corr: isTop=%b  z0=%.5f -> %.5f  shift=%.5f%n",
                                  isTop, z0, z0New, mu_mc - mu_data);
            return;
        }

        // --- MC mode: Gaussian smearing ---
        double smearingValue;
        boolean usedBinned = false;
        double  lookupVal  = 0.;
        if (z0Binned.containsKey(binnedLookupVariable) &&
                z0Binned.get(binnedLookupVariable).valTop.length > 0) {
            BinnedParam bp = z0Binned.get(binnedLookupVariable);
            lookupVal     = getLookupValue(track);
            smearingValue = isTop ? lookupBinnedValue(bp.edgesTop, bp.valTop, lookupVal)
                                  : lookupBinnedValue(bp.edgesBot, bp.valBot, lookupVal);
            usedBinned = true;
        } else {
            smearingValue = isTop ? z0SmearingValueTop : z0SmearingValueBot;
        }

        double sz0  = random.nextGaussian() * smearingValue;
        double z0New = relSmearingZ0 ? z0 + sz0 * z0 : z0 + sz0;
        params[BaseTrack.Z0] = z0New;
        ((BaseTrack) track).setTrackParameters(params, bfield);
        if (debug)
            System.out.printf("MC z0 smear: isTop=%b  src=%s  sigma=%.5e  z0=%.5f -> %.5f%n",
                              isTop,
                              usedBinned ? "binned[" + binnedLookupVariable + "=" + lookupVal + "]" : "flat",
                              smearingValue, z0, z0New);
    }

    /**
     * Smear omega directly (alternative to p smearing — avoids the p-to-omega
     * conversion and thus does not need the actual B-field for the smear itself).
     *
     * @param track  the track to modify (must be a BaseTrack instance)
     * @return |omega_old / omega_new|, the scale factor applied to p (1.0 if nothing applied)
     */
    public double updateWithSmearOmega(Track track) {
        double[] params = track.getTrackParameters();
        double   omega  = params[BaseTrack.OMEGA];
        boolean  isTop  = params[BaseTrack.TANLAMBDA] > 0.;

        // Look up smearing value (binned or flat)
        double smearingValue;
        if (omegaBinned.containsKey(binnedLookupVariable)) {
            BinnedParam bp = omegaBinned.get(binnedLookupVariable);
            double lv = getLookupValue(track);
            smearingValue = isTop ? lookupBinnedValue(bp.edgesTop, bp.valTop, lv)
                                  : lookupBinnedValue(bp.edgesBot, bp.valBot, lv);
        } else {
            smearingValue = isTop ? omegaSmearingValueTop : omegaSmearingValueBot;
        }

        // --- Data mode: mean correction only ---
        if (isData) {
            if (!applyMeanCorr) return 1.0;
            double mu_data = 0., mu_mc = 0.;
            boolean hasMu = false;
            if (omegaBinned.containsKey(binnedLookupVariable)) {
                BinnedParam bp = omegaBinned.get(binnedLookupVariable);
                if (bp.muDatTop.length > 0) {
                    double lv = getLookupValue(track);
                    mu_data = isTop ? lookupBinnedValue(bp.edgesTop, bp.muDatTop, lv)
                                    : lookupBinnedValue(bp.edgesBot, bp.muDatBot, lv);
                    mu_mc   = isTop ? lookupBinnedValue(bp.edgesTop, bp.muMcTop,  lv)
                                    : lookupBinnedValue(bp.edgesBot, bp.muMcBot,  lv);
                    hasMu = true;
                }
            }
            if (!hasMu) {
                if (!hasMeanCorrOmega) return 1.0;
                mu_data = isTop ? omMeanDataTop : omMeanDataBot;
                mu_mc   = isTop ? omMeanMcTop   : omMeanMcBot;
            }
            double omegaNew = omega + (mu_mc - mu_data);
            double scale    = (omegaNew != 0.) ? omega / omegaNew : 1.0;
            params[BaseTrack.OMEGA] = omegaNew;
            ((BaseTrack) track).setTrackParameters(params, bfield);
            if (debug)
                System.out.printf("Data omega corr: isTop=%b  omega=%.5f -> %.5f  scale=%.5f%n",
                                  isTop, omega, omegaNew, scale);
            return Math.abs(scale);
        }

        // --- MC mode: Gaussian smearing ---
        double omegaNew = omega + random.nextGaussian() * smearingValue;
        // Guard against zero curvature after smearing
        if (omegaNew == 0.) omegaNew = omega;
        double scale = omega / omegaNew;
        params[BaseTrack.OMEGA] = omegaNew;
        ((BaseTrack) track).setTrackParameters(params, bfield);
        if (debug)
            System.out.printf("MC omega smear: isTop=%b  sigma=%.5e  omega=%.5f -> %.5f  scale=%.5f%n",
                              isTop, smearingValue, omega, omegaNew, scale);
        return Math.abs(scale);
    }

    /** Print a human-readable summary of the loaded configuration. */
    public void printConfig() {
        StringBuilder sep = new StringBuilder();
        for (int i = 0; i < 72; i++) sep.append('=');
        System.out.println("\n" + sep);
        System.out.println("  TrackSmearingTool");
        System.out.println(sep.toString());
        System.out.println("  JSON:           " + smearingFile);
        System.out.println("  Mode:           " + (isData
                ? "Data  (mean correction only, no Gaussian smearing)"
                : "MC    (Gaussian smearing applied)"));
        System.out.printf ("  smearingFactor: %.4f%n", smearingFactor);
        System.out.println("  relSmearingP:   " + (relSmearingP  ? "relative" : "absolute")
                         + "   relSmearingZ0: " + (relSmearingZ0 ? "relative" : "absolute"));
        System.out.println("  smearOmega:     " + smearOmega);

        System.out.println("\n  [p smearing]  (" + (relSmearingP ? "relative" : "absolute") + ")");
        System.out.printf ("    flat:  top=%.4e   bot=%.4e%n", pSmearingValueTop, pSmearingValueBot);
        if (hasMeanCorrP && pBinned.isEmpty())
            System.out.printf("    mu_data: top=%.4e  bot=%.4e%n    mu_mc:   top=%.4e  bot=%.4e%n",
                              pMeanDataTop, pMeanDataBot, pMeanMcTop, pMeanMcBot);
        if (!pBinned.isEmpty())
            for (Map.Entry<String, BinnedParam> e : pBinned.entrySet())
                printBinTable("p", e.getKey(), e.getValue());
        else
            System.out.println("    (no binned p smearing loaded)");

        System.out.println("\n  [omega smearing]  (absolute)");
        System.out.printf ("    flat:  top=%.4e   bot=%.4e%n", omegaSmearingValueTop, omegaSmearingValueBot);
        if (!omegaBinned.isEmpty())
            for (Map.Entry<String, BinnedParam> e : omegaBinned.entrySet())
                printBinTable("omega", e.getKey(), e.getValue());
        else
            System.out.println("    (no binned omega smearing loaded)");

        System.out.println("\n  [z0 smearing]  (" + (relSmearingZ0 ? "relative" : "absolute") + ")");
        System.out.printf ("    flat:  top=%.4e   bot=%.4e%n", z0SmearingValueTop, z0SmearingValueBot);
        if (!z0Binned.isEmpty())
            for (Map.Entry<String, BinnedParam> e : z0Binned.entrySet())
                printBinTable("z0", e.getKey(), e.getValue());
        else
            System.out.println("    (no binned z0 smearing loaded)");

        System.out.println(sep.toString() + "\n");
    }

    // =========================================================
    // Private helpers
    // =========================================================

    private void loadJsonFromFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            parseJson(br);
        } catch (IOException e) {
            throw new RuntimeException("TrackSmearingTool: cannot read JSON file: " + path, e);
        }
    }

    private void loadJsonFromResource(String resourcePath) {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null)
            throw new RuntimeException(
                "TrackSmearingTool: JSON resource not found on classpath: " + resourcePath);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            parseJson(br);
        } catch (IOException e) {
            throw new RuntimeException(
                "TrackSmearingTool: cannot read JSON resource: " + resourcePath, e);
        }
    }

    private void parseJson(BufferedReader br) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        JSONObject cfg = new JSONObject(sb.toString());

            // --- p smearing ---
            if (cfg.has("pSmearing")) {
                JSONObject ps = cfg.getJSONObject("pSmearing");
                pSmearingValueTop = ps.getDouble("top") * smearingFactor;
                pSmearingValueBot = ps.getDouble("bot") * smearingFactor;
                if (ps.has("mu_data") && ps.has("mu_mc")) {
                    JSONObject md = ps.getJSONObject("mu_data");
                    JSONObject mm = ps.getJSONObject("mu_mc");
                    pMeanDataTop = md.getDouble("top");
                    pMeanDataBot = md.getDouble("bot");
                    pMeanMcTop   = mm.getDouble("top");
                    pMeanMcBot   = mm.getDouble("bot");
                    hasMeanCorrP = true;
                }
            }

            // --- z0 smearing ---
            if (cfg.has("z0Smearing")) {
                JSONObject zs = cfg.getJSONObject("z0Smearing");
                z0SmearingValueTop = zs.getDouble("top") * smearingFactor;
                z0SmearingValueBot = zs.getDouble("bot") * smearingFactor;
                if (zs.has("mu_data") && zs.has("mu_mc")) {
                    JSONObject md = zs.getJSONObject("mu_data");
                    JSONObject mm = zs.getJSONObject("mu_mc");
                    z0MeanDataTop = md.getDouble("top");
                    z0MeanDataBot = md.getDouble("bot");
                    z0MeanMcTop   = mm.getDouble("top");
                    z0MeanMcBot   = mm.getDouble("bot");
                    hasMeanCorrZ0 = true;
                }
            }

            // --- omega smearing ---
            if (cfg.has("omegaSmearing")) {
                JSONObject os = cfg.getJSONObject("omegaSmearing");
                omegaSmearingValueTop = os.getDouble("top") * smearingFactor;
                omegaSmearingValueBot = os.getDouble("bot") * smearingFactor;
                if (os.has("mu_data") && os.has("mu_mc")) {
                    JSONObject md = os.getJSONObject("mu_data");
                    JSONObject mm = os.getJSONObject("mu_mc");
                    omMeanDataTop = md.getDouble("top");
                    omMeanDataBot = md.getDouble("bot");
                    omMeanMcTop   = mm.getDouble("top");
                    omMeanMcBot   = mm.getDouble("bot");
                    hasMeanCorrOmega = true;
                }
            }

            // --- relSmearing flags (support both per-param and legacy shared key) ---
            if      (cfg.has("relSmearingP"))  relSmearingP  = cfg.getBoolean("relSmearingP");
            else if (cfg.has("relSmearing"))   relSmearingP  = cfg.getBoolean("relSmearing");

            if      (cfg.has("relSmearingZ0")) relSmearingZ0 = cfg.getBoolean("relSmearingZ0");
            else if (cfg.has("relSmearing"))   relSmearingZ0 = cfg.getBoolean("relSmearing");

            // --- smearOmega flag ---
            if (cfg.has("smearOmega")) smearOmega = cfg.getBoolean("smearOmega");

            // --- Binned sections (keys of the form {section}_binned_{varname}) ---
            parseBinnedSection(cfg, "pSmearing_binned_",     pBinned,     true);
            parseBinnedSection(cfg, "omegaSmearing_binned_", omegaBinned, true);
            parseBinnedSection(cfg, "z0Smearing_binned_",    z0Binned,    false);

            // Propagate hasMeanCorr flags from any loaded binned section
            for (BinnedParam bp : pBinned.values())
                if (bp.muDatTop.length > 0) { hasMeanCorrP = true; break; }
            for (BinnedParam bp : omegaBinned.values())
                if (bp.muDatTop.length > 0) { hasMeanCorrOmega = true; break; }
            for (BinnedParam bp : z0Binned.values())
                if (bp.muDatTop.length > 0) { hasMeanCorrZ0 = true; break; }

            useFixedSmearing  = true;
            useSeparateTopBot = true;
    }

    /**
     * Parse all JSON keys with the given prefix as binned smearing tables.
     * Each matching key encodes the lookup-variable name after the prefix.
     */
    private void parseBinnedSection(JSONObject cfg, String prefix,
                                    Map<String, BinnedParam> binMap, boolean applyFactor) {
        Iterator<String> keys = cfg.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            if (!k.startsWith(prefix)) continue;
            String var = k.substring(prefix.length());
            if (var.isEmpty()) continue;
            JSONObject v = cfg.getJSONObject(k);
            if (!v.has("top") || !v.has("bot")) continue;

            JSONObject top = v.getJSONObject("top");
            JSONObject bot = v.getJSONObject("bot");
            BinnedParam bp = new BinnedParam();
            bp.edgesTop = toDoubleArray(top.getJSONArray("bin_edges"));
            bp.edgesBot = toDoubleArray(bot.getJSONArray("bin_edges"));

            if (top.has("values")) {
                bp.valTop = toDoubleArray(top.getJSONArray("values"));
                bp.valBot = toDoubleArray(bot.getJSONArray("values"));
                if (applyFactor) {
                    for (int i = 0; i < bp.valTop.length; i++) bp.valTop[i] *= smearingFactor;
                    for (int i = 0; i < bp.valBot.length; i++) bp.valBot[i] *= smearingFactor;
                }
            }
            if (top.has("mu_data") && top.has("mu_mc")) {
                bp.muDatTop = toDoubleArray(top.getJSONArray("mu_data"));
                bp.muMcTop  = toDoubleArray(top.getJSONArray("mu_mc"));
                bp.muDatBot = toDoubleArray(bot.getJSONArray("mu_data"));
                bp.muMcBot  = toDoubleArray(bot.getJSONArray("mu_mc"));
            }
            binMap.put(var, bp);
        }
    }

    /**
     * Step-function (nearest-bin) lookup: return the value for the bin that
     * contains x.  Clamps to the first/last bin if x is out of range.
     */
    private static double lookupBinnedValue(double[] edges, double[] values, double x) {
        if (edges.length < 2 || values.length == 0) return 0.;
        if (x <= edges[0])                 return values[0];
        if (x >= edges[edges.length - 1])  return values[values.length - 1];
        // Binary search
        int lo = 0, hi = edges.length - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (x < edges[mid]) hi = mid;
            else                 lo = mid;
        }
        return values[Math.min(lo, values.length - 1)];
    }

    /**
     * Return the value of the binned-lookup variable for a given track.
     * Default is tanLambda; also supports "phi0"/"phi" and "nHits"/"nhits".
     */
    private double getLookupValue(Track track) {
        double[] params = track.getTrackParameters();
        switch (binnedLookupVariable) {
            case "phi0":  case "phi":
                return params[BaseTrack.PHI];
            case "nHits": case "nhits":
                return (double) track.getTrackerHits().size();
            default:
                return params[BaseTrack.TANLAMBDA];
        }
    }

    /** Return the total momentum magnitude from track Cartesian components. */
    private static double getP(Track track) {
        double px = track.getPX(), py = track.getPY(), pz = track.getPZ();
        return Math.sqrt(px * px + py * py + pz * pz);
    }

    private static double[] toDoubleArray(JSONArray arr) {
        double[] result = new double[arr.length()];
        for (int i = 0; i < arr.length(); i++) result[i] = arr.getDouble(i);
        return result;
    }

    private static void printBinTable(String section, String varName, BinnedParam bp) {
        int n = Math.max(0, bp.edgesTop.length - 1);
        System.out.printf("    binned %s (%s, %d bins):%n", section, varName, n);
        for (int i = 0; i < n; i++) {
            System.out.printf("      [%2d] %.3e  %.3e", i, bp.edgesTop[i], bp.edgesTop[i + 1]);
            if (bp.valTop.length > i)
                System.out.printf("  top_sig=%.3e  bot_sig=%.3e",
                                  bp.valTop[i],
                                  i < bp.valBot.length ? bp.valBot[i] : 0.);
            if (bp.muDatTop.length > i)
                System.out.printf("  muDat=[%.3e,%.3e]  muMc=[%.3e,%.3e]",
                                  bp.muDatTop[i],
                                  i < bp.muDatBot.length ? bp.muDatBot[i] : 0.,
                                  i < bp.muMcTop.length  ? bp.muMcTop[i]  : 0.,
                                  i < bp.muMcBot.length  ? bp.muMcBot[i]  : 0.);
            System.out.println();
        }
    }
}
