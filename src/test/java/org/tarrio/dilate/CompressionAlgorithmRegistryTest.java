package org.tarrio.dilate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.tarrio.dilate.CompressionAlgorithmRegistry.CompressionAlgorithmProvider;
import org.tarrio.dilate.Codec.Decoder;
import org.tarrio.dilate.Codec.Encoder;

import junit.framework.TestCase;

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
		provider = control
				.createMock(CompressionAlgorithmRegistry.CompressionAlgorithmProvider.class);
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
		registry.register(TEST_ALGORITHM, provider);
		assertEquals(compressor, registry.get(TEST_ALGORITHM));
		control.verify();
	}

	public void testProvidesRegisteredInstance() throws Exception {
		control.replay();
		registry.register(TEST_ALGORITHM, compressor);
		assertEquals(compressor, registry.get(TEST_ALGORITHM));
		control.verify();
	}

	public void testInstantiatesCompressorUsingDefaultConstructor()
			throws Exception {
		control.replay();
		registry.register(TEST_ALGORITHM, MockCompressor.class);
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