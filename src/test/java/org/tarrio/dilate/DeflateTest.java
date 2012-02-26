package org.tarrio.dilate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import junit.framework.TestCase;

public class DeflateTest extends TestCase {

	private static final Symbol[] SYMBOLS = new Symbol[] {
			new Symbol((byte) 'a', 0, 0), new Symbol((byte) 'b', 0, 0),
			new Symbol((byte) 'c', 0, 0), new Symbol((byte) 'd', 0, 0),
			new Symbol((byte) 'e', 0, 0), new Symbol((byte) 0, 4, 3),
			new Symbol((byte) 'f', 0, 0), new Symbol((byte) 'g', 0, 0),
			new Symbol((byte) 'h', 0, 0), new Symbol((byte) 'i', 0, 0),
			new Symbol((byte) 'j', 0, 0), };
	private IMocksControl control;
	private InputStream input;
	private OutputStream output;
	private Codec codec;
	private Codec.Encoder encoder;
	private Codec.Decoder decoder;
	private Compressor compressor;

	@Override
	protected void setUp() throws Exception {
		control = EasyMock.createControl();
		output = new ByteArrayOutputStream();
		codec = control.createMock(Codec.class);
		encoder = control.createMock(Codec.Encoder.class);
		decoder = control.createMock(Codec.Decoder.class);
		compressor = new DeflateImpl(codec);
	}

	public void testEncodesSymbols() throws Exception {
		input = new ByteArrayInputStream("abcdebcdfghij".getBytes());

		EasyMock.expect(codec.getEncoder(output)).andReturn(encoder);
		for (Symbol symbol : SYMBOLS) {
			encoder.write(symbol);
		}
		encoder.close();

		control.replay();
		compressor.compress(input, output);
		control.verify();
	}

	public void testDecodesSymbols() throws Exception {
		EasyMock.expect(codec.getDecoder(input)).andReturn(decoder);
		for (Symbol symbol : SYMBOLS) {
			EasyMock.expect(decoder.read()).andReturn(symbol);
		}
		EasyMock.expect(decoder.read()).andReturn(null);

		control.replay();
		compressor.decompress(input, output);
		control.verify();
		assertEquals("abcdebcdfghij", output.toString());
	}
}
