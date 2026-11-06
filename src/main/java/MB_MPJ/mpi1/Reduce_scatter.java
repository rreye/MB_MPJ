/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/* 
MPI Collective          MPI.COMM_WORLD.Reduce_scatter
MPI.Datatype            MPI.FLOAT
MPI.Op                  MPI.SUM
Description		It reduces a vector of length L = X/sizeof(float) float items. 
			In the scatter phase, the L items are split as evenly as possible. 
			Exactly, when np = #processes, L = r*np+s (s = L mod np),
			then process with rank i gets r+1 items when i<s , and r items when i>=s.
*/

public class Reduce_scatter extends CollectiveBench 
{
  	private Reduce_scatter() 
	{
		this.datatype = MPI.FLOAT;
		this.op = MPI.SUM;
  	}

  	protected void Benchrun(int size,int iters) throws MPIException 
	{
		double total = 0.0;
		int recvcount[] = new int[this.nprocess];

		for(int i=0; i<this.nprocess; i++)
			recvcount[i] = getRecvCount(size, datatype_size, i);

 		for(int j=0; j< MicroBench.NBARRIERS; j++)
	        	MPI.COMM_WORLD.Barrier();
        	
              	for (int i = 0; i < iters; i++)
	    	{
	 		double t1 = MicroBench.Wtime();
	 		MPI.COMM_WORLD.Reduce_scatter(A, 0, B, 0, recvcount, datatype, op);
	 		double t2 = MicroBench.Wtime();
	 		total += t2 - t1;
	 		syncAfterIteration();
		}
	        myTime[0] = total/iters;
	}

  	public static void main(String[] argv) throws MicroBenchException
	{
		Reduce_scatter bc = new Reduce_scatter();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
  	}

	private int getRecvCount(int size, int unitSize, int rank)
	{
		int numE, num, mod;

		numE = size/unitSize;
		mod = numE%this.nprocess;
		num = (numE-mod)/this.nprocess;

		if(rank < mod)
			return num+1;
		else
			return num;
	}
}
