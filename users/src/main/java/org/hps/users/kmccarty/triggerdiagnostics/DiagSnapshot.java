package org.hps.users.kmccarty.triggerdiagnostics;

import org.hps.users.kmccarty.triggerdiagnostics.event.ClusterMatchStatus;
import org.hps.users.kmccarty.triggerdiagnostics.event.ClusterStatModule;
import org.hps.users.kmccarty.triggerdiagnostics.event.TriggerMatchStatus;
import org.hps.users.kmccarty.triggerdiagnostics.event.TriggerStatModule;
import org.hps.users.kmccarty.triggerdiagnostics.util.TriggerDiagnosticUtil;


/**
 * Class <code>DiagSnapshot</code> creates a snapshot of the trigger
 * diagnostics at a specific time that can be passed to other classes.
 * It is entirely static and will not change after creation.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class DiagSnapshot {
	public final ClusterStatModule clusterRunStatistics;
	public final ClusterStatModule clusterLocalStatistics;
	public final SinglesTriggerStatModule singlesRunStatistics;
	public final SinglesTriggerStatModule singlesLocalStatistics;
	public final PairTriggerStatModule pairRunStatistics;
	public final PairTriggerStatModule pairLocalStatistics;
	
	/**
	 * Instantiates a new snapshot. The snapshot creates a copy of the
	 * statistical information stored in the status objects and makes
	 * a copy of this information available to other classes.
	 * @param localCluster - The local cluster data object.
	 * @param globalCluster - The run cluster data object.
	 * @param localSingles - The local singles trigger data object.
	 * @param globalSingles - The run singles trigger data object.
	 * @param localPair - The local pair trigger data object.
	 * @param globalPair - The run pair trigger data object.
	 */
	DiagSnapshot(ClusterMatchStatus localCluster, ClusterMatchStatus globalCluster,
			TriggerMatchStatus localSingles, TriggerMatchStatus globalSingles,
			TriggerMatchStatus localPair, TriggerMatchStatus globalPair) {
		clusterRunStatistics = globalCluster.cloneStatModule();
		clusterLocalStatistics = localCluster.cloneStatModule();
		singlesRunStatistics = new SinglesTriggerStatModule(globalSingles);
		singlesLocalStatistics = new SinglesTriggerStatModule(localSingles);
		pairRunStatistics = new PairTriggerStatModule(globalPair);
		pairLocalStatistics = new PairTriggerStatModule(localPair);
	}
	
	/**
	 * 
	 * Class <code>SinglesTriggerStatModule</code> is a wrapper for the
	 * generic <code>TriggerStatModule</code> that provides specific
	 * methods to obtain cut results rather than needing to reference
	 * a cut index.
	 * 
	 * @author Kyle McCarty <mccarty@jlab.org>
	 */
	public class SinglesTriggerStatModule extends TriggerStatModule {
		/**
		 * Instantiates a <code>SinglesTriggerStatModule</code> with
		 * statistics cloned from the base object.
		 * @param base - The source for the statistical data.
		 */
		SinglesTriggerStatModule(TriggerStatModule base) {
			super(base);
		}
		
		/**
		 * Gets the number of times the cluster energy upper bound cut
		 * failed to match.
		 * @param triggerNumber - The trigger for which to get the value.
		 * @return Returns the number of times the cut failed as an
		 * <code>int</code> primitive.
		 */
		public int getEMaxFailures(int triggerNumber) {
			return getCutFailures(triggerNumber, TriggerDiagnosticUtil.SINGLES_ENERGY_MAX);
		}
		
		/**
		 * Gets the number of times the cluster energy lower bound cut
		 * failed to match.
		 * @param triggerNumber - The trigger for which to get the value.
		 * @return Returns the number of times the cut failed as an
		 * <code>int</code> primitive.
		 */
		public int getEMinFailures(int triggerNumber) {
			return getCutFailures(triggerNumber, TriggerDiagnosticUtil.SINGLES_ENERGY_MIN);
		}
		
		/**
		 * Gets the number of times the cluster hit count cut failed
		 * to match.
		 * @param triggerNumber - The trigger for which to get the value.
		 * @return Returns the number of times the cut failed as an
		 * <code>int</code> primitive.
		 */
		public int getHitCountFailures(int triggerNumber) {
			return getCutFailures(triggerNumber, TriggerDiagnosticUtil.SINGLES_HIT_COUNT);
		}
	}
	
	/**
	 * 
	 * Class <code>PairTriggerStatModule</code> is a wrapper for the
	 * generic <code>TriggerStatModule</code> that provides specific
	 * methods to obtain cut results rather than needing to reference
	 * a cut index.
	 * 
	 * @author Kyle McCarty <mccarty@jlab.org>
	 */
	public class PairTriggerStatModule extends TriggerStatModule {
		/**
		 * Instantiates a <code>PairTriggerStatModule</code> with
		 * statistics cloned from the base object.
		 * @param base - The source for the statistical data.
		 */
		PairTriggerStatModule(TriggerStatModule base) {
			super(base);
		}
		
		/**
		 * Gets the number of times the pair energy sum cut failed to
		 * match.
		 * @param triggerNumber - The trigger for which to get the value.
		 * @return Returns the number of times the cut failed as an
		 * <code>int</code> primitive.
		 */
		public int getESumFailures(int triggerNumber) {
			return getCutFailures(triggerNumber, TriggerDiagnosticUtil.PAIR_ENERGY_SUM);
		}
		
		/**
		 * Gets the number of times the pair energy difference cut failed
		 * to match.
		 * @param triggerNumber - The trigger for which to get the value.
		 * @return Returns the number of times the cut failed as an
		 * <code>int</code> primitive.
		 */
		public int getEDiffFailures(int triggerNumber) {
			return getCutFailures(triggerNumber, TriggerDiagnosticUtil.PAIR_ENERGY_DIFF);
		}
		
		/**
		 * Gets the number of times the pair energy slope cut failed
		 * to match.
		 * @param triggerNumber - The trigger for which to get the value.
		 * @return Returns the number of times the cut failed as an
		 * <code>int</code> primitive.
		 */
		public int getESlopeFailures(int triggerNumber) {
			return getCutFailures(triggerNumber, TriggerDiagnosticUtil.PAIR_ENERGY_SLOPE);
		}
		
		/**
		 * Gets the number of times the pair coplanarity cut failed to
		 * match.
		 * @param triggerNumber - The trigger for which to get the value.
		 * @return Returns the number of times the cut failed as an
		 * <code>int</code> primitive.
		 */
		public int getCoplanarityFailures(int triggerNumber) {
			return getCutFailures(triggerNumber, TriggerDiagnosticUtil.PAIR_COPLANARITY);
		}
	}
}
