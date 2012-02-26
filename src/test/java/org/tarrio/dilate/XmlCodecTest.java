package org.tarrio.dilate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.tarrio.dilate.Codec.Decoder;
import org.tarrio.dilate.Codec.Encoder;

import junit.framework.TestCase;

public class XmlCodecTest extends TestCase {

	private static final Symbol[] SYMBOLS = new Symbol[] {
			new Symbol((byte) 'a'), new Symbol((byte) 'b'),
			new Symbol((byte) 'c'), new Symbol((byte) 'd'),
			new Symbol((byte) 'e'), new Symbol(4, 3), new Symbol((byte) 'f'),
			new Symbol((byte) 'g'), new Symbol((byte) 'h'),
			new Symbol((byte) 'i'), new Symbol((byte) 'j') };

	private static final String COMPRESSED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<compressedData>\n"
			+ "  <byte value=\"97\"/>\n  <byte value=\"98\"/>\n"
			+ "  <byte value=\"99\"/>\n  <byte value=\"100\"/>\n"
			+ "  <byte value=\"101\"/>\n"
			+ "  <reference distance=\"4\" length=\"3\"/>\n"
			+ "  <byte value=\"102\"/>\n  <byte value=\"103\"/>\n"
			+ "  <byte value=\"104\"/>\n  <byte value=\"105\"/>\n"
			+ "  <byte value=\"106\"/>\n</compressedData>\n";
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
		for (Symbol symbol : SYMBOLS) {
			encoder.write(symbol);
		}
		encoder.close();

		assertEquals(COMPRESSED_XML, output.toString());
	}

	public void testDecodeSingleBytes() throws Exception {
		Decoder decoder = codec.getDecoder(new ByteArrayInputStream(
				COMPRESSED_XML.getBytes()));
		Symbol[] syms = new Symbol[SYMBOLS.length];
		for (int i = 0; i < syms.length; ++i) {
			assertEquals(SYMBOLS[i], decoder.read());
		}
		assertEquals(null, decoder.read());
	}

	public void testRoundtripCompressedData() throws Exception {
		Decoder decoder = codec.getDecoder(new ByteArrayInputStream(
				COMPRESSED_XML.getBytes()));
		Symbol sym = decoder.read();
		while (sym != null) {
			encoder.write(sym);
			sym = decoder.read();
		}
		encoder.close();

		assertEquals(COMPRESSED_XML, output.toString());
	}

}
