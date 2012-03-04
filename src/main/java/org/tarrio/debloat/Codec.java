/**
 * Copyright 2012 Jacobo Tarrio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tarrio.debloat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Defines an API for classes that provide methods to read and write compressed
 * data to and from streams.
 * 
 * @author Jacobo Tarrio
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

		/**
		 * Sets the name of the algorithm that was used to compress the data.
		 * 
		 * @throws IOException
		 *             If there was any problem writing data.
		 */
		void setAlgorithm(String algorithm) throws IOException;

		/**
		 * Writes a symbol to the output stream.
		 * 
		 * @param symbol
		 *            The symbol to write.
		 * @throws IOException
		 *             If there was any problem encoding or writing the data.
		 */
		void write(Symbol symbol) throws IOException;

		/**
		 * Finishes encoding the data.
		 * 
		 * @throws IOException
		 *             If there was any problem encoding or writing the data.
		 */
		void close() throws IOException;
	}

	/**
	 * Defines an API for classes that read compressed data.
	 */
	public interface Decoder {

		/**
		 * Returns the name of the algorithm that was used to compress the data.
		 * 
		 * @throws IOException
		 *             If there was any problem reading or decoding the data.
		 */
		String getAlgoritm() throws IOException;

		/**
		 * Reads a symbol from the input stream.
		 * 
		 * @return The symbol that was read, or null if the end of the stream
		 *         was reached.
		 * @throws IOException
		 *             If there was any problem reading or decoding the data.
		 */
		Symbol read() throws IOException;
	}
}
