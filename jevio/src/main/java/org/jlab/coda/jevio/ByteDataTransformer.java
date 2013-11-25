package org.jlab.coda.jevio;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.*;

/**
 * This utility class contains methods for transforming a raw byte array into arrays of
 * other types and vice versa as well as other handy methods.
 * 
 * @author heddle
 * @author timmer
 * @author wolin
 *
 */
public class ByteDataTransformer {

    /**
     * Converts a byte array into an int array.
     * Use {@link #toIntArray(byte[], java.nio.ByteOrder)} for better performance.
     *
     * @param bytes the byte array.
     * @param byteOrder the endianness of the data in the byte array,
     *       {@link ByteOrder#BIG_ENDIAN} or {@link ByteOrder#LITTLE_ENDIAN}.
     * @return the raw bytes converted into an int array.
     */
    public static int[] getAsIntArray(byte bytes[], ByteOrder byteOrder) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(byteOrder);
        int intsize = bytes.length / 4;
        int array[] = new int[intsize];
        for (int i = 0; i < intsize; i++) {
            array[i] = byteBuffer.getInt();
        }
        return array;
    }


    /**
     * Converts a byte array into an short array.
     * Use {@link #toShortArray(byte[], java.nio.ByteOrder)} for better performance.
     *
     * @param bytes the byte array.
     * @param byteOrder the endianness of the data in the byte array,
     *       {@link ByteOrder#BIG_ENDIAN} or {@link ByteOrder#LITTLE_ENDIAN}.
     * @return the raw bytes converted into an int array.
     */
    public static short[] getAsShortArray(byte bytes[], ByteOrder byteOrder) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(byteOrder);
        int shortsize = bytes.length / 2;
        short array[] = new short[shortsize];
        for (int i = 0; i < shortsize; i++) {
            array[i] = byteBuffer.getShort();
        }
        return array;
    }

	/**
	 * Converts a byte array into an short array while accounting for padding.
     * Use {@link #toShortArray(byte[], int, java.nio.ByteOrder)} for better performance.
	 *
     * @param bytes the byte array.
     * @param padding number of <b>bytes</b> at the end of the byte array to ignore.
     *                Valid values are 0 and mulitples of 2 (though only 0 & 2 are used).
     * @param byteOrder the endianness of the data in the byte array,
     *       {@link ByteOrder#BIG_ENDIAN} or {@link ByteOrder#LITTLE_ENDIAN}.
	 * @return the raw bytes converted into an int array.
	 */
	public static short[] getAsShortArray(byte bytes[], int padding, ByteOrder byteOrder) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(byteOrder);
		int shortsize = bytes.length / 2;
        if (padding%2 == 0) {
            shortsize -= padding/2;
        }
		short array[] = new short[shortsize];
		for (int i = 0; i < shortsize; i++) {
			array[i] = byteBuffer.getShort();
		}
		return array;
	}

	
	/**
	 * Converts a byte array into a long array.
     * Use {@link #toLongArray(byte[], java.nio.ByteOrder)} for better performance.
	 *
	 * @param bytes the byte array.
     * @param byteOrder the endianness of the data in the byte array,
     *       {@link ByteOrder#BIG_ENDIAN} or {@link ByteOrder#LITTLE_ENDIAN}.
	 * @return the raw bytes converted into a long array.
	 */
	public static long[] getAsLongArray(byte bytes[], ByteOrder byteOrder) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(byteOrder);
		int longsize = bytes.length / 8;
		long array[] = new long[longsize];
		for (int i = 0; i < longsize; i++) {
			array[i] = byteBuffer.getLong();
		}
		return array;
	}

	/**
	 * Converts a byte array into a double array.
     * Use {@link #toDoubleArray(byte[], java.nio.ByteOrder)} for better performance.
	 *
	 * @param bytes the byte array.
     * @param byteOrder the endianness of the data in the byte array,
     *       {@link ByteOrder#BIG_ENDIAN} or {@link ByteOrder#LITTLE_ENDIAN}.
	 * @return the raw bytes converted into a double array.
	 */
	public static double[] getAsDoubleArray(byte bytes[], ByteOrder byteOrder) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(byteOrder);
		int doublesize = bytes.length / 8;
		double array[] = new double[doublesize];
		for (int i = 0; i < doublesize; i++) {
			array[i] = byteBuffer.getDouble();
		}
		return array;
	}
	
	/**
	 * Converts a byte array into a float array.
     * Use {@link #toFloatArray(byte[], java.nio.ByteOrder)} for better performance.
	 *
	 * @param bytes the byte array.
     * @param byteOrder the endianness of the data in the byte array,
     *       {@link ByteOrder#BIG_ENDIAN} or {@link ByteOrder#LITTLE_ENDIAN}.
	 * @return the raw bytes converted into a float array.
	 */
	public static float[] getAsFloatArray(byte bytes[], ByteOrder byteOrder) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(byteOrder);
		int floatsize = bytes.length / 4;
		float array[] = new float[floatsize];
		for (int i = 0; i < floatsize; i++) {
			array[i] = byteBuffer.getFloat();
		}
		return array;
	}


    /**
     * Copies a 2-byte short value into a 4-byte int while preserving bit pattern.
     * In other words, it treats the short as an unsigned value. The resulting int
     * does NOT undergo sign extension (bits of highest 2 bytes are not all ones),
     * and will not be a negative number.
     *
     * @param shortVal short value considered to be unsigned
     * @return the equivalent int value
     */
    public static final int shortBitsToInt(short shortVal) {
        return (shortVal & 0x0000ffff);
    }


    /**
     * Copies a 1-byte byte value into a 4-byte int while preserving bit pattern.
     * In other words, it treats the byte as an unsigned value. The resulting int
     * does NOT undergo sign extension (bits of highest 3 bytes are not all ones),
     * and will not be a negative number.
     *
     * @param byteVal byte value considered to be unsigned
     * @return the equivalent int value
     */
    public static final int byteBitsToInt(byte byteVal) {
        return (byteVal & 0x000000ff);
    }


    // =========================
    // primitive type --> byte[]
    // =========================

    /**
     * Copies an integer value into 4 bytes of a byte array.
     * @param intVal integer value
     * @param b byte array
     * @param off offset into the byte array
     */
    public static final void intToBytes(int intVal, byte[] b, int off) {
        b[off  ] = (byte)(intVal >> 24);
        b[off+1] = (byte)(intVal >> 16);
        b[off+2] = (byte)(intVal >>  8);
        b[off+3] = (byte)(intVal      );
    }


    /**
     * Copies a short value into 2 bytes of a byte array.
     * @param val short value
     * @param b byte array
     * @param off offset into the byte array
     */
    public static final void shortToBytes(short val, byte[] b, int off) {
        b[off  ] = (byte)(val >>  8);
        b[off+1] = (byte)(val      );
    }



    /**
     * Turn short into byte array.
     *
     * @param data short to convert
     * @param byteOrder byte order of returned byte array (big endian if null)
     * @return byte array representing short
     */
    public static byte[] toBytes(short data, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return new byte[] {
                    (byte)(data >>> 8),   // byte cast just truncates data val
                    (byte)(data),
            };
        }
        else {
            return new byte[] {
                    (byte)(data),
                    (byte)(data >>> 8),
            };
        }
    }


    /**
     * Turn short into byte array.
     * Avoids creation of new byte array with each call.
     *
     * @param data short to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @param dest array in which to store returned bytes
     * @param off offset into dest array where returned bytes are placed
     * @throws EvioException if dest is null or too small or offset negative
     */
    public static void toBytes(short data, ByteOrder byteOrder, byte[] dest, int off)
            throws EvioException {

        if (dest == null || dest.length < 2+off || off < 0) {
            throw new EvioException("bad arg(s)");
        }

        if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
            dest[off  ] = (byte)(data >>> 8);
            dest[off+1] = (byte)(data      );
        }
        else {
            dest[off  ] = (byte)(data      );
            dest[off+1] = (byte)(data >>> 8);
        }
    }


    /**
     * Turn short array into byte array.
     *
     * @param data short array to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @return byte array representing short array or null if data is null
     * @throws EvioException if data array has too many elements to convert to a byte array
     */
    public static byte[] toBytes(short[] data, ByteOrder byteOrder) throws EvioException {

        if (data == null) return null;
        if (data.length > Integer.MAX_VALUE/2) {
            throw new EvioException("short array has too many elements to convert to byte array");
        }

        if (byteOrder == null) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        byte[] b = new byte[data.length*2];

        ByteBuffer buf = ByteBuffer.wrap(b).order(byteOrder);
        ShortBuffer ib = buf.asShortBuffer();
        ib.put(data, 0, data.length);

        return b;
    }


    /**
      * Turn short array into byte array.
      * Avoids creation of new byte array with each call.
      *
      * @param data short array to convert
      * @param byteOrder byte order of written bytes (big endian if null)
      * @param dest array in which to store written bytes
      * @param off offset into dest array where returned bytes are placed
      * @throws EvioException if data is null, dest is null or too small, or offset negative;
      *                       if data array has too many elements to convert to a byte array
      */
     public static void toBytes(short[] data, ByteOrder byteOrder, byte[] dest, int off)
             throws EvioException {

         if (data == null || dest == null || dest.length < 2*data.length+off || off < 0) {
             throw new EvioException("bad arg(s)");
         }

         if (data.length > Integer.MAX_VALUE/2) {
             throw new EvioException("short array has too many elements to convert to byte array");
         }

         if (byteOrder == null) {
             byteOrder = ByteOrder.BIG_ENDIAN;
         }

         ByteBuffer buf = ByteBuffer.wrap(dest).order(byteOrder);
         buf.position(off);
         ShortBuffer ib = buf.asShortBuffer();
         ib.put(data, 0, data.length);

         return;
     }


    // =========================

    /**
     * Turn int into byte array.
     *
     * @param data int to convert
     * @param byteOrder byte order of returned byte array (big endian if null)
     * @return byte array representing int
     */
    public static byte[] toBytes(int data, ByteOrder byteOrder) {
        if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
            return new byte[] {
                    (byte)(data >> 24),
                    (byte)(data >> 16),
                    (byte)(data >>  8),
                    (byte)(data),
            };
        }
        else {
            return new byte[] {
                    (byte)(data),
                    (byte)(data >>  8),
                    (byte)(data >> 16),
                    (byte)(data >> 24),
            };
        }
    }


    /**
     * Turn int into byte array.
     * Avoids creation of new byte array with each call.
     *
     * @param data int to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @param dest array in which to store returned bytes
     * @param off offset into dest array where returned bytes are placed
     * @throws EvioException if dest is null or too small or offset negative
     */
    public static void toBytes(int data, ByteOrder byteOrder, byte[] dest, int off)
            throws EvioException {

        if (dest == null || dest.length < 4+off || off < 0) {
            throw new EvioException("bad arg(s)");
        }

        if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
            dest[off  ] = (byte)(data >> 24);
            dest[off+1] = (byte)(data >> 16);
            dest[off+2] = (byte)(data >>  8);
            dest[off+3] = (byte)(data      );
        }
        else {
            dest[off  ] = (byte)(data      );
            dest[off+1] = (byte)(data >>  8);
            dest[off+2] = (byte)(data >> 16);
            dest[off+3] = (byte)(data >> 24);
        }
    }


    /**
     * Turn int array into byte array.
     *
     * @param data int array to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @return byte array representing int array or null if data is null
     * @throws EvioException if data array as too many elements to
     *                       convert to a byte array
     */
    public static byte[] toBytes(int[] data, ByteOrder byteOrder) throws EvioException {

        if (data == null) return null;
        if (data.length > Integer.MAX_VALUE/4) {
            throw new EvioException("int array has too many elements to convert to byte array");
        }

        if (byteOrder == null) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        byte[] b = new byte[data.length*4];

        ByteBuffer buf = ByteBuffer.wrap(b).order(byteOrder);
        IntBuffer ib = buf.asIntBuffer();
        ib.put(data, 0, data.length);

        return b;
    }


    /**
     * Turn int array into byte array.
     *
     * @param data int array to convert
     * @param offset into int array
     * @param length number of integers to conver to bytes
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @return byte array representing int array or null if data is null
     * @throws EvioException if data array has too many elements to
     *                       convert to a byte array
     */
    public static byte[] toBytes(int[] data, int offset, int length, ByteOrder byteOrder)
                    throws EvioException {

        if (data == null) return null;
        if (offset < 0 || length < 1) return null;
        if (data.length > Integer.MAX_VALUE/4) {
            throw new EvioException("int array has too many elements to convert to byte array");
        }

        if (byteOrder == null) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        byte[] b = new byte[length*4];

        ByteBuffer buf = ByteBuffer.wrap(b).order(byteOrder);
        IntBuffer ib = buf.asIntBuffer();
        ib.put(data, offset, length);

        return b;
    }


    /**
     * Turn int array into byte array.
     * Avoids creation of new byte array with each call.
     *
     * @param data int array to convert
     * @param byteOrder byte order of written bytes (big endian if null)
     * @param dest array in which to store written bytes
     * @param off offset into dest array where returned bytes are placed
     * @throws EvioException if data is null, dest is null or too small, or offset negative;
     *                       if data array has too many elements to convert to a byte array
     */
    public static void toBytes(int[] data, ByteOrder byteOrder, byte[] dest, int off)
            throws EvioException{

        if (data == null || dest == null || dest.length < 4*data.length+off || off < 0) {
            throw new EvioException("bad arg(s)");
        }

        if (data.length > Integer.MAX_VALUE/4) {
            throw new EvioException("int array has too many elements to convert to byte array");
        }

        if (byteOrder == null) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        ByteBuffer buf = ByteBuffer.wrap(dest).order(byteOrder);
        buf.position(off);
        IntBuffer ib = buf.asIntBuffer();
        ib.put(data, 0, data.length);

        return;
    }


    /**
     * Turn int array into byte array.
     * Uses IO streams to do the work - very inefficient.
     * Keep around for tutorial purposes.
     *
     * @param data int array to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @return byte array representing int array
     * @throws EvioException if data array has too many elements to
     *                       convert to a byte array
     */
    public static byte[] toBytesStream(int[] data, ByteOrder byteOrder) throws EvioException {

        if (data == null) return null;
        if (data.length > Integer.MAX_VALUE/4) {
            throw new EvioException("int array has too many elements to convert to byte array");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(4*data.length);
        DataOutputStream out = new DataOutputStream(baos);

        try {
            for (int i : data) {
                if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
                    out.writeInt(i);
                }
                else {
                    out.writeInt(Integer.reverseBytes(i));
                }
            }
            out.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    // =========================

    /**
     * Turn long into byte array.
     *
     * @param data long to convert
     * @param byteOrder byte order of returned byte array (big endian if null)
     * @return byte array representing long
     */
    public static byte[] toBytes(long data, ByteOrder byteOrder) {

        if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
            return new byte[] {
                    (byte)(data >> 56),
                    (byte)(data >> 48),
                    (byte)(data >> 40),
                    (byte)(data >> 32),
                    (byte)(data >> 24),
                    (byte)(data >> 16),
                    (byte)(data >>  8),
                    (byte)(data      ),
            };
        }
        else {
            return new byte[] {
                    (byte)(data      ),
                    (byte)(data >>  8),
                    (byte)(data >> 16),
                    (byte)(data >> 24),
                    (byte)(data >> 32),
                    (byte)(data >> 40),
                    (byte)(data >> 48),
                    (byte)(data >> 56),
            };
        }
    }


    /**
     * Turn long into byte array.
     * Avoids creation of new byte array with each call.
     *
     * @param data long to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @param dest array in which to store returned bytes
     * @param off offset into dest array where returned bytes are placed
     * @throws EvioException if dest is null or too small or offset negative
     */
    public static void toBytes(long data, ByteOrder byteOrder, byte[] dest, int off)
            throws EvioException {

        if (dest == null || dest.length < 8+off || off < 0) {
            throw new EvioException("bad arg(s)");
        }

        if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
            dest[off  ] = (byte)(data >> 56);
            dest[off+1] = (byte)(data >> 48);
            dest[off+2] = (byte)(data >> 40);
            dest[off+3] = (byte)(data >> 32);
            dest[off+4] = (byte)(data >> 24);
            dest[off+5] = (byte)(data >> 16);
            dest[off+6] = (byte)(data >>  8);
            dest[off+7] = (byte)(data      );
        }
        else {
            dest[off  ] = (byte)(data      );
            dest[off+1] = (byte)(data >>  8);
            dest[off+2] = (byte)(data >> 16);
            dest[off+3] = (byte)(data >> 24);
            dest[off+4] = (byte)(data >> 32);
            dest[off+5] = (byte)(data >> 40);
            dest[off+6] = (byte)(data >> 48);
            dest[off+7] = (byte)(data >> 56);
        }
    }


    /**
     * Turn long array into byte array.
     *
     * @param data long array to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @return byte array representing long array or null if data is null
     * @throws EvioException if data array has too many elements to
     *                       convert to a byte array
     */
    public static byte[] toBytes(long[] data, ByteOrder byteOrder) throws EvioException {

        if (data == null) return null;
        if (data.length > Integer.MAX_VALUE/8) {
            throw new EvioException("long array has too many elements to convert to byte array");
        }

        if (byteOrder == null) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        byte[] b = new byte[data.length*8];

        ByteBuffer buf = ByteBuffer.wrap(b).order(byteOrder);
        LongBuffer ib = buf.asLongBuffer();
        ib.put(data, 0, data.length);

        return b;
    }


    /**
     * Turn long array into byte array.
     * Avoids creation of new byte array with each call.
     *
     * @param data long array to convert
     * @param byteOrder byte order of written bytes (big endian if null)
     * @param dest array in which to store written bytes
     * @param off offset into dest array where returned bytes are placed
     * @throws EvioException if data is null, dest is null or too small, or offset negative;
     *                       if data array has too many elements to convert to a byte array
     */
    public static void toBytes(long[] data, ByteOrder byteOrder, byte[] dest, int off)
            throws EvioException{

        if (data == null || dest == null || dest.length < 8*data.length+off || off < 0) {
            throw new EvioException("bad arg(s)");
        }

        if (data.length > Integer.MAX_VALUE/8) {
            throw new EvioException("long array has too many elements to convert to byte array");
        }

        if (byteOrder == null) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        ByteBuffer buf = ByteBuffer.wrap(dest).order(byteOrder);
        buf.position(off);
        LongBuffer ib = buf.asLongBuffer();
        ib.put(data, 0, data.length);

        return;
    }


    // =========================

    /**
     * Turn float into byte array.
     *
     * @param data float to convert
     * @param byteOrder byte order of returned byte array (big endian if null)
     * @return byte array representing float
     */
    public static byte[] toBytes(float data, ByteOrder byteOrder) {
        return toBytes(Float.floatToRawIntBits(data), byteOrder);
    }

    /**
     * Turn float into byte array.
     * Avoids creation of new byte array with each call.
     *
     * @param data float to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @param dest array in which to store returned bytes
     * @param off offset into dest array where returned bytes are placed
     * @throws EvioException if dest is null or too small or offset negative
     */
    public static void toBytes(float data, ByteOrder byteOrder, byte[] dest, int off)
            throws EvioException {

        toBytes(Float.floatToRawIntBits(data), byteOrder, dest, off);
    }

    /**
     * Turn float array into byte array.
     *
     * @param data float array to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @return byte array representing float array or null if data is null
     * @throws EvioException if data array has too many elements to
     *                       convert to a byte array
     */
    public static byte[] toBytes(float[] data, ByteOrder byteOrder) throws EvioException {

        if (data == null) return null;
        if (data.length > Integer.MAX_VALUE/4) {
            throw new EvioException("float array has too many elements to convert to byte array");
        }

        if (byteOrder == null) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        byte[] b = new byte[data.length*4];

        ByteBuffer buf = ByteBuffer.wrap(b).order(byteOrder);
        FloatBuffer ib = buf.asFloatBuffer();
        ib.put(data, 0, data.length);

        return b;
    }

    /**
     * Turn float array into byte array.
     * Avoids creation of new byte array with each call.
     *
     * @param data float array to convert
     * @param byteOrder byte order of written bytes (big endian if null)
     * @param dest array in which to store written bytes
     * @param off offset into dest array where returned bytes are placed
     * @throws EvioException if data is null, dest is null or too small, or offset negative;
     *                       if data array has too many elements to convert to a byte array
     */
    public static void toBytes(float[] data, ByteOrder byteOrder, byte[] dest, int off)
            throws EvioException{

        if (data == null || dest == null || dest.length < 4*data.length+off || off < 0) {
            throw new EvioException("bad arg(s)");
        }

        if (data.length > Integer.MAX_VALUE/4) {
            throw new EvioException("float array has too many elements to convert to byte array");
        }

        if (byteOrder == null) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        ByteBuffer buf = ByteBuffer.wrap(dest).order(byteOrder);
        buf.position(off);
        FloatBuffer ib = buf.asFloatBuffer();
        ib.put(data, 0, data.length);

        return;
    }

    // =========================

    /**
     * Turn double into byte array.
     *
     * @param data double to convert
     * @param byteOrder byte order of returned byte array (big endian if null)
     * @return byte array representing double
     */
    public static byte[] toBytes(double data, ByteOrder byteOrder) {
        return toBytes(Double.doubleToRawLongBits(data), byteOrder);
    }

    /**
     * Turn double into byte array.
     * Avoids creation of new byte array with each call.
     *
     * @param data double to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @param dest array in which to store returned bytes
     * @param off offset into dest array where returned bytes are placed
     * @throws EvioException if dest is null or too small or offset negative
     */
    public static void toBytes(double data, ByteOrder byteOrder, byte[] dest, int off)
            throws EvioException {

        toBytes(Double.doubleToRawLongBits(data), byteOrder, dest, off);
    }

    /**
     * Turn double array into byte array.
     *
     * @param data double array to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @return byte array representing double array or null if data is null
     * @throws EvioException if data array has too many elements to
     *                       convert to a byte array
     */
    public static byte[] toBytes(double[] data, ByteOrder byteOrder) throws EvioException {

        if (data == null) return null;
        if (data.length > Integer.MAX_VALUE/8) {
            throw new EvioException("double array has too many elements to convert to byte array");
        }

        if (byteOrder == null) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        byte[] b = new byte[data.length*8];

        ByteBuffer buf = ByteBuffer.wrap(b).order(byteOrder);
        DoubleBuffer ib = buf.asDoubleBuffer();
        ib.put(data, 0, data.length);

        return b;
    }

    /**
     * Turn double array into byte array.
     * Avoids creation of new byte array with each call.
     *
     * @param data double array to convert
     * @param byteOrder byte order of written bytes (big endian if null)
     * @param dest array in which to store written bytes
     * @param off offset into dest array where returned bytes are placed
     * @throws EvioException if data is null, dest is null or too small, or offset negative;
     *                       if data array has too many elements to convert to a byte array
     */
    public static void toBytes(double[] data, ByteOrder byteOrder, byte[] dest, int off)
            throws EvioException{

        if (data == null || dest == null || dest.length < 8*data.length+off || off < 0) {
            throw new EvioException("bad arg(s)");
        }

        if (data.length > Integer.MAX_VALUE/8) {
            throw new EvioException("double array has too many elements to convert to byte array");
        }

        if (byteOrder == null) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        ByteBuffer buf = ByteBuffer.wrap(dest).order(byteOrder);
        buf.position(off);
        DoubleBuffer ib = buf.asDoubleBuffer();
        ib.put(data, 0, data.length);

        return;
    }

    /**
     * Turn double array into byte array.
     * Slower than {@link #toBytes(double[], ByteOrder)}.
     * Keep this around for completeness purposes - a record
     * of what was already tried and found wanting. Tried this
     * for all data types.
     *
     * @param data double array to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @return byte array representing double array or null if data is null
     * @throws EvioException if data array has too many elements to
     *                       convert to a byte array
     */
    public static byte[] toBytes2(double[] data, ByteOrder byteOrder) throws EvioException {

        if (data == null) return null;
        if (data.length > Integer.MAX_VALUE/8) {
            throw new EvioException("double array has too many elements to convert to byte array");
        }

        byte[] stor = new byte[8];
        byte[] byts = new byte[data.length*8];

        try {
            for (int i = 0; i < data.length; i++) {
                toBytes(data[i], byteOrder, stor, 0);
                System.arraycopy(stor, 0, byts, i*8, 8);
            }
        }
        catch (EvioException e) {/* never happen */}

        return byts;
    }

    /**
     * Turn double array into byte array.
     * Avoids creation of new byte array with each call.
     * Slower than {@link #toBytes(double[], ByteOrder, byte[], int)}.
     * Keep this around for completeness purposes - a record
     * of what was already tried and found wanting. Tried this
     * for all data types.
     *
     * @param data double array to convert
     * @param byteOrder byte order of returned bytes (big endian if null)
     * @param dest array in which to store returned bytes
     * @param off offset into dest array where returned bytes are placed
     * @throws EvioException if data is null, dest is null or too small, or offset negative;
     *                       if data array has too many elements to convert to a byte array
     */
    public static void toBytes2(double[] data, ByteOrder byteOrder, byte[] dest, int off)
            throws EvioException{

        if (data == null || dest == null || dest.length < 8*data.length+off || off < 0) {
            throw new EvioException("bad arg(s)");
        }
        if (data.length > Integer.MAX_VALUE/8) {
            throw new EvioException("double array has too many elements to convert to byte array");
        }

        byte[] stor = new byte[8];

        try {
            for (int i = 0; i < data.length; i++) {
                toBytes(data[i], byteOrder, stor, 0);
                System.arraycopy(stor, 0, dest, off + i*8, 8);
            }
        }
        catch (EvioException e) {/* never happen */}

        return;
    }

    // =========================
    // byte[] --> primitive type
    // =========================

    /**
     * Turn section of byte array into a short.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @param off offset into data array
     * @return short converted from byte array
     * @throws EvioException if data is null or wrong size, or off is negative
     */
    public static short toShort(byte[] data, ByteOrder byteOrder, int off) throws EvioException {
        if (data == null || data.length < 2+off || off < 0) {
            throw new EvioException("bad data arg");
        }

        if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
            return (short)(
                (0xff & data[  off]) << 8 |
                (0xff & data[1+off])
            );
        }
        else {
            return (short)(
                (0xff & data[  off]) |
                (0xff & data[1+off]) << 8
            );
        }
    }

    /**
     * Turn 2 bytes into a short.
     *
     * @param b1 1st byte
     * @param b2 2nd byte
     * @param byteOrder if big endian, 1st byte is most significant (big endian if null)
     * @return short converted from byte array
     */
    public static short toShort(byte b1, byte b2, ByteOrder byteOrder) {

        if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
            return (short)(
                (0xff & b1) << 8 |
                (0xff & b2)
            );
        }
        else {
            return (short)(
                (0xff & b1) |
                (0xff & b2) << 8
            );
        }
    }

    /**
     * Turn byte array into a short array.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @return short array converted from byte array
     * @throws EvioException if data is null or odd size
     */
    public static short[] toShortArray(byte[] data, ByteOrder byteOrder) throws EvioException {
        if (data == null || data.length % 2 != 0) {
            throw new EvioException("bad data arg");
        }

        short[] sa = new short[data.length / 2];
        for (int i=0; i < sa.length; i++) {
            sa[i] = toShort(data[(i*2)],
                            data[(i*2)+1],
                            byteOrder);
        }
        return sa;
    }

    /**
     * Turn byte array into a short array while taking padding into account.
     *
     * @param data byte array to convert
     * @param padding number of <b>bytes</b> at the end of the byte array to ignore.
     *                Valid values are 0 and 2
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @return short array converted from byte array
     * @throws EvioException if data is null or odd size; padding not 0 or 2
     */
    public static short[] toShortArray(byte[] data, int padding, ByteOrder byteOrder) throws EvioException {
        if (data == null || data.length % 2 != 0 || (padding != 0 && padding != 2)) {
            throw new EvioException("bad data arg");
        }

        short[] sa = new short[(data.length - padding)/2];
        for (int i=0; i < sa.length; i++) {
            sa[i] = toShort(data[(i*2)],
                            data[(i*2)+1],
                            byteOrder);
        }
        return sa;
    }

    /**
     * Turn byte array into a short array while taking padding into account.
     *
     * @param data byte array to convert
     * @param padding number of <b>bytes</b> at the end of the byte array to ignore.
     *                Valid values are 0 and 2
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @param dest array in which to write converted bytes
     * @param off offset into dest array
     * @throws EvioException if data is null or wrong size; dest is null, wrong size, or off < 0
     */
    public static void toShortArray(byte[] data, int padding, ByteOrder byteOrder, short[] dest, int off)
            throws EvioException {

        if (data == null || data.length % 2 != 0 || (padding != 0 && padding != 2) ||
            dest == null || 2*dest.length < data.length-padding+off || off < 0) {
            throw new EvioException("bad data arg");
        }

        int len = data.length - padding;
System.out.println("toShortArray: padding = " + padding + ", data len = " + data.length);
        for (int i = 0; i < data.length - padding - 1; i+=2) {
            dest[i/2+off] = toShort(data[i],
                                    data[i+1],
                                    byteOrder);
        }
    }

    /**
     * Turn byte array into an short array.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @param dest array in which to write converted bytes
     * @param off offset into dest array
     * @throws EvioException if data is null or wrong size; dest is null, wrong size, or off < 0
     */
    public static void toShortArray(byte[] data, ByteOrder byteOrder, short[] dest, int off)
            throws EvioException {

        if (data == null || data.length % 2 != 0 ||
            dest == null || 2*dest.length < data.length+off || off < 0) {
            throw new EvioException("bad data arg");
        }

        for (int i = 0; i < data.length-1; i+=2) {
            dest[i/2+off] = toShort(data[i],
                                    data[i + 1],
                                    byteOrder);
        }
    }

	    // =========================

    /**
     * Turn section of byte array into an int.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @param off offset into data array
     * @return int converted from byte array
     * @throws EvioException if data is null or wrong size, or off is negative
     */
    public static int toInt(byte[] data, ByteOrder byteOrder, int off) throws EvioException {
        if (data == null || data.length < 4+off || off < 0) {
            throw new EvioException("bad data arg");
        }

        if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
            return (
                (0xff & data[  off]) << 24 |
                (0xff & data[1+off]) << 16 |
                (0xff & data[2+off]) <<  8 |
                (0xff & data[3+off])
            );
        }
        else {
            return (
                (0xff & data[  off])       |
                (0xff & data[1+off]) <<  8 |
                (0xff & data[2+off]) << 16 |
                (0xff & data[3+off]) << 24
            );
        }
    }

    /**
     * Turn 4 bytes into an int.
     *
     * @param b1 1st byte
     * @param b2 2nd byte
     * @param b3 3rd byte
     * @param b4 4th byte
     * @param byteOrder if big endian, 1st byte is most significant &
     *                  4th is least (big endian if null)
     * @return int converted from byte array
     */
    public static int toInt(byte b1, byte b2, byte b3, byte b4, ByteOrder byteOrder) {

        if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
            return (
                (0xff & b1) << 24 |
                (0xff & b2) << 16 |
                (0xff & b3) <<  8 |
                (0xff & b4)
            );
        }
        else {
            return (
                (0xff & b1)       |
                (0xff & b2) <<  8 |
                (0xff & b3) << 16 |
                (0xff & b4) << 24
            );
        }
    }

    /**
     * Turn byte array into an int array.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @return int array converted from byte array
     * @throws EvioException if data is null or wrong size
     */
    public static int[] toIntArray(byte[] data, ByteOrder byteOrder) throws EvioException {
        if (data == null || data.length % 4 != 0) {
            throw new EvioException("bad data arg");
        }

        int indx;
        int[] ints = new int[data.length / 4];
        for (int i = 0; i < ints.length; i++) {
            indx = i*4;
            ints[i] = toInt(data[indx],
                            data[indx+1],
                            data[indx+2],
                            data[indx+3],
                            byteOrder);
        }
        return ints;
    }

    /**
     * Turn byte array into an int array.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @param dest array in which to write converted bytes
     * @param off offset into dest array
     * @throws EvioException if data is null or wrong size; dest is null, wrong size, or off < 0
     */
    public static void toIntArray(byte[] data, ByteOrder byteOrder, int[] dest, int off)
            throws EvioException {

        if (data == null || data.length % 4 != 0 ||
            dest == null || 4*dest.length < data.length+off || off < 0) {
            throw new EvioException("bad data arg");
        }

        for (int i = 0; i < data.length-3; i+=4) {
            dest[i/4+off] = toInt(data[i],
                                  data[i+1],
                                  data[i+2],
                                  data[i+3],
                                  byteOrder);
        }
    }

    // =========================

    /**
     * Turn section of byte array into a long.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @param off offset into data array
     * @return long converted from byte array
     * @throws EvioException if data is null or wrong size, or off is negative
     */
    public static long toLong(byte[] data, ByteOrder byteOrder, int off) throws EvioException {
        if (data == null || data.length < 8+off || off < 0) {
            throw new EvioException("bad data arg");
        }

        if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
            return (
                // Convert to longs before shift because digits
                // are lost with ints beyond the 32-bit limit
                (long)(0xff & data[  off]) << 56 |
                (long)(0xff & data[1+off]) << 48 |
                (long)(0xff & data[2+off]) << 40 |
                (long)(0xff & data[3+off]) << 32 |
                (long)(0xff & data[4+off]) << 24 |
                (long)(0xff & data[5+off]) << 16 |
                (long)(0xff & data[6+off]) <<  8 |
                (long)(0xff & data[7+off])
            );
        }
        else {
            return (
                (long)(0xff & data[  off])       |
                (long)(0xff & data[1+off]) <<  8 |
                (long)(0xff & data[2+off]) << 16 |
                (long)(0xff & data[3+off]) << 24 |
                (long)(0xff & data[4+off]) << 32 |
                (long)(0xff & data[5+off]) << 40 |
                (long)(0xff & data[6+off]) << 48 |
                (long)(0xff & data[7+off]) << 56
            );
        }
    }


    /**
     * Turn 8 bytes into a long.
     *
     * @param b1 1st byte
     * @param b2 2nd byte
     * @param b3 3rd byte
     * @param b4 4th byte
     * @param b5 5th byte
     * @param b6 6th byte
     * @param b7 7th byte
     * @param b8 8th byte
     * @param byteOrder if big endian, 1st byte is most significant &
     *                  8th least (big endian if null)
     * @return long converted from byte array
     */
    public static long toLong(byte b1, byte b2, byte b3, byte b4,
                              byte b5, byte b6, byte b7, byte b8, ByteOrder byteOrder) {

        if (byteOrder == null || byteOrder == ByteOrder.BIG_ENDIAN) {
            return (
                (long)(0xff & b1) << 56 |
                (long)(0xff & b2) << 48 |
                (long)(0xff & b3) << 40 |
                (long)(0xff & b4) << 32 |
                (long)(0xff & b5) << 24 |
                (long)(0xff & b6) << 16 |
                (long)(0xff & b7) <<  8 |
                (long)(0xff & b8)
            );
        }
        else {
            return (
                (long)(0xff & b1)       |
                (long)(0xff & b2) <<  8 |
                (long)(0xff & b3) << 16 |
                (long)(0xff & b4) << 24 |
                (long)(0xff & b5) << 32 |
                (long)(0xff & b6) << 40 |
                (long)(0xff & b7) << 48 |
                (long)(0xff & b8) << 56
            );
        }
    }


    /**
     * Turn byte array into a long array.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @return long array converted from byte array
     * @throws EvioException if data is null or wrong size
     */
    public static long[] toLongArray(byte[] data, ByteOrder byteOrder) throws EvioException {
        if (data == null || data.length % 8 != 0) {
            throw new EvioException("bad data arg");
        }

        int indx;
        long[] lngs = new long[data.length / 8];
        for (int i = 0; i < lngs.length; i++) {
            indx = i*8;
            lngs[i] = toLong(data[indx],
                             data[indx+1],
                             data[indx+2],
                             data[indx+3],
                             data[indx+4],
                             data[indx+5],
                             data[indx+6],
                             data[indx+7],
                             byteOrder);
        }
        return lngs;
    }

    /**
     * Turn byte array into an long array.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @param dest array in which to write converted bytes
     * @param off offset into dest array
     * @throws EvioException if data is null or wrong size; dest is null, wrong size, or off < 0
     */
    public static void toLongArray(byte[] data, ByteOrder byteOrder, long[] dest, int off)
            throws EvioException {

        if (data == null || data.length % 8 != 0 ||
            dest == null || 8*dest.length < data.length+off || off < 0) {
            throw new EvioException("bad data arg");
        }

        for (int i = 0; i < data.length-7; i+=8) {
            dest[i/8+off] = toLong(data[i],
                                   data[i + 1],
                                   data[i + 2],
                                   data[i + 3],
                                   data[i + 4],
                                   data[i + 5],
                                   data[i + 6],
                                   data[i + 7],
                                   byteOrder);
        }
    }

    // =========================

    /**
     * Turn section of byte array into a float.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @param off offset into data array
     * @return float converted from byte array
     * @throws EvioException if data is null or wrong size, or off is negative
     */
    public static float toFloat(byte[] data, ByteOrder byteOrder, int off) throws EvioException {
        return Float.intBitsToFloat(toInt(data, byteOrder, off));
    }

    /**
     * Turn 4 bytes into a float.
     *
     * @param b1 1st byte
     * @param b2 2nd byte
     * @param b3 3rd byte
     * @param b4 4th byte
     * @param byteOrder if big endian, 1st byte is most significant &
     *                  4th least (big endian if null)
     * @return float converted from byte array
     */
    public static float toFloat(byte b1, byte b2, byte b3, byte b4, ByteOrder byteOrder) {
        return Float.intBitsToFloat(toInt(b1,b2,b3,b4, byteOrder));
    }

    /**
     * Turn byte array into a float array.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @return float array converted from byte array
     * @throws EvioException if data is null or wrong size
     */
    public static float[] toFloatArray(byte[] data, ByteOrder byteOrder) throws EvioException {
        if (data == null || data.length % 4 != 0) {
            throw new EvioException("bad data arg");
        }

        int indx;
        float[] flts = new float[data.length / 4];
        for (int i = 0; i < flts.length; i++) {
            indx = i*4;
            flts[i] = toFloat(data[indx],
                              data[indx+1],
                              data[indx+2],
                              data[indx+3],
                              byteOrder
                             );
        }
        return flts;
    }

    /**
     * Turn byte array into an float array.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @param dest array in which to write converted bytes
     * @param off offset into dest array
     * @throws EvioException if data is null or wrong size; dest is null, wrong size, or off < 0
     */
    public static void toFloatArray(byte[] data, ByteOrder byteOrder, float[] dest, int off)
            throws EvioException {

        if (data == null || data.length % 4 != 0 ||
            dest == null || 4*dest.length < data.length+off || off < 0) {
            throw new EvioException("bad data arg");
        }

        for (int i = 0; i < data.length-3; i+=4) {
            dest[i/4+off] = toFloat(data[i],
                                    data[i + 1],
                                    data[i + 2],
                                    data[i + 3],
                                    byteOrder);
        }
    }

    // =========================

    /**
     * Turn section of byte array into a double.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @param off offset into data array
     * @return double converted from byte array
     * @throws EvioException if data is null or wrong size, or off is negative
     */
    public static double toDouble(byte[] data, ByteOrder byteOrder, int off) throws EvioException {
        return Double.longBitsToDouble(toLong(data, byteOrder, off));
    }

    /**
     * Turn 8 bytes into a double.
     *
     * @param b1 1st byte
     * @param b2 2nd byte
     * @param b3 3rd byte
     * @param b4 4th byte
     * @param b5 5th byte
     * @param b6 6th byte
     * @param b7 7th byte
     * @param b8 8th byte
     * @param byteOrder if big endian, 1st byte is most significant &
     *                  8th least (big endian if null)
     * @return double converted from byte array
     */
    public static double toDouble(byte b1, byte b2, byte b3, byte b4,
                                  byte b5, byte b6, byte b7, byte b8, ByteOrder byteOrder) {
        return Double.longBitsToDouble(toLong(b1,b2,b3,b4,b5,b6,b7,b8, byteOrder));
    }

    /**
     * Turn byte array into a double array.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @return double array converted from byte array
     * @throws EvioException if data is null or wrong size
     */
    public static double[] toDoubleArray(byte[] data, ByteOrder byteOrder) throws EvioException {
        if (data == null || data.length % 8 != 0) {
            throw new EvioException("bad data arg");
        }

        int indx;
        double[] dbls = new double[data.length / 8];
        for (int i=0; i < dbls.length; i++) {
            indx = i*8;
            dbls[i] = toDouble(data[indx],
                               data[indx+1],
                               data[indx+2],
                               data[indx+3],
                               data[indx+4],
                               data[indx+5],
                               data[indx+6],
                               data[indx+7],
                               byteOrder);
        }
        return dbls;
    }

    /**
     * Turn byte array into an double array.
     *
     * @param data byte array to convert
     * @param byteOrder byte order of supplied bytes (big endian if null)
     * @param dest array in which to write converted bytes
     * @param off offset into dest array
     * @throws EvioException if data is null or wrong size; dest is null, wrong size, or off < 0
     */
    public static void toDoubleArray(byte[] data, ByteOrder byteOrder, double[] dest, int off)
            throws EvioException {

        if (data == null || data.length % 8 != 0 ||
            dest == null || 8*dest.length < data.length+off || off < 0) {
            throw new EvioException("bad data arg");
        }

        for (int i = 0; i < data.length-7; i+=8) {
            dest[i/8+off] = toDouble(data[i],
                                     data[i + 1],
                                     data[i + 2],
                                     data[i + 3],
                                     data[i + 4],
                                     data[i + 5],
                                     data[i + 6],
                                     data[i + 7],
                                     byteOrder);
        }
    }


}
