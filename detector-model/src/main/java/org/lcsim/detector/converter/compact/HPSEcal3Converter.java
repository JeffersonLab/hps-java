package org.lcsim.detector.converter.compact;

import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jdom.DataConversionException;
import org.jdom.Element;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.LogicalVolume;
import org.lcsim.detector.PhysicalVolume;
import org.lcsim.detector.RotationGeant;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.material.MaterialStore;
import org.lcsim.detector.solids.Trd;
import org.lcsim.geometry.compact.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.geometry.subdetector.HPSEcal3;

public class HPSEcal3Converter extends AbstractSubdetectorConverter {

    private static Logger LOGGER = Logger.getLogger(HPSEcal3Converter.class.toString());
    
    static class CrystalRange {

        int xIndexMax;
        int xIndexMin;
        int yIndexMax;
        int yIndexMin;

        CrystalRange(final Element elem) throws Exception {
            xIndexMin = xIndexMax = yIndexMin = yIndexMax = 0;

            if (elem.getAttribute("ixmin") != null) {
                xIndexMin = elem.getAttribute("ixmin").getIntValue();
            } else {
                throw new RuntimeException("Missing ixmin parameter.");
            }

            if (elem.getAttribute("ixmax") != null) {
                xIndexMax = elem.getAttribute("ixmax").getIntValue();
            } else {
                throw new RuntimeException("Missing ixmax parameter.");
            }

            if (elem.getAttribute("iymin") != null) {
                yIndexMin = elem.getAttribute("iymin").getIntValue();
            } else {
                throw new RuntimeException("Missing ixmax parameter.");
            }

            if (elem.getAttribute("iymax") != null) {
                yIndexMax = elem.getAttribute("iymax").getIntValue();
            } else {
                throw new RuntimeException("Missing iymax parameter.");
            }
        }
    }

    static final double crystalToleranceX = 0.2;
    // Tolerance factor for separating crystals to avoid overlaps.
    static final double crystalToleranceY = 0.35;

    // Margin for mother volume.
    static final double margin = 1.1;

    // Tolerance factor for moving crystals to appropriate place in mom volume.
    static final double tolerance = 0.0;

    IIdentifierDictionary dict;
    IIdentifierHelper helper;

    List<CrystalRange> ranges = new ArrayList<CrystalRange>();

    private boolean checkRange(final int ix, final int iy, final List<CrystalRange> ranges) {
        if (ranges.size() == 0) {
            return true;
        }
        for (final CrystalRange range : ranges) {
            if (ix >= range.xIndexMin && ix <= range.xIndexMax && iy >= range.yIndexMin && iy <= range.yIndexMax) {
                return false;
            }

        }
        return true;
    }

