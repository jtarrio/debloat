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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A circular buffer implementation that keeps enough space for as many bytes
 * for the maximum match distance plus the maximum length plus one extra byte.
 * 
 * That extra byte serves to be able to distinguish between completely empty and
 * completely full buffers.
 * 
 * @author Jacobo Tarrio
 */
public class RingBufferImpl implements RingBuffer {

	/**
	 * The default maximum match distance.
	 */
	private static final int DEFAULT_MAX_DISTANCE = 32768;

	/**
	 * The default maximum match length.
	 */
	private static final int DEFAULT_MAX_LENGTH = 258;

	private final InputStream inputStream;
	private final OutputStream outputStream;
	private final int maxDistance;
	private final int maxLength;
	private final byte[] buffer;
	private int bufPosOffset;
	private int bufBottom;
	private int bufPos;
	private int bufTop;
	private boolean eof;
	private PositionMap positionMap;

	/**
	 * Creates a ringbuffer to read data from a given input stream.
	 * 
	 * @param inputStream
	 *            The stream to read the data from.
	 */
	RingBufferImpl(InputStream inputStream) {
		this(inputStream, 2 * DEFAULT_MAX_DISTANCE, DEFAULT_MAX_DISTANCE, DEFAULT_MAX_LENGTH);
	}

	/**
	 * Creates a read ringbuffer with the given size parameters.
	 * 
	 * @param inputStream
	 *            The stream to read the data from.
	 * @param maxDistance
	 *            Maximum match distance.
	 * @param maxLength
	 *            Maximum match length.
	 */
	RingBufferImpl(InputStream inputStream, int bufferSize, int maxDistance, int maxLength) {
		this.inputStream = inputStream;
		this.outputStream = null;
		this.maxDistance = maxDistance;
		this.maxLength = maxLength;
		this.buffer = new byte[Math.max(bufferSize, maxDistance + maxLength + 1)];
		this.bufPosOffset = 0;
		this.bufBottom = 0;
		this.bufPos = 0;
		this.bufTop = 0;
		this.eof = false;
		this.positionMap = new PositionMap();
	}

	/**
	 * Creates a ringbuffer to write data to a given output stream.
	 * 
	 * @param outputStream
	 *            The stream to write the data to.
	 */
	RingBufferImpl(OutputStream outputStream) {
		this(outputStream, DEFAULT_MAX_DISTANCE, DEFAULT_MAX_LENGTH);
	}

	/**
	 * Creates a write ringbuffer with the given size parameters.
	 * 
	 * @param outputStream
	 *            The stream to write the data to.
	 * @param maxDistance
	 *            Maximum match distance.
	 * @param maxLength
	 *            Maximum match length.
	 */
	RingBufferImpl(OutputStream outputStream, int maxDistance, int maxLength) {
		this.outputStream = outputStream;
		this.inputStream = null;
		this.maxDistance = maxDistance;
		this.maxLength = maxLength;
		this.buffer = new byte[maxDistance + maxLength + 1];
		this.bufBottom = 0;
		this.bufPos = 0;
	}

	@Override
	public int read(byte[] destBuffer, int length) throws IOException {
		int readSoFar = 0;
		while (readSoFar < length) {
			int read = partialRead(destBuffer, readSoFar, length - readSoFar);
			if (read == -1) {
				break;
			}
			readSoFar += read;
		}
		return readSoFar == 0 && eof ? -1 : readSoFar;
	}

	@Override
	public int skip(int length) throws IOException {
		return read(null, length);
	}

	@Override
	public Match findPastMatch() throws IOException {
		fillBuffer();
		Match bestMatch = null;
		Position position = positionMap.getFirstMatchingPosition();
		while (position != null) {
			int distance = position.getDistance();
			if (distance > maxDistance) {
				break;
			}
			if (distance >= 1) {
				int length = checkMatch(distance);
				if (length >= 3
						&& (bestMatch == null || bestMatch.getLength() < length)) {
					bestMatch = new MatchImpl(distance, length);
				}
			}
			position = position.getNext();
		}
		return bestMatch;
	}

	@Override
	public void write(byte b) throws IOException {
		makeRoom(1);
		buffer[bufPos] = b;
		outputStream.write(b);
		++bufPos;
		bufPos %= buffer.length;
	}

	@Override
	public void write(byte[] data) throws IOException {
		write(data, 0, data.length);
	}

