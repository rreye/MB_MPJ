/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import java.util.Locale;

import MB_MPJ.MicroBench;

/*
 * Common base for the IMB-MPI1 bandwidth benchmarks (Uniband/Biband).
 *
 * IMB classifies them as ParallelTransferMsgRate: every process participates,
 * an even number of processes is required, and the output is
 * #bytes #repetitions Mbytes/sec Msg/sec. The reported message rate uses the
 * window size and the number of pairs, and Biband doubles the bandwidth scale
 * because it moves data in both directions.
 */
public abstract class MsgRateBench extends MicroBench
{
	protected int minRanks()
	{
		return 2;
	}

	protected boolean requiresEvenRanks()
	{
		return true;
	}

	protected boolean compactHeader()
	{
		return true;
	}

	/* IMB scale_bw: 1.0 for Uniband, 2.0 for Biband */
	protected double messageRateScale()
	{
		return 1.0;
	}

	protected void printHeaderResults()
	{
		System.out.println(" #bytes \t #repetitions \t Mbytes/sec \t Msg/sec");
	}

	protected void printCsvHeader()
	{
		writeCsvHeader("size_bytes,repetitions,mb_per_sec,msg_per_sec");
	}

	protected void printResults(int size, int iters, double[] times)
	{
		printResultsHeader();

		double max = 0.0;

		for(int i = 0; i < times.length; i++)
		{
			if(times[i] > max)
				max = times[i];
		}

		double tmax = 1E6 * max;
		int peers = this.nprocess / 2;
		double messageRate = (tmax > 0.0)
			? (messageRateScale() * 1E6 * WINDOW_SIZE * peers / tmax)
			: 0.0;
		double throughput = (tmax > 0.0)
			? (messageRateScale() * WINDOW_SIZE * peers * size / tmax)
			: 0.0;

		System.out.format(Locale.ENGLISH, " %12d %12d %12.2f %12.0f%n",
				  size, iters, throughput, messageRate);
		writeCsvRow(String.format(Locale.ENGLISH, "%d,%d,%.6f,%.6f",
				  size, iters, throughput, messageRate));
	}
}
