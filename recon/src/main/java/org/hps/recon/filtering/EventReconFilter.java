package org.hps.recon.filtering;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.lcsim.util.Driver;

import org.lcsim.geometry.Detector;

/**
 * @author mgraham
 */
public class EventReconFilter extends Driver {

    private static final Logger LOGGER = Logger.getLogger(EventReconFilter.class.getPackage().getName());

    private int nprocessed = 0;
    private int npassed = 0;

    public EventReconFilter() {
    }

    public void endOfData() {
        System.out.println(this.getClass().getSimpleName() + " Summary: ");
        System.out.println("events processed = " + nprocessed);
        System.out.println("events passed    = " + npassed);
        System.out.println("       rejection = " + ((double) npassed) / nprocessed);

    }

    public void incrementEventProcessed() {
        nprocessed++;
    }

    public void incrementEventPassed() {
        npassed++;
    }

    public void skipEvent() {
        throw new Driver.NextEventException();
    }

    protected Double beamEnergy;

    public void setBeamEnergy(double e) {
        this.beamEnergy = e;
    }

    public double getBeamEnergy() {
        return this.beamEnergy;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        BeamEnergyCollection beamEnergyCollection = this.getConditionsManager()
                .getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();
        if (beamEnergy == null && beamEnergyCollection != null && beamEnergyCollection.size() != 0)
            beamEnergy = beamEnergyCollection.get(0).getBeamEnergy();
        else {
            LOGGER.log(Level.WARNING, "warning:  beam energy not found.  Using a 6.6 GeV as the default energy");
            beamEnergy = 6.6;
        }

    }
}
