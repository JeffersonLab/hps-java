package org.lcsim.hps.recon.tracking.apv25;

//--- Constants ---//
import org.lcsim.hps.recon.tracking.HPSSVTConstants;

//--- hps-java ---//
import org.hps.util.ClockSingleton;
import org.hps.util.RingBuffer;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: Apv25Full.java,v 1.9 2013/04/25 22:11:14 meeg Exp $
 */
public class Apv25Full {
    
    // APV25 trigger bit
    public static boolean readoutBit = false;
    protected int triggerLatency = 0;	// Clock cycles
    protected int triggerLatencyTime = 240; // ns
    
    // APV25 Channels; An APV25 Readout Chip contains a total of 128 channels
    private Apv25Channel[] channels = new Apv25Channel[HPSSVTConstants.CHANNELS];
  
    /**
     * Default Ctor
     */
    public Apv25Full(){
        
        // Instantiate all APV25 channels
        for(int channelN = 0; channelN < HPSSVTConstants.CHANNELS; channelN++){
            channels[channelN] = new Apv25Channel();
        }
        // Set the trigger latency
        this.setLatency(triggerLatencyTime);
    }
    
    /**
     * 
     */
    public void setLatency(int triggerLatencyTime /*ns*/){
        this.triggerLatency = (int) Math.floor(triggerLatencyTime/HPSSVTConstants.SAMPLING_INTERVAL);
        for(int channelN = 0; channelN < HPSSVTConstants.CHANNELS; channelN++) channels[channelN].getPipeline().resetPointerPositions();
    }
    
    /**
     * Return an instance of an APV25 channel
     * 
     * @param channel: APV25 channel of interest (0-127)
     * @return an instance of an Apv25Channel
     */
    public Apv25Channel getChannel(int channel){
        if(channel >= HPSSVTConstants.CHANNELS) throw new RuntimeException();
        return channels[channel];
    }
    
    /**
     * Inject charge into a channel and shape the signal.  The resulting 
     * shaper signal is then sampled into the analog pipeline
     * 
     * @param charge   : Total charge being injected
     * @param pipeline : Analog pipeline associated with a channel
     */
    public void injectCharge(int channel, double charge) {
        
        	// Shape the injected charge
            this.getChannel(channel).shapeSignal(charge);

            // Sample the resulting shaper signal
            this.getChannel(channel).sampleShaperSignal();
    }
    
    /**
     * 
     */
    public void incrementPointerPositions(){
        for(int channel = 0; channel < channels.length; channel++){
            channels[channel].pipeline.step();
        }
    }
    
    /**
     * 
     */
    public Apv25AnalogData readOut(){
        
        Apv25AnalogData data = new Apv25AnalogData();
        for(int channel = 0; channel < HPSSVTConstants.CHANNELS; channel++){
            
            // Only readout the channel if the channel isn't bad
            if(!this.getChannel(channel).isBadChannel()){
                // Readout the value stored in the buffer
                double sample = (this.getChannel(channel).getPipeline().readout()/HPSSVTConstants.FRONT_END_GAIN)*HPSSVTConstants.MULTIPLEXER_GAIN;
                data.setChannelData(channel, sample);
            }
        }
        return data;
    }
    
    //------------------------------------------//
    //               APV25 Channel              //
    //------------------------------------------//
    public class Apv25Channel {
        
        private Apv25ShaperSignal shaperSignal;
        private Apv25Pipeline pipeline = new Apv25Pipeline();
        
        private double shapingTime = 50; // [ns]
        boolean badChannel = false;
        
        /**
         * Default Constructor
         */
        public Apv25Channel(){
        }
        
        /**
         * Set the shaping time
         * 
         * @param shapingTime : APV25 shaping time. The default Tp is set to 50 ns.
         */
        public void setShapingTime(double shapingTime) {
            this.shapingTime = shapingTime;
        }
        
        /**
         * 
         */
        public void markAsBadChannel(){
            badChannel = true;
        }
        
        /**
         * 
         */
        public boolean isBadChannel(){
            return badChannel;
        }
        
        /**
         * 
         */
        public Apv25Pipeline getPipeline(){
        	return pipeline;
        }
                
