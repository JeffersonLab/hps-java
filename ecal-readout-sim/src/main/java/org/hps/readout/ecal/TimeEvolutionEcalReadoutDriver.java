package org.hps.readout.ecal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.ecal.HPSCalorimeterHit;
import org.lcsim.event.CalorimeterHit;

/**
 * Performs readout of ECal hits.
 * Simulates time evolution of preamp output pulse.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: TimeEvolutionEcalReadoutDriver.java,v 1.1 2013/02/25 22:39:26 meeg Exp $
 */
public class TimeEvolutionEcalReadoutDriver extends EcalReadoutDriver<HPSCalorimeterHit> {
    //buffer for deposited energy
    Map<Long, RingBuffer> eDepMap = null;
    //length of ring buffer (in readout cycles)
    int bufferLength = 20;
    //shaper time constant in ns; negative values generate square pulses of the given width
    double t0 = 18.0;
    
    public TimeEvolutionEcalReadoutDriver() {
		hitClass = HPSCalorimeterHit.class;
    }

    public void setT0(double t0) {
        this.t0 = t0;
    }

    public void setBufferLength(int bufferLength) {
        this.bufferLength = bufferLength;
        eDepMap = new HashMap<Long, RingBuffer>();
    }

    @Override
    protected void readHits(List<HPSCalorimeterHit> hits) {
        for (Long cellID : eDepMap.keySet()) {
            RingBuffer eDepBuffer = eDepMap.get(cellID);
            if (eDepBuffer.currentValue() > threshold) {
            	HPSCalorimeterHit h = new HPSCalorimeterHit(eDepBuffer.currentValue(), readoutTime(), cellID, hitType);
            	hits.add(h);
            }
            eDepBuffer.step();
        }
    }

    @Override
    protected void putHits(List<CalorimeterHit> hits) {
        //fill the readout buffers
        for (CalorimeterHit hit : hits) {
            RingBuffer eDepBuffer = eDepMap.get(hit.getCellID());
            if (eDepBuffer == null) {
                eDepBuffer = new RingBuffer(bufferLength);
                eDepMap.put(hit.getCellID(), eDepBuffer);
            }
            for (int i = 0; i < bufferLength; i++) {
                eDepBuffer.addToCell(i, hit.getRawEnergy() * pulseAmplitude((i + 1) * readoutPeriod + readoutTime() - (ClockSingleton.getTime() + hit.getTime())));
            }
        }
    }

    @Override
    protected void initReadout() {
        //initialize buffers
        eDepMap = new HashMap<Long, RingBuffer>();
    }

    private double pulseAmplitude(double time) {
        if (time < 0.0)
            return 0.0;
        if (t0 > 0.0) {
            return (time / t0) * Math.exp(1.0 - time / t0);
        } else {
            if (time < -t0)
                return 1.0;
            else
                return 0.0;
        }
    }
}
