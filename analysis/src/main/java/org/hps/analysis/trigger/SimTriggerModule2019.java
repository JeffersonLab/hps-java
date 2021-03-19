package org.hps.analysis.trigger;

import java.util.ArrayList;
import java.util.List;

import org.hps.analysis.trigger.util.PairTrigger2019;
import org.hps.analysis.trigger.util.SinglesTrigger2019;

/**
 * Class <code>SimTriggerModule2019</code> is a container class that holds
 * simulated trigger results for each of the four primary triggers
 * produced by the <code>DataTriggerSimDriver2019</code>.
 * 
 * Class <code>SimTriggerModule2019</code> is developed based on Class <code>SimTriggerModule</code> 
 * 
 * @param <E> - The type of clusters from which the triggers were
 * simulated. This will always be either <code>Cluster</code> or
 * <code>VTPCluster</code>. 
 * @see DataTriggerSimDriver
 */
public class SimTriggerModule2019<E> {
    private final List<SinglesTrigger2019<E>> singles0;
    private final List<SinglesTrigger2019<E>> singles1;
    private final List<SinglesTrigger2019<E>> singles2;
    private final List<SinglesTrigger2019<E>> singles3;
    private final List<PairTrigger2019<E[]>> pair0;
    private final List<PairTrigger2019<E[]>> pair1;
    private final List<PairTrigger2019<E[]>> pair2;
    private final List<PairTrigger2019<E[]>> pair3;
    
    /**
     * Constructs a new <code>SimTriggerModule2019</code> with the no
     * triggers results for any triggers.
     */
    SimTriggerModule2019() {
        singles0 = new ArrayList<SinglesTrigger2019<E>>(0);
        singles1 = new ArrayList<SinglesTrigger2019<E>>(0);
        singles2 = new ArrayList<SinglesTrigger2019<E>>(0);
        singles3 = new ArrayList<SinglesTrigger2019<E>>(0);
        pair0    = new ArrayList<PairTrigger2019<E[]>>(0);
        pair1    = new ArrayList<PairTrigger2019<E[]>>(0);
        pair2    = new ArrayList<PairTrigger2019<E[]>>(0);
        pair3    = new ArrayList<PairTrigger2019<E[]>>(0);
    }

    /**
     * Constructs a new <code>SimTriggerModule2019</code> with the specified trigger
     * results for each of the four primary triggers.
     * 
     * @param singles0Triggers - The results for the singles 0 trigger.
     * @param singles1Triggers - The results for the singles 1 trigger.
     * @param singles2Triggers - The results for the singles 2 trigger.
     * @param singles3Triggers - The results for the singles 3 trigger.
     * @param pair0Triggers    - The results for the pair 0 trigger.
     * @param pair1Triggers    - The results for the pair 1 trigger.
     * @param pair2Triggers    - The results for the pair 2 trigger.
     * @param pair3Triggers    - The results for the pair 3 trigger.
     */
    SimTriggerModule2019(List<SinglesTrigger2019<E>> singles0Triggers,
            List<SinglesTrigger2019<E>> singles1Triggers, List<SinglesTrigger2019<E>> singles2Triggers,
            List<SinglesTrigger2019<E>> singles3Triggers, List<PairTrigger2019<E[]>> pair0Triggers,
            List<PairTrigger2019<E[]>> pair1Triggers, List<PairTrigger2019<E[]>> pair2Triggers,
            List<PairTrigger2019<E[]>> pair3Triggers) {
        this.singles0 = singles0Triggers;
        this.singles1 = singles1Triggers;
        this.singles2 = singles2Triggers;
        this.singles3 = singles3Triggers;
        this.pair0 = pair0Triggers;
        this.pair1 = pair1Triggers;
        this.pair2 = pair2Triggers;
        this.pair3 = pair3Triggers;
    }

