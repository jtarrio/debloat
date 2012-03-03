package org.tarrio.debloat;

/**
 * Returns a codec instance.
 * 
 * @author Jacobo Tarrio
 */
public class CodecFactory {

	/**
	 * Avoid subclassing and instantiation.
	 */
	private CodecFactory() {
	}

	/**
	 * Returns a codec instance.
	 */
	public static Codec getCodec() {
		return new XmlCodec();
	}
}
