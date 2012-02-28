package org.tarrio.dilate;

import java.io.IOException;

/**
 * A buffer that keeps the last and next bytes processed (read/written) to be
 * able to find and reconstruct duplicates.
 * 
 * @author Jacobo Tarrio
 */
public interface LookupBuffer {

	/**
	 * Reads bytes into a byte array.
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
	 * Reads and discards the given number of bytes, effectively skipping them.
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
	 * Finds the best match in the past data for the data at the current and
	 * next read positions. The best match is the longest match that is closest
	 * to the current position.
	 * 
	 * @return A {@link Match} object giving the distance and length of the best
	 *         match, or null if no good match was found.
	 * @throws IOException
	 *             If there was a problem reading from the stream.
	 */
	Match findPastMatch() throws IOException;

	/**
	 * Writes one byte.
	 * 
	 * @param b
	 *            The byte to write.
	 * @throws IOException
	 *             If there was a problem writing to the stream.
	 */
	void write(byte b) throws IOException;

	/**
	 * Writes a byte array.
	 * 
	 * @param data
	 *            The array with the bytes to write.
	 * @throws IOException
	 *             If there was a problem writing to the stream.
	 */
	void write(byte[] data) throws IOException;

	/**
	 * Writes a part of a byte array.
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
