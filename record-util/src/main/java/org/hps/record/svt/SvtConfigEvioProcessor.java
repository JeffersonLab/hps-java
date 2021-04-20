package org.hps.record.svt;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.svt.SvtConfigData.RocTag;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Get a list of SVT config data from an EVIO data stream.
 * 
 * @see SvtConfigData
 */
public class SvtConfigEvioProcessor extends EvioEventProcessor {

    private static Logger LOGGER = Logger.getLogger(SvtConfigEvioProcessor.class.getPackage().getName());

    private static final int DATA_TAG = 51;
    private static final int CONTROL_TAG = 66;
    private static final int CONFIG_TAG = 57614;

    private List<SvtConfigData> configs = new ArrayList<SvtConfigData>();

    private int timestamp = 0;
    
    public void process(EvioEvent evioEvent) {
        SvtConfigData config = null;
        BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);
        int configBanks = 0;
        if (headBank != null) {
            if (headBank.getIntData()[3] != 0) {
                timestamp = headBank.getIntData()[3];
                //LOGGER.finest("set timestamp " + timestamp + " from head bank");
            }
        }
        for (BaseStructure bank : evioEvent.getChildrenList()) {
            if (bank.getHeader().getTag() == DATA_TAG || bank.getHeader().getTag() == CONTROL_TAG) {
                if (bank.getChildrenList() != null) {
                    for (BaseStructure subBank : bank.getChildrenList()) {
                        if (subBank.getHeader().getTag() == CONFIG_TAG) {
                            String[] stringData = subBank.getStringData();
                            if (stringData == null) {
                                LOGGER.warning("string data is null");
                                if (subBank.getRawBytes() != null) {
                                    LOGGER.info("raw byte array len " + subBank.getRawBytes().length);
                                    LOGGER.info("cnv to string data" + '\n' + new String(subBank.getRawBytes(), StandardCharsets.UTF_8));
                                } else {
                                    LOGGER.warning("Raw byte array is null.");
                                }
                            } else {
                                if (config == null) {
                                    config = new SvtConfigData(timestamp);
                                }
                                if (stringData.length > 0) {
                                    System.out.println("found string data with length " + stringData.length);
                                    for (int i = 0; i < stringData.length; i++) {
                                        System.out.println("Printing raw string data " + i + " ...");
                                        System.out.println(stringData[i]);
                                        System.out.println("End print raw string data");
                                    }                                                                       
                                    if (!stringData[0].trim().isEmpty()) {
                                        LOGGER.info("Adding SVT config data with len " + stringData[0].length() + " ..." + '\n' + stringData[0]);                                        
                                        config.setData(RocTag.fromTag(bank.getHeader().getTag()), stringData[0]);
                                        ++configBanks;
                                    } else {
                                        LOGGER.warning("String data has no content.");
                                    }
                                } else {
                                    LOGGER.warning("String data has zero len.");
                                }
                            }
                        }
                    }
                }
            }
        }
        if (config != null) {
            LOGGER.info("Adding SVT config " + evioEvent.getEventNumber() + " with " + configBanks
                    + " banks and timestamp " + timestamp + " from event " + evioEvent.getEventNumber());
            this.configs.add(config);
        }
    }

    public List<SvtConfigData> getSvtConfigs() {
        return configs;
    }
}
