package org.lcsim.hps.recon.tracking.trf;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IDetectorElementContainer;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.IPhysicalVolumeContainer;
import org.lcsim.detector.solids.Trd;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.SiTrackerSpectrometer;
import org.lcsim.recon.tracking.trfbase.Interactor;
import org.lcsim.recon.tracking.trfbase.MultiInteractor;
import org.lcsim.recon.tracking.trflayer.ClusterFindManager;
import org.lcsim.recon.tracking.trflayer.InteractingLayer;
import org.lcsim.recon.tracking.trflayer.Layer;
import org.lcsim.recon.tracking.trfzp.ClusFindZPlane2;
import org.lcsim.recon.tracking.trfzp.LayerZPlane;
import org.lcsim.recon.tracking.trfzp.ThinZPlaneMs;
import org.lcsim.util.Driver;

/**
 * Driver to create trf Detector for HPS Test Proposal geometry.
 * 
 * TODO: Run trf in process method.
 *  
 * @author jeremym
 * @author ngraf
 * @version $Id: TestPropTrfDriver.java,v 1.1 2011/08/01 23:56:34 jeremy Exp $
 */
public class TestPropTrfDriver extends Driver
{
    // Default name of tracking detector.
    private String trackerName = "Tracker";
    
    private double maxChisqDiff = 20.0;
    
    public static class HPSDetector extends org.lcsim.recon.tracking.trflayer.Detector
    {
        public int addLayer(String name, Layer lyr)
        {
            return super.addLayer(name, lyr);
        }
    }    
       
    public TestPropTrfDriver()
    {}
    
    public void setTrackerName(String trackerName)
    {
        this.trackerName = trackerName;
    }
    
    public void detectorChanged(Detector detector)
    {
        //System.out.println(this.getName() + ".detectorChanged - " + detector.getName());
        
        // Make trf Detector object.
        HPSDetector trfDetector = new HPSDetector();
                        
        // FIXME: For now, this is the only detector allowed.
        if (!detector.getName().equals("HPS-Test-JLAB-v2pt1"))
        {
            throw new RuntimeException("This Driver is not designed to work with your detector " + detector.getName() + "!");
        }
        
        SiTrackerSpectrometer tracker = (SiTrackerSpectrometer)detector.getSubdetector(trackerName);
        
        if (tracker == null)
        {
            throw new RuntimeException("There was a problem getting the subdetector " + trackerName + " from the detector.");
        }
        
        IDetectorElement top = tracker.getDetectorElement();
        IDetectorElementContainer layers = top.getChildren().get(0).getChildren();
        for (IDetectorElement layer : layers)
        {
            //System.out.println("layer --> " + layer.getName());
            IDetectorElementContainer modules = layer.getChildren();
            for (IDetectorElement module : modules)
            {
                double xOverX0 = calculateModuleX0(module);
                //System.out.println("module --> " + module.getName() + ", xOverX0 = " + xOverX0);
                Trd moduleTrd = (Trd)module.getGeometry().getLogicalVolume().getSolid();
                //System.out.println("y = " + moduleTrd.getXHalfLength1() + ", x = " + moduleTrd.getZHalfLength());
                IDetectorElementContainer components = module.getChildren();
                for (IDetectorElement component : components)
                {
                    if (component instanceof SiSensor)
                    {
                        //System.out.println("sensor --> " 
                        //        + component.getName() 
                        //        + " @ z = " 
                        //        + component.getGeometry().getPosition().z() + " mm");
                        
                        // Get the sensor Z position in global coord.
                        double zSense = component.getGeometry().getPosition().z();
                        
                        // Create a trf layer.                        
                        ClusterFindManager find = new ClusFindZPlane2(zSense, maxChisqDiff);
                        org.lcsim.recon.tracking.trflayer.Layer lyr = new LayerZPlane(zSense, find);
                        
                        List multiInt = new ArrayList();
                        
                        // Make multiple scattering surface.                        
                        Interactor mcs = new ThinZPlaneMs(xOverX0);
                        multiInt.add(mcs);                  
                        
                        // Make multiple scattering interactor.
                        Interactor multi = new MultiInteractor(multiInt);
                        
                        // Build an interacting layer
                        org.lcsim.recon.tracking.trflayer.Layer intlayer = new InteractingLayer(lyr, multi);
                        
                        // Add interacting layer to the detector
                        trfDetector.addLayer(component.getName(), intlayer);
                    }
                }
            }
            //System.out.println("-------------");
        }
        
        // Debug print detector.
        System.out.println(trfDetector);
    }
    
    private double calculateModuleX0(IDetectorElement module)
    {
        double xOverX0 = 0;
        IPhysicalVolume modulePhysVol = module.getGeometry().getPhysicalVolume();
        IPhysicalVolumeContainer components = modulePhysVol.getLogicalVolume().getDaughters();
        for (IPhysicalVolume component : components)
        {
            //System.out.println(component.getName() + " - " + component.getLogicalVolume().getMaterial().getName());
            double x0 = component.getLogicalVolume().getMaterial().getRadiationLengthWithDensity();
            Trd trd = (Trd)component.getLogicalVolume().getSolid();
            double thickness = trd.getYHalfLength1();
            xOverX0 += thickness/(x0*10.);
            //System.out.println("x0 = " + x0 + ", thickness = " + thickness + ", xOverX0 = "+(thickness/(x0*10.)));
        }
        //System.out.println("total_xOverX0 = "+xOverX0);
        return xOverX0;
    }
    
    public void process(EventHeader event)
    {
        // TODO: Run trf here.
    }
}