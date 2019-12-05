package org.lcsim.geometry.compact.converter;

import org.jdom.Element;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.LongModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.PSVacuumChamber;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.SvtBox;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.SvtBoxBasePlate;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.UChannelL46Bottom;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.UChannelL46BottomPlate;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.UChannelL46Top;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.UChannelL46TopPlate;
import org.lcsim.geometry.compact.converter.HPSTracker2019GeometryDefinition.ShortModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTracker2019GeometryDefinition.ShortModuleBundleOneSensor;
import org.lcsim.geometry.compact.converter.HPSTracker2019GeometryDefinition.UChannelL14Bottom;
import org.lcsim.geometry.compact.converter.HPSTracker2019GeometryDefinition.UChannelL14BottomPlate;
import org.lcsim.geometry.compact.converter.HPSTracker2019GeometryDefinition.UChannelL14Top;
import org.lcsim.geometry.compact.converter.HPSTracker2019GeometryDefinition.UChannelL14TopPlate;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder.BaseModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder.HalfModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition.TestRunModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition.TrackingVolume;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;

public class HPSTracker2019LCDDBuilder extends HPSTracker2014v1LCDDBuilder {

    public HPSTracker2019LCDDBuilder(boolean debugFlag, Element node, LCDD lcdd, SensitiveDetector sens) {
        super(debugFlag, node, lcdd, sens);
    }

    @Override
    public HPSTrackerGeometryDefinition createGeometryDefinition(boolean debug, Element node) {
        return new HPSTracker2019GeometryDefinition(_debug, node);
    }

