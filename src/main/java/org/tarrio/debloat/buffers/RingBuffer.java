package org.tarrio.debloat.buffers;

import java.io.IOException;

/**
 * A circular buffer with enough capacity to find a sequence of bytes in the
 * past bytes read, and to reconstruct a reference to past bytes written.
 * 
 * @author Jacobo Tarrio
 */
public interface RingBuffer {

	/**
	 * Reads bytes from the input stream into a byte array.
	 * 
	 * @param destBuffer
	 *            The destination byte array. May be null.
	 * @param length
	 *            The number of bytes to read.
	 * @return The number of bytes that were actually read, or -1 if the end of
	 *         the stream was reached.
	 * @throws IOException
	 *             If there was a problem reading from the stream.
	 */
	int read(byte[] destBuffer, int length) throws IOException;

	/**
	 * Reads and discards the given number of bytes from the input stream,
	 * effectively skipping them.
	 * 
	 * @param length
	 *            The number of bytes to skip.
	 * @return The number of bytes that were actually skipped, or -1 if the end
	 *         of the stream was reached.
	 * @throws IOException
	 *             If there was a problem reading from the stream.
	 */
	int skip(int length) throws IOException;

	/**
	 * Checks if the next-to-be-read data is a duplicate of data read in the
	 * past, and if so, returns an object containing the match distance and the
	 * length for the longest match that is closest to the current position.
	 * 
	 * @return A {@link Match} object giving the distance and length of the best
	 *         match, or null if no good match was found.
	 * @throws IOException
	 *             If there was a problem reading from the stream.
	 */
	Match findPastMatch() throws IOException;

	/**
	 * Writes one byte to the output stream.
	 * 
	 * @param b
	 *            The byte to write.
	 * @throws IOException
	 *             If there was a problem writing to the stream.
	 */
	void write(byte b) throws IOException;

	/**
	 * Writes the contents of a byte array to the output stream.
	 * 
	 * @param data
	 *            The array with the bytes to write.
	 * @throws IOException
	 *             If there was a problem writing to the stream.
	 */
	void write(byte[] data) throws IOException;

	/**
	 * Writes a part of a byte array to the output stream.
	 * 
	 * @param data
	 *            The array with the bytes to write.
	 * @param offset
	 *            The starting offset within the array of the bytes to write.
	 * @param length
	 *            The number of bytes to write.
	 * @throws IOException
	 *             If there was a problem writing to the stream.
	 */
	void write(byte[] data, int offset, int length) throws IOException;

	/**
	 * Retrieves data written in the past and writes it again at the current
	 * position.
	 * 
	 * @param distance
	 *            The number of bytes in the past.
	 * @param length
	 *            The number of bytes to copy.
	 * @throws IOException
	 *             If there was a problem writing to the stream.
	 */
	void repeatPastMatch(int distance, int length) throws IOException;

	/**
	 * Contains the results of a search for a past matching substring, as
	 * returned by findPastMatch.
	 */
	public interface Match {
		/**
		 * @return The match distance, that is: the number of bytes in the past
		 *         the duplicate was found.
		 */
		public int getDistance();

		/**
		 * @return The match length.
		 */
		public int getLength();
	}

}
