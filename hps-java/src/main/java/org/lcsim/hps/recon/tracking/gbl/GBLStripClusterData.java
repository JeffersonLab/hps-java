package org.lcsim.hps.recon.tracking.gbl;

import org.lcsim.event.GenericObject;
import org.lcsim.hps.recon.tracking.gbl.GBLOutput.PerigeeParams;

public class GBLStripClusterData implements GenericObject {
	
	/*
	 * 
	 * Interface enumerator to access the correct data
	 * 
	 */
	private static class GBLINT {
		public static final int ID = 0;
		public static final int BANK_INT_SIZE = 1;
	}
	private static class GBLDOUBLE {
		public static final int PATH3D = 0;
		public static final int PATH = 1;
		public static final int BANK_DOUBLE_SIZE = 2;
	}
	// array holding the integer data
	private int bank_int[] = new int[GBLINT.BANK_INT_SIZE];
	// array holding the double data
	private double bank_double[] = new double[GBLDOUBLE.BANK_DOUBLE_SIZE];
	
	/**
	 * Default constructor
	 */
	public GBLStripClusterData(int id) {
		setId(id);
	}
	
	/**
	 * @param set track id to val
	 */
	public void setId(int val) {
		bank_int[GBLINT.ID] = val;
	}
	
	/**
	 * @return track id for this object
	 */
	public int getId() {
		return this.getIntVal(GBLINT.ID);
	}
	
	/**
	 * Set path length to this strip cluster
	 * @param val
	 */
	public void setPath(double val) {
		bank_double[GBLDOUBLE.PATH] = val;
	}
	
	/**
	 * Get path length to this strip cluster
	 */
	public double getPath() {
		return getDoubleVal(GBLDOUBLE.PATH);
	}

	/**
	 * Set path length to this strip cluster
	 * @param val
	 */
	public void setPath3D(double val) {
		bank_double[GBLDOUBLE.PATH3D] = val;
	}
	
	/**
	 * Get path length to this strip cluster
	 */
	public double getPath3D() {
		return getDoubleVal(GBLDOUBLE.PATH3D);
	}

	
	
	
	

	/*
	 * The functions below are all overide from 
	 * @see org.lcsim.event.GenericObject#getNInt()
	 */
	
	public int getNInt() {
		return GBLINT.BANK_INT_SIZE;
	}

	public int getNFloat() {
		return 0;
	}

	public int getNDouble() {
		return GBLDOUBLE.BANK_DOUBLE_SIZE;
	}

	public int getIntVal(int index) {
		return bank_int[index];
	}

	public float getFloatVal(int index) {
		return 0;
	}

	public double getDoubleVal(int index) {
		return bank_double[index];
	}

	public boolean isFixedSize() {
		return false;
	}


}
