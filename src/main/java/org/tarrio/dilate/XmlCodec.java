package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A Codec that stores the compressed data in XML.
 * 
 * @author Jacobo
 */
class XmlCodec implements Codec {

	private static final String ROOT_TAG = "compressedData";
	private static final String BLOCK_TAG = "block";
	private static final String LAST_ATTRIB = "last";
	private static final String BYTE_TAG = "byte";
	private static final String VALUE_ATTRIB = "value";
	private static final String REFERENCE_TAG = "reference";
	private static final String DISTANCE_ATTRIB = "distance";
	private static final String LENGTH_ATTRIB = "length";

	private static final byte[] XML_HEADER_1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			.getBytes();
	private static final byte[] XML_HEADER_2 = ("<" + ROOT_TAG + ">\n")
			.getBytes();
	private static final byte[] XML_FOOTER = ("</" + ROOT_TAG + ">\n")
			.getBytes();
	private static final byte[] BLOCK_HEADER = ("  <" + BLOCK_TAG + ">\n")
			.getBytes();
	private static final byte[] BLOCK_FOOTER = ("  </" + BLOCK_TAG + ">\n")
			.getBytes();
	private static final byte[] LAST_BLOCK_HEADER = ("  <" + BLOCK_TAG + " "
			+ LAST_ATTRIB + "=\"true\">\n").getBytes();
	private static final String BYTE_FORMAT = "    <" + BYTE_TAG + " "
			+ VALUE_ATTRIB + "=\"%d\"/>\n";
	private static final String REFERENCE_FORMAT = "    <" + REFERENCE_TAG
			+ " " + DISTANCE_ATTRIB + "=\"%d\" " + LENGTH_ATTRIB
			+ "=\"%d\"/>\n";

	@Override
	public Encoder getEncoder(OutputStream output) throws IOException {
		return new EncoderImpl(output);
	}

	@Override
	public Decoder getDecoder(InputStream input) throws IOException {
		return new DecoderImpl(input);
	}

	/**
	 * A class to encode compressed data into XML documents.
	 */
	private class EncoderImpl implements Encoder {

		private final OutputStream output;

		public EncoderImpl(OutputStream output) throws IOException {
			this.output = output;
			output.write(XML_HEADER_1);
			output.write(XML_HEADER_2);
		}

		@Override
		public BlockEncoder newBlock() {
			return new BlockEncoderImpl();
		}

		@Override
		public void close() throws IOException {
			output.write(XML_FOOTER);
		}

		/**
		 * A class to encode a compressed data block into a XML &lt;block&gt;
		 * tag.
		 */
		private class BlockEncoderImpl implements BlockEncoder {

			private boolean lastBlock = false;
			private Compression compression = null;
			private List<Chunk> chunks = new ArrayList<Chunk>();

			@Override
			public BlockEncoder setLastBlock(boolean lastBlock) {
				this.lastBlock = lastBlock;
				return this;
			}

			@Override
			public BlockEncoder compress(Compression compression) {
				this.compression = compression;
				return this;
			}

			@Override
			public BlockEncoder addChunk(Chunk chunk) {
				chunks.add(chunk);
				return this;
			}

			@Override
			public Encoder endBlock() throws IOException {
				if (lastBlock) {
					output.write(LAST_BLOCK_HEADER);
				} else {
					output.write(BLOCK_HEADER);
				}
				for (Chunk chunk : chunks) {
					if (chunk.getDistance() == 0
							|| compression == Compression.NONE) {
						for (byte singleByte : chunk.getBytes()) {
							output.write(String.format(BYTE_FORMAT, singleByte)
									.getBytes());
						}
					} else {
						output.write(String.format(REFERENCE_FORMAT,
								chunk.getDistance(), chunk.getLength())
								.getBytes());
					}
				}
				output.write(BLOCK_FOOTER);
				return EncoderImpl.this;
			}

		}
	}

	/**
	 * A class to decode compressed data stored in XML documents.
	 */
	private class DecoderImpl implements Decoder {
		private byte[] buffer;
		private int bufPos;
		private NodeList blocks;
		private int currentBlock;

		public DecoderImpl(InputStream input) throws IOException {
			try {
				DocumentBuilder db = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder();
				Element root = db.parse(input).getDocumentElement();
				if (!ROOT_TAG.equals(root.getTagName())) {
					throw new IOException(String.format(
							"XML document root is not %s but %s", ROOT_TAG,
							root.getTagName()));
				}
				blocks = root.getElementsByTagName(BLOCK_TAG);
				currentBlock = 0;
				buffer = new byte[Chunk.MAX_LENGTH * 2];
				bufPos = 0;
			} catch (ParserConfigurationException e) {
				throw new IOException(e);
			} catch (SAXException e) {
				throw new IOException(e);
			}
		}