    /**
     * Gets the simulated trigger results for the indicated singles trigger.
     * Note that only inputs of <code>0</code>, <code>1</code>, <code>2</code> and <code>3</code> are
     * allowed.
     * @param triggerNumber - A value of either <code>0</code>, to
     * obtain the singles 0 trigger results, or <code>1</code>, to obtain
     * the singles 1 trigger results, or <code>2</code>, to obtain
     * the singles 2 trigger results, or <code>3</code>, to obtain
     * the singles 3 trigger results.
     * @return Returns the trigger results as a <code>List</code> of
     * <code>SinglesTrigger2019</code> objects.
     * @throws IllegalArgumentException Occurs if the input argument
     * is not either <code>0</code> or <code>1</code> or <code>2</code> or <code>3</code>.
     */
    public List<SinglesTrigger2019<E>> getSinglesTriggers(int triggerNumber) {
        // Return the appropriate trigger list.
        if(triggerNumber == 0) { return getSingles0Triggers(); }
        else if(triggerNumber == 1) { return getSingles1Triggers(); }
        else if(triggerNumber == 2) { return getSingles2Triggers(); }
        else if(triggerNumber == 3) { return getSingles3Triggers(); }
        
        // Any other trigger number is not valid and should produce an
        // exception.
        throw new IllegalArgumentException("Trigger number " + triggerNumber + " is not valid.");
    }
    
    /**
     * Gets the simulated trigger results for the singles 0 trigger.
     * @return Returns the trigger results as a <code>List</code> of
     * <code>SinglesTrigger2019</code> objects.
     */
    public List<SinglesTrigger2019<E>> getSingles0Triggers() {
        return singles0;
    }
    
    /**
     * Gets the simulated trigger results for the singles 1 trigger.
     * @return Returns the trigger results as a <code>List</code> of
     * <code>SinglesTrigger2019</code> objects.
     */
    public List<SinglesTrigger2019<E>> getSingles1Triggers() {
        return singles1;
    }
    
    /**
     * Gets the simulated trigger results for the singles 2 trigger.
     * @return Returns the trigger results as a <code>List</code> of
     * <code>SinglesTrigger2019</code> objects.
     */
    public List<SinglesTrigger2019<E>> getSingles2Triggers() {
        return singles2;
    }
    
    /**
     * Gets the simulated trigger results for the singles 3 trigger.
     * @return Returns the trigger results as a <code>List</code> of
     * <code>SinglesTrigger2019</code> objects.
     */
    public List<SinglesTrigger2019<E>> getSingles3Triggers() {
        return singles3;
    }
    
    /**
     * Gets the simulated trigger results for the indicated pair trigger.
     * Note that only inputs of <code>0</code>, <code>1</code>, <code>2</code> and <code>3</code> are
     * allowed.
     * @param triggerNumber - A value of either <code>0</code>, to
     * obtain the pair 0 trigger results, or <code>1</code>, to obtain
     * the pair 1 trigger results, or <code>2</code>, to obtain
     * the pair 2 trigger results, or <code>3</code>, to obtain
     * the pair 3 trigger results.
     * @return Returns the trigger results as a <code>List</code> of
     * <code>PairTrigger2019</code> objects.
     * @throws IllegalArgumentException Occurs if the input argument
     * is not either <code>0</code> or <code>1</code> or <code>2</code> or <code>3</code>.
     */
    public List<PairTrigger2019<E[]>> getPairTriggers(int triggerNumber) {
        // Return the appropriate trigger list.
        if(triggerNumber == 0) { return getPair0Triggers(); }
        else if(triggerNumber == 1) { return getPair1Triggers(); }
        else if(triggerNumber == 2) { return getPair2Triggers(); }
        else if(triggerNumber == 3) { return getPair3Triggers(); }
        
        // Any other trigger number is not valid and should produce an
        // exception.
        throw new IllegalArgumentException("Trigger number " + triggerNumber + " is not valid.");
    }
    
    /**
     * Gets the simulated trigger results for the pair 0 trigger.
     * @return Returns the trigger results as a <code>List</code> of
     * <code>PairTrigger2019</code> objects.
     */
    public List<PairTrigger2019<E[]>> getPair0Triggers() {
        return pair0;
    }
    
    /**
     * Gets the simulated trigger results for the pair 1 trigger.
     * @return Returns the trigger results as a <code>List</code> of
     * <code>PairTrigger2019</code> objects.
     */
    public List<PairTrigger2019<E[]>> getPair1Triggers() {
        return pair1;
    }
    
    /**
     * Gets the simulated trigger results for the pair 2 trigger.
     * @return Returns the trigger results as a <code>List</code> of
     * <code>PairTriggerConfig2019</code> objects.
     */
    public List<PairTrigger2019<E[]>> getPair2Triggers() {
        return pair2;
    }
    
    /**
     * Gets the simulated trigger results for the pair 3 trigger.
     * @return Returns the trigger results as a <code>List</code> of
     * <code>PairTriggerConfig2019</code> objects.
     */
    public List<PairTrigger2019<E[]>> getPair3Triggers() {
        return pair3;
    }
}
