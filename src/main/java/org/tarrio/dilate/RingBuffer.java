package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RingBuffer implements LookupBuffer {

	private static final int DEFAULT_MAX_DISTANCE = 32768;
	private static final int DEFAULT_MAX_LENGTH = 258;

	private final InputStream inputStream;
	private final OutputStream outputStream;
	private final int maxDistance;
	private final int maxLength;
	private final byte[] buffer;
	private int bufBottom;
	private int bufPos;
	private int bufTop;
	private boolean eof;

	public RingBuffer(InputStream inputStream) {
		this(inputStream, DEFAULT_MAX_DISTANCE, DEFAULT_MAX_LENGTH);
	}

	public RingBuffer(InputStream inputStream, int maxDistance, int maxLength) {
		this.inputStream = inputStream;
		this.outputStream = null;
		this.maxDistance = maxDistance;
		this.maxLength = maxLength;
		this.buffer = new byte[maxDistance + maxLength + 1];
		this.bufBottom = 0;
		this.bufPos = 0;
		this.bufTop = 0;
		this.eof = false;
	}

	public RingBuffer(OutputStream outputStream) {
		this(outputStream, DEFAULT_MAX_DISTANCE, DEFAULT_MAX_LENGTH);
	}
	
	public RingBuffer(OutputStream outputStream, int maxDistance, int maxLength) {
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
		return maxRead;
	}

	private void fillBuffer() throws IOException {
		if (inputStream == null) {
			throw new IllegalStateException("Cannot read from write buffer");
		}
		if (eof) {
			return;
		}
		int usedBelow = bufPos + (bufPos >= bufBottom ? 0 : buffer.length)
				- bufBottom;
		int usedAbove = bufTop + (bufTop >= bufPos ? 0 : buffer.length)
				- bufPos;
		if (usedBelow > maxDistance) {
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
		return read;
	}

	@Override
	public int skip(int length) throws IOException {
		return read(null, length);
	}

	@Override
	public Match findPastMatch() throws IOException {
		fillBuffer();
		Match bestMatch = null;
		for (int i = bufBottom; i % buffer.length != bufPos; ++i) {
			int j = checkMatch(i);
			if (j >= 3 && (bestMatch == null || bestMatch.getLength() <= j)) {
				bestMatch = new MatchImpl((buffer.length + bufPos - i)
						% buffer.length, j);
			}
		}
		return bestMatch;
	}

	private int checkMatch(int i) {
		int j;
		for (j = 0; j < maxLength && (bufPos + j) % buffer.length != bufTop; ++j) {
			if (buffer[(i + j) % buffer.length] != buffer[(bufPos + j)
					% buffer.length]) {
				break;
			}
		}
		return j;
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
			writtenSoFar += partialWrite(data, offset + writtenSoFar, length - writtenSoFar);
		}
	}

	private int partialWrite(byte[] data, int offset, int length) throws IOException {
		int maxWrite = Math.min(length, (bufBottom > bufPos ? bufBottom : buffer.length) - bufPos);
		System.arraycopy(data, offset, buffer, bufPos, maxWrite);
		outputStream.write(data, offset, maxWrite);
		bufPos += maxWrite;
		bufPos %= buffer.length;
		return maxWrite;
	}

	private void makeRoom(int length) {
		if (outputStream == null) {
			throw new IllegalStateException("Cannot write to read buffer");
		}
		int usedBelow = bufPos + (bufPos >= bufBottom ? 0 : buffer.length)
				- bufBottom;
		if (usedBelow > maxDistance) {
			int discard = usedBelow - maxDistance;
			bufBottom += discard;
			bufBottom %= buffer.length;
			usedBelow -= discard;
		}
	}

	@Override
	public void repeatPastMatch(int distance, int length) throws IOException {
		if (distance < 1 || distance > maxDistance) {
			throw new IndexOutOfBoundsException("Repeat distance is not valid: " + distance);
		}
		if (length < 3 || length > maxLength) {
			throw new IndexOutOfBoundsException("Repeat length is not valid: " + length);
		}
		int past = (bufPos + buffer.length - distance) % buffer.length;
		for (int i = 0; i < length; ++i) {
			write(buffer[past]);
			++past;
			past %= buffer.length;
		}
	}

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

}
