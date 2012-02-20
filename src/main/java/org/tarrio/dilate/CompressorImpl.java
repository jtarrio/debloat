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

public class CompressorImpl implements Compressor {

	public static final int DEFAULT_TARGET_BLOCK_SIZE = 65536;
	
	private final Codec codec;

	private final int targetBlockSize;

	public CompressorImpl(Codec codec) {
		this(codec, DEFAULT_TARGET_BLOCK_SIZE);
	}
	
	public CompressorImpl(Codec codec, int targetBlockSize) {
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
				int nextSize = targetBlockSize <= blockSize ? targetBlockSize : targetBlockSize - blockSize;
				chunk = getNextChunk(buffer, nextSize);
			}
			if (chunk == null) {
				block.setLastBlock(true);
			}
			block.endBlock();
		} while (chunk != null);
		encoder.close();
	}
	
	private Chunk getNextChunk(LookbackBuffer buffer, int targetSize) throws IOException {
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
			return new Chunk(match.getDistance(), match.getLength(), buf);
		} else {
			byte[] buf = new byte[targetSize];
			int pos;
			for (pos = 0; pos < buf.length; ++pos) {
				if (buffer.isEof() || buffer.findMatch() != null) {
					break;
				}
				buf[pos] = buffer.read();
			}
			return new Chunk(0, pos, Arrays.copyOf(buf, pos));
		}
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
}