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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import org.tarrio.debloat.CompressionAlgorithm;
import org.tarrio.debloat.codecs.CodecFactory;
import org.tarrio.debloat.registry.CompressionAlgorithmRegistry;

import junit.framework.TestCase;

/**
 * End-to-end tests for Debloat.
 * 
 * @author Jacobo Tarrio
 */
public class EndToEndTest extends TestCase {

	private static final String TEST_DATA = "En un lugar de la Mancha, de cuyo"
			+ " nombre no quiero acordarme, no ha mucho tiempo que vivía un"
			+ " hidalgo de los de lanza en astillero, adarga antigua, rocín"
			+ " flaco y galgo corredor. Una olla de algo más vaca que carnero,"
			+ " salpicón las más noches, duelos y quebrantos los sábados,"
			+ " lantejas los viernes, algún palomino de añadidura los"
			+ " domingos, consumían las tres partes de su hacienda."
			+ " El resto della concluían sayo de velarte, calzas de velludo"
			+ " para las fiestas, con sus pantuflos de lo mesmo, y los días"
			+ " de entresemana se honraba con su vellorí de lo más fino."
			+ " Tenía en su casa una ama que pasaba de los cuarenta, y una"
			+ " sobrina que no llegaba a los veinte, y un mozo de campo y"
			+ " plaza, que así ensillaba el rocín como tomaba la podadera."
			+ " Frisaba la edad de nuestro hidalgo con los cincuenta años; era"
			+ " de complexión recia, seco de carnes, enjuto de rostro, gran"
			+ " madrugador y amigo de la caza. Quieren decir que tenía el"
			+ " sobrenombre de Quijada, o Quesada, que en esto hay alguna"
			+ " diferencia en los autores que deste caso escriben; aunque,"
			+ " por conjeturas verosímiles, se deja entender que se llamaba"
			+ " Quejana. Pero esto importa poco a nuestro cuento; basta que"
			+ " en la narración dél no se salga un punto de la verdad.";

	private static final byte[] BINARY_DATA = makeBinaryData();

	public void testCompressUncompressTextWithLz77() throws Exception {
		CompressionAlgorithm compressor = CompressionAlgorithmRegistry
				.getInstance().get("lz77");
		doTestCompressUncompress(compressor, TEST_DATA.getBytes());
	}

	public void testCompressUncompressBinaryDataWithLz77() throws Exception {
		CompressionAlgorithm compressor = CompressionAlgorithmRegistry
				.getInstance().get("lz77");
		doTestCompressUncompress(compressor, BINARY_DATA);
	}

	public void testCompressUncompressTextWithLzw() throws Exception {
		CompressionAlgorithm compressor = CompressionAlgorithmRegistry
				.getInstance().get("lzw");
		doTestCompressUncompress(compressor, TEST_DATA.getBytes());
	}

	public void testCompressUncompressBinaryDataWithLzw() throws Exception {
		CompressionAlgorithm compressor = CompressionAlgorithmRegistry
				.getInstance().get("lzw");
		doTestCompressUncompress(compressor, BINARY_DATA);
	}

	private void doTestCompressUncompress(CompressionAlgorithm compressor,
			byte[] testData) throws IOException {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(testData);
		Codec codec = CodecFactory.getCodec();
		ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
		compressor.compress(inputStream, codec.getEncoder(compressedStream));

		ByteArrayOutputStream uncompressedStream = new ByteArrayOutputStream();
		compressor.decompress(codec.getDecoder(new ByteArrayInputStream(
				compressedStream.toByteArray())), uncompressedStream);

		assertByteArraysEqual(testData, uncompressedStream.toByteArray());
	}

	private void assertByteArraysEqual(byte[] expected, byte[] actual) {
		int[] differences = new int[10];
		int diffCount = 0;
		boolean more = false;
		assertEquals("Sizes should be the same", expected.length, actual.length);
		for (int i = 0; i < expected.length; ++i) {
			if (expected[i] != actual[i]) {
				if (diffCount == differences.length) {
					more = true;
					break;
				}
				differences[diffCount++] = i;
			}
		}
		if (diffCount != 0) {
			StringBuilder sb = new StringBuilder("Differences found: ");
			for (int i = 0; i < diffCount; ++i) {
				int index = differences[i];
				sb.append(String.format(
						"\nBytes at position #%d are different: expected %d vs actual %d",
						index, expected[index] & 0xff, actual[index] & 0xff));
			}
			if (more) {
				sb.append("\n... and more");
			}
			fail(sb.toString());
		}
	}

	private static byte[] makeBinaryData() {
		Random random = new Random(1337L);
		byte[] testData = new byte[200000];
		for (int i = 0; i < testData.length; ++i) {
			testData[i] = (byte) random.nextInt(256);
		}
		return testData;
	}
}
