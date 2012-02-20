package org.tarrio.dilate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import junit.framework.TestCase;

public class CompressorTest extends TestCase {

	private IMocksControl control;
	private InputStream input;
	private OutputStream output;
	private Codec codec;
	private Codec.Encoder encoder;
	private Codec.BlockEncoder blockEncoder;
	private Codec.Decoder decoder;
	private Codec.BlockDecoder blockDecoder;
	private Compressor compressor;
	
	@Override
	protected void setUp() throws Exception {
		control = EasyMock.createControl();
		output = new ByteArrayOutputStream();
		codec = control.createMock(Codec.class);
		encoder = control.createMock(Codec.Encoder.class);
		blockEncoder = control.createMock(Codec.BlockEncoder.class);
		decoder = control.createMock(Codec.Decoder.class);
		blockDecoder = control.createMock(Codec.BlockDecoder.class);
		compressor = new CompressorImpl(codec);
	}
	
	public void testEncodesChunks() throws Exception {
		input = new ByteArrayInputStream("abcdebcdfghij".getBytes());

		Chunk chunk1 = new Chunk(0, 5, "abcde".getBytes());
		Chunk chunk2 = new Chunk(4, 3, "bcd".getBytes());
		Chunk chunk3 = new Chunk(0, 5, "fghij".getBytes());
		
		EasyMock.expect(codec.getEncoder(output)).andReturn(encoder);
		EasyMock.expect(encoder.newBlock()).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.addChunk(chunk1)).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.addChunk(chunk2)).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.addChunk(chunk3)).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.setLastBlock(true)).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.endBlock()).andReturn(encoder);
		encoder.close();

		control.replay();
		compressor.compress(input, output);
		control.verify();
	}

	public void testEncodesChunksOverBlockBoundaries() throws Exception {
		compressor = new CompressorImpl(codec, 4);
		input = new ByteArrayInputStream("abcdebcdefghij".getBytes());

		Chunk chunk1 = new Chunk(0, 4, "abcd".getBytes());
		Chunk chunk2 = new Chunk(0, 1, "e".getBytes());
		Chunk chunk3 = new Chunk(4, 4, "bcde".getBytes());
		Chunk chunk4 = new Chunk(0, 4, "fghi".getBytes());
		Chunk chunk5 = new Chunk(0, 1, "j".getBytes());
		
		EasyMock.expect(codec.getEncoder(output)).andReturn(encoder);
		EasyMock.expect(encoder.newBlock()).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.addChunk(EasyMock.eq(chunk1))).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.endBlock()).andReturn(encoder);
		EasyMock.expect(encoder.newBlock()).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.addChunk(EasyMock.eq(chunk2))).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.addChunk(EasyMock.eq(chunk3))).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.endBlock()).andReturn(encoder);
		EasyMock.expect(encoder.newBlock()).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.addChunk(EasyMock.eq(chunk4))).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.endBlock()).andReturn(encoder);
		EasyMock.expect(encoder.newBlock()).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.addChunk(EasyMock.eq(chunk5))).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.setLastBlock(true)).andReturn(blockEncoder);
		EasyMock.expect(blockEncoder.endBlock()).andReturn(encoder);
		encoder.close();

		control.replay();
		compressor.compress(input, output);
		control.verify();
	}
	
	public void testDecodesChunks() throws Exception {
		Chunk chunk1 = new Chunk(0, 5, "abcde".getBytes());
		Chunk chunk2 = new Chunk(4, 3, "bcd".getBytes());
		Chunk chunk3 = new Chunk(0, 5, "fghij".getBytes());

		EasyMock.expect(codec.getDecoder(input)).andReturn(decoder);
		EasyMock.expect(decoder.nextBlock()).andReturn(blockDecoder);
		EasyMock.expect(blockDecoder.getNextChunk()).andReturn(chunk1);
		EasyMock.expect(blockDecoder.getNextChunk()).andReturn(chunk2);
		EasyMock.expect(blockDecoder.getNextChunk()).andReturn(chunk3);
		EasyMock.expect(blockDecoder.getNextChunk()).andReturn(null);
		EasyMock.expect(blockDecoder.isLastBlock()).andReturn(true);
		
		control.replay();
		compressor.decompress(input, output);
		control.verify();
		assertEquals("abcdebcdfghij", output.toString());
	}

	public void testDecodesChunksOverBlockBoundaries() throws Exception {
		compressor = new CompressorImpl(codec, 4);
		Chunk chunk1 = new Chunk(0, 5, "abcde".getBytes());
		Chunk chunk2 = new Chunk(4, 2, "bc".getBytes());
		Chunk chunk3 = new Chunk(0, 3, "fghi".getBytes());
		Chunk chunk4 = new Chunk(0, 1, "j".getBytes());

		EasyMock.expect(codec.getDecoder(input)).andReturn(decoder);
		EasyMock.expect(decoder.nextBlock()).andReturn(blockDecoder);
		EasyMock.expect(blockDecoder.getNextChunk()).andReturn(chunk1);
		EasyMock.expect(blockDecoder.getNextChunk()).andReturn(chunk2);
		EasyMock.expect(blockDecoder.getNextChunk()).andReturn(null);
		EasyMock.expect(blockDecoder.isLastBlock()).andReturn(false);
		EasyMock.expect(decoder.nextBlock()).andReturn(blockDecoder);
		EasyMock.expect(blockDecoder.getNextChunk()).andReturn(chunk3);
		EasyMock.expect(blockDecoder.getNextChunk()).andReturn(null);
		EasyMock.expect(blockDecoder.isLastBlock()).andReturn(false);
		EasyMock.expect(decoder.nextBlock()).andReturn(blockDecoder);
		EasyMock.expect(blockDecoder.getNextChunk()).andReturn(chunk4);
		EasyMock.expect(blockDecoder.getNextChunk()).andReturn(null);
		EasyMock.expect(blockDecoder.isLastBlock()).andReturn(true);
		
		control.replay();
		compressor.decompress(input, output);
		control.verify();
		assertEquals("abcdebcfghij", output.toString());
	}

}
