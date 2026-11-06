/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/* 
Based on          	MPI.COMM_WORLD.Sendrecv
MPI.Datatype            MPI.BYTE
Description             The processes form a periodic communication chain. 
			Each process sends to the right and receives from the 
			left neighbor in the chain.
			The turnover count is 2 messages per sample (1 in, 1 out) 
			for each process.
*/

public class Sendrecv extends MicroBench 
{
	private Sendrecv() 
	{
		this.datatype = MPI.BYTE;
  	}

	protected int bandwidthMultiplier()
	{
		return 2;
	}

  	protected void Benchrun(int size,int iters) throws MPIException 
	{
		double startwtime,endwtime;
                int num=1;
                int dest, source, s_tag=1, r_tag = MPI.ANY_TAG;

                if(size>this.datatype_size)
                        num = size/datatype_size;

                /*  Calculate source and destination */
                dest   = (this.rank + 1) % this.nprocess;
                source = (this.rank + this.nprocess-1) % this.nprocess;

                for(int j=0; j< MicroBench.NBARRIERS; j++)
                        MPI.COMM_WORLD.Barrier();

                startwtime = MicroBench.Wtime();
                for (int i = 0; i < iters; i++)
                {
                        MPI.COMM_WORLD.Sendrecv(A, 0, num, datatype, dest, s_tag, B, 0, num, datatype, source, r_tag);
                }
                endwtime = MicroBench.Wtime();
                myTime[0] = (double)(endwtime - startwtime)/iters;
	}

  	public static void main(String[] argv) throws MicroBenchException
	{
		Sendrecv bc = new Sendrecv();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
  	}
}
