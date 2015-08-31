package org.hps.datacat.client;

import org.json.JSONObject;

/**
 * 
 * @author Jeremy McCormick, SLAC
 */
final class DatasetLocationImpl implements DatasetLocation {

    private DatasetSite site;
    private String resource;
    private ScanStatus scanStatus = ScanStatus.UNKNOWN;
    private long size;
    private int runMin;
    private int runMax;
    private int eventCount;
    
    DatasetLocationImpl(JSONObject jsonObject) {
        parse(jsonObject);
    }
    
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
    
    @Override
    public DatasetSite getSite() {
        return this.site;
    }

    @Override
    public String getResource() {
        return resource;
    }

    @Override
    public ScanStatus getScanStatus() {
        return scanStatus;
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    public int getRunMin() {
        return this.runMin;
    }

    @Override
    public int getRunMax() {
        return this.runMax;
    }

    @Override
    public int getEventCount() {
        return this.eventCount;
    }

}
