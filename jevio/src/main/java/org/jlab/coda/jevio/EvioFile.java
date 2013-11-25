package org.jlab.coda.jevio;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.BitSet;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * This is the a class of interest to the user. Create an <code>EvioFile</code> from a <code>File</code>
 * object corresponding to an event file, and from this class you can test the file for consistency and,
 * more importantly, you can call <code>parseNextEvent</code> to get new events and to stream the embedded
 * structures to an IEvioListener.
 *
 * The streaming effect of parsing an event is that the parser will read the event and hand off structures,
 * such as banks, to any IEvioListeners. For those familiar with XML, the event is processed SAX-like.
 * It is up to the listener to decide what to do with the structures.
 * <p>
 * As an alternative to stream processing, after an event is parsed, the user can use the events treeModel
 * for access to the structures. For those familiar with XML, the event is processed DOM-like.
 * <p>
 * NOTE: Even though this class has a constructor that accepts an i/o mode, that is for backwards
 * compatibility only. An <code>EvioFile</code> is used for reading and parsing events only.
 * To write an event file, use an <code>EventWriter</code> object.
 *
 * @deprecated Use the EvioReader class instead which can read both files and buffers.
 *
 * @author heddle
 *
 */
public class EvioFile {

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

    /**
     * Offset to get magic number from start of file.
     */
    private static final int MAGIC_OFFSET = 28;

    /**
     * Offset to get version number from start of file.
     */
    private static final int VERSION_OFFSET = 20;

    /**
     * Mask to get version number from 6th int in block.
     */
    private static final int VERSION_MASK = 0xff;

	/**
	 * The buffer representing a map of the input file.
	 */
	private MappedByteBuffer mappedByteBuffer;

    /**
     * Endianness of the data, either {@link java.nio.ByteOrder#BIG_ENDIAN} or {@link java.nio.ByteOrder#LITTLE_ENDIAN}.
     */
    private ByteOrder byteOrder;

	/**
	 * The current block header for evio versions 1, 2 & 3.
	 */
    private BlockHeaderV2 blockHeader2 = new BlockHeaderV2();

    /**
     * The current block header for evio version 4.
     */
    private BlockHeaderV4 blockHeader4 = new BlockHeaderV4();

    /**
     * Reference to current block header, any version, through interface.
     */
    private IBlockHeader blockHeader;

	/**
	 * Root element tag for XML file
	 */
	private static final String ROOT_ELEMENT = "evio-data";

    /**
     * Version 4 files may have an xml format dictionary in the
     * first event of the first block;
     */
    private String xmlDictionary;

	/**
	 * Used to assign a transient number [1..n] to events as they are
	 */
	private int eventNumber = 0;

	/**
	 * This is the number of events in the file. It is not computed unless asked for, and if asked for it is computed
	 * and cached in this variable.
	 */
	private int eventCount = -1;

    /**
     * Evio version number (1-4). Obtain this by reading first block header.
     */
    private int evioVersion;

    /**
     * Absolute path of the underlying file.
     */
    private String path;

    /**
     * Parser object for this file/buffer.
     */
    private EventParser parser;


	/**
	 * Creates an event file for reading.
	 *
	 * @param file the file that contains EVIO events.
	 * @throws IOException
	 */
	public EvioFile(File file) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(file);
        path = file.getAbsolutePath();
		FileChannel inputChannel = fileInputStream.getChannel();
		mapFile(inputChannel);
		inputChannel.close(); // this object is no longer needed

        // Read first event in case it's a dictionary, we'll also
        // find the file's endianness & evio version #.
        try {
            nextEvent();
        }
        catch (EvioException e) {
            throw new IOException("Failed reading first event", e);
        }

