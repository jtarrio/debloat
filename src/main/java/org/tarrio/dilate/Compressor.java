package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An interface for compressors that read uncompressed/compressed data from a
 * stream and write compressed/uncompressed data into another stream.
 * 
 * @author Jacobo
 */
public interface Compressor {

	/**
	 * Reads uncompressed data from an input stream and writes a compressed
	 * version of it to the output stream.
	 * 
	 * @param input
	 *            The stream to read uncompressed data from.
	 * @param output
	 *            The stream to write compressed data into.
	 * @throws IOException
	 *             If there was a problem reading from the input stream or
	 *             writing into the output stream.
	 */
	public abstract void compress(InputStream input, OutputStream output)
			throws IOException;

	/**
	 * Reads compressed data from an input stream and writes the uncompressed
	 * version of it to the output stream.
	 * 
	 * @param input
	 *            The stream to read compressed data from.
	 * @param output
	 *            The stream to write uncompressed data into.
	 * @throws IOException
	 *             If there was a problem reading from the input stream or
	 *             writing into the output stream.
	 */
	public abstract void decompress(InputStream input, OutputStream output)
			throws IOException;

}