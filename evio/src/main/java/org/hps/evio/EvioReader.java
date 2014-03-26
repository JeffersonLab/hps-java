/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.evio;

import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 *
 * @author meeg
 */
public abstract class EvioReader {

	// Debug flag.
	protected boolean debug = false;
	protected String hitCollectionName = null;

	//return true if appropriate EVIO bank found
	abstract boolean makeHits(EvioEvent event, EventHeader lcsimEvent);

	public void setHitCollectionName(String hitCollectionName) {
		this.hitCollectionName = hitCollectionName;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
}
