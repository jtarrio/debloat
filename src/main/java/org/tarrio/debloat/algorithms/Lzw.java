package org.tarrio.debloat.algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tarrio.debloat.Symbol;
import org.tarrio.debloat.Codec.Decoder;
import org.tarrio.debloat.Codec.Encoder;

/**
 * Implementation of the Lempel-Ziv-Welch compression algorithm.
 * 
 * @author Jacobo Tarrio
 */
public class Lzw extends AbstractCompressionAlgorithmImpl {

	static final int MAX_ENTRIES = 4096;

	private int maxEntries;

	/**
	 * Creates a LZW compressor with a dictionary of size 4096.
	 */
	public Lzw() {
		this(MAX_ENTRIES);
	}

	/**
	 * Creates a LZW compressor with a dictionary of the given size.
	 * 
	 * Visible for testing.
	 * 
	 * @param maxEntries
	 *            The maximum number of entries in the dictionary.
	 */
	Lzw(int maxEntries) {
		this.maxEntries = maxEntries;
	}

	@Override
	protected String getAlgorithmName() {
		return "lzw";
	}

	@Override
	public void doCompress(InputStream input, Encoder outputEncoder)
			throws IOException {
		byte[] buffer = new byte[maxEntries];
		int bufTop = 0;
		Dictionary dict = new Dictionary(maxEntries);
		int prevEntry = -1;
		int read = 1;
		while (read > 0) {
			if (dict.getSize() == maxEntries) {
				dict.reset();
				outputEncoder.write(Symbol.newReset());
			}
			read = input.read(buffer, bufTop, 1);
			if (read > 0) {
				++bufTop;
			}
			int curEntry = dict.getEntryNum(buffer, bufTop);
			if (prevEntry != -1 && curEntry == -1) {
				outputEncoder.write(Symbol.newDictionaryRef(prevEntry));
				dict.addEntry(prevEntry, buffer, bufTop);
				buffer[0] = buffer[bufTop - 1];
				bufTop = 1;
				prevEntry = buffer[0] & 0xff;
			} else {
				prevEntry = curEntry;
			}
		}
		if (prevEntry != -1) {
			outputEncoder.write(Symbol.newDictionaryRef(prevEntry));
		}
	}

	@Override
	public void doDecompress(Decoder inputDecoder, OutputStream output)
			throws IOException {
		byte[] buffer = new byte[maxEntries - 256];
		int bufTop = 0;
		Dictionary dict = new Dictionary(maxEntries);
		Integer prevEntry = null;
		Symbol symbol = null;
		do {
			if (bufTop > 0) {
				output.write(buffer, 0, bufTop);
			}
			symbol = inputDecoder.read();
			if (symbol instanceof Symbol.Reset) {
				dict.reset();
				prevEntry = null;
				bufTop = 0;
			} else if (symbol instanceof Symbol.DictionaryRef) {
				Symbol.DictionaryRef ref = (Symbol.DictionaryRef) symbol;
				int entryNum = ref.getEntry();
				int dictSize = dict.getSize();
				if (entryNum == dictSize) {
					buffer[bufTop] = buffer[0];
					dict.addEntry(prevEntry, buffer, bufTop + 1);
				}
				int length = dict.getEntry(entryNum, buffer, bufTop);
				if (prevEntry != null && entryNum != dictSize) {
					dict.addEntry(prevEntry, buffer, bufTop + 1);
				}
				if (bufTop > 0) {
					System.arraycopy(buffer, bufTop, buffer, 0, length);
				}
				bufTop = length;
				prevEntry = entryNum;
			} else if (symbol != null) {
				throw new IllegalStateException("Read invalid symbol type "
						+ symbol.getClass().getSimpleName());
			}
		} while (symbol != null);
	}

	/**
	 * A dictionary of byte sequences. Each new element of the dictionary
	 * receives an item number in order of insertion, but they can be searched
	 * efficiently via a hash table.
	 * 
	 * Each dictionary entry represents a string, and is stored as the last byte
	 * in the string plus a pointer to an entry that represents the previous
	 * bytes.
	 * 
	 * The number of hash buckets is higher than the number of elements to
	 * reduce hash collisions as much as possible. The first 256 elements of the
	 * dictionary represent all the 8-bit byte values so they aren't hashed.
	 * 
	 * @author Jacobo Tarrio
	 */
	private static class Dictionary {

