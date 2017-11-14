package org.lcsim.geometry.compact.converter;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.lcsim.detector.DetectorIdentifierHelper;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiTrackerModule;
import org.lcsim.geometry.compact.Subdetector;

public abstract class HPSTrackerJavaBuilder implements IHPSTrackerJavaBuilder {

    protected boolean _debug = false;
    private JavaSurveyVolume baseSurveyVolume;
    protected List<JavaSurveyVolume> javaSurveyVolumes = new ArrayList<JavaSurveyVolume>();
    protected DetectorIdentifierHelper detectorIdentifierHelper;
    protected IIdentifierDictionary identifierDictionary;
    protected Subdetector subdet;
    protected List<IDetectorElement> layerDetectorElements = new ArrayList<IDetectorElement>();
    protected List<IDetectorElement> moduleDetectorElements = new ArrayList<IDetectorElement>();
    protected IDetectorElement baseDetectorElement = null;
    public HPSTrackerBuilder _builder = null;
    protected Element node = null;

    public HPSTrackerJavaBuilder(boolean debugFlag, Element node) {
        this._debug = debugFlag;
        this.node = node;
    }

    public abstract void build(ILogicalVolume trackingVolume);

    public abstract HPSTrackerGeometryDefinition createGeometryDefinition(boolean debug, Element node);

    /**
     * Add to list of objects.
     * 
     * @param geom - object to add.
     */
    public void add(JavaSurveyVolume geom) {
        javaSurveyVolumes.add(geom);
    }

    public void setBuilder(HPSTrackerBuilder b) {
        _builder = b;
    }

    public void build() {
        _builder.build();
    }

    public void setDebug(boolean debug) {
        _debug = debug;
    }

    public boolean isDebug() {
        return _debug;
    }

    public DetectorIdentifierHelper getDetectorIdentifierHelper() {
        return detectorIdentifierHelper;
    }

    public void setDetectorIdentifierHelper(DetectorIdentifierHelper detectorIdentifierHelper) {
        this.detectorIdentifierHelper = detectorIdentifierHelper;
    }

    public IIdentifierDictionary getIdentifierDictionary() {
        return identifierDictionary;
    }

    public void setIdentifierDictionary(IIdentifierDictionary identifierDictionary) {
        this.identifierDictionary = identifierDictionary;
    }

    public void setSubdetector(Subdetector subdet) {
        this.subdet = subdet;
    }

    public Subdetector getSubdetector() {
        return this.subdet;
    }

    // This finds specific type. I would like to use the ID for this but can't, I think.
    // TODO there must be a factory instance to do this
    public SiTrackerModule getModuleDetectorElement(SiTrackerModule testElement) {
        if (isDebug())
            System.out.printf("%s: getModuleDetectorElement for module  %s path: \"%s\"\n", this.getClass()
                    .getSimpleName(), testElement.getName(), testElement.getGeometry().getPathString());
        SiTrackerModule element = null;
        for (IDetectorElement e : moduleDetectorElements) {
            SiTrackerModule m = (SiTrackerModule) e;
            if (isDebug())
                System.out.printf("%s: compare with module  %s path: %s\"%s\" \n", this.getClass().getSimpleName(),
                        m.getName(), m.getGeometry().getPathString());
            if (m.getGeometry().getPathString().equals(testElement.getGeometry().getPathString())) {
                if (element != null)
                    throw new RuntimeException("two DE sharing extended ID?");
                if (isDebug())
                    System.out.printf("%s: found it\n", this.getClass().getSimpleName());
                element = m;
            }
        }
        return element;
    }

    // Find detector elements
    // TODO This should be using some global geometry code like DetectorElementStore?
    public IDetectorElement getLayerDetectorElement(IExpandedIdentifier expId) {
        IDetectorElement element = null;
        if (isDebug())
            System.out.printf("%s: search among %d layer DEs\n", this.getClass().getSimpleName(),
                    layerDetectorElements.size());
        for (IDetectorElement e : layerDetectorElements) {
            if (isDebug())
                System.out.printf("%s: test %s\n", this.getClass().getSimpleName(), e.getName());
            ExpandedIdentifier eId = (ExpandedIdentifier) e.getExpandedIdentifier();
            if (eId.equals(expId)) { // TODO order matters as expId is an interface without that function!?
                // check that only one was found
                if (element != null)
                    throw new RuntimeException("two DE sharing extended ID?");
                if (isDebug())
                    System.out.printf("%s: found it\n", this.getClass().getSimpleName());
                element = e;
            }

        }
        return element;
    }

    public void addLayerDetectorElement(IDetectorElement e) {
        IExpandedIdentifier expId = e.getExpandedIdentifier();
        if (getLayerDetectorElement(expId) != null)
            throw new RuntimeException("Trying to add an existing layer detector element.");
        layerDetectorElements.add(e);
    }

    public void addBaseDetectorElement(IDetectorElement e) {
        baseDetectorElement = e;
    }

    public IDetectorElement getBaseDetectorElement() {
        return baseDetectorElement;
    }

    public void addModuleDetectorElement(IDetectorElement e) {
        if (!(e instanceof SiTrackerModule))
            throw new RuntimeException("Trying to add an existing module of wrong type.");
        if (getModuleDetectorElement((SiTrackerModule) e) != null)
            throw new RuntimeException("Trying to add an already existing module detector element.");
        layerDetectorElements.add(e);
    }

    /**
     * @return the baseTrackerGeometry
     */
    public JavaSurveyVolume getBaseTrackerGeometry() {
        return baseSurveyVolume;
    }

    /**
     * @param baseTrackerGeometry the baseTrackerGeometry to set
     */
    public void setBaseTrackerGeometry(JavaSurveyVolume baseTrackerGeometry) {
        this.baseSurveyVolume = baseTrackerGeometry;
    }

}
