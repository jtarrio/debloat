package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Compressor {

	public abstract void compress(InputStream input, OutputStream output)
			throws IOException;

	public abstract void decompress(InputStream input, OutputStream output)
			throws IOException;

}