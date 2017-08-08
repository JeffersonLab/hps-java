package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.DataConversionException;
import org.jdom.Element;
import org.lcsim.detector.Transform3D;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.BaseModule;

public abstract class HPSTrackerBuilder {

    private static final Logger LOGGER = Logger.getLogger(HPSTrackerBuilder.class.getPackage().getName());

    private boolean debug = false;
    public List<BaseModuleBundle> modules = new ArrayList<BaseModuleBundle>();
    protected List<SurveyVolume> surveyVolumes = new ArrayList<SurveyVolume>();
    protected Element node;
    protected List<MilleParameter> milleparameters = new ArrayList<MilleParameter>();
    
    /**
     * Default constructor to create a geometry.
     * 
     * @param debug output flag
     * @param node to have access to compact xml
     */
    public HPSTrackerBuilder(boolean debug, Element node) {
        this.debug = debug;
        this.node = node;
        
        // Applying SVT alignment constants from the conditions database must be explicitly enabled with a system property.
        if (System.getProperties().getProperty("org.hps.conditions.enableSvtAlignmentConstants") != null) {
            LOGGER.config("Alignment conditions will be read from database.");
            try {
                this.milleparameters = SvtAlignmentConstantsReader.readMilleParameters();
            } catch (Exception e) {
                throw new RuntimeException("Error reading alignment parameters from conditions database.", e);
            }
        } else {
            // Read alignment constants from compact XML file (default behavior).
            LOGGER.config("Mille parameters will be read from compact.xml file.");
            initAlignmentParameters();
        }
        
        if(debug) {
            for (MilleParameter p : milleparameters)
                System.out.printf("%d,%f \n", p.getId(),p.getValue());
        }
    }

    /**
     * Extract alignment constants from xml description
     */
    private void initAlignmentParameters() {

        if (debug)
            System.out.printf("%s: initAlignmentParameters from %s\n", this.getClass().getSimpleName(),
                    node.getAttributeValue("name"));

        // Get alignment parameters.
        int detId = -1;
        try {
            detId = node.getAttribute("id").getIntValue();
        } catch (DataConversionException e) {
            e.printStackTrace();
        }
        String detName = node.getAttributeValue("name");
        String detType = node.getAttributeValue("type");

        Element constantsElement = node.getChild("millepede_constants");
        if (constantsElement == null) {
            throw new RuntimeException("no millepede constants found in compact file");
        }

        if (debug)
            System.out.printf("%s: %d alignment corrections for detId=%d detName=%s detType=%s\n", this.getClass()
                    .getSimpleName(), constantsElement.getChildren("millepede_constant").size(), detId, detName,
                    detType);

        int id = -99999;
        double value = -99999;
        for (Iterator iConstant = constantsElement.getChildren("millepede_constant").iterator(); iConstant.hasNext();) {
            Element constantElement = (Element) iConstant.next();
            try {
                id = constantElement.getAttribute("name").getIntValue();
                value = constantElement.getAttribute("value").getDoubleValue();
            } catch (DataConversionException e) {
                e.printStackTrace();
            }
            // System.out.printf("%s: constant %d value %f\n",this.getClass().getSimpleName(),id,value);

            MilleParameter p = new MilleParameter(id, value, 0.0);
            // System.out.printf("%s: Milleparameter: %s\n", this.getClass().getSimpleName(),p.toString());
            milleparameters.add(p);
        }

        if (debug) {
            System.out.printf("%s: Initialized %d alignment parameters:\n", this.getClass().getSimpleName(),
                    milleparameters.size());
            for (MilleParameter p : milleparameters)
                System.out.printf("%s: %s \n", this.getClass().getSimpleName(), p.toString());
        }

    }