    @Override
    public void convert(final Subdetector subdet, final Detector detector) {
        
        LOGGER.info("converting subdetector " + subdet.getName());

        helper = subdet.getDetectorElement().getIdentifierHelper();
        dict = helper.getIdentifierDictionary();

        // Crystal dimensions.
        final Element dimensions = subdet.getNode().getChild("dimensions");

        double dx1, dx2, dy1, dy2, dz;
        Element layout;
        double beamgap = 0;
        double beamgapTop, beamgapBottom;
        int nx, ny;
        double dface;
        double tdx, tdy, tdz;
        double bdx, bdy, bdz;
        tdx = tdy = tdz = bdx = bdy = bdz = 0.;

        try {
            dx1 = dimensions.getAttribute("x1").getDoubleValue();
            dx2 = dimensions.getAttribute("x2").getDoubleValue();
            dy1 = dimensions.getAttribute("y1").getDoubleValue();
            dy2 = dimensions.getAttribute("y2").getDoubleValue();
            dz = dimensions.getAttribute("z").getDoubleValue();

            // Layout parameters.
            layout = subdet.getNode().getChild("layout");
            if (layout.getAttribute("beamgap") != null) {
                beamgap = layout.getAttribute("beamgap").getDoubleValue();
            } else {
                if (layout.getAttribute("beamgapTop") == null || layout.getAttribute("beamgapBottom") == null) {
                    throw new RuntimeException(
                            "Missing beamgap parameter in layout element, and beamgapTop or beamgapBottom was not provided.");
                }
            }
            beamgapTop = beamgap;
            if (layout.getAttribute("beamgapTop") != null) {
                beamgapTop = layout.getAttribute("beamgapTop").getDoubleValue();
            }
            beamgapBottom = beamgap;
            if (layout.getAttribute("beamgapBottom") != null) {
                beamgapBottom = layout.getAttribute("beamgapBottom").getDoubleValue();
            }
            nx = layout.getAttribute("nx").getIntValue();
            ny = layout.getAttribute("ny").getIntValue();
            dface = layout.getAttribute("dface").getDoubleValue();

            final Element topElement = layout.getChild("top");
            final Element bottomElement = layout.getChild("bottom");
            if (topElement != null) {
                if (topElement.getAttribute("dx") != null) {
                    tdx = topElement.getAttribute("dx").getDoubleValue();
                }
                if (topElement.getAttribute("dy") != null) {
                    tdy = topElement.getAttribute("dy").getDoubleValue();
                }
                if (topElement.getAttribute("dz") != null) {
                    tdz = topElement.getAttribute("dz").getDoubleValue();
                }
            }
            if (bottomElement != null) {
                if (bottomElement.getAttribute("dx") != null) {
                    bdx = bottomElement.getAttribute("dx").getDoubleValue();
                }
                if (bottomElement.getAttribute("dy") != null) {
                    bdy = bottomElement.getAttribute("dy").getDoubleValue();
                }
                if (bottomElement.getAttribute("dz") != null) {
                    bdz = bottomElement.getAttribute("dz").getDoubleValue();
                }
            }
        } catch (final DataConversionException e) {
            throw new RuntimeException("Error converting HPSEcal3 from XML.", e);
        }

        // Crystal material.
        final Element mat = subdet.getNode().getChild("material");
        final String materialName = mat.getAttributeValue("name");

        // Setup range of indices to be skipped.
        for (final Object obj : layout.getChildren("remove")) {
            final Element remove = (Element) obj;
            try {
                ranges.add(new CrystalRange(remove));
            } catch (final Exception x) {
                throw new RuntimeException(x);
            }
        }

        if (!ranges.isEmpty()) {
            // FIXME: Assumes single range of removed crystals.
            ((HPSEcalDetectorElement) subdet.getDetectorElement()).setBeamGapIndices(ranges.get(0));
        }

        // Setup crystal logical volume.
        final Trd crystalTrap = new Trd("crystal_trap", dx1, dx2, dy1, dy2, dz);
        final ILogicalVolume crystalLogVol = new LogicalVolume("crystal_volume", crystalTrap, MaterialStore
                .getInstance().get(materialName));

        //
        // Now we calculate parameters for crystal placement...
        //

        // Slope of the trapezoid side in X.
        final double sx = (dx2 - dx1) / (2 * dz);

        // Angle of the side of the trapezoid w.r.t. center line in X. Rotation
        // about Y axis.
        final double dthetay = atan(sx);

        // Slope of the trapezoid side in Y.
        final double sy = (dy2 - dy1) / (2 * dz);

        // Angle of the side of the trapezoid w.r.t. center line in Y. Rotation
        // about X axis.
        final double dthetax = atan(sx);

        // Distance between (virtual) angular origin and center of trapezoid in
        // X.
        final double z0x = dx1 / sx + dz;

        // Distance between (virtual) angular origin and center of trapezoid in
        // Y.
        final double z0y = dy1 / sy + dz;

        // Odd or even number of crystals in X.
        final boolean oddx = nx % 2 != 0;

        // Calculate number of X for loop.
        if (oddx) {
            nx -= 1;
            nx /= 2;
        } else {
            nx /= 2;
        }

        double ycorrtot = 0;
        double zcorrtoty = 0;

        // Crystal sequence number used for unique volume names.
        int crystaln = 1;

        // Base name for volume.
        final String baseName = subdet.getName() + "_crystal";

        // World volume.
        final ILogicalVolume mom = detector.getWorldVolume().getLogicalVolume();

        for (int iy = 1; iy <= ny; iy++) {
            double zcorrtotx = 0;
            double xcorrtot = 0;

            final int coeffy = 2 * iy - 1;
            final double thetax = coeffy * dthetax;
            final double zcorry = dy1 * (2 * sin(coeffy * dthetax));
            final double ycorr = zcorry * tan((coeffy - 1) * dthetax);
            final double ycenter = z0y * sin(coeffy * dthetax) + ycorr + ycorrtot + crystalToleranceY * iy;
            final double thetaz = 0;

            for (int ix = 0; ix <= nx; ix++) {
                // Coefficient for even/odd crystal
                int coeffx = 2 * ix;
                if (!oddx) {
                    coeffx -= 1;
                    // For even number of crystals, the 0th is skipped.
                    if (ix == 0) {
                        continue;
                    }
                }

                // Set parameters for next crystal placement.
                final double thetay = coeffx * dthetay;
                final double zcorrx = dx1 * (2 * sin(coeffx * dthetay));
                final double xcorr = zcorrx * tan((coeffx - 1) * dthetay);
                final double xcenter = z0x * sin(coeffx * dthetay) + xcorr + xcorrtot + crystalToleranceX * ix;
                double zcenter = z0y * (cos(coeffy * dthetax) - 1) + z0x * (cos(coeffx * dthetay) - 1) + zcorrx
                        + zcorrtotx + zcorry + zcorrtoty;
                zcenter += dz;

                //
                // Bottom section.
                //

                if (this.checkRange(ix, -iy, ranges)) {
                    // Transform of positive bottom crystal.
                    final ITranslation3D iposBot = new Translation3D(xcenter + bdx,
                            -(beamgapBottom + ycenter + tolerance) + bdy, zcenter + tolerance + dface + bdz);
                    final IRotation3D irotBot = new RotationGeant(-thetax, -thetay, thetaz);

                    // Place positive crystal.
                    final IPhysicalVolume posCrystalPlacementBot = new PhysicalVolume(
                            new Transform3D(iposBot, irotBot), baseName + crystaln, crystalLogVol, mom, crystaln);
                    this.createDetectorElement(detector, subdet, posCrystalPlacementBot, ix, -iy);
                    ++crystaln;
                }

                // Reflection to negative.
                if (ix != 0) {
                    if (this.checkRange(-ix, -iy, ranges)) {
                        // Transform of negative.
                        final ITranslation3D iposnegBot = new Translation3D(-xcenter + bdx,
                                -(beamgapBottom + ycenter + tolerance) + bdy, zcenter + tolerance + dface + bdz);
                        final IRotation3D irotnegBot = new RotationGeant(-thetax, thetay, thetaz);

                        // Place negative crystal.
                        final PhysicalVolume negCrystalPlacementBot = new PhysicalVolume(new Transform3D(iposnegBot,
                                irotnegBot), baseName + crystaln, crystalLogVol, detector.getWorldVolume()
                                .getLogicalVolume(), crystaln);
                        this.createDetectorElement(detector, subdet, negCrystalPlacementBot, -ix, -iy);
                        ++crystaln;
                    }
                }

                if (this.checkRange(ix, iy, ranges)) {
                    // Transform of positive top crystal.
                    final Translation3D iposTop = new Translation3D(xcenter + tdx, beamgapTop + ycenter + tolerance
                            + tdy, zcenter + tolerance + dface + tdz);
                    final IRotation3D irotTop = new RotationGeant(thetax, -thetay, thetaz);

                    // Place positive top crystal.
                    final PhysicalVolume posCrystalPlacementTop = new PhysicalVolume(new Transform3D(iposTop, irotTop),
                            baseName + crystaln, crystalLogVol, detector.getWorldVolume().getLogicalVolume(), crystaln);
                    this.createDetectorElement(detector, subdet, posCrystalPlacementTop, ix, iy);
                    ++crystaln;
                }

                // Reflection to negative.
                if (ix != 0) {
                    if (this.checkRange(-ix, iy, ranges)) {
                        // Transform of negative.
                        final ITranslation3D iposnegTop = new Translation3D(-xcenter + tdx, beamgapTop + ycenter
                                + tolerance + tdy, zcenter + tolerance + dface + tdz);
                        final IRotation3D irotnegTop = new RotationGeant(thetax, thetay, thetaz);

                        // Place negative crystal.
                        final PhysicalVolume negCrystalPlacementTop = new PhysicalVolume(new Transform3D(iposnegTop,
                                irotnegTop), baseName + crystaln, crystalLogVol, detector.getWorldVolume()
                                .getLogicalVolume(), crystaln);
                        this.createDetectorElement(detector, subdet, negCrystalPlacementTop, -ix, iy);
                        ++crystaln;
                    }
                }

                // Increment running X and Z totals and include tolerance to
                // avoid overlaps.
                xcorrtot += xcorr;
                zcorrtotx += zcorrx;
            }

            // Increment running Y totals.
            ycorrtot += ycorr;
            zcorrtoty += zcorry;
        }

        // Initialize state of ECal detector element.
        ((HPSEcalDetectorElement) subdet.getDetectorElement()).initialize();
    }

