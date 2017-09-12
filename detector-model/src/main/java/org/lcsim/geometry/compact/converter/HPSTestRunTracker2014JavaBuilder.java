/**
 * 
 */
package org.lcsim.geometry.compact.converter;

import java.util.ArrayList;

import org.jdom.Element;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.BasePlate;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.Sensor;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.SupportBottom;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.SupportPlateBottom;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.SupportPlateTop;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.SupportTop;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.TestRunHalfModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder.BaseModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition.TestRunModuleBundle;

/**
 * Class used by java converter to build java run time objects for the detector It encapsulates and adds the LCDD specific information to the generic @HPSTestRunTracker2014Builder.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class HPSTestRunTracker2014JavaBuilder extends HPSTrackerJavaBuilder {

    /**
     * Default constructor
     * 
     * @param node
     */
    public HPSTestRunTracker2014JavaBuilder(boolean debugFlag, Element node) {
        super(debugFlag, node);
    }

    /**
     * Build the JAVA geometry objects from the geometry definition.
     * 
     * @param trackingVolume - the reference volume.
     */
    public void build(ILogicalVolume trackingVolume) {

        // build geometry
        setBuilder(createGeometryDefinition(_debug, node));

        if (_builder == null)
            throw new RuntimeException("need to set builder class before calling build!");

        if (isDebug())
            System.out.printf("%s: build the base geometry objects\n", getClass().getSimpleName());

        _builder.build();

        if (isDebug())
            System.out.printf("%s: DONE build the base geometry objects\n", getClass().getSimpleName());

        if (isDebug())
            System.out.printf("%s: build the JAVA geometry objects\n", getClass().getSimpleName());

        // initialize the list to store a reference to each object
        javaSurveyVolumes = new ArrayList<JavaSurveyVolume>();

        // Go through the list of volumes to build that is created in the generic builder class
        JavaSurveyVolume trackingGeometry = new JavaSurveyVolume(
                _builder.getSurveyVolume(org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition.TrackingVolume.class),
                trackingVolume);
        add(trackingGeometry);
        // setBaseTrackerGeometry(new GhostJavaBaseGeom(_builder.getBaseGeometry(Base.class), trackingGeometry));
        setBaseTrackerGeometry(new JavaSurveyVolume(_builder.getSurveyVolume(TrackerEnvelope.class), trackingGeometry,
                1));
        add(getBaseTrackerGeometry());
        JavaSurveyVolume basePlateGeometry = new JavaGhostSurveyVolume(_builder.getSurveyVolume(BasePlate.class),
                getBaseTrackerGeometry());
        add(basePlateGeometry);
        // skip the c-support, this is purely a reference volume in the builder so should have no use here!?
        // JavaBaseGeometry cSupportGeometry = new GhostJavaBaseGeom(_builder.getBaseGeometry(CSupport.class), baseTrackerGeometry);
        // add(cSupportGeometry);
        JavaSurveyVolume supportBottomGeometry = new JavaGhostSurveyVolume(
                _builder.getSurveyVolume(SupportBottom.class), getBaseTrackerGeometry());
        add(supportBottomGeometry);
        JavaSurveyVolume supportPlateBottomGeometry = new JavaGhostSurveyVolume(
                _builder.getSurveyVolume(SupportPlateBottom.class), getBaseTrackerGeometry());
        add(supportPlateBottomGeometry);
        JavaSurveyVolume supportTopGeometry = new JavaGhostSurveyVolume(_builder.getSurveyVolume(SupportTop.class),
                getBaseTrackerGeometry());
        add(supportTopGeometry);
        JavaSurveyVolume supportPlateTopGeometry = new JavaGhostSurveyVolume(
                _builder.getSurveyVolume(SupportPlateTop.class), getBaseTrackerGeometry());
        add(supportPlateTopGeometry);

        // build modules

        if (isDebug())
            System.out.printf("%s: build JAVA modules\n", getClass().getSimpleName());

        // Loop over all modules created
        for (BaseModuleBundle mod : _builder.modules) {
            TestRunModuleBundle m = (TestRunModuleBundle) mod;
            if (isDebug()) {
                System.out.printf("%s: build module %s (layer %d half %s)\n", getClass().getSimpleName(),
                        m.module.getName(), m.getLayer(), m.getHalf());
                m.print();
            }

            // Find the mother among the objects using its name, should probably have a better way...
            String name_mother = m.getMother().getName();
            JavaSurveyVolume mother = null;
            for (JavaSurveyVolume g : javaSurveyVolumes) {
                if (g.getName().equals(name_mother)) {
                    mother = g;
                    break;
                }
            }
            // Check that it had a mother
            if (mother == null)
                throw new RuntimeException("Cound't find mother to module " + m.module.getName());

            if (isDebug())
                System.out.printf("%s: found mother %s to module %s\n", getClass().getSimpleName(), mother.getName(),
                        m.module.getName());

            // put the module in the list of objects that will be added to LCDD
            addTestRunModule(m, mother);

            if (isDebug())
                System.out.printf("%s: DONE build module %s\n", getClass().getSimpleName(), m.module.getName());

        }

        if (isDebug())
            System.out.printf("%s: DONE build JAVA modules\n", getClass().getSimpleName());

        if (isDebug())
            System.out.printf("%s: DONE building the JAVA geometry objects\n", getClass().getSimpleName());
        if (isDebug()) {
            System.out.printf("%s: DONE building the JAVA geometry objects\n", getClass().getSimpleName());
            System.out.printf("%s: List of all the JAVA geometry objects built\n", this.getClass().getSimpleName());
            for (JavaSurveyVolume bg : javaSurveyVolumes) {
                System.out.printf("-------\n%s\n", bg.toString());
            }
        }

        // Set visualization features
        // setVis();

    }

    /**
     * Rules for adding the JAVA module geometry.
     * 
     * @param bundle - module to be added
     * @param mother - mother JAVA geometry object
     */
    protected void addTestRunModule(TestRunModuleBundle bundle, JavaSurveyVolume mother) {

        if (isDebug()) {
            System.out.printf("%s: addModule %s containing:\n", this.getClass().getSimpleName(),
                    bundle.module.getName());
            bundle.print();
        }

        // Create the module
        JavaSurveyVolume lcddM = new JavaGhostSurveyVolume(bundle.module, mother);
        add(lcddM);

        // add half modules
        if (bundle.halfModuleAxial != null)
            addHalfModule((TestRunHalfModuleBundle) bundle.halfModuleAxial, lcddM);
        if (bundle.halfModuleStereo != null)
            addHalfModule((TestRunHalfModuleBundle) bundle.halfModuleStereo, lcddM);

        if (isDebug()) {
            System.out.printf("%s: DONE addModule %s \n", this.getClass().getSimpleName(), bundle.module.getName());
        }

    }

    /**
     * Rules for adding the JAVA half module geometry.
     * 
     * @param bundle - module to be added
     * @param mother - mother JAVA geometry object
     */
    private void addHalfModule(TestRunHalfModuleBundle bundle, JavaSurveyVolume mother) {
        // Create the half-module
        // This is not a ghost element but reflects the module
        // concept in the old compact description
        // TODO fix the layer IDs
        int oldCompactModuleId = 0;
        JavaSurveyVolume lcddHM = new JavaSurveyVolume(bundle.halfModule, mother, oldCompactModuleId);
        add(lcddHM);

        // ComponentNumber is taken from old geometry where it is simply a counter when adding the xml daughters to the TestRunModule.
        // It is simply 0 for sensor and 1 for carbon fiber in the old geometry
        int componentNumber = ((Sensor) bundle.sensor).getId();

        // create the sensor
        JavaSurveyVolume lcddS = new JavaSurveyVolume(bundle.sensor, lcddHM, componentNumber);
        add(lcddS);

        // create the active sensor
        JavaSurveyVolume lcddAS = new JavaSurveyVolume(bundle.activeSensor, lcddS, componentNumber);
        add(lcddAS);

        // if(isDebug()) {
        // System.out.printf("%s: added sensor %s \n",this.getClass().getSimpleName(), lcddS.getName());
        // System.out.printf("%s: local coordinate system\n%s\n",this.getClass().getSimpleName(), bundle.sensor.getCoord().toString());
        // dsd
        // }

    }

    @Override
    public HPSTrackerGeometryDefinition createGeometryDefinition(boolean debug, Element node) {
        return new HPSTestRunTracker2014GeometryDefinition(debug, node);
    }

}
