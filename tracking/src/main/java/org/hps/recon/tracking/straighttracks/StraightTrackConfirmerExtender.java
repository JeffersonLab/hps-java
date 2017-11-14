/*
 * StraightTrackConfirmerExtender.java
 */
package org.hps.recon.tracking.straighttracks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.recon.tracking.seedtracker.HitManager;
import org.lcsim.recon.tracking.seedtracker.MergeSeedLists;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedLayer;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.recon.tracking.seedtracker.SortHits;
import org.lcsim.recon.tracking.seedtracker.SortLayers;
import org.lcsim.recon.tracking.seedtracker.diagnostic.ISeedTrackerDiagnostics;

/**
 * The StraightTrackConfirmerExtender class attempts to add hits to an input seed. While the algorithms used for the
 * confirm and extend phases are identical, there are small differences in procedures when there are no more tracker
 * layers to check. The confirm phase simply outputs a list of SeedCandidates that have at least the minimum number of
 * confirm hits added to the track. The extend phases completes track finding and imposes the merge criteria to
 * eliminate inferior track candidates when a pair of candidates shares more than one hit.
 *
 * @author cozzy, Richard Partridge 
 * 
 * Modified for HPS straight track fitting by Matt Graham 10/29/2014
 */
public class StraightTrackConfirmerExtender {

    public enum Task {
        CONFIRM, EXTEND;
    }

    private int _nfit;
    private int _maxfit = 1000000000; // initialize maximum number of fits to 10^9
    private HitManager _hmanager;
    private StraightTrackFitter _fitter;
    private MergeSeedLists _merger;
    private List<SeedCandidate> _result;
    private ISeedTrackerDiagnostics _diag = null;

    /**
     * Constructor for the ConfirmerExtender class.
     *
     * @param hitmanager hit manager that provides access to hits sorted by layer / sector
     * @param helixfitter helix fitter
     */
    public StraightTrackConfirmerExtender(HitManager hitmanager, StraightTrackFitter helixfitter) {

        _hmanager = hitmanager;
        _fitter = helixfitter;
        _merger = new MergeSeedLists();
    }

    /**
     * Retrieves the result of the confirmation or extension.
     *
     * @return result The list of confirmed/extended seeds..
     */
    public List<SeedCandidate> getResult() {

        return _result;
    }

    /**
     * Set the maximum number of fit trials for a given seed to be confirmed/extended.
     *
     * @param maxfit maximum number of trials
     */
    public void setMaxFit(int maxfit) {
        _maxfit = maxfit;
    }

    /**
     * Get the number of fit trials for the last confirm/extend.
     *
     * @return number of helix fits
     */
    public int getNFit() {
        return _nfit;
    }

    /**
     * Set the diagnostic class to be used (null for no diagnostics).
     *
     * @param d diagnostic class
     */
    public void setDiagnostics(ISeedTrackerDiagnostics d) {
        _diag = d;
        _merger.setDiagnostic(_diag);
    }

    /**
     * Try to confirm a seed using a specified strategy. The strategy specifies the layers to use in trying to confirm
     * the seed as well as the minimum number of confirm layers that were added to the seed. Typically, there will be a
     * single confirm layer that is required for a seed to be confirmed. Confirmed seeds must also pass the chisq cut
     * and other constraints specified in the strategy.
     *
     * @param seed seed to be confirmed
     * @param strategy strategy to use for confirming this seed
     * @param bfield magnetic field
     * @return true if seed was confirmed
     */
    public boolean Confirm(SeedCandidate seed, SeedStrategy strategy, double bfield) {

        // Create a list to hold the confirmed / extended seeds
        _result = new ArrayList<SeedCandidate>();

        // Establish which layers are to be checked
        seed.setUncheckedLayers(strategy.getLayers(SeedLayer.SeedType.Confirm));

        // Process the seed
        doTask(seed, Task.CONFIRM, strategy, bfield);

        // Return true if we found at least one confirming seed candidate
        return _result.size() > 0;
    }

