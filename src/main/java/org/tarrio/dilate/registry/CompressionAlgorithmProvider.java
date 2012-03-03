package org.tarrio.dilate.registry;

import org.tarrio.dilate.CompressionAlgorithm;

/**
 * An interface for classes that return compression algorithm instances.
 */
public interface CompressionAlgorithmProvider {
	public CompressionAlgorithm get();
}