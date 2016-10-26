package org.hps.analysis.trigger.util;

public class LocalOutputLogger {
    private StringBuffer outputBuffer = new StringBuffer();
    
    public void printf(String text, Object... args) {
        outputBuffer.append(String.format(text, args));
    }
    
    public void println() { printf(String.format("%n")); }
    
    public void println(String text) { printf(String.format("%s%n", text)); }
    
    public void print(String text) { printf(text); }
    
    public void printLog() {
        System.out.println(outputBuffer.toString());
        clearLog();
    }
    
    public void printNewLine() { println(); }
    
    public void printNewLine(int quantity) {
        for(int i = 0; i < quantity; i++) { println(); }
    }
    
    public void clearLog() {
        outputBuffer = new StringBuffer();
    }
}
