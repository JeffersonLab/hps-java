package org.hps.recon.tracking.gbl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Millepede-II (binary) record.
 * Containing information for local (track) and global fit.
 *
 *         real array              integer array
 *     0   0.0                     error count (this record)
 *     1   RMEAS, measured value   0                            -+
 *     2   local derivative        index of local derivative     |
 *     3   local derivative        index of local derivative     |
 *     4    ...                                                  | block
 *         SIGMA, error (>0)       0                             |
 *         global derivative       label of global derivative    |
 *         global derivative       label of global derivative   -+
 *         RMEAS, measured value   0
 *         local derivative        index of local derivative
 *         local derivative        index of local derivative
 *         ...
 *         SIGMA, error            0
 *         global derivative       label of global derivative
 *         global derivative       label of global derivative
 *         ...
 *         global derivative       label of global derivative
 *
 * @author Norman A Graf
 *
 * @version $Id$
 * 
 */
public class MilleBinary
{
    FileChannel _channel;
    List<Integer> _intBuffer = new ArrayList<Integer>();
    List<Float> _floatBuffer = new ArrayList<Float>();
    
    static String DEFAULT_OUTPUT_FILE_NAME = "millepedeData.bin"; 
    
    /**
     * Default Constructor
     */
    public MilleBinary() {
        try {
            _channel = new FileOutputStream(DEFAULT_OUTPUT_FILE_NAME).getChannel();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MilleBinary.class.getName()).log(Level.SEVERE, null, ex);
        }
        _intBuffer.add(0); // first word is error counter
        _floatBuffer.add(0f);
    }

    /**
     * Fully qualified Constructor
     * @param outputFileName name of output binary file for millepede II
     */
        public MilleBinary(String outputFileName)
    {
        try {
            _channel = new FileOutputStream(outputFileName).getChannel();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MilleBinary.class.getName()).log(Level.SEVERE, null, ex);
        }
        _intBuffer.add(0); // first word is error counter
        _floatBuffer.add(0f);
    }

    /**
     * Closes the binary output file
     */
    public void close()
    {
        try {
            _channel.close();
        } catch (IOException ex) {
            Logger.getLogger(MilleBinary.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Add data block to (end of) record.
     * @param aMeas      Value
     * @param aErr       Error
     * @param indLocal   List of labels of local parameters
     * @param derLocal   List of derivatives for local parameters
     * @param labGlobal  List of labels of global parameters
     * @param derGlobal  List of derivatives for global parameters
     */
        public void addData(float aMeas, float aErr,
                        List<Integer> indLocal,
                        List<Double> derLocal,
                        List<Integer> labGlobal,
                        List<Double> derGlobal)
    {
        _intBuffer.add(0);
        _floatBuffer.add(aMeas);
        for (int i = 0; i < indLocal.size(); ++i) {
            _intBuffer.add(indLocal.get(i));
            _floatBuffer.add((float) derLocal.get(i).doubleValue());
        }
        _intBuffer.add(0);
        _floatBuffer.add(aErr);
        for (int i = 0; i < labGlobal.size(); ++i) {
            if (derGlobal.get(i) != 0) {
                _intBuffer.add(labGlobal.get(i));
                _floatBuffer.add((float) derGlobal.get(i).doubleValue());
            }
        }
    }

    /**
     * Write record to file.
     */
        public void writeRecord()
    {
        int recordLength = _intBuffer.size() * 2 * 4; // writing both ints and floats, each is 4 bytes
        ByteBuffer b = ByteBuffer.allocate((recordLength + 1) * 2); // writing one extra word per collection
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(recordLength);
        for (Float f : _floatBuffer) {
            b.putFloat(f);
        }
        for (Integer i : _intBuffer) {
            b.putInt(i);
        }
        b.flip();
        try {
            _channel.write(b);
        } catch (IOException ex) {
            Logger.getLogger(MilleBinary.class.getName()).log(Level.SEVERE, null, ex);
        }
        b.clear();
        _floatBuffer.clear();
        _intBuffer.clear();
        _intBuffer.add(0); // first word is error counter
        _floatBuffer.add(0f);
    }
}
