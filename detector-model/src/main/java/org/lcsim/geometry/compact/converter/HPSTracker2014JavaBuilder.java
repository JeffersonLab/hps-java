/**
 * 
 */
package org.lcsim.geometry.compact.converter;

import java.util.ArrayList;

import org.jdom.Element;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.BaseSensor;
//import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.Sensor;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.LongHalfModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.LongModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.PSVacuumChamber;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.SvtBox;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.SvtBoxBasePlate;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder.BaseModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder.HalfModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition.TestRunModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition.TrackingVolume;

/**
 * Class used by java converter to build java run time objects for the detector
 * It encapsulates and adds the LCDD specific information to the generic
 * @HPSTestRunTracker2014Builder.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class HPSTracker2014JavaBuilder extends HPSTestRunTracker2014JavaBuilder {

    /**
     * Default constructor
     * 
     * @param node
     */
    public HPSTracker2014JavaBuilder(boolean debugFlag, Element node) {
        super(debugFlag, node);
    }

    /**
     * Build the JAVA geometry objects from the geometry definition.
     * 
     * @param trackingVolume
     *            - the reference volume.
     */
    public void build(ILogicalVolume trackingVolume) {

        // build geometry
        setBuilder(createGeometryDefinition(this._debug, node));

        if (_builder == null)
            throw new RuntimeException(
                    "need to set builder class before calling build!");

        if (isDebug())
            System.out.printf("%s: build the base geometry objects\n",
                    getClass().getSimpleName());

        _builder.build();

        if (isDebug())
            System.out.printf("%s: DONE build the base geometry objects\n",
                    getClass().getSimpleName());

        if (isDebug())
            System.out.printf("%s: build the JAVA geometry objects\n",
                    getClass().getSimpleName());

        // initialize the list to store a reference to each object
        javaSurveyVolumes = new ArrayList<JavaSurveyVolume>();

        // Go through the list of volumes to build that is created in the
        // generic builder class
        JavaSurveyVolume tracking = new JavaSurveyVolume(
                _builder.getSurveyVolume(TrackingVolume.class), trackingVolume);
        add(tracking);
        JavaSurveyVolume chamber = new JavaGhostSurveyVolume(
                _builder.getSurveyVolume(PSVacuumChamber.class), tracking);
        add(chamber);
        setBaseTrackerGeometry(new JavaSurveyVolume(
                _builder.getSurveyVolume(SvtBox.class), chamber, 1));
        add(getBaseTrackerGeometry());
        JavaSurveyVolume svtBoxBasePlate = new JavaGhostSurveyVolume(
                _builder.getSurveyVolume(SvtBoxBasePlate.class),
                getBaseTrackerGeometry());
        add(svtBoxBasePlate);

        // build modules

        if (isDebug())
            System.out.printf("%s: build JAVA modules\n", getClass()
                    .getSimpleName());

        // Loop over all modules created
        for (BaseModuleBundle mod : _builder.modules) {
            BaseModuleBundle m = mod;
            if (isDebug()) {
                System.out.printf("%s: build module %s (layer %d half %s)\n",
                        getClass().getSimpleName(), m.module.getName(),
                        m.getLayer(), m.getHalf());
                m.print();
            }

            // Find the mother among the objects using its name, should probably
            // have a better way...
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
                throw new RuntimeException("Cound't find mother to module "
                        + m.module.getName());

            if(isDebug()) {
                System.out.printf("%s: found ewfsdhf mother %s to module %s\n",
                        getClass().getSimpleName(), mother.getName(),
                        m.module.getName());
            }

            // put the module in the list of objects that will be added to LCDD
            addModule(m, mother);

            if (isDebug())
                System.out.printf("%s: DONE build module %s\n", getClass()
                        .getSimpleName(), m.module.getName());

        }

        // if(isDebug())
        System.out.printf("%s: DONE build JAVA modules\n", getClass()
                .getSimpleName());

        // System.out.printf("%s: Built %d JAVA geometry objects\n",
        // getClass().getSimpleName(),javaSurveyVolumes.size());

        if (isDebug()) {
            System.out.printf("%s: DONE building the JAVA geometry objects\n",
                    getClass().getSimpleName());
            System.out.printf(
                    "%s: List of all the JAVA geometry objects built\n", this
                            .getClass().getSimpleName());
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
     * @param bundle
     *            - module to be added
     * @param mother
     *            - mother JAVA geometry object
     */
    protected void addModule(BaseModuleBundle bundle, JavaSurveyVolume mother) {
        if (bundle instanceof TestRunModuleBundle) {
            addTestRunModule((TestRunModuleBundle) bundle, mother);
        } else if (bundle instanceof LongModuleBundle) {
            addLongModule((LongModuleBundle) bundle, mother);
        } else {
            throw new RuntimeException("The bundle is of unknown class type!");
        }
    }

    /**
     * Rules for adding the LCDD module geometry.
     * 
     * @param bundle
     *            - module to be added
     * @param mother
     *            - mother LCDD geometry object
     */
    protected void addLongModule(LongModuleBundle bundle,
            JavaSurveyVolume mother) {
        // This could perhaps be fixed if there is a relation with daughters in
        // geometry definition?
        // create the module
        JavaSurveyVolume lcddM = new JavaGhostSurveyVolume(bundle.module,
                mother);
        add(lcddM);
        if (bundle.halfModuleAxialHole != null)
            addLongHalfModule(bundle.halfModuleAxialHole, lcddM);
        if (bundle.halfModuleAxialSlot != null)
            addLongHalfModule(bundle.halfModuleAxialSlot, lcddM);
        // if(bundle.coldBlock!=null) add(new LCDDSurveyVolume(bundle.coldBlock,
        // lcdd, lcddM));
        if (bundle.halfModuleStereoHole != null)
            addLongHalfModule(bundle.halfModuleStereoHole, lcddM);
        if (bundle.halfModuleStereoSlot != null)
            addLongHalfModule(bundle.halfModuleStereoSlot, lcddM);
    }

    protected void addLongHalfModule(HalfModuleBundle bundle2,
            JavaSurveyVolume mother) {

        LongHalfModuleBundle bundle = (LongHalfModuleBundle) bundle2;

        // Create the half-module
        // This is not a ghost element but reflects the module
        // concept in the old compact description
        int oldCompactModuleId = 0;
        JavaSurveyVolume lcddHM = new JavaSurveyVolume(bundle.halfModule,
                mother, oldCompactModuleId);

        add(lcddHM);

        // ComponentNumber is taken from old geometry where it is simply a
        // counter when adding the xml daughters to the TestRunModule.
        // It is simply 0 for sensor and 1 for carbon fiber in the old geometry
        int componentNumber = ((BaseSensor) bundle.sensor).getId();

        // create the sensor
        JavaSurveyVolume lcddS = new JavaSurveyVolume(bundle.sensor, lcddHM,
                componentNumber);
        add(lcddS);

        // create the active sensor
        JavaSurveyVolume lcddAS = new JavaSurveyVolume(bundle.activeSensor,
                lcddS, componentNumber);
        add(lcddAS);

        if(isDebug()) {
            System.out.printf("%s: added sensor %s \n", this.getClass()
                    .getSimpleName(), lcddS.getName());
            System.out.printf("%s: local coordinate system\n%s\n", this.getClass()
                    .getSimpleName(), bundle.sensor.getCoord().toString());
        }
    }

    @Override
    public HPSTrackerGeometryDefinition createGeometryDefinition(boolean debug,
            Element node) {
        return new HPSTracker2014GeometryDefinition(debug, node);
    }

}
