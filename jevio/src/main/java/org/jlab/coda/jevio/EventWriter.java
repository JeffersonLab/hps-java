package org.jlab.coda.jevio;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.BitSet;

/**
 * An EventWriter object is used for writing events to a file or to a byte buffer.
 * This class does NOT write versions 1-3 data, only version 4!
 * This class is threadsafe.
 *
 * @author heddle
 * @author timmer
 */
public class EventWriter {

    /**
	 * This <code>enum</code> denotes the status of a read. <br>
	 * SUCCESS indicates a successful read/write. <br>
	 * END_OF_FILE indicates that we cannot read because an END_OF_FILE has occurred. Technically this means that what
	 * ever we are trying to read is larger than the buffer's unread bytes.<br>
	 * EVIO_EXCEPTION indicates that an EvioException was thrown during a read, possibly due to out of range values,
	 * such as a negative start position.<br>
     * CANNOT_OPEN_FILE  that we cannot write because the destination file cannot be opened.<br>
	 * UNKNOWN_ERROR indicates that an unrecoverable error has occurred.
	 */
	public static enum IOStatus {
		SUCCESS, END_OF_FILE, EVIO_EXCEPTION, CANNOT_OPEN_FILE, UNKNOWN_ERROR
	}


    /**
     * Offset to where the block length is written in the byte buffer,
     * which always has a physical record header at the top.
     */
    private static int BLOCK_LENGTH_OFFSET = 0;

    /**
     * Offset to where the block number is written in the byte buffer,
     * which always has a physical record header at the top.
     */
    private static int BLOCK_NUMBER_OFFSET = 4;

    /**
     * Offset to where the header length is written in the byte buffer,
     * which always has a physical record header at the top.
     */
    private static int HEADER_LENGTH_OFFSET = 8;

    /**
     * Offset to where the event count is written in the byte buffer,
     * which always has a physical record header at the top.
     */
    private static int EVENT_COUNT_OFFSET = 12;

    /**
     * Offset to where the first reserved workd is written in the byte buffer,
     * which always has a physical record header at the top.
     */
    private static int RESERVED1_COUNT_OFFSET = 16;

    /**
     * Offset to where the bit information is written in the byte buffer,
     * which always has a physical record header at the top.
     */
    private static int BIT_INFO_OFFSET = 20;

    /**
     * Offset to where the magic number is written in the byte buffer,
     * which always has a physical record header at the top.
     */
    private static final int MAGIC_OFFSET = 28;

    /** Mask to get version number from 6th int in block. */
    private static final int VERSION_MASK = 0xff;

    /**
     * The default maximum size for a single block used for writing, in ints.
     * This gives block sizes of about 4MB. It is a soft limit since a single
     * event larger than this limit may need to be written.
     */
    private static int DEFAULT_BLOCK_SIZE = 1024000;

    /** The default maximum event count for a single block used for writing. */
    private static int DEFAULT_BLOCK_COUNT = 10000;

    /**
     * The upper limit of maximum size for a single block used for writing,
     * in ints. This gives block sizes of about 40MB. It is a soft limit since
     * a single event larger than this limit may need to be written.
     */
    private static int MAX_BLOCK_SIZE = 10240000;

    /** The upper limit of maximum event count for a single block used for writing. */
    private static int MAX_BLOCK_COUNT = 100000;

    /**
     * The lower limit of maximum size for a single block used for writing,
     * in ints. This gives block sizes of about 400 bytes. It is a soft limit since
     * a single event larger than this limit may need to be written.
     */
    private static int MIN_BLOCK_SIZE = 100;

    /** The lower limit of maximum event count for a single block used for writing. */
    private static int MIN_BLOCK_COUNT = 1;



    /**
     * Maximum block size for a single block (block header & following events).
     * There may be exceptions to this limit if the size of an individual
     * event exceeds this limit.
     * Default is {@link #DEFAULT_BLOCK_SIZE}.
     */
    private int blockSizeMax;

    /**
     * Maximum number of events in a block (events following a block header).
     * Default is {@link #DEFAULT_BLOCK_COUNT}.
     */
    private int blockCountMax;

    /** Running count of the block number. */
    private int blockNumber;

    /**
     * Size in bytes needed to write all the events in the <code>eventBufferHoldingList</code>
     * to file or in the <code>eventHoldingList</code> to buffer.
     */
    private int holdingListTotalBytes;

    /**
     * Dictionary to include in xml format in the first event of the first block
     * when writing the file.
     */
    private String xmlDictionary;

    /** The next write will include an xml dictionary. */
    private boolean writeDictionary;

    /**
     * Bit information in the block headers:<p>
     * <ul>
     * <li>Bit one: is the first event a dictionary?  Used in first block only.
     * <li>Bit two: is this the last block?
     * </ul>
     */
    private BitSet bitInfo;

    /** <code>True</code> if {@link #close()} was called, else <code>false</code>. */
    private boolean closed;

    /** <code>True</code> if writing to file, else <code>false</code>. */
    private boolean toFile;

    /** <code>True</code> if appending to file/buffer, <code>false</code> if (over)writing. */
    private boolean append;

    /** <code>True</code> if a last, empty block header has already been written. */
    private boolean lastBlockOut;

    /** Version 4 block header reserved int 1. Used by CODA for source ID in event building. */
    private int reserved1;

    /** Version 4 block header reserved int 2. */
    private int reserved2;

    /** Keep tabs on where we initially started. For files this is 0. */
    private int initialPosition;

    /** Number of events written to buffer or file (although may not be flushed yet). */
    private int eventsWritten;

    /** Contains an empty, last block header which is placed at the file/buffer end
     *  after each write. */
    private ByteBuffer emptyLastHeader;

    //-----------------------
    // File related members
    //-----------------------

    /** The byte order in which to write a file. */
    private ByteOrder byteOrder;

    /** The output stream used for writing a file. */
    private FileOutputStream fileOutputStream;

    /** The file channel, used for writing a file, derived from the output stream. */
    private FileChannel fileChannel;

    /**
     * This is an internal byte buffer corresponding to one block (physical record). When this gets full it
     * will be flushed to the external output stream (<code>dataOutputStream</code>) that represents the file.
     */
    private ByteBuffer blockBuffer;

    /**
     * List used to store events (in ByteBuffer form) temporarily before writing them to the file.
     * This is done to accumulate events while keeping track of the total memory needed to write them.
     * Once enough events are accumulated to at least fill a block, they are taken off this list,
     * placed in a block, and written to file. This way the blocks contain an integral number of events.
     */
    private LinkedList<ByteBuffer> eventBufferHoldingList = new LinkedList<ByteBuffer>();

    //-----------------------
    // Buffer related members
    //-----------------------

    /** The output buffer when writing to a buffer. */
    private ByteBuffer buffer;

    /** When writing to a buffer, keep tabs on the front of the last header written. */
    private int bufferHeaderPosition;

    /**
     * List used to store events (in EvioBank form) temporarily before writing them to a buffer.
     * This is done to accumulate events while keeping track of the total memory needed to write them.
     * Once enough events are accumulated to at least fill a block, they are taken off this list
     * and written to the buffer. This way the blocks contain an integral number of events.
     */
    private LinkedList<EvioBank> eventHoldingList = new LinkedList<EvioBank>();


    
    //---------------------------------------------
    // FILE Constructors
    //---------------------------------------------

    /**
     * Creates an <code>EventWriter</code> for writing to a file in native byte order.
     * If the file already exists, its contents will be overwritten.
     * If it doesn't exist, it will be created.
     *
     * @param file the file object to write to.<br>
     * @throws EvioException block size too small or file cannot be created
     */
    public EventWriter(File file) throws EvioException {
        this(file, false);
    }

    /**
     * Creates an <code>EventWriter</code> for writing to a file in native byte order.
     * If the file already exists, its contents will be overwritten unless
     * it is being appended to. If it doesn't exist, it will be created.
     *
     * @param file     the file object to write to.<br>
     * @param append   if <code>true</code> and the file already exists,
     *                 all events to be written will be appended to the
     *                 end of the file.
     *
     * @throws EvioException block size too small or file cannot be created
     */
    public EventWriter(File file, boolean append) throws EvioException {
        this(file, DEFAULT_BLOCK_SIZE, DEFAULT_BLOCK_COUNT,
             ByteOrder.nativeOrder(), null, null, true, append);
    }

    /**
     * Creates an <code>EventWriter</code> for writing to a file in native byte order.
     * If the file already exists, its contents will be overwritten unless
     * it is being appended to. If it doesn't exist, it will be created.
     *
     * @param file       the file object to write to.<br>
     * @param dictionary dictionary in xml format or null if none.
     * @param append     if <code>true</code> and the file already exists,
     *                   all events to be written will be appended to the
     *                   end of the file.
     *
     * @throws EvioException block size too small or file cannot be created
     */
    public EventWriter(File file, String dictionary, boolean append) throws EvioException {
        this(file, DEFAULT_BLOCK_SIZE, DEFAULT_BLOCK_COUNT,
             ByteOrder.nativeOrder(), dictionary, null, true, append);
    }

