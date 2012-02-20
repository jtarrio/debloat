package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Codec {

	Encoder getEncoder(OutputStream output) throws IOException;
	Decoder getDecoder(InputStream input) throws IOException;
	
	public enum Compression {
		NONE, STATIC_HUFFMAN, DYNAMIC_HUFFMAN;
	}
	
	public interface Encoder {
		BlockEncoder newBlock();
		void close() throws IOException;
	}
	
	public interface BlockEncoder {
		BlockEncoder setLastBlock(boolean lastBlock);
		BlockEncoder compress(Compression compression);
		BlockEncoder addChunk(Chunk chunk);
		Encoder endBlock() throws IOException;
	}
	
	public interface Decoder {
		BlockDecoder nextBlock() throws IOException;
	}
	
	public interface BlockDecoder {
		boolean isLastBlock();
		Chunk getNextChunk() throws IOException;
	}
}
