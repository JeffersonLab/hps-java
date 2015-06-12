package org.hps.monitoring.trigger;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import org.hps.analysis.trigger.data.DiagnosticSnapshot;
import org.hps.analysis.trigger.util.ComponentUtils;

public class ShifterTrigPanel extends JPanel {
	private static final Color BG_WARNING = new Color(255, 235, 20);
	private static final Color BG_CRITICAL = new Color(230, 0, 0);
	private static final Color FONT_WARNING = new Color(255, 157, 0);
	private static final Color FONT_CRITICAL = new Color(117, 0, 0);
	private static final long serialVersionUID = 1L;
	
	private JLabel panelTitle;
	private JLabel[] fieldTitle;
	private JLabel[] fieldValue;
	
	/**
	 * Instantiates a new <code>ShifterTrigPanel</code> with the
	 * indicated name.
	 */
	public ShifterTrigPanel(String name) {
		// Instantiate a layout for the fields.
		SpringLayout layout = new SpringLayout();
		setLayout(layout);
		
		// Instantiate the header title.
		panelTitle = new JLabel(name);
		panelTitle.setVerticalAlignment(JLabel.CENTER);
		panelTitle.setHorizontalAlignment(JLabel.CENTER);
		add(panelTitle);
		
		// Instantiate the field title labels.
		String[] titleName = { "Cluster Efficiency", "Singles 0 Logic Efficiency",
				"Singles 0 Trigger Efficiency", "Singles 1 Logic Efficiency",
				"Singles 1 Trigger Efficiency", "Pair 0 Logic Efficiency",
				"Pair 0 Trigger Efficiency", "Pair 1 Logic Efficiency", "Pair 1 Trigger Efficiency" };
		fieldTitle = new JLabel[titleName.length];
		for(int index = 0; index < titleName.length; index++) {
			fieldTitle[index] = new JLabel(titleName[index]);
			fieldTitle[index].setVerticalAlignment(JLabel.CENTER);
			fieldTitle[index].setHorizontalAlignment(JLabel.RIGHT);
			fieldTitle[index].setOpaque(true);
			add(fieldTitle[index]);
		}
		
		// Instantiate the field value labels.
		fieldValue = new JLabel[titleName.length];
		for(int index = 0; index < titleName.length; index++) {
			fieldValue[index] = new JLabel("");
			fieldValue[index].setVerticalAlignment(JLabel.CENTER);
			fieldValue[index].setHorizontalAlignment(JLabel.LEFT);
			fieldValue[index].setOpaque(true);
			add(fieldValue[index]);
		}
		
		// Get the longest title.
		int maxWidth = -1;
		int maxIndex = -1;
		for(int index = 0; index < titleName.length; index++) {
			int width = fieldTitle[index].getFontMetrics(fieldTitle[index].getFont()).stringWidth(titleName[index]);
			if(width > maxWidth) {
				maxWidth = width;
				maxIndex = index;
			}
		}
		
		// Define border edge and spacing variables.
		String EAST = SpringLayout.EAST;
		String WEST = SpringLayout.WEST;
		String NORTH = SpringLayout.NORTH;
		String SOUTH = SpringLayout.SOUTH;
		int hinternal =  5;
		int vinternal = 10;
		int hexternal =  5;
		int vexternal =  5;
		
		// Position the panel header.
		layout.putConstraint(EAST,  panelTitle, hexternal, EAST,  this);
		layout.putConstraint(WEST,  panelTitle, hexternal, WEST,  this);
		layout.putConstraint(NORTH, panelTitle, vexternal, NORTH, this);
		
		// Position the field entries.
		Component lastComp = panelTitle;
		for(int index = 0; index < titleName.length; index++) {
			// For all field titles except the largest, lock the right
			// edge of the title to match the position of the largest
			// title's right edge. The largest title is allowed to size
			// itself to its preferred width.
			if(index == maxIndex) {
				layout.putConstraint(NORTH, fieldTitle[index], vinternal, SOUTH, lastComp);
				layout.putConstraint(WEST,  fieldTitle[index], hexternal, WEST,  this);
			} else {
				layout.putConstraint(NORTH, fieldTitle[index], vinternal, SOUTH, lastComp);
				layout.putConstraint(WEST,  fieldTitle[index], hexternal, WEST,  this);
				layout.putConstraint(EAST,  fieldTitle[index],         0, EAST,  fieldTitle[maxIndex]);
			}
			
			// Position the field value label to the right of the field
			// title label. It should use up the remainder of the width
			// allowed by the component.
			layout.putConstraint(WEST,  fieldValue[index], hinternal, EAST,  fieldTitle[index]);
			layout.putConstraint(EAST,  fieldValue[index], hexternal, EAST,  this);
			layout.putConstraint(NORTH, fieldValue[index], vinternal, SOUTH, lastComp);
			
			// Update the "last component" to the current field title
			// label.
			lastComp = fieldTitle[index];
		}
		
		// Update the fonts.
		setFont(getFont());
	}
	
