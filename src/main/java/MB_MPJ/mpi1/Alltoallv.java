/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/* 
MPI Collective          MPI.COMM_WORLD.Alltoallv
MPI.Datatype            MPI.BYTE
Description		Every process inputs X*nprocs bytes (X for each process) 
			and receives X*nprocs bytes (X from each process).
*/

public class Alltoallv extends CollectiveBench 
{
  	private Alltoallv() 
	{
		this.datatype = MPI.BYTE;
  	}

  	protected void Benchrun(int size,int iters) throws MPIException 
	{
		double total = 0.0;
  		int num=1;
		int counts[] = new int[this.nprocess];
                int displs[] = new int[this.nprocess];

    		if(size>this.datatype_size)
                        num = size/datatype_size;

                /* Initialization of displacement and send/receive counts */
                for (int i=0; i<this.nprocess; i++)
                {
                        displs[i] = num*i;
                        counts[i] = num;
                }
 
 		for(int j=0; j< MicroBench.NBARRIERS; j++)
	        	MPI.COMM_WORLD.Barrier();
        	
              	for (int i = 0; i < iters; i++)
	    	{
			double t1 = MicroBench.Wtime();
			MPI.COMM_WORLD.Alltoallv(A, 0, counts, displs, datatype, B, 0, counts, displs, datatype);
			double t2 = MicroBench.Wtime();
			total += t2 - t1;
			syncAfterIteration();
		}
	        myTime[0] = total/iters;
	}

  	public static void main(String[] argv) throws MicroBenchException
	{
		Alltoallv bc = new Alltoallv();
		bc.init(argv);
		bc.run(bc.bufferCount*bc.nprocess, bc.bufferCount*bc.nprocess);
  	}
}
