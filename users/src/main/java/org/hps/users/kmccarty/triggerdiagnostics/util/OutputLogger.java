package org.hps.users.kmccarty.triggerdiagnostics.util;

public class OutputLogger {
    private static StringBuffer outputBuffer = new StringBuffer();
    
	public static final void printf(String text, Object... args) {
		outputBuffer.append(String.format(text, args));
	}
	
	public static final void println() { printf(String.format("%n")); }
	
	public static final void println(String text) { printf(String.format("%s%n", text)); }
	
	public static final void print(String text) { printf(text); }
	
	public static final void printLog() {
		System.out.println(outputBuffer.toString());
		clearLog();
	}
	
	public static final void printNewLine() { println(); }
	
	public static final void printNewLine(int quantity) {
		for(int i = 0; i < quantity; i++) { println(); }
	}
	
	public static final void clearLog() {
		outputBuffer = new StringBuffer();
	}
}
