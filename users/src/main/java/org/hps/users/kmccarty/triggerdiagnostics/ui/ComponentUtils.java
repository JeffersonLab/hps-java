package org.hps.users.kmccarty.triggerdiagnostics.ui;

import java.awt.Component;

import org.hps.users.kmccarty.triggerdiagnostics.util.TriggerDiagnosticUtil;

/**
 * Class <code>ComponentUtils</code> is a list of utility methods used
 * by the trigger diagnostic GUI.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class ComponentUtils {
	/** The default spacing used between a horizontal edge of one
	 * component and the horizontal edge of another. */
	static final int hinternal = 10;
	/** The default spacing used between a vertical edge of one
	 * component and the vertical edge of another. */
	static final int vinternal = 10;
	/** The default spacing used between a horizontal edge of one
	 * component and the edge of its parent component. */
	static final int hexternal = 0;
	/** The default spacing used between a vertical edge of one
	 * component and the edge of its parent component. */
	static final int vexternal = 0;
	
	/**
	 * Gets a <code>String</code> composed of a number of instances of
	 * character <code>c</code> equal to <code>number</code>.
	 * @param c - The character to repeat.
	 * @param number - The number of repetitions.
	 * @return Returns the repeated character as a <code>String</code>.
	 */
	public static final String getChars(char c, int number) {
		// Create a buffer to store the characters in.
		StringBuffer s = new StringBuffer();
		
		// Add the indicated number of instances.
		for(int i = 0; i < number; i++) {
			s.append(c);
		}
		
		// Return the string.
		return s.toString();
	}
	
	/**
	 * Gets the number of digits in the base-10 String representation
	 * of an integer primitive. Negative signs are not included in the
	 * digit count.
	 * @param value - The value of which to obtain the length.
	 * @return Returns the number of digits in the String representation
	 * of the argument value.
	 */
	public static final int getDigits(int value) {
		return TriggerDiagnosticUtil.getDigits(value);
	}
	
	/**
	 * Gets the maximum value from a list of values.
	 * @param values - The values to compare.
	 * @return Returns the largest of the argument values.
	 * @throws IllegalArgumentException Occurs if no values are given.
	 */
	public static final int max(int... values) throws IllegalArgumentException {
		// Throw an error if no arguments are provided.
		if(values == null || values.length == 0) {
			throw new IllegalArgumentException("Can not determine maximum value from a list of 0 values.");
		}
		
		// If there is only one value, return it.
		if(values.length == 1) { return values[0]; }
		
		// Otherwise, get the largest value.
		int largest = Integer.MIN_VALUE;
		for(int value : values) {
			if(value > largest) { largest = value; }
		}
		
		// Return the result.
		return largest;
	}
	
	/**
	 * Gets the x-coordinate immediately to the right of the given
	 * component.
	 * @param c - The component of which to find the edge.
	 * @return Returns the x-coordinate as an <code>int</code> value.
	 */
	static final int getNextX(Component c) {
		return getNextX(c, 0);
	}
	
	/**
	 * Gets the x-coordinate a given distance to the right edge of the
	 * argument component.
	 * @param c - The component of which to find the edge.
	 * @param spacing - The additional spacing past the edge of the
	 * component to add.
	 * @return Returns the x-coordinate as an <code>int</code> value.
	 */
	static final int getNextX(Component c, int spacing) {
		return c.getX() + c.getWidth() + spacing;
	}
	
	/**
	 * Gets the y-coordinate immediately below the given component.
	 * @param c - The component of which to find the edge.
	 * @return Returns the y-coordinate as an <code>int</code> value.
	 */
	static final int getNextY(Component c) {
		return getNextY(c, 0);
	}
	
	/**
	 * Gets the y-coordinate a given distance below the bottom edge
	 * of the argument component.
	 * @param c - The component of which to find the edge.
	 * @param spacing - The additional spacing past the edge of the
	 * component to add.
	 * @return Returns the y-coordinate as an <code>int</code> value.
	 */
	static final int getNextY(Component c, int spacing) {
		return c.getY() + c.getHeight() + spacing;
	}
}
