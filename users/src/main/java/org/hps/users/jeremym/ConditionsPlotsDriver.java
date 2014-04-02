package org.hps.users.jeremym;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.util.List;

import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Test plots of conditions data.
 */
public class ConditionsPlotsDriver extends Driver {
    
    boolean _done = false;
    IPlotter _plotter;
    AIDA aida = AIDA.defaultInstance();

// ecal_gains   
// +-----------+-----------+
// | min(gain) | max(gain) |
// +-----------+-----------+
// |  0.029101 |  0.579484 |
// +-----------+-----------+    
    IHistogram1D _ecalGainsPlot;

// ecal_calibrations   
// mysql> select min(pedestal), max(pedestal), min(noise), max(noise) from ecal_calibrations;
// +---------------+---------------+------------+------------+
// | min(pedestal) | max(pedestal) | min(noise) | max(noise) |
// +---------------+---------------+------------+------------+
// |     54.779303 |    216.704387 |   1.159222 | 189.222908 |
// +---------------+---------------+------------+------------+
       
    public void startOfData() {          
        System.out.println(this.getClass().getSimpleName() + ".startOfData");
        //IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        //ITree tree = analysisFactory.createTreeFactory().create();        
        IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory("ECAL Conditions Plots");
        _plotter = plotterFactory.create("HPS ECal Monitoring Plots");
        _ecalGainsPlot = aida.histogramFactory().createHistogram1D("ECAL Gains Distribution", "Channel Gain [ADC counts]", 100, 0.03, 0.58);
        _plotter.createRegion();
        _plotter.region(0).plot(_ecalGainsPlot);
        _plotter.show();
    }
       
    // FIXME: Move plotting to startOfRun() after adding that method to lcsim Driver class.
    public void process(EventHeader event) {
        System.out.println(this.getClass().getSimpleName() + ".process");
        if (!_done) {
            System.out.println("plotting...");
            Detector detector = event.getDetector();
            List<EcalCrystal> channels = detector.getDetectorElement().findDescendants(EcalCrystal.class);
            System.out.println("got " + channels.size() + " ECAL channels");
            for (EcalCrystal channel : channels) {
                System.out.println("plotting gain " + channel.getGain() + " for channel X,Y " + channel.getX() + "," + channel.getY());
                _ecalGainsPlot.fill(channel.getGain());
            }            
            
            _done = true;
        }        
    }    
}
