package org.tarrio.debloat.algorithms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.tarrio.debloat.Codec;
import org.tarrio.debloat.CompressionAlgorithm;
import org.tarrio.debloat.Symbol;
import org.tarrio.debloat.algorithms.Lz77;

import junit.framework.TestCase;

public class Lz77Test extends TestCase {

	private static final Symbol[] SYMBOLS = new Symbol[] {
			Symbol.newByte((byte) 'a'), Symbol.newByte((byte) 'b'),
			Symbol.newByte((byte) 'c'), Symbol.newByte((byte) 'd'),
			Symbol.newByte((byte) 'e'), Symbol.newBackRef(4, 3),
			Symbol.newByte((byte) 'f'), Symbol.newByte((byte) 'g'),
			Symbol.newByte((byte) 'h'), Symbol.newByte((byte) 'i'),
			Symbol.newByte((byte) 'j') };

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
		encoder.setAlgorithm(Lz77.NAME);
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
		EasyMock.expect(decoder.getAlgoritm()).andReturn(Lz77.NAME);
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
