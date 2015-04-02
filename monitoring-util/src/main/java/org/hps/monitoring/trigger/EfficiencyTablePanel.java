package org.hps.monitoring.trigger;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.hps.analysis.trigger.data.DiagnosticSnapshot;
import org.hps.analysis.trigger.data.TriggerStatModule;
import org.hps.analysis.trigger.util.ComponentUtils;

public class EfficiencyTablePanel extends AbstractTablePanel implements DiagnosticUpdatable {
	// Static variables.
	private static final long serialVersionUID = 0L;
	
	// Table models.
	private TableTextModel localModel;
	private TableTextModel globalModel;
	
	// Column/row reference variables.
	private static final int ROWS = 7;
	private static final int COLUMNS = 6;
	/* private static final int COL_HEADER    = 0;
	private static final int COL_SINGLES_0 = 1;
	private static final int COL_SINGLES_1 = 2;
	private static final int COL_PAIR_0    = 3;
	private static final int COL_PAIR_1    = 4; */
	private static final int COL_COUNT     = 5;
	/* private static final int ROW_HEADER    = 0;
	private static final int ROW_PULSER    = 1;
	private static final int ROW_COSMIC    = 2;
	private static final int ROW_SINGLES_0 = 3;
	private static final int ROW_SINGLES_1 = 4;
	private static final int ROW_PAIR_0    = 5;
	private static final int ROW_PAIR_1    = 6; */
	
	// Global/local reference variables.
	private static final int GLOBAL = 0;
	private static final int LOCAL  = 1;
	
	// Trigger type reference variables.
	private static final int TYPE_SINGLES_0 = TriggerStatModule.SINGLES_0;
	private static final int TYPE_SINGLES_1 = TriggerStatModule.SINGLES_1;
	private static final int TYPE_PAIR_0    = TriggerStatModule.PAIR_0;
	private static final int TYPE_PAIR_1    = TriggerStatModule.PAIR_1;
	
	// Column/row header names.
	private static final String[] COL_NAMES = {
		"", "Singles 0", "Singles 1", "Pair 0", "Pair 1", "Count"
	};
	private static final String[] ROW_NAMES = {
		"", "Random", "Cosmic", "Singles 0", "Singles 1", "Pair 0", "Pair 1"
	};
	
	/**
	 * Instantiates a new <code>EfficiencyTablePanel</code>.
	 */
	public EfficiencyTablePanel() {
		// Instantiate the superclass.
		super();
		
		// Set the orientation to vertical.
		setOrientation(ORIENTATION_VERTICAL);
	}
	