	@Override
	public void setBackground(Color bg) {
		// Set the superclass background.
		super.setBackground(bg);
		
		// Set the component backgrounds.
		if(panelTitle != null) {
			panelTitle.setBackground(bg);
			for(int index = 0; index < fieldTitle.length; index++) {
				fieldTitle[index].setBackground(bg);
				
				// If the field value label has a special alert color,
				// then do not overwrite it.
				if(!fieldValue[index].getBackground().equals(BG_WARNING)
						&& !fieldValue[index].getBackground().equals(BG_CRITICAL)) {
					fieldValue[index].setBackground(bg);
				}
			}
		}
	}
	
	@Override
	public void setFont(Font font) {
		// Set the superclass font.
		super.setFont(font);
		
		// Set the component fonts.
		if(panelTitle != null) {
			panelTitle.setFont(font.deriveFont(Font.BOLD, (float) (font.getSize2D() * 1.5)));
			for(int index = 0; index < fieldTitle.length; index++) {
				fieldTitle[index].setFont(font.deriveFont(Font.BOLD));
				fieldValue[index].setFont(font);
			}
		}
	}
	
	@Override
	public void setForeground(Color fg) {
		// Set the superclass foreground.
		super.setForeground(fg);
		
		// Set the component backgrounds.
		if(panelTitle != null) {
			panelTitle.setForeground(fg);
			for(int index = 0; index < fieldTitle.length; index++) {
				fieldTitle[index].setForeground(fg);
				
				// If the field value label has a special alert color,
				// then do not overwrite it.
				if(!fieldValue[index].getForeground().equals(FONT_WARNING)
						&& !fieldValue[index].getForeground().equals(FONT_CRITICAL)) {
					fieldValue[index].setBackground(fg);
				}
			}
		}
	}
	
