package org.tarrio.debloat.registry;

import org.tarrio.debloat.CompressionAlgorithm;

/**
 * Interface for programmatic configuration of the
 * {@link CompressionAlgorithmRegistry}.
 * 
 * You can use this to bind algorithm names to implementations given by the
 * class that implements the algorithm, a single instance of a
 * {@link CompressionAlgorithm} instance, or a
 * {@link CompressionAlgorithmProvider}.
 * 
 * @author Jacobo Tarrio
 */
public interface CompressionAlgorithmRegistrationModule {

	/**
	 * Configure the registry with the given binder.
	 */
	void configure(CompressionAlgorithmBinder binder);
}
