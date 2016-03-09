package org.hps.users.spaul;

//import org.hps.recon.ecal.triggerbank.AbstractIntData;
//import org.hps.recon.ecal.triggerbank.TIData;
//import org.lcsim.event.GenericObject;

import java.io.File;
import java.io.IOException;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;
import org.hps.conditions.ConditionsDriver;
import org.hps.recon.ecal.cluster.ClusterUtilities;
//import org.hps.recon.ecal.triggerbank.AbstractIntData;
//import org.hps.recon.ecal.triggerbank.TIData;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;

public class PulserFilter extends Driver{

    public void process(EventHeader event) {

        // only keep pulser triggers:
        if (!event.hasCollection(GenericObject.class,"TriggerBank"))
            throw new Driver.NextEventException();
        boolean isPulser=false;
        for (GenericObject gob : event.get(GenericObject.class,"TriggerBank"))
        {
            if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) continue;
            TIData tid = new TIData(gob);
            if (tid.isPulserTrigger())
            {
                isPulser=true;
                break;
            }
        }

        // don't drop any events with EPICS data or scalers data
        // (could also do this via event tag=31)
        final EpicsData edata = EpicsData.read(event);
        if (edata != null) return;
        ScalerData sdata = ScalerData.read(event);
        if(sdata != null) return;

        if (!isPulser) throw new Driver.NextEventException();


    }

    public static void main(String arg[]) throws IOException{
        ConditionsDriver hack = new ConditionsDriver();
        hack.setDetectorName("HPS-EngRun2015-Nominal-v1");
        hack.setFreeze(true);
        hack.setRunNumber(Integer.parseInt(arg[2]));
        hack.initialize();
        PulserFilter pf = new PulserFilter();
        LCIOWriter writer = new LCIOWriter(arg[1]);
        File file = new File(arg[0]);
        LCIOReader reader = new LCIOReader(file);
        System.out.println(file.getPath());

        try{
            while(true){
                try{
                    EventHeader eh = reader.read();
                    if(eh.getEventNumber() %100 == 0)
                        System.out.println(eh.getEventNumber());
                    pf.process(eh);
                    writer.write(eh);
                }catch(Driver.NextEventException e){

                }
            }
        }catch(IOException e){
            e.printStackTrace();
            reader.close();
        }


        writer.close();
    }
}