/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/*
IMB-MPI1 Biband
Based on		MPI.COMM_WORLD.Irecv/Isend/Request.Waitall
MPI.Datatype		MPI.BYTE
Description		Every process is paired with (rank + nprocess/2) % nprocess.
			Each side posts a window of receives and a window of sends
			to its partner, waits for all of them and then exchanges a
			one-byte ack. Traffic is bidirectional per pair.
*/

public class Biband extends MsgRateBench
{
	private Biband()
	{
		this.datatype = MPI.BYTE;
	}

	protected double messageRateScale()
	{
		return 2.0;
	}

	protected void Benchrun(int size,int iters) throws MPIException
	{
		double startwtime,endwtime;
		int num = 1, s_tag = 1, r_tag = 1;
		int peers = this.nprocess / 2;
		char[] ack = new char[1];
		Request[] requests = new Request[2 * WINDOW_SIZE];

		if(size>this.datatype_size)
			num = size/datatype_size;

		for(int j=0; j< MicroBench.NBARRIERS; j++)
			MPI.COMM_WORLD.Barrier();

		startwtime = MicroBench.Wtime();
		if(this.rank < peers)
		{
			int dest = this.rank + peers;

			for (int i = 0; i < iters; i++)
			{
				for (int ws = 0; ws < WINDOW_SIZE; ws++)
					requests[ws] = MPI.COMM_WORLD.Irecv(B, 0, num, datatype, dest, r_tag);

				for (int ws = 0; ws < WINDOW_SIZE; ws++)
					requests[ws + WINDOW_SIZE] = MPI.COMM_WORLD.Isend(A, 0, num, datatype, dest, s_tag);

				Request.Waitall(requests);
				MPI.COMM_WORLD.Recv(ack, 0, 1, MPI.CHAR, dest, r_tag);
			}
		}
		else
		{
			int source = this.rank - peers;

			for (int i = 0; i < iters; i++)
			{
				for (int ws = 0; ws < WINDOW_SIZE; ws++)
					requests[ws] = MPI.COMM_WORLD.Irecv(B, 0, num, datatype, source, r_tag);

				for (int ws = 0; ws < WINDOW_SIZE; ws++)
					requests[ws + WINDOW_SIZE] = MPI.COMM_WORLD.Isend(A, 0, num, datatype, source, s_tag);

				Request.Waitall(requests);
				MPI.COMM_WORLD.Send(ack, 0, 1, MPI.CHAR, source, s_tag);
			}
		}
		endwtime = MicroBench.Wtime();
		myTime[0] = (endwtime - startwtime)/iters;
	}

	public static void main(String[] argv) throws MicroBenchException
	{
		Biband bc = new Biband();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
	}
}
