/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/*
IMB-P2P SendRecv_Replace
Based on		MPI.COMM_WORLD.Sendrecv_replace
MPI.Datatype		MPI.BYTE
Description		The processes form a periodic communication chain.
			Each process sends to the right and receives from the
			left neighbor using the same buffer. All processes take
			part and at least 2 are required.
*/

public class SendRecv_Replace extends P2PBench
{
	private SendRecv_Replace()
	{
		this.datatype = MPI.BYTE;
	}

	/*
	 * Each process performs one send/receive replacement per iteration,
	 * so IMB-P2P divides the aggregate time by iters * nprocess.
	 */
	protected double messagesPerIteration()
	{
		return 1.0;
	}

	protected void Benchrun(int size,int iters) throws MPIException
	{
		double startwtime,endwtime;
		int num = 1, tag = 0;
		int dest = (this.rank + 1) % this.nprocess;
		int source = (this.rank + this.nprocess - 1) % this.nprocess;

		if(size>this.datatype_size)
			num = size/datatype_size;

		startwtime = MicroBench.Wtime();
		for (int i = 0; i < iters; i++)
		{
			touchSendBuffer((byte[])A, num);
			MPI.COMM_WORLD.Sendrecv_replace(A, 0, num, datatype, dest, tag, source, tag);
		}
		touchRecvBuffer((byte[])A, num);
		endwtime = MicroBench.Wtime();
		myTime[0] = endwtime - startwtime;
	}

	public static void main(String[] argv) throws MicroBenchException
	{
		SendRecv_Replace bc = new SendRecv_Replace();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
	}
}
