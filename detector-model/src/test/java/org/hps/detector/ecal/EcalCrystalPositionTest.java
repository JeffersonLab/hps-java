package org.hps.detector.ecal;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalCrystalPosition;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalCrystalPosition.EcalCrystalPositionCollection;


/**
 * Simplistic test of loading ECal crystal positions from the conditions database
 * and associating to an ecal channel object.
 * <p>
 * This test must go into the <i>detector-model</i> module because the detector
 * converters are not available in the <i>conditions</i> package. 
 * 
 * @author jeremym
 */
public class EcalCrystalPositionTest extends TestCase {
    
    public void testEcalCrystalPositions() throws Exception {
        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        mgr.setDetector("HPS-PhysicsRun2016-Nominal-v4-4", 0); /* any run number and detector will work here */
        
        EcalCrystalPositionCollection positions = 
                mgr.getCachedConditions(EcalCrystalPositionCollection.class, "ecal_crystal_positions").getCachedData();
        EcalChannelCollection channels = 
                mgr.getCachedConditions(EcalChannelCollection.class, "ecal_channels").getCachedData();
        
        for (EcalCrystalPosition position : positions) {
            int channelId = position.getChannelId();
            double xback =  position.getBackX();
            double yback = position.getBackY();
            double zback = position.getBackZ();
            double xfront = (double)position.getFrontX();
            double yfront = (double)position.getFrontY();
            double zfront = (double)position.getFrontZ();
            position.getRowId();
            EcalChannel channel = channels.findChannel(channelId);
            System.out.println(channel);
            //System.out.println(position);
            double frontX = position.getFrontX();
            double frontY = position.getFrontY();
            double frontZ = position.getFrontZ();
            double backX = position.getBackX();
            double backY = position.getBackY();
            double backZ = position.getBackZ();
            System.out.println("fx, fy, fz, bx, by, bz = " + frontX + ", " + frontY + ", " + frontZ + ", " +
                    backX + ", " + backY + ", " + backZ);
            System.out.println();
            System.out.println("back "+xback+" "+yback+" "+zback+"    ");
            System.out.println("front "+xfront+" "+yfront+" "+zfront+"    ");
        }
    }
}
