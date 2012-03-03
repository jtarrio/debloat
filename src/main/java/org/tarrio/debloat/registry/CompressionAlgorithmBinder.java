package org.tarrio.debloat.registry;

/**
 * An interface that allows to give the name of an algorithm to configure.
 * 
 * @author Jacobo Tarrio
 */
public interface CompressionAlgorithmBinder {

	/**
	 * Receives an algorithm's name and returns a class that allows to select
	 * how that algorithm is instantiated.
	 * 
	 * @param name
	 *            The name for the algorithm.
	 * @return An algorithm selector.
	 */
	CompressionAlgorithmSelector bind(String name);
}
