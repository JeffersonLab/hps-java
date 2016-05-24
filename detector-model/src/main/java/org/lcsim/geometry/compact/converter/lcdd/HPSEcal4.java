package org.lcsim.geometry.compact.converter.lcdd;

import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.ecal.Geant4Position;
import org.hps.detector.ecal.Transformations;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.detector.converter.compact.HPSEcal4Converter;
import org.lcsim.geometry.compact.converter.lcdd.util.Define;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.PhysVol;
import org.lcsim.geometry.compact.converter.lcdd.util.Position;
import org.lcsim.geometry.compact.converter.lcdd.util.Rotation;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Trapezoid;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;
import org.hps.conditions.ecal.EcalCrystalPosition;
import org.hps.conditions.ecal.EcalCrystalPosition.EcalCrystalPositionCollection;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;

/**
 * Convert a compact subdetector to LCDD output using ECal database conditions
 * for the crystal positions.
 * 
 * @author annie
 * @author jeremym
 */
public class HPSEcal4 extends LCDDSubdetector {

    private static Logger LOGGER = Logger.getLogger(HPSEcal4Converter.class.getPackage().getName());

    HPSEcal4(Element node) throws JDOMException {
        super(node);
    }

    void addToLCDD(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (sens == null) {
            throw new RuntimeException("SensitiveDetector parameter points to null.");
        }
        
        int system = this.getSystemID();
        Element mat = node.getChild("material");
        String materialName = mat.getAttributeValue("name");
        
        Element dimensions = node.getChild("dimensions");
        double dx1 = dimensions.getAttribute("x1").getDoubleValue();
        double dx2 = dimensions.getAttribute("x2").getDoubleValue();
        double dy1 = dimensions.getAttribute("y1").getDoubleValue();
        double dy2 = dimensions.getAttribute("y2").getDoubleValue();
        double dz = dimensions.getAttribute("z").getDoubleValue();
        
        Element tra = node.getChild("translations");
        double trTop[] = new double[3];
        trTop[0] = tra.getAttribute("top_tr_x").getDoubleValue();
        trTop[1] = tra.getAttribute("top_tr_y").getDoubleValue();
        trTop[2] = tra.getAttribute("top_tr_z").getDoubleValue();        
        double trBot[] = new double[3];
        trBot[0] = tra.getAttribute("bot_tr_x").getDoubleValue();
        trBot[1] = tra.getAttribute("bot_tr_y").getDoubleValue();
        trBot[2] = tra.getAttribute("bot_tr_z").getDoubleValue();
        
        Element rota = node.getChild("rotations");
        double rotTop[] = new double[3];
        rotTop[0] = rota.getAttribute("top_rot_alpha").getDoubleValue();
        rotTop[1] = rota.getAttribute("top_rot_beta").getDoubleValue();
        rotTop[2] = rota.getAttribute("top_rot_gamma").getDoubleValue();
        double rotBot[] = new double[3];
        rotBot[0] = rota.getAttribute("bot_rot_alpha").getDoubleValue();
        rotBot[1] = rota.getAttribute("bot_rot_beta").getDoubleValue();
        rotBot[2] = rota.getAttribute("bot_rot_gamma").getDoubleValue();
        
        Trapezoid crystalTrap = new Trapezoid("crystal_trap", dx1, dx2, dy1, dy2, dz);
        Volume crystalLogVol = new Volume("crystal_volume", crystalTrap, lcdd.getMaterial(materialName));
        crystalLogVol.setSensitiveDetector(sens);
        setVisAttributes(lcdd, this.getNode(), crystalLogVol);
        lcdd.add(crystalTrap);
        lcdd.add(crystalLogVol);
        Volume world = lcdd.pickMotherVolume(this);
        Define define = lcdd.getDefine();
        
        Transformations transTop = new Transformations(trTop, rotTop);
        Transformations transBot = new Transformations(trBot, rotBot);
        
        // Get database conditions.
        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        EcalCrystalPositionCollection positions = mgr.getCachedConditions(EcalCrystalPositionCollection.class,
                "ecal_crystal_positions").getCachedData();
        EcalChannelCollection channels = mgr.getCachedConditions(EcalChannelCollection.class,
                "ecal_channels").getCachedData();
        
        LOGGER.info("Read " + positions.size() + " ECal crystal positions from conditions database.");
        
        // Loop over crystal positions.
        for (EcalCrystalPosition position : positions) {
            
            int channelId = position.getChannelId();
            EcalChannel channel = channels.findChannel((long) channelId);
            int iy = channel.getY();
            int ix = channel.getX();                                    
            String baseName = "crystal_" + ix + "_" + iy;
            
            // z axis rotation parameter of the whole module
            double zrot_cry = rotBot[0];
            Transformations trans = transBot;
            if (iy > 0) {
                // z axis rotation parameter of entire module
                zrot_cry = rotTop[0];
                trans = transTop;
            }
            
            Geant4Position g4pos = trans.transformToGeant4(position, channel);
            double[] centerxyz = g4pos.getCenterArr();
            double[] thetaxyz = g4pos.getTaitBryanAngles();

            // Transform of crystal.
            Position ipos = new Position(baseName + "_pos", centerxyz[0], centerxyz[1], centerxyz[2]);
            Rotation irot = new Rotation(baseName + "_rot", thetaxyz[0], thetaxyz[1], thetaxyz[2] - zrot_cry);
            define.addPosition(ipos);
            define.addRotation(irot);

            // Place the crystal with volume IDs.
            PhysVol CrystalPlacementBot = new PhysVol(crystalLogVol, world, ipos, irot);
            CrystalPlacementBot.addPhysVolID("system", system);
            CrystalPlacementBot.addPhysVolID("ix", ix);
            CrystalPlacementBot.addPhysVolID("iy", iy);
            
            // Debug print all info about physvol.
            LOGGER.fine("Created physvol for crystal (" + ix + ", " + iy + "); channel ID " + channelId 
                    + "; pos (" + centerxyz[0] + ", " + centerxyz[1] + ", " + centerxyz[2] 
                    + "); rot (" + thetaxyz[0] + ", " + thetaxyz[1] + ", " + (thetaxyz[2] - zrot_cry) + ")");
        }      
    }
           
    public boolean isCalorimeter() {
        return true;
    }
}
