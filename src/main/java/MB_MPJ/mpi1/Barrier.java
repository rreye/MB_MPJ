/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/* 
MPI Collective          MPI.COMM_WORLD.Barrier
*/

public class Barrier extends MicroBench 
{
  	private Barrier() 
	{}

	protected boolean needsBuffers()
	{
		return false;
	}

	protected boolean singleIteration()
	{
		return true;
	}

  	protected void Benchrun(int size,int iters) throws MPIException 
	{
		double startwtime,endwtime;

 		for(int j=0; j< MicroBench.NBARRIERS; j++)
	        	MPI.COMM_WORLD.Barrier();
        	
		startwtime = MicroBench.Wtime();
              	for (int i = 0; i < iters; i++)
			MPI.COMM_WORLD.Barrier();
                endwtime = MicroBench.Wtime();
	        myTime[0] = (double)(endwtime - startwtime)/iters;
	}

  	public static void main(String[] argv) throws MicroBenchException
	{
		Barrier bc = new Barrier();
		bc.init(argv);
		bc.run(0,0);
  	}
}
