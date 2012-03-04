/**
 * Copyright 2012 Jacobo Tarrio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tarrio.debloat.registry;

import org.tarrio.debloat.CompressionAlgorithm;

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
