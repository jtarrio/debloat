package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tarrio.dilate.Codec.Decoder;
import org.tarrio.dilate.Codec.Encoder;
import org.tarrio.dilate.LookupBuffer.Match;

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

	private final Codec codec;

	/**
	 * Creates a compressor that reads and writes compressed data using the
	 * given codec and tries to read the data to compress in blocks of the
	 * default target block size.
	 * 
	 * @param codec
	 *            The codec to use.
	 */
	public Lz77CompressorImpl(Codec codec) {
		this.codec = codec;
	}

	@Override
	public void compress(InputStream input, OutputStream output)
			throws IOException {
		LookupBuffer buffer = new LookupBuffer(input);
		Encoder encoder = codec.getEncoder(output);
		encoder.setAlgorithm(ALGORITHM);
		Symbol symbol = readNextSymbol(buffer);
		while (symbol != null) {
			encoder.write(symbol);
			symbol = readNextSymbol(buffer);
		}
		encoder.close();
	}

	@Override
	public void decompress(InputStream input, OutputStream output)
			throws IOException {
		Decoder decoder = codec.getDecoder(input);
		String algoritm = decoder.getAlgoritm();
		if (!ALGORITHM.equals(algoritm)) {
			throw new IllegalStateException(String.format(
					"Tried to decompress %s data with a %s decompressor",
					algoritm, ALGORITHM));
		}
		LookupBuffer buffer = new LookupBuffer(output);
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
	private Symbol readNextSymbol(LookupBuffer buffer) throws IOException {
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