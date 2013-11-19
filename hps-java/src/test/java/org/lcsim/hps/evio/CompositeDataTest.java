package org.lcsim.hps.evio;

import java.nio.ByteOrder;
import java.util.List;

import junit.framework.TestCase;

import org.jlab.coda.jevio.ByteDataTransformer;
import org.jlab.coda.jevio.CompositeData;
import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EvioException;

public class CompositeDataTest extends TestCase {

    public void testMe() {
        doIt();
    }

    public void doIt() {

        int[] bank = new int[24];

        /***********************/
        /* bank of tagsegments */
        /***********************/
        bank[0] = 23;                       // bank length
        bank[1] = 6 << 16 | 0xF << 8 | 3;   // tag = 6, bank contains composite type, num = 3

        // N(I,D,F,2S,8a)
        // first part of composite type (for format) = tagseg (tag & type ignored, len used)
        bank[2]  = 5 << 20 | 0x3 << 16 | 4; // tag = 5, seg has char data, len = 4
        // ASCII chars values in latest evio string (array) format, N(I,D,F,2S,8a) with N=2
        bank[3]  = 0x4E << 24 | 0x28 << 16 | 0x49 << 8 | 0x2C;    // N ( I ,
        bank[4]  = 0x44 << 24 | 0x2C << 16 | 0x46 << 8 | 0x2C;    // D , F ,
        bank[5]  = 0x32 << 24 | 0x53 << 16 | 0x2C << 8 | 0x38 ;   // 2 S , 8
        bank[6]  = 0x61 << 24 | 0x29 << 16 | 0x00 << 8 | 0x04 ;   // a ) \0 \4

        // second part of composite type (for data) = bank (tag, num, type ignored, len used)
        bank[7]  = 16;
        bank[8]  = 6 << 16 | 0xF << 8 | 1;
        bank[9]  = 0x2; // N
        bank[10] = 0x00001111; // I

        // Double
        double d = Math.PI * (-1.e-100);
        long  dl = Double.doubleToLongBits(d);
        bank[11] = (int) (dl >>> 32);    // higher 32 bits
        bank[12] = (int)  dl;            // lower 32 bits

        // Float
        float f = (float)(Math.PI*(-1.e-24));
        int  fi = Float.floatToIntBits(f);
        bank[13] = fi;

        bank[14] = 0x11223344; // 2S

        bank[15]  = 0x48 << 24 | 0x49 << 16 | 0x00 << 8 | 0x48;    // H  I \0  H
        bank[16]  = 0x4F << 24 | 0x00 << 16 | 0x04 << 8 | 0x04;    // 0 \ 0 \4 \4

        // duplicate data
        for (int i=0; i < 7; i++) {
            bank[17+i] = bank[10+i];
        }

        // all composite including headers
        int[] allData = new int[22];
        for (int i=0; i < 22; i++) {
            allData[i] = bank[i+2];
        }

        try {
            // change int array into byte array
            byte[] byteArray = ByteDataTransformer.toBytes(allData, ByteOrder.BIG_ENDIAN);

            // Create composite object
            CompositeData cData = new CompositeData(byteArray, ByteOrder.BIG_ENDIAN);

            // print out general data
            printCompositeDataObject(cData);
        }
        catch (EvioException e) {
            e.printStackTrace();
        }

    }


    /**
     * Print the data from a CompositeData object in a user-friendly form.
     * @param cData CompositeData object
     */
    public static void printCompositeDataObject(CompositeData cData) {

        // Get lists of data items & their types from composite data object
        List<Object> items = cData.getItems();
        List<DataType> types = cData.getTypes();

        // Use these list to print out data of unknown format
        DataType type;
        int len = items.size();
        for (int i=0; i < len; i++) {
            type =  types.get(i);
            System.out.print(String.format("type = %9s, val = ", type));
            switch (type) {
                case INT32:
                case UINT32:
                case UNKNOWN32:
                    int j = (Integer)items.get(i);
                    System.out.println("0x"+Integer.toHexString(j));
                    break;
                case LONG64:
                case ULONG64:
                    long l = (Long)items.get(i);
                    System.out.println("0x"+Long.toHexString(l));
                    break;
                case SHORT16:
                case USHORT16:
                    short s = (Short)items.get(i);
                    System.out.println("0x"+Integer.toHexString(s));
                    break;
                case CHAR8:
                case UCHAR8:
                    byte b = (Byte)items.get(i);
                    System.out.println("0x"+Integer.toHexString(b));
                    break;
                case FLOAT32:
                    float ff = (Float)items.get(i);
                    System.out.println(""+ff);
                    break;
                case DOUBLE64:
                    double dd = (Double)items.get(i);
                    System.out.println(""+dd);
                    break;
                case CHARSTAR8:
                    String[] strs = (String[])items.get(i);
                    for (String ss : strs) {
                        System.out.print(ss + ", ");
                    }
                    System.out.println();
                    break;
                default:
            }
        }

    }



}
