package org.tarrio.dilate;

public final class CompressorFactory {

	private CompressorFactory() {
	}
	
	public static Compressor newInstance() {
		return new CompressorImpl(new XmlCodec());
	}
}
