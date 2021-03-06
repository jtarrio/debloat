/**
 * Copyright 2012 Jacobo Tarrio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tarrio.debloat.buffers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.tarrio.debloat.buffers.RingBuffer;
import org.tarrio.debloat.buffers.RingBufferImpl;
import org.tarrio.debloat.buffers.RingBuffer.Match;

import junit.framework.TestCase;

/**
 * Tests for {@link RingBufferImpl}.
 *
 * @author Jacobo Tarrio
 */
public class RingBufferImplTest extends TestCase {

	public void testReadByteArrays() throws Exception {
		byte[] testData = "abcdef".getBytes();
		RingBuffer buffer = makeBuffer(testData);
		byte[] readData = new byte[4];
		assertEquals(4, buffer.read(readData, 4));
		assertByteArrayEquals(testData, readData, 0, 0, 4);
		assertEquals(2, buffer.read(readData, 4));
		assertByteArrayEquals(testData, readData, 4, 0, 2);
		assertEquals(-1, buffer.read(readData, 1));
	}

	public void testReadSmallBuffer() throws Exception {
		byte[] testData = "abcdef".getBytes();
		RingBuffer buffer = makeBuffer(testData, 3);
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
		RingBuffer buffer = makeBuffer(testData);
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
		RingBuffer buffer = makeBuffer(testData, 15, 6);
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
		RingBuffer buffer = makeBuffer(testData, 8, 4);
		for (int i = 0; i < 10; ++i) {
			assertNull(buffer.findPastMatch());
			buffer.skip(1);
		}
		assertNull(buffer.findPastMatch());
	}

	public void testFindPastMatchRepeatedString() throws Exception {
		byte[] testData = "121212121212".getBytes();
		RingBuffer buffer = makeBuffer(testData);
		assertNull(buffer.findPastMatch());
		buffer.skip(1);
		assertNull(buffer.findPastMatch());
		buffer.skip(1);
		Match match = buffer.findPastMatch();
		assertNotNull(match);
		assertEquals(2, match.getDistance());
		assertEquals(10, match.getLength());
	}

	public void testFindPastMatchWithMaxDistanceAndLength() throws Exception {
		Match match = getMatchForDistanceAndLength(32768, 258);
		assertNotNull(match);
		assertEquals(32768, match.getDistance());
		assertEquals(258, match.getLength());
	}

	public void testFindPastMatchCantExceedMaxLength() throws Exception {
		Match match = getMatchForDistanceAndLength(32768, 300);
		assertNotNull(match);
		assertEquals(32768, match.getDistance());
		assertEquals(258, match.getLength());
	}

	public void testFindPastMatchCantExceedMaxDistance() throws Exception {
		Match match = getMatchForDistanceAndLength(32769, 258);
		assertNotNull(match);
		// It finds the 00 00 01 at position 511 (FF 00 / 00 01 at position
		// 510).
		assertEquals(32258, match.getDistance());
		assertEquals(3, match.getLength());
	}

	private Match getMatchForDistanceAndLength(int distance, int length)
			throws IOException {
		byte[] testData = new byte[distance + length];
		fillWithNumbers(testData, 0, distance);
		fillWithNumbers(testData, distance, length);
		RingBuffer buffer = makeBuffer(testData);
		buffer.skip(distance);
		Match match = buffer.findPastMatch();
		return match;
	}

	private void fillWithNumbers(byte[] buffer, int offset, int length) {
		for (int i = 0; i < length; ++i) {
			int counter = i / 2;
			int bytePos = i % 2;
			buffer[i + offset] = (byte) ((counter & (0xff << (8 * bytePos))) >> (8 * bytePos));
		}
	}

	public void testWriteSingleBytes() throws Exception {
		byte[] testData = "abcdef".getBytes();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RingBuffer buffer = makeBuffer(stream);
		for (int i = 0; i < testData.length; ++i) {
			buffer.write(testData[i]);
		}
		assertByteArrayEquals(testData, stream.toByteArray(), 0, 0,
				testData.length);
	}

	public void testWriteByteArrays() throws Exception {
		byte[] testData = "abcdef".getBytes();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RingBuffer buffer = makeBuffer(stream);
		buffer.write(testData, 0, 4);
		buffer.write(testData, 4, 2);
		assertByteArrayEquals(testData, stream.toByteArray(), 0, 0,
				testData.length);
	}

	public void testWriteSmallBuffer() throws Exception {
		byte[] testData = "abcdef".getBytes();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RingBuffer buffer = makeBuffer(stream, 3, 0, 3);
		buffer.write(testData[0]);
		buffer.write(testData, 1, 3);
		buffer.write(testData[4]);
		buffer.write(testData, 5, 1);
		assertByteArrayEquals(testData, stream.toByteArray(), 0, 0,
				testData.length);
	}

	public void testRepeatPastMatch() throws Exception {
		byte[] testData = "12345678903456a34567b3456".getBytes();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RingBuffer buffer = makeBuffer(stream);
		buffer.write(testData, 0, 10);
		buffer.repeatPastMatch(8, 4);
		buffer.write((byte) 'a');
		buffer.repeatPastMatch(13, 5);
		buffer.write((byte) 'b');
		buffer.repeatPastMatch(6, 4);
		assertByteArrayEquals(testData, stream.toByteArray(), 0, 0,
				testData.length);
	}

	public void testRepeatPastMatchRepeatedString() throws Exception {
		byte[] testData = "121212121212".getBytes();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RingBuffer buffer = makeBuffer(stream);
		buffer.write("12".getBytes());
		buffer.repeatPastMatch(2, 10);
		assertByteArrayEquals(testData, stream.toByteArray(), 0, 0,
				testData.length);
	}

	private RingBuffer makeBuffer(byte[] testData) {
		return new RingBufferImpl(new ByteArrayInputStream(testData));
	}

	private RingBuffer makeBuffer(byte[] testData, int bufferLength) {
		return new RingBufferImpl(new ByteArrayInputStream(testData),
				bufferLength + 1, 0, bufferLength);
	}

	private RingBuffer makeBuffer(byte[] testData, int bufferLength,
			int lookupLength) {
		return new RingBufferImpl(new ByteArrayInputStream(testData),
				bufferLength, lookupLength, bufferLength - lookupLength - 1);
	}

	private RingBuffer makeBuffer(ByteArrayOutputStream stream) {
		return new RingBufferImpl(stream);
	}

	private RingBuffer makeBuffer(ByteArrayOutputStream stream,
			int bufferLength, int maxDistance, int maxLength) {
		return new RingBufferImpl(stream, maxDistance, maxLength);
	}

	private void assertByteArrayEquals(byte[] expected, byte[] actual,
			int expectedOffset, int actualOffset, int length) {
		for (int i = 0; i < length; ++i) {
			assertEquals(expected[expectedOffset + i], actual[actualOffset + i]);
		}
	}
}
