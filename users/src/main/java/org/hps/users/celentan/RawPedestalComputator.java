package org.hps.users.celentan;

import java.util.List;

import org.hps.conditions.deprecated.EcalConditions;
import org.hps.recon.ecal.ECalUtils;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.IOException;


public class RawPedestalComputator extends Driver{
	
	   String inputCollectionRaw = "EcalReadoutHits";
	   int row,column;
	   
	   int[] windowRaw=new int[47*11];//in case we have the raw waveform, this is the window lenght (in samples)
	   boolean[] isFirstRaw=new boolean[47*11];  
	   
	   double[] pedestal=new double[47*11];
	   double[] noise=new double[47*11];
	   double[] result;
	   
	   int pedSamples=50;
	   int nEvents=0;
	   @Override
	    public void detectorChanged(Detector detector) {
	    	System.out.println("Pedestal computator: detector changed");
	    	for (int ii=0;ii<11*47;ii++){
	    		isFirstRaw[ii]=true;
	    		pedestal[ii]=0;
	    		noise[ii]=0;
	    	}
	   }
	   

	    @Override
	    public void process(EventHeader event) {
	    	int ii=0;
	    	if (event.hasCollection(RawTrackerHit.class, inputCollectionRaw)){       	
	    		List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollectionRaw);
	          	for (RawTrackerHit hit : hits) {
	          		row=hit.getIdentifierFieldValue("iy");
	                column=hit.getIdentifierFieldValue("ix");	
	                ii = ECalUtils.getHistoIDFromRowColumn(row,column);
	                if ((row!=0)&&(column!=0)){
	                	if (!ECalUtils.isInHole(row,column)){
	                		if (isFirstRaw[ii]){ //at the very first hit we read for this channel, we need to read the window length and save it
	                			isFirstRaw[ii]=false;
	                			windowRaw[ii]=hit.getADCValues().length;  
	                		 }
	                  		result=ECalUtils.computeAmplitude(hit.getADCValues(),windowRaw[ii],pedSamples);
	                  		pedestal[ii]+=result[1];
	                  		noise[ii]+=result[2];	                  		
	                	}
	                }	
	          	}		
	    	  }	
	    	nEvents++;
	    }
	    
	    @Override
	    public void endOfData() {
	    	try{
	    	PrintWriter writerTop = new PrintWriter("default01.ped","UTF-8");
    		PrintWriter writerBottom = new PrintWriter("default02.ped","UTF-8");	
	    	
	    	for (int ii=0;ii<11*47;ii++){	    	
	    		int row,column;	
	    		row=ECalUtils.getRowFromHistoID(ii);
	    		column=ECalUtils.getColumnFromHistoID(ii);
	    		if (ECalUtils.isInHole(row,column)) continue;
   				if ((row==0)||(column==0)) continue;
	    		pedestal[ii]/=nEvents;
	    		noise[ii]/=nEvents;

	    		long daqID=EcalConditions.physicalToDaqID(EcalConditions.makePhysicalID(column,row));
	    		
	    		int crate=EcalConditions.getCrate(daqID);
	    		int slot=EcalConditions.getSlot(daqID);
	    		int channel=EcalConditions.getChannel(daqID);

	    		System.out.println(column+" "+row+" "+crate+" "+slot+" "+channel+" "+pedestal[ii]+" "+noise[ii]);
	    		
	    		
	    		
	    		if (crate==37){
	    			writerTop.print(slot+" "+channel+" "+(int)(Math.round(pedestal[ii]))+" "+(int)(Math.round(noise[ii]))+"\r\n");
	    		}
	    		else if (crate==39){
	    			writerBottom.print(slot+" "+channel+" "+(int)(Math.round(pedestal[ii]))+" "+(int)(Math.round(noise[ii]))+"\r\n");
	    		}
	    		
	    	}

    		writerTop.close();
    		writerBottom.close();
	    	}
	    	 catch(FileNotFoundException fnfe)
	        {

	            System.out.println(fnfe.getMessage());

	        }

	        catch(IOException ioe)
	        {

	            System.out.println(ioe.getMessage());

	        }
	    }
}










