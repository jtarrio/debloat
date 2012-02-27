package org.tarrio.dilate;

public class CodecFactory {

	private CodecFactory() {
	}

	public static Codec getCodec() {
		return new XmlCodec();
	}
}