	/**
	 * Updates the panel statistical display with data from the
	 * argument snapshot.
	 * @param stat - The snapshot from which to derive statistical
	 *               data.
	 */
	public void updatePanel(DiagnosticSnapshot stat) {
		// If the snapshot is null, insert "null" values in the
		// field panels,
		if(stat == null) {
			// Populate the fields with a "null" entry.
			for(int index = 0; index < fieldValue.length; index++) {
				fieldValue[index].setText("--- / --- (  N/A  %)");
			}
			
			// No data exists, so no further processing is needed.
			return;
		}
		
		// Define index constants.
		int RECON = 0;
		int SSP = 1;
		int TRIGGER_0 = 0;
		int TRIGGER_1 = 1;
		
		// Get the tracked values from the snapshot.
		int seenClusters = stat.getClusterStats().getReconClusterCount();
		int[][] seenSinglesTriggers = {
			{ stat.getSingles0Stats().getReconSimulatedTriggers(), stat.getSingles1Stats().getReconSimulatedTriggers() },
			{ stat.getSingles0Stats().getSSPSimulatedTriggers(), stat.getSingles1Stats().getSSPSimulatedTriggers() }
		};
		int[][] seenPairTriggers = {
                        { stat.getPair0Stats().getReconSimulatedTriggers(), stat.getPair1Stats().getReconSimulatedTriggers() },
                        { stat.getPair0Stats().getSSPSimulatedTriggers(), stat.getPair1Stats().getSSPSimulatedTriggers() }
		};
		int matchedClusters = stat.getClusterStats().getMatches();
		int[][] matchedSinglesTriggers = {
                        { stat.getSingles0Stats().getMatchedReconSimulatedTriggers(), stat.getSingles1Stats().getMatchedReconSimulatedTriggers() },
                        { stat.getSingles0Stats().getMatchedSSPSimulatedTriggers(), stat.getSingles1Stats().getMatchedSSPSimulatedTriggers() }
		};
		int[][] matchedPairTriggers = {
                        { stat.getPair0Stats().getMatchedReconSimulatedTriggers(), stat.getPair1Stats().getMatchedReconSimulatedTriggers() },
                        { stat.getPair0Stats().getMatchedSSPSimulatedTriggers(), stat.getPair1Stats().getMatchedSSPSimulatedTriggers() }
		};
		
		// Get the largest digit of the tracked values. This should
		// always be one of the "seen" values.
		int mostDigits = ComponentUtils.max(seenClusters, seenSinglesTriggers[0][0], seenSinglesTriggers[0][1],
				seenSinglesTriggers[1][0], seenSinglesTriggers[1][1], seenPairTriggers[0][0], seenPairTriggers[0][1],
				seenPairTriggers[1][0], seenPairTriggers[1][1]);
		int spaces = ComponentUtils.getDigits(mostDigits);
		
		// Populate the cluster field panel.
		processEfficiency(seenClusters, matchedClusters, 0, spaces, 0.98, 0.94);
		
		// Populate the singles trigger field panels.
		processEfficiency(seenSinglesTriggers[RECON][TRIGGER_0], matchedSinglesTriggers[RECON][TRIGGER_0], 1, spaces, 0.99, 0.95);
                processEfficiency(seenSinglesTriggers[SSP][TRIGGER_0],   matchedSinglesTriggers[SSP][TRIGGER_0],   2, spaces, 0.99, 0.95);
                processEfficiency(seenSinglesTriggers[RECON][TRIGGER_1], matchedSinglesTriggers[RECON][TRIGGER_1], 3, spaces, 0.99, 0.95);
                processEfficiency(seenSinglesTriggers[SSP][TRIGGER_1],   matchedSinglesTriggers[SSP][TRIGGER_1],   4, spaces, 0.99, 0.95);
		
		// Populate the pair trigger field panels.
		processEfficiency(seenPairTriggers[RECON][TRIGGER_0], matchedPairTriggers[RECON][TRIGGER_0], 5, spaces, 0.99, 0.95);
                processEfficiency(seenPairTriggers[SSP][TRIGGER_0],   matchedPairTriggers[SSP][TRIGGER_0],   6, spaces, 0.99, 0.95);
                processEfficiency(seenPairTriggers[RECON][TRIGGER_1], matchedPairTriggers[RECON][TRIGGER_1], 7, spaces, 0.99, 0.95);
                processEfficiency(seenPairTriggers[SSP][TRIGGER_1],   matchedPairTriggers[SSP][TRIGGER_1],   8, spaces, 0.99, 0.95);
	}
	
	/**
	 * Updates the indicated field value using the indicated number
	 * seen and matched elements. Automatically handles the special
	 * case of zero seen elements and also updates the colors of the
	 * field labels to the appropriate color based on the efficiency
	 * and the thresholds for warnings.
	 * @param seen - The number of elements seen.
	 * @param matched - The number of elements matched.
	 * @param fieldIndex - The index for the field that should display
	 *                     the statistical data.
	 * @param spaces - The number of spaces to giveto each displayed
	 *                 value.
	 * @param threshWarning - The threshold at which the "warning
	 *                        color should be used.
	 * @param threshCritical - The threshold at which the "critical"
	 *                         color should be used.
	 */
	private void processEfficiency(int seen, int matched, int fieldIndex, int spaces, double threshWarning, double threshCritical) {
		// Calculate the efficiency.
		double efficiency = 100.0 * matched / seen;
		
		// Create the format string.
		String format = "%" + spaces + "d / %";
		
		// If the number of values seen is zero, there is no
		// percentage that can be calculated.
		if(seen == 0) {
			fieldValue[fieldIndex].setText(String.format(format + " (  N/A  %%)", seen, matched));
		}
		
		// Otherwise, include the percentage.
		else {
			fieldValue[fieldIndex].setText(String.format(format + " (7.3f%%)", seen, matched, efficiency));
		}
		
		// If the efficiency is below the critical threshold,
		// change the field background to the critical color.
		if(efficiency < threshCritical) {
			fieldValue[fieldIndex].setBackground(BG_CRITICAL);
			fieldValue[fieldIndex].setForeground(FONT_CRITICAL);
		}
		
		// Otherwise, if the efficiency is below the warning
		// level, set the field background to the warning color.
		else if(efficiency < threshWarning) {
			fieldValue[fieldIndex].setBackground(BG_WARNING);
			fieldValue[fieldIndex].setForeground(FONT_WARNING);
		}
		
		// Otherwise, use the default component background.
		else {
			fieldValue[fieldIndex].setBackground(getBackground());
			fieldValue[fieldIndex].setForeground(getForeground());
		}
	}
}