    @Override
    public void build(Volume worldVolume) {

        // set and build geometry
        setBuilder();

        if (_builder == null)
            throw new RuntimeException("need to set builder class before calling build!");

        if (isDebug())
            System.out.printf("%s: build the base geometry objects\n", getClass().getSimpleName());

        _builder.build();

        if (isDebug())
            System.out.printf("%s: DONE build the base geometry objects\n", getClass().getSimpleName());

        if (isDebug())
            System.out.printf("%s: build the LCDD geometry objects\n", getClass().getSimpleName());

        LCDDSurveyVolume trackingGeometry = new LCDDSurveyVolume(_builder.getSurveyVolume(TrackingVolume.class),
                worldVolume);
        add(trackingGeometry);

        // baseSurveyVolume = new
        // LCDDSurveyVolume(_builder.getSurveyVolume(PSVacuumChamber.class),
        // lcdd, trackingGeometry);
        // add(baseSurveyVolume);
        LCDDSurveyVolume vacuumChamberVolume = new LCDDGhostSurveyVolume(
                _builder.getSurveyVolume(PSVacuumChamber.class), trackingGeometry);
        add(vacuumChamberVolume);

        // LCDDSurveyVolume svtBox = new
        // LCDDSurveyVolume(_builder.getSurveyVolume(SvtBox.class), lcdd,
        // baseSurveyVolume);
        // add(svtBox);

        LCDDSurveyVolume svtBox = new LCDDSurveyVolume(_builder.getSurveyVolume(SvtBox.class), lcdd,
                vacuumChamberVolume);
        baseSurveyVolume = svtBox;
        add(baseSurveyVolume);

        LCDDSurveyVolume svtBoxBasePlate = new LCDDSurveyVolume(_builder.getSurveyVolume(SvtBoxBasePlate.class), lcdd,
                svtBox);
        add(svtBoxBasePlate);

        try {
            _builder.getSurveyVolume(UChannelL14Bottom.class);
        } catch (RuntimeException e) {
            System.out.printf("%s: couldnt find UChannelL14Bottom class among %d volumes\n", this.getClass()
                    .getSimpleName(), _builder.surveyVolumes.size());
            for (SurveyVolume vol : _builder.surveyVolumes)
                System.out.printf("%s:vol %s\n", this.getClass().getSimpleName(), vol.toString());
            throw new RuntimeException("Couldn't find UChannelL14Bottom survey volume.");
        }

        LCDDSurveyVolume uChannelL14Bottom = new LCDDGhostSurveyVolume(
                _builder.getSurveyVolume(UChannelL14Bottom.class), svtBox);
        add(uChannelL14Bottom);

        LCDDSurveyVolume uChannelL14BottomPlate = new LCDDSurveyVolume(
                _builder.getSurveyVolume(UChannelL14BottomPlate.class), lcdd, svtBox);
        add(uChannelL14BottomPlate);

        LCDDSurveyVolume uChannelL14Top = new LCDDGhostSurveyVolume(_builder.getSurveyVolume(UChannelL14Top.class),
                svtBox);
        add(uChannelL14Top);

        LCDDSurveyVolume uChannelL14TopPlate = new LCDDSurveyVolume(
                _builder.getSurveyVolume(UChannelL14TopPlate.class), lcdd, svtBox);
        add(uChannelL14TopPlate);

        LCDDSurveyVolume uChannelL46Bottom = new LCDDGhostSurveyVolume(
                _builder.getSurveyVolume(UChannelL46Bottom.class), svtBox);
        add(uChannelL46Bottom);

        LCDDSurveyVolume uChannelL46BottomPlate = new LCDDSurveyVolume(
                _builder.getSurveyVolume(UChannelL46BottomPlate.class), lcdd, svtBox);
        add(uChannelL46BottomPlate);

        LCDDSurveyVolume uChannelL46Top = new LCDDGhostSurveyVolume(_builder.getSurveyVolume(UChannelL46Top.class),
                svtBox);
        add(uChannelL46Top);

        LCDDSurveyVolume uChannelL46TopPlate = new LCDDSurveyVolume(
                _builder.getSurveyVolume(UChannelL46TopPlate.class), lcdd, svtBox);
        add(uChannelL46TopPlate);

        // build modules

        if (isDebug())
            System.out.printf("%s: build modules\n", getClass().getSimpleName());

        addModules();

        System.out.printf("%s: Built %d LCDD geometry objects\n", getClass().getSimpleName(), lcddSurveyVolumes.size());

        if (isDebug()) {
            System.out.printf("%s: List of all %d LCDD geometry objects built\n", this.getClass().getSimpleName(),
                    lcddSurveyVolumes.size());
            for (SurveyVolumeImpl bg : lcddSurveyVolumes) {
                System.out.printf("-------\n%s\n", bg.toString());
            }
        }

        // Set visualization features
        setVisualization();

    }

    /**
     * Rules for adding the LCDD modules.
     */
    protected void addModules() {
        // Loop over all modules created
        for (BaseModuleBundle mod : _builder.modules) {
            // SVTModuleBundle m = (SVTModuleBundle) mod;
            BaseModuleBundle m = mod;
            if (isDebug()) {
                System.out.printf("%s: module layer %d half %s\n", getClass().getSimpleName(), m.getLayer(),
                        m.getHalf());
                m.print();
            }

            // Find the mother among the LCDD objects using its name, should
            // probably have a better way...
            String name_mother = m.getMother().getName();
            LCDDSurveyVolume mother = null;
            for (LCDDSurveyVolume g : lcddSurveyVolumes) {
                if (g.getName().equals(name_mother)) {
                    mother = g;
                    break;
                }
            }
            // Check that it had a mother
            if (mother == null)
                throw new RuntimeException("Cound't find mother to module layer " + m.getLayer() + " half "
                        + m.getHalf());

            if (isDebug())
                System.out.printf("%s: found mother %s for module layer %d half %s\n", getClass().getSimpleName(),
                        mother.getName(), m.getLayer(), m.getHalf());

            // add the module to the list of objects that will be added to LCDD
            addModule(m, mother);

        }
    }

