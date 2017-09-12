package org.lcsim.geometry.compact.converter;

import java.util.List;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jdom.Element;

/**
 * Class containing the final translation and rotation correction from alignment.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class AlignmentCorrection {

    private Element node = null;
    private Rotation rotation = null;
    private Hep3Vector translation = null;
    private List<MilleParameter> milleParameters = null;

    public AlignmentCorrection(double x, double y, double z, double rot_x, double rot_y, double rot_z) {
        setTranslation(x, y, z);
        setRotation(rot_x, rot_y, rot_z);
    }

    public AlignmentCorrection() {
    }

    public Rotation getRotation() {
        return rotation;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public void setRotation(double rot_x, double rot_y, double rot_z) {
        Rotation rx = new Rotation(new Vector3D(1, 0, 0), rot_x);
        Rotation ry = new Rotation(new Vector3D(0, 1, 0), rot_y);
        Rotation rz = new Rotation(new Vector3D(0, 0, 1), rot_z);
        // Build full rotation
        Rotation rzyx = rz.applyTo(ry.applyTo(rx));
        setRotation(rzyx);
    }

    public Hep3Vector getTranslation() {
        return translation;
    }

    public void setTranslation(Hep3Vector translation) {
        this.translation = translation;
    }

    public void setTranslation(double x, double y, double z) {
        setTranslation(new BasicHep3Vector(x, y, z));
    }

    public void setMilleParameters(List<MilleParameter> params) {
        milleParameters = params;
    }

    public List<MilleParameter> getMilleParameters() {
        return milleParameters;
    }

    public void setNode(Element node) {
        this.node = node;
    }

    public Element getNode() {
        return node;
    }

}