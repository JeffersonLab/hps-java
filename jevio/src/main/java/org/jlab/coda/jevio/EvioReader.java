package org.jlab.coda.jevio;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * This is a class of interest to the user. It is used to read an evio version 4 or earlier
 * format file or buffer. Create an <code>EvioReader</code> from a <code>File</code>
 * object corresponding to an event file, and from this class you can test the file
 * for consistency and, more importantly, you can call {@link #parseNextEvent} or
 * {@link #parseEvent(int)} to get new events and to stream the embedded structures
 * to an IEvioListener. The same can be done by creating an <code>EvioReader</code>
 * from a <code>ByteBuffer</code>.
 *
 * The streaming effect of parsing an event is that the parser will read the event and hand off structures,
 * such as banks, to any IEvioListeners. For those familiar with XML, the event is processed SAX-like.
 * It is up to the listener to decide what to do with the structures.
 * <p>
 * As an alternative to stream processing, after an event is parsed, the user can use the events treeModel
 * for access to the structures. For those familiar with XML, the event is processed DOM-like.
 * <p>
 *
 * @author heddle
 * @author timmer
 *
 */
public class EvioReader {

    /**
	 * This <code>enum</code> denotes the status of a read. <br>
	 * SUCCESS indicates a successful read. <br>
	 * END_OF_FILE indicates that we cannot read because an END_OF_FILE has occurred. Technically this means that what
	 * ever we are trying to read is larger than the buffer's unread bytes.<br>
	 * EVIO_EXCEPTION indicates that an EvioException was thrown during a read, possibly due to out of range values,
	 * such as a negative start position.<br>
	 * UNKNOWN_ERROR indicates that an unrecoverable error has occurred.
	 */
	public static enum ReadStatus {
		SUCCESS, END_OF_FILE, EVIO_EXCEPTION, UNKNOWN_ERROR
	}

	/**
	 * This <code>enum</code> denotes the status of a write.<br>
	 * SUCCESS indicates a successful write. <br>
	 * CANNOT_OPEN_FILE indicates that we cannot write because the destination file cannot be opened.<br>
	 * EVIO_EXCEPTION indicates that an EvioException was thrown during a write.<br>
	 * UNKNOWN_ERROR indicates that an unrecoverable error has occurred.
	 */
	public static enum WriteStatus {
		SUCCESS, CANNOT_OPEN_FILE, EVIO_EXCEPTION, UNKNOWN_ERROR
	}

    /**  Offset to get magic number from start of file. */
    private static final int MAGIC_OFFSET = 28;

    /** Offset to get version number from start of file. */
    private static final int VERSION_OFFSET = 20;

    /** Mask to get version number from 6th int in block. */
    private static final int VERSION_MASK = 0xff;

    /** Root element tag for XML file */
    private static final String ROOT_ELEMENT = "evio-data";


    /** Skip through a file/buffer and store positions of all the events
     *  in random access mode for versions 4+. */
    private final ArrayList<Integer> eventPositions = new ArrayList<Integer>(10000);


    /** Used to assign a transient number [1..n] to events as they are being read. */
    private int eventNumber = 0;

    /**
     * This is the number of events in the file. It is not computed unless asked for,
     * and if asked for it is computed and cached in this variable.
     */
    private int eventCount = -1;

    /** Evio version number (1-4). Obtain this by reading first block header. */
    private int evioVersion;

    /**
     * Endianness of the data being read, either
     * {@link java.nio.ByteOrder#BIG_ENDIAN} or
     * {@link java.nio.ByteOrder#LITTLE_ENDIAN}.
     */
    private ByteOrder byteOrder;

	/** The current block header for evio versions 1-3. */
    private BlockHeaderV2 blockHeader2 = new BlockHeaderV2();

    /** The current block header for evio version 4. */
    private BlockHeaderV4 blockHeader4 = new BlockHeaderV4();

    /** Reference to current block header, any version, through interface. */
    private IBlockHeader blockHeader;

    /** Block number expected when reading. Used to check sequence of blocks. */
    private int blockNumberExpected = 1;

    /** If true, throw an exception if block numbers are out of sequence. */
    private boolean checkBlockNumberSequence;

    /** Is this the last block in the file or buffer? */
    private boolean lastBlock;

    /**
     * Version 4 files may have an xml format dictionary in the
     * first event of the first block.
     */
    private String dictionaryXML;

    /** The buffer being read. */
    private ByteBuffer byteBuffer;

    /** Parser object for this file/buffer. */
    private EventParser parser;

    /** Initial position of buffer or mappedByteBuffer when reading a file. */
    private int initialPosition;

    //------------------------
    // File specific members
    //------------------------

    /** Absolute path of the underlying file. */
    private String path;

    /**
     * The buffer representing a map of the input file which is also
     * accessed through {@link #byteBuffer}.
     */
    private MappedByteBuffer mappedByteBuffer;


    //------------------------
    // EvioReader's state
    //------------------------

    /**
     * This class stores the state of this reader so it can be recovered
     * after a state-changing method has been called -- like {@link #rewind()}.
     */
    private class ReaderState {
        private boolean lastBlock;
        private int eventNumber;
        private int byteBufferPosition;
        private int blockNumberExpected;
        private BlockHeaderV2 blockHeader2;
        private BlockHeaderV4 blockHeader4;
    }

    /**
     * This method saves the current state of this EvioReader object.
     * @return the current state of this EvioReader object.
     */
    private ReaderState getState() {
        ReaderState currentState = new ReaderState();
        currentState.lastBlock   = lastBlock;
        currentState.eventNumber = eventNumber;
        currentState.byteBufferPosition  = byteBuffer.position();
        currentState.blockNumberExpected = blockNumberExpected;
        if (evioVersion > 3) {
            currentState.blockHeader4 = (BlockHeaderV4)blockHeader4.clone();
        }
        else {
            currentState.blockHeader2 = (BlockHeaderV2)blockHeader2.clone();
        }

        return currentState;
    }


    /**
     * This method restores a previously saved state of this EvioReader object.
     * @param state a previously stored state of this EvioReader object.
     */
    private void restoreState(ReaderState state) {
        lastBlock = state.lastBlock;
        eventNumber = state.eventNumber;
        byteBuffer.position(state.byteBufferPosition);

        blockNumberExpected = state.blockNumberExpected;
        if (evioVersion > 3) {
            blockHeader = blockHeader4 = state.blockHeader4;
        }
        else {
            blockHeader = blockHeader2 = state.blockHeader2;
        }
    }


    //------------------------

    private void printBuffer(ByteBuffer buf, int lenInInts) {
        IntBuffer ibuf = buf.asIntBuffer();
        for (int i=0; i < lenInInts; i++) {
            System.out.println("  Buf(" + i + ") = " + Integer.toHexString(ibuf.get(i)));
        }
    }

    /**
     * Constructor for reading an event file.
     *
     * @param path the full path to the file that contains events.
     *             For writing event files, use an <code>EventWriter</code> object.
     * @see EventWriter
     * @throws IOException   if read failure
     * @throws EvioException if file arg is null
     */
    public EvioReader(String path) throws EvioException, IOException {
        this(new File(path));
    }

    /**
     * Constructor for reading an event file.
     *
     * @param path the full path to the file that contains events.
     *             For writing event files, use an <code>EventWriter</code> object.
     * @param checkBlkNumSeq if <code>true</code> check the block number sequence
     *                       and throw an exception if it is not sequential starting
     *                       with 1
     * @see EventWriter
     * @throws IOException   if read failure
     * @throws EvioException if file arg is null;
     *                       if first block number != 1 when checkBlkNumSeq arg is true
     */
    public EvioReader(String path, boolean checkBlkNumSeq) throws EvioException, IOException {
        this(new File(path), checkBlkNumSeq);
    }

    /**
     * Constructor for reading an event file.
     *
     * @param file the file that contains events.
     * @see EventWriter
     * @throws IOException   if read failure
     * @throws EvioException if file arg is null
     */
    public EvioReader(File file) throws EvioException, IOException {
        this(file, false);
    }


    /**
     * Constructor for reading an event file.
     *
     * @param file the file that contains events.
     * @param checkBlkNumSeq if <code>true</code> check the block number sequence
     *                       and throw an exception if it is not sequential starting
     *                       with 1
     * @see EventWriter
     * @throws IOException   if read failure
     * @throws EvioException if file arg is null;
     *                       if first block number != 1 when checkBlkNumSeq arg is true
     */
    public EvioReader(File file, boolean checkBlkNumSeq)
                                        throws EvioException, IOException {
        if (file == null) {
            throw new EvioException("File arg is null");
        }

        checkBlockNumberSequence = checkBlkNumSeq;
        initialPosition = 0;

        FileInputStream fileInputStream = new FileInputStream(file);
        path = file.getAbsolutePath();

        FileChannel fileChannel = fileInputStream.getChannel();
        mapFile(fileChannel);
        fileChannel.close(); // this object is no longer needed

        // Read first block header and find the file's endianness & evio version #.
        // If there's a dictionary, read that too.
        if (getFirstHeader() != ReadStatus.SUCCESS) {
            throw new IOException("Failed reading first block header/dictionary");
        }

        // For the lastest evio format, generate a table
        // of all event positions in buffer for random access.
        if (evioVersion > 3) {
            eventCount = 0;
            generateEventPositionTable();
        }

        parser = new EventParser();
    }

    /**
     * Constructor for reading a buffer.
     *
     * @param byteBuffer the buffer that contains events.
     * @see EventWriter
     * @throws IOException   if read failure
     * @throws EvioException if file arg is null
     */
    public EvioReader(ByteBuffer byteBuffer) throws EvioException, IOException {
        this(byteBuffer, false);
    }

    /**
     * Constructor for reading a buffer.
     *
     * @param byteBuffer the buffer that contains events.
     * @param checkBlkNumSeq if <code>true</code> check the block number sequence
     *                       and throw an exception if it is not sequential starting
     *                       with 1
     * @see EventWriter
     * @throws IOException   if read failure
     * @throws EvioException if file arg is null;
     *                       if first block number != 1 when checkBlkNumSeq arg is true
     */
    public EvioReader(ByteBuffer byteBuffer, boolean checkBlkNumSeq)
                                                    throws EvioException, IOException {

        if (byteBuffer == null) {
            throw new EvioException("Buffer arg is null");
        }

        checkBlockNumberSequence = checkBlkNumSeq;
        initialPosition = byteBuffer.position();
        this.byteBuffer = byteBuffer;

        // Read first block header and find the file's endianness & evio version #.
        // If there's a dictionary, read that too.
        if (getFirstHeader() != ReadStatus.SUCCESS) {
            throw new IOException("Failed reading first block header/dictionary");
        }

        // For the lastest evio format, generate a table
        // of all event positions in buffer for random access.
        if (evioVersion > 3) {
            eventCount = 0;
            generateEventPositionTable();
        }

        parser = new EventParser();
    }

    /**
     * This method can be used to avoid creating additional EvioReader
     * objects by reusing this one with another buffer. The method
     * {@link #close()} is called before anything else.
     *
     * @param buf ByteBuffer to be read
     * @throws IOException   if read failure
     * @throws EvioException if first block number != 1 when checkBlkNumSeq arg is true
     */
    public synchronized void setBuffer(ByteBuffer buf) throws EvioException, IOException {

        close();

        lastBlock           = false;
        eventNumber         = 0;
        eventCount          = -1;
        blockNumberExpected = 1;
        dictionaryXML       = null;
        initialPosition     = buf.position();
        this.byteBuffer     = buf;

        // Read first block header and find the file's endianness & evio version #.
        // If there's a dictionary, read that too.
        if (getFirstHeader() != ReadStatus.SUCCESS) {
            throw new IOException("Failed reading first block header/dictionary");
        }

        // For the lastest evio format, generate a table
        // of all event positions in buffer for random access.
        if (evioVersion > 3) {
            eventCount = 0;
            generateEventPositionTable();
        }
    }

    /**
     * Is this reader checking the block number sequence and
     * throwing an exception is it's not sequential and starting with 1?
     * @return <code>true</code> if checking block number sequence, else <code>false</code>
     */
    public boolean checkBlockNumberSequence() {
        return checkBlockNumberSequence;
    }

    /**
     * Get the evio version number.
     * @return evio version number.
     */
    public int getEvioVersion() {
        return evioVersion;
    }

    /**
      * Get the path to the file.
      * @return path to the file
      */
     public String getPath() {
         return path;
     }

    /**
     * Get the file/buffer parser.
     * @return file/buffer parser.
     */
    public EventParser getParser() {
        return parser;
    }

    /**
     * Set the file/buffer parser.
     * @param parser file/buffer parser.
     */
    public void setParser(EventParser parser) {
        if (parser != null) {
            this.parser = parser;
        }
    }

     /**
     * Get the XML format dictionary is there is one.
     *
     * @return XML format dictionary, else null.
     */
    public String getDictionaryXML() {
        return dictionaryXML;
    }

    /**
     * Does this evio file have an associated XML dictionary?
     *
     * @return <code>true</code> if this evio file has an associated XML dictionary,
     *         else <code>false</code>
     */
    public boolean hasDictionaryXML() {
        return dictionaryXML != null;
    }

    /**
     * Get the number of events remaining in the file.
     *
     * @return number of events remaining in the file
     * @throws EvioException if failed reading from coda v3 file
     */
    public int getNumEventsRemaining() throws EvioException {
        return getEventCount() - eventNumber;
    }

	/**
	 * Maps the file into memory. The data are not actually loaded in memory-- subsequent reads will read
	 * random-access-like from the file.
	 *
     * @param inputChannel the input channel.
     * @throws IOException if file cannot be opened
	 */
	private synchronized void mapFile(FileChannel inputChannel) throws IOException {
		long sz = inputChannel.size();
		mappedByteBuffer = inputChannel.map(FileChannel.MapMode.READ_ONLY, 0L, sz);
        byteBuffer = mappedByteBuffer;
	}

	/**
	 * Obtain the file size using the memory mapped buffer's capacity, which should be the same.
	 *
	 * @return the file size in bytes--actually the mapped memory size.
	 */
	public int fileSize() {
		return byteBuffer.capacity();
	}

    /**
     * Get the memory mapped buffer corresponding to the event file.
     *
     * @return the memory mapped buffer corresponding to the event file.
     */
    public MappedByteBuffer getMappedByteBuffer() {
        return mappedByteBuffer;
    }

    /**
     * Get the byte buffer being read directly or corresponding to the event file.
     *
     * @return the byte buffer being read directly or corresponding to the event file.
     */
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    /**
     * Generate a table (ArrayList) of positions of events in file/buffer.
     * This method does <b>not</b> affect the byteBuffer position, eventNumber,
     * or lastBlock values. Valid only in versions 4 and later.
     *
     * @throws EvioException if version 3 or earlier
     */
    private void generateEventPositionTable() throws EvioException {

        if (evioVersion < 4) {
            throw new EvioException("Unsupported version (" + evioVersion + ")");
        }

        int      i, position, byteLen, bytesLeft, blockEventCount, blockHdrSize;
        boolean  curLastBlock=false, firstBlock=true, hasDictionary=false;


        // Start at the beginning of byteBuffer without changing
        // its current position. Do this with absolute gets.
        position  = initialPosition;
        bytesLeft = byteBuffer.limit() - position;
//        System.out.println("generatePositionTable: ");
//        System.out.println("    byte buf cap  = " + byteBuffer.capacity());
//        System.out.println("    byte buf lim  = " + byteBuffer.limit());
//        System.out.println("    byte buf pos  = " + byteBuffer.position());
//        System.out.println("    bytesLeft     = " + bytesLeft);
//        System.out.println("    (initial) pos = " + position);
//        System.out.println();

        while (!curLastBlock) {
//System.out.println("generatePositionTable: pos = " + position +
//                           ", ver pos = " + (position + 4*BlockHeaderV4.EV_VERSION) +
//                           ", h siz pos = " + (position + 4*BlockHeaderV4.EV_HEADERSIZE) +
//                           ", ev count pos = " + (position + 4*BlockHeaderV4.EV_COUNT));

            // Look at block header to get info. Swapping is taken care of.
            i               = byteBuffer.getInt(position + 4*BlockHeaderV4.EV_VERSION);
            blockHdrSize    = byteBuffer.getInt(position + 4*BlockHeaderV4.EV_HEADERSIZE);
            blockEventCount = byteBuffer.getInt(position + 4*BlockHeaderV4.EV_COUNT);

            eventCount  += blockEventCount;
            curLastBlock = BlockHeaderV4.isLastBlock(i);
            if (firstBlock) hasDictionary = BlockHeaderV4.hasDictionary(i);

//            System.out.println("    ver       = 0x" + Integer.toHexString(i));
//            System.out.println("    hdr size  = 0x" + Integer.toHexString(blockHdrSize));
//            System.out.println("    blk count = 0x" + Integer.toHexString(blockEventCount));
//            System.out.println("    last blk  = " + curLastBlock);
//            System.out.println("    has dict  = " + hasDictionary);
//            System.out.println();

            // Hop over block header to data
            position  += 4*blockHdrSize;
            bytesLeft -= 4*blockHdrSize;

//System.out.println("    hopped blk hdr, bytesLeft = " + bytesLeft + ", pos = " + position);
            // Check for a dictionary - the first event in the first block.
            // It's not included in the header block count, but we must take
            // it into account by skipping over it.
            if (firstBlock && hasDictionary) {
                firstBlock = false;

                // Get its length - bank's len does not include itself
                byteLen = 4*(byteBuffer.getInt(position) + 1);

                // Skip over dictionary
                position  += byteLen;
                bytesLeft -= byteLen;
//System.out.println("    hopped dict, bytesLeft = " + bytesLeft + ", pos = " + position);
            }

            // For each event in block, store its location
            for (i=0; i < blockEventCount; i++) {
                // Sanity check - must have at least 1 header's amount left
                if (bytesLeft < 8) {
                    throw new EvioException("File/buffer bad format");
                }

                // Store current position
                eventPositions.add(position);

                // Get length of current event (including full header)
                byteLen = 4*(byteBuffer.getInt(position) + 1);

                position  += byteLen;
                bytesLeft -= byteLen;
//System.out.println("    hopped event, bytesLeft = " + bytesLeft + ", pos = " + position + "\n");
            }
        }

    }


    /**
     * Reads part of the first block (physical record) header in order to determine
     * the evio version # and endianness of the file or buffer in question. These things
     * do <b>not</b> need to be examined in subsequent block headers.
     *
     * @return status of read attempt
     */
    protected synchronized ReadStatus getFirstHeader() {

        // Have enough remaining bytes to read?
        if (byteBuffer.remaining() < 32) {
            byteBuffer.clear();
            return ReadStatus.END_OF_FILE;
        }

        try {
            // Set the byte order to match the buffer/file's ordering.

            // Check the magic number for endianness. This requires
            // peeking ahead 7 ints or 28 bytes. Set the endianness
            // once we figure out what it is (buffer defaults to big endian).
            byteOrder = byteBuffer.order();
            int magicNumber = byteBuffer.getInt(MAGIC_OFFSET);

            if (magicNumber != IBlockHeader.MAGIC_NUMBER) {
                if (byteOrder == ByteOrder.BIG_ENDIAN) {
                    byteOrder = ByteOrder.LITTLE_ENDIAN;
                }
                else {
                    byteOrder = ByteOrder.BIG_ENDIAN;
                }
                byteBuffer.order(byteOrder);

                // Reread magic number to make sure things are OK
                magicNumber = byteBuffer.getInt(MAGIC_OFFSET);
                if (magicNumber != IBlockHeader.MAGIC_NUMBER) {
System.out.println("ERROR reread magic # (" + magicNumber + ") & still not right");
                    return ReadStatus.EVIO_EXCEPTION;
                }
            }

            // Check the version number. This requires peeking ahead 5 ints or 20 bytes.
            evioVersion = byteBuffer.getInt(VERSION_OFFSET) & VERSION_MASK;
            if (evioVersion < 1)  {
                return ReadStatus.EVIO_EXCEPTION;
            }
//System.out.println("Evio version# = " + evioVersion);

            try {
                if (evioVersion >= 4) {
                    // Cache the starting position
                    blockHeader4.setBufferStartingPosition(byteBuffer.position());
                    // Read the header data.
                    blockHeader4.setSize(byteBuffer.getInt());
                    blockHeader4.setNumber(byteBuffer.getInt());
                    blockHeader4.setHeaderLength(byteBuffer.getInt());
                    blockHeader4.setEventCount(byteBuffer.getInt());
                    blockHeader4.setReserved1(byteBuffer.getInt());

                    // Use 6th word to set bit info & version
                    blockHeader4.parseToBitInfo(byteBuffer.getInt());
                    blockHeader4.setVersion(evioVersion);
                    lastBlock = blockHeader4.getBitInfo(1);

                    blockHeader4.setReserved2(byteBuffer.getInt());
                    blockHeader4.setMagicNumber(byteBuffer.getInt());
                    blockHeader = blockHeader4;

                    // Deal with non-standard header lengths here
                    int headerLenDiff = blockHeader4.getHeaderLength() - BlockHeaderV4.HEADER_SIZE;
                    // If too small quit with error since headers have a minimum size
                    if (headerLenDiff < 0) {
                        return ReadStatus.EVIO_EXCEPTION;
                    }
                    // If bigger, read extra ints
                    else if (headerLenDiff > 0) {
                        for (int i=0; i < headerLenDiff; i++) {
//System.out.println("Getting extra header int");
                            byteBuffer.getInt();
                        }
                    }

//System.out.println("BlockHeader v4:");
//System.out.println("   block length  = " + blockHeader4.getSize() + " ints");
//System.out.println("   block number  = " + blockHeader4.getNumber());
//System.out.println("   header length = " + blockHeader4.getHeaderLength() + " ints");
//System.out.println("   event count   = " + blockHeader4.getEventCount());
//System.out.println("   version       = " + blockHeader4.getVersion());
//System.out.println("   has Dict      = " + blockHeader4.getBitInfo(0));
//System.out.println("   is End        = " + lastBlock);
//System.out.println("   magic number  = " + Integer.toHexString(blockHeader4.getMagicNumber()));
//System.out.println();

                    // Is there a dictionary? If so, read it here.
                    if (blockHeader4.hasDictionary()) {
                        readDictionary();
                    }
                }
                else {
                    // cache the starting position
                    blockHeader2.setBufferStartingPosition(byteBuffer.position());

                    // read the header data.
                    blockHeader2.setSize(byteBuffer.getInt());
                    blockHeader2.setNumber(byteBuffer.getInt());
                    blockHeader2.setHeaderLength(byteBuffer.getInt());
                    blockHeader2.setStart(byteBuffer.getInt());
                    blockHeader2.setEnd(byteBuffer.getInt());
                    // skip version
                    byteBuffer.getInt();
                    blockHeader2.setVersion(evioVersion);
                    blockHeader2.setReserved1(byteBuffer.getInt());
                    blockHeader2.setMagicNumber(byteBuffer.getInt());
                    blockHeader = blockHeader2;
                }

                // check block number if so configured
                if (checkBlockNumberSequence) {
                    if (blockHeader.getNumber() != blockNumberExpected) {

System.out.println("block # out of sequence, got " + blockHeader.getNumber() +
                           " expecting " + blockNumberExpected);

                        return ReadStatus.EVIO_EXCEPTION;
                    }
                    blockNumberExpected++;
                }
            }
            catch (EvioException e) {
                e.printStackTrace();
                return ReadStatus.EVIO_EXCEPTION;
            }

        }
        catch (BufferUnderflowException a) {
System.err.println("ERROR endOfBuffer " + a);
            byteBuffer.clear();
            return ReadStatus.UNKNOWN_ERROR;
        }

        return ReadStatus.SUCCESS;
    }


    /**
     * Reads the block (physical record) header. Assumes the mapped buffer is positioned
     * at the start of the next block header (physical record.) By the time this is called,
     * the version # and byte order have already been determined. Not necessary to do that
     * for each block header that's read.<br>
     *
     * A Bank header is 8, 32-bit ints. The first int is the size of the block in ints
     * (not counting the length itself, i.e., the number of ints to follow).
     *
     * Most users should have no need for this method, since most applications do not
     * care about the block (physical record) header.
     *
     * @return status of read attempt
     */
    protected synchronized ReadStatus nextBlockHeader() {

        // We already read the last block header
        if (lastBlock) {
            return ReadStatus.END_OF_FILE;
        }

        // Have enough remaining?
        if (byteBuffer.remaining() < 32) {
            byteBuffer.clear();
            return ReadStatus.END_OF_FILE;
        }

        try {
            if (evioVersion >= 4) {
                // Cache the starting position
                blockHeader4.setBufferStartingPosition(byteBuffer.position());

                // Read the header data.
                blockHeader4.setSize(byteBuffer.getInt());
                blockHeader4.setNumber(byteBuffer.getInt());
                blockHeader4.setHeaderLength(byteBuffer.getInt());
                blockHeader4.setEventCount(byteBuffer.getInt());
                blockHeader4.setReserved1(byteBuffer.getInt());
                // Use 6th word to set bit info
                blockHeader4.parseToBitInfo(byteBuffer.getInt());
                blockHeader4.setVersion(evioVersion);
                lastBlock = blockHeader4.getBitInfo(1);
                blockHeader4.setReserved2(byteBuffer.getInt());
                blockHeader4.setMagicNumber(byteBuffer.getInt());
                blockHeader = blockHeader4;

                // Deal with non-standard header lengths here
                int headerLenDiff = blockHeader4.getHeaderLength() - BlockHeaderV4.HEADER_SIZE;
                // If too small quit with error since headers have a minimum size
                if (headerLenDiff < 0) {
                    return ReadStatus.EVIO_EXCEPTION;
                }
                // If bigger, read extra ints
                else if (headerLenDiff > 0) {
                    for (int i=0; i < headerLenDiff; i++) {
                        byteBuffer.getInt();
                    }
                }

//System.out.println("BlockHeader v4:");
//System.out.println("   block length  = " + blockHeader4.getSize() + " ints");
//System.out.println("   block number  = " + blockHeader4.getNumber());
//System.out.println("   header length = " + blockHeader4.getHeaderLength() + " ints");
//System.out.println("   event count   = " + blockHeader4.getEventCount());
//System.out.println("   version       = " + blockHeader4.getVersion());
//System.out.println("   has Dict      = " + blockHeader4.getBitInfo(0));
//System.out.println("   is End        = " + lastBlock);
//System.out.println("   magic number  = " + Integer.toHexString(blockHeader4.getMagicNumber()));
//System.out.println();

            }
            else if (evioVersion < 4) {
                // cache the starting position
                blockHeader2.setBufferStartingPosition(byteBuffer.position());

                // read the header data.
                blockHeader2.setSize(byteBuffer.getInt());
                blockHeader2.setNumber(byteBuffer.getInt());
                blockHeader2.setHeaderLength(byteBuffer.getInt());
                blockHeader2.setStart(byteBuffer.getInt());
                blockHeader2.setEnd(byteBuffer.getInt());
                // skip version
                byteBuffer.getInt();
                blockHeader2.setVersion(evioVersion);
                blockHeader2.setReserved1(byteBuffer.getInt());
                blockHeader2.setMagicNumber(byteBuffer.getInt());
                blockHeader = blockHeader2;
            }
            else {
                // bad version # - should never happen
                return ReadStatus.EVIO_EXCEPTION;
            }

            // check block number if so configured
            if (checkBlockNumberSequence) {
                if (blockHeader.getNumber() != blockNumberExpected) {

System.out.println("block # out of sequence, got " + blockHeader.getNumber() +
                   " expecting " + blockNumberExpected);

                    return ReadStatus.EVIO_EXCEPTION;
                }
                blockNumberExpected++;
            }
        }
        catch (EvioException e) {
            e.printStackTrace();
            return ReadStatus.EVIO_EXCEPTION;
        }
        catch (BufferUnderflowException a) {
System.err.println("ERROR endOfBuffer " + a);
            byteBuffer.clear();
            return ReadStatus.UNKNOWN_ERROR;
        }

        return ReadStatus.SUCCESS;
    }


    /**
     * This method is only called once at the very beginning if buffer is known to have
     * a dictionary. It then reads that dictionary. Only called in versions 4 & up.
     *
     * @return <code>true</code> if dictionary was found & read, else <code>false</code>.
     * @throws EvioException if failed read due to bad buffer format;
     *                       if version 3 or earlier
     */
     private synchronized void readDictionary() throws EvioException {

         if (evioVersion < 4) {
             throw new EvioException("Unsupported version (" + evioVersion + ")");
         }

         // How many bytes remain in this block?
         int bytesRemaining = blockBytesRemaining();
         if (bytesRemaining < 12) {
             throw new EvioException("Not enough data in first block");
         }

         // Once here, we are assured the entire next event is in this block.
         int length = byteBuffer.getInt();
         if (length < 1) {
             throw new EvioException("Bad value for dictionary length (too big for java?)");
         }
         bytesRemaining -= 4;

         // Since we're only interested in length, read but ignore rest of the header.
         byteBuffer.getInt();
         bytesRemaining -= 4;

         // get the raw data
         int eventDataSizeBytes = 4*(length - 1);
         if (bytesRemaining < eventDataSizeBytes) {
             throw new EvioException("Not enough data in first block");
         }

         byte bytes[] = new byte[eventDataSizeBytes];

         // Read in dictionary data
         try {
             byteBuffer.get(bytes, 0, eventDataSizeBytes);
         }
         catch (Exception e) {
             throw new EvioException("Problems reading buffer");
         }

         // This is the very first event and must be a dictionary
         String[] strs = BaseStructure.unpackRawBytesToStrings(bytes, 0);
         if (strs == null) {
             throw new EvioException("Data in bad format");
         }
         dictionaryXML = strs[0];
     }


    /**
     * Get the event in the file/buffer at a given index (starting at 1).
     * As useful as this sounds, most applications will probably call
     * {@link #parseEvent(int)} instead, since it combines combines
     * getting the event with parsing it.<p>
     *
     * @param  index number of event desired, starting at 1, from beginning of file/buffer
     * @return the event in the file at the given index or null if none
     * @throws EvioException if failed read due to bad file/buffer format;
     *                       if out of memory; if index < 1
     */
    public synchronized EvioEvent getEvent(int index) throws EvioException {

        if (index < 1) {
            throw new EvioException("index arg starts at 1");
        }

        if (index > eventPositions.size()) {
            return null;
        }

        if (evioVersion < 4) {
            return gotoEventNumber(index);
        }

        index--;

        EvioEvent event = new EvioEvent();
        BaseStructureHeader header = event.getHeader();

        int eventDataSizeBytes = 0;
        // Save the current position
        int origPosition = byteBuffer.position();
        // Go to the correct position for the desired event
        int position = eventPositions.get(index);
        byteBuffer.position(position);

        // Read length of event
        int length = byteBuffer.getInt();
        if (length < 1) {
            throw new EvioException("Bad file/buffer format");
        }
        header.setLength(length);

        try {
            // Read second header word
            if (byteOrder == ByteOrder.BIG_ENDIAN) {
                // Interested in bit pattern, not negative numbers
                header.setTag(ByteDataTransformer.shortBitsToInt(byteBuffer.getShort()));
                int dt = ByteDataTransformer.byteBitsToInt(byteBuffer.get());
                int type = dt & 0x3f;
                int padding = dt >>> 6;
                // If only 7th bit set, that can only be the legacy tagsegment type
                // with no padding information - convert it properly.
                if (dt == 0x40) {
                    type = DataType.TAGSEGMENT.getValue();
                    padding = 0;
                }
                header.setDataType(type);
                header.setPadding(padding);

                // Once we know what the data type is, let the no-arg constructed
                // event know what type it is holding so xml names are set correctly.
                event.setXmlNames();
                header.setNumber(ByteDataTransformer.byteBitsToInt(byteBuffer.get()));
            }
            else {
                header.setNumber(ByteDataTransformer.byteBitsToInt(byteBuffer.get()));
                int dt = ByteDataTransformer.byteBitsToInt(byteBuffer.get());
                int type = dt & 0x3f;
                int padding = dt >>> 6;
                if (dt == 0x40) {
                    type = DataType.TAGSEGMENT.getValue();
                    padding = 0;
                }
                header.setDataType(type);
                header.setPadding(padding);

                event.setXmlNames();
                header.setTag(ByteDataTransformer.shortBitsToInt(byteBuffer.getShort()));
            }

            // Read the raw data
            eventDataSizeBytes = 4*(header.getLength() - 1);
            byte bytes[] = new byte[eventDataSizeBytes];
            byteBuffer.get(bytes, 0, eventDataSizeBytes);

            event.setRawBytes(bytes);
            event.setByteOrder(byteOrder);
            event.setEventNumber(index + 1);

        }
        catch (OutOfMemoryError e) {
            throw new EvioException("Out Of Memory: (event size = " + eventDataSizeBytes + ")", e);
        }
        catch (Exception e) {
            throw new EvioException("Error", e);
        }
        finally {
            // Reset the buffer position in all cases
            byteBuffer.position(origPosition);
        }

        return event;
    }


	/**
	 * This is the workhorse method. It retrieves the desired event from the file/buffer,
     * and then parses it SAX-like. It will drill down and uncover all structures
     * (banks, segments, and tagsegments) and notify any interested listeners.
	 *
     * @param  index number of event desired, starting at 1, from beginning of file/buffer
	 * @return the parsed event at the given index or null if none
     * @throws EvioException if failed read due to bad file/buffer format;
     *                       if out of memory; if index < 1
	 */
	public synchronized EvioEvent parseEvent(int index) throws EvioException {
		EvioEvent event = getEvent(index);
        if (event != null) parseEvent(event);
		return event;
	}


    /**
     * Get the next event in the file. As useful as this sounds, most applications will probably call
     * {@link #parseNextEvent()} instead, since it combines combines getting the next
     * event with parsing the next event.<p>
     * In evio version 4, events no longer cross block boundaries. There are only one or more
     * complete events in each block. No changes were made to this method from versions 1-3 in order
     * to read the version 4 format as it is subset of versions 1-3 with variable block length.
     *
     * @return the next event in the file. On error it throws an EvioException.
     *         On end of file, it returns <code>null</code>.
     * @throws EvioException if failed read due to bad buffer format
     */
    public synchronized EvioEvent nextEvent() throws EvioException {
        EvioEvent event = new EvioEvent();
         BaseStructureHeader header = event.getHeader();

         // Are we at the top of the file?
         boolean isFirstEvent = false;
         if (byteBuffer.position() == 0) {
             ReadStatus status = nextBlockHeader();
             if (status != ReadStatus.SUCCESS) {
                 throw new EvioException("Failed reading block header in nextEvent.");
             }
             isFirstEvent = true;
         }

         // How many bytes remain in this block?
         // Must see if we have to deal with crossing physical record boundaries (< version 4).
         int bytesRemaining = blockBytesRemaining();
         if (bytesRemaining < 0) {
             throw new EvioException("Number of block bytes remaining is negative.");
         }

         // Are we exactly at the end of the block (physical record)?
         if (bytesRemaining == 0) {
             ReadStatus status = nextBlockHeader();
             if (status == ReadStatus.SUCCESS) {
                 return nextEvent();
             }
             else if (status == ReadStatus.END_OF_FILE) {
                 return null;
             }
             else {
                 throw new EvioException("Failed reading block header in nextEvent.");
             }
         }
         // Or have we already read in the last event?
         else if (blockHeader.getBufferEndingPosition() == byteBuffer.position()) {
             return null;
         }

         // Version   4: once here, we are assured the entire next event is in this block.
         // Version 1-3: no matter what, we can get the length of the next event.
         // A non positive length indicates eof;
         int length = byteBuffer.getInt();
         if (length < 1) {
             return null;
         }

         header.setLength(length);
         bytesRemaining -= 4; // just read in 4 bytes

         // Versions 1-3: if we were unlucky, after reading the length
         //               there are no bytes remaining in this bank.
         // Don't really need the "if (version < 4)" here except for clarity.
         if (evioVersion < 4) {
             if (bytesRemaining == 0) {
                 ReadStatus status = nextBlockHeader();
                 if (status == ReadStatus.END_OF_FILE) {
                     return null;
                 }
                 else if (status != ReadStatus.SUCCESS) {
                     throw new EvioException("Failed reading block header in nextEvent.");
                 }
                 bytesRemaining = blockBytesRemaining();
             }
         }

         // Now should be good to go, except data may cross block boundary.
         // In any case, should be able to read the rest of the header.
         if (byteOrder == ByteOrder.BIG_ENDIAN) {
             // interested in bit pattern, not negative numbers
             header.setTag(ByteDataTransformer.shortBitsToInt(byteBuffer.getShort()));
             int dt = ByteDataTransformer.byteBitsToInt(byteBuffer.get());
             int type = dt & 0x3f;
             int padding = dt >>> 6;
             // If only 7th bit set, that can only be the legacy tagsegment type
             // with no padding information - convert it properly.
             if (dt == 0x40) {
                 type = DataType.TAGSEGMENT.getValue();
                 padding = 0;
             }
             header.setDataType(type);
             header.setPadding(padding);

             // Once we know what the data type is, let the no-arg constructed
             // event know what type it is holding so xml names are set correctly.
             event.setXmlNames();
             header.setNumber(ByteDataTransformer.byteBitsToInt(byteBuffer.get()));
         }
         else {
             header.setNumber(ByteDataTransformer.byteBitsToInt(byteBuffer.get()));
             int dt = ByteDataTransformer.byteBitsToInt(byteBuffer.get());
             int type = dt & 0x3f;
             int padding = dt >>> 6;
             if (dt == 0x40) {
                 type = DataType.TAGSEGMENT.getValue();
                 padding = 0;
             }
             header.setDataType(type);
             header.setPadding(padding);

             event.setXmlNames();
             header.setTag(ByteDataTransformer.shortBitsToInt(byteBuffer.getShort()));
         }
         bytesRemaining -= 4; // just read in 4 bytes

         // get the raw data
         int eventDataSizeInts = header.getLength() - 1;
         int eventDataSizeBytes = 4 * eventDataSizeInts;

         try {
             byte bytes[] = new byte[eventDataSizeBytes];

             int bytesToGo = eventDataSizeBytes;
             int offset = 0;

             // Don't really need the "if (version < 4)" here except for clarity.
             if (evioVersion < 4) {
                 // be in while loop if have to cross block boundary[ies].
                 while (bytesToGo > bytesRemaining) {
                     byteBuffer.get(bytes, offset, bytesRemaining);

                     ReadStatus status = nextBlockHeader();
                     if (status == ReadStatus.END_OF_FILE) {
                         return null;
                     }
                     else if (status != ReadStatus.SUCCESS) {
                         throw new EvioException("Failed reading block header after crossing boundary in nextEvent.");
                     }
                     bytesToGo -= bytesRemaining;
                     offset += bytesRemaining;
                     bytesRemaining = blockBytesRemaining();
                 }
             }

             // last (perhaps only) read.
             byteBuffer.get(bytes, offset, bytesToGo);
             event.setRawBytes(bytes);
             event.setByteOrder(byteOrder); // add this to track endianness, timmer

             // if this is the very first event, check to see if it's a dictionary
             if (isFirstEvent && blockHeader.hasDictionary()) {
                 // if we've NOT already been through this before, extract the dictionary
                 if (dictionaryXML == null) {
                     dictionaryXML = event.getStringData()[0];
                 }
                 // give the user the next event, not the dictionary
                 return nextEvent();
             }
             else {
                 event.setEventNumber(++eventNumber);
                 return event;
             }
         }
         catch (OutOfMemoryError ome) {
             System.err.println("Out Of Memory\n" +
                     "eventDataSizeBytes = " + eventDataSizeBytes + "\n" +
                             "bytes Remaining = " + bytesRemaining + "\n" +
                             "event Count: " + eventCount);
             return null;
         }
         catch (Exception e) {
             e.printStackTrace();
             return null;
         }
     }

	/**
	 * This is the workhorse method. It retrieves the next event from the file, and then parses is SAX-like. It will
	 * drill down and uncover all structures (banks, segments, and tagsegments) and notify any interested listeners.
	 *
	 * @return the event that was parsed. On error it throws an EvioException. On end of file, it returns
	 *         <code>null</code>.
	 * @throws EvioException if read failure or bad format
	 */
	public synchronized EvioEvent parseNextEvent() throws EvioException {
		EvioEvent event = nextEvent();
		if (event != null) {
			parseEvent(event);
		}
		return event;
	}

	/**
	 * This will parse an event, SAX-like. It will drill down and uncover all structures (banks, segments, and
	 * tagsegments) and notify any interested listeners.
	 *
	 * As useful as this sounds, most applications will probably call {@link #parseNextEvent()} instead,
	 * since it combines combines getting the next event with parsing the next event .
	 *
	 * @param evioEvent the event to parse.
	 * @throws EvioException if bad format
	 */
	public synchronized void parseEvent(EvioEvent evioEvent) throws EvioException {
		parser.parseEvent(evioEvent);
	}

	/**
	 * Get the number of bytes remaining in the current block (physical record). This is used for pathology checks like
	 * crossing the block boundary.
	 *
	 * @return the number of bytes remaining in the current block (physical record).
	 */
	private int blockBytesRemaining() {
		try {
			return blockHeader.bytesRemaining(byteBuffer.position());
		}
		catch (EvioException e) {
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * The equivalent of rewinding the file. What it actually does
     * is set the position of the buffer back to 0. This method,
     * along with the two <code>position()</code> and the <code>close()</code>
     * method, allows applications to treat files in a normal random
     * access manner.
	 */
	public void rewind() {
		byteBuffer.position(initialPosition);
        blockHeader.setBufferStartingPosition(initialPosition);
		eventNumber = 0;
        lastBlock = false;
        blockNumberExpected = 1;
	}

	/**
	 * This is equivalent to obtaining the current position in the file.
     * What it actually does is return the position of the buffer. This
     * method, along with the <code>rewind()</code>, <code>position(int)</code>
     * and the <code>close()</code> method, allows applications to treat files
     * in a normal random access manner.
	 *
	 * @return the position of the buffer.
	 */
	public int position() {
		return byteBuffer.position();
	}

	/**
	 * This is equivalent to setting the current position in the file.
     * What it actually does is set the position of the buffer. This
     * method, along with the <code>rewind()</code>, <code>position()</code>
     * and the <code>close()</code> method, allows applications to treat files
     * in a normal random access manner.
	 *
	 * @param position the new position of the buffer.
	 */
	public void position(int position) {
		byteBuffer.position(position);
	}

	/**
	 * This is equivalent to closing the file. What it actually does is
     * clear all data from the buffer, and sets its position to 0.
     * This method, along with the <code>rewind()</code> and the two
     * <code>position()</code> methods, allows applications to treat files
     * in a normal random access manner.
	 */
	public void close() {
		byteBuffer.clear();
	}

	/**
	 * This returns the current (active) block (physical record) header.
     * Since most users have no interest in physical records, this method
     * should not be used often. Mostly it is used by the test programs in the
	 * <code>EvioReaderTest</code> class.
	 *
	 * @return the current block header.
	 */
	public IBlockHeader getCurrentBlockHeader() {
		return blockHeader;
	}

	/**
	 * Go to a specific event in the file. The events are numbered 1..N.
     * This number is transient--it is not part of the event as stored in the evio file.
     * In versions 4 and up this is just a wrapper on {@link #getEvent(int)}.
	 *
	 * @param evNumber the event number in a 1..N counting sense, from the start of the file.
     * @return the specified event in file or null if there's an error or nothing at that event #.
	 */
	public EvioEvent gotoEventNumber(int evNumber) {

		if (evNumber < 1) {
			return null;
		}

        if (evioVersion > 3) {
            try {
                return parseEvent(evNumber);
            }
            catch (EvioException e) {
                return null;
            }
        }

		rewind();
		EvioEvent event;

        int currentCount = 0;

		try {
			// get the first evNumber - 1 events without parsing
			for (int i = currentCount+1; i < evNumber; i++) {
				event = nextEvent();
				if (event == null) {
					throw new EvioException("Asked to go to event: " + evNumber + ", which is beyond the end of file");
				}
			}
			// get one more event, the evNumber'th event, with parsing
			return parseNextEvent();
		}
		catch (EvioException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
     * Rewrite the file to XML (not including dictionary).
	 *
	 * @param path the path to the XML file.
	 * @return the status of the write.
	 */
	public WriteStatus toXMLFile(String path) {
		return toXMLFile(path, null);
	}

	/**
	 * Rewrite the file to XML (not including dictionary).
	 *
	 * @param path the path to the XML file.
	 * @param progressListener and optional progress listener, can be <code>null</code>.
	 * @return the status of the write.
	 * @see IEvioProgressListener
	 */
	public WriteStatus toXMLFile(String path, IEvioProgressListener progressListener) {
		FileOutputStream fos;

		try {
			fos = new FileOutputStream(path);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			return WriteStatus.CANNOT_OPEN_FILE;
		}

		try {
			XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(fos);
			xmlWriter.writeStartDocument();
			xmlWriter.writeCharacters("\n");
			xmlWriter.writeComment("Event source file: " + path);

			// start the root element
			xmlWriter.writeCharacters("\n");
			xmlWriter.writeStartElement(ROOT_ELEMENT);
			xmlWriter.writeAttribute("numevents", "" + getEventCount());
            xmlWriter.writeCharacters("\n");
        
            // The difficulty is that this method can be called at
            // any time. So we need to save our state and then restore
            // it when we're done.
            ReaderState state = getState();

			// rewind
			rewind();

			// now loop through the events
			EvioEvent event;
			try {
				while ((event = parseNextEvent()) != null) {
					event.toXML(xmlWriter);
					// anybody interested in progress?
					if (progressListener != null) {
						progressListener.completed(event.getEventNumber(), getEventCount());
					}
				}
			}
			catch (EvioException e) {
				e.printStackTrace();
				return WriteStatus.UNKNOWN_ERROR;
			}

			// done. Close root element, end the document, and flush.
//			xmlWriter.writeCharacters("\n");
			xmlWriter.writeEndElement();
			xmlWriter.writeEndDocument();
			xmlWriter.flush();
			xmlWriter.close();

			try {
				fos.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}

            // Restore our original settings
            restoreState(state);

		}
		catch (XMLStreamException e) {
			e.printStackTrace();
			return WriteStatus.UNKNOWN_ERROR;
		}
        catch (FactoryConfigurationError e) {
            return WriteStatus.UNKNOWN_ERROR;
        }
        catch (EvioException e) {
            return WriteStatus.EVIO_EXCEPTION;
        }

		return WriteStatus.SUCCESS;
	}


    /**
     * This is the number of events in the file. Any dictionary event is <b>not</b>
     * included in the count. In versions 3 and earlier, it is not computed unless
     * asked for, and if asked for it is computed and cached.
     *
     * @return the number of events in the file.
     * @throws EvioException if read failure
     */
    public int getEventCount()  throws EvioException {

        if (evioVersion > 3) return eventCount;

        if (eventCount < 0) {
            // The difficulty is that this method can be called at
            // any time. So we need to save our state and then restore
            // it when we're done.
            ReaderState state = getState();

            rewind();
            eventCount = 0;

            while ((nextEvent()) != null) {
                eventCount++;
            }

            // Restore our original settings
            restoreState(state);
        }

        return eventCount;
    }

	/**
	 * Method used for diagnostics. It compares two event files. It checks the following (in order):<br>
	 * <ol>
	 * <li> That neither file is null.
	 * <li> That both files exist.
	 * <li> That neither file is a directory.
	 * <li> That both files can be read.
	 * <li> That both files are the same size.
	 * <li> That both files contain the same number of events.
	 * <li> Finally, that they are the same in a byte-by-byte comparison.
	 * </ol>
     * NOTE: Two files with the same events but different physical record size will be reported as different.
     *       They will fail the same size test.
     * @param evFile1 first file to be compared
     * @param evFile2 second file to be compared
	 * @return <code>true</code> if the files are, byte-by-byte, identical.
	 */
	public static boolean compareEventFiles(File evFile1, File evFile2) {

		if ((evFile1 == null) || (evFile2 == null)) {
			System.out.println("In compareEventFiles, one or both files are null.");
			return false;
		}

		if (!evFile1.exists() || !evFile2.exists()) {
			System.out.println("In compareEventFiles, one or both files do not exist.");
			return false;
		}

		if (evFile1.isDirectory() || evFile2.isDirectory()) {
			System.out.println("In compareEventFiles, one or both files is a directory.");
			return false;
		}

		if (!evFile1.canRead() || !evFile2.canRead()) {
			System.out.println("In compareEventFiles, one or both files cannot be read.");
			return false;
		}

		String name1 = evFile1.getName();
		String name2 = evFile2.getName();

		long size1 = evFile1.length();
		long size2 = evFile2.length();

		if (size1 == size2) {
			System.out.println(name1 + " and " + name2 + " have the same length: " + size1);
		}
		else {
			System.out.println(name1 + " and " + name2 + " have the different lengths.");
			System.out.println(name1 + ": " + size1);
			System.out.println(name2 + ": " + size2);
			return false;
		}

		try {
			EvioReader evioFile1 = new EvioReader(evFile1);
			EvioReader evioFile2 = new EvioReader(evFile2);
			int evCount1 = evioFile1.getEventCount();
			int evCount2 = evioFile2.getEventCount();
			if (evCount1 == evCount2) {
				System.out.println(name1 + " and " + name2 + " have the same #events: " + evCount1);
			}
			else {
				System.out.println(name1 + " and " + name2 + " have the different #events.");
				System.out.println(name1 + ": " + evCount1);
				System.out.println(name2 + ": " + evCount2);
				return false;
			}
		}
        catch (EvioException e) {
            e.printStackTrace();
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }


		System.out.print("Byte by byte comparison...");
		System.out.flush();

		int onetenth = (int)(1 + size1/10);

		//now a byte-by-byte comparison
		try {
			FileInputStream fis1 = new FileInputStream(evFile1);
			FileInputStream fis2 = new FileInputStream(evFile1);

			for (int i = 0; i < size1; i++) {
				try {
					int byte1 = fis1.read();
					int byte2 = fis2.read();

					if (byte1 != byte2) {
						System.out.println(name1 + " and " + name2 + " different at byte offset: " + i);
						return false;
					}

					if ((i % onetenth) == 0) {
						System.out.print(".");
						System.out.flush();
					}
				}
				catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}

			System.out.println("");

			try {
				fis1.close();
				fis2.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}


		System.out.println("files " + name1 + " and " + evFile2.getPath() + " are identical.");
		return true;
	}

}