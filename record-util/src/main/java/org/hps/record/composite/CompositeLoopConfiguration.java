package org.hps.record.composite;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.source.RecordSource;
import org.hps.evio.LCSimEventBuilder;
import org.hps.record.composite.CompositeRecordProcessor;
import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;
import org.hps.record.et.EtConnection;
import org.hps.record.et.EtEventProcessor;
import org.hps.record.evio.EvioEventProcessor;
import org.lcsim.util.Driver;

/**
 * A configuration object for the {@link ProcessingChain}.
 * The instance variables are readable within this package, but
 * must be set through the public set methods when used
 * outside of it, e.g. from MonitoringApplication.
 */
public class CompositeLoopConfiguration {
        
    boolean stopOnErrors = true;
    boolean stopOnEndRun = true;
    
    int maxRecords = -1;
         
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
                 
    public CompositeLoopConfiguration setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }
    
    public CompositeLoopConfiguration setEtConnection(EtConnection connection) {
        this.connection = connection;
        return this;
    }
    
    public CompositeLoopConfiguration setDataSourceType(DataSourceType sourceType) {
        this.sourceType = sourceType;
        return this;
    }
    
    public CompositeLoopConfiguration setProcessingStage(ProcessingStage processingStage) {
        this.processingStage = processingStage;
        return this;
    }
    
    public CompositeLoopConfiguration setRecordSource(RecordSource recordSource) {
        this.recordSource = recordSource;
        return this;
    }
    
    public CompositeLoopConfiguration setLCSimEventBuilder(LCSimEventBuilder eventBuilder) {
        this.eventBuilder = eventBuilder;
        return this;
    }
    
    public CompositeLoopConfiguration setDetectorName(String detectorName) {
        this.detectorName = detectorName;
        return this;
    }
    
    public CompositeLoopConfiguration setStopOnErrors(boolean stopOnErrors) {
        this.stopOnErrors = stopOnErrors;
        return this;
    }
    
    public CompositeLoopConfiguration setStopOnEndRun(boolean stopOnEndRun) {
        this.stopOnEndRun = stopOnEndRun;
        return this;
    }
    
    public CompositeLoopConfiguration setMaxRecords(int maxRecords) {
        if (maxRecords < 1)
            throw new IllegalArgumentException("Invalid maxRecords value: " + maxRecords);
        this.maxRecords = maxRecords;
        return this;
    }
    
    public CompositeLoopConfiguration add(EtEventProcessor processor) {
        etProcessors.add(processor);
        return this;
    }
    
    public CompositeLoopConfiguration add(EvioEventProcessor processor) {
        evioProcessors.add(processor);
        return this;
    }
    
    public CompositeLoopConfiguration add(Driver processor) {
        drivers.add(processor);
        return this;
    }
    
    public CompositeLoopConfiguration add(CompositeRecordProcessor processor) {
        compositeProcessors.add(processor);
        return this;
    }
}
