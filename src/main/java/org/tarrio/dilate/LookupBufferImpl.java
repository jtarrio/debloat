package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A buffer that keeps the last and next bytes processed (read/written) to be
 * able to find and reconstruct duplicates.
 * 
 * @author Jacobo Tarrio
 */
public class LookupBufferImpl implements LookupBuffer {

	private static final int DEFAULT_MAX_DISTANCE = 32768;
	private static final int DEFAULT_MAX_LENGTH = 258;

	private final InputStream inputStream;
	private final OutputStream outputStream;
	private final int maxDistance;
	private final int maxLength;
	private byte[] buffer;
	private int position;
	private int start;
	private int end;
	private boolean eofReached;
	private final PositionMap positionMap;
	private int endOfMapped;

	/**
	 * Constructs a {@link LookupBufferImpl} that reads from the given stream and
	 * can find duplicates of up to 32kB distance and 258 bytes length.
	 * 
	 * @param inputStream
	 *            The stream to read from.
	 */
	public LookupBufferImpl(InputStream inputStream) {
		this(inputStream, DEFAULT_MAX_DISTANCE * 3, DEFAULT_MAX_DISTANCE,
				DEFAULT_MAX_LENGTH);
	}

	/**
	 * Constructs a {@link LookupBufferImpl} that reads from the given stream and
	 * has the given capacity, maximum distance, and maximum length.
	 * 
	 * Used for unit tests only.
	 * 
	 * @param inputStream
	 *            The stream to read from.
	 * @param bufferLength
	 *            The buffer's total size.
	 * @param maxDistance
	 *            The maximum distance for a found duplicate.
	 * @param maxLength
	 *            The maximum length of a found duplicate.
	 */
	LookupBufferImpl(InputStream inputStream, int bufferLength, int maxDistance,
			int maxLength) {
		this.inputStream = inputStream;
		this.outputStream = null;
		this.maxDistance = maxDistance;
		this.maxLength = maxLength;
		this.buffer = new byte[bufferLength];
		this.position = 0;
		this.start = 0;
		this.end = 0;
		this.eofReached = false;
		this.positionMap = new PositionMap();
		this.endOfMapped = 0;
	}

	/**
	 * Constructs a {@link LookupBufferImpl} that writes to the given stream and can
	 * find duplicates of up to 32kB distance and 258 bytes length.
	 * 
	 * @param outputStream
	 *            The stream to write to.
	 */
	public LookupBufferImpl(OutputStream outputStream) {
		this(outputStream, DEFAULT_MAX_DISTANCE * 2 + DEFAULT_MAX_LENGTH,
				DEFAULT_MAX_DISTANCE, DEFAULT_MAX_LENGTH);
	}

	/**
	 * Constructs a {@link LookupBufferImpl} that writes to the given stream and has
	 * the given capacity, maximum distance, and maximum length.
	 * 
	 * Used for unit tests only.
	 * 
	 * @param inputStream
	 *            The stream to read from.
	 * @param bufferLength
	 *            The buffer's total size.
	 * @param maxDistance
	 *            The maximum distance for a found duplicate.
	 * @param maxLength
	 *            The maximum length of a found duplicate.
	 */
	LookupBufferImpl(OutputStream outputStream, int bufferLength, int maxDistance,
			int maxLength) {
		this.outputStream = outputStream;
		this.inputStream = null;
		this.maxDistance = maxDistance;
		this.maxLength = maxLength;
		this.buffer = new byte[bufferLength];
		this.position = 0;
		this.positionMap = null;
	}

	@Override
	public int read(byte[] destBuffer, int length) throws IOException {
		feedMoreBytes(length);
		if (position == end) {
			return -1;
		} else {
			int copyLength = Math.min(length, end - position);
			if (destBuffer != null) {
				System.arraycopy(buffer, position, destBuffer, 0, copyLength);
			}
			position += copyLength;
			return copyLength;
		}
	}

	@Override
	public int skip(int length) throws IOException {
		return read(null, length);
	}

	@Override
	public Match findPastMatch() throws IOException {
		feedMoreBytes(maxLength);
		Match bestMatch = null;
		PositionMap.Position candidate = positionMap.findMatchingPositions();
		while (candidate != null) {
			int i = candidate.getValue() - start;
			if (i < position) {
				int j;
				for (j = 0; j < maxLength && i + j < end && position + j < end; ++j) {
					if (buffer[i + j] != buffer[position + j]) {
						break;
					}
				}
				if (j >= 3 && (bestMatch == null || bestMatch.getLength() < j)) {
					bestMatch = new MatchImpl(position - i, j);
				}
			}
			candidate = candidate.getNext();
		}
		return bestMatch;
	}

	@Override
	public void write(byte b) throws IOException {
		makeRoom(1);
		buffer[position++] = b;
		outputStream.write(b);
	}

	@Override
	public void write(byte[] data) throws IOException {
		write(data, 0, data.length);
	}

