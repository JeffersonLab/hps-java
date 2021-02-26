package org.hps.test.it;

import org.lcsim.util.Driver;

public class RefPlotsDriver extends Driver {

    private String aidaFileName = "plots";

    public void setAidaFileName(String s) {
        aidaFileName = s;
    }

    public String getAidaFileName() {
        return aidaFileName;
    }
}
