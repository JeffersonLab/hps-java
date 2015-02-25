package org.hps.users.baltzell;

import hep.aida.ICloud2D;
import hep.aida.IPlotter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class EcalPedestalPlots extends Driver {

    AIDA aida = AIDA.defaultInstance();
    IPlotter plotter;

    final int NEV = 100;
    final int NX = 46;

    long nev = 0;
    int refreshRate = 1;
    long thisTime, prevTime;

    List<ICloud2D> LT = new ArrayList<ICloud2D>(NX / 2);
    List<ICloud2D> LB = new ArrayList<ICloud2D>(NX / 2);
    List<ICloud2D> RT = new ArrayList<ICloud2D>(NX / 2);
    List<ICloud2D> RB = new ArrayList<ICloud2D>(NX / 2);

    private EcalConditions ecalConditions = null;

    public EcalPedestalPlots() {
    }

    protected void detectorChanged(Detector detector) {
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        for (int ii = 0; ii < NX / 2; ii++) {
            LT.add(aida.cloud2D("EcalPedestalPlots:LT",1000));
            LB.add(aida.cloud2D("EcalPedestalPlots:LB",1000));
            RT.add(aida.cloud2D("EcalPedestalPlots:RT",1000));
            RB.add(aida.cloud2D("EcalPedestalPlots:RB",1000));
        }
        thisTime = 0;
        prevTime = 0;

        plotter.createRegions(2,2);
        plotter.show();
    }

    public void process(EventHeader event) {

        if (!event.hasCollection(RawCalorimeterHit.class,"EcalReadoutHits")) {
            return;
        }
        if (!event.hasCollection(Map.class,"EcalRunningPedestals")) {
            return;
        }
        if (nev++ % NEV != 0) {
            return;
        }

        List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class,"EcalReadoutHits");
        Map<RawCalorimeterHit, Double> peds = (Map<RawCalorimeterHit, Double>) event
                .get("EcalRunningPedestals");

        for (RawCalorimeterHit hit : hits) {
            if (!peds.containsKey(hit)) {
                continue;
            }

            final double time = event.getTimeStamp()/1e9;
            final double ped = peds.get(hit);
            final EcalChannel chan = getChannel(hit);
            final int xx = chan.getX();
            final int yy = chan.getY();

            if (xx < 0) {
                if (yy < 0) {
                    LB.get(xx + NX / 2).fill(time,ped);
                } else {
                    LT.get(xx + NX / 2).fill(time,ped);
                }
            } else {
                if (yy < 0) {
                    RB.get(xx + NX / 2 - 1).fill(time,ped);
                } else {
                    RT.get(xx + NX / 2 - 1).fill(time,ped);
                }
            }
        }

        thisTime = System.currentTimeMillis() / 1000;
        if ((thisTime - prevTime) > refreshRate) {
            prevTime = thisTime;
            redraw();
        }
    }

    void redraw() {
        plotter.region(0).clear();
        plotter.region(1).clear();
        plotter.region(2).clear();
        plotter.region(3).clear();
        for (int ii = 0; ii < NX / 2; ii++) {
            plotter.region(0).plot(LB.get(ii));
            plotter.region(1).plot(LT.get(ii));
            plotter.region(2).plot(RB.get(ii));
            plotter.region(3).plot(RT.get(ii));
        }
        plotter.region(0).refresh();
        plotter.region(1).refresh();
        plotter.region(2).refresh();
        plotter.region(3).refresh();
    }

    public EcalChannel getChannel(RawCalorimeterHit hit) {
        return ecalConditions.getChannelCollection().findGeometric(hit.getCellID());
    }
}
