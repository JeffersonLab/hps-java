package org.lcsim.hps.recon.tracking;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IGeometryInfo;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.IPhysicalVolumeContainer;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.material.IMaterial;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.ISolid;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerModule;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.event.HPSTransformations;



/**
 * Material manager using the detector geometry. 

 * Uses a private class to set up detector volumes. 
 * This can probably make use of the DetectorGeometry classes from lcsim instead for the model.
 * Something to consider in the future.
 *  
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 * @version $Id: MaterialSupervisor.java,v 1.4 2013/11/07 03:54:58 phansson Exp $ $Date: 2013/11/07 03:54:58 $ $Author: phansson $ 
 */
public class MaterialSupervisor extends MaterialManager {
    
    private List<ScatteringDetectorVolume> _detectorVolumes = new ArrayList<ScatteringDetectorVolume>();
    

    public MaterialSupervisor() {
        super();
        this._includeMS = true;
    }
    public MaterialSupervisor(boolean includeMS) {
        super(includeMS);
    }
    
    @Override
    public void setDebug(boolean debug) {
        super.setDebug(debug);
    }
    
    public List<ScatteringDetectorVolume> getMaterialVolumes() {
        return _detectorVolumes;
    }

    @Override
    public void buildModel(Detector det) {
        //super.buildModel(det);
        //if(DEBUG) 
        System.out.printf("%s: ###########################################################\n",this.getClass().getSimpleName());
        System.out.printf("%s: Build detector model\n",this.getClass().getSimpleName());
        List<SiSensor> sensors = det.getSubdetector("Tracker").getDetectorElement().findDescendants(SiSensor.class);
        //List<SiTrackerModule> modules = det.getDetectorElement().findDescendants(SiTrackerModule.class);
        System.out.printf("%s: %d sensors\n",this.getClass().getSimpleName(),sensors.size());
        System.out.printf("%s: %5s %32s %22s %15s %10s %10s\n",this.getClass().getSimpleName(),"ID","Pos (mm)","size(mm)","t(mm)","t(%R.L)","type");
        for (SiSensor module: sensors) {
            
            SiStripPlane plane = new SiStripPlane(module);
                    
            System.out.printf("%s: %5d %32s %15.2fx%.2f %10.2f %10.3f %10s\n",this.getClass().getSimpleName(),plane.getId(),plane.origin().toString(),
                                                                                plane.getUnmeasuredDimension(),plane.getMeasuredDimension(),
                                                                                plane.getThickness(),plane.getThicknessInRL()*100,
                                                                                SvtUtils.getInstance().isAxial(module) ? "axial" : "stereo");            
            _detectorVolumes.add(plane);     
        }
        System.out.printf("%s: ###########################################################\n",this.getClass().getSimpleName());
    }
    
    
    
    public interface ScatteringDetectorVolume {
        public String getName();
        public double getMaterialTraversed(Hep3Vector dir);
        public double getMaterialTraversedInRL(Hep3Vector dir);
        public void print();
        public IDetectorElement getDetectorElement();
    }
    
    //public abstract class DetectorPlane extends SiSensor {
    public interface DetectorPlane  extends ScatteringDetectorVolume{
        public double getThickness();
        public double getThicknessInRL();
        public double getLength();
        public double getWidth();
        public Hep3Vector origin();
        public Hep3Vector normal();
        public int getId();

    }
    
    private abstract class SiPlane implements DetectorPlane {
        abstract void addMaterial();
    }
    
    public class SiStripPlane extends SiPlane {
        private Hep3Vector _org = null; // origin
        private Hep3Vector _w = null;   // normal to plane
        private Hep3Vector _u = null;
        private Hep3Vector _v = null;
        private Materials _materials = new Materials();
        private SiSensor _sensor;
        private double _length;
        private double _width;

        public SiStripPlane(SiSensor module) {
            _sensor = module;
            setOrigin();
            setNormal();
            setMeasuredCoordinate();
            setUnmeasuredCoordinate();
            setDimensions();
            addMaterial();
            
        }
        
        @Override
        public IDetectorElement getDetectorElement() {
            return getSensor();
        }
        
        private SiTrackerModule getModule() {
            return (SiTrackerModule)getGeometry().getDetectorElement().getParent();
        }
        
     
        private  IGeometryInfo getGeometry() {
            return getSensor().getGeometry();
        }
        
