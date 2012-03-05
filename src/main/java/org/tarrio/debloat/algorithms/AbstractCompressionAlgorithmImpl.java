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

package org.tarrio.debloat.algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tarrio.debloat.CompressionAlgorithm;
import org.tarrio.debloat.Codec.Decoder;
import org.tarrio.debloat.Codec.Encoder;

/**
 * A base class for the algorithm implementations provided with Debloat.
 * 
 * This base class sets the algorithm name in the encoder and closes it after
 * compression, and also checks that the algoritm indicated by the decoder
 * matches the algorithm this class implements.
 * 
 * @author Jacobo Tarrio
 */
public abstract class AbstractCompressionAlgorithmImpl implements
		CompressionAlgorithm {

	/**
	 * Returns the name of the algorithm implemented by this class.
	 */
	protected abstract String getAlgorithmName();

	/**
	 * Override this function to implement the core compression algorithm.
	 * 
	 * The caller is in charge of setting the algorithm's name in the output
	 * encoder and of closing it after encoding is done.
	 * 
	 * @param input
	 *            The stream where the data to compress comes from.
	 * @param outputEncoder
	 *            The encoder to write the compressed data to.
	 * @throws IOException
	 *             If there was a problem reading or writing data.
	 */
	protected abstract void doCompress(InputStream input, Encoder outputEncoder)
			throws IOException;

	/**
	 * Override this function to implement the core decompression algorithm.
	 * 
	 * The caller is in charge of checking that the algorithm's name in the
	 * input decoder matches that returned by getAlgorithmName().
	 * 
	 * @param inputDecoder
	 *            The decoder to read compressed data from.
	 * @param output
	 *            The stream where the uncompressed data will be written to.
	 * @throws IOException
	 *             If there was a problem reading or writing data.
	 */
	protected abstract void doDecompress(Decoder inputDecoder,
			OutputStream output) throws IOException;

	@Override
	public final void compress(InputStream input, Encoder outputEncoder)
			throws IOException {
		outputEncoder.setAlgorithm(getAlgorithmName());
		doCompress(input, outputEncoder);
		outputEncoder.close();
	}

	@Override
	public final void decompress(Decoder inputDecoder, OutputStream output)
			throws IOException {
		String algorithm = inputDecoder.getAlgoritm();
		String thisName = getAlgorithmName();
		if (!thisName.equals(algorithm)) {
			throw new IllegalStateException(String.format(
					"Tried to decompress %s data with a %s decompressor",
					algorithm, thisName));
		}
		doDecompress(inputDecoder, output);
	}
}
