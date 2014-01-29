package org.lcsim.hps.recon.tracking.gbl;

import org.lcsim.event.GenericObject;
import org.lcsim.hps.recon.tracking.gbl.GBLOutput.PerigeeParams;

public class GBLTrackData implements GenericObject {
	
	/*
	 * 
	 * Interface enumerator to access the correct data
	 * 
	 */
	private static class GBLINT {
		public static final int TRACKID = 0;
		public static final int BANK_INT_SIZE = 1;
	}
	private static class GBLDOUBLE {
		public static final int PERKAPPA =0;
		public static final int PERTHETA = 1;
		public static final int PERPHI = 2;
		public static final int PERD0 = 3;
		public static final int PERZ0 = 4;
		public static final int BANK_DOUBLE_SIZE = 5;
	}
	// array holding the integer data
	private int bank_int[] = new int[GBLINT.BANK_INT_SIZE];
	// array holding the double data
	private double bank_double[] = new double[GBLDOUBLE.BANK_DOUBLE_SIZE];
	
	/**
	 * Default constructor
	 */
	public GBLTrackData() {		
	}
	
	/**
	 * @param set track id to val
	 */
	public void setTrackId(int val) {
		bank_int[GBLINT.TRACKID] = val;
	}
	
	/**
	 * @return track id for this object
	 */
	public int getTrackId() {
		return this.getIntVal(GBLINT.TRACKID);
	}
	
	/**
	 * @param perPar is the perigee parameters that is added to object
	 */
	public void setPerigeeTrackParameters(PerigeeParams perPar) {
		this.bank_double[GBLDOUBLE.PERKAPPA] = perPar.getKappa();
		this.bank_double[GBLDOUBLE.PERTHETA] = perPar.getTheta();
		this.bank_double[GBLDOUBLE.PERPHI] = perPar.getPhi();
		this.bank_double[GBLDOUBLE.PERD0] = perPar.getD0();
		this.bank_double[GBLDOUBLE.PERZ0] = perPar.getZ0();
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
