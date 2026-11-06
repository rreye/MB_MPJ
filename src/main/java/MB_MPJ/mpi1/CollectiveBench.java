/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;

import mpi.*;

/*
 * Common base for IMB-MPI1 collective benchmarks. IMB reports only the
 * min/max/average times for collectives; there is no Mbytes/sec column.
 */
public abstract class CollectiveBench extends MicroBench
{
	protected boolean reportsBandwidth()
	{
		return false;
	}

	/* IMB-MPI1 -sync: barrier after each collective iteration */
	protected void syncAfterIteration() throws MPIException
	{
		if(MicroBench.SYNC)
			MPI.COMM_WORLD.Barrier();
	}
}
