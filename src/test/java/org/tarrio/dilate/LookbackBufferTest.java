package org.tarrio.dilate;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

public class LookbackBufferTest extends TestCase {

	public void testRead() throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream("0123456789".getBytes());
		LookbackBuffer lookbackBuffer = new LookbackBuffer(input);
		for (int i = 0; i < 10; ++i) {
			assertFalse(lookbackBuffer.isEof());
			assertEquals('0' + i, lookbackBuffer.read());
		}
		assertTrue(lookbackBuffer.isEof());
		assertEquals(-1, lookbackBuffer.read());
	}
	
	public void testReadSmallBuffer() throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream("0123456789".getBytes());
		LookbackBuffer lookbackBuffer = new LookbackBuffer(input, 5, 2);
		for (int i = 0; i < 10; ++i) {
			assertFalse(lookbackBuffer.isEof());
			assertEquals('0' + i, lookbackBuffer.read());
		}
		assertTrue(lookbackBuffer.isEof());
		assertEquals(-1, lookbackBuffer.read());
	}
	
	public void testSkip() throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream("0123456789".getBytes());
		LookbackBuffer lookbackBuffer = new LookbackBuffer(input);
		for (int i = 0; i < 3; ++i) {
			assertFalse(lookbackBuffer.isEof());
			assertEquals('0' + i, lookbackBuffer.read());
		}
		lookbackBuffer.skip(4);
		for (int i = 7; i < 10; ++i) {
			assertFalse(lookbackBuffer.isEof());
			assertEquals('0' + i, lookbackBuffer.read());
		}
		assertTrue(lookbackBuffer.isEof());
		assertEquals(-1, lookbackBuffer.read());		
	}
	
	public void testSkipSmallBuffer() throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream("0123456789".getBytes());
		LookbackBuffer lookbackBuffer = new LookbackBuffer(input, 6, 2);
		for (int i = 0; i < 3; ++i) {
			assertFalse(lookbackBuffer.isEof());
			assertEquals('0' + i, lookbackBuffer.read());
		}
		lookbackBuffer.skip(4);
		for (int i = 7; i < 10; ++i) {
			assertFalse(lookbackBuffer.isEof());
			assertEquals('0' + i, lookbackBuffer.read());
		}
		assertTrue(lookbackBuffer.isEof());
		assertEquals(-1, lookbackBuffer.read());		
	}
	
	public void testSearchMatchNoMatches() throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream("0123456789".getBytes());
		LookbackBuffer lookbackBuffer = new LookbackBuffer(input, 3, 0);
		lookbackBuffer.skip(5);
		for (int i = 0; i < 5; ++i) {
			assertNull(lookbackBuffer.findMatch());
			lookbackBuffer.skip(1);
		}
	}
	
	public void testSearchMatchSimpleMatch() throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream("0123412389".getBytes());
		LookbackBuffer lookbackBuffer = new LookbackBuffer(input);
		lookbackBuffer.skip(5);
		LookbackBuffer.Match match = lookbackBuffer.findMatch();
		assertNotNull(match);
		assertEquals(4, match.getDistance());
		assertEquals(3, match.getLength());
	}

	public void testSearchMatchFindLongest() throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream("0123123412123489".getBytes());
		LookbackBuffer lookbackBuffer = new LookbackBuffer(input);
		lookbackBuffer.skip(10);
		LookbackBuffer.Match match = lookbackBuffer.findMatch();
		assertNotNull(match);
		assertEquals(6, match.getDistance());
		assertEquals(4, match.getLength());
	}

	public void testSearchMatchFindExtra() throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream("01231231231231234".getBytes());
		LookbackBuffer lookbackBuffer = new LookbackBuffer(input);
		lookbackBuffer.skip(4);
		LookbackBuffer.Match match = lookbackBuffer.findMatch();
		assertNotNull(match);
		assertEquals(3, match.getDistance());
		assertEquals(12, match.getLength());
		lookbackBuffer.skip(match.getLength());
		assertEquals('4', lookbackBuffer.read());
	}

	public void testSearchMatchSmallBuffer() throws Exception {
		ByteArrayInputStream input = new ByteArrayInputStream("01231231234".getBytes());
		LookbackBuffer lookbackBuffer = new LookbackBuffer(input, 12, 4);
		lookbackBuffer.skip(7);
		LookbackBuffer.Match match = lookbackBuffer.findMatch();
		assertNotNull(match);
		assertEquals(3, match.getDistance());
		assertEquals(3, match.getLength());
	}

}
