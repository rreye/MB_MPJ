/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/* Size sweep, iteration count and warmup rules of MicroBench. */
class MicroBenchSizingTest
{
	private static final class StubBench extends MicroBench
	{
		protected void Benchrun(int size, int iters)
		{
		}
	}

	private int savedIters;
	private int savedOverallVol;
	private int savedMinSize;
	private int savedMaxSize;
	private int[] savedMessageSizes;
	private final StubBench bench = new StubBench();

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

	@Test
	void iterationCountKeepsIterationsForSmallMessages()
	{
		MicroBench.ITERS = 1000;
		MicroBench.OVERALL_VOL = 40 * MicroBench.MEGA;

		assertEquals(1000, MicroBench.iterationCount(1));
	}

	@Test
	void iterationCountIsLimitedByOverallVolume()
	{
		MicroBench.ITERS = 1000;
		MicroBench.OVERALL_VOL = 40 * MicroBench.MEGA;

		assertEquals(10, MicroBench.iterationCount(4 * MicroBench.MEGA));
	}

	@Test
	void iterationCountIsAtLeastOne()
	{
		MicroBench.ITERS = 1000;
		MicroBench.OVERALL_VOL = 40 * MicroBench.MEGA;

		assertEquals(1, MicroBench.iterationCount(64 * MicroBench.MEGA));
	}

	@Test
	void iterationCountForZeroSizeUsesIterations()
	{
		MicroBench.ITERS = 123;
		MicroBench.OVERALL_VOL = 40 * MicroBench.MEGA;

		assertEquals(123, MicroBench.iterationCount(0));
	}

	@Test
	void messageSizesArePowersOfTwoWithinBounds()
	{
		MicroBench.MESSAGE_SIZES = null;
		MicroBench.MIN_SIZE = 1;
		MicroBench.MAX_SIZE = 8;

		assertArrayEquals(new int[] {1, 2, 4, 8}, MicroBench.messageSizes());
	}

	@Test
	void messageSizesUseExplicitListWhenProvided()
	{
		MicroBench.MESSAGE_SIZES = new int[] {3, 7};

		assertArrayEquals(new int[] {3, 7}, MicroBench.messageSizes());
	}

	@Test
	void defaultMessageSweepEndsAtFourMiB()
	{
		MicroBench.MESSAGE_SIZES = null;
		MicroBench.MIN_SIZE = 1;
		MicroBench.MAX_SIZE = 4 * MicroBench.MEGA;

		int[] sizes = MicroBench.messageSizes();

		assertEquals(23, sizes.length);
		assertEquals(1, sizes[0]);
		assertEquals(4 * MicroBench.MEGA, sizes[sizes.length - 1]);
	}

	@Test
	void mpi1WarmupUsesTenPercentWithMinimumOfTen()
	{
		assertEquals(100, bench.warmupIterations(1000));
		assertEquals(10, bench.warmupIterations(50));
		assertEquals(5, bench.warmupIterations(5));
	}
}
