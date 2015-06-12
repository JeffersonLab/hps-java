/**
 * 
 */
package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Vector;

import org.jdom.Element;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.ActiveSensor;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.BaseModule;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.CarbonFiber;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.HalfModuleLamination;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.Hybrid;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.Sensor;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.TestRunHalfModule;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.TestRunHalfModuleBundle;

/**
 * 
 * Common geometry information for the HPS trackers
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
abstract public class HPSTrackerGeometryDefinition extends HPSTrackerBuilder {



    //steering
    public boolean doAxial = true;
    public boolean doStereo = true;
    public boolean doColdBlock = false;
    public boolean doBottom = true;
    public boolean doTop = true;
    public int layerBitMask =  0x1;    //0x1;//


    //General
    static final double inch = 25.4; //mm
    protected static final boolean useSiStripsConvention = true;
    protected static final boolean use30mradRotation = true;
    protected static final boolean useFakeHalfModuleAxialPos = false;

    // Global position references	
    protected static final double target_pos_wrt_base_plate_x = 162.3; //from Marco's 3D model
    protected static final double target_pos_wrt_base_plate_y = 80.55; //from Tim's sketchup //68.75; //from Marco's 3D model
    protected static final double target_pos_wrt_base_plate_z = 926.59; //from Marco's 3D model
    protected static final double PS_vac_box_inner_height = 7.0*inch;
    protected static final double PS_vac_box_inner_width = 16.38*inch;
    // Inner length of the vacuum box is defined here until the horizontal fan-out. 
    // It's a little random as I'm actually not creating a volume for it here and thus
    // it just needs to capture the SVT box.
    protected static final double PS_vac_box_inner_length = 53.4*inch; 



    public HPSTrackerGeometryDefinition(boolean debug, Element node) {
        super(debug, node);
    }

    
    protected abstract HalfModuleBundle getHalfModuleBundle(BaseModule module, String halfModuleName);
    protected abstract void makeModuleBundle(int layer, String half);
    protected abstract TestRunHalfModule createTestRunHalfModuleAxial(String volName, BaseModule mother, AlignmentCorrection alignmentCorrection, int layer, String half);
    protected abstract TestRunHalfModule createTestRunHalfModuleStereo(String volName, BaseModule mother, AlignmentCorrection alignmentCorrection, int layer, String half);

    
    
    
    protected boolean doLayer(int layer) {
        int a = (1<<(layer-1)) & layerBitMask;
        return a!=0?true:false;
    }

    
    

    /**
     * Create the half-module.
     * @param side - stereo or axial
     * @param mother to the half-module
     */
    protected void makeHalfModule(String side, BaseModule mother) {
        
        String moduleName = mother.getName();
    
        if(isDebug()) System.out.printf("%s: makeHalfModule for %s %s \n", this.getClass().getSimpleName(), moduleName, side);
    
        String volName = moduleName + "_halfmodule_" + side;
    
        // top or bottom?
        String half = mother.getHalf();
        boolean isTopLayer = !mother.isBottom();
    
        // find layer
        int layer = mother.getLayer();
    
        // axial or stereo
        boolean isAxial = isAxialFromName(volName);
    
        // find layer according to old definition
        int millepedeLayer = getMillepedeLayer(volName);
        
        //if(isDebug()) System.out.printf("%s: half? %s layer %d oldlayer %d axial? %s\n", 
        //        this.getClass().getSimpleName(), isTopLayer?"top":"bottom", layer,oldLayer,isAxial?"yes":"no");
    
        // find alignment correction to this volume
        AlignmentCorrection alignmentCorrection =  getHalfModuleAlignmentCorrection(isTopLayer, millepedeLayer);
        
        
        // find the module bundle that it will be added to
        //TestRunModuleBundle bundle  = (TestRunModuleBundle)getModuleBundle(mother);
        //TestRunHalfModuleBundle halfModuleBundle;
        TestRunModuleBundle bundle  = (TestRunModuleBundle) getModuleBundle(mother);
        if(bundle==null) {
            throw new RuntimeException("Couldn't find bundle for " + volName + " from mother " + mother.getName());
        }
        
        // Build the half-module bundle and add the half-module to it
        HalfModuleBundle halfModuleBundle;
        TestRunHalfModule halfModule;
        if(isAxial) {
            halfModule = createTestRunHalfModuleAxial(volName, mother, alignmentCorrection, layer, half);
            halfModuleBundle = new TestRunHalfModuleBundle(halfModule);
            bundle.halfModuleAxial = halfModuleBundle;
        } else {
            halfModule = createTestRunHalfModuleStereo(volName, mother, alignmentCorrection, layer, half);
            halfModuleBundle = new TestRunHalfModuleBundle(halfModule);
            bundle.halfModuleStereo = halfModuleBundle;
        } 
    
    
        // create the half module components 
    
        makeHalfModuleComponentSensor(halfModule);
    
        makeHalfModuleComponentKapton(halfModule);
    
        makeHalfModuleComponentCF(halfModule);
    
        makeHalfModuleComponentHybrid(halfModule);
    
    
    
    
    }


    

    void makeHalfModuleComponentHybrid(TestRunHalfModule mother) {
    
        if(isDebug()) System.out.printf("%s: makeHalfModuleComponentHybrid for %s \n", this.getClass().getSimpleName(), mother.getName());
    
        String volName = mother.getName() + "_hybrid";
    
        // Build the half-module
    
        //  id is hard coded
        int component_number = 3;
    
        Hybrid hybrid = new Hybrid(volName,mother,component_number);
        hybrid.setMaterial("G10");
    
        TestRunHalfModuleBundle hm = (TestRunHalfModuleBundle) getHalfModuleBundle((BaseModule) mother.getMother(), mother.getName());
        hm.hybrid = hybrid;
    
        if(isDebug()) System.out.printf("%s: added hybrid to half-module with name %s \n", this.getClass().getSimpleName(), hm.halfModule.getName());
    
    
    }

    void makeHalfModuleComponentCF(TestRunHalfModule mother) {
    
        if(isDebug()) System.out.printf("%s: makeHalfModuleComponentCF for %s \n", this.getClass().getSimpleName(), mother.getName());
    
    
        String volName = mother.getName() + "_cf";
    
        // Build the half-module
    
        //  id is hard coded
        int component_number = 1;
    
        CarbonFiber cf = new CarbonFiber(volName,mother,component_number);
        cf.setMaterial("CarbonFiber");
    
        TestRunHalfModuleBundle hm = (TestRunHalfModuleBundle) getHalfModuleBundle((BaseModule) mother.getMother(), mother.getName());
        hm.carbonFiber = cf;
    
    }

    void makeHalfModuleComponentKapton(BaseModule mother) {
    
        if(isDebug()) System.out.printf("%s: makeHalfModuleComponentKapton for %s \n", this.getClass().getSimpleName(), mother.getName());
    
        String volName = mother.getName() + "_lamination";
    
        // Build the half-module
    
        //  id is hard coded
        int component_number = 2;
    
        HalfModuleLamination lamination = new HalfModuleLamination(volName,mother,component_number);
        lamination.setMaterial("Kapton");
    
    
        HalfModuleBundle hm = getHalfModuleBundle((BaseModule) mother.getMother(), mother.getName());
        hm.lamination = lamination;
    
    }

   
    
    protected void makeHalfModuleComponentSensor(BaseModule mother) {
    
        if(isDebug()) System.out.printf("%s: makeHalfModuleComponentSensor for %s \n", this.getClass().getSimpleName(), mother.getName());
    
        String volName = mother.getName() + "_sensor";
    
        // sensor id is hard coded in old geometry to be zero by counting over the components of the module
        int component_number = 0;
    
        //  
        Sensor sensor = new Sensor(volName, mother, null, component_number);
        sensor.setMaterial("Silicon");
    
        HalfModuleBundle hm = getHalfModuleBundle((BaseModule)mother.getMother(), mother.getName());
        hm.sensor = sensor;
    
    
        makeHalfModuleComponentActiveSensor(sensor);
    
    
    }
    
    private void makeHalfModuleComponentActiveSensor(Sensor mother) {
        
        if(isDebug()) System.out.printf("%s: makeHalfModuleComponentActiveSensor for %s \n", this.getClass().getSimpleName(), mother.getName());
    
        String volName = mother.getName() + "_active";
    
        ActiveSensor active_sensor = new ActiveSensor(volName, mother);
        active_sensor.setMaterial("Silicon");
    
        HalfModuleBundle hm = getHalfModuleBundle((BaseModule) mother.getMother().getMother(), mother.getMother().getName());
        hm.activeSensor = active_sensor;
    
    }
    
    
    

    /**
     * Tracking volume geometry definition. 
     */
    public static class TrackingVolume extends SurveyVolume {
        public TrackingVolume(String name, SurveyVolume mother) {
            super(name,mother, null);
            init();
        }
        protected void setPos() {
            // Dummy survey positions to setup a coordinate system
            ballPos = new BasicHep3Vector(0,0,0);
            veePos = new BasicHep3Vector(1,0,0);
            flatPos = new BasicHep3Vector(0,1,0);
        }
        protected void setCenter() {
            // at the origin
            setCenter(new BasicHep3Vector(0,0,0));
        }
        protected void setBoxDim() {
            // do nothing since we are not building a tracking volume
        }
    }

    /**
     * TODO This class is shared among geometry definitions but should really be in the test run class. Fix this. 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class TestRunModuleBundle extends BaseModuleBundle {
        public HalfModuleBundle halfModuleAxial = null;
        public HalfModuleBundle halfModuleStereo = null;
        protected SurveyVolume coldBlock = null;
        public TestRunModuleBundle(BaseModule m) {
           super(m);
        }
        public void print() {
            if(module!=null) System.out.printf("%s: %s\n", this.getClass().getSimpleName(),module.toString());
            if(halfModuleAxial!=null) halfModuleAxial.print();
            if(coldBlock!=null)System.out.printf("%s: %s\n", this.getClass().getSimpleName(),coldBlock.getName());
            if(halfModuleStereo!=null) halfModuleStereo.print();
        }
     }
    

   








}




