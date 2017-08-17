package org.lcsim.geometry.compact.converter;

import org.jdom.Element;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.LongModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTracker2017GeometryDefinition.ShortModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder.BaseModuleBundle;
import org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition.TestRunModuleBundle;

public class HPSTracker2017JavaBuilder extends HPSTracker2014v1JavaBuilder {

    public HPSTracker2017JavaBuilder(boolean debugFlag, Element node) {
        super(debugFlag, node);
    }

    @Override
    public HPSTrackerGeometryDefinition createGeometryDefinition(boolean debug,
            Element node) {
        return new HPSTracker2017GeometryDefinition(debug, node);
    }

    protected void addModule(BaseModuleBundle bundle, JavaSurveyVolume mother) {
        if (bundle instanceof TestRunModuleBundle) {
            addTestRunModule((TestRunModuleBundle) bundle, mother);
        } else if (bundle instanceof ShortModuleBundle) {
            addShortModule((ShortModuleBundle) bundle, mother);

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
    protected void addShortModule(ShortModuleBundle bundle,
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

}
