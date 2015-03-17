package org.hps.monitoring.trigger;

import org.hps.analysis.trigger.DiagSnapshot;
import org.hps.analysis.trigger.event.ClusterStatModule;
import org.hps.analysis.trigger.util.ComponentUtils;

/**
 * Class <code>ClusterTablePanel</code> is an implementation of class
 * <code>AbstractTablePanel</code> for cluster statistical data.<br/>
 * <br/>
 * This implements the interface <code>DiagnosticUpdatable</code>.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see AbstractTablePanel
 */
public class ClusterTablePanel extends AbstractTwoColumnTablePanel {
	// Static variables.
	private static final long serialVersionUID = 0L;
	private static final String[] TABLE_TITLES = { "Recon Clusters", "SSP Clusters", "Matched Clusters",
			"Failed (Position)", "Failed (Energy)", "Failed (Hit Count)" };
	
	// Table model mappings.
	private static final int ROW_RECON_COUNT      = 0;
	private static final int ROW_SSP_COUNT        = 1;
	private static final int ROW_MATCHED          = 2;
	private static final int ROW_FAILED_POSITION  = 3;
	private static final int ROW_FAILED_ENERGY    = 4;
	private static final int ROW_FAILED_HIT_COUNT = 5;
	
	/**
	 * Instantiate a new <code>ClusterTablePanel</code>.
	 */
	public ClusterTablePanel() { super(TABLE_TITLES); }
	
	@Override
	public void updatePanel(DiagSnapshot snapshot) {
		// If the snapshot is null, all values should be "N/A."
		if(snapshot == null) {
			// Output cluster count data.
			String scalerNullValue = "---";
			setLocalRowValue(ROW_RECON_COUNT,  scalerNullValue);
			setLocalRowValue(ROW_SSP_COUNT,    scalerNullValue);
			setGlobalRowValue(ROW_RECON_COUNT, scalerNullValue);
			setGlobalRowValue(ROW_SSP_COUNT,   scalerNullValue);
			
			// Output the tracked statistical data.
			String percentNullValue = "--- / --- (---%)";
			setLocalRowValue(ROW_MATCHED,           percentNullValue);
			setLocalRowValue(ROW_FAILED_POSITION,   percentNullValue);
			setLocalRowValue(ROW_FAILED_ENERGY,     percentNullValue);
			setLocalRowValue(ROW_FAILED_HIT_COUNT,  percentNullValue);
			setGlobalRowValue(ROW_MATCHED,          percentNullValue);
			setGlobalRowValue(ROW_FAILED_POSITION,  percentNullValue);
			setGlobalRowValue(ROW_FAILED_ENERGY,    percentNullValue);
			setGlobalRowValue(ROW_FAILED_HIT_COUNT, percentNullValue);
		}
		
		// Otherwise, populate the table with the diagnostic data.
		else {
			// Get the cluster statistical banks.
			ClusterStatModule lstat = snapshot.clusterLocalStatistics;
			ClusterStatModule rstat = snapshot.clusterRunStatistics;
			
			// Get the largest number of digits in any of the values.
			int mostDigits = ComponentUtils.max(lstat.getReconClusterCount(), lstat.getSSPClusterCount(), lstat.getMatches(),
					lstat.getPositionFailures(), lstat.getEnergyFailures(), lstat.getHitCountFailures(),
					rstat.getReconClusterCount(), rstat.getSSPClusterCount(), rstat.getMatches(),
					rstat.getPositionFailures(), rstat.getEnergyFailures(), rstat.getHitCountFailures());
			int spaces = ComponentUtils.getDigits(mostDigits);
			
			// Put the number of reconstructed and SSP clusters into
			// the tables.
			int[] clusterValue = {
					lstat.getReconClusterCount(),
					lstat.getSSPClusterCount(),
					rstat.getReconClusterCount(),
					rstat.getSSPClusterCount()
			};
			String countFormat = "%" + spaces + "d";
			setLocalRowValue(ROW_RECON_COUNT,  String.format(countFormat, clusterValue[0]));
			setLocalRowValue(ROW_SSP_COUNT,    String.format(countFormat, clusterValue[1]));
			setGlobalRowValue(ROW_RECON_COUNT, String.format(countFormat, clusterValue[2]));
			setGlobalRowValue(ROW_SSP_COUNT,   String.format(countFormat, clusterValue[3]));
			
			// Output the tracked statistical data.
			int total;
			String percentFormat = "%" + spaces + "d / %" + spaces + "d (%7.3f)";
			int[] statValue = {
					lstat.getMatches(),
					lstat.getPositionFailures(),
					lstat.getEnergyFailures(),
					lstat.getHitCountFailures(),
					rstat.getMatches(),
					rstat.getPositionFailures(),
					rstat.getEnergyFailures(),
					rstat.getHitCountFailures()
			};
			
			total = lstat.getReconClusterCount();
			setLocalRowValue(ROW_MATCHED,          String.format(percentFormat, statValue[0], total, 100.0 * statValue[0] / total));
			setLocalRowValue(ROW_FAILED_POSITION,  String.format(percentFormat, statValue[1], total, 100.0 * statValue[1] / total));
			setLocalRowValue(ROW_FAILED_ENERGY,    String.format(percentFormat, statValue[2], total, 100.0 * statValue[2] / total));
			setLocalRowValue(ROW_FAILED_HIT_COUNT, String.format(percentFormat, statValue[3], total, 100.0 * statValue[3] / total));
			
			total = rstat.getReconClusterCount();
			setGlobalRowValue(ROW_MATCHED,          String.format(percentFormat, statValue[4], total, 100.0 * statValue[4] / total));
			setGlobalRowValue(ROW_FAILED_POSITION,  String.format(percentFormat, statValue[5], total, 100.0 * statValue[5] / total));
			setGlobalRowValue(ROW_FAILED_ENERGY,    String.format(percentFormat, statValue[6], total, 100.0 * statValue[6] / total));
			setGlobalRowValue(ROW_FAILED_HIT_COUNT, String.format(percentFormat, statValue[7], total, 100.0 * statValue[7] / total));
		}
	}
}