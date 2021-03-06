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

package org.tarrio.debloat.registry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.tarrio.debloat.CompressionAlgorithm;
import org.tarrio.debloat.Codec.Decoder;
import org.tarrio.debloat.Codec.Encoder;
import org.tarrio.debloat.registry.CompressionAlgorithmBinder;
import org.tarrio.debloat.registry.CompressionAlgorithmProvider;
import org.tarrio.debloat.registry.CompressionAlgorithmRegistrationModule;
import org.tarrio.debloat.registry.CompressionAlgorithmRegistry;

import junit.framework.TestCase;

/**
 * Tests for {@link CompressionAlgorithmRegistry}.
 * 
 * @author Jacobo Tarrio
 */
public class CompressionAlgorithmRegistryTest extends TestCase {

	private static final String TEST_ALGORITHM = "test-algorithm";
	private static final String BOGUS_ALGORITHM = "bogus-algorithm";
	private CompressionAlgorithmRegistry registry;
	private IMocksControl control;
	private CompressionAlgorithmProvider provider;
	private CompressionAlgorithm compressor;

	@Override
	protected void setUp() throws Exception {
		control = EasyMock.createControl();
		registry = new CompressionAlgorithmRegistry();
		provider = control.createMock(CompressionAlgorithmProvider.class);
		compressor = control.createMock(CompressionAlgorithm.class);
	}

	public void testGetReturnsNullForUnknownAlgorithm() throws Exception {
		control.replay();
		assertNull(registry.get(BOGUS_ALGORITHM));
		control.verify();
	}

	public void testInstantiatesCompressorUsingProvider() throws Exception {
		EasyMock.expect(provider.get()).andReturn(compressor);

		control.replay();
		registry.registerFromModule(new CompressionAlgorithmRegistrationModule() {
			@Override
			public void configure(CompressionAlgorithmBinder binder) {
				binder.bind(TEST_ALGORITHM).toProvider(provider);
			}
		});
		assertEquals(compressor, registry.get(TEST_ALGORITHM));
		control.verify();
	}

	public void testProvidesRegisteredInstance() throws Exception {
		control.replay();
		registry.registerFromModule(new CompressionAlgorithmRegistrationModule() {
			@Override
			public void configure(CompressionAlgorithmBinder binder) {
				binder.bind(TEST_ALGORITHM).toInstance(compressor);
			}
		});
		assertEquals(compressor, registry.get(TEST_ALGORITHM));
		control.verify();
	}

	public void testInstantiatesCompressorUsingDefaultConstructor()
			throws Exception {
		control.replay();
		registry.registerFromModule(new CompressionAlgorithmRegistrationModule() {
			@Override
			public void configure(CompressionAlgorithmBinder binder) {
				binder.bind(TEST_ALGORITHM).to(MockCompressor.class);
			}
		});
		CompressionAlgorithm returnedCompressor = registry.get(TEST_ALGORITHM);
		assertTrue(returnedCompressor instanceof MockCompressor);
		assertTrue(((MockCompressor) returnedCompressor).usedDefault);
		control.verify();
	}

	public void testReadsConfigFromPropertiesFile() throws Exception {
		ByteArrayInputStream propertiesStream = new ByteArrayInputStream(
				(TEST_ALGORITHM + "=" + MockCompressor.class.getName() + "\n")
						.getBytes());
		registry.registerFromProperties(propertiesStream);
		control.replay();
		CompressionAlgorithm returnedCompressor = registry.get(TEST_ALGORITHM);
		assertTrue(returnedCompressor instanceof MockCompressor);
		assertTrue(((MockCompressor) returnedCompressor).usedDefault);
		control.verify();
	}

	public void testReadsConfigFromXmlFile() throws Exception {
		ByteArrayInputStream xmlStream = new ByteArrayInputStream(
				String.format(
						"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
								+ "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n"
								+ "<properties>\n"
								+ "<entry key=\"%s\">%s</entry>\n"
								+ "</properties>\n", TEST_ALGORITHM,
						MockCompressor.class.getName()).getBytes());
		registry.registerFromXml(xmlStream);
		control.replay();
		CompressionAlgorithm returnedCompressor = registry.get(TEST_ALGORITHM);
		assertTrue(returnedCompressor instanceof MockCompressor);
		assertTrue(((MockCompressor) returnedCompressor).usedDefault);
		control.verify();
	}

	private static class MockCompressor implements CompressionAlgorithm {
		public boolean usedDefault = false;

		@SuppressWarnings("unused")
		public MockCompressor(int foo) {
			usedDefault = false;
		}

		@SuppressWarnings("unused")
		public MockCompressor() {
			usedDefault = true;
		}

		@Override
		public void compress(InputStream input, Encoder outputEncoder)
				throws IOException {
		}

		@Override
		public void decompress(Decoder inputDecoder, OutputStream output)
				throws IOException {
		}
	}
}
