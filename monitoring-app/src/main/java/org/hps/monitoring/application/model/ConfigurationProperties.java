package org.hps.monitoring.application.model;

import java.util.Map.Entry;
import java.util.logging.Level;

import org.hps.monitoring.application.model.PropertyTypes.BooleanProperty;
import org.hps.monitoring.application.model.PropertyTypes.IntegerProperty;
import org.hps.monitoring.application.model.PropertyTypes.StringProperty;
import org.hps.record.enums.DataSourceType;

public class ConfigurationProperties extends AbstractProperties {
                   
    public ConfigurationProperties() {
        addDefaultProperties();
    }
    
    public void clear() {
        this.properties.clear();
    }
    
    public final void addDefaultProperties() {
        add(new BooleanProperty("Blocking", "ET station IO blocking", false));
        add(new IntegerProperty("ChunkSize", "ET read chunk size", 1));
        add(new StringProperty("ConditionsTag", "Conditions tag", null));
        add(new StringProperty("DataSourcePath", "Path to data source", null));
        add(new StringProperty("DetectorAlias", "Path to data source", null));
        add(new StringProperty("DetectorName", "Detector name", "HPS-EngRun2015-Nominal-v5-0-fieldmap"));
        add(new BooleanProperty("DisconnectOnEndRun", "Disconnect at end of run", true));
        add(new BooleanProperty("DisconnectOnError", "Disconnect on errors", true));
        add(new StringProperty("EtName", "Name of ET buffer file", "ETBuffer"));
        add(new StringProperty("EventBuilderClassName", "Name of event builder class", "org.hps.evio.LCSimEngRunEventBuilder"));
        add(new BooleanProperty("FreezeConditions", "Freeze detector conditions after init", false));
        add(new StringProperty("Host", "Host name of ET server", "localhost"));
        add(new StringProperty("LogFileName", "Name of log file", "monitoring.log"));
        add(new StringProperty("LogLevel", "Default log level", "ALL"));
        add(new StringProperty("LogLevelFilter", "Default log filtering level", "ALL"));
        add(new BooleanProperty("LogToFile", "Log to an external file instead of terminal", false));
        add(new IntegerProperty("MaxEvents", "Maximum events after session will terminate", -1));
        add(new IntegerProperty("MaxRecentFiles", "Maximum number of recent files to store", 10));
        add(new BooleanProperty("PlotPopup", "Enable pop-up of plots", true));
        add(new IntegerProperty("Port", "ET server port", 11111));
        add(new IntegerProperty("Prescale", "ET event prescaling", 1));
        add(new StringProperty("ProcessingStage", "Processing stages to execute", "LCIO"));
        add(new IntegerProperty("QueueSize", "ET queue size", 0));
        add(new StringProperty("RecentFiles", "List of recent files", null));
        add(new StringProperty("StationName", "ET station name", "MY_STATION"));
        add(new IntegerProperty("StationPosition", "ET station position", 1));
        add(new StringProperty("SteeringFile", "LCSim steering file path", null));
        add(new StringProperty("SteeringResource", "LCSim steering file path", "org/hps/steering/monitoring/DummyMonitoring.lcsim"));
        add(new StringProperty("SteeringType", "LCSim steering type (file or resource)", "RESOURCE"));
        add(new IntegerProperty("UserRunNumber", "User run number", null));
        add(new BooleanProperty("Verbose", "Verbose setting of ET system", false));
        add(new StringProperty("WaitMode", "Wait mode of ET client", "TIMED"));
        add(new IntegerProperty("WaitTime", "Max wait time of ET client", 1000000000));
                
        add(new StringProperty("Test", "Test property", "herp derp"));
    }
    
    public void firePropertiesChanged() {
        for (Entry<String, AbstractProperty<?>> entry : this.properties.entrySet()) {
            entry.getValue().firePropertyChanged();
        }
    }
    
    public DataSourceType getDataSourceType() {
        return DataSourceType.valueOf(this.get("DataSourceType").getValue().toString());
    }
    
    public Level getLogLevel() {
        return Level.parse(this.get("LogLevel").getValue().toString());
    }
             
}
