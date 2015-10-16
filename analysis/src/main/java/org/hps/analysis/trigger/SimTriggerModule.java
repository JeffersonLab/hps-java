package org.hps.analysis.trigger;

import java.util.ArrayList;
import java.util.List;

import org.hps.analysis.trigger.util.PairTrigger;
import org.hps.analysis.trigger.util.SinglesTrigger;

/**
 * Class <code>SimTriggerModule</code> is a container class that holds
 * simulated trigger results for each of the four primary triggers
 * produced by the <code>DataTriggerSimDriver</code>.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <E> - The type of clusters from which the triggers were
 * simulated. This will always be either <code>Cluster</code> or
 * <code>SSPCluster</code>. 
 * @see DataTriggerSimDriver
 */
public class SimTriggerModule<E> {
	private final List<SinglesTrigger<E>> singles0;
	private final List<SinglesTrigger<E>> singles1;
	private final List<PairTrigger<E[]>> pair0;
	private final List<PairTrigger<E[]>> pair1;
	
	/**
	 * Constructs a new <code>SimTriggerModule</code> with the no
	 * triggers results for any triggers.
	 */
	SimTriggerModule() {
		singles0 = new ArrayList<SinglesTrigger<E>>(0);
		singles1 = new ArrayList<SinglesTrigger<E>>(0);
		pair0    = new ArrayList<PairTrigger<E[]>>(0);
		pair1    = new ArrayList<PairTrigger<E[]>>(0);
	}
	
	/**
	 * Constructs a new <code>SimTriggerModule</code> with the specified
	 * trigger results for each of the four primary triggers.
	 * @param singles0Triggers - The results for the singles 0 trigger.
	 * @param singles1Triggers - The results for the singles 1 trigger.
	 * @param pair0Triggers - The results for the pair 0 trigger.
	 * @param pair1Triggers - The results for the pair 1 trigger.
	 */
	SimTriggerModule(List<SinglesTrigger<E>> singles0Triggers, List<SinglesTrigger<E>> singles1Triggers,
			List<PairTrigger<E[]>> pair0Triggers, List<PairTrigger<E[]>> pair1Triggers) {
		this.singles0 = singles0Triggers;
		this.singles1 = singles1Triggers;
		this.pair0    = pair0Triggers;
		this.pair1    = pair1Triggers;
	}
	
	/**
	 * Gets the simulated trigger results for the singles 0 trigger.
	 * @return Returns the trigger results as a <code>List</code> of
	 * <code>SinglesTrigger</code> objects.
	 */
	public List<SinglesTrigger<E>> getSingles0Triggers() {
		return singles0;
	}
	
	/**
	 * Gets the simulated trigger results for the singles 1 trigger.
	 * @return Returns the trigger results as a <code>List</code> of
	 * <code>SinglesTrigger</code> objects.
	 */
	public List<SinglesTrigger<E>> getSingles1Triggers() {
		return singles1;
	}
	
	/**
	 * Gets the simulated trigger results for the pair 0 trigger.
	 * @return Returns the trigger results as a <code>List</code> of
	 * <code>PairTrigger</code> objects.
	 */
	public List<PairTrigger<E[]>> getPair0Triggers() {
		return pair0;
	}
	
	/**
	 * Gets the simulated trigger results for the pair 1 trigger.
	 * @return Returns the trigger results as a <code>List</code> of
	 * <code>PairTrigger</code> objects.
	 */
	public List<PairTrigger<E[]>> getPair1Triggers() {
		return pair1;
	}
}
