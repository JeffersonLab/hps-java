package org.lcsim.geometry.compact.converter.lcdd;

import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.geometry.compact.converter.lcdd.util.Define;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.PhysVol;
import org.lcsim.geometry.compact.converter.lcdd.util.Position;
import org.lcsim.geometry.compact.converter.lcdd.util.Rotation;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Trapezoid;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;

/**
 * LCDD model for the HPS inner ECal.
 * 
 * Coordinate System defined as follows:<br>
 * 
 * <ul>
 * <li>Beam travels in +X direction.</li>
 * <li>Magnetic field is in -Z.</li>
 * <li>Y is the beam bend plane.</li>
 * </ul>
 * 
 * The <dimensions> element defines the crystal HALF dimensions: x1, x2, y1, y2, and z.<br>
 * 
 * The <layout> element defines the number of crystals and (optionally) crystals to be left out.<br>
 * 
 * <ul>
 * <li><b>beamgap</b> - offset from the beamline in the Y coordinate of the top and bottom halves</li>
 * <li><b>beamgapTop</b> - offset from the beamline in the Y coordinate for the top half (defaults to <b>beamgap</b>)</li>
 * <li><b>beamgapBottom</b> - offset from the beamline in the Y coordinate for the bottom half (defaults to <b>beamgap</b>)</li>
 * <li><b>nx</b> - number of crystals in X</li>
 * <li><b>ny</b> - number of crystals in Y along each side of beam</li>
 * <li><b>dface</b> - distance from origin to the face of the calorimeter along Z</li>
 * </ul>
 * 
 * Under the layout element, <remove> tags can be included to exclude crystal placement by range. This element
 * has the following parameters.<br>
 * 
 * <ul>
 * <li><b>ixmin</b> - minimum x index to exclude (inclusive)</li>
 * <li><b>ixmax</b> - maximum x index to exclude (inclusive)</li>
 * <li><b>iymin</b> - minimum y index to exclude (inclusive)</li>
 * <li><b>iymax</b> - maximum y index to exclude (inclusive)</li>
 * </ul>
 * 
 * To be excluded, a crystal's ID must pass all four of these min/max checks.<br>
 */
public class HPSEcal3 extends LCDDSubdetector {
    // Tolerance factor for moving crystals to appropriate place in mom volume.
    static final double tolerance = 0.0;

    // Tolerance factor for separating crystals to avoid overlaps.
    static final double crystalToleranceY = 0.35;
    static final double crystalToleranceX = 0.2;

    // Margin for mother volume.
    static final double margin = 1.1;

    List<CrystalRange> ranges = new ArrayList<CrystalRange>();

    private static class CrystalRange {
        int ixmin;
        int ixmax;
        int iymin;
        int iymax;

        CrystalRange(Element elem) throws Exception {
            ixmin = ixmax = iymin = iymax = 0;

            if (elem.getAttribute("ixmin") != null) {
                ixmin = elem.getAttribute("ixmin").getIntValue();
            } else {
                throw new RuntimeException("Missing ixmin parameter.");
            }

            if (elem.getAttribute("ixmax") != null) {
                ixmax = elem.getAttribute("ixmax").getIntValue();
            } else {
                throw new RuntimeException("Missing ixmax parameter.");
            }

            if (elem.getAttribute("iymin") != null) {
                iymin = elem.getAttribute("iymin").getIntValue();
            } else {
                throw new RuntimeException("Missing ixmax parameter.");
            }

            if (elem.getAttribute("iymax") != null) {
                iymax = elem.getAttribute("iymax").getIntValue();
            } else {
                throw new RuntimeException("Missing iymax parameter.");
            }
        }
    }

    private boolean checkRange(int ix, int iy, List<CrystalRange> ranges) {
        if (ranges.size() == 0)
            return true;
        for (CrystalRange range : ranges) {
            if ((ix >= range.ixmin && ix <= range.ixmax) && ((iy >= range.iymin) && (iy <= range.iymax))) {
                return false;
            }

        }
        return true;
    }

    HPSEcal3(Element node) throws JDOMException {
        super(node);
    }