        // reset buffer to beginning
        rewind();
        parser = new EventParser();
    }

	/**
	 * Creates an event file.
	 *
	 * @param path the full path to the file that contains events.
	 * For writing event files, use an <code>EventWriter</code> object.
	 * @see EventWriter
	 * @throws IOException
	 */
	public EvioFile(String path) throws IOException {
		this(new File(path));
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
     * Get the xml format dictionary is there is one.
     *
     * @return xml format dictionary, else null.
     */
    public String getXmlDictionary() {
        return xmlDictionary;
    }

    /**
     * Does this evio file have an associated dictionary?
     *
     * @return <code>true</code> if this evio file has an associated dictionary,
     *         else <code>false</code>
     */
    public boolean hasDictionary() {
        return xmlDictionary != null;
    }

    /**
     * Get the number of events remaining in the file.
     *
     * @return number of events remaining in the file
     * @throws org.jlab.coda.jevio.EvioException if failed reading from coda v3 file
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
	}

	/**
	 * Obtain the file size using the memory mapped buffer's capacity, which should be the same.
	 *
	 * @return the file size in bytes--actually the mapped memory size.
	 */
	public int fileSize() {
		return mappedByteBuffer.capacity();
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
     * Reads the block (physical record) header. Assumes the mapped buffer is positioned
     * at the start of the next block header (physical record.)<br>
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
        // have enough remaining?
        if (mappedByteBuffer.remaining() < 32) {
            mappedByteBuffer.clear();
            return ReadStatus.END_OF_FILE;
        }

        try {
            // Set the byte order to match the file's ordering.

            // Check the magic number for endianness. This requires
            // peeking ahead 7 ints or 24 bytes. Set the endianness
            // once we figure out what it is (buffer defaults to big endian).
            byteOrder = mappedByteBuffer.order();
            int magicNumber = mappedByteBuffer.getInt(MAGIC_OFFSET);

            if (magicNumber != IBlockHeader.MAGIC_NUMBER) {
                // Originally checked ByteOrder.nativeOrder() which is NOT what you want -  timmer
                if (byteOrder == ByteOrder.BIG_ENDIAN) {
                    byteOrder = ByteOrder.LITTLE_ENDIAN;
                }
                else {
                    byteOrder = ByteOrder.BIG_ENDIAN;
                }
                mappedByteBuffer.order(byteOrder);
            }

            // Check the version number. This requires peeking ahead 5 ints or 20 bytes.
            // Use the correct class for the block header once we know the version.
            int version = mappedByteBuffer.getInt(VERSION_OFFSET) & VERSION_MASK;

            try {
                if (version > 0 && version < 4) {
                    // cache the starting position
                    blockHeader2.setBufferStartingPosition(mappedByteBuffer.position());

                    // read the header data.
                    blockHeader2.setSize(mappedByteBuffer.getInt());
                    blockHeader2.setNumber(mappedByteBuffer.getInt());
                    blockHeader2.setHeaderLength(mappedByteBuffer.getInt());
                    blockHeader2.setStart(mappedByteBuffer.getInt());
                    blockHeader2.setEnd(mappedByteBuffer.getInt());
                    // skip version
                    mappedByteBuffer.getInt();
                    blockHeader2.setVersion(version);
                    blockHeader2.setReserved1(mappedByteBuffer.getInt());
                    blockHeader2.setMagicNumber(mappedByteBuffer.getInt());
                    blockHeader = blockHeader2;
                }
                else if (version >= 4) {
                    // cache the starting position
                    blockHeader4.setBufferStartingPosition(mappedByteBuffer.position());

                    // read the header data.
                    blockHeader4.setSize(mappedByteBuffer.getInt());
                    blockHeader4.setNumber(mappedByteBuffer.getInt());
                    blockHeader4.setHeaderLength(mappedByteBuffer.getInt());
                    blockHeader4.setEventCount(mappedByteBuffer.getInt());
                    // unused
                    mappedByteBuffer.getInt();
                    // use 6th word to set bit info
                    blockHeader4.parseToBitInfo(mappedByteBuffer.getInt());
                    blockHeader4.setVersion(version);
                    // unused
                    mappedByteBuffer.getInt();
                    blockHeader4.setMagicNumber(mappedByteBuffer.getInt());
                    blockHeader = blockHeader4;
                }
                else {
                    // error
System.err.println("ERROR unsupported evio version number in file");
                    return ReadStatus.EVIO_EXCEPTION;
                }

                // each file is restricted to one version, so set it once
                if (evioVersion < 1) {
                    evioVersion = version;
                }
            }
            catch (EvioException e) {
                e.printStackTrace();
                return ReadStatus.EVIO_EXCEPTION;
            }
        }
        catch (BufferUnderflowException a) {
System.err.println("ERROR endOfBuffer " + a);
            mappedByteBuffer.clear();
            return ReadStatus.UNKNOWN_ERROR;
        }

        return ReadStatus.SUCCESS;
    }


   /**
     * Get the next event in the file. As useful as this sounds, most applications will probably call
     * {@link #parseNextEvent() parseNextEvent} instead, since it combines combines getting the next
     * event with parsing the next event.<p>
     * In evio version 4, events no longer cross block boundaries. There are only one or more
     * complete events in each block. No changes were made to this method from versions 2 & 3 in order
     * to read the version 4 format as it is subset of versions 1-3 with variable block length.
     *
     * @return the next event in the file. On error it throws an EvioException.
     *         On end of file, it returns <code>null</code>.
     * @throws EvioException
     */
    public synchronized EvioEvent nextEvent() throws EvioException {
        EvioEvent event = new EvioEvent();
        BaseStructureHeader header = event.getHeader();

        // Are we at the top of the file?
        boolean isFirstEvent = false;
        if (mappedByteBuffer.position() == 0) {
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
        else if (blockHeader.getBufferEndingPosition() == mappedByteBuffer.position()) {
            return null;
        }

        // Version     4: once here, we are assured the entire next event is in this block.
        // Version 1,2,3: no matter what, we can get the length of the next event.
        // A non positive length indicates eof;
        int length = mappedByteBuffer.getInt();
        if (length < 1) {
            return null;
        }

        header.setLength(length);
        bytesRemaining -= 4; // just read in 4 bytes

        // Version 2: if we were unlucky, after reading the length there are no bytes remaining in this bank.
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

        // now should be good to go, except data may cross block boundary
        // in any case, should be able to read the rest of the header.
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            // interested in bit pattern, not negative numbers
            header.setTag(ByteDataTransformer.shortBitsToInt(mappedByteBuffer.getShort()));
            int dt = ByteDataTransformer.byteBitsToInt(mappedByteBuffer.get());
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
            header.setNumber(ByteDataTransformer.byteBitsToInt(mappedByteBuffer.get()));
        }
        else {
            header.setNumber(ByteDataTransformer.byteBitsToInt(mappedByteBuffer.get()));
            int dt = ByteDataTransformer.byteBitsToInt(mappedByteBuffer.get());
            int type = dt & 0x3f;
            int padding = dt >>> 6;
            if (dt == 0x40) {
                type = DataType.TAGSEGMENT.getValue();
                padding = 0;
            }
            header.setDataType(type);
            header.setPadding(padding);

            event.setXmlNames();
            header.setTag(ByteDataTransformer.shortBitsToInt(mappedByteBuffer.getShort()));
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
                    mappedByteBuffer.get(bytes, offset, bytesRemaining);

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
            mappedByteBuffer.get(bytes, offset, bytesToGo);
            event.setRawBytes(bytes);
            event.setByteOrder(byteOrder); // add this to track endianness, timmer

            // if this is the very first event, check to see if it's a dictionary
            if (isFirstEvent && blockHeader.hasDictionary()) {
                // if we've NOT already been through this before, extract the dictionary
                if (xmlDictionary == null) {
                    xmlDictionary = event.getStringData()[0];
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
	 * @throws EvioException
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
	 * As useful as this sounds, most applications will probably call {@link #parseNextEvent() parseNextEvent} instead,
	 * since it combines combines getting the next event with parsing the next event .
	 *
	 * @param evioEvent the event to parse.
	 * @throws EvioException
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
			return blockHeader.bytesRemaining(mappedByteBuffer.position());
		}
		catch (EvioException e) {
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * The equivalent of rewinding the file. What it actually does is set the position of the mapped memory buffer back
	 * to 0. This method, along with the two <code>position</code> and the <code>close</code> method, allows
	 * applications to treat this in a normal random access file manner.
	 */
	public void rewind() {
		mappedByteBuffer.position(0);
		eventNumber = 0;
	}

	/**
	 * This is equivalent to obtaining the current position in the file. What it actually does is return the position of
	 * the mapped memory buffer. This method, along with the <code>rewind</code>, <code>position(int)</code> and the
	 * <code>close</code> method, allows applications to treat this in a normal random access file manner.
	 *
	 * @return the position of the file.
	 */
	public int position() {
		return mappedByteBuffer.position();
	}

	/**
	 * This is equivalent to setting the current position in the file. What it actually does is set the position of the
	 * mapped memory buffer. This method, along with the <code>rewind</code>, <code>position()</code> and the
	 * <code>close</code> method, allows applications to treat this in a normal random access file manner.
	 *
	 * @param position the new position of the file.
	 */
	public void position(int position) {
		mappedByteBuffer.position(position);
	}

	/**
	 * This is equivalent to closing the file. What it actually does is clear all data from the mapped memory buffer,
	 * and sets its position to 0. This method, along with the <code>rewind</code> and the two <code>position()</code>
	 * methods, allows applications to treat this in a normal random access file manner.
	 */
	public void close() {
		mappedByteBuffer.clear();
	}

	/**
	 * This returns the current (active) block (physical record) header. Since most users have no interest in physical
	 * records, this method should not be used often. Mostly it is used by the test programs in the
	 * <code>EvioFileTest</code> class.
	 *
	 * @return the current block header.
	 */
	public IBlockHeader getCurrentBlockHeader() {
		return blockHeader;
	}

	/**
	 * Go to a specific event in the file. The events are numbered 1..N.
     * This number is transient--it is not part of the event as stored in the evio file.
	 *
	 * @param evNumber the event number in a 1..N counting sense, from the start of the file.
     * @return the specified event in the file.
	 */
	public EvioEvent gotoEventNumber(int evNumber) {

		if (evNumber < 1) {
			return null;
		}

		// rewind
		rewind();
		EvioEvent event;

		try {
			// get the first evNumber - 1 events without parsing
			for (int i = 1; i < evNumber; i++) {
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
	 * Rewrite the entire file to XML.
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
	 * @see org.jlab.coda.jevio.IEvioProgressListener
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

			// now loop through the events
			// rewind
			rewind();
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
			xmlWriter.writeCharacters("\n");
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

		rewind();
		System.out.println("XML write was successful");
		return WriteStatus.SUCCESS;
	}

    /**
     * This is the number of events in the file. It is not computed unless asked for,
     * and if asked for it is computed and cached. Any dictionary event is <b>not</b>
     * included in the count.
     *
     * @return the number of events in the file.
     * @throws EvioException
     */
    public int getEventCount() throws EvioException {
System.out.println("\nCalling getEventCount()\n");
        if (eventCount < 0) {
            rewind();
            eventCount = 0;

            // In evio version 4+, each block stores the number of events in its header
            // so there's no need to parse through all the events again.
            if (evioVersion >= 4) {
                ReadStatus status;
                while (true) {
                    status = nextBlockHeader();
                    if (status == ReadStatus.END_OF_FILE) {
                        break;
                    }
                    else if (status != ReadStatus.SUCCESS) {
                        throw new EvioException("Failed reading block header while counting events.");
                    }
                    // note: getEventCount() does not include dictionary
                    eventCount += blockHeader4.getEventCount();
                    mappedByteBuffer.position((int)blockHeader4.nextBufferStartingPosition());
                }
            }
            else {
                while ((nextEvent()) != null) {
                        eventCount++;
                }
            }

            rewind();
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
	 * NOTE: Two files with the same events but different physical record size will be reported as different. They will fail the same size test.
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
			EvioFile evioFile1 = new EvioFile(evFile1);
			EvioFile evioFile2 = new EvioFile(evFile2);
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