/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/* 
Based on          	MPI.COMM_WORLD.Isend/Request.Waitall, MPI.COMM_WORLD.Recv
MPI.Datatype            MPI.BYTE
Description             Exchange is a communications pattern that often occurs in grid 
			splitting algorithms (boundary ex- changes). The group of processes
			is seen as a periodic chain, and each process exchanges data with 
			both left and right neighbor in the chain.
			The turnover count is four messages per sample (two in, two out) for 
			each process. Both Isend messages use different offsets of the same
			send buffer, as in IMB.
*/

public class Exchange extends MicroBench 
{
	private Exchange() 
	{
		this.datatype = MPI.BYTE;
  	}

	protected int sendBufferMultiplier()
	{
		return 2;
	}

	protected int bandwidthMultiplier()
	{
		return 4;
	}

  	protected void Benchrun(int size,int iters) throws MPIException 
	{
		double startwtime,endwtime;
                int num=1;
                int left = 0, right = 0, s_tag = 1, r_tag = MPI.ANY_TAG; // IMB uses ANY_TAG by default
                Request[] requests = new Request[2];

                if(size>this.datatype_size)
                        num = size/datatype_size;

                if(this.rank < this.nprocess-1)         right = this.rank+1;
                if(this.rank > 0)                       left = this.rank-1;
                if(this.rank == this.nprocess-1)        right = 0;
                if(this.rank == 0)                      left = this.nprocess-1;

                for(int j=0; j< MicroBench.NBARRIERS; j++)
                        MPI.COMM_WORLD.Barrier();

                startwtime = MicroBench.Wtime();
                for (int i = 0; i < iters; i++)
                {
			requests[0] = MPI.COMM_WORLD.Isend(A, 0, num, datatype, right, s_tag);
			requests[1] = MPI.COMM_WORLD.Isend(A, num, num, datatype, left, s_tag);
			MPI.COMM_WORLD.Recv(B, 0, num, datatype, left, r_tag);
			MPI.COMM_WORLD.Recv(B, 0, num, datatype, right, r_tag);
			Request.Waitall(requests);
                }
                endwtime = MicroBench.Wtime();
                myTime[0] = (double)(endwtime - startwtime)/iters;
	}

  	public static void main(String[] argv) throws MicroBenchException
	{
		Exchange bc = new Exchange();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
  	}
}
