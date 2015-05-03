package org.hps.monitoring.trigger;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import org.hps.analysis.trigger.data.DiagnosticSnapshot;

public class ShifterTrigPanel extends JPanel {
	private static final Color BG_WARNING = new Color(255, 235, 20);
	private static final Color BG_CRITICAL = new Color(230, 0, 0);
	private static final Color FONT_WARNING = new Color(255, 157, 0);
	private static final Color FONT_CRITICAL = new Color(117, 0, 0);
	private static final long serialVersionUID = 1L;
	
	private JLabel panelTitle;
	private JLabel[] fieldTitle;
	private JLabel[] fieldValue;
	
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
	
	public void updatePanel(DiagnosticSnapshot snapshot) {
		
	}
}