    /**
     * Extract @AlignmentCorrection for a half-module
     * 
     * @param isTopLayer - top or bottom layer
     * @param layer - to identify which sensor it is.
     * @return the alignment correction for this half-module
     */
    protected AlignmentCorrection getHalfModuleAlignmentCorrection(boolean isTopLayer, int layer) {
        int rFound = 0;
        int tFound = 0;
        double r[] = {0, 0, 0};
        double t[] = {0, 0, 0};
        for (MilleParameter p_loop : milleparameters) {
            boolean paramIsTop = p_loop.getHalf() == 1 ? true : false;
            int paramLayer = p_loop.getSensor();
            if (paramIsTop == isTopLayer && paramLayer == layer) {
                if (p_loop.getType() == 1) {
                    t[p_loop.getDim() - 1] = p_loop.getValue();
                    tFound++;
                } else if (p_loop.getType() == 2) {
                    r[p_loop.getDim() - 1] = p_loop.getValue();
                    rFound++;
                }
            }
        }
        if (tFound != 3 || rFound != 3) {
            throw new RuntimeException("Problem finding translation alignment parameters (found t " + tFound + " r "
                    + rFound + ") for " + (isTopLayer ? "top" : "bottom") + " layer " + layer);
        }
        AlignmentCorrection c = new AlignmentCorrection();
        c.setTranslation(new BasicHep3Vector(t));
        c.setRotation(r[0], r[1], r[2]);
        return c;
    }

    /**
     * Extract @AlignmentCorrection for the support
     * 
     * @param isTopLayer - top or bottom layer
     * @return the alignment correction
     */
    protected AlignmentCorrection getL13UChannelAlignmentCorrection(boolean isTopLayer) {
        double r[] = {0, 0, 0};
        double t[] = {0, 0, 0};
        for (MilleParameter p_loop : milleparameters) {
            boolean paramIsTop = p_loop.getHalf() == 1 ? true : false;
            if (paramIsTop == isTopLayer && p_loop.getType() == 3) {
                // xcheck
                if (p_loop.getSensor() != 0)
                    throw new RuntimeException("sensor name is not zero for support plate param! " + p_loop.getSensor());
                // get the correction
                r[p_loop.getDim() - 1] = p_loop.getValue();
            }
        }
        AlignmentCorrection c = new AlignmentCorrection();
        c.setTranslation(new BasicHep3Vector(t));
        c.setRotation(r[0], r[1], r[2]);
        return c;
    }

    /**
     * Build the local geometry
     */
    public abstract void build();

