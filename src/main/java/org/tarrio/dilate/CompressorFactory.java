package org.tarrio.dilate;

/**
 * A Factory that allows applications to obtain a compressor.
 * 
 * @author Jacobo
 */
public final class CompressorFactory {

	private CompressorFactory() {
	}

	/**
	 * Returns a new instance of a {@link Compressor}.
	 */
	public static Compressor newInstance() {
		return new DeflateImpl(new XmlCodec());
	}
}
