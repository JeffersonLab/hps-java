package org.hps.monitoring.record;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.source.RecordSource;
import org.hps.evio.LCSimEventBuilder;
import org.hps.monitoring.enums.DataSourceType;
import org.hps.monitoring.record.EventProcessingChain.ProcessingStage;
import org.hps.monitoring.record.composite.CompositeRecordProcessor;
import org.hps.monitoring.record.etevent.EtConnection;
import org.hps.monitoring.record.etevent.EtEventProcessor;
import org.hps.monitoring.record.evio.EvioEventProcessor;
import org.lcsim.util.Driver;

/**
 * A configuration object for the {@link EventProcessingChain}.
 * The instance variables are readable within this package, but
 * must be set through the public set methods when used
 * outside of it, e.g. from MonitoringApplication.
 */
public class EventProcessingConfiguration {
        
    boolean stopOnErrors = true;
    boolean stopOnEndRun = true;

    DataSourceType sourceType = DataSourceType.ET_SERVER;
    ProcessingStage processingStage = ProcessingStage.LCIO;
    
    String filePath = null;
    EtConnection connection = null;
    RecordSource recordSource = null;
    LCSimEventBuilder eventBuilder = null;
    String detectorName = null;
    
    List<EvioEventProcessor> evioProcessors = new ArrayList<EvioEventProcessor>();
    List<Driver> drivers = new ArrayList<Driver>();
    List<CompositeRecordProcessor> compositeProcessors = new ArrayList<CompositeRecordProcessor>();
    List<EtEventProcessor> etProcessors = new ArrayList<EtEventProcessor>();
                 
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public void setEtConnection(EtConnection connection) {
        this.connection = connection;
    }
    
    public void setDataSourceType(DataSourceType sourceType) {
        this.sourceType = sourceType;
    }
    
    public void setProcessingStage(ProcessingStage processingStage) {
        this.processingStage = processingStage;
    }
    
    public void setRecordSource(RecordSource recordSource) {
        this.recordSource = recordSource;
    }
    
    public void setLCSimEventBuild(LCSimEventBuilder eventBuilder) {
        this.eventBuilder = eventBuilder;
    }
    
    public void setDetectorName(String detectorName) {
        this.detectorName = detectorName;
    }
    
    public void setStopOnErrors(boolean stopOnErrors) {
        this.stopOnErrors = stopOnErrors;
    }
    
    public void setStopOnEndRun(boolean stopOnEndRun) {
        this.stopOnEndRun = stopOnEndRun;
    }
    
    public void add(EtEventProcessor processor) {
        etProcessors.add(processor);
    }
    
    public void add(EvioEventProcessor processor) {
        evioProcessors.add(processor);
    }
    
    public void add(Driver processor) {
        drivers.add(processor);
    }
    
    public void add(CompositeRecordProcessor processor) {
        compositeProcessors.add(processor);
    }
}