	@Override
	public void write(byte[] data, int offset, int length) throws IOException {
		makeRoom(length);
		System.arraycopy(data, offset, buffer, position, length);
		position += length;
		outputStream.write(data, offset, length);
	}

	@Override
	public void repeatPastMatch(int distance, int length) throws IOException {
		makeRoom(length);
		for (int i = 0; i < length; ++i) {
			write(buffer[position - distance]);
		}
	}

	/**
	 * A class to contain the results of a search for a past matching substring,
	 * as returned by findPastMatch.
	 */
	public static class MatchImpl implements Match {
		private int distance;
		private int length;

		private MatchImpl(int distance, int length) {
			super();
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
	 * Reads more data from the input stream into the buffer, making sure to
	 * leave enough space to find duplicates as far backwards as necessary and
	 * of the required maximum length, so that we can then read data from the
	 * buffer.
	 * 
	 * @param length
	 *            The number of bytes we are going to read from the buffer after
	 *            filling it.
	 * @throws IOException
	 *             If there was a problem writing to the stream.
	 */
	private void feedMoreBytes(int length) throws IOException {
		if (inputStream == null) {
			throw new IllegalStateException("Cannot read from a write buffer");
		}

		if (eofReached) {
			return;
		}

		if (position > maxDistance && position > end - length
				&& position > end - maxLength) {
			int removeBytes = position - maxDistance;
			System.arraycopy(buffer, removeBytes, buffer, 0, end - removeBytes);
			position -= removeBytes;
			start += removeBytes;
			end -= removeBytes;
			endOfMapped -= removeBytes;
		}
		if (end < buffer.length) {
			int readBytes = inputStream.read(buffer, end, buffer.length - end);
			if (readBytes == -1) {
				eofReached = true;
			} else {
				end += readBytes;
				positionMap.updatePositions();
			}
		}
	}

	/**
	 * Makes room to write more data into the buffer.
	 * 
	 * @param length
	 *            The number of bytes we are going to write into the buffer.
	 */
	private void makeRoom(int length) {
		if (outputStream == null) {
			throw new IllegalStateException("Cannot write to a read buffer");
		}

		if (position > maxDistance
				&& (position > buffer.length - maxLength || position > buffer.length
						- length)) {
			int removeBytes = position - maxDistance;
			System.arraycopy(buffer, removeBytes, buffer, 0, position
					- removeBytes);
			position -= removeBytes;
		}
	}

	/**
	 * A hash map that gives, for each 3-byte sequence, the position where it
	 * occurs in the buffer.
	 * 
	 * It is used internally to speed up the duplicate search.
	 */
	private class PositionMap {
		private static final int KEY_LENGTH = 3;
		private static final int BUCKETS = 32771;

		private Position[] positions = new Position[BUCKETS];

		/**
		 * Finds the positions where the 3 bytes at the current position appear.
		 * 
		 * Too old positions are removed, but too new positions aren't.
		 */
		public Position findMatchingPositions() {
			int hash = calculateHash(position);
			cleanBucket(hash);
			return positions[hash];
		}

		/**
		 * Adds newly read data to the map.
		 */
		public void updatePositions() {
			int endCount = end - KEY_LENGTH;
			for (int i = Math.max(endOfMapped, 0); i < endCount; ++i) {
				int hash = calculateHash(i);
				positions[hash] = new Position(i + start, positions[hash]);
			}
			endOfMapped = endCount;
		}

		/**
		 * Calculates a hash for 3 consecutive bytes.
		 * 
		 * @param pos
		 *            The position in the buffer to calculate the hash for.
		 * @return The calculated hash.
		 */
		private int calculateHash(int pos) {
			int hash = 0;
			for (int i = pos; i < pos + KEY_LENGTH; ++i) {
				hash *= 257;
				hash += (buffer[i] & 0xff);
				hash %= BUCKETS;
			}
			return hash;
		}

		/**
		 * Removes too old positions from a hash bucket.
		 * 
		 * @param hash
		 *            The number of the bucket to clean.
		 */
		private void cleanBucket(int hash) {
			Position current = skipOld(positions[hash]);
			positions[hash] = current;
			while (current != null) {
				Position next = skipOld(current.getNext());
				current.setNext(next);
				current = next;
			}
		}

		/**
		 * Skips old positions in the position list.
		 */
		private Position skipOld(Position current) {
			while (current != null
					&& current.getValue() < position + start - maxDistance) {
				current = current.getNext();
			}
			return current;
		}

		/**
		 * A node in a linked list of positions.
		 */
		private class Position {
			private int value;
			private Position next;

			private Position(int value, Position next) {
				super();
				this.value = value;
				this.next = next;
			}

			public int getValue() {
				return value;
			}

			public Position getNext() {
				return next;
			}

			public void setNext(Position next) {
				this.next = next;
			}
		}
	}

}
