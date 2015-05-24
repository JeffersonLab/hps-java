package org.hps.users.baltzell;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.util.aida.AIDA;

public class EcalPulseFitter {

    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();
    IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(null);
    IFitFactory fitFactory = analysisFactory.createFitFactory();
    IFitter fitter=fitFactory.createFitter();
    IFunction fitFunction=new Ecal3PoleFunction();
    IDataPointSet fitData=aida.analysisFactory().createDataPointSetFactory(null).create("ADC DataPointSet", 2);

    private final int nsPerSample=4;
    public int debug = 0;
    public String fitFileName = null;
    public FileWriter fitFileWriter = null;
    public boolean fixShapeParameter = false;
    final private double threePoleWidth=2.478;
    
    
    public IFitResult fitPulse(short samples[],int threshCross,double maxADC,double noise) {
       
        if (debug>0) System.err.println("FITTING.....................................................");
     
        final int threshRange[]={6,25};
        final int fitRange[]={-10,15};
        final int pedRange[]={-10,-5};
       
        // don't bother with pulses far from trigger:
        if (threshCross < threshRange[0] || threshCross > threshRange[1]) return null;
    
        // calculate pedestal for initializing fit parameters:
        int nped=0;
        double ped=0;
        for (int ii=threshCross+pedRange[0]; ii<threshCross+pedRange[1]; ii++) 
        {
            if (ii<0) continue;
            if (ii>=samples.length) break;
            ped += samples[ii];
            nped++;
        }
        
        // don't bother trying to fit:
        if (nped==0) return null;
        ped /= nped;
       
        // choose points to fit and get starting value for pulse integral:
        fitData.clear();
        int nFitPoints=0;
        int sumADC=0;
//        int maxADC=0;
        for (int ii=threshCross+fitRange[0]; ii<threshCross+fitRange[1]; ii++) 
        {
            if (ii<0) continue;
            if (ii>=samples.length) break;
            if (debug>0) System.err.print(ii+":"+samples[ii]+" ");
//            if (samples[ii] > maxADC) maxADC=samples[ii];
            sumADC += samples[ii];
            fitData.addPoint();
            fitData.point(nFitPoints).coordinate(0).setValue(ii);
            fitData.point(nFitPoints).coordinate(1).setValue(samples[ii]);
            fitData.point(nFitPoints).coordinate(1).setErrorMinus(noise);
            fitData.point(nFitPoints).coordinate(1).setErrorPlus(noise);
            nFitPoints++;
        }
        if (debug>0) System.err.print("\n");
       
        // don't bother trying to fit:
        if (nFitPoints < 10) return null;
        if (maxADC < ped) return null;
        
        if (debug>0) System.err.println("------- "+ped+" "+threshCross+" "+maxADC);

        final double pulseIntegral = sumADC-ped*nFitPoints;
 
        // initialize parameters:
        fitFunction.setParameter("pedestal",ped);
        fitFunction.setParameter("time0",(double)threshCross-2);
        fitFunction.setParameter("integral",pulseIntegral>0 ? pulseIntegral : 2);
        fitFunction.setParameter("width",threePoleWidth);

        // constrain parameters:
        fitter.fitParameterSettings("integral").setBounds(0,999999);
        fitter.fitParameterSettings("time0").setBounds(1,30);
        fitter.fitParameterSettings("width").setBounds(0.1,5);
        if (fixShapeParameter) fitter.fitParameterSettings("width").setFixed(true);

        ((Ecal3PoleFunction)fitFunction).setDebug(debug>1);

        if (debug>0) {
            System.err.println(String.format("A= %8.2f",fitFunction.parameter("integral")));
            System.err.println(String.format("T= %8.2f",fitFunction.parameter("time0")*4));
            System.err.println(String.format("P= %8.2f",fitFunction.parameter("pedestal")));
            System.err.println(String.format("S= %8.2f",fitFunction.parameter("width")));
        } else Logger.getLogger("org.freehep.math.minuit").setLevel(Level.OFF);

        IFitResult fitResult = fitter.fit(fitData,fitFunction);
        writeFit(samples,fitResult);
       
        if (debug>0) {
            final double P = fitResult.fittedParameter("pedestal");
            final double I = fitResult.fittedParameter("integral");
            final double T = fitResult.fittedParameter("time0")*nsPerSample;
            final double S = fitResult.fittedParameter("width");
            final double Q = fitResult.quality();
            final double E[] = fitResult.errors();

            if (debug>0) {
               System.err.println(";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;");
               System.err.println(String.format("I = %8.2f %8.2f",I,sumADC-P*30));
               System.err.println(String.format("T = %8.2f %8.2f",T,threshCross));
               System.err.println(String.format("P = %8.2f %8.2f",P,ped));
               System.err.println(String.format("S = %8.2f %8.2f",S,threePoleWidth));
               System.err.println(String.format("M = %8.2f",maxADC-P));
            }
        }
        
        return fitResult;
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
    
}