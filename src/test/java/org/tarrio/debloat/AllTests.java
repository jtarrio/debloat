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

package org.tarrio.debloat;

import org.tarrio.debloat.algorithms.Lz77Test;
import org.tarrio.debloat.algorithms.LzwTest;
import org.tarrio.debloat.buffers.RingBufferImplTest;
import org.tarrio.debloat.codecs.XmlCodecTest;
import org.tarrio.debloat.registry.CompressionAlgorithmRegistryTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * General test suite.
 * 
 * @author Jacobo Tarrio
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTestSuite(CompressionAlgorithmRegistryTest.class);
		suite.addTestSuite(EndToEndTest.class);
		suite.addTestSuite(Lz77Test.class);
		suite.addTestSuite(LzwTest.class);
		suite.addTestSuite(RingBufferImplTest.class);
		suite.addTestSuite(XmlCodecTest.class);
		// $JUnit-END$
		return suite;
	}

}
