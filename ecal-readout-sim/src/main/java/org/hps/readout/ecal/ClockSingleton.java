/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.readout.ecal;

/**
 * singleton clock class - use ClockDriver to control.
 * A better solution might be to store absolute time in the event.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: ClockSingleton.java,v 1.6 2012/07/31 00:08:40 meeg Exp $
 */
public class ClockSingleton {

	public static final ClockSingleton _instance = new ClockSingleton();
	private int clock;
	//time between events (bunch spacing)
	private double dt = 2.0;

	private ClockSingleton() {
	}

	public static void init() {
		_instance.clock = 0;
	}

	public static int getClock() {
		return _instance.clock;
	}

	public static double getTime() {
		return _instance.dt * _instance.clock;
	}

	public static double getDt() {
		return _instance.dt;
	}

	public static void setDt(double dt) {
		_instance.dt = dt;
	}

	public static void step() {
		_instance.clock++;
	}
}
