/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import java.util.Locale;

/*
 * Common base for IMB-P2P benchmarks whose ranks do not all move the same
 * number of messages per iteration. Rank 0 aggregates the elapsed times and
 * the message count, as in IMB-P2P.
 */
public abstract class AggregatedBench extends P2PBench
{
	/* Number of messages moved by the whole communicator in the measured run. */
	protected abstract double totalMessages(int iters);

	protected void printCsvHeader()
	{
		writeCsvHeader("size_bytes,repetitions,latency_us,mb_per_sec,msg_per_sec");
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

		double messages = totalMessages(iters);
		double latency = (messages > 0.0) ? (1E6 * sum / messages) : 0.0;
		double bandwidth = (max > 0.0) ? (messages * size / (1E6 * max)) : 0.0;
		double messageRate = (max > 0.0) ? (messages / max) : 0.0;

		System.out.format(Locale.ENGLISH, " %12d %12d %12.2f %12.2f %12.0f%n",
				  size, iters, latency, bandwidth, messageRate);
		writeCsvRow(String.format(Locale.ENGLISH, "%d,%d,%.6f,%.6f,%.6f",
				  size, iters, latency, bandwidth, messageRate));
	}
}
