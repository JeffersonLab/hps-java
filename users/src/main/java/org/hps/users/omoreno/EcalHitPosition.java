package org.hps.users.omoreno;

//--- lcsim ---//
import org.lcsim.event.GenericObject;

/**
 * 
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: EcalHitPosition.java,v 1.1 2013/06/02 18:24:06 omoreno Exp $
 */
public class EcalHitPosition implements GenericObject {

	double[] position; 
	
	public EcalHitPosition(double[] position){
		this.position = position; 
	}
	
	/**
	 * 
	 */
	@Override
	public double getDoubleVal(int index) {
		return position[index];
	}

	@Override
	public float getFloatVal(int arg0) {
		return 0;
	}

	@Override
	public int getIntVal(int arg0) {
		return 0;
	}

	@Override
	public int getNDouble() {
		return position.length;
	}

	@Override
	public int getNFloat() {
		return 0;
	}

	@Override
	public int getNInt() {
		return 0;
	}

	@Override
	public boolean isFixedSize() {
		return true;
	}
}