    /**
     * Bundle volumes into a module.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    public abstract static class BaseModuleBundle {
        public SurveyVolume module = null;

        public BaseModuleBundle(BaseModule m) {
            module = m;
        }

        public int getLayer() {
            if (module == null)
                throw new RuntimeException("Need to add module to bundle first!");
            return HPSTrackerBuilder.getLayerFromVolumeName(module.getName());
        }

        public String getHalf() {
            if (module == null)
                throw new RuntimeException("Need to add module to bundle first!");
            return HPSTrackerBuilder.getHalfFromName(module.getName());
        }

        public SurveyVolume getMother() {
            if (module == null)
                throw new RuntimeException("Need to add module to bundle first!");
            return module.getMother();
        }

        public abstract void print();
    }

    /**
     * Bundle volumes into a half-module. TODO If the geometry definition has access to daughter information I could
     * avoid this?
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    public static abstract class HalfModuleBundle {
        public SurveyVolume halfModule = null;
        public SurveyVolume lamination = null;
        public SurveyVolume sensor = null;
        public SurveyVolume activeSensor = null;

        public HalfModuleBundle(SurveyVolume hm) {
            halfModule = hm;
        }

        public HalfModuleBundle() {
        }

        public void print() {
            if (halfModule != null)
                System.out.printf("%s: %s\n", this.getClass().getSimpleName(), halfModule.toString());
            if (activeSensor != null)
                System.out.printf("%s: %s\n", this.getClass().getSimpleName(), activeSensor.toString());
        }
    }

    
    public static boolean isTopFromName(String name) {
        return getHalfFromName(name).equals("top") ? true : false;
    }
    
    public static String getHalfFromName(String name) {
        boolean matchBottom = Pattern.matches(".*bottom.*", name);
        boolean matchTop = Pattern.matches(".*top.*", name);
        
        if(matchBottom && matchTop)
            throw new RuntimeException("found both halfs from name  " + name);

        if(matchBottom)
            return "bottom";
        if(matchTop)
            return "top";

        // check for other signatures
        Pattern pattern = Pattern.compile(".*_L\\d\\d?(t|b).*");
        Matcher matcher = pattern.matcher(name);
        if(matcher.matches()) {
            if(matcher.group(1).equals("t"))
                return "top";
            else if(matcher.group(1).equals("b"))
                return "bottom";
            else
                throw new RuntimeException("I should never get here from name " + name);
        } 
        throw new RuntimeException("Couldn't find half from " + name);
    }

    public static int getLayerFromVolumeName(String name) {
        Matcher matcher = Pattern.compile(".*module_L(\\d+).*").matcher(name);
        if(matcher.matches())
            return Integer.parseInt( matcher.group(1));
        else
            throw new RuntimeException("cannot find layer from " + name);
    }

    public static boolean isBase(String name) {
        if (name.endsWith("base")) {
            return true;
        }
        return false;
    }

    public static boolean isHalfModule(String name) {
        if (name.endsWith("halfmodule_axial") || name.endsWith("halfmodule_axial_slot")
                || name.endsWith("halfmodule_axial_hole") || name.endsWith("halfmodule_stereo")
                || name.endsWith("halfmodule_stereo_slot") || name.endsWith("halfmodule_stereo_hole")) {
            return true;
        }
        return false;
    }
    
    public static boolean isModule(String name) {
        return Pattern.matches("module_L\\d+[bt]$", name);
    }
    
    public static int getUChannelSupportLayer(String name) {
        if(isUChannelSupport(name)) {
            Pattern patter = Pattern.compile("^support_[a-z]*_L([1-6])[1-6]$");
            Matcher matcher = patter.matcher(name);
            if(matcher.matches() ) {
                int layer = Integer.parseInt(matcher.group(1));
                return layer;
            } else {
                throw new RuntimeException("this is not a valid u-channel name: " + name);
            }
        } else {
            throw new RuntimeException("this is not a valid u-channel name: " + name);
        }
    }
    
    
    public static boolean isUChannelSupport(String name) {
        Pattern patter = Pattern.compile("^support_[a-z]*_L(4|1)(6|3)$");
        Matcher matcher = patter.matcher(name);
        return matcher.matches();
    }

    public static boolean isSupportRingKinMount(String name) {
        Pattern patter = Pattern.compile("^c_support_kin_L13(b|t)$");
        Matcher matcher = patter.matcher(name);
        return matcher.matches();
    }    
    
    
    public static boolean isSensor(String name) {
        if (name.endsWith("sensor")) {
            return true;
        }
        return false;
    }

    public static boolean isActiveSensor(String name) {
        if (name.endsWith("sensor_active")) {
            return true;
        }
        return false;
    }

    protected boolean isDebug() {
        return debug;
    }

    protected void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Find geometry object by type.
     * 
     * @param c - class type to be found
     * @return the found type.
     */
    public <T> T getSurveyVolume(Class<T> c) {
        // if(isDebug()) System.out.printf("%s: get Item %s\n", this.getClass().getSimpleName(),c.getName());

        for (SurveyVolume item : surveyVolumes) {
            // if(isDebug()) System.out.printf("%s: item %s\n", getClass().getSimpleName(),item.getClass().getName());
            if (c.isInstance(item)) {
                return (T) item;
            }
        }
        throw new RuntimeException("Coulnd't find instance of " + c.getSimpleName() + " among the "
                + surveyVolumes.size() + " tracker items!");
    }

    protected List<BaseModuleBundle> getModules() {
        return modules;
    }

