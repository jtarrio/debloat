package org.tarrio.debloat;

import org.tarrio.debloat.registry.CompressionAlgorithmRegistryTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(CompressionAlgorithmRegistryTest.class);
		suite.addTestSuite(EndToEndTest.class);
		suite.addTestSuite(RingBufferTest.class);
		suite.addTestSuite(Lz77Test.class);
		suite.addTestSuite(XmlCodecTest.class);
		//$JUnit-END$
		return suite;
	}

}