    /**
     * Try to extend a seed using a specified strategy. The strategy specifies the layers to use in extending the seed
     * as well as the minimum number of layers required for a seed to be a track candidate. Any track candidates found
     * as a result of the extend operation are merged with the list of track candidates found so far to eliminate
     * inferior fits when a pair of track candidates shares more than one hit.
     *
     * @param seed seed to be extended
     * @param strategy strategy to use
     * @param bfield magnetic field
     * @param foundseeds list of track candidates found so far
     */
    public void Extend(SeedCandidate seed, SeedStrategy strategy, double bfield, List<SeedCandidate> foundseeds) {

        // Initialize the list of extended seed candidates to those already found
        _result = foundseeds;

        // Establish which layers are to be checked
        seed.setUncheckedLayers(strategy.getLayers(SeedLayer.SeedType.Extend));

        // Extend the seed and return
        doTask(seed, Task.EXTEND, strategy, bfield);
        return;
    }

    /**
     * Perform the confirm or extend step.
     *
     * @param inputseed seed to be confirmed/extended
     * @param task confirm or extend (enum)
     * @param strategy strategy to use
     * @param bfield magnetic field
     */
    private void doTask(SeedCandidate inputseed, Task task, SeedStrategy strategy, double bfield) {

        // Initialize the counter for the number of fits performed on this seed
        _nfit = 0;

        // // Instantiate the fast hit checker
        // FastCheck checker = new FastCheck(strategy, bfield, _diag);
        // if(this._applySectorBinning) checker.setDoSectorBinCheck(this._hmanager.getSectorManager());

        // Calculate the minimum number of hits to succeed, retrieve the chisq cuts
        int minhits = strategy.getMinHits();
        if (task == Task.CONFIRM)
            minhits = strategy.getMinConfirm() + 3;
        double badhitchisq = strategy.getBadHitChisq();
        double maxchisq = strategy.getMaxChisq();

        // Create a LIFO queue of seeds to be searched for a confirmation/extension
        // hit (note that a LIFO queue is used to minimize memory usage)
        LinkedList<SeedCandidate> seedlist = new LinkedList<SeedCandidate>();

        // The bestseed is a SeedCandidate the meets the requirements for becoming
        // a track, shares at least one hit with the inputseed, and has been deemed
        // the best such seed by the track merging criteria
        //
        // Initialize the best seed to null
        SeedCandidate bestseed = null;

        // If we have already found track candidates, check for duplicates
        // that share hits with the seed, finding the best such duplicate candidate.
        for (SeedCandidate trkcand : _result) {
            if (_merger.isDuplicate(inputseed, trkcand))
                bestseed = findBestCandidate(trkcand, bestseed, strategy);
        }

        // Create a map between the SeedLayers to be checked and a list of hits on the layer to check
        Map<SeedLayer, List<HelicalTrackHit>> hitmap = new HashMap<SeedLayer, List<HelicalTrackHit>>();

        // Loop over the layers to be checked
        for (SeedLayer lyr : inputseed.getUncheckedLayers()) {

            hitmap.put(lyr, _hmanager.getTrackerHits(lyr));
        }

        // Create a list of layers that have hits to check
        List<SeedLayer> lyrlist = new ArrayList<SeedLayer>();
        lyrlist.addAll(hitmap.keySet());

        // Sort the layers in order of increasing number of hits
        SortLayers lyrsort = new SortLayers(hitmap);
        Collections.sort(lyrlist, lyrsort);

        // Store the layers to check in the seed
        inputseed.setUncheckedLayers(lyrlist);

        // Start with the input seed
        seedlist.add(inputseed);

        // Keep looping until we have fully processed all seed candidates
        while (seedlist.size() > 0) {

            // If we have exceeded the maximum number of fits, print warning and stop processing seed candidates
            if (_nfit > _maxfit) {
                System.out.println("Maximum number of fits exceeded in " + task.toString() + " step");
                if (bestseed == null) {
                    System.out.println("No track candidates are associated with the seed hits");
                } else {
                    System.out.println("Track candidate with " + bestseed.getHits().size() + " hits and chisq of "
                            + bestseed.getHelix().chisqtot() + " associated with the seed hits");
                }
                break;
            }

            // Pull the last seed off the queue (use a LIFO queue to minimize queue length)
            SeedCandidate seed = seedlist.poll();

            // Check if there are enough unchecked layers to meet the minimum number of hits
            int lyrsleft = seed.getUncheckedLayers().size();
            int possiblehits = lyrsleft + seed.getHits().size();
            if (possiblehits < minhits)
                continue;

            // If there is a best fit candidate, see if there is still a chance of beating it
            if (bestseed != null) {

                // If the maximimum hits we can achieve is >1 fewer than the best fit, skip this candidate
                int besthits = bestseed.getHits().size();
                if (possiblehits < besthits - 1)
                    continue;

                // If the maximum hits we can achieve equals the best fit, skip if we have a worse chi2
                double chisq = seed.getHelix().chisqtot();
                double bestchisq = seed.getHelix().chisqtot();
                if ((possiblehits == besthits) && chisq > bestchisq)
                    continue;

                // If the maximum hits we can achieve is 1 fewer than the best fit, skip if the bad hit criteria can't
                // be met
                if ((possiblehits == besthits - 1) && (chisq > bestchisq - badhitchisq))
                    continue;
            }

            // See if there are any layers left for confirm/extend
            if (lyrsleft == 0) {

                // Take final action on this seed
                if (task == Task.CONFIRM) {

                    // No more layers and min hit requirement is met, seed is confirmed
                    _result.add(seed);

                } else if (task == Task.EXTEND) {

                    // Merge the seed into the list of extended seeds
                    boolean merged = _merger.Merge(_result, seed, strategy);

                    // If the seed survived the merge, make it our new best candidate
                    if (merged)
                        bestseed = findBestCandidate(seed, bestseed, strategy);
                }

                // Done with this seed
                continue;
            }

            // Pull the next layer off the queue
            SeedLayer lyr = seed.getNextLayer();
            HelicalTrackFit helix = seed.getHelix();

            // Retrieve the chisq for the last fit and initialize the best fit chisq for this layer
            double oldchisq = helix.chisqtot();
            double oldcirclechisq = helix.chisq()[0];
            double chisqbest = 1.e99;

            // Get the list of hits to check for this layer and sort them by x-y distance from current helix
            List<HelicalTrackHit> hitlist = hitmap.get(lyr);
            SortHits comp = new SortHits(helix);
            Collections.sort(hitlist, comp);

            // Loop over the sorted hits in this layer
            for (HelicalTrackHit hit : hitlist) {

                // Make a test seed including the new hit
                SeedCandidate test = new SeedCandidate(seed);
                test.addHit(hit);

                // // Check that this hit is potentially viable
                // if (!checker.CheckHitSeed(hit, seed)) {
                // if (_diag != null) _diag.fireCheckHitFailed(hit, test);
                // continue;
                // }

                // Fit the test seed
                boolean success = _fitter.FitCandidate(test, strategy);
                _nfit++;

                // Check if the fit was successful
                if (success) {

                    // Success - attach the fit to the test seed
                    HelicalTrackFit newhelix = _fitter.getHelix();
                    test.setHelix(newhelix);

                    // Add the seed to the LIFO queue of seed candidates and update the best chisq
                    seedlist.addFirst(test);
                    chisqbest = Math.min(chisqbest, newhelix.chisqtot());

                }
            }

            // Finished checking hits in the current layer. If all the fit trials for
            // this layer are potentially bad hits, include the starting seed (less the
            // current layer, which was popped off the layer queue) in the seed list.
            if (chisqbest - oldchisq > strategy.getBadHitChisq())
                seedlist.addFirst(seed);
        }

        // Finished looping over the seeds in the LIFO candidate queue - we are done!
        return;
    }

    /**
     * Check two track candidates and return the best one subject to the merging criteria.
     *
     * @param trial new candidate to try
     * @param oldbest previous best candidate (or null if no best candidate has been found)
     * @param strategy strategy in use
     * @return best track candidate
     */
    private SeedCandidate findBestCandidate(SeedCandidate trial, SeedCandidate oldbest, SeedStrategy strategy) {

        // If the old best candidate is null, return the trial candidate
        if (oldbest == null)
            return trial;

        // If the trial candidate is better, return it
        if (_merger.isBetter(trial, oldbest, strategy))
            return trial;

        // If no improvement, return the old candidate
        return oldbest;
    }
}