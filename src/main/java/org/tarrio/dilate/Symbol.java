package org.tarrio.dilate;

/**
 * A class that represents a symbol consisting of either a single byte or a
 * reference to past data.
 * 
 * @author Jacobo Tarrio
 */
public class Symbol {

	private final byte byteValue;
	private final int distance;
	private final int length;

	/**
	 * Constructs a byte symbol.
	 * 
	 * @param byteValue
	 *            The value of the byte for this symbol.
	 */
	public Symbol(byte byteValue) {
		this.byteValue = byteValue;
		this.distance = 0;
		this.length = 0;
	}

	/**
	 * Constructs a reference symbol.
	 * 
	 * @param distance
	 *            The match distance.
	 * @param length
	 *            The match length.
	 */
	public Symbol(int distance, int length) {
		this.byteValue = 0;
		this.distance = distance;
		this.length = length;
	}

	/**
	 * @return The value of the byte for this symbol.
	 */
	public byte getByte() {
		return byteValue;
	}

	/**
	 * @return The match distance for this symbol.
	 */
	public int getDistance() {
		return distance;
	}

	/**
	 * @return The match length for this symbol.
	 */
	public int getLength() {
		return length;
	}

	/**
	 * @return Whether this symbol is a reference symbol.
	 */
	public boolean isReference() {
		return distance >= 1 && length > 1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + distance;
		result = prime * result + length;
		result = prime * result + byteValue;
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
		if (byteValue != other.byteValue)
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (isReference()) {
			return "Symbol [distance=" + distance + ", length=" + length + "]";
		} else {
			return "Symbol [byteValue=" + byteValue + "]";
		}
	}

}
