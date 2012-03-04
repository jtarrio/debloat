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
 * An interface for compressors that read uncompressed/compressed data from a
 * stream and write compressed/uncompressed data into another stream.
 * 
 * @author Jacobo Tarrio
 */
public interface CompressionAlgorithm {

	/**
	 * Reads uncompressed data from an input stream and writes a compressed
	 * version of it to the output stream.
	 * 
	 * @param input
	 *            The stream to read uncompressed data from.
	 * @param outputEncoder
	 *            The encoder to write compressed data to.
	 * @throws IOException
	 *             If there was a problem reading from the input stream or
	 *             writing into the output stream.
	 */
	public abstract void compress(InputStream input, Codec.Encoder outputEncoder)
			throws IOException;

	/**
	 * Reads compressed data from an input stream and writes the uncompressed
	 * version of it to the output stream.
	 * 
	 * @param inputDecoder
	 *            The decoder to read compressed data from.
	 * @param output
	 *            The stream to write uncompressed data into.
	 * @throws IOException
	 *             If there was a problem reading from the input stream or
	 *             writing into the output stream.
	 */
	public abstract void decompress(Codec.Decoder inputDecoder, OutputStream output)
			throws IOException;

}