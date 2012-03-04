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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tarrio.debloat.Codec.Decoder;
import org.tarrio.debloat.codecs.CodecFactory;
import org.tarrio.debloat.registry.CompressionAlgorithmRegistry;

/**
 * Example command-line utility to compress and decompress files using the
 * Debloat library.
 * 
 * @author Jacobo Tarrio
 */
public class DebloatCmd {

	private static final String DEFAULT_ALGORITHM = "lz77";

	private final InputStream inputStream;
	private final OutputStream outputStream;
	private final String algorithm;
	private final Operation operation;

	public enum Operation {
		COMPRESS, DECOMPRESS;
	}

	public DebloatCmd(String inputFileName, String outputFileName,
			String algorithm, Operation operation) throws FileNotFoundException {
		this.inputStream = inputFileName == null ? System.in
				: new FileInputStream(inputFileName);
		this.outputStream = outputFileName == null ? System.out
				: new FileOutputStream(outputFileName);
		this.algorithm = algorithm;
		this.operation = operation;
	}

	private void run() throws IOException {
		Codec codec = CodecFactory.getCodec();
		if (operation == Operation.COMPRESS) {
			CompressionAlgorithm compressor = CompressionAlgorithmRegistry
					.getInstance().get(algorithm);
			compressor.compress(inputStream, codec.getEncoder(outputStream));
		} else {
			Decoder decoder = codec.getDecoder(inputStream);
			CompressionAlgorithm compressor = CompressionAlgorithmRegistry
					.getInstance().get(decoder);
			compressor.decompress(decoder, outputStream);
		}
	}

	private static void showHelp() {
		System.err
				.println("Arguments: [<command>] <inputFilename> <outputFilename>");
		System.err.println("");
		System.err
				.println("If the input or output file names are not specified, or if they are \"-\",");
		System.err.println("the standard input/output will be used.");
		System.err.println("");
		System.err.println("Commands:");
		System.err.println("  -c : Compress (default)");
		System.err.println("  -d : Decompress");
		System.err.println("  -a=<algorithm> : Select algorithm (default: "
				+ DEFAULT_ALGORITHM + ")");
		System.err.println("        Available algorithms:");
		for (String algorithm : CompressionAlgorithmRegistry.getInstance()
				.getAlgorithms()) {
			System.err.println("          - " + algorithm);
		}
	}

	public static DebloatCmd parseArgs(String[] args)
			throws FileNotFoundException {
		String input = null;
		String output = null;
		String algorithm = DEFAULT_ALGORITHM;
		Operation operation = Operation.COMPRESS;
		for (String arg : args) {
			if (arg.startsWith("-") && !"-".equals(arg)) {
				if ("-d".equals(arg)) {
					operation = Operation.DECOMPRESS;
				} else if ("-c".equals(arg)) {
					operation = Operation.COMPRESS;
				} else if (arg.startsWith("-a=")) {
					algorithm = arg.substring(3);
				} else {
					showHelp();
				}
			} else {
				if (input == null) {
					input = "-".equals(arg) ? null : arg;
				} else if (output == null) {
					output = "-".equals(arg) ? null : arg;
				} else {
					showHelp();
				}
			}
		}

		return new DebloatCmd(input, output, algorithm, operation);
	}

	public static void main(String[] args) throws IOException {
		parseArgs(args).run();
	}

}
