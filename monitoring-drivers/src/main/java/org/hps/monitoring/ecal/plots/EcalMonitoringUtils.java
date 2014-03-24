package org.hps.monitoring.ecal.plots;

public class EcalMonitoringUtils{

    public static int getRowFromHistoID(int id){
        return (5-(id%11));
    }

    public static int getColumnFromHistoID(int id){
        return ((id/11)-23);
    }
    
    public static int getHistoIDFromRowColumn(int row,int column){
        return (-row+5)+11*(column+23);
    }
    
    public static Boolean isInHole(int row,int column){
        Boolean ret;
        ret=false;
        if ((row==1)||(row==-1)){
                if ((column<=-2)&&(column>=-8)) ret=true;
        }
        return ret;
    }

}
