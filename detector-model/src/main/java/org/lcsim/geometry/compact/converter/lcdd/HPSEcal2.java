package org.lcsim.geometry.compact.converter.lcdd;

import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.geometry.compact.converter.lcdd.util.Box;
import org.lcsim.geometry.compact.converter.lcdd.util.Define;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.PhysVol;
import org.lcsim.geometry.compact.converter.lcdd.util.Position;
import org.lcsim.geometry.compact.converter.lcdd.util.Rotation;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Trapezoid;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;

/**
 * LCDD model for the HPS inner ECal. Coordinate System Notes: Beam travels in +X direction. Magnetic field is -Z (????). Y is the beam bend plane. The dimensions element defines the crystal HALF dimensions: x1, x2, y1, y2, and z. The layout element defines the placement of the crystals.
 * <ul>
 * <li>beamgap - offset from the beamline in the Y coordinate</li>
 * <li>nx - number of crystals in X</li>
 * <li>ny - number of crystals in Y along each side of beam</li>
 * <li>dface - distance from origin to the face of the calorimeter along X</li>
 * </ul>
 * 
 * @author Jeremy McCormick
 * @author Tim Nelson
 * @version $Id: HPSEcal2.java,v 1.1 2011/07/14 22:45:55 jeremy Exp $
 */
public class HPSEcal2 extends LCDDSubdetector {

    // Tolerance factor for moving crystals to appropriate place in mom volume.
    static final double tolerance = 0.1;

    // Tolerance factor for separating crystals to avoid overlaps.
    static final double crystalTolerance = 0.1;

    HPSEcal2(Element node) throws JDOMException {
        super(node);
    }

    void addToLCDD(LCDD lcdd, SensitiveDetector sens) throws JDOMException {
        if (sens == null)
            throw new RuntimeException("SensitiveDetector is null!");

        // Crystal dimensions.
        Element dimensions = node.getChild("dimensions");
        double dx1 = dimensions.getAttribute("x1").getDoubleValue();
        double dx2 = dimensions.getAttribute("x2").getDoubleValue();
        double dy1 = dimensions.getAttribute("y1").getDoubleValue();
        double dy2 = dimensions.getAttribute("y2").getDoubleValue();
        double dz = dimensions.getAttribute("z").getDoubleValue();

        // Crystal material.
        Element mat = node.getChild("material");
        String materialName = mat.getAttributeValue("name");

        // Layout parameters.
        Element layout = node.getChild("layout");
        double beamgap = layout.getAttribute("beamgap").getDoubleValue();
        int nx = layout.getAttribute("nx").getIntValue();
        int ny = layout.getAttribute("ny").getIntValue();
        double dface = layout.getAttribute("dface").getDoubleValue();

        // Setup crystal logical volume.
        Trapezoid crystalTrap = new Trapezoid("crystal_trap", dx1, dx2, dy1, dy2, dz);
        Volume crystalLogVol = new Volume("crystal_volume", crystalTrap, lcdd.getMaterial(materialName));
        crystalLogVol.setSensitiveDetector(sens);

        lcdd.add(crystalTrap);
        lcdd.add(crystalLogVol);

        // Mother volume dimensions.
        double mx, my, mz;
        double margin = 1.1;
        mx = nx * Math.max(dx2, dx1) * margin;
        my = ny * Math.max(dy2, dy1) * margin;
        mz = dz * margin;

        // Envelope box and logical volume for one section of ECal.
        Box momBox = new Box("ecal_env_box", mx * 2, my * 2, mz * 2);
        Volume momVol = new Volume("ecal_env_volume", momBox, lcdd.getMaterial("Air"));

        lcdd.add(momBox);
        lcdd.add(momVol);

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
            double ycenter = z0y * sin(coeffy * dthetax) + ycorr + ycorrtot + (crystalTolerance * iy);

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
                // FIXME Need documentation here.
                double thetay = coeffx * dthetay;
                double zcorrx = dx1 * (2 * sin(coeffx * dthetay));
                double xcorr = zcorrx * tan((coeffx - 1) * dthetay);
                double xcenter = z0x * sin(coeffx * dthetay) + xcorr + xcorrtot + (crystalTolerance * ix);
                double zcenter = z0y * (cos(coeffy * dthetax) - 1) + z0x * (cos(coeffx * dthetay) - 1) + zcorrx
                        + zcorrtotx + zcorry + zcorrtoty;
                zcenter += dz;

                double thetaz = 0;

                String baseName = "crystal" + ix + "-" + iy;

                // Transform of positive.
                Position ipos = new Position(baseName + "_pos_pos", xcenter, ycenter - my + tolerance, zcenter - mz
                        + tolerance);
                Rotation irot = new Rotation(baseName + "_rot_pos", thetax, -thetay, thetaz);

                define.addPosition(ipos);
                define.addRotation(irot);

                // Place positive crystal.
                PhysVol posCrystalPlacement = new PhysVol(crystalLogVol, momVol, ipos, irot);

                // Add volume IDs.
                posCrystalPlacement.addPhysVolID("ix", ix);
                posCrystalPlacement.addPhysVolID("iy", iy);

                // Reflection to negative.
                if (ix != 0) {
                    // Transform of negative.
                    Position iposneg = new Position(baseName + "_pos_neg", -xcenter, ycenter - my + tolerance, zcenter
                            - mz + tolerance);
                    Rotation irotneg = new Rotation(baseName + "_rot_neg", thetax, thetay, thetaz);

                    define.addPosition(iposneg);
                    define.addRotation(irotneg);

                    // Place negative crystal.
                    PhysVol negCrystalPlacement = new PhysVol(crystalLogVol, momVol, iposneg, irotneg);

                    // Add volume IDs.
                    negCrystalPlacement.addPhysVolID("ix", -ix);
                    negCrystalPlacement.addPhysVolID("iy", iy);
                }

                // Increment running X and Z totals and include tolerance to avoid overlaps.
                xcorrtot += xcorr;
                zcorrtotx += zcorrx;
            }

            // Increment running Y totals.
            ycorrtot += ycorr;
            zcorrtoty += zcorry;
        }

        // Get mother volume for envelope.
        Volume world = lcdd.pickMotherVolume(this);

        // Place the top section.
        Position mpostop = new Position(momVol.getVolumeName() + "_top_pos", 0, my + beamgap, dface + mz);
        // Rotation mrottop = new Rotation(momVol.getVolumeName() + "_top_rot", 0, -Math.PI/2, (3*Math.PI)/2);
        Rotation mrottop = new Rotation(momVol.getVolumeName() + "_top_rot", 0, 0, 0);
        define.addPosition(mpostop);
        define.addRotation(mrottop);
        PhysVol topSide = new PhysVol(momVol, world, mpostop, mrottop);
        topSide.addPhysVolID("system", this.getSystemID());
        topSide.addPhysVolID("side", 1);

        // Place the bottom section.
        Position mposbot = new Position(momVol.getVolumeName() + "_bot_pos", 0, -my - beamgap, dface + mz);
        Rotation mrotbot = new Rotation(momVol.getVolumeName() + "_bot_rot", 0, 0, Math.PI);
        define.addPosition(mposbot);
        define.addRotation(mrotbot);
        PhysVol botSide = new PhysVol(momVol, world, mposbot, mrotbot);
        botSide.addPhysVolID("system", this.getSystemID());
        botSide.addPhysVolID("side", -1);

        // Make the section box invisible.
        momVol.setVisAttributes(lcdd.getVisAttributes("InvisibleWithDaughters"));
    }

    public boolean isCalorimeter() {
        return true;
    }
}