        /**
         * Shape the injected charge
         * 
         * @param charge 
         */
        public void shapeSignal(double charge){
            shaperSignal = new Apv25ShaperSignal(charge);
        }
        
        /**
         *
         */
        public void sampleShaperSignal(){
            
            // Obtain the beam time
            double beamTime = ClockSingleton.getTime();
            
            // Fill the analog pipeline starting with the cell to which the writer pointer is pointing 
            // to. Signals arriving within the same bucket of length <samplingTime> will be shifted in
            // time depending on when they arrive.
            for(int cell = 0; cell < HPSSVTConstants.ANALOG_PIPELINE_LENGTH; cell++){
                
                // Time at which the shaper signal will be sampled
            	int sampleTime = cell*((int) HPSSVTConstants.SAMPLING_INTERVAL) - (int) (beamTime%HPSSVTConstants.SAMPLING_INTERVAL);
                
                // Sample the shaper signal
                double sample = shaperSignal.getAmplitudeAtTime(sampleTime, shapingTime);
                
                // Add the value to the pipeline
                pipeline.addToCell(cell, sample);
            }
        }
    }
    
    //-------------------------------------//
    //       APV25 Analog Pipeline         //
    //-------------------------------------//
    public class Apv25Pipeline extends RingBuffer {

        // TODO: Possibly store the pipeline in the event
        
        // Note: ptr gives the position of the trigger pointer
        private int writerPointer = 0;
        
        /**
         * Constructor
         */
        public Apv25Pipeline(){
             
            // Initialize the pipeline to the APV25 pipeline length
            super(HPSSVTConstants.ANALOG_PIPELINE_LENGTH);
            
            // Initialize the position of the trigger pointer to a random position
            this.ptr = (int) (Math.random()*HPSSVTConstants.ANALOG_PIPELINE_LENGTH);
        }        
        
        /**
         * 
         */
        public void resetPointerPositions(){
        	writerPointer = (ptr + triggerLatency)%HPSSVTConstants.ANALOG_PIPELINE_LENGTH;
        }
        
        /**
         * 
         */
        @Override
        public void addToCell(int position, double element){
            int writePosition = (writerPointer + position)%HPSSVTConstants.ANALOG_PIPELINE_LENGTH;
            if(writePosition == this.ptr) return;
            array[writePosition] += element;
        }
        
        /**
         * 
         */
        public double readout(){
            double triggerPointerValue = this.currentValue();
            array[ptr] = 0;
            return triggerPointerValue;
        }
        
        /**
         * 
         */
        @Override
        public void step(){
            super.step();
            writerPointer = (ptr + triggerLatency)%HPSSVTConstants.ANALOG_PIPELINE_LENGTH;
        }
        
        /**
         * 
         */
        @Override
        public String toString(){
            String analogPipeline = "[ ";
            for(int element = 0; element < HPSSVTConstants.ANALOG_PIPELINE_LENGTH; element++){
                if(element == ptr) analogPipeline += " TP ===>";
                else if(element == writerPointer) analogPipeline += " WP ===>";
                analogPipeline += (array[element] + ", ");
            }
            analogPipeline += "] ";
            return analogPipeline;
        }
        
        /**
         * 
         */
        public double getWriterPointerValue(){
        	return array[writerPointer];
        }
    }

    //-----------------------------------//
    //        APV25 Shaper Signal        //
    //-----------------------------------//
    public class Apv25ShaperSignal {

        // Shaper signal maximum amplitude
        private double maxAmp = 0;

        /**
         * Constructor
         * 
         * @param charge: Charge injected into a channel
         */
        Apv25ShaperSignal(double charge) {
            // Find the maximum amplitude of the shaper signal
            maxAmp = (charge/HPSSVTConstants.MIP)*HPSSVTConstants.FRONT_END_GAIN;  // mV
        }

        /**
         * Get the amplitude at a time t
         * 
         * @param time: time at which the shaper signal is to be sampled
         */
        public double getAmplitudeAtTime(double time, double shapingTime) {
            return maxAmp * (Math.max(0, time) / shapingTime) * Math.exp(1 - (time / shapingTime));
        }

    }
    

}
