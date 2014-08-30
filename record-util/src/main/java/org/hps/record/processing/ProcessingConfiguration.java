package org.hps.record.processing;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.source.RecordSource;
import org.hps.evio.LCSimEventBuilder;
import org.hps.record.composite.CompositeProcessor;
import org.hps.record.et.EtConnection;
import org.hps.record.et.EtProcessor;
import org.hps.record.evio.EvioProcessor;
import org.lcsim.util.Driver;

/**
 * A configuration object for the {@link ProcessingChain}.
 * The instance variables are readable within this package, but
 * must be set through the public set methods when used
 * outside of it, e.g. from MonitoringApplication.
 */
public class ProcessingConfiguration {
        
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
    
    List<EvioProcessor> evioProcessors = new ArrayList<EvioProcessor>();
    List<Driver> drivers = new ArrayList<Driver>();
    List<CompositeProcessor> compositeProcessors = new ArrayList<CompositeProcessor>();
    List<EtProcessor> etProcessors = new ArrayList<EtProcessor>();
                 
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
    
    public void setMaxRecords(int maxRecords) {
        if (maxRecords < 1)
            throw new IllegalArgumentException("Invalid maxRecords value: " + maxRecords);
        this.maxRecords = maxRecords;
    }
    
    public void add(EtProcessor processor) {
        etProcessors.add(processor);
    }
    
    public void add(EvioProcessor processor) {
        evioProcessors.add(processor);
    }
    
    public void add(Driver processor) {
        drivers.add(processor);
    }
    
    public void add(CompositeProcessor processor) {
        compositeProcessors.add(processor);
    }
}