		private int hashBuckets;
		private int[] hashTable;
		private DictEntry[] entries;
		private int nextEntry;

		/**
		 * Creates a dictionary which is able to store the given number of
		 * entries.
		 * 
		 * @param numEntries
		 */
		public Dictionary(int numEntries) {
			hashBuckets = (int) Math.floor(numEntries * Math.sqrt(2));
			hashTable = new int[hashBuckets];
			entries = new DictEntry[numEntries];
			for (int i = 0; i < 256; ++i) {
				entries[i] = new DictEntry(null, (byte) i);
			}
			reset();
		}

		/**
		 * Reset the dictionary to its default contents.
		 */
		public void reset() {
			nextEntry = 257;
			for (int i = 0; i < hashBuckets; ++i) {
				hashTable[i] = -1;
			}
		}

		/**
		 * Returns the number of elements currently in the dictionary.
		 */
		public int getSize() {
			return nextEntry;
		}

		/**
		 * Adds a new entry representing a byte preceded by a previous
		 * dictionary entry.
		 * 
		 * @param prevEntry
		 *            The dictionary entry that represents the previous bytes.
		 * @param buffer
		 *            The buffer where all the data is being processed.
		 * @param bufTop
		 *            The number of elements in the buffer.
		 * @return The index of the new element.
		 */
		public int addEntry(int prevEntry, byte[] buffer, int bufTop) {
			entries[nextEntry] = new DictEntry(entries[prevEntry],
					buffer[bufTop - 1]);
			int hash = hash(buffer, bufTop);
			while (hashTable[hash] != -1) {
				++hash;
				hash %= hashBuckets;
			}
			hashTable[hash] = nextEntry;
			return nextEntry++;
		}

		/**
		 * Returns the index of an element that matches a byte sequence.
		 * @param buffer The data to match.
		 * @param bufTop The length of the data to match.
		 * @return The index of the matching element, or -1 if none exists.
		 */
		public int getEntryNum(byte[] buffer, int bufTop) {
			if (bufTop == 1) {
				return buffer[0] & 0xff;
			}
			int hash = hash(buffer, bufTop);
			int entryNum = hashTable[hash];
			while (entryNum != -1 && !isHashBucket(hash, buffer, bufTop - 1)) {
				++hash;
				hash %= hashBuckets;
				entryNum = hashTable[hash];
			}
			return entryNum == -1 ? null : entryNum;
		}

		/**
		 * Fills a buffer with the contents of the element at the given index.
		 * @param num The element index.
		 * @param buffer The buffer to write the data to.
		 * @param offset The offset within the buffer to start writing data.
		 * @return The number of bytes copied.
		 */
		public int getEntry(int num, byte[] buffer, int offset) {
			if (num < 256) {
				buffer[offset] = (byte) num;
				return 1;
			}
			DictEntry entry = entries[num];
			int pos = 0;
			while (entry != null) {
				buffer[offset + pos++] = entry.thisByte;
				entry = entry.prevEntry;
			}
			int i = offset;
			int j = offset + pos - 1;
			while (i < j) {
				byte c = buffer[i];
				buffer[i] = buffer[j];
				buffer[j] = c;
				++i;
				--j;
			}
			return pos;
		}

		private boolean isHashBucket(int hash, byte[] buffer, int lastByte) {
			DictEntry entry = entries[hashTable[hash]];
			while (lastByte >= 0) {
				if (entry == null || entry.thisByte != buffer[lastByte]) {
					return false;
				}
				--lastByte;
				entry = entry.prevEntry;
			}
			return entry == null;
		}

		private int hash(byte[] bytes, int length) {
			int hash = 0;
			for (int i = 0; i < length; ++i) {
				hash *= 257;
				hash += bytes[i] & 0xff;
				hash %= hashBuckets;
			}
			return hash;
		}

		private static class DictEntry {
			public DictEntry prevEntry;
			public byte thisByte;

			public DictEntry(DictEntry prevEntry, byte thisByte) {
				this.prevEntry = prevEntry;
				this.thisByte = thisByte;
			}
		}
	}
}