    void addToLCDD(LCDD lcdd, SensitiveDetector sens) throws JDOMException {
        if (sens == null)
            throw new RuntimeException("SensitiveDetector parameter points to null.");

        // Crystal dimensions.
        Element dimensions = node.getChild("dimensions");
        double dx1 = dimensions.getAttribute("x1").getDoubleValue();
        double dx2 = dimensions.getAttribute("x2").getDoubleValue();
        double dy1 = dimensions.getAttribute("y1").getDoubleValue();
        double dy2 = dimensions.getAttribute("y2").getDoubleValue();
        double dz = dimensions.getAttribute("z").getDoubleValue();

        int system = this.getSystemID();

        // Crystal material.
        Element mat = node.getChild("material");
        String materialName = mat.getAttributeValue("name");

        // Layout parameters.
        Element layout = node.getChild("layout");
        double beamgap = 0;
        if (layout.getAttribute("beamgap") != null) {
            beamgap = layout.getAttribute("beamgap").getDoubleValue();
        } else {
            if (layout.getAttribute("beamgapTop") == null || layout.getAttribute("beamgapBottom") == null) {
                throw new RuntimeException("Missing beamgap parameter in layout element, and beamgapTop or beamgapBottom was not provided.");
            }
        }
        double beamgapTop = beamgap;
        if (layout.getAttribute("beamgapTop") != null) {
            beamgapTop = layout.getAttribute("beamgapTop").getDoubleValue();
        }
        double beamgapBottom = beamgap;
        if (layout.getAttribute("beamgapBottom") != null) {
            beamgapBottom = layout.getAttribute("beamgapBottom").getDoubleValue();
        } 
        int nx = layout.getAttribute("nx").getIntValue();
        int ny = layout.getAttribute("ny").getIntValue();
        double dface = layout.getAttribute("dface").getDoubleValue();
        
        double tdx, tdy, tdz;        
        double bdx, bdy, bdz;
        tdx = tdy = tdz = bdx = bdy = bdz = 0.;
        Element topElement = layout.getChild("top");
        Element bottomElement = layout.getChild("bottom");
        if (topElement != null) {
            if (topElement.getAttribute("dx") != null)
                tdx = topElement.getAttribute("dx").getDoubleValue();
            if (topElement.getAttribute("dy") != null)
                tdy = topElement.getAttribute("dy").getDoubleValue();
            if (topElement.getAttribute("dz") != null)
                tdz = topElement.getAttribute("dz").getDoubleValue();
        }
        if (bottomElement != null) {
            if (bottomElement.getAttribute("dx") != null)
                bdx = bottomElement.getAttribute("dx").getDoubleValue();
            if (bottomElement.getAttribute("dy") != null)
                bdy = bottomElement.getAttribute("dy").getDoubleValue();
            if (bottomElement.getAttribute("dz") != null)
                bdz = bottomElement.getAttribute("dz").getDoubleValue();
        }

        // Setup range of indices to be skipped.
        for (Object obj : layout.getChildren("remove")) {
            Element remove = (Element) obj;
            try {
                ranges.add(new CrystalRange(remove));
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        }

        // Setup crystal logical volume.
        Trapezoid crystalTrap = new Trapezoid("crystal_trap", dx1, dx2, dy1, dy2, dz);
        Volume crystalLogVol = new Volume("crystal_volume", crystalTrap, lcdd.getMaterial(materialName));
        crystalLogVol.setSensitiveDetector(sens);

        // Set vis attributes on crystal log vol.
        setVisAttributes(lcdd, this.getNode(), crystalLogVol);

        // Add shape and log vol to lcdd.
        lcdd.add(crystalTrap);
        lcdd.add(crystalLogVol);

        // Place crystals in world volume.
        Volume world = lcdd.pickMotherVolume(this);

        //
        // Now we calculate parameters for crystal placement...
        //

        // Slope of the trapezoid side in X.
        double sx = (dx2 - dx1) / (2 * dz);

        // Angle of the side of the trapezoid w.r.t. center line in X. Rotation about Y axis.
        double dthetay = atan(sx);

        // Slope of the trapezoid side in Y.
        double sy = (dy2 - dy1) / (2 * dz);

        // Angle of the side of the trapezoid w.r.t. center line in Y. Rotation about X axis.
        double dthetax = atan(sx);

        // Distance between (virtual) angular origin and center of trapezoid in X.
        double z0x = dx1 / sx + dz;

        // Distance between (virtual) angular origin and center of trapezoid in Y.
        double z0y = dy1 / sy + dz;

        // Odd or even number of crystals in X.
        boolean oddx = (nx % 2 != 0);

        // Calculate number of X for loop.
        if (oddx) {
            nx -= 1;
            nx /= 2;
        } else {
            nx /= 2;
        }

        double ycorrtot = 0;
        double zcorrtoty = 0;

        Define define = lcdd.getDefine();

        for (int iy = 1; iy <= ny; iy++) {
            double zcorrtotx = 0;
            double xcorrtot = 0;

            int coeffy = 2 * iy - 1;
            double thetax = coeffy * dthetax;
            double zcorry = dy1 * (2 * sin(coeffy * dthetax));
            double ycorr = zcorry * tan((coeffy - 1) * dthetax);
            double ycenter = z0y * sin(coeffy * dthetax) + ycorr + ycorrtot + (crystalToleranceY * iy);
            double thetaz = 0;

            for (int ix = 0; ix <= nx; ix++) {
                // Coefficient for even/odd crystal
                int coeffx = 2 * ix;
                if (!oddx) {
                    coeffx -= 1;
                    // For even number of crystals, the 0th is skipped.
                    if (ix == 0)
                        continue;
                }

                // Set parameters for next crystal placement.
                double thetay = coeffx * dthetay;
                double zcorrx = dx1 * (2 * sin(coeffx * dthetay));
                double xcorr = zcorrx * tan((coeffx - 1) * dthetay);
                double xcenter = z0x * sin(coeffx * dthetay) + xcorr + xcorrtot + (crystalToleranceX * ix);
                double zcenter = z0y * (cos(coeffy * dthetax) - 1) + z0x * (cos(coeffx * dthetay) - 1) + zcorrx + zcorrtotx + zcorry + zcorrtoty;
                zcenter += dz;

                String baseName = "crystal" + ix + "-" + iy;

                //
                // Bottom section.
                //

                if (checkRange(ix, -iy, ranges)) {
                    // Transform of positive bottom crystal.
                    Position iposBot = new Position(baseName + "_pos_pos_bot", xcenter + bdx, -(beamgapBottom + ycenter + tolerance) + bdy, zcenter + tolerance + dface + bdz);
                    
                    //System.out.println("iposBot = " + iposBot.x() + ", " + iposBot.y() + " , " + iposBot.z() + " --> " + ix + ", " + -iy);
                    Rotation irotBot = new Rotation(baseName + "_rot_pos_bot", -thetax, -thetay, thetaz);
                    define.addPosition(iposBot);
                    define.addRotation(irotBot);

                    // Place positive crystal.
                    PhysVol posCrystalPlacementBot = new PhysVol(crystalLogVol, world, iposBot, irotBot);

                    // Add volume IDs.
                    posCrystalPlacementBot.addPhysVolID("system", system);
                    posCrystalPlacementBot.addPhysVolID("ix", ix);
                    posCrystalPlacementBot.addPhysVolID("iy", -iy);
                }

                // Reflection to negative.
                if (ix != 0) {
                    if (checkRange(-ix, -iy, ranges)) {
                        // Transform of negative.
                        Position iposnegBot = new Position(baseName + "_pos_neg_bot", -xcenter + bdx, -(beamgapBottom + ycenter + tolerance) + bdy, zcenter + tolerance + dface + bdz);
                        //System.out.println("iposnegBot = " + iposnegBot.x() + ", " + iposnegBot.y() + " , " + iposnegBot.z() + " --> " + -ix + ", " + -iy);
                        Rotation irotnegBot = new Rotation(baseName + "_rot_neg_bot", -thetax, thetay, thetaz);

                        define.addPosition(iposnegBot);
                        define.addRotation(irotnegBot);

                        // Place negative crystal.
                        PhysVol negCrystalPlacementBot = new PhysVol(crystalLogVol, world, iposnegBot, irotnegBot);

                        // Add volume IDs.
                        negCrystalPlacementBot.addPhysVolID("system", system);
                        negCrystalPlacementBot.addPhysVolID("ix", -ix);
                        negCrystalPlacementBot.addPhysVolID("iy", -iy);
                    }
                }

                if (checkRange(ix, iy, ranges)) {
                    // Transform of positive top crystal.
                    Position iposTop = new Position(baseName + "_pos_pos_top", xcenter + tdx, beamgapTop + ycenter + tolerance + tdy, zcenter + tolerance + dface + tdz);
                    //System.out.println("iposTop = " + iposTop.x() + ", " + iposTop.y() + " , " + iposTop.z() + " --> " + ix + ", " + iy);
                    Rotation irotTop = new Rotation(baseName + "_rot_pos_top", thetax, -thetay, thetaz);
                    define.addPosition(iposTop);
                    define.addRotation(irotTop);

                    // Place positive top crystal.
                    PhysVol posCrystalPlacementTop = new PhysVol(crystalLogVol, world, iposTop, irotTop);

                    // Add volume IDs.
                    posCrystalPlacementTop.addPhysVolID("system", system);
                    posCrystalPlacementTop.addPhysVolID("ix", ix);
                    posCrystalPlacementTop.addPhysVolID("iy", iy);
                }

                // Reflection to negative.
                if (ix != 0) {
                    if (checkRange(-ix, iy, ranges)) {
                        // Transform of negative.
                        Position iposnegTop = new Position(baseName + "_pos_neg_top", -xcenter + tdx, beamgapTop + ycenter + tolerance + tdy, zcenter + tolerance + dface + tdz);
                        //System.out.println("iposTop = " + iposnegTop.x() + ", " + iposnegTop.y() + " , " + iposnegTop.z() + " --> " + -ix + ", " + iy);
                        Rotation irotnegTop = new Rotation(baseName + "_rot_neg_top", thetax, thetay, thetaz);

                        define.addPosition(iposnegTop);
                        define.addRotation(irotnegTop);

                        // Place negative crystal.
                        PhysVol negCrystalPlacementTop = new PhysVol(crystalLogVol, world, iposnegTop, irotnegTop);

                        // Add volume IDs.
                        negCrystalPlacementTop.addPhysVolID("system", system);
                        negCrystalPlacementTop.addPhysVolID("ix", -ix);
                        negCrystalPlacementTop.addPhysVolID("iy", iy);
                    }
                }

                // Increment running X and Z totals and include tolerance to avoid overlaps.
                xcorrtot += xcorr;
                zcorrtotx += zcorrx;
            }

            // Increment running Y totals.
            ycorrtot += ycorr;
            zcorrtoty += zcorry;
        }
    }

    public boolean isCalorimeter() {
        return true;
    }
}
