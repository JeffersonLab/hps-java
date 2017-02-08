package org.lcsim.geometry.compact.converter.lcdd;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.geometry.compact.converter.lcdd.util.Box;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.Material;
import org.lcsim.geometry.compact.converter.lcdd.util.PhysVol;
import org.lcsim.geometry.compact.converter.lcdd.util.Position;
import org.lcsim.geometry.compact.converter.lcdd.util.Rotation;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;

public class HPSMuonCalorimeter extends LCDDSubdetector {

    HPSMuonCalorimeter(Element node) throws JDOMException {
        super(node);
    }

    void addToLCDD(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        String name = node.getAttributeValue("name");
        int id = node.getAttribute("id").getIntValue();
        Volume mother = lcdd.pickMotherVolume(this);

        for (Object layerObject : node.getChildren("layer")) {

            Element layer = (Element) layerObject;
            int layerId = layer.getAttribute("id").getIntValue();

            int slice = 1;
            for (Object boxObject : layer.getChildren("box")) {

                Element element = (Element) boxObject;

                double x, y, z, px, py, pz, rx, ry, rz;
                x = y = z = px = py = pz = rx = ry = rz = 0.;

                if (element.getAttribute("x") != null) {
                    x = element.getAttribute("x").getDoubleValue();
                } else {
                    throw new RuntimeException("x is required.");
                }
                if (element.getAttribute("y") != null) {
                    y = element.getAttribute("y").getDoubleValue();
                } else {
                    throw new RuntimeException("y is required.");
                }
                if (element.getAttribute("z") != null) {
                    z = element.getAttribute("z").getDoubleValue();
                } else {
                    throw new RuntimeException("z is required.");
                }

                if (element.getAttribute("px") != null)
                    px = element.getAttribute("px").getDoubleValue();
                if (element.getAttribute("py") != null)
                    py = element.getAttribute("py").getDoubleValue();
                if (element.getAttribute("pz") != null)
                    pz = element.getAttribute("pz").getDoubleValue();

                if (element.getAttribute("rx") != null)
                    rx = element.getAttribute("rx").getDoubleValue();
                if (element.getAttribute("ry") != null)
                    ry = element.getAttribute("ry").getDoubleValue();
                if (element.getAttribute("rz") != null)
                    rz = element.getAttribute("rz").getDoubleValue();

                String materialString = element.getAttributeValue("material");
                Material material = lcdd.getMaterial(materialString);

                String shapeBaseName = name + "_layer" + layerId + "_sublayer" + slice;

                Box box = new Box(shapeBaseName + "_box", x, y, z);
                lcdd.add(box);

                Position pos = new Position(shapeBaseName + "_pos", px, py, pz);
                lcdd.add(pos);

                Rotation rot = new Rotation(shapeBaseName + "_rot", rx, ry, rz);
                lcdd.add(rot);

                Volume vol = new Volume(shapeBaseName + "_vol", box, material);

                boolean sensitive = false;
                if (element.getAttribute("sensitive") != null)
                    sensitive = element.getAttribute("sensitive").getBooleanValue();

                if (sensitive) {
                    vol.setSensitiveDetector(sens);
                }

                lcdd.add(vol);

                PhysVol physVol = new PhysVol(vol, mother, pos, rot);
                physVol.addPhysVolID("layer", layerId);
                physVol.addPhysVolID("slice", slice);
                if (py >= 0) {
                    physVol.addPhysVolID("side", 1);
                } else {
                    physVol.addPhysVolID("side", -1);
                }
                physVol.addPhysVolID("system", id);

                ++slice;
            }
        }

    }

    public boolean isCalorimeter() {
        return true;
    }
}