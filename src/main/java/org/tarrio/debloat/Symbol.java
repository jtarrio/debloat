package org.tarrio.debloat;

/**
 * A class that represents a symbol. There are several types of symbols, such as
 * single bytes, references to past data, dictionary entry references, or
 * "reset buffer" symbols.
 * 
 * @author Jacobo Tarrio
 */
public abstract class Symbol {

	private static final Reset theResetSymbol = new Reset();

	/**
	 * Factory method to get a symbol that represents a byte.
	 * 
	 * @param byteValue
	 *            The value of the byte this symbol represents.
	 * @return A symbol of byte type.
	 */
	public static Symbol newByte(byte byteValue) {
		return new Byte(byteValue);
	}

	/**
	 * Factory method to get a symbol that represents a backreference.
	 * 
	 * @param distance
	 *            The match distance for the symbol.
	 * @param length
	 *            The match length.
	 * @return A symbol of backreference type.
	 */
	public static Symbol newBackRef(int distance, int length) {
		return new BackRef(distance, length);
	}

	/**
	 * Factory method to get a symbol that represents a reference to a
	 * dictionary entry.
	 * 
	 * @param entry
	 *            The number of the entry the symbol represents.
	 * @return A symbol of dictionary type.
	 */
	public static Symbol newDictionaryRef(int entry) {
		return new DictionaryRef(entry);
	}

	/**
	 * Factory method to get a symbol that represents a buffer reset.
	 * 
	 * @return A symbol of reset type.
	 */
	public static Symbol newReset() {
		return theResetSymbol;
	}

	/**
	 * A class for symbols that represent single bytes.
	 */
	public static class Byte extends Symbol {

		private final byte byteValue;

		private Byte(byte byteValue) {
			this.byteValue = byteValue;
		}

		/**
		 * Returns the value of the byte this symbol represents.
		 */
		public byte getByteValue() {
			return byteValue;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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
			Byte other = (Byte) obj;
			if (byteValue != other.byteValue)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Byte [byteValue=" + byteValue + "]";
		}
	}

	/**
	 * A class for symbols that represent back references.
	 */
	public static class BackRef extends Symbol {

		private final int distance;
		private final int length;

		private BackRef(int distance, int length) {
			this.distance = distance;
			this.length = length;
		}

		/**
		 * The distance of the backreference this symbol represents.
		 */
		public int getDistance() {
			return distance;
		}

		/**
		 * The length of the backreference this symbol represents.
		 */
		public int getLength() {
			return length;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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
			BackRef other = (BackRef) obj;
			if (distance != other.distance)
				return false;
			if (length != other.length)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "BackRef [distance=" + distance + ", length=" + length + "]";
		}
	}

	/**
	 * A class for symbols that represent dictionary references.
	 */
	public static class DictionaryRef extends Symbol {

		private final int entry;

		private DictionaryRef(int entry) {
			this.entry = entry;
		}

		/**
		 * Returns the number of the entry this symbol represents.
		 */
		public int getEntry() {
			return entry;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + entry;
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
			DictionaryRef other = (DictionaryRef) obj;
			if (entry != other.entry)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "DictionaryRef [entry=" + entry + "]";
		}
	}

	/**
	 * A class for a symbol that represents a buffer reset.
	 */
	public static class Reset extends Symbol {

		private Reset() {
		}

		@Override
		public String toString() {
			return "Reset []";
		}
	}
}
