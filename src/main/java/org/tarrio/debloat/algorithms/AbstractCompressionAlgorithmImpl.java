package org.tarrio.debloat.algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tarrio.debloat.CompressionAlgorithm;
import org.tarrio.debloat.Codec.Decoder;
import org.tarrio.debloat.Codec.Encoder;

public abstract class AbstractCompressionAlgorithmImpl implements CompressionAlgorithm {

	protected abstract String getAlgorithmName();

	protected abstract void doCompress(InputStream input, Encoder outputEncoder)
			throws IOException;

	protected abstract void doDecompress(Decoder inputDecoder,
			OutputStream output) throws IOException;

	@Override
	public void compress(InputStream input, Encoder outputEncoder)
			throws IOException {
		outputEncoder.setAlgorithm(getAlgorithmName());
		doCompress(input, outputEncoder);
	}

	@Override
	public void decompress(Decoder inputDecoder, OutputStream output)
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
