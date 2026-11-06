/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/* IMB-P2P linear congruential generator used by the random benchmarks. */
class RandomBenchTest
{
	private static final class StubRandom extends RandomBench
	{
		protected void Benchrun(int size, int iters)
		{
		}

		protected double totalMessages(int iters)
		{
			return 0.0;
		}

		public void nprocess(int value)
		{
			this.nprocess = value;
		}
	}

	@Test
	void nextRandomMatchesTheImbP2PSequence()
	{
		StubRandom bench = new StubRandom();

		bench.resetRandom();

		assertEquals(1184965489L, bench.nextRandom());
		assertEquals(1778132755L, bench.nextRandom());
		assertEquals(2078363206L, bench.nextRandom());
		assertEquals(1878360418L, bench.nextRandom());
		assertEquals(4214996071L, bench.nextRandom());
		assertEquals(2422788310L, bench.nextRandom());
	}

	@Test
	void resetRandomRestartsTheSequence()
	{
		StubRandom bench = new StubRandom();

		bench.resetRandom();
		long first = bench.nextRandom();
		bench.nextRandom();
		bench.resetRandom();

		assertEquals(first, bench.nextRandom());
	}

	@Test
	void nextRankMapsTheSequenceOntoTheProcessCount()
	{
		StubRandom bench = new StubRandom();
		bench.nprocess(4);

		bench.resetRandom();

		assertEquals(1, bench.nextRank());
		assertEquals(3, bench.nextRank());
		assertEquals(2, bench.nextRank());
		assertEquals(2, bench.nextRank());
	}
}
