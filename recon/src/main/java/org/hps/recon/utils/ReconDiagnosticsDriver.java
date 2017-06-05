package org.hps.recon.utils;

import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class ReconDiagnosticsDriver extends Driver {

    private long _startTime;

    private AIDA aida = AIDA.defaultInstance();

    private int mb = 1024 * 1024;

    private int _timeInterval = 1;
    private int _memInterval = 1;

    private int _nEvents;

    //Getting the runtime reference from system
    Runtime runtime;

    @Override
    protected void detectorChanged(Detector detector) {
        _startTime = System.nanoTime();
        runtime = Runtime.getRuntime();
    }

    @Override
    protected void process(EventHeader event) {
        _nEvents++;
        //TODO average quantities if intervals != 1
        if ((_nEvents % _memInterval) == 0) {
            System.out.println("##### Heap utilization statistics [MB] #####");

            //Print used memory
            System.out.println("Used Memory:"
                    + (runtime.totalMemory() - runtime.freeMemory()) / mb);

            //Print free memory
            System.out.println("Free Memory:"
                    + runtime.freeMemory() / mb);

            //Print total available memory
            System.out.println("Total Memory:" + runtime.totalMemory() / mb);

            //Print Maximum available memory
            System.out.println("Max Memory:" + runtime.maxMemory() / mb);
        }

        if ((_nEvents % _timeInterval) == 0) {
            long deltaTime = System.nanoTime() - _startTime;
            List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
            double svtsize = rawHits.size();
            aida.cloud2D("elapsed time vs SVT size").fill(svtsize, deltaTime);
            _startTime = System.nanoTime();
            double usedMem = (runtime.totalMemory() - runtime.freeMemory()) / mb;
            double freeMem = runtime.freeMemory() / mb;
            double totalMem = (runtime.totalMemory() - runtime.freeMemory()) / mb;
            double maxMem = runtime.maxMemory() / mb;
            System.out.println(event.getRunNumber() + " " + event.getEventNumber() + " " + svtsize + " " + deltaTime + " " + usedMem + " " + freeMem + " " + totalMem + " " + maxMem);
        }
    }

    public void setTimeInterval(int i) {
        _timeInterval = i;
    }

    public void setMemoryInterval(int i) {
        _memInterval = i;
    }
}
