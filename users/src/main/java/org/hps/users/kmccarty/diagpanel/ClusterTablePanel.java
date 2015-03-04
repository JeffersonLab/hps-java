package org.hps.users.kmccarty.diagpanel;

import org.hps.users.kmccarty.DiagSnapshot;

/**
 * Class <code>ClusterTablePanel</code> is an implementation of class
 * <code>AbstractTablePanel</code> for cluster statistical data.<br/>
 * <br/>
 * This implements the interface <code>DiagnosticUpdatable</code>.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see AbstractTablePanel
 */
public class ClusterTablePanel extends AbstractTablePanel {
	// Static variables.
	private static final long serialVersionUID = 0L;
	private static final String[] TABLE_TITLES = { "Recon Clusters:", "SSP Clusters", "Matched Clusters",
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
			/*
			 * This is disabled until the snapshot object is stable and
			 * is subject to change. It will not work if enabled now.
			// Get the largest number of digits in any of the values.
			int mostDigits = 0;
			for(int valueID = 0; valueID < DiagSnapshot.CL_BANK_SIZE; valueID++) {
				int localDigits = ComponentUtils.getDigits(snapshot.getClusterValue(LOCAL, valueID));
				int globalDigits = ComponentUtils.getDigits(snapshot.getClusterValue(GLOBAL, valueID));
				mostDigits = ComponentUtils.max(mostDigits, localDigits, globalDigits);
			}
			
			// Put the number of reconstructed and SSP clusters into
			// the tables.
			int[] clusterValue = {
					snapshot.getClusterValue(LOCAL,  DiagSnapshot.CL_VALUE_RECON_CLUSTERS),
					snapshot.getClusterValue(LOCAL,  DiagSnapshot.CL_VALUE_SSP_CLUSTERS),
					snapshot.getClusterValue(GLOBAL, DiagSnapshot.CL_VALUE_RECON_CLUSTERS),
					snapshot.getClusterValue(GLOBAL, DiagSnapshot.CL_VALUE_SSP_CLUSTERS)
			};
			String countFormat = "%" + mostDigits + "d";
			setLocalRowValue(ROW_RECON_COUNT,  String.format(countFormat, clusterValue[0]));
			setLocalRowValue(ROW_SSP_COUNT,    String.format(countFormat, clusterValue[1]));
			setGlobalRowValue(ROW_RECON_COUNT, String.format(countFormat, clusterValue[2]));
			setGlobalRowValue(ROW_SSP_COUNT,   String.format(countFormat, clusterValue[3]));
			
			// Output the tracked statistical data.
			int total;
			String percentFormat = "%" + mostDigits + "d / %" + mostDigits + "d (%7.3f)";
			int[] statValue = {
					snapshot.getClusterValue(DiagSnapshot.TYPE_LOCAL, DiagSnapshot.CL_VALUE_MATCHED),
					snapshot.getClusterValue(DiagSnapshot.TYPE_LOCAL, DiagSnapshot.CL_VALUE_FAIL_POSITION),
					snapshot.getClusterValue(DiagSnapshot.TYPE_LOCAL, DiagSnapshot.CL_VALUE_FAIL_ENERGY),
					snapshot.getClusterValue(DiagSnapshot.TYPE_LOCAL, DiagSnapshot.CL_VALUE_FAIL_HIT_COUNT),
					snapshot.getClusterValue(DiagSnapshot.TYPE_GLOBAL, DiagSnapshot.CL_VALUE_MATCHED),
					snapshot.getClusterValue(DiagSnapshot.TYPE_GLOBAL, DiagSnapshot.CL_VALUE_FAIL_POSITION),
					snapshot.getClusterValue(DiagSnapshot.TYPE_GLOBAL, DiagSnapshot.CL_VALUE_FAIL_ENERGY),
					snapshot.getClusterValue(DiagSnapshot.TYPE_GLOBAL, DiagSnapshot.CL_VALUE_FAIL_HIT_COUNT)
			};
			
			total = snapshot.getClusterValue(DiagSnapshot.TYPE_LOCAL, DiagSnapshot.CL_VALUE_RECON_CLUSTERS);
			setLocalRowValue(ROW_MATCHED,          String.format(percentFormat, statValue[0], total, 100.0 * statValue[0] / total));
			setLocalRowValue(ROW_FAILED_POSITION,  String.format(percentFormat, statValue[1], total, 100.0 * statValue[1] / total));
			setLocalRowValue(ROW_FAILED_ENERGY,    String.format(percentFormat, statValue[2], total, 100.0 * statValue[2] / total));
			setLocalRowValue(ROW_FAILED_HIT_COUNT, String.format(percentFormat, statValue[3], total, 100.0 * statValue[3] / total));
			
			total = snapshot.getClusterValue(DiagSnapshot.TYPE_GLOBAL, DiagSnapshot.CL_VALUE_RECON_CLUSTERS);
			setGlobalRowValue(ROW_MATCHED,          String.format(percentFormat, statValue[4], total, 100.0 * statValue[4] / total));
			setGlobalRowValue(ROW_FAILED_POSITION,  String.format(percentFormat, statValue[5], total, 100.0 * statValue[5] / total));
			setGlobalRowValue(ROW_FAILED_ENERGY,    String.format(percentFormat, statValue[6], total, 100.0 * statValue[6] / total));
			setGlobalRowValue(ROW_FAILED_HIT_COUNT, String.format(percentFormat, statValue[7], total, 100.0 * statValue[7] / total));
			*/
		}
	}
}