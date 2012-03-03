package org.tarrio.debloat.algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tarrio.debloat.Codec;
import org.tarrio.debloat.CompressionAlgorithm;
import org.tarrio.debloat.Symbol;
import org.tarrio.debloat.buffers.RingBuffer;
import org.tarrio.debloat.buffers.RingBufferFactory;
import org.tarrio.debloat.buffers.RingBuffer.Match;

/**
 * Implementation of LZ77.
 * 
 * @author Jacobo Tarrio
 */
public class Lz77 implements CompressionAlgorithm {

	/**
	 * This algorithm's name.
	 * 
	 * Visible for testing.
	 */
	static final String NAME = "LZ77";

	@Override
	public void compress(InputStream input, Codec.Encoder encoder)
			throws IOException {
		RingBuffer buffer = RingBufferFactory.newReadBuffer(input);
		encoder.setAlgorithm(NAME);
		Symbol symbol = readNextSymbol(buffer);
		while (symbol != null) {
			encoder.write(symbol);
			symbol = readNextSymbol(buffer);
		}
		encoder.close();
	}

	@Override
	public void decompress(Codec.Decoder decoder, OutputStream output)
			throws IOException {
		String algoritm = decoder.getAlgoritm();
		if (!NAME.equals(algoritm)) {
			throw new IllegalStateException(String.format(
					"Tried to decompress %s data with a %s decompressor",
					algoritm, NAME));
		}
		RingBuffer buffer = RingBufferFactory.newWriteBuffer(output);
		Symbol symbol = decoder.read();
		while (symbol != null) {
			if (symbol instanceof Symbol.BackRef) {
				Symbol.BackRef backref = (Symbol.BackRef) symbol;
				buffer.repeatPastMatch(backref.getDistance(), backref.getLength());
			} else if (symbol instanceof Symbol.Byte) {
				buffer.write(((Symbol.Byte) symbol).getByteValue());
			} else {
				throw new IllegalStateException("Found symbol of unrecognized type " + symbol.getClass().getSimpleName());
			}
			symbol = decoder.read();
		}
	}

	/**
	 * Obtains the next symbol from the contents of a look-up buffer.
	 * 
	 * @param buffer
	 *            The look-up buffer to read from.
	 * @return The symbol that was read.
	 * @throws IOException
	 *             If there was any problem reading from the buffer.
	 */
	private Symbol readNextSymbol(RingBuffer buffer) throws IOException {
		byte[] buf = new byte[1];
		Match match = buffer.findPastMatch();
		if (match != null) {
			buffer.skip(match.getLength());
			return Symbol.newBackRef(match.getDistance(), match.getLength());
		} else {
			int read = buffer.read(buf, 1);
			return read == 1 ? Symbol.newByte(buf[0]) : null;
		}
	}
}