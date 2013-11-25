package org.jlab.coda.jevio;

import org.jlab.coda.jevio.EvioReader.ReadStatus;


/**
 * A set of static functions that test evio files. It also has some other diagnostic methods for getting counts.
 * 
 * @author heddle
 * 
 */
public class EvioFileTest {

	/**
	 * This enum is used for file testing. <br>
	 * PASS indicates that a given test passed. <br>
	 * FAIL indicates that a given test failed.
	 */
	public static enum TestResult {
		PASS, FAIL
	}

	/**
	 * Get a total count of the number of physical records.
	 * 
	 * @param reader reader of the file to be processed.
	 * @return the total count of blocks (physical records.)
	 */
	public static int totalBlockCount(EvioReader reader) {
		reader.rewind();
		ReadStatus status = EvioReader.ReadStatus.SUCCESS;
		int count = 0;

		while (status == ReadStatus.SUCCESS) {
			status = reader.nextBlockHeader();
			if (status == ReadStatus.SUCCESS) {
				reader.position((int)reader.getCurrentBlockHeader().nextBufferStartingPosition());
				count++;
			}
		}

		System.out.println("total block count: " + count);

		reader.rewind();
		return count;
	}


	/**
	 * Tests whether we can look through the file and find all the block headers.
	 * 
     * @param reader reader of the file to be processed.
	 * @return the result of this test, either PASS or FAIL.
	 */
	public static TestResult readAllBlockHeadersTest(EvioReader reader) {
		reader.rewind();
		ReadStatus status = ReadStatus.SUCCESS;
		int blockCount = 0;

		while (status == ReadStatus.SUCCESS) {
			status = reader.nextBlockHeader();
			if (status == ReadStatus.SUCCESS) {
				reader.position((int)reader.getCurrentBlockHeader().nextBufferStartingPosition());
				blockCount++;
			}
		}

		TestResult result;

		// should have read to the end of the file
		if (status == ReadStatus.END_OF_FILE) {
			System.out.println("Total blocks read: " + blockCount);
			result = TestResult.PASS;
		}
		else {
			result = TestResult.FAIL;
		}

		System.out.println("readAllBlockHeadersTest: " + result);

		reader.rewind();
		return result;
    }


	/**
	 * Tests whether we can look through the file read all the events.
	 * 
     * @param reader reader of the file to be processed.
	 * @return the result of this test, either <code>TestResult.PASS</code> or <code>TestResult.FAIL</code>.
	 */
	public static TestResult readAllEventsTest(EvioReader reader) {
		// store current file position
		int oldPosition = reader.position();

		reader.rewind();
		EvioEvent event;
		int count = 0;
		TestResult result = TestResult.PASS;

		try {
			while ((event = reader.nextEvent()) != null) {
				count++;
				BaseStructureHeader header = event.getHeader();

				System.out.println(count + ")  size: " + header.getLength() + " type: " + header.getDataTypeName()
						+ " \"" + event.getDescription() + "\"");
			}
		}
		catch (EvioException e) {
			e.printStackTrace();
			result = TestResult.FAIL;
		}

		System.out.println("readAllBlockHeadersTest: " + result);

		// restore file position
		reader.position(oldPosition);
		return result;
	}


	/**
	 * Tests whether we can parse events from the file.
	 * 
     * @param reader reader of the file to be processed.
	 * @param num the number to parse. Will try to parse this many (unless it runs out.) Use -1 to parse all events.
	 *            Note: if <code>num</code> is greater than the number of events in the file, it doesn't constitute an
	 *            error.
	 * @return the result of this test, either <code>TestResult.PASS</code> or <code>TestResult.FAIL</code>.
	 */
	public static TestResult parseEventsTest(EvioReader reader, int num) {
		// store current file position
		int oldPosition = reader.position();

		if (num < 0) {
			num = Integer.MAX_VALUE;
		}

		reader.rewind();
		EvioEvent event;
		int count = 0;
		TestResult result = TestResult.PASS;

		try {
			while ((count < num) && ((event = reader.nextEvent()) != null)) {
				reader.parseEvent(event);
				count++;
			}
		}
		catch (EvioException e) {
			e.printStackTrace();
			result = TestResult.FAIL;
		}

		System.out.println("parseEventsTest parsed: " + count + " events");
		System.out.println("parseEventsTest result: " + result);

		// restore file position
		reader.position(oldPosition);
		return result;
	}

}
