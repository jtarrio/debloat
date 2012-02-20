package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Defines an API for classes that provide methods to read and write compressed
 * data.
 * 
 * @author Jacobo
 * 
 */
public interface Codec {

	/**
	 * Returns an instance of a class to write compressed data into an output
	 * stream.
	 * 
	 * @param output
	 *            The stream to write the data into.
	 * @return An encoder instance.
	 * @throws IOException
	 *             If there was any problem writing into the output stream.
	 */
	Encoder getEncoder(OutputStream output) throws IOException;

	/**
	 * Returns an instance of a class to read compressed data from an input
	 * stream.
	 * 
	 * @param input
	 *            The stream to read data from.
	 * @return A decoder instance.
	 * @throws IOException
	 *             If there was any problem reading from the input stream.
	 */
	Decoder getDecoder(InputStream input) throws IOException;

	/**
	 * Compression method.
	 */
	public enum Compression {
		/**
		 * Automatically determine whether and how the block's data should be
		 * compressed.
		 */
		AUTO,

		/**
		 * Do not compress the block's data.
		 */
		NONE,

		/**
		 * Compress the block's data and encode the references using static
		 * Huffman.
		 */
		STATIC_HUFFMAN,

		/**
		 * Compress the block's data and encode the references using dynamic
		 * Huffman.
		 */
		DYNAMIC_HUFFMAN;
	}

	/**
	 * Defines an API for classes that write compressed data.
	 */
	public interface Encoder {
		/**
		 * Starts a new compressed data block.
		 * 
		 * @return An encoder for the new block.
		 */
		BlockEncoder newBlock() throws IOException;

		/**
		 * Finishes encoding the data.
		 * 
		 * @throws IOException
		 *             If there was any problem writing the data.
		 */
		void close() throws IOException;
	}

	/**
	 * API with methods to save a compressed data block.
	 */
	public interface BlockEncoder {
		/**
		 * Sets whether the current block is the last block of the compressed
		 * data stream.
		 * 
		 * @return The current block encoder, for chaining operations.
		 */
		BlockEncoder setLastBlock(boolean lastBlock);

		/**
		 * Sets the compression method to use for this block.
		 * 
		 * @return The current block encoder, for chaining operations.
		 */
		BlockEncoder compress(Compression compression);

		/**
		 * Adds a chunk to the compressed data.
		 * 
		 * @param chunk
		 *            The chunk to add.
		 * @return The current block encoder, for chaining operations.
		 */
		BlockEncoder addChunk(Chunk chunk);

		/**
		 * Ends the current block and writes it to the output.
		 * 
		 * @return The current encoder, for chaining operations.
		 * @throws IOException
		 *             If there was any problem writing the data.
		 */
		Encoder endBlock() throws IOException;
	}

	/**
	 * Defines an API for classes that read compressed data.
	 */
	public interface Decoder {
		/**
		 * Reads the next block.
		 * 
		 * @return A decoder for the next block, or null if there are no blocks
		 *         left.
		 * @throws IOException
		 *             If there was any problem reading the data.
		 */
		BlockDecoder nextBlock() throws IOException;
	}

	/**
	 * API with methods to read from a compressed data block.
	 */
	public interface BlockDecoder {
		/**
		 * Returns whether the current block is the last block in the compressed
		 * data stream.
		 */
		boolean isLastBlock();

		/**
		 * Returns the next chunk in the current compressed data block.
		 * 
		 * @return The next chunk in the block, or null if there are no more
		 *         chunks.
		 * @throws IOException
		 *             If there was any problem reading the data.
		 */
		Chunk getNextChunk() throws IOException;
	}
}