    /**
     * Create a DetectorElement for an ECal crystal.
     *
     * @param detector The full detector.
     * @param subdet The subdetector.
     * @param crystal The crystal physical volume.
     * @param ix The value of the ix field.
     * @param iy The value of the iy field.
     */
    private final void createDetectorElement(final Detector detector, final Subdetector subdet,
            final IPhysicalVolume crystal, final int ix, final int iy) {
        final String path = "/" + crystal.getName();
        final IExpandedIdentifier expId = new ExpandedIdentifier(helper.getIdentifierDictionary().getNumberOfFields());
        expId.setValue(dict.getFieldIndex("system"), subdet.getSystemID());
        expId.setValue(dict.getFieldIndex("ix"), ix);
        expId.setValue(dict.getFieldIndex("iy"), iy);
        final IIdentifier id = helper.pack(expId);
        new EcalCrystal(subdet.getName() + "_crystal" + crystal.getCopyNumber(), subdet.getDetectorElement(), path, id);
    }

    @Override
    public Class getSubdetectorType() {
        return HPSEcal3.class;
    }

    @Override
    public IDetectorElement makeSubdetectorDetectorElement(final Detector detector, final Subdetector subdetector) {
        LOGGER.info("creating detector element for subdetector " + subdetector.getName());
        final IDetectorElement subdetectorDE = new HPSEcalDetectorElement(subdetector.getName(),
                detector.getDetectorElement());
        subdetector.setDetectorElement(subdetectorDE);
        return subdetectorDE;
    }
}
