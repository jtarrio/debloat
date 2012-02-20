package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A buffer that allows to search for already-read data that matches current
 * data.
 * 
 * It keeps lookahead and lookback areas, each one 1/3rd of the total buffer
 * size. It is possible to check whether we have reached the end of the input
 * stream, read one byte, skip any number of bytes, or check if the incoming
 * data matches data in the lookback area.
 * 
 * @author Jacobo
 */
class LookbackBuffer {

	private static final int DEFAULT_BUFFER_SIZE = 32768 * 3;

	private final InputStream input;
	private final byte[] buffer;
	private int start;
	private int position;
	private int end;
	private final int lookbackSize;
	private boolean eof;
	private final Map<Integer, List<Integer>> positionMap;

	/**
	 * Creates a look-back buffer for the given input stream with the default
	 * size.
	 * 
	 * @param input
	 *            The stream to read data from.
	 */
	public LookbackBuffer(InputStream input) {
		this(input, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a look-back buffer with the given size for the given input
	 * stream. The minimum look-back and look-ahead sizes will each be one third
	 * of the buffer size.
	 * 
	 * @param input
	 *            The stream to read data from.
	 * @param bufferSize
	 *            The total size for the buffer.
	 */
	public LookbackBuffer(InputStream input, int bufferSize) {
		this(input, bufferSize, bufferSize / 3);
	}

	/**
	 * Creates a look-back buffer with the given total for the given input
	 * stream. The look-back size is also given, which is also the look-ahead
	 * size.
	 * 
	 * @param input
	 *            The stream to read data from.
	 * @param bufferSize
	 *            The total size for the buffer.
	 * @param lookbackSize
	 *            The look-back and look-ahead size.
	 */

	public LookbackBuffer(InputStream input, int bufferSize, int lookbackSize) {
		assert lookbackSize > 0 && bufferSize > lookbackSize * 2;
		this.input = input;
		this.buffer = new byte[bufferSize];
		this.start = 0;
		this.position = 0;
		this.end = 0;
		this.lookbackSize = lookbackSize;
		this.eof = false;
		this.positionMap = new HashMap<Integer, List<Integer>>();
	}

	/**
	 * Checks whether we have reached the end of both the input stream and the
	 * buffer.
	 * 
	 * @return Whether we have reached the end of both the input stream and the
	 *         buffer.
	 * @throws IOException
	 *             If there was any problem reading from the input stream.
	 */
	public boolean isEof() throws IOException {
		fillBuffer();
		return this.eof && position >= end;
	}

	/**
	 * Reads one byte.
	 * 
	 * @return The byte read, or -1 if we have reached the end of the buffer
	 *         &mdash; use isEof() to verify this condition.
	 * @throws IOException
	 *             If there was any problem reading from the input stream.
	 */
	public byte read() throws IOException {
		if (isEof()) {
			return -1;
		}
		if (position >= 2) {
			int hash = makeHash(-2);
			findPositions(hash).add(0, start + position - 2);
		}
		return buffer[position++];
	}

	/**
	 * Checks whether the data at the current position matches data at other
	 * position in the lookback buffer. The repeated data will have at least
	 * three bytes, and may also appear in the future data.
	 * 
	 * @return A {@link Match} object representing a position where the current
	 *         data appears in the lookback buffer, or null if no such position
	 *         was found.
	 * @throws IOException
	 *             If there was a problem reading from the input stream.
	 */
	public Match findMatch() throws IOException {
		fillBuffer();
		if (position > end - 3) {
			return null;
		}
		int hash = makeHash(0);
		List<Integer> positions = findPositions(hash);
		Match bestMatch = null;
		for (int pos : positions) {
			Match match = checkMatch(pos);
			if (match.getDistance() > 32767) {
				System.out.println("foo");
			}
			if (bestMatch == null || match.getLength() > bestMatch.getLength()) {
				bestMatch = match;
			}
		}
		return bestMatch;
	}

	/**
	 * Skips several bytes. Provided for unit tests.
	 * 
	 * @param count
	 *            Number of bytes to skip.
	 * @throws IOException
	 *             If there was any problem reading from the input stream.
	 */
	void skip(int count) throws IOException {
		for (int i = 0; i < count; ++i) {
			read();
		}
	}

	/**
	 * Calculates a quick hash from three bytes at the given offset from the
	 * current position.
	 * 
	 * @param offset
	 *            The offset to read the bytes from.
	 * @return The hash representation from the three bytes.
	 */
	private int makeHash(int offset) {
		return (buffer[position + offset] * 256 + buffer[position + offset + 1])
				* 256 + buffer[position + offset + 2];
	}

	/**
	 * Finds all the positions where three bytes with the given hash appear.
	 * 
	 * @param hash
	 *            The hash to look for.
	 * @return A list of absolute positions where the hash appears. If it
	 *         appears nowhere, an empty list is returned.
	 */
	private List<Integer> findPositions(int hash) {
		List<Integer> positions = positionMap.get(hash);
		if (positions == null) {
			positions = new LinkedList<Integer>();
			positionMap.put(hash, positions);
		} else {
			for (int i = 0; i < positions.size();) {
				if (positions.get(i) < this.position + this.start
						- this.lookbackSize) {
					positions.remove(i);
				} else {
					++i;
				}
			}
		}
		return positions;
	}

	/**
	 * Compares the bytes at the given position with the bytes at the current
	 * position and returns the match distance and the match length.
	 * 
	 * @param pos
	 *            The absolute position where the match is.
	 * @return A {@link Match} object representing the match distance and
	 *         length.
	 */
	private Match checkMatch(int pos) {
		int len = 0;
		for (len = 0; len + position < end; ++len) {
			if (buffer[pos - start + len] != buffer[position + len]) {
				break;
			}
		}
		return new Match(position + start - pos, len);
	}

	/**
	 * Makes sure the buffer is full, and moves its contents around if the
	 * current position goes too far into the lookahead area.
	 * 
	 * @throws IOException
	 *             If there was any problem reading from the input stream.
	 */
	private void fillBuffer() throws IOException {
		if (!eof) {
			if (end > 0 && position > lookbackSize
					&& position >= end - lookbackSize) {
				int moveOffset = position - lookbackSize;
				System.arraycopy(buffer, moveOffset, buffer, 0, end
						- moveOffset);
				start += moveOffset;
				position -= moveOffset;
				end -= moveOffset;
			}
			int read = input.read(buffer, end, buffer.length - end);
			if (read == -1) {
				eof = true;
			} else {
				end += read;
			}
		}
	}

	/**
	 * Representation of a match distance and length.
	 */
	public class Match {
		private int distance;
		private int length;

		private Match(int distance, int length) {
			super();
			this.distance = distance;
			this.length = length;
		}

		/**
		 * Returns the match distance.
		 */
		public int getDistance() {
			return distance;
		}

		/**
		 * Returns the match length.
		 */
		public int getLength() {
			return length;
		}

	}

}
