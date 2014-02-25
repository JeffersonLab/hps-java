package org.hps.monitoring.ecal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Class <code>StatusPanel</code> displays text in a set of fields.
 *
 *@author Kyle McCarty
 */
public class StatusPanel extends JPanel {
	private static final long serialVersionUID = -8353479383875379010L;
	private int leftBuffer = 10;
	private int upperBuffer = 10;
	private BackPanel background = new BackPanel();
	private JLabel[][] field;
	private static final String nullValue = "---";
	
	/**
	 * <b>StatusPanel</b><br/><br/>
	 * <code>public <b>StatusPanel</b>(String... fieldName)</code><br/><br/>
	 * Creates a new status panel with display fields with the indicated
	 * names. They will be assigned a field index in the order that they
	 * are given starting with zero.
	 * @param fieldName - The names of the fields to display.
	 */
	public StatusPanel(String... fieldName) {
		// Initialize the component.
		super();
		
		// Set the layout manager to manual.
		setLayout(null);
		
		// Build the text fields.
		int curZ = 0;
		field = new JLabel[fieldName.length][2];
		for(int i = 0; i < field.length; i++) {
			for(int j = 0; j < field[i].length; j++) {
				field[i][j] = new JLabel();
				field[i][j].setOpaque(true);
				field[i][j].setBackground(Color.WHITE);
				add(field[i][j]);
				setComponentZOrder(field[i][j], curZ);
				curZ++;
			}
			field[i][0].setText(fieldName[i] + ":");
		}
		
		// Start the fields as null by default.
		clearValues();
		
		// Build the background panel.
		add(background);
		setComponentZOrder(background, curZ);
	}
	
	/**
	 * <b>setFieldValue</b><br/><br/>
	 * Sets the value of the indicated field.
	 * @param index - The field's index.
	 * @param value - The new value to display.
	 * @throws IndexOutOfBoundsException Occurs when the field index
	 * is neither more than the existing number of fields or is negative.
	 */
	public void setFieldValue(int index, String value) throws IndexOutOfBoundsException {
		if(index >= 0 && index < field.length) {
			if(value == null) { field[index][1].setText(nullValue); }
			else  { field[index][1].setText(value); }
		}
		else { throw new IndexOutOfBoundsException("Invalid field index."); }
	}
	
	public void setSize(int width, int height) {
		super.setSize(width, height);
		resize();
	}
	
	public void setSize(Dimension d) {
		super.setSize(d);
		resize();
	}
	
	/**
	 * <b>clearValues</b><br/><br/>
	 * <code>public void <b>clearValues</b>()</code><br/><br/>
	 * Sets all of the fields on the status display to the null value.
	 */
	public void clearValues() {
		for(int i = 0; i < field.length; i++) {
			field[i][1].setText(nullValue);
		}
	}
	
	private void resize() {
		// Define the width an height as convenience variables.
		int width = getWidth();
		int height = getHeight();
		
		// Size the background panel.
		background.setBounds(0, 0, width, height);
		
		// Size and place the text labels.
		if(field.length != 0) {
			int labelHeight = (height - (int)(upperBuffer + 5)) / field.length;
			int labelRem = (height - upperBuffer - 8) % field.length;
			int curY = (int)(upperBuffer + 2);
			for(int i = 0; i < field.length; i++) {
				// Determine the appropriate field height.
				int thisHeight = labelHeight;
				if(labelRem > 0) {
					thisHeight++;
					labelRem--;
				}
			
				// Place the field.
				field[i][0].setBounds(leftBuffer, curY, 65, thisHeight);
				field[i][1].setBounds(getNextX(field[i][0]), curY, 100, thisHeight);
				
				// Increment the current position.
				curY += thisHeight;
			}
		}
	}
	
	/**
	 *<b>getNextX</b><br/><br/>
	 * <code>private int <b>getNextX</b>(Component c)</code><br/><br/>
	 * Finds the x-coordinate immediately after the component.
	 * @param c - The component of which to find the end.
	 * @return Returns the x-coordinate at the end of the component. 
	 */
	private final static int getNextX(Component c) { return getNextX(c, 0); }
	
	/**
	/**
	 *<b>getNextX</b><br/><br/>
	 * <code>private int <b>getNextX</b>(Component c, int buffer)</code><br/><br/>
	 * Finds the x-coordinate after the component with a given buffer.
	 * @param c - The component of which to find the end.
	 * @param buffer - The extra space after the component to be included.
	 * @return Returns the x-coordinate at the end of the component,
	 * with a buffer length.
	 */
	private final static int getNextX(Component c, int buffer) {
		return c.getX() + c.getWidth() + buffer;
	}
	
	/**
	 * Class <code>BackPanel</code> simply renders the background panel
	 * for the status panel.
	 */
	private class BackPanel extends JPanel {
		private static final long serialVersionUID = 4997805650267243080L;

		public void paint(Graphics g) {
			// Render the panel background.
			g.setColor(Color.WHITE);
			g.fillRect(0, upperBuffer, getWidth(), getHeight() - upperBuffer);
			g.setColor(Color.GRAY);
			g.drawRect(0, upperBuffer, getWidth() - 1, getHeight() - upperBuffer - 1);
			g.setColor(Color.LIGHT_GRAY);
			g.drawRect(1, upperBuffer + 1, getWidth() - 3, getHeight() - upperBuffer - 3);
		}
	}
}
