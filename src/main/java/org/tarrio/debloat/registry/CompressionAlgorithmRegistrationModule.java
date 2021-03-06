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
