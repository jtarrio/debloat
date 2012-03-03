package org.tarrio.debloat.registry;

import org.tarrio.debloat.CompressionAlgorithm;

/**
 * An interface for classes that return compression algorithm instances.
 */
public interface CompressionAlgorithmProvider {
	public CompressionAlgorithm get();
}