    /**
     * Find module among the existing bundles.
     * 
     * @param layer - layer id
     * @param half - top or bottom half
     * @return module or null if not found
     */
    protected BaseModuleBundle getModuleBundle(int layer, String half) {
        for (BaseModuleBundle m : modules) {
            if (m.getLayer() == layer && m.getHalf().equals(half)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Find module among the existing bundles.
     * 
     * @param module - to find
     * @return bundle
     */
    protected BaseModuleBundle getModuleBundle(BaseModule module) {
        return getModuleBundle(module.getLayer(), module.getHalf());
    }

    /**
     * Add module to list.
     * 
     * @param bundle - module to add.
     */
    protected void addModuleBundle(BaseModuleBundle bundle) {
        BaseModuleBundle b = getModuleBundle(bundle.getLayer(), bundle.getHalf());
        if (b == null) {
            modules.add(bundle);
        } else {
            throw new RuntimeException("There is already a module bundle with layer " + bundle.getLayer()
                    + " and half " + bundle.getHalf());
        }
    }

    /**
     * Checks if the orientation of the sensor is axial. Uses the moduleId definition from the "old" geometry for
     * consistency.
     * 
     * @return true if it is, false if it is a stereo sensor
     */
    public static boolean isAxial(boolean isTopLayer, int layer) {
        if (isTopLayer && layer % 2 == 1) {
            return true;
        } else if (!isTopLayer && layer % 2 == 0) {
            return true;
        }
        return false;
    }

    /**
     * Find transform to parent volume coordinate system.
     * 
     * @param t - current transform to mother
     * @param mother - geometry object from current transform
     * @param targetMotherName - parent volume defining new vector coordinate system
     * @return transform.
     */
    public static Transform3D getTransform(Transform3D t, SurveyVolume mother, String targetMotherName) {
        int debug = 0;
        if (debug > 0)
            System.out.printf("getTransform mother %s target %s with current transform\n%s\n", mother.getName(),
                    targetMotherName, t.toString());
        if (mother == null)
            throw new RuntimeException("Trying to get mother transform but there is no mother?!");
        if (mother.getName().equals(targetMotherName)) {
            if (debug > 0)
                System.out.printf("found the transform\n");
            return t;
        } else {
            if (debug > 0)
                System.out.printf("add mother transform\n%s\n", mother.getCoord().getTransformation().toString());
            Transform3D trans = Transform3D.multiply(mother.getCoord().getTransformation(), t);
            if (debug > 0)
                System.out.printf("resulting transform\n%s\ncontinue searching\n", trans.toString());
            return getTransform(trans, mother.getMother(), targetMotherName);
        }

    }

    /**
     * Find the vector in a parent volume coordinate system.
     * 
     * @param vec - vector to transform
     * @param geometry - geometry where vector is defined.
     * @param targetMotherName - parent volume defining new vector coordinate system
     * @return transformed vector.
     */
    public static Hep3Vector transformToMotherCoord(Hep3Vector vec, SurveyVolume geometry, String targetMotherName) {
        int debug = 0;
        SurveyVolume mother = geometry.getMother();
        if (debug > 0)
            System.out.printf("transformToMotherCoord vec %s geomtry %s  mother %s target %s\n", vec.toString(),
                    geometry.getName(), geometry.getMother().getName(), targetMotherName);

        Transform3D t = getTransform(geometry.getCoord().getTransformation(), mother, targetMotherName);

        Hep3Vector vec_t = t.transformed(vec);

        if (debug > 0) {
            System.out.printf("transformToMotherCoord apply transform \n%s\n", t.toString());
            System.out.printf("transformToMotherCoord vec_t%s\n", vec_t.toString());
        }

        return vec_t;
    }

    /**
     * Find the vector in the tracking volume coordinate system.
     * 
     * @param vec - vector to transform
     * @param geometry - geometry where vector is defined.
     * @return transformed vector.
     */
    public static Hep3Vector transformToTracking(Hep3Vector vec, SurveyVolume geometry) {
        return transformToParent(vec, geometry, "trackingVolume");
    }

    /**
     * Find the vector in a mother volume coordinate system.
     * 
     * @param vec - vector to transform
     * @param geometry - geometry where vector is defined.
     * @return transformed vector.
     */
    public static Hep3Vector transformToParent(Hep3Vector vec, SurveyVolume geometry, String targetName) {
        int debug = 0;
        if (debug > 0)
            System.out.printf("\ntransformToParent: vec %s in local coordiantes of %s\n", vec.toString(),
                    geometry.getName());
        if (geometry.getMother() == null) {
            if (debug > 0)
                System.out.printf("\ntransformToParent: no mother, return null\n", vec.toString(), geometry.getName());
            return null;
        }
        if (debug > 0)
            System.out.printf("\ntransformToParent: vec %s in local coordiantes of %s with mother %s\n",
                    vec.toString(), geometry.getName(), geometry.getMother().getName().toString());
        SurveyCoordinateSystem coord = geometry.getCoord();
        if (coord == null) {
            throw new RuntimeException("transformToParent: no coordinate system found for %s, return null "
                    + geometry.getName());
        }
        Hep3Vector vec_mother_coord = coord.getTransformation().transformed(vec);
        if (debug > 0)
            System.out.printf("vec_mother_coord %s\n", vec_mother_coord.toString());
        if (geometry.getMother().getName().equals(targetName)) {
            if (debug > 0)
                System.out.printf("reached target \"%s\"tracking volume. Return \n", targetName);
            return vec_mother_coord;
        } else {
            if (debug > 0)
                System.out.printf("continue searching.\n");
        }
        return transformToParent(vec_mother_coord, geometry.getMother(), targetName);
    }
    
    
    /**
     * Find the vector in a mother volume coordinate system.
     * 
     * @param vec - vector to transform
     * @param geometry - geometry where vector is defined.
     * @return transformed vector.
     */
    public static Hep3Vector rotateToParent(Hep3Vector vec, SurveyVolume geometry, String targetName) {
        int debug = 0;
        if (debug > 0)
            System.out.printf("\nrotateToParent: vec %s in local coordiantes of %s\n", vec.toString(),
                    geometry.getName());
        if (geometry.getMother() == null) {
            if (debug > 0)
                System.out.printf("\nrotateToParent: no mother, return null\n", vec.toString(), geometry.getName());
            return null;
        }
        if (debug > 0)
            System.out.printf("\nrotateToParent: vec %s in local coordinates of %s with mother %s\n",
                    vec.toString(), geometry.getName(), geometry.getMother().getName().toString());
        SurveyCoordinateSystem coord = geometry.getCoord();
        if (coord == null) {
            throw new RuntimeException("rotateToParent: no coordinate system found for %s, return null "
                    + geometry.getName());
        }
        Hep3Vector vec_mother_coord = coord.getTransformation().rotated(vec);
        if (debug > 0)
            System.out.printf("vec_mother_coord %s\n", vec_mother_coord.toString());
        if (geometry.getMother().getName().equals(targetName)) {
            if (debug > 0)
                System.out.printf("reached target \"%s\"tracking volume. Return \n", targetName);
            return vec_mother_coord;
        } else {
            if (debug > 0)
                System.out.printf("continue searching.\n");
        }
        return rotateToParent(vec_mother_coord, geometry.getMother(), targetName);
    }

    

    /**
     * Get axial or stereo key name from string
     * 
     * @param name
     * @return axial or not boolean
     */
    public static boolean isAxialFromName(String name) {
        boolean isAxial;
        if (name.contains("axial"))
            isAxial = true;
        else if (name.contains("stereo"))
            isAxial = false;
        else
            throw new RuntimeException("no axial or stereo name found from " + name);
        return isAxial;
    }

    /**
     * Get hole or slot key name from string
     * 
     * @param name.
     * @return hole or not boolean
     */
    public static boolean isHoleFromName(String name) {
        boolean isHole;
        if (name.contains("hole"))
            isHole = true;
        else if (name.contains("slot"))
            isHole = false;
        else
            throw new RuntimeException("no hole or slot keys found in name " + name);
        return isHole;
    }

    /**
     * Extract old definition of Test Run sensor number.
     * 
     * @param isTopLayer - top or bottom layer
     * @param l - layer
     * @param isAxial - axial or stereo sensor
     * @return
     */
    public int getOldGeomDefLayerFromVolumeName(String name) {

        String half = getHalfFromName(name);
        int l = getLayerFromVolumeName(name);
        boolean isTopLayer = false;
        if (half == "top")
            isTopLayer = true;
        else if (half == "bottom")
            isTopLayer = false;
        else
            throw new RuntimeException("no half found from " + name);
        boolean isAxial = isAxialFromName(name);
        return getOldLayerDefinition(isTopLayer, l, isAxial);
    }

    /**
     * Get the layer number consistent with the old geometry definition.
     * 
     * @param module name that contains layer and half information.
     * @return the layer.
     */
    public int getOldLayerDefinition(boolean isTopLayer, int l, boolean isAxial) {
        int layer = -1;
        if (isAxial) {
            if (isTopLayer) {
                layer = 2 * l - 1;
            } else {
                layer = 2 * l;
            }
        } else {
            if (isTopLayer) {
                layer = 2 * l;
            } else {
                layer = 2 * l - 1;
            }
        }
        return layer;
    }

    /**
     * Definition of the millepede layer number.
     * 
     * @param name of half-module or sensor
     * @return millepede layer number.
     */
    abstract public int getMillepedeLayer(String name);

}