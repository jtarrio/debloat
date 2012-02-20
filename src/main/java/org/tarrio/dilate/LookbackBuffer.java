package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LookbackBuffer {

	private static final int DEFAULT_BUFFER_SIZE = 32768 * 3;

	private final InputStream input;
	private final byte[] buffer;
	private int start;
	private int position;
	private int end;
	private final int lookbackSize;
	private boolean eof;
	private final Map<Integer, List<Integer>> positionMap;

	public LookbackBuffer(InputStream input) {
		this(input, DEFAULT_BUFFER_SIZE);
	}

	public LookbackBuffer(InputStream input, int bufferSize) {
		this(input, bufferSize, bufferSize / 3);
	}

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

	public boolean isEof() throws IOException {
		fillBuffer();
		return this.eof && position >= end;
	}

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

	public void skip(int count) throws IOException {
		for (int i = 0; i < count; ++i) {
			read();
		}
	}

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

	private int makeHash(int offset) {
		return (buffer[position + offset] * 256 + buffer[position + offset + 1]) * 256 + buffer[position + offset + 2];
	}

	private List<Integer> findPositions(int hash) {
		List<Integer> positions = positionMap.get(hash);
		if (positions == null) {
			positions = new LinkedList<Integer>();
			positionMap.put(hash, positions);
		} else {
			for (int i = 0; i < positions.size();) {
				if (positions.get(i) < this.position + this.start - this.lookbackSize) {
					positions.remove(i);
				} else {
					++i;
				}
			}
		}
		return positions;
	}
	
	private Match checkMatch(int pos) {
		int len = 0;
		for (len = 0; len + position < end; ++len) {
			if (buffer[pos - start + len] != buffer[position + len]) {
				break;
			}
		}
		return new Match(position + start - pos, len);
	}

	private void fillBuffer() throws IOException {
		if (!eof) {
			if (position > lookbackSize * 2) {
				int moveOffset = position - lookbackSize;
				System.arraycopy(buffer, moveOffset, buffer, 0, end - moveOffset);
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

	public class Match {
		private int distance;
		private int length;

		public Match(int distance, int length) {
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

}
