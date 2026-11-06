/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import MB_MPJ.MicroBench;

/* IMB-P2P defaults and cache-line touch helpers of P2PBench. */
class P2PBenchDefaultsTest
{
	/* Widens the capabilities to public so the test can assert on them. */
	private static final class StubP2P extends P2PBench
	{
		protected void Benchrun(int size, int iters)
		{
		}

		public int minRanks()
		{
			return super.minRanks();
		}

		public boolean requiresEvenRanks()
		{
			return super.requiresEvenRanks();
		}

		public boolean validRankCount(int nprocess)
		{
			return super.validRankCount(nprocess);
		}

		public boolean supportsMpi1Options()
		{
			return super.supportsMpi1Options();
		}

		public boolean usesTimeLimit()
		{
			return super.usesTimeLimit();
		}

		public boolean supportsTouchOptions()
		{
			return super.supportsTouchOptions();
		}

		public boolean warmUpEachSize()
		{
			return super.warmUpEachSize();
		}

		public boolean customSamplePreparation()
		{
			return super.customSamplePreparation();
		}

		public int defaultIterations()
		{
			return super.defaultIterations();
		}

		public int defaultOverallVolumeMiB()
		{
			return super.defaultOverallVolumeMiB();
		}

		public int warmupIterations(int iters)
		{
			return super.warmupIterations(iters);
		}

		public double messagesPerIteration()
		{
			return super.messagesPerIteration();
		}

		public boolean msgwr()
		{
			return MSGWR;
		}

		public void msgwr(boolean value)
		{
			MSGWR = value;
		}

		public boolean msgrd()
		{
			return MSGRD;
		}

		public void msgrd(boolean value)
		{
			MSGRD = value;
		}
	}

	private boolean savedMsgwr;
	private boolean savedMsgrd;
	private final StubP2P bench = new StubP2P();

	@BeforeEach
	void saveState()
	{
		this.savedMsgwr = this.bench.msgwr();
		this.savedMsgrd = this.bench.msgrd();
	}

	@AfterEach
	void restoreState()
	{
		this.bench.msgwr(this.savedMsgwr);
		this.bench.msgrd(this.savedMsgrd);
	}

	@Test
	void p2pUsesItsOwnIterationDefaults()
	{
		assertEquals(100000, this.bench.defaultIterations());
		assertEquals(800, this.bench.defaultOverallVolumeMiB());
	}

	@Test
	void p2pRequiresTwoRanksAndAllowsEveryCount()
	{
		assertEquals(2, this.bench.minRanks());
		assertFalse(this.bench.requiresEvenRanks());
		assertTrue(this.bench.validRankCount(3));
	}

	@Test
	void p2pDisablesMpi1OptionsAndEnablesTouchOptions()
	{
		assertFalse(this.bench.supportsMpi1Options());
		assertFalse(this.bench.usesTimeLimit());
		assertTrue(this.bench.supportsTouchOptions());
		assertTrue(this.bench.warmUpEachSize());
		assertTrue(this.bench.customSamplePreparation());
	}

	@Test
	void p2pWarmupUsesTenPercentWithMinimumOfOne()
	{
		assertEquals(100, this.bench.warmupIterations(1000));
		assertEquals(1, this.bench.warmupIterations(10));
		assertEquals(1, this.bench.warmupIterations(5));
		assertEquals(0, this.bench.warmupIterations(1));
	}

	@Test
	void p2pReportsTwoMessageLatenciesPerIteration()
	{
		assertEquals(2.0, this.bench.messagesPerIteration(), 0.0);
	}

	@Test
	void touchSendBufferWritesOneBytePerCacheLine()
	{
		this.bench.msgwr(true);
		byte[] buffer = new byte[130];

		this.bench.touchSendBuffer(buffer, buffer.length);

		assertEquals(0, buffer[0]);
		assertEquals(1, buffer[64]);
		assertEquals(2, buffer[128]);
		assertEquals(0, buffer[1]);
		assertEquals(0, buffer[63]);
	}

	@Test
	void touchSendBufferIsSkippedWhenDisabled()
	{
		this.bench.msgwr(false);
		byte[] buffer = new byte[130];

		this.bench.touchSendBuffer(buffer, buffer.length);

		for(byte value : buffer)
			assertEquals(0, value);
	}

	@Test
	void touchRecvBufferReadsOneBytePerCacheLine()
	{
		this.bench.msgrd(true);
		byte[] buffer = new byte[130];

		this.bench.touchRecvBuffer(buffer, buffer.length);

		assertEquals(0, buffer[64]);
		assertEquals(0, buffer[129]);
	}

	@Test
	void touchHelpersTolerateShortBuffers()
	{
		this.bench.msgwr(true);
		this.bench.msgrd(true);
		byte[] buffer = new byte[10];

		this.bench.touchSendBuffer(buffer, buffer.length);
		this.bench.touchRecvBuffer(buffer, buffer.length);

		assertEquals(0, buffer[1]);
	}
}
