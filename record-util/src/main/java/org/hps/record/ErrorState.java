package org.hps.record;

public class ErrorState {
    
    Exception lastError;
    
    public ErrorState() {        
    }
    
    public Throwable getLastError() {
        return lastError;
    }
    
    public void setLastError(Exception lastError) {
        this.lastError = lastError;
    }
    
    public boolean hasError() {
        return lastError != null;
    }
    
    public void rethrow() throws Exception {        
        Exception throwMe = lastError;        
        clear(); // Clear error state before throwing.
        throw throwMe;
    }
    
    public void clear() {
        lastError = null;
    }
    
    public void print() {
        lastError.printStackTrace();
    }

}
