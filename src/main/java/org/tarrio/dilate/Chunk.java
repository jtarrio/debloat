package org.tarrio.dilate;

import java.util.Arrays;

public class Chunk {

	public static final int MAX_LENGTH = 32767;

	private int distance;
	private int length;
	private byte[] bytes;

	public Chunk(int distance, int length, byte[] bytes) {
		this.distance = distance;
		this.length = length;
		this.bytes = bytes;
	}

	public int getDistance() {
		return distance;
	}

	public int getLength() {
		return length;
	}

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
