/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/*
IMB-P2P Corandom
Based on		MPI.COMM_WORLD.Irecv/Request.Wait, MPI.COMM_WORLD.Send
MPI.Datatype		MPI.BYTE
Description		Each iteration shuffles a random ring of all ranks. Every
			rank receives from the previous rank in the ring and
			sends to the next one, so all ranks participate. Every
			rank shuffles the same sequence, so the ring matches
			without extra communication.
*/

public class Corandom extends RandomBench
{
	private Corandom()
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
		int[] randomRing = new int[this.nprocess];
		Request request = null;

		if(size>this.datatype_size)
			num = size/datatype_size;

		for(int i = 0; i < this.nprocess; i++)
			randomRing[i] = i;

		resetRandom();
		startwtime = MicroBench.Wtime();
		for (int i = 0; i < iters; i++)
		{
			for (int k = 0; k < this.nprocess; k++)
			{
				int j = (int)(nextRandom() % this.nprocess);
				int r = randomRing[k];
				randomRing[k] = randomRing[j];
				randomRing[j] = r;
			}

			int pos = 0;
			while(randomRing[pos] != this.rank)
				pos++;

			int source = randomRing[(pos + this.nprocess - 1) % this.nprocess];
			int dest = randomRing[(pos + 1) % this.nprocess];

			request = MPI.COMM_WORLD.Irecv(B, 0, num, datatype, source, 0);
			touchSendBuffer((byte[])A, num);
			MPI.COMM_WORLD.Send(A, 0, num, datatype, dest, 0);
			request.Wait();
			touchRecvBuffer((byte[])B, num);
		}
		endwtime = MicroBench.Wtime();
		myTime[0] = endwtime - startwtime;
	}

	public static void main(String[] argv) throws MicroBenchException
	{
		Corandom bc = new Corandom();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
	}
}
