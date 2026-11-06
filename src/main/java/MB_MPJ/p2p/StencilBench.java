/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

/*
 * Common base for the IMB-P2P stencil benchmarks. The number of ranks must
 * factor into a 2D or 3D grid; every rank exchanges one message with each
 * neighbour per iteration.
 */
public abstract class StencilBench extends AggregatedBench
{
	/* Grid dimensions, or null when nprocess cannot form a valid grid. */
	protected abstract int[] grid(int nprocess);

	/* Number of neighbours: 4 for Stencil2D and 6 for Stencil3D. */
	protected abstract int neighbors();

	protected boolean validRankCount(int nprocess)
	{
		return grid(nprocess) != null;
	}

	protected double totalMessages(int iters)
	{
		return (double)neighbors() * this.nprocess * iters;
	}
}
