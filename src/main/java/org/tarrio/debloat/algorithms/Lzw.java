package org.tarrio.debloat.algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tarrio.debloat.Symbol;
import org.tarrio.debloat.Codec.Decoder;
import org.tarrio.debloat.Codec.Encoder;

public class Lzw extends AbstractCompressionAlgorithmImpl {

	static final int MAX_ENTRIES = 4096;
	
	private int maxEntries;
	
	public Lzw() {
		this(MAX_ENTRIES);
	}
	
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
		Integer prevEntry = null;
		int read = 1;
		while (read > 0) {
			if (dict.getSize() == maxEntries) {
				dict.reset();
				outputReset(outputEncoder);
			}
			read = input.read(buffer, bufTop, 1);
			if (read > 0) {
				++bufTop;
			}
			Integer curEntry = dict.getEntryNum(buffer, bufTop);
			if (prevEntry != null && curEntry == null) {
				outputEntry(prevEntry, outputEncoder);
				dict.addEntry(prevEntry, buffer, bufTop);
				buffer[0] = buffer[bufTop - 1];
				bufTop = 1;
				prevEntry = buffer[0] & 0xff;
			} else {
				prevEntry = curEntry;
			}
		}
		if (prevEntry != null) {
			outputEntry(prevEntry, outputEncoder);
		}
		outputEncoder.close();
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
				int length = dict.getEntry(ref.getEntry(), buffer, bufTop);
				if (prevEntry != null) {
					dict.addEntry(prevEntry, buffer, bufTop + 1);
				}
				if (bufTop > 0) {
					System.arraycopy(buffer, bufTop, buffer, 0, length);
				}
				bufTop = length;
				prevEntry = ref.getEntry();
			} else if (symbol != null) {
				throw new IllegalStateException("Read invalid symbol type "
						+ symbol.getClass().getSimpleName());
			}
		} while (symbol != null);
	}

	private void outputReset(Encoder outputEncoder) throws IOException {
		outputEncoder.write(Symbol.newReset());
	}

	private void outputEntry(int entry, Encoder outputEncoder)
			throws IOException {
		outputEncoder.write(Symbol.newDictionaryRef(entry));
	}

	private static class Dictionary {

		private int hashBuckets;
		private int[] hashTable;
		private DictEntry[] entries;
		private int nextEntry;

		public Dictionary(int numEntries) {
			hashBuckets = numEntries * 3 / 2;
			hashTable = new int[hashBuckets];
			entries = new DictEntry[numEntries];
			reset();
		}

		public void reset() {
			nextEntry = 257;
			for (int i = 0; i < 256; ++i) {
				entries[i] = new DictEntry(null, (byte) i);
				hashTable[i] = i;
			}
			for (int i = 257; i < entries.length; ++i) {
				hashTable[i] = -1;
			}
		}

		public int getSize() {
			return nextEntry;
		}

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

		public Integer getEntryNum(byte[] buffer, int bufTop) {
			int hash = hash(buffer, bufTop);
			int entryNum = hashTable[hash];
			while (entryNum != -1 && !isHashBucket(hash, buffer, bufTop - 1)) {
				++hash;
				hash %= hashBuckets;
				entryNum = hashTable[hash];
			}
			return entryNum == -1 ? null : entryNum;
		}

		public int getEntry(int num, byte[] buffer, int offset) {
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
