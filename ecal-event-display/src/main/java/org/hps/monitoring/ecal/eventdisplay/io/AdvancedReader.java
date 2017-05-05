package org.hps.monitoring.ecal.eventdisplay.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;

/**
 * Class <code>AdvancedReader</code> is an implementation of <code>Reader
 * </code> that allows lines to be read both forwards and backwards.
 */
public class AdvancedReader extends Reader {
    private RandomAccessFile file;
    private long mark = -1;
    
    /**
     * Constructs an <code>AdvancedReader</code> from the file located
     * at the given file path.
     * @param filepath - The path to the file that is to be loaded.
     * @throws FileNotFoundException - Occurs when there is no file at
     * the indicated file path.
     */
    public AdvancedReader(String filepath) throws FileNotFoundException {
        file = new RandomAccessFile(filepath, "r");
    }
    
    /**
     * Constructs an <code>AdvancedReader</code> from the indicated file.
     * @param inputFile - The file that is to be loaded.
     * @throws FileNotFoundException - Occurs when the referenced file
     * does not exist.
     */
    public AdvancedReader(File inputFile) throws FileNotFoundException {
        file = new RandomAccessFile(inputFile.getAbsolutePath(), "r");
    }
    
    @Override
    public void close() throws IOException { file.close(); }
    
    @Override
    public void mark(int readAheadLimit) throws IOException { mark = file.getFilePointer(); }
    
    @Override
    public boolean markSupported() { return true; }
    
    @Override
    public int read() throws IOException { return file.read(); }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        // Read a character from the file into each spot in the array.
        for(int i = 0; i < cbuf.length; i++) {
            // Get the next character.
            int curChar = file.read();
            
            // If the character exists, write it to the array.
            if(curChar != 0) { cbuf[i] = (char)curChar; }
            
            // Otherwise, return noting the number of filled slots.
            else { return i; }
        }
        
        // If we reach the end of the loop, all array slots are filled.
        return cbuf.length;
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        // We will fill the char array until either the indicated number
        // of slots are filled or until we run out of slots.
        int[] max = { cbuf.length, off + len };
        
        // Track the number of slots written to.
        int slotsFilled = 0;
        
        // Fill the array slots.
        for(int i = off; (i < max[0] && i < max[1]); i++) {
            // Get the next character.
            int curChar = file.read();
            
            // If the character is defined, write it.
            if(curChar != -1) {
                cbuf[i] = (char)curChar;
                slotsFilled++;
            }
            
            // Otherwise, return noting the number of filled slots.
            else { return slotsFilled; }
        }
        
        // If we reach the end of the loop, all array slots are filled.
        return slotsFilled;
    }
    
    /**
     * Reads the next line of text. A line is considered to be
     * terminated by any one of a line feed ('\n'), a carriage return
     * ('\r'), or a carriage return followed immediately by a linefeed.
     * @return A String containing the contents of the line, not
     * including any line-termination characters, or null if the end
     * of the stream has been reached.
     * @throws IOException - Occurs if there is an issue reading
     * the file.
     */
    public String readNextLine() throws IOException {
        // Store whether the new line we found was a carriage return
        // or a proper newline.
        boolean readCarriageReturn = false;
        
        // Add characters to a buffer until we reach either a new line
        // or a carriage return.
        int curChar = -1;
        StringBuffer curLine = new StringBuffer();
        while((curChar = file.read()) != -1) {
            if(curChar != '\n' && curChar != '\r') { curLine.append((char)curChar); }
            else if(curChar == '\r') {
                readCarriageReturn = true;
                break;
            }
            else { break; }
        }
        
        // If we reached this point because we hit the end of the file,
        // check to make sure that the string buffer contains something.
        // If it doesn't, then we started at the end of the file and
        // should return null. Otherwise, just return what is in the
        // string buffer.
        if(curChar == -1) {
            if(curLine.length() == 0) { return null; }
            else { return curLine.toString(); }
        }
        
        // If we found a carriage return as the new line character, then
        // we need to check if the next line is a newline '\n' character,
        // since some systems us '/r/n' for a new line signifier.
        if(readCarriageReturn) {
            curChar = file.read();
            if(curChar != '\n') { file.seek(file.getFilePointer() - 1); }
        }
        
        // Return the buffer.
        return curLine.toString();
    }
    
    /**
     * Reads the previous line of text. A line is considered to be
     * terminated by any one of a line feed ('\n'), a carriage return
     * ('\r'), or a carriage return followed immediately by a linefeed.
     * @return A <code>String</code> containing the contents of the
     * line, not including any line-termination characters, or null
     * if the beginning of the stream has been reached.
     * @throws IOException - Occurs if there is an issue reading
     * the file.
     */
    public String readPreviousLine() throws IOException {
        // Define variables.
        short newlinesRead = 0;
        boolean allowDuplicate = false;
        int lastChar = -1;
        long offset = file.getFilePointer();
        int curChar;
        
        // If we are at the start of the file, return null.
        if(offset == 0) { return null; }
        
        while(newlinesRead < 3) {    
            // Decrement the offset.
            offset -= 1;
            
            // If the offset is still within the bounds of the file,
            // then go to it.
            if(offset >= 0) {
                // Get the new file position.
                file.seek(offset);
                
                // Read the character there.
                curChar = file.read();
                
                // If this a new line character, account for it.
                if(curChar == '\n' || curChar == '\r') {
                    // If we have read a newline, and we then read another
                    // immediately after it, then we will accept that newline
                    // as well, since some systems use 
                    if(((lastChar == '\n' && curChar == '\r') || (lastChar == '\r' && curChar == '\n')) && allowDuplicate) {
                        allowDuplicate = false;
                    }
                    
                    // If this is the first newline character we have
                    // seen, then we have reached the end of the desired
                    // line. We need to keep going until we find the start.
                    else if(newlinesRead < 3) {
                        newlinesRead++;
                        allowDuplicate = true;
                    }
                }
                
                // If a character other than a newline was read after
                // a newline, then a second newline is always unrelated.
                else { allowDuplicate = false; }
                
                // Set the current character to the last read character.
                lastChar = curChar;
            }
            // If the offset has reached zero, but didn't start there,
            // then we have reached the start of the file. We handle
            // this case.
            else {
                // Set the pointer to the beginning of the file.
                file.seek(0);
                
                // If we are looking for the final new line, then the
                // start of the file counts and we should return.
                if(newlinesRead == 2) { return readNextLine(); }
                
                // Otherwise, there is no previous line.
                else { return null; }
            }
        }
        
        // If we reach this return line, we should be at the correct
        // position to read the previous line.
        return readNextLine();
    }
    
    @Override
    public void reset() throws IOException {
        if(mark != -1) { file.seek(mark); }
        else { throw new IOException("mark() must be called before reset()."); }
    }
    
    @Override
    public long skip(long n) throws IOException {
        // Read n characters.
        for(int i = 0; i < n; i++) {
            // Get the skipped character.
            int curChar = file.read();
            
            // If it is invalid, return.
            if(curChar == -1) { return i; }
        }
        
        // If we reached the end of the loop, the requested number of
        // characters have been skipped.
        return n;
    }
}