    /**
     * Creates an <code>EventWriter</code> for writing to a file in native byte order.
     * If the file already exists, its contents will be overwritten.
     * If it doesn't exist, it will be created.
     *
     * @param filename name of the file to write to.<br>
     * @throws EvioException block size too small or file cannot be created
     */
    public EventWriter(String filename) throws EvioException {
        this(filename, false);
    }

    /**
     * Creates an <code>EventWriter</code> for writing to a file in native byte order.
     * If the file already exists, its contents will be overwritten unless
     * it is being appended to. If it doesn't exist, it will be created.
     *
     * @param filename name of the file to write to.<br>
     * @param append   if <code>true</code> and the file already exists,
     *                 all events to be written will be appended to the
     *                 end of the file.
     *
     * @throws EvioException block size too small or file cannot be created
     */
    public EventWriter(String filename, boolean append) throws EvioException {
        this(new File(filename), DEFAULT_BLOCK_SIZE, DEFAULT_BLOCK_COUNT,
             ByteOrder.nativeOrder(), null, null, true, append);
    }

    /**
     * Creates an <code>EventWriter</code> for writing to a file in the
     * specified byte order.
     * If the file already exists, its contents will be overwritten unless
     * it is being appended to. If it doesn't exist, it will be created.
     *
     * @param filename  name of the file to write to.<br>
     * @param append    if <code>true</code> and the file already exists,
     *                  all events to be written will be appended to the
     *                  end of the file.
     * @param byteOrder the byte order in which to write the file.
     *
     * @throws EvioException block size too small or file cannot be created
     */
    public EventWriter(String filename, boolean append, ByteOrder byteOrder) throws EvioException {
        this(new File(filename), DEFAULT_BLOCK_SIZE, DEFAULT_BLOCK_COUNT,
             byteOrder, null, null, true, append);
    }

    /**
     * Create an <code>EventWriter</code> for writing events to a file.
     * If the file already exists, its contents will be overwritten.
     * If it doesn't exist, it will be created.
     *
     * @param file          the file object to write to.<br>
     * @param blockSizeMax  the max blocksize to use which must be >= {@link #MIN_BLOCK_SIZE}
     *                      and <= {@link #MAX_BLOCK_SIZE} ints.
     *                      The size of the block will not be larger than this size
     *                      unless a single event itself is larger.
     * @param blockCountMax the max number of events in a single block which must be
     *                      >= {@link #MIN_BLOCK_COUNT} and <= {@link #MAX_BLOCK_COUNT}.
     * @param byteOrder     the byte order in which to write the file.
     * @param xmlDictionary dictionary in xml format or null if none.
     * @param bitInfo       set of bits to include in first block header.
     *
     * @throws EvioException block size too small or file cannot be created
     */
    public EventWriter(File file, int blockSizeMax, int blockCountMax, ByteOrder byteOrder,
                       String xmlDictionary, BitSet bitInfo)
                                        throws EvioException {

        this(file, blockSizeMax, blockCountMax, byteOrder,
             xmlDictionary, bitInfo, true, false);
    }

    /**
     * Create an <code>EventWriter</code> for writing events to a file.
     * If the file already exists, its contents will be overwritten
     * unless the "overWriteOK" argument is <code>false</code> in
     * which case an exception will be thrown. If it doesn't exist,
     * it will be created.
     *
     * @param file          the file to write to.<br>
     * @param blockSizeMax  the max blocksize to use which must be >= {@link #MIN_BLOCK_SIZE}
     *                      and <= {@link #MAX_BLOCK_SIZE} ints.
     *                      The size of the block will not be larger than this size
     *                      unless a single event itself is larger.
     * @param blockCountMax the max number of events in a single block which must be
     *                      >= {@link #MIN_BLOCK_COUNT} and <= {@link #MAX_BLOCK_COUNT}.
     * @param byteOrder     the byte order in which to write the file.
     * @param xmlDictionary dictionary in xml format or null if none.
     * @param bitInfo       set of bits to include in first block header.
     * @param overWriteOK   if <code>false</code> and the file already exists,
     *                      an exception is thrown rather than overwriting it.
     *
     * @throws EvioException block size too small, file exists and cannot be deleted,
     *                       file exists and user requested no deletion.
     */
    public EventWriter(File file, int blockSizeMax, int blockCountMax, ByteOrder byteOrder,
                       String xmlDictionary, BitSet bitInfo, boolean overWriteOK)
                                        throws EvioException {

        this(file, blockSizeMax, blockCountMax, byteOrder,
             xmlDictionary, bitInfo, overWriteOK, false);
    }



    /**
     * Create an <code>EventWriter</code> for writing events to a file.
     * If the file already exists, its contents will be overwritten
     * unless the "overWriteOK" argument is <code>false</code> in
     * which case an exception will be thrown. Unless ..., the option to
     * append these events to an existing file is <code>true</code>,
     * in which case everything is fine. If the file doesn't exist,
     * it will be created. Byte order defaults to big endian if arg is null.
     *
     * @param file          the file to write to.<br>
     * @param blockSizeMax  the max blocksize to use which must be >= {@link #MIN_BLOCK_SIZE}
     *                      and <= {@link #MAX_BLOCK_SIZE} ints.
     *                      The size of the block will not be larger than this size
     *                      unless a single event itself is larger.
     * @param blockCountMax the max number of events in a single block which must be
     *                      >= {@link #MIN_BLOCK_COUNT} and <= {@link #MAX_BLOCK_COUNT}.
     * @param byteOrder     the byte order in which to write the file. This is ignored
     *                      if appending to existing file.
     * @param xmlDictionary dictionary in xml format or null if none.
     * @param bitInfo       set of bits to include in first block header.
     * @param overWriteOK   if <code>false</code> and the file already exists,
     *                      an exception is thrown rather than overwriting it.
     * @param append        if <code>true</code> and the file already exists,
     *                      all events to be written will be appended to the
     *                      end of the file.
     *
     * @throws EvioException if blockSizeMax or blockCountMax exceed limits;
     *                       if defined dictionary while appending;
     *                       if file arg is null;
     *                       if file could not be opened or positioned;
     *                       if file exists but user requested no over-writing or appending.
     *
     */
    public EventWriter(File file, int blockSizeMax, int blockCountMax, ByteOrder byteOrder,
                          String xmlDictionary, BitSet bitInfo, boolean overWriteOK,
                          boolean append) throws EvioException {

        if (blockSizeMax < MIN_BLOCK_SIZE || blockSizeMax > MAX_BLOCK_SIZE) {
            throw new EvioException("Block size arg must be bigger or smaller");
        }

        if (blockCountMax < MIN_BLOCK_COUNT || blockCountMax > MAX_BLOCK_COUNT) {
            throw new EvioException("Block count arg must be bigger or smaller");
        }

        if (file == null) {
            throw new EvioException("Null file arg");
        }

        if (byteOrder == null) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        toFile             = true;
        this.append        = append;
        // byte order may be overwritten if appending
        this.byteOrder     = byteOrder;
    	this.blockSizeMax  = blockSizeMax;
        this.blockCountMax = blockCountMax;
        this.xmlDictionary = xmlDictionary;

        blockNumber = 1;

        // For convenience, create buffer to hold a last, empty block header.
        emptyLastHeader = ByteBuffer.allocate(32);
        emptyLastHeader.order(byteOrder);
        emptyLastHeader.putInt(8); // block len, words
        emptyLastHeader.putInt(1); // block number
        emptyLastHeader.putInt(8); // header len, words
        emptyLastHeader.putInt(0); // event count
        emptyLastHeader.putInt(0); // reserved 1
        emptyLastHeader.putInt(0x204); // last block = true, version = 4
        emptyLastHeader.putInt(0); // reserved 2
        emptyLastHeader.putInt(0xc0da0100); // magic #
        emptyLastHeader.flip();


        if (bitInfo != null) {
            this.bitInfo = (BitSet)bitInfo.clone();
        }
        else {
            this.bitInfo = new BitSet(24);
        }

        if (xmlDictionary != null) {
            // Appending means not adding new dictionary
            if (append) {
                throw new EvioException("Cannot specify dictionary when appending");
            }
            this.bitInfo.set(0,true);
            writeDictionary = true;
        }

    	// If we can't overwrite or append and file exists, throw exception
    	if (!overWriteOK && !append && (file.exists() && file.isFile())) {
            throw new EvioException("File exists but user requested no over-writing or appending, "
                                            + file.getPath());
    	}

		try {
            if (append) {
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                fileChannel = raf.getChannel();

                // Something to read block headers into
                buffer = ByteBuffer.allocate(32);

                // Look at first block header to find endianness & version.
                // Endianness given in constructor arg, when appending, is ignored.
                examineFirstBufferHeader();

                // Oops, gotta redo this since file has different byte order
                // than specified in constructor arg.
                if (this.byteOrder != byteOrder) {
                    emptyLastHeader.clear();
                    emptyLastHeader.order(this.byteOrder);
                    emptyLastHeader.putInt(8); // block len, words
                    emptyLastHeader.putInt(1); // block number
                    emptyLastHeader.putInt(8); // header len, words
                    emptyLastHeader.putInt(0); // event count
                    emptyLastHeader.putInt(0); // reserved 1
                    emptyLastHeader.putInt(0x204); // last block = true, version = 4
                    emptyLastHeader.putInt(0); // reserved 2
                    emptyLastHeader.putInt(0xc0da0100); // magic #
                    emptyLastHeader.flip();
                }

                // Prepare for appending by moving file position
                toAppendPosition();

                // Create block buffer with default size.
                // This method will be called before each write.
                // Do this after examineFirstBufferHeader() so the
                // byte order is set properly. If appending, don't
                // increase the blockNumber since it was properly
                // set in toAppendPosition().
                getCleanBuffer(0,0,false);
            }
            else {
                fileOutputStream = new FileOutputStream(file, append);
                fileChannel = fileOutputStream.getChannel();

                // Create block buffer w/ default size & set blockNumber to 1
                getCleanBuffer(0,0,true);
            }
		}
        catch (FileNotFoundException e) {
            throw new EvioException("File could not be opened for writing, " +
                                            file.getPath(), e);
        }
        catch (IOException e) {
            throw new EvioException("File could not be positioned for appending, " +
                                            file.getPath(), e);
        }

    }


