package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.tarrio.dilate.Codec.BlockDecoder;
import org.tarrio.dilate.Codec.BlockEncoder;
import org.tarrio.dilate.Codec.Decoder;
import org.tarrio.dilate.Codec.Encoder;
import org.tarrio.dilate.LookbackBuffer.Match;

/**
 * Implementation of DEFLATE that uses a specified codec for the compressed data
 * format.
 */
public class DeflateImpl implements Compressor {

	/**
	 * Default target block size.
	 */
	public static final int DEFAULT_TARGET_BLOCK_SIZE = 65536;

	private final Codec codec;
	private final int targetBlockSize;

	/**
	 * Creates a compressor that reads and writes compressed data using the
	 * given codec and tries to read the data to compress in blocks of the
	 * default target block size.
	 * 
	 * @param codec
	 *            The codec to use.
	 */
	public DeflateImpl(Codec codec) {
		this(codec, DEFAULT_TARGET_BLOCK_SIZE);
	}

	/**
	 * Creates a compressor that reads and writes compressed data using the
	 * given codec, and tries to read the data to compress in blocks of the
	 * specified size.
	 * 
	 * @param codec
	 *            The codec to use.
	 * @param targetBlockSize
	 *            The target block size.
	 */
	public DeflateImpl(Codec codec, int targetBlockSize) {
		this.codec = codec;
		this.targetBlockSize = targetBlockSize;
	}

	@Override
	public void compress(InputStream input, OutputStream output)
			throws IOException {
		LookbackBuffer buffer = new LookbackBuffer(input);
		Encoder encoder = codec.getEncoder(output);
		Chunk chunk = getNextChunk(buffer, targetBlockSize);
		do {
			BlockEncoder block = encoder.newBlock();
			int blockSize = 0;
			while (chunk != null && blockSize < targetBlockSize) {
				block.addChunk(chunk);
				blockSize += chunk.getLength();
				int nextSize = targetBlockSize <= blockSize ? targetBlockSize
						: targetBlockSize - blockSize;
				chunk = getNextChunk(buffer, nextSize);
			}
			if (chunk == null) {
				block.setLastBlock(true);
			}
			block.endBlock();
		} while (chunk != null);
		encoder.close();
	}

	@Override
	public void decompress(InputStream input, OutputStream output)
			throws IOException {
		Decoder decoder = codec.getDecoder(input);
		BlockDecoder block;
		do {
			block = decoder.nextBlock();
			if (block == null) {
				break;
			}
			Chunk chunk = block.getNextChunk();
			while (chunk != null) {
				output.write(chunk.getBytes());
				chunk = block.getNextChunk();
			}
		} while (!block.isLastBlock());
	}

	/**
	 * Reads the next chunk from the input stream, trying to find matches in the
	 * look-back buffer.
	 * 
	 * @param buffer
	 *            The look-back buffer to read from.
	 * @param targetSize
	 *            The target block size.
	 * @return A chunk representing new data or a reference to already read
	 *         data.
	 * @throws IOException
	 *             If there was a problem reading from the look-back buffer.
	 */
	private Chunk getNextChunk(LookbackBuffer buffer, int targetSize)
			throws IOException {
		if (buffer.isEof()) {
			return null;
		}
		Match match = buffer.findMatch();
		if (match != null) {
			byte[] buf = new byte[match.getLength()];
			for (int i = 0; i < match.getLength(); ++i) {
				buf[i] = buffer.read();
			}
			if (match.getDistance() > 32767) {
				System.out.println("foo");
			}
			return new Chunk(match.getDistance(), buf);
		} else {
			byte[] buf = new byte[targetSize];
			int pos;
			for (pos = 0; pos < buf.length; ++pos) {
				if (buffer.isEof() || buffer.findMatch() != null) {
					break;
				}
				buf[pos] = buffer.read();
			}
			return new Chunk(0, Arrays.copyOf(buf, pos));
		}
	}
}