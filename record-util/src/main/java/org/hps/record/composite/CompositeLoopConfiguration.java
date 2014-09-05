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
 * A configuration object for the {@link CompositeLoop}.
 * 
 * The instance variables are readable within the package,
 * e.g. by {@link CompositeLoop}, but their values must be 
 * set through the public set methods when the class is
 * used outside its package.
 * 
 * Depending on how this object is setup, some arguments
 * may end up being ignored (e.g. setting a file path
 * when actually using an ET server, etc.).
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
                     
    /**
     * Set the full path to a file being used as an event source.
     * This is ignored if the ET system is being used.
     * @param filePath The full path to a file.
     * @return This object.
     */
    public CompositeLoopConfiguration setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }
    
    /**
     * Set the ET connection parameters.
     * This is ignored if using direct file streaming.
     * @param connection The ET connection parameters.
     * @return This object.
     */
    public CompositeLoopConfiguration setEtConnection(EtConnection connection) {
        this.connection = connection;
        return this;
    }
    
    /**
     * Set the data source type e.g. ET server, EVIO file or LCIO file.
     * @param sourceType The data source type.
     * @return This object.
     */
    public CompositeLoopConfiguration setDataSourceType(DataSourceType sourceType) {
        this.sourceType = sourceType;
        return this;
    }
    
    /**
     * Set the processing stage to include i.e. ET only, ET to EVIO, or EVIO to LCIO.
     * This may be ignored if the argument does not make sense given the 
     * {@link org.hps.record.enums.DataSourceType} of this configuration.
     * @param processingStage The processing stage to include in the record chaining.
     * @return This object.
     */
    public CompositeLoopConfiguration setProcessingStage(ProcessingStage processingStage) {
        this.processingStage = processingStage;
        return this;
    }
    
    /**
     * Set directly the <code>RecordSource</code> that will supply records.
     * @param recordSource The <code>RecordSource</code> that will supply records.
     * @return This object.
     */
    public CompositeLoopConfiguration setRecordSource(RecordSource recordSource) {
        this.recordSource = recordSource;
        return this;
    }
    
    /**
     * Set the <code>LCSimEventBuilder</code> that will be used to translate from raw EVIO
     * events to LCIO. 
     * 
     * The detector name will be set on this object from within {@link CompositeLoop}. 
     * 
     * @param eventBuilder The LCSimEventBuilder object.
     * @return This object.
     */
    public CompositeLoopConfiguration setLCSimEventBuilder(LCSimEventBuilder eventBuilder) {
        this.eventBuilder = eventBuilder;
        return this;
    }
    
    /**
     * Set the name of the detector definition to be used e.g. from detector-data/detectors dir. 
     * @param detectorName The name of the detector.
     * @return This object.
     */
    public CompositeLoopConfiguration setDetectorName(String detectorName) {
        this.detectorName = detectorName;
        return this;
    }
    
    /**
     * Set whether the loop will stop when event processing errors occur.
     * Certain types of errors are considered fatal or are used to control
     * the loop and will never be ignored (e.g. ET system errors, etc.).
     * @param stopOnErrors True to stop the loop when errors occur.
     * @return This object.
     */
    public CompositeLoopConfiguration setStopOnErrors(boolean stopOnErrors) {
        this.stopOnErrors = stopOnErrors;
        return this;
    }
    
    /**
     * Set whether loop will stop when an end of run record is encountered
     * e.g. from an EvioEvent.
     * @param stopOnEndRun True to stop on end of run.
     * @return This object.
     */
    public CompositeLoopConfiguration setStopOnEndRun(boolean stopOnEndRun) {
        this.stopOnEndRun = stopOnEndRun;
        return this;
    }
    
    /**
     * Set the maximum number of records to run.
     * @param maxRecords
     * @return
     */
    public CompositeLoopConfiguration setMaxRecords(int maxRecords) {
        if (maxRecords < 1)
            throw new IllegalArgumentException("Invalid maxRecords value: " + maxRecords);
        this.maxRecords = maxRecords;
        return this;
    }
    
    /**
     * Add an {@link org.hps.record.et.EtEventProcessor} to the loop.
     * @param processor The EtEventProcessor.
     * @return This object.
     */
    public CompositeLoopConfiguration add(EtEventProcessor processor) {
        etProcessors.add(processor);
        return this;
    }
   
    /**
     * Add an {@link org.hps.record.evio.EvioEventProcessor} to the loop.
     * @param processor The EvioEventProcessor.
     * @return This object.
     */
    public CompositeLoopConfiguration add(EvioEventProcessor processor) {
        evioProcessors.add(processor);
        return this;
    }
    
    /**
     * Add an org.lcsim <code>Driver</code> to the loop.
     * @param processor The Driver.
     * @return This object.
     */
    public CompositeLoopConfiguration add(Driver processor) {
        drivers.add(processor);
        return this;
    }
    
    /**
     * Add a {@link org.hps.record.composite.CompositeRecordProcessor} to the loop.
     * @param processor The CompositeRecordProcessor.
     * @return This object.
     */
    public CompositeLoopConfiguration add(CompositeRecordProcessor processor) {
        compositeProcessors.add(processor);
        return this;
    }
}