	@Override
	public void updatePanel(DiagnosticSnapshot runSnapshot, DiagnosticSnapshot localSnapshot) {
		// If there is no snapshot, the tables should all display an
		// empty value.
		if(runSnapshot == null || localSnapshot == null) {
			for(int row = 1; row < ROWS; row++) {
				for(int col = 1; col < COLUMNS; col++) {
					localModel.setValueAt("---", row, col);
					globalModel.setValueAt("---", row, col);
				}
			}
		}
		
		// Otherwise, update the table cells from the snapshot data.
		else {
			// Get the efficiency modules.
			DiagnosticSnapshot[] stat = new DiagnosticSnapshot[2];
			stat[GLOBAL] = runSnapshot;
			stat[LOCAL] = localSnapshot;
			
			// Get the trigger count for each trigger type for both the
			// local and global snapshots.
			int[][][] matched = new int[2][4][6];
			int[][][] triggers = new int[2][4][6];
			for(int i = 0; i < 2; i++) {
				for(int triggerType = 0; triggerType < 6; triggerType++) {
					// Get the total triggers seen for each type.
					triggers[i][TYPE_SINGLES_0][triggerType] = stat[i].getSingles0Stats().getSSPSimulatedTriggers(triggerType);
					triggers[i][TYPE_SINGLES_1][triggerType] = stat[i].getSingles1Stats().getSSPSimulatedTriggers(triggerType);
					triggers[i][TYPE_PAIR_0][triggerType] = stat[i].getPair0Stats().getSSPSimulatedTriggers(triggerType);
					triggers[i][TYPE_PAIR_1][triggerType] = stat[i].getPair1Stats().getSSPSimulatedTriggers(triggerType);
					
					// Get the total triggers matched for each type.
					matched[i][TYPE_SINGLES_0][triggerType] = stat[i].getSingles0Stats().getMatchedSSPSimulatedTriggers(triggerType);
					matched[i][TYPE_SINGLES_1][triggerType] = stat[i].getSingles1Stats().getMatchedSSPSimulatedTriggers(triggerType);
					matched[i][TYPE_PAIR_0][triggerType] = stat[i].getPair0Stats().getMatchedSSPSimulatedTriggers(triggerType);
					matched[i][TYPE_PAIR_1][triggerType] = stat[i].getPair1Stats().getMatchedSSPSimulatedTriggers(triggerType);
				}
			}
			
			// Determine the spacing needed to display the largest numerical
			// cell value.
			int numWidth = -1;
			for(int tiTriggerType = 0; tiTriggerType < 6; tiTriggerType++) {
				for(int seenTriggerType = 0; seenTriggerType < 4; seenTriggerType++) {
					int rSize = ComponentUtils.getDigits(triggers[GLOBAL][seenTriggerType][tiTriggerType]);
					int lSize = ComponentUtils.getDigits(triggers[LOCAL][seenTriggerType][tiTriggerType]);
					numWidth = ComponentUtils.max(numWidth, rSize, lSize);
				}
			}
			
			// Generate the format string for the cells.
			String format = "%" + numWidth + "d / %" + numWidth + "d";
			
			// Update the table.
			for(int tiTriggerType = 0; tiTriggerType < 6; tiTriggerType++) {
				// Fill the row/column combinations that hold trigger
				// statistical information.
				for(int seenTriggerType = 0; seenTriggerType < 4; seenTriggerType++) {
					// Fill the local table cell.
					String localText = String.format(format, matched[LOCAL][seenTriggerType][tiTriggerType],
							triggers[LOCAL][seenTriggerType][tiTriggerType]);
					if(triggers[LOCAL][seenTriggerType][tiTriggerType] == 0) {
						localText = localText + " (  N/A  %)";
					} else {
						localText = String.format("%s (%7.3f%%)", localText,
								(100.0 * matched[LOCAL][seenTriggerType][tiTriggerType] / triggers[LOCAL][seenTriggerType][tiTriggerType]));
					}
					localModel.setValueAt(localText, tiTriggerType + 1, seenTriggerType + 1);
					
					// Fill the global table cell.
					String globalText = String.format(format, matched[GLOBAL][seenTriggerType][tiTriggerType],
							triggers[GLOBAL][seenTriggerType][tiTriggerType]);
					if(triggers[GLOBAL][seenTriggerType][tiTriggerType] == 0) {
						globalText = globalText + " (  N/A  %)";
					} else {
						globalText = String.format("%s (%7.3f%%)", globalText,
								(100.0 * matched[LOCAL][seenTriggerType][tiTriggerType] / triggers[GLOBAL][seenTriggerType][tiTriggerType]));
					}
					globalModel.setValueAt(globalText, tiTriggerType + 1, seenTriggerType + 1);
				}
				
				// Populate the count column.
				localModel.setValueAt("" + stat[LOCAL].getTITriggers(tiTriggerType, true), tiTriggerType + 1, COL_COUNT);
				globalModel.setValueAt("" + stat[GLOBAL].getTITriggers(tiTriggerType, true), tiTriggerType + 1, COL_COUNT);
			}
		}
	}
	
	@Override
	protected JTable[] initializeTables(Object... args) {
		// Initialize the table models. There should be one row and
		// one column for each type of trigger plus an additional one
		// of each for headers.
		localModel = new TableTextModel(ROWS, COLUMNS);
		globalModel = new TableTextModel(ROWS, COLUMNS);
		
		// Set the column headers.
		for(int col = 0; col < COLUMNS; col++) {
			localModel.setValueAt(COL_NAMES[col], 0, col);
			globalModel.setValueAt(COL_NAMES[col], 0, col);
		}
		
		// Set the row headers.
		for(int row = 0; row < ROWS; row++) {
			localModel.setValueAt(ROW_NAMES[row], row, 0);
			globalModel.setValueAt(ROW_NAMES[row], row, 0);
		}
		
		// Make a cell renderer.
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		
		// Create JTable objects to display the data.
		JTable localTable = new JTable(localModel);
		localTable.setRowSelectionAllowed(false);
		localTable.setColumnSelectionAllowed(false);
		localTable.setCellSelectionEnabled(false);
		localTable.setShowVerticalLines(false);
		localTable.getColumnModel().getColumn(0).setMaxWidth(150);
		localTable.getColumnModel().getColumn(COL_COUNT).setMaxWidth(150);
		for(int col = 1; col < COLUMNS; col++) {
			localTable.getColumnModel().getColumn(col).setCellRenderer(centerRenderer);
		}
		localTable.setFont(new Font("monospaced", localTable.getFont().getStyle(), localTable.getFont().getSize()));
		
		JTable globalTable = new JTable(globalModel);
		globalTable.setRowSelectionAllowed(false);
		globalTable.setColumnSelectionAllowed(false);
		globalTable.setCellSelectionEnabled(false);
		globalTable.setShowVerticalLines(false);
		globalTable.getColumnModel().getColumn(0).setMaxWidth(150);
		globalTable.getColumnModel().getColumn(COL_COUNT).setMaxWidth(150);
		for(int col = 1; col < COLUMNS; col++) {
			globalTable.getColumnModel().getColumn(col).setCellRenderer(centerRenderer);
		}
		globalTable.setFont(new Font("monospaced", globalTable.getFont().getStyle(), globalTable.getFont().getSize()));
		
		// Return the tables.
		return new JTable[] { localTable, globalTable };
	}
	
}