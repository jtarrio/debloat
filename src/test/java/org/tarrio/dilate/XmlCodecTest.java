package org.tarrio.dilate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.tarrio.dilate.Codec.BlockDecoder;
import org.tarrio.dilate.Codec.BlockEncoder;
import org.tarrio.dilate.Codec.Compression;
import org.tarrio.dilate.Codec.Decoder;
import org.tarrio.dilate.Codec.Encoder;

import junit.framework.TestCase;

public class XmlCodecTest extends TestCase {

	private static final Chunk[] CHUNKS = new Chunk[] {
			new Chunk(0, 5, "abcde".getBytes()),
			new Chunk(4, 2, "bc".getBytes()),
			new Chunk(0, 3, "fgh".getBytes()), new Chunk(0, 1, "i".getBytes()),
			new Chunk(0, 1, "j".getBytes()) };

	private static final String UNCOMPRESSED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<compressedData>\n  <block>\n"
			+ "    <byte value=\"97\"/>\n    <byte value=\"98\"/>\n"
			+ "    <byte value=\"99\"/>\n    <byte value=\"100\"/>\n"
			+ "    <byte value=\"101\"/>\n    <byte value=\"98\"/>\n"
			+ "    <byte value=\"99\"/>\n    <byte value=\"102\"/>\n"
			+ "    <byte value=\"103\"/>\n    <byte value=\"104\"/>\n"
			+ "  </block>\n  <block last=\"true\">\n"
			+ "    <byte value=\"105\"/>\n    <byte value=\"106\"/>\n"
			+ "  </block>\n</compressedData>\n";
	private static final String COMPRESSED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<compressedData>\n  <block>\n"
			+ "    <byte value=\"97\"/>\n    <byte value=\"98\"/>\n"
			+ "    <byte value=\"99\"/>\n    <byte value=\"100\"/>\n"
			+ "    <byte value=\"101\"/>\n"
			+ "    <reference distance=\"4\" length=\"2\"/>\n"
			+ "    <byte value=\"102\"/>\n    <byte value=\"103\"/>\n"
			+ "    <byte value=\"104\"/>\n  </block>\n"
			+ "  <block last=\"true\">\n    <byte value=\"105\"/>\n"
			+ "    <byte value=\"106\"/>\n  </block>\n" + "</compressedData>\n";
	private static final String MORE_COMPRESSED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<compressedData>\n  <block>\n"
			+ "    <byte value=\"97\"/>\n    <byte value=\"98\"/>\n"
			+ "    <byte value=\"99\"/>\n    <byte value=\"100\"/>\n"
			+ "    <byte value=\"101\"/>\n"
			+ "    <reference distance=\"4\" length=\"20\"/>\n"
			+ "    <byte value=\"102\"/>\n    <byte value=\"103\"/>\n"
			+ "    <byte value=\"104\"/>\n  </block>\n"
			+ "  <block last=\"true\">\n    <byte value=\"105\"/>\n"
			+ "    <byte value=\"106\"/>\n  </block>\n" + "</compressedData>\n";
	private XmlCodec codec;
	private ByteArrayOutputStream output;
	private Encoder encoder;

	@Override
	protected void setUp() throws Exception {
		codec = new XmlCodec();
		output = new ByteArrayOutputStream();
		encoder = codec.getEncoder(output);
	}

	public void testEncodeWithCompression() throws Exception {
		encoder.newBlock().addChunk(CHUNKS[0]).addChunk(CHUNKS[1])
				.addChunk(CHUNKS[2]).endBlock().newBlock().addChunk(CHUNKS[3])
				.addChunk(CHUNKS[4]).setLastBlock(true).endBlock().close();

		assertEquals(COMPRESSED_XML, output.toString());
	}

	public void testEncodeForCompressionNone() throws Exception {
		encoder.newBlock().addChunk(CHUNKS[0]).addChunk(CHUNKS[1])
				.addChunk(CHUNKS[2]).compress(Compression.NONE).endBlock()
				.newBlock().addChunk(CHUNKS[3]).addChunk(CHUNKS[4])
				.setLastBlock(true).endBlock().close();

		assertEquals(UNCOMPRESSED_XML, output.toString());
	}

	public void testDecodeWithCompression() throws Exception {
		StringBuilder sb = new StringBuilder();
		Decoder decoder = codec.getDecoder(new ByteArrayInputStream(
				COMPRESSED_XML.getBytes()));
		BlockDecoder block;
		do {
			block = decoder.nextBlock();
			Chunk nextChunk = block.getNextChunk();
			while (nextChunk != null) {
				sb.append(new String(nextChunk.getBytes()));
				nextChunk = block.getNextChunk();
			}
		} while (!block.isLastBlock());

		assertEquals("abcdebcfghij", sb.toString());
	}
	
	public void testDecodeWithMoreCompression() throws Exception {
		StringBuilder sb = new StringBuilder();
		Decoder decoder = codec.getDecoder(new ByteArrayInputStream(
				MORE_COMPRESSED_XML.getBytes()));
		BlockDecoder block;
		do {
			block = decoder.nextBlock();
			Chunk nextChunk = block.getNextChunk();
			while (nextChunk != null) {
				sb.append(new String(nextChunk.getBytes()));
				nextChunk = block.getNextChunk();
			}
		} while (!block.isLastBlock());

		assertEquals("abcdebcdebcdebcdebcdebcdefghij", sb.toString());
	}
	
	public void testRoundtripCompressedData() throws Exception {
		Decoder decoder = codec.getDecoder(new ByteArrayInputStream(
				COMPRESSED_XML.getBytes()));
		BlockDecoder block;
		do {
			block = decoder.nextBlock();
			BlockEncoder encodingBlock = encoder.newBlock().setLastBlock(
					block.isLastBlock());
			Chunk nextChunk = block.getNextChunk();
			while (nextChunk != null) {
				encodingBlock.addChunk(nextChunk);
				nextChunk = block.getNextChunk();
			}
			encodingBlock.endBlock();
		} while (!block.isLastBlock());
		encoder.close();

		assertEquals(COMPRESSED_XML, output.toString());
	}

}
