package org.hps.online.recon.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.et.EtAttachment;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.enums.Age;
import org.jlab.coda.et.enums.Mode;
import org.jlab.coda.et.enums.Modify;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;

/**
 * Example ET station configured as parallel and round-robin
 */
public class ETStationExample {

    final static String BUFFER    = "/tmp/ETBuffer";
    final static String HOSTNAME  = "localhost";
    final static int    PORT      = 11111;
    final static String STATION   = "STATION";
    final static Mode   MODE      = Mode.SLEEP;
    final static int    WAIT_TIME = 0;
    final static int    CHUNK     = 1;
    final static Modify MODIFY    = Modify.NOTHING;

    final static String detectorName = "HPS-PhysicsRun2016-Pass2";

    public static void main(String[] args) throws Exception {

        // Get number and name for station (number is not used for ordering)
        if (args.length == 0) {
            throw new RuntimeException("Missing station num");
        }
        Integer stationNum = Integer.parseInt(args[0].trim());
        final String stationName = STATION + "_" + stationNum;

        // Setup logger for station
        final Logger log = Logger.getLogger(stationName);
        log.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            public synchronized String format(LogRecord lr) {
                return "[ " + lr.getLoggerName() + " ] " +
                        lr.getMessage() + '\n';
            }
        });
        log.addHandler(handler);

        // Configure the ET system
        final EtSystemOpenConfig etConfig =
                new EtSystemOpenConfig(BUFFER, HOSTNAME, PORT);
        final EtSystem sys = new EtSystem(etConfig, EtConstants.debugInfo);
        sys.open();
        final EtStationConfig stationConfig = new EtStationConfig();
        stationConfig.setFlowMode(EtConstants.stationParallel);
        stationConfig.setBlockMode(EtConstants.stationBlocking);
        stationConfig.setSelectMode(EtConstants.stationSelectRRobin);
        final EtStation stat =
                sys.createStation(stationConfig, stationName, 1, EtConstants.end); // what should position be???
        final EtAttachment att = sys.attach(stat);

        log.info("Initialized station: " + stationName);
        log.info("Station pos: " + sys.getStationPosition(stat));
        log.info("Station parallel pos: " + sys.getStationParallelPosition(stat));
        log.info("Num stations: " + sys.getNumStations());

        final List<Integer> gotEvents = new ArrayList<Integer>();

        // On exit, print event IDs processed by this station and cleanup the ET system
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("Got event ids: " + Arrays.toString(gotEvents.toArray()));
                log.info("Shutting down...");
                try {
                    sys.detach(att);
                    sys.removeStation(stat);
                    sys.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log.info("Goodbye!");
            }
        });

        // Event loop
        while (true) {
            EtEvent[] evts = {};
            try {
                log.info("Waiting for events...");
                evts = sys.getEvents(att, MODE, MODIFY, WAIT_TIME, CHUNK);
                log.info("Got events!");
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (EtEvent et : evts) {

                // Print ET event data
                log.info("ET id: " + et.getId());
                log.info("ET mem size: " + et.getMemSize());
                log.info("ET len: " + et.getLength());
                log.info("ET age: " + Age.getName(et.getAge().ordinal()));
                log.info("ET group: " + et.getGroup());

                // Print EVIO event data
                EvioEvent evio =
                        new EvioReader(et.getDataBuffer()).parseNextEvent();
                log.info("EVIO num: " + evio.getEventNumber());
                log.info("EVIO tot bytes: " + evio.getTotalBytes());
                int eventId[] = EvioEventUtilities.getEventIdData(evio);
                if (eventId != null) {
                    log.info("EVIO id: " + eventId[0]);
                } else {
                    log.info("EVIO id: NOT FOUND");
                }
            }

            // Is this needed???
            //log.info("Putting events...");
            //sys.putEvents(att, evts);
            //log.info("Done putting events!");
        }
    }
}