	@Override
	public void write(byte[] data, int offset, int length) throws IOException {
		makeRoom(length);
		int writtenSoFar = 0;
		while (writtenSoFar < length) {
			writtenSoFar += partialWrite(data, offset + writtenSoFar, length
					- writtenSoFar);
		}
	}

	@Override
	public void repeatPastMatch(int distance, int length) throws IOException {
		if (distance < 1 || distance > maxDistance) {
			throw new IndexOutOfBoundsException(
					"Repeat distance is not valid: " + distance);
		}
		if (length < 3 || length > maxLength) {
			throw new IndexOutOfBoundsException("Repeat length is not valid: "
					+ length);
		}
		int past = (bufPos + buffer.length - distance) % buffer.length;
		for (int i = 0; i < length; ++i) {
			write(buffer[past]);
			++past;
			past %= buffer.length;
		}
	}

	/**
	 * Reads a contiguous sequence of bytes until a buffer boundary has been
	 * reached.
	 * 
	 * @param destBuffer
	 *            The byte array to write the read data to.
	 * @param offset
	 *            The starting offset within destBuffer to start writing the
	 *            data in.
	 * @param length
	 *            The number of bytes to write into destBuffer.
	 * @return The number of bytes that were read, or -1 if no bytes could be
	 *         read.
	 * @throws IOException
	 *             If there was a problem reading from the buffer.
	 */
	private int partialRead(byte[] destBuffer, int offset, int length)
			throws IOException {
		fillBuffer();
		int maxRead = Math.min(length, (bufTop >= bufPos ? bufTop
				: buffer.length) - bufPos);
		if (maxRead == 0) {
			return -1;
		}
		if (destBuffer != null) {
			System.arraycopy(buffer, bufPos, destBuffer, offset, maxRead);
		}
		bufPos += maxRead;
		bufPos %= buffer.length;
		bufPosOffset += maxRead;
		return maxRead;
	}

	/**
	 * Discards old data and fills the buffer with data from the input stream.
	 * 
	 * @throws IOException
	 *             If there was a problem reading from the stream.
	 */
	private void fillBuffer() throws IOException {
		if (inputStream == null) {
			throw new IllegalStateException("Cannot read from write buffer");
		}
		if (eof) {
			return;
		}
		int usedBelow = (bufPos + buffer.length - bufBottom) % buffer.length;
		int usedAbove = (bufTop + buffer.length - bufPos) % buffer.length;
		if (usedBelow > maxDistance && usedAbove <= maxLength) {
			int discard = usedBelow - maxDistance;
			bufBottom += discard;
			bufBottom %= buffer.length;
			usedBelow -= discard;
		}
		int available = buffer.length - usedBelow - usedAbove - 1;
		while (available > 0 && !eof) {
			available -= partialFillBuffer(available);
		}
	}

	/**
	 * Reads from the input stream and writes into a contiguous area in the
	 * buffer until a buffer boundary is reached.
	 * 
	 * @param length
	 *            The desired number of bytes to read.
	 * @return The number of bytes that were read. When the end of the input
	 *         stream is reached, the eof attribute is set to true.
	 * @throws IOException
	 *             If there was a problem reading from the stream.
	 */
	private int partialFillBuffer(int length) throws IOException {
		int maxRead = Math.min(length, (bufBottom > bufTop ? bufBottom
				: buffer.length) - bufTop);
		int read = inputStream.read(buffer, bufTop, maxRead);
		if (read == -1) {
			eof = true;
			return 0;
		}
		bufTop += read;
		bufTop %= buffer.length;
		positionMap.indexPositions(read);
		return read;
	}

	/**
	 * Checks the length of the match (potentially) starting at a given
	 * distance.
	 * 
	 * @param distance
	 *            The match distance.
	 * @return The length of the match, from 0 up to maxLength.
	 */
	private int checkMatch(int distance) {
		int j;
		for (j = 0; j < maxLength && (bufPos + j) % buffer.length != bufTop; ++j) {
			if (buffer[(bufPos - distance + j + buffer.length) % buffer.length] != buffer[(bufPos + j)
					% buffer.length]) {
				break;
			}
		}
		return j;
	}