        SiSensor getSensor() {
            return _sensor;
        }
        
        Polygon3D getPsidePlane() {
            return getSensor().getBiasSurface(ChargeCarrier.HOLE);
        }

        Polygon3D getNsidePlane() {
            return getSensor().getBiasSurface(ChargeCarrier.ELECTRON);
        }
    
       
        
        @Override
        public double getMaterialTraversed(Hep3Vector dir) {
            //the distance inside the plane (note I don't care about sign of unit vector only projection distance)
            double cth = Math.abs(VecOp.dot(dir, _w));
            double t = _materials.getThickness();
            return t/cth;
        }

        @Override
        public double getMaterialTraversedInRL(Hep3Vector dir) {
            //the distance inside the plane (note I don't care about sign of unit vector only projection distance)
            double cth = Math.abs(VecOp.dot(dir, _w));
            double t = _materials.getThicknessInRL();
            return t/cth;
        }

        @Override
        protected void addMaterial() {
            
            IPhysicalVolume parent = getModule().getGeometry().getPhysicalVolume();
            IPhysicalVolumeContainer daughters = parent.getLogicalVolume().getDaughters();
            //System.out.printf("%s found %d daugters to SiTrackerModule\n",this.getClass().getSimpleName(),daughters.size());
            for(IPhysicalVolume daughter : daughters) {
                ILogicalVolume logicalVolume = daughter.getLogicalVolume();
                IMaterial material = logicalVolume.getMaterial();
                String name = material.getName();
                double X0 = 10.0* material.getRadiationLength()/material.getDensity();
                Box solid = (Box) logicalVolume.getSolid();
                //System.out.printf("%s x %f y %f z %f box\n",this.getClass().getSimpleName(),solid.getXHalfLength(),solid.getYHalfLength(),solid.getZHalfLength());
                double halfThickness = solid.getZHalfLength();
                addMaterial(name, material.getDensity()/1000.0, X0,2.0*halfThickness);
            }
        }
        
        public void addMaterial(String type, double density, double radLen, double t) {
            _materials.add(type, density, radLen, t);
        }
        
        
        @Override
        public double getThickness() {
            return _materials.getThickness();
        }

        @Override
        public double getThicknessInRL() {
            return _materials.getThicknessInRL();
        }

        private void setDimensions() {
            // The dimensions are taken from the full module
            IPhysicalVolume physVol_parent = getModule().getGeometry().getPhysicalVolume();
            ILogicalVolume logVol_parent = physVol_parent.getLogicalVolume();
            ISolid solid_parent = logVol_parent.getSolid();
            Box box_parent;
            if(Box.class.isInstance(solid_parent)) {
                box_parent = (Box) solid_parent;
            } else {
                throw new RuntimeException("Couldn't cast the module volume to a box!?");
            }
            _length = box_parent.getXHalfLength()*2.0;
            _width = box_parent.getYHalfLength()*2.0;
            
        }
        
        @Override
        public Hep3Vector origin() {
            
            return _org;
        }

        public void setOrigin(Hep3Vector org) {
             
            this._org = org;
        }

        private void setOrigin() {
            // Use origin of p-side surface
            Hep3Vector origin = VecOp.mult(HPSTransformations.getMatrix(),_sensor.getGeometry().getPosition());
            //transform to p-side
            Polygon3D psidePlane = this.getPsidePlane();
            Translation3D transformToPside = new Translation3D(VecOp.mult(-1*psidePlane.getDistance(), psidePlane.getNormal()));
            this._org = transformToPside.translated(origin);
        }
        
        @Override
        public Hep3Vector normal() {
            if(_w==null) {
                _w = this.getPsidePlane().getNormal();
                System.out.printf("setting normal from pside normal %s\n",_w.toString());
                _w = VecOp.mult(VecOp.mult(HPSTransformations.getMatrix(),getSensor().getReadoutElectrodes(ChargeCarrier.HOLE).getLocalToGlobal().getRotation().getRotationMatrix()), _w);
                System.out.printf("normal after local to global to tracking rotation %s\n",_w.toString());
            }
            return this._w;
        }
        
        private void setNormal() {
            _w = this.getPsidePlane().getNormal();
            _w = VecOp.mult(VecOp.mult(HPSTransformations.getMatrix(),getSensor().getReadoutElectrodes(ChargeCarrier.HOLE).getLocalToGlobal().getRotation().getRotationMatrix()), _w);
        }
        
