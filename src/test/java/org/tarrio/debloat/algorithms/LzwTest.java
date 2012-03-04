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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.tarrio.debloat.Codec;
import org.tarrio.debloat.Codec.Decoder;
import org.tarrio.debloat.Symbol;
import org.tarrio.debloat.Codec.Encoder;

import junit.framework.TestCase;

/**
 * Tests for {@link Lzw}.
 * 
 * @author Jacobo Tarrio
 */
public class LzwTest extends TestCase {

	private static final byte[] SIMPLE_EXAMPLE_BYTES = "TOBEORNOTTOBEORTOBEORNOT"
			.getBytes();

	private static final byte[] REPEATED_BYTES = "TOTTTTTTBE".getBytes();

	private static final Symbol[] SIMPLE_EXAMPLE_SYMBOLS = new Symbol[] {
			Symbol.newDictionaryRef('T'), Symbol.newDictionaryRef('O'),
			Symbol.newDictionaryRef('B'), Symbol.newDictionaryRef('E'),
			Symbol.newDictionaryRef('O'), Symbol.newDictionaryRef('R'),
			Symbol.newDictionaryRef('N'), Symbol.newDictionaryRef('O'),
			Symbol.newDictionaryRef('T'), Symbol.newDictionaryRef(257),
			Symbol.newDictionaryRef(259), Symbol.newDictionaryRef(261),
			Symbol.newDictionaryRef(266), Symbol.newDictionaryRef(260),
			Symbol.newDictionaryRef(262), Symbol.newDictionaryRef(264) };

	private static final Symbol[] RESETTING_EXAMPLE_SYMBOLS = new Symbol[] {
			Symbol.newDictionaryRef('T'), Symbol.newDictionaryRef('O'),
			Symbol.newDictionaryRef('B'), Symbol.newDictionaryRef('E'),
			Symbol.newDictionaryRef('O'), Symbol.newDictionaryRef('R'),
			Symbol.newDictionaryRef('N'), Symbol.newDictionaryRef('O'),
			Symbol.newDictionaryRef('T'), Symbol.newDictionaryRef(257),
			Symbol.newDictionaryRef(259), Symbol.newReset(),
			Symbol.newDictionaryRef('O'), Symbol.newDictionaryRef('R'),
			Symbol.newDictionaryRef('T'), Symbol.newDictionaryRef('O'),
			Symbol.newDictionaryRef('B'), Symbol.newDictionaryRef('E'),
			Symbol.newDictionaryRef(257), Symbol.newDictionaryRef('N'),
			Symbol.newDictionaryRef('O'), Symbol.newDictionaryRef('T') };

	private static final Symbol[] REPEATED_EXAMPLE_SYMBOLS = new Symbol[] {
			Symbol.newDictionaryRef('T'), Symbol.newDictionaryRef('O'),
			Symbol.newDictionaryRef('T'), Symbol.newDictionaryRef(259),
			Symbol.newDictionaryRef(260), Symbol.newDictionaryRef('B'),
			Symbol.newDictionaryRef('E') };

	private IMocksControl control;
	private Encoder encoder;
	private Decoder decoder;

	@Override
	protected void setUp() throws Exception {
		control = EasyMock.createControl();
		encoder = control.createMock(Codec.Encoder.class);
		decoder = control.createMock(Codec.Decoder.class);
	}

	public void testCompressSimpleText() throws Exception {
		Lzw compressor = new Lzw();
		ByteArrayInputStream stream = new ByteArrayInputStream(
				SIMPLE_EXAMPLE_BYTES);

		encoder.setAlgorithm(compressor.getAlgorithmName());
		for (int i = 0; i < SIMPLE_EXAMPLE_SYMBOLS.length; ++i) {
			encoder.write(SIMPLE_EXAMPLE_SYMBOLS[i]);
		}
		encoder.close();

		control.replay();
		compressor.compress(stream, encoder);
		control.verify();
	}

	public void testDecompressSimpleText() throws Exception {
		Lzw compressor = new Lzw();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		EasyMock.expect(decoder.getAlgoritm()).andReturn(
				compressor.getAlgorithmName());
		for (int i = 0; i < SIMPLE_EXAMPLE_SYMBOLS.length; ++i) {
			EasyMock.expect(decoder.read())
					.andReturn(SIMPLE_EXAMPLE_SYMBOLS[i]);
		}
		EasyMock.expect(decoder.read()).andReturn(null);

		control.replay();
		compressor.decompress(decoder, stream);
		control.verify();

		byte[] outputBytes = stream.toByteArray();
		assertEquals(SIMPLE_EXAMPLE_BYTES.length, outputBytes.length);
		for (int i = 0; i < outputBytes.length; ++i) {
			assertEquals(SIMPLE_EXAMPLE_BYTES[i], outputBytes[i]);
		}
	}

	public void testCompressWithResets() throws Exception {
		Lzw compressor = new Lzw(268);
		ByteArrayInputStream stream = new ByteArrayInputStream(
				SIMPLE_EXAMPLE_BYTES);

		encoder.setAlgorithm(compressor.getAlgorithmName());
		for (int i = 0; i < RESETTING_EXAMPLE_SYMBOLS.length; ++i) {
			encoder.write(RESETTING_EXAMPLE_SYMBOLS[i]);
		}
		encoder.close();

		control.replay();
		compressor.compress(stream, encoder);
		control.verify();
	}

	public void testDecompressWithResets() throws Exception {
		Lzw compressor = new Lzw();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		EasyMock.expect(decoder.getAlgoritm()).andReturn(
				compressor.getAlgorithmName());
		for (int i = 0; i < RESETTING_EXAMPLE_SYMBOLS.length; ++i) {
			EasyMock.expect(decoder.read()).andReturn(
					RESETTING_EXAMPLE_SYMBOLS[i]);
		}
		EasyMock.expect(decoder.read()).andReturn(null);

		control.replay();
		compressor.decompress(decoder, stream);
		control.verify();

		byte[] outputBytes = stream.toByteArray();
		assertEquals(SIMPLE_EXAMPLE_BYTES.length, outputBytes.length);
		for (int i = 0; i < outputBytes.length; ++i) {
			assertEquals(SIMPLE_EXAMPLE_BYTES[i], outputBytes[i]);
		}
	}

	public void testCompressRepeatedText() throws Exception {
		Lzw compressor = new Lzw();
		ByteArrayInputStream stream = new ByteArrayInputStream(REPEATED_BYTES);

		encoder.setAlgorithm(compressor.getAlgorithmName());
		for (int i = 0; i < REPEATED_EXAMPLE_SYMBOLS.length; ++i) {
			encoder.write(REPEATED_EXAMPLE_SYMBOLS[i]);
		}
		encoder.close();

		control.replay();
		compressor.compress(stream, encoder);
		control.verify();
	}

	public void testDecompressRepeatedText() throws Exception {
		Lzw compressor = new Lzw();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		EasyMock.expect(decoder.getAlgoritm()).andReturn(
				compressor.getAlgorithmName());
		for (int i = 0; i < REPEATED_EXAMPLE_SYMBOLS.length; ++i) {
			EasyMock.expect(decoder.read()).andReturn(
					REPEATED_EXAMPLE_SYMBOLS[i]);
		}
		EasyMock.expect(decoder.read()).andReturn(null);

		control.replay();
		compressor.decompress(decoder, stream);
		control.verify();

		byte[] outputBytes = stream.toByteArray();
		assertEquals(REPEATED_BYTES.length, outputBytes.length);
		for (int i = 0; i < outputBytes.length; ++i) {
			assertEquals(REPEATED_BYTES[i], outputBytes[i]);
		}
	}

}
