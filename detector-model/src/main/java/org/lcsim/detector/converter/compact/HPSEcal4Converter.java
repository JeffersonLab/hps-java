package org.lcsim.detector.converter.compact;

import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalCrystalPosition;
import org.hps.conditions.ecal.EcalCrystalPosition.EcalCrystalPositionCollection;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.detector.ecal.EcalCrystal;
import org.hps.detector.ecal.Geant4Position;
import org.hps.detector.ecal.HPSEcalDetectorElement;
import org.hps.detector.ecal.Transformations;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.RotationGeant;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.PhysicalVolume;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.LogicalVolume;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.material.MaterialStore;
import org.lcsim.detector.solids.Trd;
import org.lcsim.geometry.compact.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.geometry.subdetector.HPSEcal4;

/*
 * 
 * @author annie
 * @author jeremym
 */

public class HPSEcal4Converter extends AbstractSubdetectorConverter {

    private static Logger LOGGER = Logger.getLogger(HPSEcal4Converter.class.getPackage().getName());

    private IIdentifierDictionary dict;
    private IIdentifierHelper helper;

    @Override
    public void convert(final Subdetector subdet, final Detector detector) {

        helper = subdet.getDetectorElement().getIdentifierHelper();
        dict = helper.getIdentifierDictionary();
        final Element dimensions = subdet.getNode().getChild("dimensions");
        double dx1, dx2, dy1, dy2, dz;

        double trTop[] = new double[3];
        double trBot[] = new double[3];
        double rotTop[] = new double[3];
        double rotBot[] = new double[3];
        
        try {
            dx1 = dimensions.getAttribute("x1").getDoubleValue();
            dx2 = dimensions.getAttribute("x2").getDoubleValue();
            dy1 = dimensions.getAttribute("y1").getDoubleValue();
            dy2 = dimensions.getAttribute("y2").getDoubleValue();
            dz = dimensions.getAttribute("z").getDoubleValue();

            Element tra = subdet.getNode().getChild("translations");

            trTop[0] = tra.getAttribute("top_tr_x").getDoubleValue();
            trTop[1] = tra.getAttribute("top_tr_y").getDoubleValue();
            trTop[2] = tra.getAttribute("top_tr_z").getDoubleValue();

            trBot[0] = tra.getAttribute("bot_tr_x").getDoubleValue();
            trBot[1] = tra.getAttribute("bot_tr_y").getDoubleValue();
            trBot[2] = tra.getAttribute("bot_tr_z").getDoubleValue();

            Element rota = subdet.getNode().getChild("rotations");

            rotTop[0] = rota.getAttribute("top_rot_alpha").getDoubleValue();
            rotTop[1] = rota.getAttribute("top_rot_beta").getDoubleValue();
            rotTop[2] = rota.getAttribute("top_rot_gamma").getDoubleValue();

            rotBot[0] = rota.getAttribute("bot_rot_alpha").getDoubleValue();
            rotBot[1] = rota.getAttribute("bot_rot_beta").getDoubleValue();
            rotBot[2] = rota.getAttribute("bot_rot_gamma").getDoubleValue();

        } catch (final DataConversionException e) {
            throw new RuntimeException("Error converting HPSEcal4 from XML.", e);
        }
        final Element mat = subdet.getNode().getChild("material");
        final String materialName = mat.getAttributeValue("name");
        final Trd crystalTrap = new Trd("crystal_trap", dx1, dx2, dy1, dy2, dz);
        final ILogicalVolume crystalLogVol = new LogicalVolume("crystal_volume", crystalTrap, MaterialStore
                .getInstance().get(materialName));
        Transformations transTop = new Transformations(trTop, rotTop);
        Transformations transBot = new Transformations(trBot, rotBot);
        final String baseName = subdet.getName() + "_crystal";
        final ILogicalVolume mom = detector.getWorldVolume().getLogicalVolume();
        
        // Get database conditions.
        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        EcalCrystalPositionCollection positions = mgr.getCachedConditions(EcalCrystalPositionCollection.class,
                "ecal_crystal_positions").getCachedData();
        EcalChannelCollection channels = mgr.getCachedConditions(EcalChannelCollection.class,
                "ecal_channels").getCachedData();
        
        // Loop over crystal positions.
        for (EcalCrystalPosition position : positions) {

            // Get channel info.
            int channelId = position.getChannelId();
            EcalChannel channel = channels.findChannel((long) channelId);
            int iy = channel.getY();
            int ix = channel.getX();

            // z axis rotation parameter of the whole module
            double zrotCry;
            Transformations trans;
            if (iy > 0) {
                zrotCry = rotTop[0];
                trans = transTop;
            } else {
                zrotCry = rotBot[0];
                trans = transBot;
            }
            
            Geant4Position g4pos = trans.transformToGeant4(position, channel);
            double[] centerxyz = g4pos.getCenterArr();
            double[] thetaxyz = g4pos.getTaitBryanAngles();
            
            final ITranslation3D iposBot = new Translation3D(centerxyz[0], centerxyz[1], centerxyz[2]);
            final IRotation3D irotBot = new RotationGeant(thetaxyz[0], thetaxyz[1], thetaxyz[2] - zrotCry);

            // Place the crystal.
            final IPhysicalVolume CrystalPlacement = new PhysicalVolume(
                    new Transform3D(iposBot, irotBot), 
                    baseName + channelId, 
                    crystalLogVol, 
                    mom, 
                    channelId);
            
            // Create the DE pointing to the crystal vol.
            this.createDetectorElement(detector, subdet, CrystalPlacement, ix, iy);
        }       
    }

    /**
     * Create a DetectorElement for an ECal crystal.
     *
     * @param detector The full detector.
     * @param subdet The sub detector.
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
        return HPSEcal4.class;
    }

    @Override
    public IDetectorElement makeSubdetectorDetectorElement(final Detector detector, final Subdetector subdetector) {
        final IDetectorElement subdetectorDE = new HPSEcalDetectorElement(subdetector.getName(),
                detector.getDetectorElement());
        subdetector.setDetectorElement(subdetectorDE);
        return subdetectorDE;
    }
}
