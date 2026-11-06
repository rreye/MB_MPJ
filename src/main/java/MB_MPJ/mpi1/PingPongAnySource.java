/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBenchException;

import mpi.*;

/*
IMB-MPI1 PingPongAnySource
Based on		MPI.COMM_WORLD.Send, MPI.COMM_WORLD.Recv
MPI.Datatype		MPI.BYTE
Description		Same pairing as IMB-MPI1 PingPong (pair0=0 and
			pair1=nprocess-1, the rest idle), but the receiver
			accepts MPI.ANY_SOURCE and MPI.ANY_TAG.
*/

public class PingPongAnySource extends PingPong
{
	private PingPongAnySource()
	{
		super();
	}

	protected int receiveSource(int partner)
	{
		return MPI.ANY_SOURCE;
	}

	protected int receiveTag()
	{
		return MPI.ANY_TAG;
	}

	public static void main(String[] argv) throws MicroBenchException
	{
		PingPongAnySource bc = new PingPongAnySource();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
	}
}