    //---------------------------------------------
    // BUFFER Constructors
    //---------------------------------------------

    /**
     * Create an <code>EventWriter</code> for writing events to a ByteBuffer.
     * Uses the default number and size of blocks in buffer. Will overwrite
     * any existing data in buffer!
     *
     * @param buf            the buffer to write to.
     * @throws EvioException if buf arg is null
     */
    public EventWriter(ByteBuffer buf) throws EvioException {

        this(buf, DEFAULT_BLOCK_SIZE, DEFAULT_BLOCK_COUNT, null, null, 0, false);
    }


    /**
     * Create an <code>EventWriter</code> for writing events to a ByteBuffer.
     * Uses the default number and size of blocks in buffer.
     *
     * @param buf            the buffer to write to.
     * @param append         if <code>true</code>, all events to be written will be
     *                       appended to the end of the buffer.
     * @throws EvioException if buf arg is null
     */
    public EventWriter(ByteBuffer buf, boolean append) throws EvioException {

        this(buf, DEFAULT_BLOCK_SIZE, DEFAULT_BLOCK_COUNT, null, null, 0, append);
    }


    /**
     * Create an <code>EventWriter</code> for writing events to a ByteBuffer.
     * Uses the default number and size of blocks in buffer.
     *
     * @param buf            the buffer to write to.
     * @param xmlDictionary  dictionary in xml format or null if none.
     * @param append         if <code>true</code>, all events to be written will be
     *                       appended to the end of the buffer.
     * @throws EvioException if buf arg is null
     */
    public EventWriter(ByteBuffer buf, String xmlDictionary, boolean append) throws EvioException {

        this(buf, DEFAULT_BLOCK_SIZE, DEFAULT_BLOCK_COUNT, xmlDictionary, null, 0, append);
    }


    /**
     * Create an <code>EventWriter</code> for writing events to a ByteBuffer.
     *
     * @param buf            the buffer to write to.
     * @param blockSizeMax   the max blocksize to use which must be >= {@link #MIN_BLOCK_SIZE}
     *                       and <= {@link #MAX_BLOCK_SIZE} ints.
     *                       The size of the block will not be larger than this size
     *                       unless a single event itself is larger.
     * @param blockCountMax  the max number of events in a single block which must be
     *                       >= {@link #MIN_BLOCK_COUNT} and <= {@link #MAX_BLOCK_COUNT}.
     * @param xmlDictionary  dictionary in xml format or null if none.
     * @param bitInfo        set of bits to include in first block header.
     * @throws EvioException if blockSizeMax or blockCountMax exceed limits; if buf arg is null
     */
    public EventWriter(ByteBuffer buf, int blockSizeMax, int blockCountMax,
                       String xmlDictionary, BitSet bitInfo) throws EvioException {

        this(buf, blockSizeMax, blockCountMax, xmlDictionary, bitInfo, 0, false);
    }


    /**
     * Create an <code>EventWriter</code> for writing events to a ByteBuffer.
     *
     * @param buf            the buffer to write to.
     * @param blockSizeMax   the max blocksize to use which must be >= {@link #MIN_BLOCK_SIZE}
     *                       and <= {@link #MAX_BLOCK_SIZE} ints.
     *                       The size of the block will not be larger than this size
     *                       unless a single event itself is larger.
     * @param blockCountMax  the max number of events in a single block which must be
     *                       >= {@link #MIN_BLOCK_COUNT} and <= {@link #MAX_BLOCK_COUNT}.
     * @param xmlDictionary  dictionary in xml format or null if none.
     * @param bitInfo        set of bits to include in first block header.
     * @param append         if <code>true</code>, all events to be written will be
     *                       appended to the end of the buffer.
     *
     * @throws EvioException if blockSizeMax or blockCountMax exceed limits;
     *                       if buf arg is null;
     *                       if defined dictionary while appending;
     */
    public EventWriter(ByteBuffer buf, int blockSizeMax, int blockCountMax,
                       String xmlDictionary, BitSet bitInfo,
                       boolean append) throws EvioException {

        this(buf, blockSizeMax, blockCountMax, xmlDictionary, bitInfo, 0, append);
    }

    /**
     * Create an <code>EventWriter</code> for writing events to a ByteBuffer.
     *
     * @param buf            the buffer to write to.
     * @param blockSizeMax   the max blocksize to use which must be >= {@link #MIN_BLOCK_SIZE}
     *                       and <= {@link #MAX_BLOCK_SIZE} ints.
     *                       The size of the block will not be larger than this size
     *                       unless a single event itself is larger.
     * @param blockCountMax  the max number of events in a single block which must be
     *                       >= {@link #MIN_BLOCK_COUNT} and <= {@link #MAX_BLOCK_COUNT}.
     * @param xmlDictionary  dictionary in xml format or null if none.
     * @param bitInfo        set of bits to include in first block header.
     * @param reserved1      set the value of the first "reserved" int in first block header.
     *                       NOTE: only CODA (i.e. EMU) software should use this.
     * @throws EvioException if blockSizeMax or blockCountMax exceed limits; if buf arg is null
     */
    public EventWriter(ByteBuffer buf, int blockSizeMax, int blockCountMax,
                       String xmlDictionary, BitSet bitInfo, int reserved1) throws EvioException {

        this(buf, blockSizeMax, blockCountMax, xmlDictionary, bitInfo, reserved1, false);
    }

    /**
     * Create an <code>EventWriter</code> for writing events to a ByteBuffer.
     *
     * @param buf            the buffer to write to.
     * @param blockSizeMax   the max blocksize to use which must be >= {@link #MIN_BLOCK_SIZE}
     *                       and <= {@link #MAX_BLOCK_SIZE} ints.
     *                       The size of the block will not be larger than this size
     *                       unless a single event itself is larger.
     * @param blockCountMax  the max number of events in a single block which must be
     *                       >= {@link #MIN_BLOCK_COUNT} and <= {@link #MAX_BLOCK_COUNT}.
     * @param xmlDictionary  dictionary in xml format or null if none.
     * @param bitInfo        set of bits to include in first block header.
     * @param reserved1      set the value of the first "reserved" int in first block header.
     *                       NOTE: only CODA (i.e. EMU) software should use this.
     * @param append         if <code>true</code>, all events to be written will be
     *                       appended to the end of the buffer.
     *
     * @throws EvioException if blockSizeMax or blockCountMax exceed limits;
     *                       if buf arg is null;
     *                       if defined dictionary while appending;
     */
    private EventWriter(ByteBuffer buf, int blockSizeMax, int blockCountMax,
                        String xmlDictionary, BitSet bitInfo, int reserved1,
                        boolean append) throws EvioException {

        initializeBuffer(buf, blockSizeMax, blockCountMax,
                         xmlDictionary, bitInfo, reserved1, append);
    }


