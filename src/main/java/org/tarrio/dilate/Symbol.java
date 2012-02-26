package org.tarrio.dilate;

public class Symbol {

	private byte value;
	private int distance;
	private int length;

	public Symbol(byte value, int distance, int length) {
		super();
		this.value = value;
		this.distance = distance;
		this.length = length;
	}

	public byte getValue() {
		return value;
	}

	public int getDistance() {
		return distance;
	}

	public int getLength() {
		return length;
	}

	public boolean isReference() {
		return distance >= 1 && length > 1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + distance;
		result = prime * result + length;
		result = prime * result + value;
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
		Symbol other = (Symbol) obj;
		if (distance != other.distance)
			return false;
		if (length != other.length)
			return false;
		if (value != other.value)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Symbol [value=" + value + ", distance=" + distance
				+ ", length=" + length + "]";
	}
}
