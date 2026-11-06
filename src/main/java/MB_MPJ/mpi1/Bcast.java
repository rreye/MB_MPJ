/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/* 
MPI Collective          MPI.COMM_WORLD.Bcast
MPI.Datatype            MPI.BYTE
Root                    i%nprocs in iteration i (Round Robin)
Description             A root process broadcasts X bytes to all.
*/

public class Bcast extends CollectiveBench 
{
  	private Bcast() 
	{
		this.datatype = MPI.BYTE;
  	}

  	protected void Benchrun(int size,int iters) throws MPIException 
	{
		double total = 0.0;
  		int num=1;
		int root = 0;

    		if(size>this.datatype_size)
                        num = size/datatype_size;

 		for(int j=0; j< MicroBench.NBARRIERS; j++)
	        	MPI.COMM_WORLD.Barrier();
        	
              	for (int i = 0; i < iters; i++)
	    	{
			Object bcBuf = (root == this.rank) ? A : B;

			double t1 = MicroBench.Wtime();
			MPI.COMM_WORLD.Bcast(bcBuf, 0, num, datatype, root);
			double t2 = MicroBench.Wtime();
			total += t2 - t1;
			if(MicroBench.ROOT_SHIFT) root = (root + 1) % this.nprocess;
			syncAfterIteration();
		}
	        myTime[0] = total/iters;
	}

  	public static void main(String[] argv) throws MicroBenchException
	{
		Bcast bc = new Bcast();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
  	}
}
