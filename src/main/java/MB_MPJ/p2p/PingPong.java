/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/*
IMB-P2P PingPong
Based on		MPI.COMM_WORLD.Send, MPI.COMM_WORLD.Recv
MPI.Datatype		MPI.BYTE
Description		Every process is paired with (rank + nprocess/2) % nprocess.
			Processes in the first half send first and receive afterwards;
			the others do the opposite. An even number of processes is
			required, and all of them take part in the measurement.
*/

public class PingPong extends P2PBench
{
	private PingPong()
	{
		this.datatype = MPI.BYTE;
	}

	protected boolean requiresEvenRanks()
	{
		return true;
	}

	protected void Benchrun(int size,int iters) throws MPIException
	{
		double startwtime,endwtime;
		int num = 1, tag = 0;
		int partner = (this.rank + this.nprocess/2) % this.nprocess;

		if(size>this.datatype_size)
			num = size/datatype_size;

		startwtime = MicroBench.Wtime();

		if(this.rank < this.nprocess/2)
		{
			for (int i = 0; i < iters; i++)
			{
				touchSendBuffer((byte[])A, num);
				MPI.COMM_WORLD.Send(A, 0, num, datatype, partner, tag);
				MPI.COMM_WORLD.Recv(B, 0, num, datatype, partner, tag);
				touchRecvBuffer((byte[])B, num);
			}
		}
		else
		{
			for (int i = 0; i < iters; i++)
			{
				MPI.COMM_WORLD.Recv(B, 0, num, datatype, partner, tag);
				touchRecvBuffer((byte[])B, num);
				touchSendBuffer((byte[])A, num);
				MPI.COMM_WORLD.Send(A, 0, num, datatype, partner, tag);
			}
		}

		endwtime = MicroBench.Wtime();
		myTime[0] = endwtime - startwtime;
	}

	public static void main(String[] argv) throws MicroBenchException
	{
		PingPong bc = new PingPong();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
	}
}
