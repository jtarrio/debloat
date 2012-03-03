package org.tarrio.debloat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.tarrio.debloat.Symbol;
import org.tarrio.debloat.XmlCodec;
import org.tarrio.debloat.Codec.Decoder;
import org.tarrio.debloat.Codec.Encoder;

import junit.framework.TestCase;

public class XmlCodecTest extends TestCase {

	private static final String ALGORITHM = "testAlgo";

	private static final Symbol[] SYMBOLS = new Symbol[] { Symbol.newByte('a'),
			Symbol.newBackRef(4, 3), Symbol.newDictionaryRef(567),
			Symbol.newReset() };

	private static final String COMPRESSED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<compressedData algorithm=\"testAlgo\">\n"
			+ "  <byte value=\"97\"/>\n"
			+ "  <reference distance=\"4\" length=\"3\"/>\n"
			+ "  <dictionary entry=\"567\"/>\n"
			+ "  <reset/>\n"
			+ "</compressedData>\n";
	private XmlCodec codec;
	private ByteArrayOutputStream output;
	private Encoder encoder;

	@Override
	protected void setUp() throws Exception {
		codec = new XmlCodec();
		output = new ByteArrayOutputStream();
		encoder = codec.getEncoder(output);
	}

	public void testEncodeSingleBytes() throws Exception {
		encoder.setAlgorithm(ALGORITHM);
		for (Symbol symbol : SYMBOLS) {
			encoder.write(symbol);
		}
		encoder.close();

		assertEquals(COMPRESSED_XML, output.toString());
	}

	public void testDecodeSingleBytes() throws Exception {
		Decoder decoder = codec.getDecoder(new ByteArrayInputStream(
				COMPRESSED_XML.getBytes()));
		assertEquals(ALGORITHM, decoder.getAlgoritm());
		Symbol[] syms = new Symbol[SYMBOLS.length];
		for (int i = 0; i < syms.length; ++i) {
			assertEquals(SYMBOLS[i], decoder.read());
		}
		assertEquals(null, decoder.read());
	}

	public void testRoundtripCompressedData() throws Exception {
		Decoder decoder = codec.getDecoder(new ByteArrayInputStream(
				COMPRESSED_XML.getBytes()));
		encoder.setAlgorithm(decoder.getAlgoritm());
		Symbol sym = decoder.read();
		while (sym != null) {
			encoder.write(sym);
			sym = decoder.read();
		}
		encoder.close();

		assertEquals(COMPRESSED_XML, output.toString());
	}

}
