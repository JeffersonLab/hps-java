/**
 * Platform for running HPS online reconstruction jobs in parallel on an ET system.
 * 
 * A separate ET station is created for each instance of the reconstruction, which runs
 * in its own process.
 * 
 * To run online reconstruction on a host machine, the {@link Server} must be started
 * with a set of properties defined in {@link StationConfiguration} that specifies the
 * parameters for the ET system and the LCSim reconstruction.
 * 
 * The {@link Client} class is used to send commands to the {@link Server}.
 * 
 * The basic client documentation can be printed using:
 * 
 * <pre>
 * <code>
 * java -jar hps-distribution-bin.jar org.hps.online.recon.Client --help
 * </code>
 * </pre>
 * 
 * The documentation for each command can be printed using:
 * <pre>
 * <code>
 * java -jar hps-distribution-bin.jar org.hps.online.recon.Client [command] --help
 * </code>
 * </pre>
 * 
 * Similarly, the server options can be shown using:
 * <pre>
 * <code>
 * java -jar hps-distribution-bin.jar org.hps.online.recon.Server --help
 * </code>
 * </pre>
 * 
 * An optional task runs server-side to automatically add output ROOT plots 
 * periodically and write them to an output target file.
 * 
 * @author jeremym
 * @version 1.0
 */
package org.hps.online.recon;