package org.tarrio.dilate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.tarrio.dilate.LookupBuffer.Match;

import junit.framework.TestCase;

public class LookupBufferTest extends TestCase {

	public void testReadByteArrays() throws Exception {
		byte[] testData = "abcdef".getBytes();
		LookupBuffer buffer = makeBuffer(testData);
		byte[] readData = new byte[4];
		assertEquals(4, buffer.read(readData, 4));
		assertByteArrayEquals(testData, readData, 0, 0, 4);
		assertEquals(2, buffer.read(readData, 4));
		assertByteArrayEquals(testData, readData, 4, 0, 2);
		assertEquals(-1, buffer.read(readData, 1));
	}

	public void testReadSmallBuffer() throws Exception {
		byte[] testData = "abcdef".getBytes();
		LookupBuffer buffer = makeBuffer(testData, 3);
		byte[] readData = new byte[3];
		assertEquals(1, buffer.read(readData, 1));
		assertEquals('a', readData[0]);
		assertEquals(3, buffer.read(readData, 3));
		assertByteArrayEquals(testData, readData, 1, 0, 3);
		assertEquals(1, buffer.read(readData, 1));
		assertEquals('e', readData[0]);
		assertEquals(1, buffer.read(readData, 3));
		assertByteArrayEquals(testData, readData, 5, 0, 1);
		assertEquals(-1, buffer.read(readData, 1));
	}

	public void testFindPastMatch() throws Exception {
		byte[] testData = "12345678903456a34567b345c34d".getBytes();
		LookupBuffer buffer = makeBuffer(testData);
		for (int i = 0; i < 10; ++i) {
			assertNull(buffer.findPastMatch());
			buffer.skip(1);
		}
		Match match = buffer.findPastMatch();
		assertNotNull(match);
		assertEquals(8, match.getDistance());
		assertEquals(4, match.getLength());
		buffer.skip(match.getLength());
		assertNull(buffer.findPastMatch());
		buffer.skip(1);
		match = buffer.findPastMatch();
		assertNotNull(match);
		assertEquals(13, match.getDistance());
		assertEquals(5, match.getLength());
		buffer.skip(match.getLength() + 1);
		match = buffer.findPastMatch();
		assertNotNull(match);
		assertEquals(6, match.getDistance());
		assertEquals(3, match.getLength());
		buffer.skip(match.getLength() + 1);
		assertNull(buffer.findPastMatch());
	}

	public void testFindPastMatchSmallBuffer() throws Exception {
		byte[] testData = "12345678903456a34567b345c34d".getBytes();
		LookupBuffer buffer = makeBuffer(testData, 15, 6);
		for (int i = 0; i < 10; ++i) {
			assertNull(buffer.findPastMatch());
			buffer.skip(1);
		}
		assertNull(buffer.findPastMatch());
		buffer.skip(4);
		assertNull(buffer.findPastMatch());
		buffer.skip(1);
		Match match = buffer.findPastMatch();
		assertNotNull(match);
		assertEquals(5, match.getDistance());
		assertEquals(4, match.getLength());
		buffer.skip(match.getLength() + 2);
		match = buffer.findPastMatch();
		assertNotNull(match);
		assertEquals(6, match.getDistance());
		assertEquals(3, match.getLength());
		buffer.skip(match.getLength() + 1);
		assertNull(buffer.findPastMatch());
	}
	
	public void testFindPastMatchOnlyOldMatches() throws Exception {
		byte[] testData = "12345678903456".getBytes();
		LookupBuffer buffer = makeBuffer(testData, 8, 4);
		for (int i = 0; i < 10; ++i) {
			assertNull(buffer.findPastMatch());
			buffer.skip(1);
		}
		assertNull(buffer.findPastMatch());		
	}
	
	public void testFindPastMatchRepeatedString() throws Exception {
		byte[] testData = "121212121212".getBytes();
		LookupBuffer buffer = makeBuffer(testData);
		assertNull(buffer.findPastMatch());
		buffer.skip(1);
		assertNull(buffer.findPastMatch());
		buffer.skip(1);
		Match match = buffer.findPastMatch();
		assertNotNull(match);
		assertEquals(2, match.getDistance());
		assertEquals(10, match.getLength());
	}
	
	public void testWriteSingleBytes() throws Exception {
		byte[] testData = "abcdef".getBytes();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		LookupBuffer buffer = new LookupBuffer(stream);
		for (int i = 0; i < testData.length; ++i) {
			buffer.write(testData[i]);
		}
		assertByteArrayEquals(testData, stream.toByteArray(), 0, 0, testData.length);
	}
	
	public void testWriteByteArrays() throws Exception {
		byte[] testData = "abcdef".getBytes();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		LookupBuffer buffer = new LookupBuffer(stream);
		buffer.write(testData, 0, 4);
		buffer.write(testData, 4, 2);
		assertByteArrayEquals(testData, stream.toByteArray(), 0, 0, testData.length);
	}

	public void testWriteSmallBuffer() throws Exception {
		byte[] testData = "abcdef".getBytes();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		LookupBuffer buffer = new LookupBuffer(stream, 3, 0, 3);
		buffer.write(testData[0]);
		buffer.write(testData, 1, 3);
		buffer.write(testData[4]);
		buffer.write(testData, 5, 1);
		assertByteArrayEquals(testData, stream.toByteArray(), 0, 0, testData.length);
	}
	
	public void testRepeatPastMatch() throws Exception {
		byte[] testData = "12345678903456a34567b3456".getBytes();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		LookupBuffer buffer = new LookupBuffer(stream);
		buffer.write(testData, 0, 10);
		buffer.repeatPastMatch(8, 4);
		buffer.write((byte) 'a');
		buffer.repeatPastMatch(13, 5);
		buffer.write((byte) 'b');
		buffer.repeatPastMatch(6, 4);
		assertByteArrayEquals(testData, stream.toByteArray(), 0, 0, testData.length);
	}

	public void testRepeatPastMatchRepeatedString() throws Exception {
		byte[] testData = "121212121212".getBytes();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		LookupBuffer buffer = new LookupBuffer(stream);
		buffer.write("12".getBytes());
		buffer.repeatPastMatch(2, 10);
		assertByteArrayEquals(testData, stream.toByteArray(), 0, 0, testData.length);
	}

	private LookupBuffer makeBuffer(byte[] testData) {
		return new LookupBuffer(new ByteArrayInputStream(testData));
	}
	
	private LookupBuffer makeBuffer(byte[] testData, int bufferLength) {
		return new LookupBuffer(new ByteArrayInputStream(testData), bufferLength, 0, bufferLength);
	}

	private LookupBuffer makeBuffer(byte[] testData, int bufferLength, int lookupLength) {
		return new LookupBuffer(new ByteArrayInputStream(testData), bufferLength, lookupLength, lookupLength);
	}


	private void assertByteArrayEquals(byte[] expected, byte[] actual, int expectedOffset, int actualOffset, int length) {
		for (int i = 0; i < length; ++i) {
			assertEquals(expected[expectedOffset + i], actual[actualOffset + i]);
		}
	}
}
