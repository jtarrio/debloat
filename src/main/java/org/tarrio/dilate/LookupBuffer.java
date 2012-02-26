package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LookupBuffer {

	public static class Match {
		private int distance;
		private int length;

		private Match(int distance, int length) {
			super();
			this.distance = distance;
			this.length = length;
		}

		public int getDistance() {
			return distance;
		}

		public int getLength() {
			return length;
		}
	}

	private final InputStream inputStream;
	private final OutputStream outputStream;
	private final int lookbackLength;
	private final int lookaheadLength;
	private byte[] buffer;
	private int position;
	private int start;
	private int end;
	private boolean eofReached;
	private final PositionMap positionMap;

	public LookupBuffer(InputStream inputStream) {
		this(inputStream, 32768 * 3, 32768, 32768);
	}

	public LookupBuffer(InputStream inputStream, int bufferLength,
			int lookbackLength, int lookaheadLength) {
		this.inputStream = inputStream;
		this.outputStream = null;
		this.positionMap = new PositionMap();
		this.lookbackLength = lookbackLength;
		this.lookaheadLength = lookaheadLength;
		this.buffer = new byte[bufferLength];
		this.position = 0;
		this.start = 0;
		this.end = 0;
		this.eofReached = false;
	}

	public LookupBuffer(OutputStream outputStream) {
		this(outputStream, 32768 * 2 + 258, 32768, 258);
	}

	public LookupBuffer(OutputStream outputStream, int bufferLength,
			int lookbackLength, int lookaheadLength) {
		this.outputStream = outputStream;
		this.inputStream = null;
		this.positionMap = null;
		this.lookbackLength = lookbackLength;
		this.lookaheadLength = lookaheadLength;
		this.buffer = new byte[bufferLength];
		this.position = 0;
	}

	public byte read() throws IOException {
		feedMoreBytes();
		if (position == end) {
			return -1;
		} else {
			return buffer[position++];
		}
	}

	public int read(byte[] destBuffer, int length) throws IOException {
		feedMoreBytes();
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

	public int skip(int length) throws IOException {
		return read(null, length);
	}

	public Match findPastMatch() throws IOException {
		feedMoreBytes();
		Match bestMatch = null;
		PositionMap.Position candidate = positionMap.findMatchingPositions();
		while (candidate != null) {
			int i = candidate.getValue() - start;
			int j;
			for (j = 0; i + j < end && position + j < end; ++j) {
				if (buffer[i + j] != buffer[position + j]) {
					break;
				}
			}
			if (j >= 3 && (bestMatch == null || bestMatch.getLength() < j)) {
				bestMatch = new Match(position - i, j);
			}
			candidate = candidate.getNext();
		}
		return bestMatch;
	}

	public void write(byte b) throws IOException {
		makeRoom(1);
		buffer[position++] = b;
		outputStream.write(b);
	}

	public void write(byte[] data) throws IOException {
		write(data, 0, data.length);
	}

	public void write(byte[] data, int offset, int length) throws IOException {
		makeRoom(length);
		System.arraycopy(data, offset, buffer, position, length);
		position += length;
		outputStream.write(data, offset, length);
	}

	public void repeatPastMatch(int distance, int length) throws IOException {
		makeRoom(length);
		for (int i = 0; i < length; ++i) {
			write(buffer[position - distance]);
		}
	}

	private void feedMoreBytes() throws IOException {
		if (inputStream == null) {
			throw new IllegalStateException("Cannot read from a write buffer");
		}

		if (eofReached) {
			return;
		}

		if (position > lookbackLength && position > end - lookaheadLength) {
			int removeBytes = position - lookbackLength;
			System.arraycopy(buffer, removeBytes, buffer, 0, end - removeBytes);
			position -= removeBytes;
			start += removeBytes;
			end -= removeBytes;
		}
		if (end < buffer.length && !eofReached) {
			int readBytes = inputStream.read(buffer, end, buffer.length - end);
			if (readBytes == -1) {
				eofReached = true;
			} else {
				end += readBytes;
				positionMap.updatePositions(readBytes);
			}
		}
	}

	private void makeRoom(int length) {
		if (outputStream == null) {
			throw new IllegalStateException("Cannot write to a read buffer");
		}

		if (position > lookbackLength
				&& (position > buffer.length - lookaheadLength || position > buffer.length
						- length)) {
			int removeBytes = position - lookbackLength;
			System.arraycopy(buffer, removeBytes, buffer, 0, position
					- removeBytes);
			position -= removeBytes;
		}
	}

	private class PositionMap {

		private static final int BUCKETS = 32771;
		private Position[] positions = new Position[BUCKETS];

		public Position findMatchingPositions() {
			return skipToPresent(positions[calculateHash(position)]);
		}

		private Position skipToPresent(Position current) {
			while (current != null && current.getValue() >= start + position) {
				current = current.getNext();
			}
			return current;
		}

		public void updatePositions(int readBytes) {
			int startCount = Math.max(end - readBytes - 2, 0);
			int endCount = end - 3;
			for (int i = startCount; i < endCount; ++i) {
				int hash = calculateHash(i);
				positions[hash] = new Position(i + start, positions[hash]);
			}
		}

		private int calculateHash(int pos) {
			int hash = 0;
			for (int i = pos; i < pos + 3; ++i) {
				hash *= 257;
				hash += buffer[i];
				hash %= BUCKETS;
			}
			return hash;
		}

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
				if (next != null && next.getValue() < start) {
					next = null;
				}
				return skipToPresent(next);
			}
		}
	}

}
