package org.lcsim.hps.monitoring;

import org.jlab.coda.et.EtAttachment;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;

/**
 * Create an EtSystem and EtAttachment from ConnectionParameters.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class EtConnection {

    ConnectionParameters param;
    EtSystem sys;
    EtAttachment att;
    EtStation stat;

    /**
     * Class constructor.
     * @param param The connection parameters.
     * @param sys The ET system.
     * @param att The ET attachment.
     * @param stat The ET station.
     */
    private EtConnection(ConnectionParameters param, EtSystem sys, EtAttachment att, EtStation stat) {
        this.param = param;
        this.sys = sys;
        this.att = att;
        this.stat = stat;
    }

    /**
     * Get the ET system.
     * @return The ET system.
     */
    EtSystem getEtSystem() {
        return sys;
    }

    /**
     * Get the ET attachment.
     * @return The ET attachment.
     */
    EtAttachment getEtAttachment() {
        return att;
    }

    /**
     * Get the ET station. 
     * @return The ET station.
     */
    EtStation getEtStation() {
        return stat;
    }

    /**
     * Get the connection parameters.
     * @return The connection parameters.
     */
    ConnectionParameters getConnectionParameters() {
        return param;
    }

    /**
     * Cleanup the ET connection.
     */
    void cleanup() {
        boolean debug = false;
        try {
            if (debug)
                System.out.println("ET cleanup - sys.detach ...");
            sys.detach(att);
            if (debug)
                System.out.println("ET cleanup - sys.removeStation ...");
            sys.removeStation(stat);
            if (debug)
                System.out.println("ET cleanup - sys.close ...");
            sys.close();
            if (debug)
                System.out.println("ET cleanup - successful");
        }
        catch (Exception e) {
            e.printStackTrace();
        }		
    }

    /**
     * Create an ET connection from connection parameters.
     * @param cn The connection parameters.
     * @return The ET connection.
     */
    static EtConnection createEtConnection(ConnectionParameters cn) {
        try {

            // make a direct connection to ET system's tcp server
            EtSystemOpenConfig config = new EtSystemOpenConfig(cn.etName, cn.host, cn.port);

            // create ET system object with verbose debugging output
            EtSystem sys = new EtSystem(config, EtConstants.debugInfo);
            sys.open();

            // configuration of a new station
            EtStationConfig statConfig = new EtStationConfig();
            statConfig.setFlowMode(cn.flowMode);
            if (!cn.blocking) {
                statConfig.setBlockMode(EtConstants.stationNonBlocking);
                if (cn.qSize > 0) {
                    statConfig.setCue(cn.qSize);
                }
            }
            // Set prescale.
            if (cn.prescale > 0) {
                System.out.println("setting prescale to " + cn.prescale);
                statConfig.setPrescale(cn.prescale);
            }

            // Create the station.
            EtStation stat = sys.createStation(statConfig, cn.statName, cn.position, cn.pposition);

            // attach to new station
            EtAttachment att = sys.attach(stat);

            // Return new connection.
            return new EtConnection(cn, sys, att, stat);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }       
}