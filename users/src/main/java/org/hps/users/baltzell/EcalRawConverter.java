package org.hps.users.baltzell;

import org.hps.users.baltzell.Ecal3PoleFunction;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.CalorimeterHitUtilities;
import org.hps.recon.ecal.EcalTimeWalk;
import org.hps.recon.ecal.EcalUtils;
import org.hps.recon.ecal.daqconfig.ConfigurationManager;
import org.hps.recon.ecal.daqconfig.FADCConfig;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.aida.AIDA;


/**
 * This class is used to convert {@link org.lcsim.event.RawCalorimeterHit}
 * and {@link org.lcsim.event.RawTrackerHit} to {@link org.lcsim.event.CalorimeterHit}
 * objects with energy information.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 * @author Nathan Baltzell <baltzell@jlab.org>
 * @author Holly Szumila <hvanc001@odu.edu>
 *
 * baltzell:  New in 2015:  (default behavior is still unchanged)
 *
 * Implemented conversion of Mode-1 to Mode-3.
 * 
 * Now using NSA/NSB for pedestal subtraction instead of integralWindow, to allow
 * treating all FADC Modes uniformly.  (New) NSA+NSB == (Old) integralWindow*4(ns) 
 * 
 * Pedestal subtracting clipped pulses more correctly for all Modes.
 * 
 * Implemented finding multiple peaks for Mode-1.
 * 
 * Implemented conversion of Mode-1 to Mode-7 with high-resolution timing.
 * Only some of the special cases in the firmware for when this algorithm fails due
 * to bad pulses (e.g. clipping) are already implemented.  Not yet writing Mode-7's
 * min/max to data stream. 
 */
public class EcalRawConverter {


    // wtf.
    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();
    IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(null);
    IFitFactory fitFactory = analysisFactory.createFitFactory();
    IFitter fitter=fitFactory.createFitter();
    IFunction fitFunction=new Ecal3PoleFunction();
    IDataPointSet data=aida.analysisFactory().createDataPointSetFactory(null).create("ADC DataPointSet", 2);
   
    IHistogram1D hWidth = aida.histogram1D("hShape",500,0,5);
    IHistogram1D hQuality = aida.histogram1D("hChiSquare",500,0,5);
    
    private boolean useTimeWalkCorrection = false;
    private boolean useRunningPedestal = false;
    private boolean constantGain = false;
    private double gain;
    private boolean use2014Gain = true;
    private boolean useDAQConfig = false;
    private FADCConfig config = null;

    public int debug = 0;
    public boolean useFit = false;
    public String fitFileName = null;
    public FileWriter fitFileWriter = null;
    public boolean fixShapeParameter = false;

    /*
     * The time for one FADC sample (units = ns).
     */
    private static final int nsPerSample = 4;
    
    /*
     * The leading-edge threshold, relative to pedestal, for pulse-finding and
     * time determination.  Units = ADC.  Used to convert mode-1 readout into
     * mode-3/7 used by clustering.
     * 
     * The default value of 12 is what we used for most of the 2014 run.
     */
    private double leadingEdgeThreshold = 12;
    
    /*
     * Integration range after (NSA) and before (NSB) threshold crossing.  Units=ns,
     * same as the DAQ configuration files.  These must be multiples of 4 ns.  Used
     * for pulse integration in Mode-1, and pedestal subtraction in all modes.
     * 
     * The default values of 20/100 are what we had during the entire 2014 run.
     */
    private int NSB = 20;
    private int NSA = 100;
  
    /*
     * The number of samples in the FADC readout window.  Needed in order to
     * properly pedestal-correct clipped pulses for Mode-3/7.  Ignored for
     * mode-1 input, since it already knows its number of samples.
     * 
     * A non-positive number disables pulse-clipped pedestals and reverts to
     * the old behavior which assumed integration range was constant.
     * 
     */
    private int windowSamples = -1;
    
    /*
     * The maximum number of peaks to be searched for.
     */
    private int nPeak = 3;
   
    /*
     * Convert Mode-1 into Mode-7, else Mode-3.
     */
    private boolean mode7 = false;


    private EcalConditions ecalConditions = null;

