package org.hps.monitoring.gui;


public interface HasErrorHandler {    
    
    void setErrorHandler(ErrorHandler errorHandler);
    
    ErrorHandler getErrorHandler();
}
