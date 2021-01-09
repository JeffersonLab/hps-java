/**
 * Platform for running HPS online reconstruction jobs in parallel on an ET system.
 *
 * A separate ET station is created for each instance of the reconstruction, which runs
 * in its own process.
 *
 * To run online reconstruction on a host machine, the {@link Server} must be started
 * with a set of properties defined in {@link StationProperties} that specifies the
 * parameters for the ET system and the LCSim reconstruction.
 */
package org.hps.online.recon;