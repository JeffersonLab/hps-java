package org.hps.users.kmccarty.diagpanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

/**
 * Class <code>AbstractTablePanel</code> displays two <code>JTable</code>
 * objects side-by-side with headers above them. The left table displays
 * statistical data for recent events processed with trigger diagnostics
 * while the right table displays the same, but over the course of the
 * entire run.<br/>
 * <br/>
 * This implements the interface <code>DiagnosticUpdatable</code>.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see JPanel
 * @see DiagnosticUpdatable
 */
public abstract class AbstractTablePanel extends JPanel implements DiagnosticUpdatable {
	// Static variables.
	private static final long serialVersionUID = 0L;
	
	// Table models.
	private final TableTextModel localModel;
	private final TableTextModel globalModel;
	
	// Components.
	private JTable localTable;
	private JLabel localHeader;
	private JTable globalTable;
	private JLabel globalHeader;
	private Dimension defaultPrefSize = new Dimension(0, 0);
	private Dimension userPrefSize = null;
	
	// Table model mappings.
	private static final int COL_TITLE = 0;
	private static final int COL_VALUE = 1;
	
	/**
	 * Instantiates an <code>AbstractTablePanel</code> with a number
	 * of rows equal to the length of the argument array. Note that
	 * the panel requires that there be at least one row.
	 * @param rowNames - An array of <code>String</code> objects that
	 * are to be displayed for the names of the table rows.
	 */
	public AbstractTablePanel(String[] rowNames) {
		// Initialize the table models. They should have two columns
		// (one for values and one for headers) and a number of rows
		// equal to the number of row names.
		localModel = new TableTextModel(rowNames.length, 2);
		globalModel = new TableTextModel(rowNames.length, 2);
		
		// Initialize the titles.
		for(int i = 0; i < rowNames.length; i++) {
			localModel.setValueAt(rowNames[i], i, COL_TITLE);
			globalModel.setValueAt(rowNames[i], i, COL_TITLE);
		}
		updatePanel(null);
		
		// Define the panel layout.
		//SpringLayout layout = new SpringLayout();
		setLayout(null);
		
		// Create header labels for the tables.
		localHeader = new JLabel("Local Statistics");
		localHeader.setHorizontalAlignment(JLabel.CENTER);
		add(localHeader);
		
		globalHeader = new JLabel("Run Statistics");
		globalHeader.setHorizontalAlignment(JLabel.CENTER);
		add(globalHeader);
		
		// Create JTable objects to display the data.
		localTable = new JTable(localModel);
		localTable.setRowSelectionAllowed(false);
		localTable.setColumnSelectionAllowed(false);
		localTable.setCellSelectionEnabled(false);
		add(localTable);
		
		globalTable = new JTable(globalModel);
		globalTable.setRowSelectionAllowed(false);
		globalTable.setColumnSelectionAllowed(false);
		globalTable.setCellSelectionEnabled(false);
		add(globalTable);
		
		// Track when the component changes size and reposition the
		// components accordingly.
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) { positionComponents(); }
		});
		
		// Define the component preferred size.
		defaultPrefSize.width = localTable.getPreferredSize().width +
				ComponentUtils.hinternal + globalTable.getPreferredSize().width;
		defaultPrefSize.height = localTable.getPreferredSize().height +
				ComponentUtils.vinternal + globalTable.getPreferredSize().height;
	}
	
	@Override
	public Dimension getPreferredSize() {
		// If there is a user-specified preferred size, return that.
		if(userPrefSize == null) { return defaultPrefSize; }
		
		// Otherwise, return the default calculated preferred size.
		else { return userPrefSize; }
	}
	
	@Override
	public void setBackground(Color bg) {
		// Set the base component background.
		super.setBackground(bg);
		
		// If the components have been initialized, pass the background
		// color change to them as appropriate. Note that the tables
		// will always retain the same background color.
		if(localTable != null) {
			// Set the header backgrounds.
			localHeader.setBackground(bg);
			globalHeader.setBackground(bg);
		}
	}
	
	@Override
	public void setFont(Font font) {
		// Set the base component font.
		super.setFont(font);
		
		// If the components have been initialized, pass the font change
		// to them as appropriate.
		if(localTable != null) {
			// Set the table fonts.
			localTable.setFont(font);
			globalTable.setFont(font);
			
			// Set the header fonts.
			Font headerFont = font.deriveFont(Font.BOLD, (float) Math.ceil(font.getSize2D() * 1.3));
			localHeader.setFont(headerFont);
			globalHeader.setFont(headerFont);
		}
	}
	
	@Override
	public void setForeground(Color fg) {
		// Set the base component foreground.
		super.setForeground(fg);
		
		// If the components have been initialized, pass the foreground
		// color change to them as appropriate. Note that the tables
		// will always retain the same foreground color.
		if(localTable != null) {
			// Set the header foregrounds.
			localHeader.setForeground(fg);
			globalHeader.setForeground(fg);
		}
	}
	
	@Override
	public void setPreferredSize(Dimension preferredSize) {
		userPrefSize = preferredSize;
	}
	
	/**
	 * Sets the value of the indicated row for the global statistical
	 * table.
	 * @param rowIndex - The row.
	 * @param value - The new value.
	 */
	protected void setGlobalRowValue(int rowIndex, String value) {
		globalModel.setValueAt(value, rowIndex, COL_VALUE);
	}
	
	/**
	 * Sets the value of the indicated row for the local statistical
	 * table.
	 * @param rowIndex - The row.
	 * @param value - The new value.
	 */
	protected void setLocalRowValue(int rowIndex, String value) {
		localModel.setValueAt(value, rowIndex, COL_VALUE);
	}
	
	/**
	 * Repositions the components to the correct places on the parent
	 * <code>JPanel</code>. This should be run whenever the panel
	 * changes size.
	 */
	private void positionComponents() {
		// Do not update if the components have not been initialized.
		if(localHeader == null) { return; }
		
		// The local components get the left half of the panel and the
		// global components the right. Find half of the panel width,
		// accounting for the internal spacing. This is an internal
		// component, so it does not employ additional spacing between
		// itself and the parent component's edges.
		int compWidth = (getWidth() - 10) / 2;
		
		// If there is any width remaining, it goes to the spacing.
		int horizontal = ComponentUtils.hinternal + (getWidth() - 10) % 2;
		
		// Place the header labels. These are given their preferred
		// height. Note that this means a very small panel may cut off
		// some of the components. First, get the preferred height of
		// the label with the larger preferred height. These should be
		// the same thing, but just in case...
		int labelHeight = localHeader.getPreferredSize().height;
		if(labelHeight < globalHeader.getPreferredSize().height) {
			labelHeight = globalHeader.getPreferredSize().height;
		}
		
		// Set the label sizes and positions.
		localHeader.setBounds(0, 0, compWidth, labelHeight);
		globalHeader.setLocation(ComponentUtils.getNextX(localHeader, horizontal), 0);
		globalHeader.setSize(compWidth, labelHeight);
		
		// The tables go under their respective labels and should fill
		// the remainder of the label height.
		int tableY = ComponentUtils.getNextY(localHeader, ComponentUtils.vinternal);
		localTable.setBounds(0, tableY, compWidth, localTable.getPreferredSize().height);
		globalTable.setBounds(globalHeader.getX(), tableY, compWidth, globalTable.getPreferredSize().height);
	}
}
