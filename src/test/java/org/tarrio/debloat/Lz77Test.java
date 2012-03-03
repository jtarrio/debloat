package org.tarrio.debloat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.tarrio.debloat.Codec;
import org.tarrio.debloat.CompressionAlgorithm;
import org.tarrio.debloat.Lz77;
import org.tarrio.debloat.Symbol;

import junit.framework.TestCase;

public class Lz77Test extends TestCase {

	private static final Symbol[] SYMBOLS = new Symbol[] {
			Symbol.newByte('a'), Symbol.newByte('b'),
			Symbol.newByte('c'), Symbol.newByte('d'),
			Symbol.newByte('e'), Symbol.newBackRef(4, 3), Symbol.newByte('f'),
			Symbol.newByte('g'), Symbol.newByte('h'),
			Symbol.newByte('i'), Symbol.newByte('j') };

	private IMocksControl control;
	private InputStream input;
	private OutputStream output;
	private Codec codec;
	private Codec.Encoder encoder;
	private Codec.Decoder decoder;
	private CompressionAlgorithm compressor;

	@Override
	protected void setUp() throws Exception {
		control = EasyMock.createControl();
		output = new ByteArrayOutputStream();
		codec = control.createMock(Codec.class);
		encoder = control.createMock(Codec.Encoder.class);
		decoder = control.createMock(Codec.Decoder.class);
		compressor = new Lz77();
	}

	public void testEncodesSymbols() throws Exception {
		input = new ByteArrayInputStream("abcdebcdfghij".getBytes());

		EasyMock.expect(codec.getEncoder(output)).andReturn(encoder);
		encoder.setAlgorithm(Lz77.ALGORITHM);
		for (Symbol symbol : SYMBOLS) {
			encoder.write(symbol);
		}
		encoder.close();

		control.replay();
		compressor.compress(input, codec.getEncoder(output));
		control.verify();
	}

	public void testDecodesSymbols() throws Exception {
		EasyMock.expect(codec.getDecoder(input)).andReturn(decoder);
		EasyMock.expect(decoder.getAlgoritm()).andReturn(Lz77.ALGORITHM);
		for (Symbol symbol : SYMBOLS) {
			EasyMock.expect(decoder.read()).andReturn(symbol);
		}
		EasyMock.expect(decoder.read()).andReturn(null);

		control.replay();
		compressor.decompress(codec.getDecoder(input), output);
		control.verify();
		assertEquals("abcdebcdfghij", output.toString());
	}
}
