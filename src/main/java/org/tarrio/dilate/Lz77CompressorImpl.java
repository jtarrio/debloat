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
 */
public class Lz77CompressorImpl implements Compressor {

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
		LookupBuffer buffer = new LookupBuffer(output);
		Symbol symbol = decoder.read();
		while (symbol != null) {
			if (symbol.isReference()) {
				buffer.repeatPastMatch(symbol.getDistance(), symbol.getLength());
			} else {
				buffer.write(symbol.getValue());
			}
			symbol = decoder.read();
		}
	}

	private Symbol readNextSymbol(LookupBuffer buffer) throws IOException {
		byte[] buf = new byte[1];
		Match match = buffer.findPastMatch();
		if (match != null) {
			buffer.skip(match.getLength());
			return new Symbol((byte) 0, match.getDistance(), match.getLength());
		} else {
			int read = buffer.read(buf, 1);
			return read == 1 ? new Symbol(buf[0], 0, 0) : null;
		}
	}
}