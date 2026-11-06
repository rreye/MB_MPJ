/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/*
IMB-P2P Unirandom
Based on		MPI.COMM_WORLD.Send, MPI.COMM_WORLD.Recv
MPI.Datatype		MPI.BYTE
Description		Each iteration chooses a random sender and a different
			random receiver. The sender calls Send and the receiver
			calls Recv. Every rank derives the same sequence, so the
			roles match without extra communication.
*/

public class Unirandom extends RandomBench
{
	private Unirandom()
	{
		this.datatype = MPI.BYTE;
	}

	protected double totalMessages(int iters)
	{
		return (double)iters * this.nprocess;
	}

	protected void Benchrun(int size,int iters) throws MPIException
	{
		double startwtime,endwtime;
		int num = 1;
		long total = (long)iters * this.nprocess;

		if(size>this.datatype_size)
			num = size/datatype_size;

		resetRandom();
		startwtime = MicroBench.Wtime();
		for (long i = 0; i < total; i++)
		{
			int sender = nextRank();
			int receiver;

			do {
				receiver = nextRank();
			} while(receiver == sender);

			if(this.rank == sender)
			{
				touchSendBuffer((byte[])A, num);
				MPI.COMM_WORLD.Send(A, 0, num, datatype, receiver, 0);
			}
			else if(this.rank == receiver)
			{
				MPI.COMM_WORLD.Recv(B, 0, num, datatype, sender, 0);
				touchRecvBuffer((byte[])B, num);
			}
		}
		endwtime = MicroBench.Wtime();
		myTime[0] = endwtime - startwtime;
	}

	public static void main(String[] argv) throws MicroBenchException
	{
		Unirandom bc = new Unirandom();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
	}
}
