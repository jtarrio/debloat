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
