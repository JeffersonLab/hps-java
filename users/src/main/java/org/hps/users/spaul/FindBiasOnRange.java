package org.hps.users.spaul;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.hps.record.epics.EpicsData;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

public class FindBiasOnRange extends Driver{
    
    String svtBiasName = "SVT:bias:top:0:v_sens";
    
    String outfile = "bias_on.txt";
    @Override 
    public void process(EventHeader event){
        final EpicsData edata = EpicsData.read(event);
        if (edata == null)
            return;
        System.out.println(edata.getKeys());
        if(!edata.hasKey(svtBiasName))
            return;
        double bias = edata.getValue(svtBiasName);
        foundBiasEvent(event.getEventNumber(), isGood(bias));
    }
    
    
    boolean prevIsGood = false;
    int prevNumber = -10;
    void foundBiasEvent(int number, boolean isGood){
        if(isGood && prevIsGood){
            PrintStream ps;
            try {
                ps = new PrintStream(outfile);
                ps.println(prevNumber + " " + number);
                ps.close();
                System.exit(0);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
        prevNumber = number;
        prevIsGood = isGood;
        
    }
    double minBias = 179;
    double maxBias = 181;
    boolean isGood(double bias){
        return bias > minBias && bias < maxBias;
    }
}