	/**
	 * Writes a contiguous sequence of bytes to the buffer and to the output
	 * stream until a buffer boundary is reached.
	 * 
	 * @param data
	 *            A byte array containing the data to write.
	 * @param offset
	 *            The offset within the array to start writing at.
	 * @param length
	 *            The number of bytes to write.
	 * @return The number of bytes that were written.
	 * @throws IOException
	 *             If there was a problem writing to the stream.
	 */
	private int partialWrite(byte[] data, int offset, int length)
			throws IOException {
		int maxWrite = Math.min(length, (bufBottom > bufPos ? bufBottom
				: buffer.length) - bufPos);
		System.arraycopy(data, offset, buffer, bufPos, maxWrite);
		outputStream.write(data, offset, maxWrite);
		bufPos += maxWrite;
		bufPos %= buffer.length;
		return maxWrite;
	}

	/**
	 * Discards old data and makes enough space in the buffer to store a given
	 * number of bytes of data.
	 * 
	 * @param length
	 *            The number of bytes to make space for.
	 */
	private void makeRoom(int length) {
		if (outputStream == null) {
			throw new IllegalStateException("Cannot write to read buffer");
		}
		int usedBelow = (bufPos + buffer.length - bufBottom) % buffer.length;
		if (usedBelow > maxDistance) {
			int discard = usedBelow - maxDistance;
			bufBottom += discard;
			bufBottom %= buffer.length;
			usedBelow -= discard;
		}
	}

	/**
	 * An implementation of RingBuffer.Match.
	 */
	private static class MatchImpl implements Match {
		private final int distance;
		private final int length;

		public MatchImpl(int distance, int length) {
			this.distance = distance;
			this.length = length;
		}

		@Override
		public int getDistance() {
			return distance;
		}

		@Override
		public int getLength() {
			return length;
		}
	}

	/**
	 * A hash map of linked lists of positions within the buffer where certain
	 * byte sequences may appear.
	 * 
	 * The positions are stored in increasing distance order and are not cleaned
	 * automatically.
	 */
	private class PositionMap {
		private static final int KEY_LENGTH = 3;
		private static final int BUCKETS = 32771;
		private Position[] positions = new Position[BUCKETS];

		/**
		 * Indexes the last bytes read in the buffer.
		 * 
		 * @param read
		 *            The number of bytes that were read.
		 */
		public void indexPositions(int read) {
			int bufBottomOffset = bufPosOffset
					- (buffer.length + bufBottom - bufPos) % buffer.length;
			int bufTopOffset = bufPosOffset + (buffer.length + bufTop - bufPos)
					% buffer.length;
			for (int i = 1 - read - KEY_LENGTH; i < 1 - KEY_LENGTH; ++i) {
				int curPos = (bufTop + buffer.length + i) % buffer.length;
				int curPosOffset = bufTopOffset + i;
				if (curPosOffset >= bufBottomOffset) {
					indexPosition(curPos, curPosOffset);
				}
			}
		}

		/**
		 * Returns the first Position object for the bytes at the current read
		 * position.
		 */
		public Position getFirstMatchingPosition() {
			return positions[calculateHash(bufPos)];
		}

		/**
		 * Indexes the bytes at a given position.
		 * 
		 * @param pos
		 *            The position to index.
		 * @param posOffset
		 *            The file offset this position represents.
		 */
		private void indexPosition(int pos, int posOffset) {
			int hash = calculateHash(pos);
			positions[hash] = new Position(posOffset, positions[hash]);
		}

		/**
		 * Calculates a bucket number for the bytes at a given position.
		 * 
		 * @param pos
		 *            The position to calculate the hash for.
		 * @return The bucket number for the bytes.
		 */
		private int calculateHash(int pos) {
			int hash = 0;
			for (int i = 0; i < KEY_LENGTH; ++i) {
				hash *= 257;
				hash += buffer[(pos + i) % buffer.length] & 0xff;
				hash %= BUCKETS;
			}
			return hash;
		}

	}

	/**
	 * A position in the input file name where a given byte sequence may appear.
	 */
	private class Position {
		private final int offset;
		private final Position next;

		private Position(int offset, Position next) {
			this.offset = offset;
			this.next = next;
		}

		/**
		 * Returns the match distance.
		 */
		public int getDistance() {
			return bufPosOffset - offset;
		}

		/**
		 * Returns the next position in the linked list.
		 */
		public Position getNext() {
			return next;
		}
	}
}
