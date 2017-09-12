package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.List;

import org.jdom.DataConversionException;
import org.jdom.Element;

public abstract class CompactSurveyVolume extends SurveyVolume {

    public static final String xmlTagNameTop = "SurveyVolumes";
    public static final String xmlTagName = "SurveyVolume";

    Element node = null;

    public CompactSurveyVolume(String name, SurveyVolume m, AlignmentCorrection alignmentCorrection, Element node2) {
        super(name, m, alignmentCorrection);
        node = node2;
    }

    public CompactSurveyVolume(String name, SurveyVolume m, AlignmentCorrection alignmentCorrection, SurveyVolume ref,
            Element node2) {
        super(name, m, alignmentCorrection, ref);
        node = node2;
    }

    /**
     * Extract survey positions from xml description
     */
    protected void setPos() {

        if (debug)
            System.out.printf("%s: getSurveyPosFromCompact for %s from %s\n", getClass().getSimpleName(), getName(),
                    node.getAttributeValue("name"));

        Element eNameTop = node.getChild(xmlTagNameTop);
        if (eNameTop == null) {
            throw new RuntimeException("no eName for " + xmlTagNameTop + " found in compact file");
        }

        List<Element> eNames = eNameTop.getChildren(xmlTagName);
        if (eNames == null) {
            throw new RuntimeException("no eNames for " + xmlTagName + " found in compact file");
        }

        for (Element eName : eNames) {

            if (eName.getAttributeValue("name").compareTo(getName()) == 0) {

                Element eCoord = eName.getChild("SurveyPos");

                if (eCoord == null) {
                    throw new RuntimeException("no eCoord for " + getName() + " found in compact file");
                }

                // Element eOrigin = eCoord.getChild("origin");
                //
                // if(eOrigin==null) {
                // throw new RuntimeException("no eOrigin for " + getName() + " found in compact file");
                // }

                // origin = null;
                // try {
                // double x,y,z;
                // x= eOrigin.getAttribute("x").getDoubleValue();
                // y= eOrigin.getAttribute("y").getDoubleValue();
                // z= eOrigin.getAttribute("z").getDoubleValue();
                // origin = new BasicHep3Vector(x, y, z);
                // } catch (DataConversionException e) {
                // e.printStackTrace();
                // }

                List<Element> eCoordComponents = eCoord.getChildren("point");

                for (Element eUnitVec : eCoordComponents) {
                    try {
                        double x, y, z;
                        if (eUnitVec.getAttributeValue("name").compareTo("ball") == 0) {
                            x = eUnitVec.getAttribute("x").getDoubleValue();
                            y = eUnitVec.getAttribute("y").getDoubleValue();
                            z = eUnitVec.getAttribute("z").getDoubleValue();
                            setBallPos(x, y, z);
                        } else if (eUnitVec.getAttributeValue("name").compareTo("vee") == 0) {
                            x = eUnitVec.getAttribute("x").getDoubleValue();
                            y = eUnitVec.getAttribute("y").getDoubleValue();
                            z = eUnitVec.getAttribute("z").getDoubleValue();
                            setVeePos(x, y, z);
                        } else if (eUnitVec.getAttributeValue("name").compareTo("flat") == 0) {
                            x = eUnitVec.getAttribute("x").getDoubleValue();
                            y = eUnitVec.getAttribute("y").getDoubleValue();
                            z = eUnitVec.getAttribute("z").getDoubleValue();
                            setFlatPos(x, y, z);
                        } else {
                            throw new RuntimeException("eUnitVec name " + eUnitVec.getAttributeValue("name")
                                    + " is ill-defined for " + getName() + " found in compact file");
                        }
                    } catch (DataConversionException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
        if (debug) {
            System.out.printf("%s: Extracted these survey constants from compact\n", this.getClass().getSimpleName());
            System.out.printf("%s: ball %s \n", this.getClass().getSimpleName(), getBallPos().toString());
            System.out.printf("%s: vee  %s \n", this.getClass().getSimpleName(), getVeePos().toString());
            System.out.printf("%s: flat %s \n", this.getClass().getSimpleName(), getFlatPos().toString());
        }

    }

    /**
     * Extract coordinate system from xml description
     */
    private void getCoordFromCompact() {

        if (debug)
            System.out.printf("%s: getCoordFromCompact for %s from %s\n", getClass().getSimpleName(), getName(),
                    node.getAttributeValue("name"));

        if (1 == 1)
            throw new UnsupportedOperationException(
                    "Need to work on interface of a new coordinate system and the ball, vee, flat procedure to build coord system!");

        Element eNameTop = node.getChild(xmlTagNameTop);
        if (eNameTop == null) {
            throw new RuntimeException("no eName for " + xmlTagNameTop + " found in compact file");
        }

        List<Element> eNames = eNameTop.getChildren(xmlTagName);
        if (eNames == null) {
            throw new RuntimeException("no eNames for " + xmlTagName + " found in compact file");
        }

        for (Element eName : eNames) {

            if (eName.getAttributeValue("name").compareTo(getName()) == 0) {

                Element eCoord = eName.getChild("SurveyCoord");

                if (eCoord == null) {
                    throw new RuntimeException("no eCoord for " + getName() + " found in compact file");
                }

                Element eOrigin = eCoord.getChild("origin");

                if (eOrigin == null) {
                    throw new RuntimeException("no eOrigin for " + getName() + " found in compact file");
                }

                Hep3Vector coord_org = null;
                Hep3Vector coord_unit_u = null;
                Hep3Vector coord_unit_v = null;
                Hep3Vector coord_unit_w = null;

                double x, y, z;
                try {
                    x = eOrigin.getAttribute("x").getDoubleValue();
                    y = eOrigin.getAttribute("y").getDoubleValue();
                    z = eOrigin.getAttribute("z").getDoubleValue();
                    coord_org = new BasicHep3Vector(x, y, z);
                } catch (DataConversionException e) {
                    e.printStackTrace();
                }

                List<Element> eCoordComponents = eCoord.getChildren("unitVec");

                for (Element eUnitVec : eCoordComponents) {
                    try {
                        double ux, uy, uz;
                        double vx, vy, vz;
                        double wx, wy, wz;
                        if (eUnitVec.getAttributeValue("name").compareTo("u") == 0) {
                            ux = eUnitVec.getAttribute("x").getDoubleValue();
                            uy = eUnitVec.getAttribute("y").getDoubleValue();
                            uz = eUnitVec.getAttribute("z").getDoubleValue();
                            coord_unit_u = new BasicHep3Vector(ux, uy, uz);
                        } else if (eUnitVec.getAttributeValue("name").compareTo("v") == 0) {
                            vx = eUnitVec.getAttribute("x").getDoubleValue();
                            vy = eUnitVec.getAttribute("y").getDoubleValue();
                            vz = eUnitVec.getAttribute("z").getDoubleValue();
                            coord_unit_v = new BasicHep3Vector(vx, vy, vz);
                        } else if (eUnitVec.getAttributeValue("name").compareTo("w") == 0) {
                            wx = eUnitVec.getAttribute("x").getDoubleValue();
                            wy = eUnitVec.getAttribute("y").getDoubleValue();
                            wz = eUnitVec.getAttribute("z").getDoubleValue();
                            coord_unit_w = new BasicHep3Vector(wx, wy, wz);
                        } else {
                            throw new RuntimeException("eUnitVec name " + eUnitVec.getAttributeValue("name")
                                    + " is ill-defined for " + getName() + " found in compact file");
                        }
                    } catch (DataConversionException e) {
                        e.printStackTrace();
                    }
                }

                throw new RuntimeException("need to work on this!");
                // SurveyCoordinateSystem coord = new SurveyCoordinateSystem(coord_org, coord_unit_u, coord_unit_v, coord_unit_w);
                // setCoord(coord);

            }
        }
        if (debug) {
            System.out.printf("%s: found coord system\n%s\n", this.getClass().getSimpleName(), getCoord().toString());
        }

    }

}
