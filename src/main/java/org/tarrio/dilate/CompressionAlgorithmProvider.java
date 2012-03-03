package org.tarrio.dilate;

/**
 * An interface for classes that return compression algorithm instances.
 */
public interface CompressionAlgorithmProvider {
	public CompressionAlgorithm get();
}