    /**
     * Encapsulate constructor initialization for buffers.
     *
     * @param buf            the buffer to write to.
     * @param blockSizeMax   the max blocksize to use which must be >= {@link #MIN_BLOCK_SIZE}
     *                       and <= {@link #MAX_BLOCK_SIZE} ints.
     *                       The size of the block will not be larger than this size
     *                       unless a single event itself is larger.
     * @param blockCountMax  the max number of events in a single block which must be
     *                       >= {@link #MIN_BLOCK_COUNT} and <= {@link #MAX_BLOCK_COUNT}.
     * @param xmlDictionary  dictionary in xml format or null if none.
     * @param bitInfo        set of bits to include in first block header.
     * @param reserved1      set the value of the first "reserved" int in first block header.
     *                       NOTE: only CODA (i.e. EMU) software should use this.
     * @param append         if <code>true</code>, all events to be written will be
     *                       appended to the end of the buffer.
     *
     * @throws EvioException if blockSizeMax or blockCountMax exceed limits;
     *                       if buf arg is null;
     *                       if defined dictionary while appending;
     */
    private void initializeBuffer(ByteBuffer buf, int blockSizeMax, int blockCountMax,
                                  String xmlDictionary, BitSet bitInfo, int reserved1,
                                  boolean append) throws EvioException {

        if (blockSizeMax < MIN_BLOCK_SIZE) {
            throw new EvioException("Max block size arg (" + blockSizeMax + ") must be >= " +
                                     MIN_BLOCK_SIZE);
        }

        if (blockSizeMax > MAX_BLOCK_SIZE) {
            throw new EvioException("Max block size arg (" + blockSizeMax + ") must be <= " +
                                     MAX_BLOCK_SIZE);
        }

        if (blockCountMax < MIN_BLOCK_COUNT) {
            throw new EvioException("Max block count arg (" + blockCountMax + ") must be >= " +
                                     MIN_BLOCK_COUNT);
        }

        if (blockCountMax > MAX_BLOCK_COUNT) {
            throw new EvioException("Max block count arg (" + blockCountMax + ") must be <= " +
                                     MAX_BLOCK_COUNT);
        }

        if (buf == null) {
            throw new EvioException("Buffer arg cannot be null");
        }

        this.append        = append;
        this.buffer        = buf;
        this.byteOrder     = buf.order();
        this.reserved1     = reserved1;
        this.blockSizeMax  = blockSizeMax;
        this.blockCountMax = blockCountMax;
        this.xmlDictionary = xmlDictionary;

        // Init variables
        toFile = false;
        closed = false;
        blockNumber = 1;
        eventsWritten = 0;
        holdingListTotalBytes = 0;
        eventHoldingList.clear();
        bufferHeaderPosition = initialPosition = buf.position();

        if (bitInfo != null) {
            this.bitInfo = (BitSet)bitInfo.clone();
        }
        else {
            this.bitInfo = new BitSet(24);
        }

        if (xmlDictionary != null) {
            // Appending means not adding new dictionary
            if (append) {
                throw new EvioException("Cannot specify dictionary when appending");
            }
            this.bitInfo.set(0,true);
            writeDictionary = true;
        }

        // For convenience, create buffer to hold a last, empty block header.
        emptyLastHeader = ByteBuffer.allocate(32);
        emptyLastHeader.order(byteOrder);
        emptyLastHeader.putInt(8); // block len, words
        emptyLastHeader.putInt(1); // block number
        emptyLastHeader.putInt(8); // header len, words
        emptyLastHeader.putInt(0); // event count
        emptyLastHeader.putInt(0); // reserved 1
        emptyLastHeader.putInt(0x204); // last block = true, version = 4
        emptyLastHeader.putInt(0); // reserved 2
        emptyLastHeader.putInt(0xc0da0100); // magic #
        emptyLastHeader.flip();


        try {
            if (append) {
                // Check endianness & version
                examineFirstBufferHeader();

                // Oops, gotta redo this since buffer
                // has different byte order than specified.
                if (byteOrder != buf.order()) {
                    emptyLastHeader.clear();
                    emptyLastHeader.order(byteOrder);
                    emptyLastHeader.putInt(8); // block len, words
                    emptyLastHeader.putInt(1); // block number
                    emptyLastHeader.putInt(8); // header len, words
                    emptyLastHeader.putInt(0); // event count
                    emptyLastHeader.putInt(0); // reserved 1
                    emptyLastHeader.putInt(0x204); // last block = true, version = 4
                    emptyLastHeader.putInt(0); // reserved 2
                    emptyLastHeader.putInt(0xc0da0100); // magic #
                    emptyLastHeader.flip();
                }

                // Prepare for appending by moving buffer position
                toAppendPosition();
            }
        }
        catch (IOException e) {
            throw new EvioException("Buffer could not be positioned for appending", e);
        }
    }


    /**
     * Set the buffer being written into (initially set in constructor).
     * This method allows the user to avoid having to create a new EventWriter
     * each time a bank needs to be written to a different buffer.
     * This does nothing if writing to a file.<p>
     * Do <b>not</b> use this method unless you know what you are doing.
     *
     * @param buf the buffer to write to.
     * @throws EvioException if this object was not closed prior to resetting the buffer,
     *                       or buffer arg is null.
     */
    public void setBuffer(ByteBuffer buf) throws EvioException {
        if (toFile) return;
        if (!closed) {
            throw new EvioException("close EventWriter before changing buffers");
        }

        initializeBuffer(buf, blockSizeMax, blockCountMax,
                         xmlDictionary, bitInfo, reserved1, append);
    }

    /**
     * Get the buffer being written into. This was initially supplied by user in constructor.
     * This returns null if writing to a file.
     *
     * @return buffer being written into; null if writing to file.
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * Get the current block number.
     * Warning, this value may be changing.
     * @return the current block number.
     */
    public int getBlockNumber() {
        return blockNumber;
    }

    /**
     * Get the number of events written to a file/buffer.
     * (Although a particular event may not yet be flushed to the file/buffer).
     * @return number of events written to a file/buffer.
     */
    public int getEventsWritten() {
        return eventsWritten;
    }

    /**
     * Set the number with which to start block numbers.
     * This method does nothing if events have already been written.
     * @param startingBlockNumber  the number with which to start block numbers.
     */
    public void setStartingBlockNumber(int startingBlockNumber) {
        // If events have been written already, forget about it
        if (eventsWritten > 0) return;
        blockNumber = startingBlockNumber;
    }

    /**
     * If writing to a file, flush events waiting to be written,
     * close the underlying data output stream and with it the file.
     * If writing to a buffer, flush events waiting to be written.
     * If writing to a buffer, {@link #setBuffer(java.nio.ByteBuffer)}
     * can be called after this method to reset and reopen this object.
     *
     * @throws IOException if error writing to file
     * @throws EvioException if writing to buffer and not enough space
     */
    public void close() throws EvioException, IOException {
        if (toFile) {
            closeFile();
        }
        else {
            closeBuffer();
        }
    }

    /**
     * Write an event (bank) to the file in evio version 4 format in blocks.
     * Each block has an integral number of events. There are limits to the
     * number of events in each block and the total size of each block.
     * A dictionary passed to the constructor will be written as the very
     * first event and does <b>not</b> need to be explicitly written using
     * this method.
     *
     * @param bank the bank to write.
     * @throws IOException error writing to file
     * @throws EvioException if bank arg is null or not enough room in buffer
     */
    public void writeEvent(EvioBank bank) throws EvioException, IOException {
        if (bank == null) {
            throw new EvioException("Attempt to write null bank using EventWriter");
        }

        if (toFile) {
            writeEventToFile(bank);
        }
        else {
            writeEventToBuffer(bank);
        }
    }


    /**
     * Get a clean buffer. If none create one, else make sure the existing buffer is large enough,
     * then initialize it. This is called when a physical record is filled and flushed to the file.
     *
     * @param size size in bytes of block
     * @param eventCount number of events in block
     * @param incBlkNum  if true, increment block # at the end
     */
    private void getCleanBuffer(int size, int eventCount, boolean incBlkNum) {
    	if (blockBuffer == null) {
            // Allocate at least the max block size in order to minimize chances
            // of having to allocate a bigger buffer later. Remember there is only
            // one buffer that gets reused.
            int initialSize = size < 4*blockSizeMax ? 4*blockSizeMax : size;
    		blockBuffer = ByteBuffer.allocate(initialSize); // this buffer is backed by an array
    		blockBuffer.order(byteOrder);
    	}
    	else {
    		// make sure buffer is big enough (+ add extra 100K)
            if (size > blockBuffer.capacity()) {
                blockBuffer = ByteBuffer.allocate(size + 100000);
                blockBuffer.order(byteOrder);
            }
            blockBuffer.clear();
    	}

    	// Write header words, some of which may be
        // overwritten later when the values are determined.
		blockBuffer.putInt(size/4);        // actual size of data being written in ints
		blockBuffer.putInt(blockNumber); // incremental count of blocks
		blockBuffer.putInt(8);             // header size always 8
		blockBuffer.putInt(eventCount);    // number of events in block
		blockBuffer.putInt(reserved1);     // unused / sourceId for coda eventbuilding
		blockBuffer.putInt(BlockHeaderV4.generateSixthWord(bitInfo)); // version = 4 & bit info stored
		blockBuffer.putInt(reserved2);     // unused
		blockBuffer.putInt(IBlockHeader.MAGIC_NUMBER); // MAGIC_NUMBER

        if (incBlkNum) blockNumber++;
    }