        public void setNormal(Hep3Vector w) {
            this._w = w;
        }

        @Override
        public void print() {
            System.out.printf("DetectorPlane:  org %s normal vector %s %.2fx%.2fmm  thickness %f R.L. (%fmm)\n",
                                origin().toString(),normal().toString(),getLength(),getWidth(),
                                getThicknessInRL(),getThickness());
        }
        
        @Override
        public int getId() {
            return _sensor.getSensorID();
        }

        @Override
        public String getName() {
            return _sensor.getName();
        }

        @Override
        public double getLength() {
            return _length;
        }

        @Override
        public double getWidth() {
            return _width;
        }
        
        double getMeasuredDimension() {
            return getLength();
        }

        double getUnmeasuredDimension() {
            return getWidth();
        }
        
        Hep3Vector getUnmeasuredCoordinate() {
            return _v;
        }
        Hep3Vector getMeasuredCoordinate() {
            return _u;
        }
        
        private  void setMeasuredCoordinate()
        {
            // p-side unit vector
            ITransform3D electrodes_to_global = getSensor().getReadoutElectrodes(ChargeCarrier.HOLE).getLocalToGlobal();
            Hep3Vector measuredCoordinate = getSensor().getReadoutElectrodes(ChargeCarrier.HOLE).getMeasuredCoordinate(0);
            measuredCoordinate = VecOp.mult(VecOp.mult(HPSTransformations.getMatrix(),electrodes_to_global.getRotation().getRotationMatrix()), measuredCoordinate);
            _u = measuredCoordinate;
        }
        
        private  void setUnmeasuredCoordinate()
        {
            // p-side unit vector
            ITransform3D electrodes_to_global = getSensor().getReadoutElectrodes(ChargeCarrier.HOLE).getLocalToGlobal();
            Hep3Vector unmeasuredCoordinate = getSensor().getReadoutElectrodes(ChargeCarrier.HOLE).getUnmeasuredCoordinate(0);
            unmeasuredCoordinate = VecOp.mult(VecOp.mult(HPSTransformations.getMatrix(),electrodes_to_global.getRotation().getRotationMatrix()), unmeasuredCoordinate);
            _v = unmeasuredCoordinate;
        }

        
        

    }
    
    
    
    
    
    
    private static class Material {
        private String _name;
        private double _X0;
        private double _density;
        private double _thickness;
        public Material(String _name, double _X0, double _density, double _thickness) {
            this._name = _name;
            this._X0 = _X0;
            this._density = _density;
            this._thickness = _thickness;
        }
        private void add(double t) {
            _thickness+=t;
        }

        public double getThickness() {
            return _thickness;
        }

        public double getDensity() {
            return _density;
        }

        public double getX0() {
            return _X0;
        }
        
    }
    
    private static class Materials {
        private List<Material> _materials = new ArrayList<Material>();
        private double _tot_X0 = -1;
        public Materials() {
        }
        public int numberOfMaterials() {
            return _materials.size();
        }
        public void add(String mat,double density, double radLen, double t) {
            boolean found = false;
            for(Material m : _materials) {
                if(m._name==mat) {
                    m.add(t);
                    found=true;
                    break;
                } 
            }
            if (!found) {
                //System.out.printf("%s: Adding %.2fmm of %s \n",this.getClass().getSimpleName(),t,mat);
                _materials.add(new Material(mat,radLen,density,t));
            }
            
        }
        public double getThicknessInRL() {
            if(_materials.isEmpty()) return 0;
            if(_tot_X0<0) {
                double sum = 0.;
                for(Material m : _materials) {
                    sum += m.getDensity()*m.getThickness();
                }
                //System.out.printf("sum = %f\n",sum);
                double tot_X0 = 0.;
                for(Material m : _materials) {
                    double w_j = m._density*m.getThickness()/(numberOfMaterials()*sum);
                    tot_X0 += w_j/(m.getThickness()/m.getX0());
                }
                //System.out.printf("tot_X0 = %f\n",tot_X0);
                _tot_X0 = 1.0/tot_X0;
            }
            return _tot_X0;
        }

        private double getThickness() {
            double t_tot = 0.;
            for(Material m : _materials) {
                t_tot += m.getThickness();
            }
            return t_tot;
        }
        
    }
    


    

    
    
    
}
