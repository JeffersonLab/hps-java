/**
 * The classes in this package represent detector conditions in the HPS ECAL subsystem.
 * 
 * Each {@link EcalChannel} represents one physical crystal in the detector.
 * 
 * The {@link EcalBadChannel} is a channel that is malfunctioning or dead and should not 
 * be used for reconstruction.  It is up to the reconstruction Drivers to filter out 
 * these channels.
 * 
 * The {@link EcalCalibration} contains the pedestal and noise values for a channel, 
 * which are the mean and the standard deviation of the digitized pre-amp output.
 * 
 * The {@link EcalGain} is the channel gain in units of MeV/ADC counts.
 * 
 * The energy of a hit is reconstructed by multipling the gain by the pedestal-subtracted
 * ADC integral (e.g. in Test Run 2012 data). 
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
package org.hps.conditions.ecal;