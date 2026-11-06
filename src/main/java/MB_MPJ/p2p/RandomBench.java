/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

/*
 * Common base for the IMB-P2P random benchmarks (Unirandom, Birandom and
 * Corandom).
 *
 * All ranks use the same 64-bit pseudo-random sequence as IMB, so every rank
 * derives the same pairs and ring without exchanging extra information. The
 * benchmarks report the IMB-P2P columns and aggregate the elapsed times and
 * the number of messages of every rank on rank 0.
 */
public abstract class RandomBench extends AggregatedBench
{
	private long randomSeed = 0;

	/* Same linear congruential generator as IMB-P2P get_next_random(). */
	protected void resetRandom()
	{
		randomSeed = 0;
	}

	protected long nextRandom()
	{
		randomSeed = randomSeed * 7560690468208412377L + 4343336064253945357L;
		return (randomSeed >>> 24) & 0xFFFFFFFFL;
	}

	protected int nextRank()
	{
		return (int)(nextRandom() % this.nprocess);
	}
}