		@Override
		public BlockDecoder nextBlock() throws IOException {
			if (currentBlock >= blocks.getLength()) {
				return null;
			}
			return new BlockDecoderImpl((Element) blocks.item(currentBlock++));
		}

		/**
		 * A class to decode a block's worth of data.
		 */
		private class BlockDecoderImpl implements BlockDecoder {
			private boolean lastBlock;
			private NodeList data;
			private int currentDatum;

			public BlockDecoderImpl(Element block) throws IOException {
				String attribute = block.getAttribute(LAST_ATTRIB);
				lastBlock = !attribute.isEmpty() && !"false".equals(attribute);
				data = block.getElementsByTagName("*");
				currentDatum = 0;
			}

			@Override
			public boolean isLastBlock() {
				return lastBlock;
			}

			@Override
			public Chunk getNextChunk() throws IOException {
				if (currentDatum >= data.getLength()) {
					return null;
				}
				Element datum = (Element) data.item(currentDatum++);
				if (BYTE_TAG.equals(datum.getTagName())) {
					return decodeBytes(datum);
				} else if (REFERENCE_TAG.equals(datum.getTagName())) {
					return decodeReference(datum);
				} else {
					throw new IOException(String.format(
							"Expected %s or %s, found %s", BYTE_TAG,
							REFERENCE_TAG, datum.getTagName()));
				}
			}

			/**
			 * Decodes the next few contiguous stream of &lt;byte&gt; elements
			 * into a single chunk.
			 * 
			 * @param datum
			 *            The first &lt;byte&gt; element in the stream.
			 * @return A chunk representing the decoded bytes.
			 * @throws IOException
			 *             If there was any problem decoding the data.
			 */
			private Chunk decodeBytes(Element datum) throws IOException {
				int read = 0;
				while (datum != null && BYTE_TAG.equals(datum.getTagName())
						&& read < Chunk.MAX_LENGTH) {
					checkHasAttribute(datum, VALUE_ATTRIB);
					byte theByte = Byte.valueOf(datum
							.getAttribute(VALUE_ATTRIB));
					recordByte(theByte);
					++read;
					datum = (Element) data.item(currentDatum);
					if (datum != null && BYTE_TAG.equals(datum.getTagName())) {
						currentDatum++;
					}
				}
				return new Chunk(0, Arrays.copyOfRange(buffer, bufPos
						- read, bufPos));
			}

			/**
			 * Decodes a reference into a chunk.
			 * 
			 * @param datum
			 *            The element to decode.
			 * @return A chunk representing the reference along with the bytes
			 *         it stands for.
			 * @throws IOException
			 *             If there was any problem decoding the data.
			 */
			private Chunk decodeReference(Element datum) throws IOException {
				checkHasAttribute(datum, DISTANCE_ATTRIB);
				checkHasAttribute(datum, LENGTH_ATTRIB);
				int distance = Integer.valueOf(datum
						.getAttribute(DISTANCE_ATTRIB));
				int length = Integer.valueOf(datum.getAttribute(LENGTH_ATTRIB));
				for (int i = 0; i < length; ++i) {
					recordByte(buffer[bufPos - distance]);
				}
				return new Chunk(distance, Arrays.copyOfRange(buffer,
						bufPos - distance - length, bufPos - distance));
			}

			/**
			 * Stores a byte into the lookback buffer.
			 * 
			 * @param theByte
			 *            The byte to store.
			 */
			private void recordByte(byte theByte) {
				if (bufPos == buffer.length) {
					System.arraycopy(buffer, Chunk.MAX_LENGTH, buffer, 0,
							Chunk.MAX_LENGTH);
					bufPos = Chunk.MAX_LENGTH;
				}
				buffer[bufPos++] = theByte;
			}

			/**
			 * Checks whether an element has a given attribute and throws an
			 * exception if it doesn't.
			 * 
			 * @param datum
			 *            The element to check.
			 * @param attribute
			 *            The attribute to check for.
			 * @throws IOException
			 *             If the attribute is not present in the element.
			 */
			private void checkHasAttribute(Element datum, String attribute)
					throws IOException {
				if (!datum.hasAttribute(attribute)) {
					throw new IOException(String.format(
							"Expected %s attribute", attribute));
				}
			}

		}
	}

}