    /**
     * Reads part of the first block (physical record) header in order to determine
     * the evio version # and endianness of the file or buffer in question. These things
     * do <b>not</b> need to be examined in subsequent block headers.
     *
     * @return status of read attempt
     */
    protected synchronized IOStatus examineFirstBufferHeader()
            throws IOException, EvioException {

        // Only for append mode
        if (!append) {
            // Internal logic error, should never have gotten here
            throw new EvioException("need to be in append mode");
        }

        int nBytes, currentPosition;

        if (toFile) {
            buffer.clear();
            // This read advances fileChannel position
            nBytes = fileChannel.read(buffer);
            // Check to see if we read the whole header
            if (nBytes != 32) {
                throw new EvioException("bad file format");
            }
            currentPosition = 0;
            fileChannel.position(0);
        }
        else {
            // Have enough remaining bytes to read?
            if (buffer.remaining() < 32) {
                return IOStatus.END_OF_FILE;
            }
            currentPosition = buffer.position();
        }

        try {
            // Set the byte order to match the buffer/file's ordering.

            // Check the magic number for endianness. This requires
            // peeking ahead 7 ints or 28 bytes. Set the endianness
            // once we figure out what it is (buffer defaults to big endian).
            byteOrder = buffer.order();
            int magicNumber = buffer.getInt(currentPosition + MAGIC_OFFSET);
//System.out.println("ERROR: magic # = " + Integer.toHexString(magicNumber));

            if (magicNumber != IBlockHeader.MAGIC_NUMBER) {
                if (byteOrder == ByteOrder.BIG_ENDIAN) {
                    byteOrder = ByteOrder.LITTLE_ENDIAN;
                }
                else {
                    byteOrder = ByteOrder.BIG_ENDIAN;
                }
                buffer.order(byteOrder);

                // Reread magic number to make sure things are OK
                magicNumber = buffer.getInt(currentPosition + MAGIC_OFFSET);
//System.out.println("Re read magic # = " + Integer.toHexString(magicNumber));
                if (magicNumber != IBlockHeader.MAGIC_NUMBER) {
System.out.println("ERROR: reread magic # (" + magicNumber + ") & still not right");
                    return IOStatus.EVIO_EXCEPTION;
                }
            }

            // Check the version number
            int bitInfo = buffer.getInt(currentPosition + BIT_INFO_OFFSET);
            int evioVersion = bitInfo & VERSION_MASK;
            if (evioVersion < 4)  {
System.out.println("ERROR: evio version# = " + evioVersion);
                return IOStatus.EVIO_EXCEPTION;
            }

//            int blockLen   = buffer.getInt(currentPosition + BLOCK_LENGTH_OFFSET);
//            int headerLen  = buffer.getInt(currentPosition + HEADER_LENGTH_OFFSET);
//            int eventCount = buffer.getInt(currentPosition + EVENT_COUNT_OFFSET);
//            int blockNum   = buffer.getInt(currentPosition + BLOCK_NUMBER_OFFSET);
//
//            boolean lastBlock = BlockHeaderV4.isLastBlock(bitInfo);
//
//            System.out.println("blockLength     = " + blockLen);
//            System.out.println("blockNumber     = " + blockNum);
//            System.out.println("headerLength    = " + headerLen);
//            System.out.println("blockEventCount = " + eventCount);
//            System.out.println("lastBlock       = " + lastBlock);
//            System.out.println("bit info        = " + Integer.toHexString(bitInfo));
//            System.out.println();

        }
        catch (BufferUnderflowException a) {
System.err.println("ERROR endOfBuffer " + a);
            return IOStatus.UNKNOWN_ERROR;
        }

        return IOStatus.SUCCESS;
    }


    /**
     * This method positions a file or buffer for the first {@link #writeEvent(EvioBank)}
     * in append mode. It makes sure that the last block header is an empty one
     * with its "last block" bit set.
     *
     * @throws IOException     if file reading/writing problems
     * @throws EvioException   if bad file/buffer format; if not in append mode
     */
    private void toAppendPosition() throws EvioException, IOException {

        // Only for append mode
        if (!append) {
            // Internal logic error, should never have gotten here
            throw new EvioException("need to be in append mode");
        }

        boolean lastBlock;
        int blockLength, blockNum, blockEventCount;
        int nBytes, bitInfo, headerLength, currentPosition;

        // The file's block #s may be fine or they may be messed up.
        // Assume they start with one and increment from there. That
        // way any additional blocks now added to the file will have a
        // reasonable # instead of incrementing from the last existing
        // block.
        blockNumber = 1;

        while (true) {
            // Read in 8 ints (32 bytes) of block header
            if (toFile) {
                buffer.clear();
                // This read advances fileChannel position
//System.out.println("toAppendPosition: (before read) file pos = " + fileChannel.position());
//System.out.println("Read buffer: pos = " + buffer.position() +
//                           ", limit = " + buffer.limit());
                nBytes = fileChannel.read(buffer);
//System.out.println("nBytes = " + nBytes);
                // Check to see if we read the whole header
                if (nBytes != 32) {
                    throw new EvioException("bad file format");
                }
                currentPosition = 0;
            }
            else {
//System.out.println("toAppendPosition: pos = " + buffer.position() +
//                                           ", limit = " + buffer.limit());
                // Have enough remaining bytes to read?
                if (buffer.remaining() < 32) {
                    throw new EvioException("bad buffer format");
                }
                currentPosition = buffer.position();
            }

            bitInfo         = buffer.getInt(currentPosition + BIT_INFO_OFFSET);
            blockLength     = buffer.getInt(currentPosition + BLOCK_LENGTH_OFFSET);
            blockNum        = buffer.getInt(currentPosition + BLOCK_NUMBER_OFFSET);
            headerLength    = buffer.getInt(currentPosition + HEADER_LENGTH_OFFSET);
            blockEventCount = buffer.getInt(currentPosition + EVENT_COUNT_OFFSET);
            lastBlock       = BlockHeaderV4.isLastBlock(bitInfo);

//            System.out.println("bitInfo         = 0x" + Integer.toHexString(bitInfo));
//            System.out.println("blockLength     = " + blockLength);
//            System.out.println("blockNumber     = " + blockNum);
//            System.out.println("headerLength    = " + headerLength);
//            System.out.println("blockEventCount = " + blockEventCount);
//            System.out.println("lastBlock       = " + lastBlock);
//            System.out.println();

            // Track total number of events in file/buffer
            eventsWritten += blockEventCount;
            blockNumber++;

            // Stop at the last block
            if (lastBlock) {
                break;
            }

            // Hop to next block header
            if (toFile) {
                fileChannel.position(fileChannel.position() + 4*blockLength - 32);
            }
            else {
                // Is there enough buffer space to hop over block?
                if (buffer.remaining() < 4*blockLength) {
                    throw new EvioException("bad buffer format");
                }

                buffer.position(buffer.position() + 4*blockLength);
            }
        }

//        if (toFile)  {
//System.out.println("position       = " + fileChannel.position());
//        }
//        else {
//System.out.println("position       = " + buffer.position());
//        }

        //-------------------------------------------------------------------------------
        // If we're here, we've just read the last block header (at least 8 words of it).
        // Easiest to rewrite it in our own image at this point. But first we
        // must check to see if the last block contains data. If it does, we
        // change a bit so it's not labeled as the last block. Then we write
        // an empty last block after the data. If, on the other hand, this
        // last block contains no data, just write over it with a new one.
        //-------------------------------------------------------------------------------

        // If last block has event(s) in it ...
        if (blockLength > headerLength) {
            // Clear last block bit in 6th header word
            bitInfo = BlockHeaderV4.clearLastBlockBit(bitInfo);

            // Rewrite header word with new bit info & hop over block

            // File now positioned right after the last header to be read
            if (toFile) {
                // Back up to before 6th block header word
                fileChannel.position(fileChannel.position() - (32 - BIT_INFO_OFFSET));
//System.out.println("toAppendPosition: writing over last block's 6th word, back up %d words" +(8 - 6));

                // Write over 6th block header word
                buffer.clear();
                buffer.putInt(bitInfo);
                buffer.flip();
                nBytes = fileChannel.write(buffer);
                if (nBytes != 4) {
                    throw new EvioException("file writing error");
                }

                // Hop over the entire block
//System.out.println("toAppendPosition: wrote over last block's 6th word, hop over %d words" +
// (blockLength - (6 + 1)));
                fileChannel.position(fileChannel.position() + 4 * blockLength - (BIT_INFO_OFFSET + 1));
            }
            // Buffer still positioned right before the last header to be read
            else {
//System.out.println("toAppendPosition: writing bitInfo (" +
//                   Integer.toHexString(bitInfo) +  ") over last block's 6th word for buffer at pos " +
//                   (buffer.position() + BIT_INFO_OFFSET));
                // Write over 6th block header word
                buffer.putInt(buffer.position() + BIT_INFO_OFFSET, bitInfo);

                // Hop over the entire block
                buffer.position(buffer.position() + 4*blockLength);
            }
        }
        else {
            // We already read in the block header, now back up so we can overwrite it.
            // If using buffer, we never incremented the position, so we're OK.
            blockNumber--;
            if (toFile) {
                fileChannel.position(fileChannel.position() - 32);
//System.out.println("toAppendPos: position (bkup) = " + fileChannel.position());
            }
        }

        // Write empty last block header. Thus if our program crashes, the file
        // will be OK. This last block header will be over-written with each
        // subsequent write/flush.
        if (toFile) {
//System.out.println("toAppendPos: write last blk header");
//System.out.println("toAppendPos: use block # = " + blockNumber);
        }
        else {
            // Have enough remaining bytes to read?
            if (buffer.remaining() < 32) {
                throw new EvioException("bad buffer format");
            }
        }

        writeEmptyLastBlockHeader(blockNumber);

        // We should now be in a state identical to that if we had
        // just now written everything currently in the file/buffer.
//System.out.println("toAppendPos: at END, blockNum = " + blockNumber);
    }