    public EcalRawConverter() {
        if (debug<1) Logger.getLogger("org.freehep.math.minuit").setLevel(Level.OFF);

    	// Track changes in the DAQ configuration.
    	ConfigurationManager.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// If the DAQ configuration should be used, load the
				// relevant settings into the driver.
				if(useDAQConfig) {
					// Get the FADC configuration.
					config = ConfigurationManager.getInstance().getFADCConfig();
					
					// Load the settings.
					NSB = config.getNSB();
					NSA = config.getNSA();
					windowSamples = config.getWindowWidth() / 4;
					
					// Get the number of peaks.
					if(config.getMode() == 1) {
						nPeak = Integer.MAX_VALUE;
					} else {
						nPeak = config.getMaxPulses();
					}
					
					// Print the FADC configuration.
					System.out.println();
					System.out.println();
					System.out.printf("NSA            :: %d ns%n", NSA);
					System.out.printf("NSB            :: %d ns%n", NSB);
					System.out.printf("Window Samples :: %d clock-cycles%n", windowSamples);
					System.out.printf("Max Peaks      :: %d peaks%n", nPeak);
					System.out.println("======================================================================");
					System.out.println("=== FADC Pulse-Processing Settings ===================================");
					System.out.println("======================================================================");
					config.printConfig();
				}
			}
    	});
    }
  
    public void setLeadingEdgeThreshold(double thresh) {
        leadingEdgeThreshold=thresh;
    }
    
    public void setNSA(int nsa) {
        if (NSA%nsPerSample !=0 || NSA<0) {
            throw new RuntimeException("NSA must be multiples of 4ns and non-negative.");
        }
        NSA=nsa;
    }
    
    public void setNSB(int nsb) {
        if (NSB%nsPerSample !=0 || NSB<0) {
            throw new RuntimeException("NSB must be multiples of 4ns and non-negative.");
        }
        NSB=nsb;
    }
    
    public void setWindowSamples(int windowSamples) {
        this.windowSamples=windowSamples;
    }
    
    public void setNPeak(int nPeak) {
        if (nPeak<1 || nPeak>3) {
            throw new RuntimeException("Npeak must be 1, 2, or 3.");
        }
        this.nPeak=nPeak;
    }
    
    public void setMode7(boolean mode7)
    {
        this.mode7=mode7;
    }

    public void setGain(double gain) {
        constantGain = true;
        this.gain = gain;
    }

    public void setUse2014Gain(boolean use2014Gain) {
        this.use2014Gain = use2014Gain;
    }

    public void setUseRunningPedestal(boolean useRunningPedestal) {
        this.useRunningPedestal=useRunningPedestal;
    }

    public void setUseTimeWalkCorrection(boolean useTimeWalkCorrection) {
        this.useTimeWalkCorrection=useTimeWalkCorrection;
    }
    
    public void setUseDAQConfig(boolean state) {
    	useDAQConfig = state;
    }

    /*
     * This should probably be deprecated.  It just integrates the entire window.
     */
    public int sumADC(RawTrackerHit hit) {
        EcalChannelConstants channelData = findChannel(hit.getCellID());
        double pedestal;
        if(useDAQConfig) {
    		//EcalChannel channel = ecalConditions.getChannelCollection().findGeometric(hit.getCellID());
    		pedestal = config.getPedestal(hit.getCellID());
        } else {
        	pedestal = channelData.getCalibration().getPedestal();
        }
        
        int sum = 0;
        short samples[] = hit.getADCValues();
        for (int isample = 0; isample < samples.length; ++isample) {
            sum += (samples[isample] - pedestal);
        }
        return sum;
    }

    /*
     * This should probably be deprecated.  HitDtoA(EventHeader,RawTrackerHit)
     * has the same functionality if NSA+NSB > windowSamples, with the exception
     * that that one also finds pulse time instead of this one's always reporting zero.
     */
    public CalorimeterHit HitDtoA(RawTrackerHit hit) {
        double time = hit.getTime();
        long id = hit.getCellID();
        double rawEnergy = adcToEnergy(sumADC(hit), id);
        return CalorimeterHitUtilities.create(rawEnergy, time, id);
    }

    /*
     * Get pedestal for a single ADC sample.
     * Choose whether to use static pedestal from database or running pedestal from mode-7.
     */
    public double getSingleSamplePedestal(EventHeader event,long cellID) {
    	if(useDAQConfig) {
    		//EcalChannel channel = ecalConditions.getChannelCollection().findGeometric(cellID);
    		return config.getPedestal(cellID);
    	}
        if (useRunningPedestal && event!=null) {
            if (event.hasItem("EcalRunningPedestals")) {
                Map<EcalChannel, Double> runningPedMap = (Map<EcalChannel, Double>) event.get("EcalRunningPedestals");
                EcalChannel chan = ecalConditions.getChannelCollection().findGeometric(cellID);
                if (!runningPedMap.containsKey(chan)){
                    System.err.println("************** Missing Pedestal");
                } else {
                    return runningPedMap.get(chan);
                }
            } else {
                System.err.println("*****************************************************************");
                System.err.println("**  You Requested a Running Pedestal, but it is NOT available. **");
                System.err.println("**     Reverting to the database. Only printing this ONCE.     **");
                System.err.println("*****************************************************************");
                useRunningPedestal = false;
            }
        }
        return findChannel(cellID).getCalibration().getPedestal();
    }

    /*
     * Get pedestal for entire pulse integral.  Account for clipping if
     * windowSamples is greater than zero.
     */
    public double getPulsePedestal(EventHeader event,long cellID,int windowSamples,int thresholdCrossing) {
        int firstSample,lastSample;
        if ( windowSamples>0 && (NSA+NSB)/nsPerSample >= windowSamples ) {
            // special case where firmware always integrates entire window
            firstSample = 0;
            lastSample = windowSamples-1;
        } else {
            firstSample = thresholdCrossing - NSB/nsPerSample;
            lastSample  = thresholdCrossing + NSA/nsPerSample-1;
            if (windowSamples > 0) {
                // properly pedestal subtract pulses clipped by edge(s) of readout window:
                if (firstSample < 0) firstSample=0;
                if (lastSample >= windowSamples) lastSample=windowSamples-1;
            }
        }
        return (lastSample-firstSample+1)*getSingleSamplePedestal(event,cellID); 
    }

    public IFitResult fitPulse(short samples[],int thresholdCrossing,double maxADC,double noise) {
       
        if (thresholdCrossing < 9 || thresholdCrossing > 15) return null;
    
        if (debug>0) System.err.println("FITTING.....................................................");
       
        data.clear();
        int nped=0;
        double ped=0;
        for (int ii=thresholdCrossing-9; ii<thresholdCrossing-5; ii++) {
            if (ii<0 || ii>=samples.length) break;
            ped += samples[ii];
            nped++;
        }
        ped /= nped;
        if (nped==0) return null;
        int npts=0;
        int sumADC=0;
        for (int ii=thresholdCrossing-10; ii<thresholdCrossing+15; ii++) {
            if (ii<0) continue;
            if (ii>=samples.length) break;
            if (debug>0) System.err.print(ii+":"+samples[ii]+" ");
            data.addPoint();
            data.point(npts).coordinate(0).setValue(ii);
            data.point(npts).coordinate(1).setValue(samples[ii]);
            data.point(npts).coordinate(1).setErrorMinus(noise);
            data.point(npts).coordinate(1).setErrorPlus(noise);
            sumADC += samples[ii];
            npts++;
        }
        if (debug>0) System.err.print("\n");
        if (npts>=10) {
            if (debug>0) System.err.println("------- "+ped+" "+thresholdCrossing+" "+maxADC);
            if (maxADC-ped < 0) return null;
            final double pulseIntegral = sumADC-ped*npts;
            fitFunction.setParameter("pedestal",ped);
            fitFunction.setParameter("time0",(double)thresholdCrossing-2);
            fitFunction.setParameter("integral",pulseIntegral>0?pulseIntegral:10);
            fitFunction.setParameter("width",2.478);
            ((Ecal3PoleFunction)fitFunction).setDebug(debug>1);

            if (debug>0) {
                System.err.println(String.format("A= %8.2f",fitFunction.parameter("integral")));
                System.err.println(String.format("T= %8.2f",fitFunction.parameter("time0")*4));
                System.err.println(String.format("P= %8.2f",fitFunction.parameter("pedestal")));
                System.err.println(String.format("S= %8.2f",fitFunction.parameter("width")));
            }

            fitter.fitParameterSettings("integral").setBounds(0,999999);
            fitter.fitParameterSettings("time0").setBounds(5,40);
            fitter.fitParameterSettings("width").setBounds(0.1,5);
            if (fixShapeParameter) fitter.fitParameterSettings("width").setFixed(true);
           
            if (debug<1) Logger.getLogger("org.freehep.math.minuit").setLevel(Level.OFF);
            IFitResult fitResult = fitter.fit(data,fitFunction);
            writeFit(samples,fitResult);
            return fitResult;
        }
        return null;
    }
    
    public void writeFit(short samples[],IFitResult fit) {
        if (fitFileName == null) return;
        if (fitFileWriter == null) {
            try { fitFileWriter=new FileWriter(fitFileName); }
            catch (IOException ee) { throw new RuntimeException("Error opening file "+fitFileName,ee); }
        }
        
        try {
            for (final short ss : samples) fitFileWriter.write(String.format("%6d ",ss));
            fitFileWriter.write(String.format("%8.3f",fit.fittedParameter("integral")));
            fitFileWriter.write(String.format("%8.3f",fit.fittedParameter("time0")));
            fitFileWriter.write(String.format("%8.3f",fit.fittedParameter("pedestal")));
            fitFileWriter.write(String.format("%8.3f",fit.fittedParameter("width")));
            fitFileWriter.write(String.format("%8.3f",fit.quality()));
            fitFileWriter.write("\n");
        } catch (IOException ee) {
            throw new RuntimeException("Error writing file "+fitFileName,ee);
        } 
    }
    
        
    
    /*
     * Emulate the FADC250 firmware in conversion of Mode-1 waveform to a Mode-3/7 pulse,
     * given a time for threshold crossing.
     */
    public double[] convertWaveformToPulse(RawTrackerHit hit,int thresholdCrossing,boolean mode7) {
        
        short samples[] = hit.getADCValues();
        //System.out.println("NewEvent");
        // choose integration range:
        int firstSample,lastSample;
        if ((NSA+NSB)/nsPerSample >= samples.length) {
            // firmware treats this case specially:
            firstSample = 0;
            lastSample = samples.length-1;
        } else {
            firstSample = thresholdCrossing - NSB/nsPerSample;
            lastSample  = thresholdCrossing + NSA/nsPerSample - 1;
        }
        
        // mode-7's minimum/pedestal (average of first 4 samples):
        double minADC=0;
        for (int jj=0; jj<4; jj++) minADC += samples[jj];
        // does the firmware's conversion of min to int occur before or after time calculation?  undocumented.
        //minADC=(int)(minADC/4); 
        minADC = (minADC/4);
        
        //System.out.println("Avg pedestal:\t"+minADC);
        
        // mode-7's max pulse height:
        double maxADC=0;
        int sampleMaxADC=0;
        
        // mode-3/7's pulse integral:
        double sumADC = 0;
        
        for (int jj=firstSample; jj<=lastSample; jj++) {
        
            if (jj<0) continue;
            if (jj>=samples.length) break;
            
            // integrate pulse:
            sumADC += samples[jj];
        }

        // find pulse maximum:
        //if (jj>firstSample && jj<samples.length-5) { // The "5" here is a firmware constant.
        for (int jj=thresholdCrossing; jj<samples.length-5; jj++) { // The "5" here is a firmware constant.
            if (samples[jj+1]<samples[jj]){ 
                sampleMaxADC=jj;
                maxADC=samples[jj];
                break;                
            }
        }


        // pulse time with 4ns resolution:
        double pulseTime=thresholdCrossing*nsPerSample;
        
        // calculate Mode-7 high-resolution time:
        if (mode7) {
        	if (thresholdCrossing < 4) {
                // special case where firmware sets max to zero and time to 4ns time.
                maxADC=0;
            }
            else if (maxADC>0) {
            // linear interpolation between threshold crossing and
            // pulse maximum to find time at pulse half-height:
          
            final double halfMax = (maxADC+minADC)/2;
            int t0 = -1;
            for (int ii=thresholdCrossing-1; ii<lastSample; ii++)
            {
              if (ii>=samples.length-1) break;
              if (samples[ii]<=halfMax && samples[ii+1]>halfMax)
              {
                t0 = ii;
                break;
              }
            }
            if (t0 > 0)
            {
            	final int t1 = t0 + 1;
              final int a0 = samples[t0];
              final int a1 = samples[t1];
              final double slope = (a1 - a0); // units = ADC/sample
              final double yint = a1 - slope * t1;  // units = ADC 
              pulseTime = ((halfMax - a0)/(a1-a0) + t0)* nsPerSample;
              
            }

           }
        }
       
        if (useFit)
        {
          final double noise=findChannel(hit.getCellID()).getCalibration().getNoise();
          IFitResult fitResult=fitPulse(samples,thresholdCrossing,maxADC,noise);
          
          if (fitResult != null) {

            final double P=fitResult.fittedParameter("pedestal");
            final double A=fitResult.fittedParameter("integral");
            final double T=fitResult.fittedParameter("time0");
            final double S=fitResult.fittedParameter("width");
            final double Q=fitResult.quality();

            // finite integral from T to T+NSAMP:
            //final int NSAMP=30;
            //final double I = (A/2/S) * S*S*S* ( 2 - Math.exp(-NSAMP/S) * (Math.pow((S+NSAMP)/S,2) + 1));

            // infinite integral:
            //final double I = A*S*S;
            
            // after normalizing the function:
            final double I = A;
            
            if (debug>0) {
               System.err.println(";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;");
               System.err.println(String.format("A= %8.2f %8.2f",I,sumADC-P*30));
               System.err.println(String.format("T= %8.2f %8.2f",T*4,pulseTime));
               System.err.println(String.format("P= %8.2f %8.2f",P,minADC));
               System.err.println(String.format("S= %8.2f",S));
               System.err.println(String.format("M= %8.2f %8.2f",A,(maxADC-P)));
            }

            hWidth.fill(S);
            hQuality.fill(Q);
            pulseTime = T*4;
            sumADC = I;
          }
        }
        return new double []{pulseTime,sumADC,minADC,maxADC};
    }
  
    /*
     * This HitDtoA is for emulating the conversion of Mode-1 readout (RawTrackerHit)
     * into what EcalRawConverter would have created from a Mode-3 or Mode-7 readout.
     * Clustering classes will read the resulting CalorimeterHits same as if they were
     * directly readout from the FADCs in Mode-3/7.
     * 
     * For Mode-3, hit time is just the time of threshold crossing, with an optional
     * time-walk correction.  For Mode-7, it is a "high-resolution" one calculated
     * by linear interpolation between threshold crossing and pulse maximum.
     *
     * TODO: Generate GenericObject (and corresponding LCRelation) to store min and max
     * to fully emulate mode-7.  This is less important for now.
     *
     */
    public ArrayList <CalorimeterHit> HitDtoA(EventHeader event, RawTrackerHit hit) {
        final long cellID = hit.getCellID();
        final short samples[] = hit.getADCValues();
        if(samples.length == 0) return null;
        
        // threshold is pedestal plus threshold configuration parameter:
        final int absoluteThreshold;
        if(useDAQConfig) {
        	//EcalChannel channel = ecalConditions.getChannelCollection().findGeometric(hit.getCellID());
        	//int leadingEdgeThreshold = ConfigurationManager.getInstance().getFADCConfig().getThreshold(channel.getChannelId());
        	int leadingEdgeThreshold = config.getThreshold(cellID);
        	absoluteThreshold = (int) (getSingleSamplePedestal(event, cellID) + leadingEdgeThreshold);
        } else {
        	absoluteThreshold = (int) (getSingleSamplePedestal(event, cellID) + leadingEdgeThreshold);
        }
        
        ArrayList <Integer> thresholdCrossings = new ArrayList<Integer>();
        
        // special case, first sample is above threshold:
        if (samples[0] > absoluteThreshold) {
            thresholdCrossings.add(0);
        } 
        
        // search for threshold crossings:
        for(int ii = 1; ii < samples.length; ++ii) {
            if ( samples[ii]   >  absoluteThreshold && 
                 samples[ii-1] <= absoluteThreshold) {
                
                // found one:
                thresholdCrossings.add(ii);

                // search for next threshold crossing begins at end of this pulse:
                if(useDAQConfig && ConfigurationManager.getInstance().getFADCConfig().getMode() == 1) {
                    // special case, emulating SSP:
                	ii += 8;
                } else {
                    // "normal" case, emulating FADC250:
                	ii += NSA/nsPerSample - 1;
                }

                // firmware limit on # of peaks:
                if (thresholdCrossings.size() >= nPeak) break;
            }
        }
        
        // make hits
        ArrayList <CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();
        for(int thresholdCrossing : thresholdCrossings) {
            // do pulse integral:
            final double[] data = convertWaveformToPulse(hit, thresholdCrossing, mode7);
            double time = data[0];
            double sum = data[1];
            final double min = data[2]; // TODO: stick min and max in a GenericObject with an 
            final double max = data[3]; // LCRelation to finish mode-7 emulation
           
            if (!useFit) {
              // do pedestal subtraction:
              sum -= getPulsePedestal(event, cellID, samples.length, thresholdCrossing);
            }
          
            // do gain scaling:
            double energy = adcToEnergy(sum, cellID);
            
            // do time-walk correction, mode-3 only:
            if (!mode7 && useTimeWalkCorrection) {
                time = EcalTimeWalk.correctTimeWalk(time,energy);
            }
            
            newHits.add(CalorimeterHitUtilities.create(energy,time,cellID));
        }
        
        return newHits;
    }

    /*
     * This HitDtoA is for Mode-3 data.  A time-walk correction can be applied.
     */
    public CalorimeterHit HitDtoA(EventHeader event,RawCalorimeterHit hit, double timeOffset) {
        if (hit.getTimeStamp() % 64 != 0) {
            System.out.println("unexpected timestamp " + hit.getTimeStamp());
        }
        double time = hit.getTimeStamp() / 16.0;
        long id = hit.getCellID();
        double pedestal = getPulsePedestal(event,id,windowSamples,(int)time/nsPerSample);
        double adcSum = hit.getAmplitude() - pedestal;
        double rawEnergy = adcToEnergy(adcSum, id);
        if (useTimeWalkCorrection) {
           time = EcalTimeWalk.correctTimeWalk(time,rawEnergy);
        }
        return CalorimeterHitUtilities.create(rawEnergy, time + timeOffset, id);
    }

    /*
     * This HitDtoA is exclusively for Mode-7 data, hence the GenericObject parameter.
     */
    public CalorimeterHit HitDtoA(EventHeader event,RawCalorimeterHit hit, GenericObject mode7Data, double timeOffset) {
        double time = hit.getTimeStamp() / 16.0; //timestamps use the full 62.5 ps resolution
        long id = hit.getCellID();
        double pedestal = getPulsePedestal(event,id,windowSamples,(int)time/nsPerSample);
        double adcSum = hit.getAmplitude() - pedestal;
        double rawEnergy = adcToEnergy(adcSum, id);        
        return CalorimeterHitUtilities.create(rawEnergy, time + timeOffset, id);
    }

    /*
     * This converts a corrected pulse integral (pedestal-subtracted and gain-scaled)
     * back into raw pulse integral with units ADC.
     */
    public RawCalorimeterHit HitAtoD(CalorimeterHit hit) {
        int time = (int) (Math.round(hit.getTime() / 4.0) * 64.0);
        long id = hit.getCellID();
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(id);
        int amplitude;
        double pedestal = getPulsePedestal(null, id, windowSamples, (int) hit.getTime() / nsPerSample);
        if (constantGain) {
            amplitude = (int) Math.round((hit.getRawEnergy() / EcalUtils.MeV) / gain + pedestal);
        } else {
            amplitude = (int) Math.round((hit.getRawEnergy() / EcalUtils.MeV) / channelData.getGain().getGain() + pedestal);
        }
        RawCalorimeterHit h = new BaseRawCalorimeterHit(id, amplitude, time);
        return h;
    }

    /*
     * return energy (units of GeV) corresponding to the ADC sum and crystal ID
     */
    private double adcToEnergy(double adcSum, long cellID) {

        // Get the channel data.
        EcalChannelConstants channelData = findChannel(cellID);
        
        if(useDAQConfig) {
        	//float gain = ConfigurationManager.getInstance().getFADCConfig().getGain(ecalConditions.getChannelCollection().findGeometric(cellID));
        	return config.getGain(cellID) * adcSum * EcalUtils.MeV;
        }  else if(use2014Gain) {
            if (constantGain) {
                return adcSum * EcalUtils.gainFactor * EcalUtils.ecalReadoutPeriod;
            } else {
                return channelData.getGain().getGain() * adcSum * EcalUtils.gainFactor * EcalUtils.ecalReadoutPeriod; // should not be used for the moment (2014/02)
            }
        } else {
            if(constantGain) {
                return gain * adcSum * EcalUtils.MeV;
            } else {
                return channelData.getGain().getGain() * adcSum * EcalUtils.MeV; //gain is defined as MeV/integrated ADC
            }
        }
    }

    /**
     * Must be set when an object EcalRawConverter is created.
     *
     * @param detector (long)
     */
    public void setDetector(Detector detector) {
        // ECAL combined conditions object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }

    /**
     * Convert physical ID to gain value.
     *
     * @param cellID (long)
     * @return channel constants (EcalChannelConstants)
     */
    public EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }
    
}
