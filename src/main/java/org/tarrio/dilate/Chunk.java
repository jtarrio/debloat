package org.tarrio.dilate;

import java.util.Arrays;

/**
 * A representation of compressed data. It can represent new data or data from
 * the past. It contains a match distance and a length, along with the bytes it
 * represents.
 * 
 * The match distance is 0 if the chunk represents new data. A value higher than
 * 0 represents data in the past: 1 would be the character before the current
 * position, 2 the character before that, etc.
 * 
 * @author Jacobo
 */
public class Chunk {

	public static final int MAX_LENGTH = 32767;

	private int distance;
	private int length;
	private byte[] bytes;

	/**
	 * Creates a chunk with the given distance, length and bytes.
	 */
	Chunk(int distance, byte[] bytes) {
		this.distance = distance;
		this.bytes = bytes;
	}

	/**
	 * Returns the match distance for this chunk.
	 */
	public int getDistance() {
		return distance;
	}

	/**
	 * Returns the length of this chunk.
	 */
	public int getLength() {
		return bytes.length;
	}

	/**
	 * Returns the byte representation of this chunk.
	 */
	public byte[] getBytes() {
		return bytes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
		result = prime * result + distance;
		result = prime * result + length;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Chunk other = (Chunk) obj;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		if (distance != other.distance)
			return false;
		if (length != other.length)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Chunk [distance=" + distance + ", length=" + length
				+ ", bytes=" + Arrays.toString(bytes) + "]";
	}

}
