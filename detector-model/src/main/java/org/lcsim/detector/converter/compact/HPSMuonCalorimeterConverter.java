package org.lcsim.detector.converter.compact;

import org.jdom.DataConversionException;
import org.jdom.Element;
import org.lcsim.detector.DetectorElement;
import org.lcsim.detector.ILogicalVolume;
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
import org.lcsim.detector.material.IMaterial;
import org.lcsim.detector.material.MaterialStore;
import org.lcsim.detector.solids.Box;
import org.lcsim.geometry.compact.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.geometry.subdetector.HPSMuonCalorimeter;

public class HPSMuonCalorimeterConverter extends AbstractSubdetectorConverter {

    public Class getSubdetectorType() {
        return HPSMuonCalorimeter.class;
    }

    public void convert(Subdetector subdet, Detector detector) {

        IIdentifierHelper helper = subdet.getDetectorElement().getIdentifierHelper();
        IIdentifierDictionary dict = helper.getIdentifierDictionary();

        try {
            Element node = subdet.getNode();
            String name = node.getAttributeValue("name");
            ILogicalVolume mother = detector.getWorldVolume().getLogicalVolume();

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

                    String materialName = element.getAttributeValue("material");
                    IMaterial material = MaterialStore.getInstance().get(materialName);

                    String shapeBaseName = name + "_layer" + layerId + "_sublayer" + slice;

                    Box box = new Box(shapeBaseName + "_box", x / 2, y / 2, z / 2);

                    ITranslation3D pos = new Translation3D(px, py, pz);
                    IRotation3D rot = new RotationGeant(rx, ry, rz);
                    ILogicalVolume vol = new LogicalVolume(shapeBaseName + "_vol", box, material);

                    String physVolName = shapeBaseName + "_pv";
                    new PhysicalVolume(new Transform3D(pos, rot), physVolName, vol, mother, 0);

                    final IExpandedIdentifier expId = new ExpandedIdentifier(helper.getIdentifierDictionary()
                            .getNumberOfFields());
                    expId.setValue(dict.getFieldIndex("system"), subdet.getSystemID());
                    expId.setValue(dict.getFieldIndex("layer"), layerId);
                    expId.setValue(dict.getFieldIndex("slice"), slice);
                    int side = 1;
                    if (py < 0) {
                        side = -1;
                    }
                    expId.setValue(dict.getFieldIndex("side"), side);
                    final IIdentifier id = helper.pack(expId);
                    new DetectorElement(shapeBaseName, subdet.getDetectorElement(), "/" + physVolName, id);

                    ++slice;
                }
            }
        } catch (DataConversionException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isCalorimeter() {
        return true;
    }
}