    /**
     * Close the underlying data output stream, and with it the file.
     *
     * @throws IOException if error writing to file
     */
    synchronized private void closeFileOrig() throws IOException {

        int headerBytes = 32;
        int requiredBufferByteSize = headerBytes;

        // write any remaining events, if there are any to be written
        if (eventBufferHoldingList.size() > 0) {

            requiredBufferByteSize = holdingListTotalBytes + headerBytes;

            if (writeDictionary) {
                // don't include dictionary in event count
                getCleanBuffer(requiredBufferByteSize, eventBufferHoldingList.size()-1, true);
                updateBitInfo(BlockHeaderV4.generateSixthWord(bitInfo, true, true));
                // after we write the header & dictionary, reset dictionary bit
                bitInfo.set(0, false);
            }
            else {
                getCleanBuffer(requiredBufferByteSize, eventBufferHoldingList.size(), true);
                updateBitInfo(BlockHeaderV4.generateSixthWord(bitInfo, false, true));
            }

            writeDictionary = false;

            // write events being stored
            for (ByteBuffer buf : eventBufferHoldingList) {
                blockBuffer.put(buf);
            }
        }
        // if there are no remaining events ...
        else {
            // write an empty header with the "isEnd" bit set to true
            getCleanBuffer(headerBytes, 0, true);
            updateBitInfo(BlockHeaderV4.generateSixthWord(bitInfo, false, true));
        }

        // write header and possibly events to file
        fileOutputStream.write(blockBuffer.array(), 0, requiredBufferByteSize);

        // finish writing file
        fileOutputStream.flush();
        fileOutputStream.close();

        closed = true;
    }

    /**
     * Close the underlying data output stream, and with it the file.
     *
     * @throws IOException if error writing to file
     */
    synchronized private void closeFile() throws IOException {

        int headerBytes = 32;
        int requiredBufferByteSize;

        // write any remaining events, if there are any to be written
        if (eventBufferHoldingList.size() > 0) {

            requiredBufferByteSize = holdingListTotalBytes + headerBytes;

            if (writeDictionary) {
                // don't include dictionary in event count
                getCleanBuffer(requiredBufferByteSize, eventBufferHoldingList.size()-1, true);
                updateBitInfo(BlockHeaderV4.generateSixthWord(bitInfo, true, false));
                // after we write the header & dictionary, reset dictionary bit
                bitInfo.set(0, false);
            }
            else {
                getCleanBuffer(requiredBufferByteSize, eventBufferHoldingList.size(), true);
                updateBitInfo(BlockHeaderV4.generateSixthWord(bitInfo, false, false));
            }

            writeDictionary = false;

            // write events being stored
            for (ByteBuffer buf : eventBufferHoldingList) {
                blockBuffer.put(buf);
            }
            blockBuffer.flip();

            // write events to file
//System.out.println("CLOSE: will write event with blockNum = " + (blockNumber-1));
            fileChannel.write(blockBuffer);
//System.out.println("CLOSE: finish writing events, then write ending header into file, blk # = " + blockNumber);

            // Write out ending empty block header.
            writeEmptyLastBlockHeader(blockNumber);
        }
        // if there are no remaining events and a last block header has not been written ...
        else if (!lastBlockOut) {
//System.out.println("CLOSE: write ending header into file, blk # = " + blockNumber);
            writeEmptyLastBlockHeader(blockNumber);
        }
        else {
//System.out.println("CLOSE: don't need to write ending header into file, already done");
        }

        // finish writing file
        fileChannel.close();

        closed = true;
    }

    /**
     * Write an event (bank) to the file in evio version 4 format in blocks.
     * Each block has an integral number of events. There are limits to the
     * number of events in each block and the total size of each block.
     * A dictionary passed to the constructor will be written as the very
     * first event and does <b>not</b> need to be explicitly written using
     * this method.
     *
     * @param bank the bank to write.
     * @throws IOException error writing to file
     */
    synchronized private void writeEventToFileOrig(EvioBank bank) throws IOException {

        if (closed) {
            throw new IOException("file was closed");
        }

        boolean gotDictionary = false;

        // If first call to this method, do some 1-time initialization
        if (blockBuffer == null) {
            getCleanBuffer(0,0, true);
            // If we've defined a dictionary, we'll make
            // that the very first event we write.
            if (xmlDictionary != null) {
                gotDictionary = true;
                writeDictionary = true;
            }
        }

        // See how much space the event will take up and get it ready (turn into ByteBuffer).
    	int currentEventBytes = bank.getTotalBytes();
    	ByteBuffer currentEventBuffer = ByteBuffer.allocate(currentEventBytes);
    	currentEventBuffer.order(byteOrder);
    	bank.write(currentEventBuffer);
        // reset to top of event buffer
        currentEventBuffer.position(0);

        //-----------------------------------------------------------------------------------
        // Do we write to block buffer & file now or do we put the event on the holding list?
        //
        // The basic principle here is that if the block buffer has blockCountMax (200) or
        // more events, it gets written. And if the block buffer would exceed is memory limit
        // (4*blockSizeMax bytes) if another event were to be added, it gets written.
        // The only condition in which a block would be bigger than this memory limit is if
        // a single event itself is bigger than that limit.
        //-----------------------------------------------------------------------------------

        // header size in bytes
        int headerBytes = 32;
        // aim for this amount of data in each block (in bytes)
        int roomForData = 4*blockSizeMax - headerBytes;

        int requiredBufferBytes;

        // If we're writing a dictionary, it must be the first event in the first block.
        if (gotDictionary) {
            // make a bank out of the dictionary
            EvioBank dictBank = new EvioBank(0, DataType.CHARSTAR8, 0);
            try {
                dictBank.appendStringData(xmlDictionary);
            }
            catch (EvioException e) { /* never happen */ }

            // put that into a buffer
            int dictEventBytes = dictBank.getTotalBytes();
            ByteBuffer dictEventBuffer = ByteBuffer.allocate(dictEventBytes);
            dictEventBuffer.order(byteOrder);
            dictBank.write(dictEventBuffer);
            dictEventBuffer.position(0);

            // add bank as first in list of events to be written
            eventBufferHoldingList.addFirst(dictEventBuffer);
            holdingListTotalBytes += dictEventBytes;
        }


        // If we have enough to overfill a block, write out what we've stored up
        if (eventBufferHoldingList.size() > 0 &&
                (holdingListTotalBytes + currentEventBytes > roomForData)) {

            // write out full block minus current event
            requiredBufferBytes = holdingListTotalBytes + headerBytes;

            if (writeDictionary) {
                // don't include dictionary in event count
                getCleanBuffer(requiredBufferBytes, eventBufferHoldingList.size()-1, true);
                // after we write the header & dictionary, reset dictionary bit
                bitInfo.set(0, false);
            }
            else {
                getCleanBuffer(requiredBufferBytes, eventBufferHoldingList.size(), true);
            }

            // write events being stored to buffer
            for (ByteBuffer buf : eventBufferHoldingList) {
                blockBuffer.put(buf);
            }

            // write buffered events to file
            fileOutputStream.write(blockBuffer.array(), 0, requiredBufferBytes);

            // reset after writing done
            writeDictionary = false;
            eventBufferHoldingList.clear();
            holdingListTotalBytes = 0;
        }

        // If current event is >= than the block limit ...
        if (currentEventBytes >= roomForData) {
            // We only get here if this is the only event - all else is written.
            // Write out only current event
            requiredBufferBytes = currentEventBytes + headerBytes;
            getCleanBuffer(requiredBufferBytes, 1, true);

            blockBuffer.put(currentEventBuffer);
        }
        else {
            // add this event to holding list
            eventBufferHoldingList.add(currentEventBuffer);
            holdingListTotalBytes += currentEventBytes;

            // write out events only if we've reached the count limit
            if (eventBufferHoldingList.size() < blockCountMax) {
                return;
            }

            requiredBufferBytes = holdingListTotalBytes + headerBytes;

            if (writeDictionary) {
                // don't include dictionary in event count
                getCleanBuffer(requiredBufferBytes, eventBufferHoldingList.size()-1, true);
                // after we write the header & dictionary, reset dictionary bit
                bitInfo.set(0, false);
            }
            else {
                getCleanBuffer(requiredBufferBytes, eventBufferHoldingList.size(), true);
            }

            // write events being stored
            for (ByteBuffer buf : eventBufferHoldingList) {
                blockBuffer.put(buf);
            }
        }

        // write events to file
        fileOutputStream.write(blockBuffer.array(), 0, requiredBufferBytes);

        // reset after writing done
        writeDictionary = false;
        eventBufferHoldingList.clear();
        holdingListTotalBytes = 0;
	}

