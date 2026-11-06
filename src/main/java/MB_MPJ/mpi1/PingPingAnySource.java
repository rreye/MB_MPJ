/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBenchException;

import mpi.*;

/*
IMB-MPI1 PingPingAnySource
Based on		MPI.COMM_WORLD.Isend/Request.Wait, MPI.COMM_WORLD.Recv
MPI.Datatype		MPI.BYTE
Description		Same pairing as IMB-MPI1 PingPing (pair0=0 and
			pair1=nprocess-1, the rest idle), but the receiver
			accepts MPI.ANY_SOURCE and MPI.ANY_TAG.
*/

public class PingPingAnySource extends PingPing
{
	private PingPingAnySource()
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
		PingPingAnySource bc = new PingPingAnySource();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
	}
}
