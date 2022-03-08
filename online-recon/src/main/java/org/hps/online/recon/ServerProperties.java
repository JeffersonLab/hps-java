package org.hps.online.recon;

import org.hps.online.recon.properties.BooleanProperty;
import org.hps.online.recon.properties.IntegerProperty;
import org.hps.online.recon.properties.LongProperty;
import org.hps.online.recon.properties.Property;
import org.hps.online.recon.properties.PropertyStore;
import org.hps.online.recon.properties.StringProperty;

public class ServerProperties extends PropertyStore {

    ServerProperties() {
        this.add(new Property<?>[] {
                new StringProperty ( "server.host", "Server host name", "localhost", true),
                new IntegerProperty( "server.port", "Server port number", 22222, true),
                new IntegerProperty( "server.start_id", "Starting station ID", 1, true),
                new StringProperty ( "server.work_dir", "Work directory", System.getProperty("user.dir"), true),
                new StringProperty ( "server.station_name", "Base name of station", "HPS_RECON", true),
                new IntegerProperty( "server.client_thread_pool_size", "Number of threads in pool for client requests", 10, true),
                new IntegerProperty( "server.task_thread_pool_size", "Number of threads in pool for server tasks", 3, true),
                new IntegerProperty( "server.remote_aida_port_start", "Starting port number for remote AIDA connections", 5000, true),
                new IntegerProperty( "server.station_monitor_interval", "Interval for station monitor (millis)", 500, true),
                new StringProperty(  "server.station_config_file_name", "File name for station properties", "station.properties", true),
                new LongProperty   ( "agg.interval", "Plot aggregation interval (millis)", 5000L, true),
                new IntegerProperty( "agg.port", "Port for AIDA aggregator", 3001, true),
                new StringProperty ( "agg.name", "Server name for aggreagtor", "HPSRecon", true),
                new StringProperty ( "agg.remotes_dir", "AIDA dir for remote trees", "/remotes", true),
                new StringProperty ( "agg.combined_dir", "AIDA dir for aggregate tree", "/combined", true),
                new IntegerProperty( "agg.cloud_bins", "Number of bins in clouds when converting from histograms", 100, true),
                new BooleanProperty( "agg.duplex", "True for duplex RMI server connection", true, true),
                new IntegerProperty( "notifier.port", "Port number for plot update notification service", 8887, true),
                new StringProperty ( "et.buffer", "Name of ET buffer file", "/tmp/ETBuffer", true),
                new StringProperty ( "et.host", "Host for ET connection", "localhost",  true),
                new IntegerProperty( "et.port", "Port for ET connection", 11111, true),
                new IntegerProperty( "et.connection_attempts", "Max ET connection attempts", 10, false)
        });
    }
}
