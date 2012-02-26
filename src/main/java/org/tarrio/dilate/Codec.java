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
	 * Defines an API for classes that write compressed data.
	 */
	public interface Encoder {
		void write(Symbol symbol) throws IOException;

		void write(Symbol[] symbols) throws IOException;

		/**
		 * Finishes encoding the data.
		 * 
		 * @throws IOException
		 *             If there was any problem writing the data.
		 */
		void close() throws IOException;
	}

	/**
	 * Defines an API for classes that read compressed data.
	 */
	public interface Decoder {
		Symbol read() throws IOException;

		int read(Symbol[] symbols) throws IOException;

		int read(Symbol[] symbols, int offset, int length) throws IOException;
	}
}
