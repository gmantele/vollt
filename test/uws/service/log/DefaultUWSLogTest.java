package uws.service.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.junit.Test;

import uws.service.log.UWSLog.LogLevel;

public class DefaultUWSLogTest {

	@Test
	public void testCanLog(){
		OutputStream output = new ByteArrayOutputStream();
		DefaultUWSLog logger = new DefaultUWSLog(output);

		// Default value = DEBUG => ALL MESSAGES CAN BE LOGGED
		assertEquals(LogLevel.DEBUG, logger.getMinLogLevel());
		for(LogLevel ll : LogLevel.values())
			assertTrue(logger.canLog(ll));

		// Test: INFO => ALL EXCEPT DEBUG CAN BE LOGGED
		logger.setMinLogLevel(LogLevel.INFO);
		assertEquals(LogLevel.INFO, logger.getMinLogLevel());
		assertFalse(logger.canLog(LogLevel.DEBUG));
		assertTrue(logger.canLog(LogLevel.INFO));
		assertTrue(logger.canLog(LogLevel.WARNING));
		assertTrue(logger.canLog(LogLevel.ERROR));
		assertTrue(logger.canLog(LogLevel.FATAL));

		// Test: WARNING => ALL EXCEPT DEBUG AND INFO CAN BE LOGGED
		logger.setMinLogLevel(LogLevel.WARNING);
		assertEquals(LogLevel.WARNING, logger.getMinLogLevel());
		assertFalse(logger.canLog(LogLevel.DEBUG));
		assertFalse(logger.canLog(LogLevel.INFO));
		assertTrue(logger.canLog(LogLevel.WARNING));
		assertTrue(logger.canLog(LogLevel.ERROR));
		assertTrue(logger.canLog(LogLevel.FATAL));

		// Test: ERROR => ONLY ERROR AND FATAL CAN BE LOGGED
		logger.setMinLogLevel(LogLevel.ERROR);
		assertEquals(LogLevel.ERROR, logger.getMinLogLevel());
		assertFalse(logger.canLog(LogLevel.DEBUG));
		assertFalse(logger.canLog(LogLevel.INFO));
		assertFalse(logger.canLog(LogLevel.WARNING));
		assertTrue(logger.canLog(LogLevel.ERROR));
		assertTrue(logger.canLog(LogLevel.FATAL));

		// Test: FATAL => ONLY FATAL CAN BE LOGGED
		logger.setMinLogLevel(LogLevel.FATAL);
		assertEquals(LogLevel.FATAL, logger.getMinLogLevel());
		assertFalse(logger.canLog(LogLevel.DEBUG));
		assertFalse(logger.canLog(LogLevel.INFO));
		assertFalse(logger.canLog(LogLevel.WARNING));
		assertFalse(logger.canLog(LogLevel.ERROR));
		assertTrue(logger.canLog(LogLevel.FATAL));
	}

}
