/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/*
IMB-P2P PingPing
Based on		MPI.COMM_WORLD.Irecv/Request.Wait, MPI.COMM_WORLD.Send
MPI.Datatype		MPI.BYTE
Description		Every process is paired with (rank + nprocess/2) % nprocess.
			All processes post a non-blocking receive, send and wait, so
			send and receive overlap. An even number of processes is
			required, and all of them take part in the measurement.
*/

public class PingPing extends P2PBench
{
	private PingPing()
	{
		this.datatype = MPI.BYTE;
	}

	protected boolean requiresEvenRanks()
	{
		return true;
	}

	protected double messagesPerIteration()
	{
		return 1.0;
	}

	protected void Benchrun(int size,int iters) throws MPIException
	{
		double startwtime,endwtime;
		int num = 1, tag = 0;
		int partner = (this.rank + this.nprocess/2) % this.nprocess;
		Request request = null;

		if(size>this.datatype_size)
			num = size/datatype_size;

		startwtime = MicroBench.Wtime();

		for (int i = 0; i < iters; i++)
		{
			request = MPI.COMM_WORLD.Irecv(B, 0, num, datatype, partner, tag);
			touchSendBuffer((byte[])A, num);
			MPI.COMM_WORLD.Send(A, 0, num, datatype, partner, tag);
			request.Wait();
			touchRecvBuffer((byte[])B, num);
		}

		endwtime = MicroBench.Wtime();
		myTime[0] = endwtime - startwtime;
	}

	public static void main(String[] argv) throws MicroBenchException
	{
		PingPing bc = new PingPing();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
	}
}
