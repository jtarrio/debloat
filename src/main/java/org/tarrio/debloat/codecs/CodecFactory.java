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

package org.tarrio.debloat.codecs;

import org.tarrio.debloat.Codec;

/**
 * Returns a codec instance.
 * 
 * @author Jacobo Tarrio
 */
public class CodecFactory {

	/**
	 * Avoid subclassing and instantiation.
	 */
	private CodecFactory() {
	}

	/**
	 * Returns a codec instance.
	 */
	public static Codec getCodec() {
		return new XmlCodec();
	}
}