    /**
     * Write an event (bank) to the file in evio version 4 format in blocks.
     * Each block has an integral number of events. There are limits to the
     * number of events in each block and the total size of each block.
     * A dictionary passed to the constructor will be written as the very
     * first event and does <b>not</b> need to be explicitly written using
     * this method.
     *
     * @param bank the bank to write.
     * @throws IOException error writing to file
     */
    synchronized private void writeEventToFile(EvioBank bank) throws IOException {

        if (closed) {
            throw new IOException("file was closed");
        }

        // See how much space the event will take up and get it ready (turn into ByteBuffer).
    	int currentEventBytes = bank.getTotalBytes();
    	ByteBuffer currentEventBuffer = ByteBuffer.allocate(currentEventBytes);
    	currentEventBuffer.order(byteOrder);
    	bank.write(currentEventBuffer);
        // get event buffer reading for writing
        currentEventBuffer.flip();
// TODO:Why not add it to block buffer now and dispense with currentEventBuffer??
        //-----------------------------------------------------------------------------------
        // Do we write to block buffer & file now or do we put the event on the holding list?
        //
        // The basic principle here is that if the block buffer has blockCountMax or
        // more events, it gets written. And if the block buffer would exceed is memory limit
        // (4*blockSizeMax bytes) if another event were to be added, it gets written.
        // The only condition in which a block would be bigger than this memory limit is if
        // a single event itself is bigger than that limit.
        //-----------------------------------------------------------------------------------

        // header size in bytes
        int headerBytes = 32;
        // aim for this amount of data in each block (in bytes)
        int roomForData = 4*blockSizeMax - headerBytes;

        int requiredBufferBytes;

        // If we're writing a dictionary, it must be the first event in the first block.
        if (writeDictionary && eventsWritten < 1) {
            // make a bank out of the dictionary
            EvioBank dictBank = new EvioBank(0, DataType.CHARSTAR8, 0);
            try {
                dictBank.appendStringData(xmlDictionary);
            }
            catch (EvioException e) { /* never happen */ }

            // put that into a buffer
            int dictEventBytes = dictBank.getTotalBytes();
            ByteBuffer dictEventBuffer = ByteBuffer.allocate(dictEventBytes);
            dictEventBuffer.order(byteOrder);
            dictBank.write(dictEventBuffer);
            dictEventBuffer.position(0);

            // add bank as first in list of events to be written
            eventBufferHoldingList.addFirst(dictEventBuffer);
            holdingListTotalBytes += dictEventBytes;
        }


        // If we have enough to overfill a block, write out what we've stored up
        if (eventBufferHoldingList.size() > 0 &&
                (holdingListTotalBytes + currentEventBytes > roomForData)) {

            // write out full block minus current event
            requiredBufferBytes = holdingListTotalBytes + headerBytes;

            if (writeDictionary) {
                // don't include dictionary in event count
                getCleanBuffer(requiredBufferBytes, eventBufferHoldingList.size()-1, true);
                // after we write the header & dictionary, reset dictionary bit
                bitInfo.set(0, false);
            }
            else {
                getCleanBuffer(requiredBufferBytes, eventBufferHoldingList.size(), true);
            }

            // write events being stored to buffer
            for (ByteBuffer buf : eventBufferHoldingList) {
                blockBuffer.put(buf);
            }
//System.out.println("writeEventToFile: write out what's stored on list, blockNum = " + blockNumber);

            // write buffered events to file
            blockBuffer.flip();
            fileChannel.write(blockBuffer);

            // reset after writing done
            writeDictionary = false;
            eventBufferHoldingList.clear();
            holdingListTotalBytes = 0;
        }

        // If current event is >= than the block limit ...
        if (currentEventBytes >= roomForData) {
            // We only get here if this is the only event - all else is written.
            // Write out only current event
            requiredBufferBytes = currentEventBytes + headerBytes;
            getCleanBuffer(requiredBufferBytes, 1, true);

            blockBuffer.put(currentEventBuffer);
        }
        else {
            // add this event to holding list
            eventBufferHoldingList.add(currentEventBuffer);
            holdingListTotalBytes += currentEventBytes;

            // write out events only if we've reached the count limit
            if (eventBufferHoldingList.size() < blockCountMax) {
//System.out.println("writeEventToFile: store ev on list, blockNum = " + blockNumber);
                eventsWritten++;
                return;
            }

            requiredBufferBytes = holdingListTotalBytes + headerBytes;

            if (writeDictionary) {
                // don't include dictionary in event count
                getCleanBuffer(requiredBufferBytes, eventBufferHoldingList.size()-1, true);
                // after we write the header & dictionary, reset dictionary bit
                bitInfo.set(0, false);
            }
            else {
                getCleanBuffer(requiredBufferBytes, eventBufferHoldingList.size(), true);
            }

            // write events being stored
            for (ByteBuffer buf : eventBufferHoldingList) {
                blockBuffer.put(buf);
            }
        }

        // write events to file
        blockBuffer.flip();
        fileChannel.write(blockBuffer);

        // Write out ending empty block header - in case app crashes, file is good.
        // File channel position is set to just before final block header so it gets
        // overwritten in next write.
        writeEmptyLastBlockHeader(blockNumber);

        // reset after writing done
        eventsWritten++;
        writeDictionary = false;
        eventBufferHoldingList.clear();
        holdingListTotalBytes = 0;
	}

    /**
     * This routine writes an empty, last block (just the header).
     * This ensures that if the data taking software crashes in the mean time,
     * we still have a file or buffer in the proper format.
     * Make sure that the "last block" bit is set. If we're not closing,
     * the block header simply gets over written in the next write.
     * The position of the buffer or file channel is not changed in preparation
     * for writing over the last block header.
     *
     * @param blockNum block number to use in header
     *
     * @throws IOException if problems writing to file
     */
    private void writeEmptyLastBlockHeader(int blockNum) throws IOException {

        // Set block number
        emptyLastHeader.putInt(BLOCK_NUMBER_OFFSET, blockNum);

        // Write block header to file/buf. This last block is always 8 ints long.
        // The regular block header may be larger but here we need only the
        // minimal length as a place holder for the next flush or as a marker
        // of the last block.
        if (toFile) {
            long originalPosition = fileChannel.position();
            fileChannel.write(emptyLastHeader);
            fileChannel.position(originalPosition);

//            ByteBuffer buf = ByteBuffer.allocate(32);
//            buf.order(byteOrder);
//System.out.println("writeEmptyLastBlockHeader: file pos = " + fileChannel.position());
//            fileChannel.read(buf);
//            int bitInfo         = buf.getInt(BIT_INFO_OFFSET);
//            int blockLength     = buf.getInt(BLOCK_LENGTH_OFFSET);
//            int blockNumber2    = buf.getInt(BLOCK_NUMBER_OFFSET);
//            int headerLength    = buf.getInt(HEADER_LENGTH_OFFSET);
//            int blockEventCount = buf.getInt(EVENT_COUNT_OFFSET);
//            boolean lastBlock   = BlockHeaderV4.isLastBlock(bitInfo);
//
//            System.out.println("bitInfo         = 0x" + Integer.toHexString(bitInfo));
//            System.out.println("blockLength     = " + blockLength);
//            System.out.println("blockNumber     = " + blockNumber2);
//            System.out.println("headerLength    = " + headerLength);
//            System.out.println("blockEventCount = " + blockEventCount);
//            System.out.println("lastBlock       = " + lastBlock);
//            System.out.println();
//            fileChannel.position(originalPosition);
        }
        else {
            int originalPosition = buffer.position();
            buffer.put(emptyLastHeader.array(), 0, 32);
            buffer.position(originalPosition);
        }
        emptyLastHeader.position(0);

        // Remember that a "last" block was written out (perhaps to be
        // overwritten for files or buffers if evWrite called again).
        lastBlockOut = true;

        return;
    }

    /**
     * Write a header into a given buffer.
     *
     * @param size size in bytes of block
     * @param eventCount number of events in block
     * @throws EvioException if not enough space in buffer
     */
    private void writeNewHeader(int size, int eventCount) throws EvioException {

        // if no room left for a header to be written ...
        if (buffer.remaining() < 32) {
            throw new EvioException("No more buffer size exceeded");
        }

        // record where beginning of header is so we can go back and update some items
        bufferHeaderPosition = buffer.position();

//System.out.println("EventWriter (header): size = " + (size/4) +
//        ", block# = " + blockNumber + ", ev Cnt = " + eventCount +
//        ", reserv1 = " + reserved1 + ", 6th wd = " +
//        BlockHeaderV4.generateSixthWord(bitInfo));

    	// Write header words, some of which will be
        // overwritten later when the values are determined.
		buffer.putInt(size/4);        // actual size in ints
		buffer.putInt(blockNumber++); // incremental count of blocks
		buffer.putInt(8);             // header size always 8
		buffer.putInt(eventCount);    // number of events in block
        buffer.putInt(reserved1);     // unused / sourceId for coda eventbuilding
		buffer.putInt(BlockHeaderV4.generateSixthWord(bitInfo)); // version = 4 & bit info stored
        buffer.putInt(reserved2);     // unused
		buffer.putInt(IBlockHeader.MAGIC_NUMBER); // MAGIC_NUMBER
    }

