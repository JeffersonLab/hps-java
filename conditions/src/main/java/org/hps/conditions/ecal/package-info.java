/**
 * <p>
 * The classes in this package represent detector conditions in the HPS ECAL subsystem.
 * <p>
 * Each {@link EcalChannel} represents one physical crystal in the detector.
 * <p>
 * The {@link EcalBadChannel} is a channel that is malfunctioning or dead and should not be used for reconstruction. It
 * is up to the reconstruction Drivers to filter out these channels.
 * <p>
 * The {@link EcalCalibration} contains the pedestal and noise values for a channel, which are the mean and the standard
 * deviation of the digitized preamplifier output.
 * <p>
 * The {@link EcalGain} is the channel gain in units of MeV/ADC counts.
 * <p>
 * The {@link EcalTimeShift} is a time shift in the electronics response.
 * <p>
 * The {@link EcalLedCalibration} is calibration information for the LED attached to an ECAL channel.
 * <p>
 * The energy of a hit is reconstructed by multiplying the gain by the pedestal-subtracted ADC integral (e.g. in Test
 * Run 2012 data).
 *
 * @author Jeremy McCormick, SLAC
 */
package org.hps.conditions.ecal;

