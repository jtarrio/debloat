package org.tarrio.dilate.registry;

import org.tarrio.dilate.CompressionAlgorithm;

/**
 * An interface that allows to select how the algorithm instance is produced.
 * 
 * @author Jacobo Tarrio
 */
public interface CompressionAlgorithmSelector {

	/**
	 * Binds the algorithm to the given class, so that new instances are created
	 * with the default constructor every time one is needed.
	 * 
	 * @param algorithmClass
	 *            The class that implements the algorithm.
	 */
	void to(Class<? extends CompressionAlgorithm> algorithmClass);

	/**
	 * Binds the algorithm to the given provider, so that the provider's get()
	 * method is called every time one is needed.
	 * 
	 * @param provider
	 *            The provider for the algorithm.
	 */
	void toProvider(CompressionAlgorithmProvider provider);

	/**
	 * Binds the algorithm to the given instance, so that the same instance is
	 * returned every time it is needed.
	 * 
	 * @param instance
	 *            The instance for the algorithm.
	 */
	void toInstance(CompressionAlgorithm instance);
}