    /**
     * Finish writing block headers and events to buffer.
     * Set the buffer's position to be after the data.
     * This means the user must do a {@link java.nio.ByteBuffer#flip()}
     * on the buffer in order to read it properly.
     *
     * @throws EvioException if not enough space in buffer
     */
    synchronized private void closeBuffer() throws EvioException {

        int headerBytes = 32;

        // Write any remaining events, if there are any to be written
        if (eventHoldingList.size() > 0) {

            int requiredBufferByteSize = holdingListTotalBytes + headerBytes;

            if (writeDictionary) {
                // Don't include dictionary in event count
                writeNewHeader(requiredBufferByteSize, eventHoldingList.size() - 1);
                updateBitInfoInBuffer(BlockHeaderV4.generateSixthWord(bitInfo, true, false));
                // After we write the header & dictionary, reset dictionary bit
                bitInfo.set(0, false);
            }
            else {
                writeNewHeader(requiredBufferByteSize, eventHoldingList.size());
                updateBitInfoInBuffer(BlockHeaderV4.generateSixthWord(bitInfo, false, false));
            }

            writeDictionary = false;

            // Write events being stored to buffer
            for (EvioBank bnk : eventHoldingList) {
                bnk.write(buffer);
            }

//System.out.println("CLOSE: finish writing events, then write ending header into buf, blk # = " + blockNumber);
            // Write out ending empty block header.
            try { writeEmptyLastBlockHeader(blockNumber); }
            catch (IOException e) {/* never happen */}
        }
        // If there are no remaining events ...
        else if (!lastBlockOut) {
//System.out.println("CLOSE: write ending header into buf, blk # = " + blockNumber);
            try { writeEmptyLastBlockHeader(blockNumber); }
            catch (IOException e) {/* never happen */}
        }
        else {
//System.out.println("CLOSE: don't need to write ending header into buf, already done");
        }

        // Set the buffer position to be just after the last bit of data
        try { buffer.position(buffer.position() + headerBytes); }
        catch (Exception e) {}

        closed = true;
    }

    /**
     * Write an event (bank) to a buffer in evio version 4 format in blocks.
     * Each block has an integral number of events. There are limits to the
     * number of events in each block and the total size of each block.
     * A dictionary passed to the constructor will be written as the very
     * first event and does <b>not</b> need to be explicitly written using
     * this method.
     *
     * @param bank the bank to write.
     * @throws EvioException if not enough room in buffer or close() already called
     */
    synchronized private void writeEventToBuffer(EvioBank bank) throws EvioException {

        if (closed) {
            throw new EvioException("close() has already been called");
        }

        // If we're writing a dictionary, it must be the first event in the first block.
        if (writeDictionary && eventsWritten < 1) {
            // Dictionary must be the first event in the first block.
            // Make a bank out of the dictionary
            EvioBank dictBank = new EvioBank(0, DataType.CHARSTAR8, 0);
            try {
                dictBank.appendStringData(xmlDictionary);
            }
            catch (EvioException e) {/* never happen */ }
            int dictEventBytes = dictBank.getTotalBytes();

            // add bank as first in list of events to be written
            eventHoldingList.addFirst(dictBank);
            holdingListTotalBytes += dictEventBytes;
        }

        // See how much space the event will take up and get it ready (turn into ByteBuffer).
        int currentEventBytes = bank.getTotalBytes();

        //-----------------------------------------------------------------------------------
        // Do we write to block & ev buffers now or do we put the event on the holding list?
        //
        // The basic principle here is that if the block buffer has blockCountMax or
        // more events, it gets written. And if the block buffer would exceed its memory limit
        // (4*blockSizeMax bytes) if another event were to be added, it gets written.
        // The only condition in which a block would be bigger than this memory limit is if
        // a single event itself is bigger than that limit.
        //-----------------------------------------------------------------------------------

        // header size in bytes
        int headerBytes = 32;

        // aim for this amount of data in each block (in bytes)
        int roomForData = 4*blockSizeMax - headerBytes;

        int requiredBufferBytes;

        // If we have enough to overfill a block ...
        if (eventHoldingList.size() > 0 &&
                (holdingListTotalBytes + currentEventBytes > roomForData)) {

            // size of block without current event
            requiredBufferBytes = holdingListTotalBytes + headerBytes;

            if (writeDictionary) {
                // don't include dictionary in event count
                writeNewHeader(requiredBufferBytes, eventHoldingList.size() - 1);
                // after we write the header & dictionary, reset dictionary bit
                bitInfo.set(0, false);
            }
            else {
                writeNewHeader(requiredBufferBytes, eventHoldingList.size());
            }

            // write events being stored to buffer
            for (EvioBank bnk : eventHoldingList) {
                bnk.write(buffer);
            }

            // reset after writing done
            writeDictionary = false;
            eventHoldingList.clear();
            holdingListTotalBytes = 0;
        }


        // if current event is >= than the block limit ...
        if (currentEventBytes >= roomForData) {
            // write out only current event
            requiredBufferBytes = currentEventBytes + headerBytes;
            writeNewHeader(requiredBufferBytes, 1);
            bank.write(buffer);
        }
        else {
            // add this event to holding list
            eventHoldingList.add(bank);
            holdingListTotalBytes += currentEventBytes;

            // write out events only if we've reached the count limit
            if (eventHoldingList.size() < blockCountMax) {
                eventsWritten++;
                return;
            }

            requiredBufferBytes = holdingListTotalBytes + headerBytes;

            if (writeDictionary) {
                // don't include dictionary in event count
                writeNewHeader(requiredBufferBytes, eventHoldingList.size() - 1);
                // after we write the header & dictionary, reset dictionary bit
                bitInfo.set(0, false);
            }
            else {
                writeNewHeader(requiredBufferBytes, eventHoldingList.size());
            }

            // write events being stored
            for (EvioBank bnk : eventHoldingList) {
                bnk.write(buffer);
            }
        }

        // reset after writing done
        eventsWritten++;
        writeDictionary = false;
        eventHoldingList.clear();
        holdingListTotalBytes = 0;
	}



    /**
     * Update the bit information field in the header using an absolute put.
     * @param word the 6th word value.
     */
    private void updateBitInfo(int word) {
//System.out.println("Set bit info word to " + word);
        blockBuffer.putInt(BIT_INFO_OFFSET, word);
    }

    /**
     * Update the bit information field in the header using an absolute put.
     * @param word the 6th word value.
     */
    private void updateBitInfoInBuffer(int word) {
//System.out.println("Set bit info word to " + word);
        buffer.putInt(bufferHeaderPosition + BIT_INFO_OFFSET, word);
    }


    /**
     * Main program for testing.
     *
     * @param args ignored command line arguments.
     */
    public static void mainOrig(String args[]) {
//      String infile  = "C:\\Documents and Settings\\heddle\\My Documents\\out.ev";
//      String outfile = "C:\\Documents and Settings\\heddle\\My Documents\\out_copy.ev";
        String infile  = "../../testdata/out.ev";
        String outfile = "../../testdata/out_copy.ev";
        int count = 0;

        try {
            //an EvioReader object is used for reading
            EvioReader inFile = new EvioReader(new File(infile));
            //an EvioWriter object is used for writing
            EventWriter eventWriter = new EventWriter(new File(outfile));
            //EventWriter eventWriter = new EventWriter(new File(outfile), 8192, true);

            EvioEvent event;
            while ((event = inFile.parseNextEvent()) != null) {
//				System.out.println("EVENT LEN: " + event.getHeader().getLength());
                eventWriter.writeEvent(event);
                count++;
            }

            eventWriter.close();
        }
        catch (EvioException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("copied: " + count + " events.");

        //compare the two files
        EvioReader.compareEventFiles(new File(infile), new File(outfile));
    }

    /**
     * Main program for testing.
     *
     * @param args ignored command line arguments.
     */
    public static void main(String args[]) {
        String infile  = "/daqfs/home/timmer/coda/jevio-4.0/testdata/out.ev";
        //String infile  = "/tmp/out_copy.ev";
        String outfile = "/tmp/out_copy.ev";
        int count = 0;

        try {
            EvioReader inFile = new EvioReader(new File(infile));

            int eventCount = inFile.getEventCount();
            System.out.println("eventCount = " + eventCount);

            EvioEvent[] event = new EvioEvent[eventCount+1];
            while ((event[count++] = inFile.parseNextEvent()) != null) {
            }

            long t1, t2, tTotal=0L;
            for (int i=0; i < 40; i++) {
                EventWriter eventWriter = new EventWriter(new File(outfile));
                t1 = System.currentTimeMillis();
                for (int j=0; j < eventCount; j++) {
                    eventWriter.writeEvent(event[i]);
                }
                eventWriter.close();
                t2 = System.currentTimeMillis();
                tTotal += t2 - t1;
            }
            System.out.println("time (ms): " + tTotal);

        }
        catch (EvioException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        //compare the two files
        EvioReader.compareEventFiles(new File(infile), new File(outfile));
    }


}