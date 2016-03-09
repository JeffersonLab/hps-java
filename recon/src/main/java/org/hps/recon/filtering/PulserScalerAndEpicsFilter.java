package org.hps.recon.filtering;

//import org.hps.recon.ecal.triggerbank.AbstractIntData;
//import org.hps.recon.ecal.triggerbank.TIData;
//import org.lcsim.event.GenericObject;

import java.io.File;
import java.io.IOException;

import org.hps.conditions.ConditionsDriver;
import org.hps.record.epics.EpicsData;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;
//import org.hps.recon.ecal.triggerbank.AbstractIntData;
//import org.hps.recon.ecal.triggerbank.TIData;

public class PulserScalerAndEpicsFilter extends Driver{

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
        
        if( event.hasCollection(GenericObject.class, "ScalerData"))
            return;

        if (!isPulser) throw new Driver.NextEventException();


    }
    /**
     * standalone way to run this:
     * 
     * @param arg [0] inputFile [1] outputFile [2] run number [3] detectorName (optional, default = "HPS-EngRun2015-Nominal-v1") 
     * @throws IOException
     */
    public static void main(String arg[]) throws IOException{
        ConditionsDriver hack = new ConditionsDriver();
        
        String detectorName = "HPS-EngRun2015-Nominal-v1";
        if(arg.length >3)
        hack.setDetectorName(arg[3]);
        hack.setFreeze(true);
        hack.setRunNumber(Integer.parseInt(arg[2]));
        hack.initialize();
        PulserScalerAndEpicsFilter pf = new PulserScalerAndEpicsFilter();
        LCIOWriter writer = new LCIOWriter(arg[1]);
        File file = new File(arg[0]);
        LCIOReader reader = new LCIOReader(file);
        System.out.println(file.getPath());
        int nEventsKept = 0;
        int nEvents = 0;
        try{
            while(true){
                try{
                    
                    EventHeader eh = reader.read();
                    if(eh.getEventNumber() %1000 == 0){
                        //Driver.this.
                        System.out.println("PulserFitter:");
                        System.out.println("    " + nEventsKept + " events kept");
                        System.out.println("    " + nEvents + "events read");
                    }
                    nEvents ++;
                    pf.process(eh); //might throw NextEventException
                    
                    nEventsKept++;
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