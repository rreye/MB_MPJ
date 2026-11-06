/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/*
IMB-P2P Birandom
Based on		MPI.COMM_WORLD.Irecv/Request.Wait, MPI.COMM_WORLD.Send
MPI.Datatype		MPI.BYTE
Description		Each iteration chooses two different random ranks. Both
			ranks post a non-blocking receive from the other, send to
			the other and wait, so the exchange is bidirectional.
			Every rank derives the same sequence, so the roles match
			without extra communication.
*/

public class Birandom extends RandomBench
{
	private Birandom()
	{
		this.datatype = MPI.BYTE;
	}

	protected double totalMessages(int iters)
	{
		return 2.0 * iters * this.nprocess;
	}

	protected void Benchrun(int size,int iters) throws MPIException
	{
		double startwtime,endwtime;
		int num = 1;
		long total = (long)iters * this.nprocess;
		Request request = null;

		if(size>this.datatype_size)
			num = size/datatype_size;

		resetRandom();
		startwtime = MicroBench.Wtime();
		for (long i = 0; i < total; i++)
		{
			int rank0 = nextRank();
			int rank1;
			int peer;

			do {
				rank1 = nextRank();
			} while(rank1 == rank0);

			if(this.rank == rank0)
				peer = rank1;
			else if(this.rank == rank1)
				peer = rank0;
			else
				continue;

			request = MPI.COMM_WORLD.Irecv(B, 0, num, datatype, peer, 0);
			touchSendBuffer((byte[])A, num);
			MPI.COMM_WORLD.Send(A, 0, num, datatype, peer, 0);
			request.Wait();
			touchRecvBuffer((byte[])B, num);
		}
		endwtime = MicroBench.Wtime();
		myTime[0] = endwtime - startwtime;
	}

	public static void main(String[] argv) throws MicroBenchException
	{
		Birandom bc = new Birandom();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
	}
}
