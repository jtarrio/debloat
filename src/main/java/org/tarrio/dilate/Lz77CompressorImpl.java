package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tarrio.dilate.RingBuffer.Match;

/**
 * Implementation of LZ77 that uses a specified codec for the compressed data
 * format.
 * 
 * @author Jacobo Tarrio
 */
public class Lz77CompressorImpl implements Compressor {

	/**
	 * This algorithm's name.
	 * 
	 * Visible for testing.
	 */
	static final String ALGORITHM = "LZ77";

	@Override
	public void compress(InputStream input, Codec.Encoder encoder)
			throws IOException {
		RingBuffer buffer = new RingBufferImpl(input);
		encoder.setAlgorithm(ALGORITHM);
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
		if (!ALGORITHM.equals(algoritm)) {
			throw new IllegalStateException(String.format(
					"Tried to decompress %s data with a %s decompressor",
					algoritm, ALGORITHM));
		}
		RingBuffer buffer = new RingBufferImpl(output);
		Symbol symbol = decoder.read();
		while (symbol != null) {
			if (symbol.isReference()) {
				buffer.repeatPastMatch(symbol.getDistance(), symbol.getLength());
			} else {
				buffer.write(symbol.getByte());
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
			return new Symbol(match.getDistance(), match.getLength());
		} else {
			int read = buffer.read(buf, 1);
			return read == 1 ? new Symbol(buf[0]) : null;
		}
	}
}