package org.hps.online.recon.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.jlab.coda.et.EtAttachment;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.enums.Mode;
import org.jlab.coda.et.enums.Modify;

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

    public static void main(String[] args) throws Exception {

        // Get number and name for station
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

        // Event loop
        while (true) {
            EtEvent[] events = null;
            try {
                log.info("Waiting for events...");
                events = sys.getEvents(att, MODE, MODIFY, WAIT_TIME, CHUNK);
                log.info("Got events!");
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
            for (EtEvent event : events) {
                log.info("Got ET event ID: " + event.getId());
                gotEvents.add(event.getId());
            }
        }

        // Print event IDs that were processed by this station and cleanup the ET system
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("Got event IDs: " + Arrays.toString(gotEvents.toArray()));
                log.info("Cleaning up...");
                try {
                    sys.detach(att);
                    sys.removeStation(stat);
                    sys.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        log.info("Goodbye!");
    }
}