    /**
     * Rules for adding the LCDD module geometry.
     * 
     * @param bundle - module to be added
     * @param mother - mother LCDD geometry object
     */
    protected void addModule(BaseModuleBundle bundle, LCDDSurveyVolume mother) {
        if (bundle instanceof ShortModuleBundleOneSensor) {
            addShortModuleOneSensor((ShortModuleBundleOneSensor) bundle, mother);
        } else if (bundle instanceof ShortModuleBundle) {
            addShortModule((ShortModuleBundle) bundle, mother);
        } else if (bundle instanceof TestRunModuleBundle) {
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
     * @param bundle - module to be added
     * @param mother - mother LCDD geometry object
     */
    protected void addShortModule(ShortModuleBundle bundle, LCDDSurveyVolume mother) {
        LCDDSurveyVolume lcddM = new LCDDGhostSurveyVolume(bundle.module, mother);
        // LCDDSurveyVolume lcddM = new LCDDSurveyVolume(bundle.module, lcdd,
        // mother);
        add(lcddM);
        if (bundle.halfModuleAxialHole != null)
            addLongHalfModule(bundle.halfModuleAxialHole, lcddM);
        if (bundle.halfModuleAxialSlot != null)
            addLongHalfModule(bundle.halfModuleAxialSlot, lcddM);
        if (bundle.coldBlock != null)
            add(new LCDDSurveyVolume(bundle.coldBlock, lcdd, lcddM));
        if (bundle.halfModuleStereoHole != null)
            addLongHalfModule(bundle.halfModuleStereoHole, lcddM);
        if (bundle.halfModuleStereoSlot != null)
            addLongHalfModule(bundle.halfModuleStereoSlot, lcddM);
    }
    
    protected void addShortModuleOneSensor(ShortModuleBundleOneSensor bundle, LCDDSurveyVolume mother) {
        LCDDSurveyVolume lcddM = new LCDDGhostSurveyVolume(bundle.module, mother);
        // LCDDSurveyVolume lcddM = new LCDDSurveyVolume(bundle.module, lcdd,
        // mother);
        add(lcddM);
        if (bundle.halfModuleAxial != null)
            addLongHalfModule(bundle.halfModuleAxial, lcddM);
        //if (bundle.halfModuleAxialSlot != null)
        //    addLongHalfModule(bundle.halfModuleAxialSlot, lcddM);
        if (bundle.coldBlock != null)
            add(new LCDDSurveyVolume(bundle.coldBlock, lcdd, lcddM));
        if (bundle.halfModuleStereo != null)
            addLongHalfModule(bundle.halfModuleStereo, lcddM);
        //if (bundle.halfModuleStereoSlot != null)
        //    addLongHalfModule(bundle.halfModuleStereoSlot, lcddM);
    }

    /**
     * Rules for adding the LCDD half module geometry.
     * 
     * @param bundle - module to be added
     * @param mother - mother LCDD geometry object
     */
    protected void addShortHalfModule(HalfModuleBundle bundle2, LCDDSurveyVolume mother) {
        // LongHalfModuleBundle bundle = (LongHalfModuleBundle) bundle2;
        HalfModuleBundle bundle = bundle2;
        // create the half-module
        LCDDSurveyVolume lcddHM = new LCDDSurveyVolume(bundle.halfModule, lcdd, mother);
        add(lcddHM);
        // create the sensor
        LCDDSurveyVolume lcddS = new LCDDSurveyVolume(bundle.sensor, lcdd, lcddHM);
        add(lcddS);
        // create the active sensor
        LCDDSurveyVolume lcddAS = new LCDDSurveyVolume(bundle.activeSensor, lcdd, lcddS);
        add(lcddAS);
        /*
         * // create the lamination LCDDSurveyVolume lcddL = new LCDDSurveyVolume(bundle.lamination, lcdd, lcddHM); add(lcddL); // create the carbon fiber frame LCDDSurveyVolume lcddCF = new LCDDSurveyVolume(bundle.carbonFiber, lcdd, lcddHM); add(lcddCF); // create the hybrid frame LCDDSurveyVolume lcddH = new LCDDSurveyVolume(bundle.hybrid, lcdd, lcddHM); add(lcddH);
         */
    }

}
