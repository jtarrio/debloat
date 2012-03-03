package org.tarrio.debloat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A Codec that stores the compressed data in XML.
 * 
 * @author Jacobo Tarrio
 */
class XmlCodec implements Codec {

	private static final String ROOT_TAG = "compressedData";
	private static final String ALGORITHM_ATTRIB = "algorithm";
	private static final String BYTE_TAG = "byte";
	private static final String VALUE_ATTRIB = "value";
	private static final String REFERENCE_TAG = "reference";
	private static final String DISTANCE_ATTRIB = "distance";
	private static final String LENGTH_ATTRIB = "length";
	private static final String DICTIONARY_TAG = "dictionary";
	private static final String ENTRY_ATTRIB = "entry";
	private static final String RESET_TAG = "reset";

	private static final byte[] XML_HEADER_1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			.getBytes();
	private static final String XML_HEADER_2_FORMAT = "<" + ROOT_TAG + " algorithm=\"%s\">\n";
	private static final byte[] XML_FOOTER = ("</" + ROOT_TAG + ">\n")
			.getBytes();
	private static final String BYTE_FORMAT = "  <" + BYTE_TAG + " "
			+ VALUE_ATTRIB + "=\"%d\"/>\n";
	private static final String REFERENCE_FORMAT = "  <" + REFERENCE_TAG + " "
			+ DISTANCE_ATTRIB + "=\"%d\" " + LENGTH_ATTRIB + "=\"%d\"/>\n";
	private static final String DICTIONARY_FORMAT = "  <" + DICTIONARY_TAG + " "
			+ ENTRY_ATTRIB + "=\"%d\"/>\n";
	private static final byte[] RESET = ("  <" + RESET_TAG + "/>\n").getBytes();

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
		private boolean wroteHeader;

		public EncoderImpl(OutputStream output) throws IOException {
			this.output = output;
			this.wroteHeader = false;
			output.write(XML_HEADER_1);
		}
		
		@Override
		public void setAlgorithm(String algorithm) throws IOException {
			output.write(String.format(XML_HEADER_2_FORMAT, algorithm).getBytes());
			this.wroteHeader = true;
		}
		
		@Override
		public void close() throws IOException {
			checkWroteHeader();
			output.write(XML_FOOTER);
		}

		@Override
		public void write(Symbol symbol) throws IOException {
			checkWroteHeader();
			if (symbol instanceof Symbol.Byte) {
				Symbol.Byte theByte = (Symbol.Byte) symbol; 
				output.write(String.format(BYTE_FORMAT, theByte.getByteValue() & 0xff)
						.getBytes());
			} else if (symbol instanceof Symbol.BackRef) {
				Symbol.BackRef backRef = (Symbol.BackRef) symbol;
				output.write(String.format(REFERENCE_FORMAT,
						backRef.getDistance(), backRef.getLength()).getBytes());
			} else if (symbol instanceof Symbol.DictionaryRef){
				Symbol.DictionaryRef dicRef = (Symbol.DictionaryRef) symbol;
				output.write(String.format(DICTIONARY_FORMAT,
						dicRef.getEntry()).getBytes());
			} else if (symbol instanceof Symbol.Reset) {
				output.write(RESET);
			} else {
				throw new IllegalStateException("Cannot write symbol of unknown type " + symbol.getClass().getSimpleName());
			}
		}

		private void checkWroteHeader() {
			if (!wroteHeader) {
				throw new IllegalStateException("Must set the algorithm's name before encoding data");
			}
		}
	}

	/**
	 * A class to decode compressed data stored in XML documents.
	 */
	private class DecoderImpl implements Decoder {
		private String algorithm;
		private NodeList symbols;
		private int currentSymbol;

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
				algorithm = root.getAttribute(ALGORITHM_ATTRIB);
				symbols = root.getElementsByTagName("*");
				currentSymbol = 0;
			} catch (ParserConfigurationException e) {
				throw new IOException(e);
			} catch (SAXException e) {
				throw new IOException(e);
			}
		}

		@Override
		public String getAlgoritm() throws IOException {
			return algorithm;
		}
		
		@Override
		public Symbol read() throws IOException {
			if (currentSymbol == symbols.getLength()) {
				return null;
			}
			Element symbol = (Element) symbols.item(currentSymbol++);
			String tagName = symbol.getTagName();
			if (BYTE_TAG.equals(tagName)) {
				return Symbol.newByte((byte) getNumericAttrib(symbol, VALUE_ATTRIB));
			} else if (REFERENCE_TAG.equals(tagName)) {
				return Symbol.newBackRef(getNumericAttrib(symbol, DISTANCE_ATTRIB),
						getNumericAttrib(symbol, LENGTH_ATTRIB));
			} else if (DICTIONARY_TAG.equals(tagName)) {
				return Symbol.newDictionaryRef(getNumericAttrib(symbol, ENTRY_ATTRIB));
			} else if (RESET_TAG.equals(tagName)) {
				return Symbol.newReset();
			} else {
				throw new IOException(String.format("Unexpected tag '%s'",
						tagName));
			}
		}

		/**
		 * Interprets an attribute as an integer value.
		 * 
		 * @param element
		 *            The element that contains the attribute.
		 * @param attrName
		 *            The attribute's name.
		 * @return The attribute's integer value.
		 * @throws IOException
		 *             If the attribute was not defined or there was a problem
		 *             parsing its value.
		 */
		private int getNumericAttrib(Element element, String attrName)
				throws IOException {
			if (!element.hasAttribute(attrName)) {
				throw new IOException(String.format("Expected '%s' attribute",
						attrName));
			}
			try {
				return Integer.valueOf(element.getAttribute(attrName));
			} catch (NumberFormatException e) {
				throw new IOException(String.format(
						"Invalid value for '%s' attribute", attrName), e);
			}
		}

	}
}
