/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import java.util.Locale;

import MB_MPJ.MicroBench;

import mpi.*;

/*
 * Common base for the IMB-P2P benchmarks.
 *
 * IMB-P2P differs from IMB-MPI1 in two ways that matter here:
 *   - every process takes part in the benchmark (paired processes), so the
 *     minimum is 2 processes;
 *   - results are aggregated on rank 0 and printed as
 *     #bytes #repetitions t[usec] Mbytes/sec Msg/sec, where the bandwidth
 *     and message rate use the slowest process and the latency uses the
 *     aggregate time of all processes.
 */
public abstract class P2PBench extends MicroBench
{
	private static final int CACHE_LINE_LEN = 64;
	private static final int BARRIER_TAG = 1000;
	private final byte[] barrierBuffer = new byte[1];
	private long touchDummy = 0;

	protected boolean supportsTouchOptions()
	{
		return true;
	}

	protected boolean customSamplePreparation()
	{
		return true;
	}

	protected void samplePause()
	{
		if(PAUSE_USEC <= 0)
			return;

		try
		{
			Thread.sleep(PAUSE_USEC / 1000, (PAUSE_USEC % 1000) * 1000);
		}
		catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}

	/* IMB-P2P dissemination barrier, outside the measured interval. */
	protected void sampleBarrier() throws MPIException
	{
		for(int mask = 1; mask < this.nprocess; mask <<= 1)
		{
			int dst = (this.rank + mask) % this.nprocess;
			int src = (this.rank - mask + this.nprocess) % this.nprocess;

			MPI.COMM_WORLD.Sendrecv(barrierBuffer, 0, 0, MPI.BYTE, dst, BARRIER_TAG,
						barrierBuffer, 0, 0, MPI.BYTE, src, BARRIER_TAG);
		}
	}

	protected boolean supportsMpi1Options()
	{
		return false;
	}

	protected int minRanks()
	{
		return 2;
	}

	/* IMB-P2P touch helpers: write/read one byte per cache line. */
	protected void touchSendBuffer(byte[] buffer, int size)
	{
		if(!MSGWR)
			return;

		for(int i = 0; i < size; i += CACHE_LINE_LEN)
			buffer[i] = (byte)touchDummy++;
	}

	protected void touchRecvBuffer(byte[] buffer, int size)
	{
		if(!MSGRD)
			return;

		for(int i = 0; i < size; i += CACHE_LINE_LEN)
			touchDummy += buffer[i];
	}

	/* IMB-P2P defaults: -iter 100000,800 (800 MiB overall volume) */
	protected int defaultIterations()
	{
		return 100000;
	}

	protected int defaultOverallVolumeMiB()
	{
		return 800;
	}

	/* IMB-P2P warms up separately for every message size. */
	protected boolean warmUpEachSize()
	{
		return true;
	}

	protected int warmupIterations(int iters)
	{
		int warmup = iters / WARMUP_PERCENT;

		if(warmup == 0 && iters > 1)
			warmup = 1;

		return warmup;
	}

	protected boolean compactHeader()
	{
		return true;
	}

	/*
	 * Number of message latencies measured per process and iteration.
	 * PingPong is sequential (send + receive), PingPing is overlapped
	 * (receive + send), so IMB divides the aggregate time by 2 and 1
	 * respectively.
	 */
	protected double messagesPerIteration()
	{
		return 2.0;
	}

	protected void printHeaderResults()
	{
		System.out.println(" #bytes \t #repetitions \t t[usec] \t Mbytes/sec \t Msg/sec");
	}

	protected void printCsvHeader()
	{
		writeCsvHeader("size_bytes,repetitions,t_us,mb_per_sec,msg_per_sec");
	}

	protected void printResults(int size, int iters, double[] times)
	{
		printResultsHeader();

		double sum = 0.0;
		double max = 0.0;

		for(int i = 0; i < times.length; i++)
		{
			sum += times[i];
			if(times[i] > max)
				max = times[i];
		}

		double divisor = messagesPerIteration() * iters * nprocess;

		double latency = (divisor > 0.0) ? (1E6 * sum / divisor) : 0.0;
		double bandwidth = (max > 0.0) ? (iters * (double)nprocess * size) / (1E6 * max) : 0.0;
		double messageRate = (max > 0.0) ? (iters * (double)nprocess) / max : 0.0;

		System.out.format(Locale.ENGLISH, " %12d %12d %12.2f %12.2f %12.0f%n",
				  size, iters, latency, bandwidth, messageRate);
		writeCsvRow(String.format(Locale.ENGLISH, "%d,%d,%.6f,%.6f,%.6f",
				  size, iters, latency, bandwidth, messageRate));
	}
}
