package org.hps.datacat.client;

import org.json.JSONObject;

/**
 * Implementation of {@link DatasetLocation} interface.
 * 
 * @author Jeremy McCormick, SLAC
 */
final class DatasetLocationImpl implements DatasetLocation {

    /**
     * The dataset's site.
     */
    private DatasetSite site;
    
    /**
     * The resource on the file system.
     */
    private String resource;
    
    /**
     * The scan status.
     */
    private ScanStatus scanStatus = ScanStatus.UNKNOWN;
    
    /**
     * The size of the file in bytes.
     */
    private long size;
    
    /**
     * The minimum run number.
     */
    private int runMin;
    
    /**
     * The maximum run number.
     */
    private int runMax;
    
    /**
     * The event count.
     */
    private int eventCount;
    
    /**
     * Create a dataset location.
     * 
     * @param site the site of the dataset location
     * @param resource the source on disk
     * @param size the size of the file
     * @param scanStatus the scan status
     */
    DatasetLocationImpl(DatasetSite site, String resource, long size, ScanStatus scanStatus) {
        this.site = site;
        this.resource = resource;
        this.scanStatus = scanStatus;
        this.size = size;
    }
    
    /**
     * Create a dataset location from JSON.
     * 
     * @param jsonObject the JSON object
     */
    DatasetLocationImpl(JSONObject jsonObject) {
        parse(jsonObject);
    }
    
    /**
     * Parse JSON data into this object.
     * 
     * @param jsonObject the JSON data
     */
    private void parse(JSONObject jsonObject) {
        if (!jsonObject.getString("_type").equals("location")) {
            throw new IllegalArgumentException("Wrong _type in JSON data: " + jsonObject.getString("_type"));
        }
        this.site = DatasetSite.valueOf(jsonObject.getString("name"));
        this.resource = jsonObject.getString("resource");
        this.size = jsonObject.getLong("size");        
        if (!jsonObject.get("scanStatus").equals(JSONObject.NULL)) {            
            this.scanStatus = ScanStatus.valueOf(jsonObject.getString("scanStatus"));
        }
        this.runMin = jsonObject.getInt("runMin");
        this.runMax = jsonObject.getInt("runMax");
        this.eventCount = jsonObject.getInt("eventCount");
    }
    
    /**
     * Get the site of the dataset (JLAB or SLAC).
     * 
     * @return the dataset site
     */
    @Override
    public DatasetSite getSite() {
        return this.site;
    }

    /**
     * Get the resource of the dataset location (file system path).
     * 
     * @return the resource of the dataset location
     */
    @Override
    public String getResource() {
        return resource;
    }

    /**
     * Get the scan status of the dataset location.
     * 
     * @return the scan status
     */
    @Override
    public ScanStatus getScanStatus() {
        return scanStatus;
    }

    /**
     * The size of the file in bytes.
     * 
     * @return the size of the file in bytes
     */
    @Override
    public long getSize() {
        return this.size;
    }

    /**
     * Get the minimum run number.
     * 
     * @return the minimum run number
     */
    @Override
    public int getRunMin() {
        return this.runMin;
    }

    /**
     * Get the maximum run number.
     * 
     * @return the maximum run number
     */
    @Override
    public int getRunMax() {
        return this.runMax;
    }
    
    /**
     * Get the event count.
     * 
     * @return the event count
     */
    @Override
    public int getEventCount() {
        return this.eventCount;
    }
    
    /**
     * Set the minimum run number.
     * 
     * @param runMin the minimum run number
     */
    void setRunMin(int runMin) {
        this.runMin = runMin;
    }
    
    /**
     * Set the maximum run number.
     * 
     * @param runMax the maximum run number
     */
    void setRunMax(int runMax) {
        this.runMax = runMax;
    }
    
    /**
     * Set the event count.
     * 
     * @param eventCount the event count
     */
    void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }
}
