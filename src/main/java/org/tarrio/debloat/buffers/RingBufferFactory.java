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

package org.tarrio.debloat.buffers;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A factory class to create ring buffers for reading and writing.
 * 
 * @author Jacobo Tarrio
 */
public class RingBufferFactory {

	/**
	 * Creates a ring buffer that reads its data from the given input stream.
	 * 
	 * @param inputStream
	 *            The stream to read data from.
	 * @return A ring buffer that reads the data from the stream.
	 */
	public static RingBuffer newReadBuffer(InputStream inputStream) {
		return new RingBufferImpl(inputStream);
	}

	/**
	 * Creates a ring buffer that writes its data to the given output stream.
	 * 
	 * @param outputStream
	 *            The stream to write data to.
	 * @return A ring buffer that writes the data to the stream.
	 */
	public static RingBuffer newWriteBuffer(OutputStream outputStream) {
		return new RingBufferImpl(outputStream);
	}
}
