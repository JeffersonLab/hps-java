package org.hps.monitoring.trigger;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import org.hps.analysis.trigger.util.ComponentUtils;

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
	public static final int ORIENTATION_HORIZONTAL = 0;
	public static final int ORIENTATION_VERTICAL   = 1;
	
	// Components.
	private JLabel localHeader;
	private JLabel globalHeader;
	protected final JTable localTable;
	protected final JTable globalTable;
	
	// Component parameters.
	private boolean horizontal = true;
	private Dimension userPrefSize = null;
	private Dimension defaultPrefSize = new Dimension(0, 0);
	
	/**
	 * Instantiates an <code>AbstractTablePanel</code>.
	 * @param args Arguments to be used when generating the panel tables.
	 */
	public AbstractTablePanel(Object... args) {
		// Initialize the tables.
		JTable[] tables = initializeTables(args);
		localTable = tables[0];
		globalTable = tables[1];
		add(globalTable);
		add(localTable);
		
		// Set the panels to their null starting values.
		updatePanel(null);
		
		// Define the panel layout.
		setLayout(null);
		
		// Create header labels for the tables.
		localHeader = new JLabel("Local Statistics");
		localHeader.setHorizontalAlignment(JLabel.CENTER);
		add(localHeader);
		
		globalHeader = new JLabel("Run Statistics");
		globalHeader.setHorizontalAlignment(JLabel.CENTER);
		add(globalHeader);
		
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
	
	/**
	 * Sets the orientation of components on the panel.
	 * @param orientation - The orientation identifier. Identifiers can
	 * be obtained as static variables from the within the root object
	 * <code>AbstractTable</code>.
	 */
	public void setOrientation(int orientation) {
		if(orientation == ORIENTATION_HORIZONTAL) {
			if(!horizontal) {
				horizontal = true;
				positionComponents();
			}
		} else if(orientation == ORIENTATION_VERTICAL) {
			if(horizontal) {
				horizontal = false;
				positionComponents();
			}
		} else {
			throw new IllegalArgumentException("Invalid orienation identifier.");
		}
	}
	
	@Override
	public void setPreferredSize(Dimension preferredSize) {
		userPrefSize = preferredSize;
	}
	
	/**
	 * Generates the two tables that are used by the component. This
	 * must return an array of size two.
	 * @param args - Any arguments that should be passed to the method
	 * for generating tables.
	 * @return Returns an array of size two, where the first index must
	 * contain the local table and the second index the global table.
	 */
	protected abstract JTable[] initializeTables(Object... args);
	
	/**
	 * Repositions the components to the correct places on the parent
	 * <code>JPanel</code>. This should be run whenever the panel
	 * changes size.
	 */
	private void positionComponents() {
		// Do not update if the components have not been initialized.
		if(localHeader == null) { return; }
		
		// If the components should be position horizontally...
		if(horizontal) {
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
		
		// Otherwise, position them vertically.
		else {
			// Place the header labels. These are given their preferred
			// height. Note that this means a very small panel may cut off
			// some of the components. First, get the preferred height of
			// the label with the larger preferred height. These should be
			// the same thing, but just in case...
			int labelHeight = localHeader.getPreferredSize().height;
			if(labelHeight < globalHeader.getPreferredSize().height) {
				labelHeight = globalHeader.getPreferredSize().height;
			}
			
			// The local components go first, taking up the entire upper
			// width of the panel.
			localHeader.setBounds(0, 0, getWidth(), labelHeight);
			localTable.setBounds(0, ComponentUtils.getNextY(localHeader, ComponentUtils.vinternal),
					getWidth(), localTable.getPreferredSize().height);
			
			// The global components go immediately below.
			globalHeader.setBounds(0, ComponentUtils.getNextY(localTable, ComponentUtils.vinternal),
					getWidth(), labelHeight);
			globalTable.setBounds(0, ComponentUtils.getNextY(globalHeader, ComponentUtils.vinternal),
					getWidth(), globalTable.getPreferredSize().height);
		}
	}
}
