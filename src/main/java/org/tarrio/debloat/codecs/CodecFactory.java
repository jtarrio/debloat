package org.tarrio.debloat.codecs;

import org.tarrio.debloat.Codec;

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
