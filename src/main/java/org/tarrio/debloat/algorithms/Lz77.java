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

package org.tarrio.debloat.algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tarrio.debloat.Codec;
import org.tarrio.debloat.Symbol;
import org.tarrio.debloat.buffers.RingBuffer;
import org.tarrio.debloat.buffers.RingBufferFactory;
import org.tarrio.debloat.buffers.RingBuffer.Match;

/**
 * Implementation of LZ77.
 * 
 * @author Jacobo Tarrio
 */
public class Lz77 extends AbstractCompressionAlgorithmImpl {

	@Override
	protected String getAlgorithmName() {
		return "lz77";
	}
	
	@Override
	public void doCompress(InputStream input, Codec.Encoder encoder)
			throws IOException {
		RingBuffer buffer = RingBufferFactory.newReadBuffer(input);
		Symbol symbol = readNextSymbol(buffer);
		while (symbol != null) {
			encoder.write(symbol);
			symbol = readNextSymbol(buffer);
		}
	}

	@Override
	public void doDecompress(Codec.Decoder decoder, OutputStream output)
			throws IOException {
		RingBuffer buffer = RingBufferFactory.newWriteBuffer(output);
		Symbol symbol = decoder.read();
		while (symbol != null) {
			if (symbol instanceof Symbol.BackRef) {
				Symbol.BackRef backref = (Symbol.BackRef) symbol;
				buffer.repeatPastMatch(backref.getDistance(), backref.getLength());
			} else if (symbol instanceof Symbol.Byte) {
				buffer.write(((Symbol.Byte) symbol).getByteValue());
			} else {
				throw new IllegalStateException("Found symbol of unrecognized type " + symbol.getClass().getSimpleName());
			}
			symbol = decoder.read();
		}
	}

	/**
	 * Obtains the next symbol from the contents of a look-up buffer.
	 * 
	 * @param buffer
	 *            The look-up buffer to read from.
	 * @return The symbol that was read.
	 * @throws IOException
	 *             If there was any problem reading from the buffer.
	 */
	private Symbol readNextSymbol(RingBuffer buffer) throws IOException {
		byte[] buf = new byte[1];
		Match match = buffer.findPastMatch();
		if (match != null) {
			buffer.skip(match.getLength());
			return Symbol.newBackRef(match.getDistance(), match.getLength());
		} else {
			int read = buffer.read(buf, 1);
			return read == 1 ? Symbol.newByte(buf[0]) : null;
		}
	}
}