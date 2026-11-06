/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/* Parsing helpers of MicroBench; no MPI runtime is needed. */
class MicroBenchOptionsTest
{
	private int savedIters;
	private int savedOverallVol;
	private int savedMinSize;
	private int savedMaxSize;
	private int[] savedMessageSizes;

	@BeforeEach
	void saveState()
	{
		savedIters = MicroBench.ITERS;
		savedOverallVol = MicroBench.OVERALL_VOL;
		savedMinSize = MicroBench.MIN_SIZE;
		savedMaxSize = MicroBench.MAX_SIZE;
		savedMessageSizes = MicroBench.MESSAGE_SIZES;
	}

	@AfterEach
	void restoreState()
	{
		MicroBench.ITERS = savedIters;
		MicroBench.OVERALL_VOL = savedOverallVol;
		MicroBench.MIN_SIZE = savedMinSize;
		MicroBench.MAX_SIZE = savedMaxSize;
		MicroBench.MESSAGE_SIZES = savedMessageSizes;
	}

	@ParameterizedTest
	@ValueSource(strings = {"on", "ON", "true", "yes", "1"})
	void parseOnOffAcceptsTrueValues(String value)
	{
		assertTrue(MicroBench.parseOnOff(value, "-flag"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"off", "OFF", "false", "no", "0"})
	void parseOnOffAcceptsFalseValues(String value)
	{
		assertFalse(MicroBench.parseOnOff(value, "-flag"));
	}

	@Test
	void parseOnOffRejectsUnknownValues()
	{
		MicroBenchException error = assertThrows(MicroBenchException.class,
			() -> MicroBench.parseOnOff("maybe", "-flag"));

		assertTrue(error.getMessage().contains("-flag"));
	}

	@Test
	void parsePositiveIntAcceptsPositiveValues()
	{
		assertEquals(42, MicroBench.parsePositiveInt("42", "-n"));
	}

	@Test
	void parsePositiveIntRejectsZeroNegativeAndText()
	{
		assertThrows(MicroBenchException.class, () -> MicroBench.parsePositiveInt("0", "-n"));
		assertThrows(MicroBenchException.class, () -> MicroBench.parsePositiveInt("-1", "-n"));
		assertThrows(MicroBenchException.class, () -> MicroBench.parsePositiveInt("abc", "-n"));
	}

	@Test
	void parsePositiveIntOrZeroAllowsZero()
	{
		assertEquals(0, MicroBench.parsePositiveIntOrZero("0", "-msglog"));
		assertEquals(22, MicroBench.parsePositiveIntOrZero("22", "-msglog"));
		assertThrows(MicroBenchException.class,
			() -> MicroBench.parsePositiveIntOrZero("-1", "-msglog"));
	}

	@Test
	void parsePositiveDoubleAcceptsPositiveValues()
	{
		assertEquals(2.5, MicroBench.parsePositiveDouble("2.5", "-time"), 0.0);
	}

	@Test
	void parsePositiveDoubleRejectsInvalidValues()
	{
		assertThrows(MicroBenchException.class, () -> MicroBench.parsePositiveDouble("0", "-time"));
		assertThrows(MicroBenchException.class, () -> MicroBench.parsePositiveDouble("-2", "-time"));
		assertThrows(MicroBenchException.class, () -> MicroBench.parsePositiveDouble("NaN", "-time"));
		assertThrows(MicroBenchException.class, () -> MicroBench.parsePositiveDouble("Infinity", "-time"));
	}

	@Test
	void parseVolumeMiBConvertsToBytes()
	{
		assertEquals(40 * MicroBench.MEGA, MicroBench.parseVolumeMiB("40", "-o"));
	}

	@Test
	void parseVolumeMiBRejectsOverflow()
	{
		assertThrows(MicroBenchException.class,
			() -> MicroBench.parseVolumeMiB("2048", "-o"));
	}

	@Test
	void parseSizeListStoresSizesAndBounds()
	{
		int[] sizes = MicroBench.parseSizeList("1,4,1024", "-msgsize");

		assertArrayEquals(new int[] {1, 4, 1024}, sizes);
		assertEquals(1, MicroBench.MIN_SIZE);
		assertEquals(1024, MicroBench.MAX_SIZE);
	}

	@Test
	void parseSizeListRejectsInvalidSizes()
	{
		assertThrows(MicroBenchException.class,
			() -> MicroBench.parseSizeList("0", "-msgsize"));
		assertThrows(MicroBenchException.class,
			() -> MicroBench.parseSizeList(String.valueOf(MicroBench.MAX_MESSAGE_SIZE * MicroBench.MEGA + 1),
				"-msgsize"));
	}

	@Test
	void parseMsgLogWithRangeUsesPowersOfTwo()
	{
		MicroBench.MESSAGE_SIZES = new int[] {7};

		MicroBench.parseMsgLog("0:10");

		assertNull(MicroBench.MESSAGE_SIZES);
		assertEquals(1, MicroBench.MIN_SIZE);
		assertEquals(1 << 10, MicroBench.MAX_SIZE);
	}

	@Test
	void parseMsgLogWithSingleValueStartsAtOneByte()
	{
		MicroBench.parseMsgLog("6");

		assertEquals(1, MicroBench.MIN_SIZE);
		assertEquals(64, MicroBench.MAX_SIZE);
	}

	@Test
	void parseMsgLogRejectsInvalidRanges()
	{
		assertThrows(MicroBenchException.class, () -> MicroBench.parseMsgLog("10:5"));
		assertThrows(MicroBenchException.class, () -> MicroBench.parseMsgLog("0:27"));
		assertThrows(MicroBenchException.class, () -> MicroBench.parseMsgLog("1:2:3"));
	}

	@Test
	void parseIterAcceptsOneTwoAndThreeFields()
	{
		MicroBench.parseIter("10");
		assertEquals(10, MicroBench.ITERS);

		MicroBench.parseIter("20,40");
		assertEquals(20, MicroBench.ITERS);
		assertEquals(40 * MicroBench.MEGA, MicroBench.OVERALL_VOL);

		MicroBench.parseIter("30,40,100");
		assertEquals(30, MicroBench.ITERS);
	}

	@Test
	void parseIterRejectsEmptyAndExtraFields()
	{
		assertThrows(MicroBenchException.class, () -> MicroBench.parseIter(""));
		assertThrows(MicroBenchException.class, () -> MicroBench.parseIter("1,2,3,4"));
	}
}
