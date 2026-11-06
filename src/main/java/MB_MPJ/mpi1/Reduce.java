/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/* 
MPI Collective          MPI.COMM_WORLD.Reduce
MPI.Datatype            MPI.FLOAT
MPI.Op                  MPI.SUM
Root			i%nprocs in iteration i (Round Robin)
Description             It reduces a vector of length L = X/sizeof(float)float items.
*/

public class Reduce extends CollectiveBench 
{
  	private Reduce() 
	{
		this.datatype = MPI.FLOAT;
		this.op = MPI.SUM;
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
	 		double t1 = MicroBench.Wtime();
	 		MPI.COMM_WORLD.Reduce(A, 0, B, 0, num, datatype, op, root);
	 		double t2 = MicroBench.Wtime();
	 		total += t2 - t1;
	 		if(MicroBench.ROOT_SHIFT) root = (root + 1) % this.nprocess;
	 		syncAfterIteration();
		}
	        myTime[0] = total/iters;
	}

  	public static void main(String[] argv) throws MicroBenchException
	{
		Reduce bc = new Reduce();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
  	}
}
