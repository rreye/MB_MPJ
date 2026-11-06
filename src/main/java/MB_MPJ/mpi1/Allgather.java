/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/* 
MPI Collective		MPI.COMM_WORLD.Allgather
MPI.Datatype		MPI.BYTE
Description		Every process inputs X bytes and receives 
			the gathered X*nprocs bytes.
*/

public class Allgather extends CollectiveBench 
{
  	private Allgather() 
	{
		this.datatype = MPI.BYTE;
  	}

  	protected void Benchrun(int size,int iters) throws MPIException 
	{
		double total = 0.0;
  		int num=1;

                if(size>this.datatype_size)
                        num = size/datatype_size;

 		for(int j=0; j< MicroBench.NBARRIERS; j++)
	        	MPI.COMM_WORLD.Barrier();
        	
              	for (int i = 0; i < iters; i++)
	    	{
	 		double t1 = MicroBench.Wtime();
	 		MPI.COMM_WORLD.Allgather(A, 0, num, datatype, B, 0, num, datatype);
	 		double t2 = MicroBench.Wtime();
	 		total += t2 - t1;
	 		syncAfterIteration();
		}
	        myTime[0] = total/iters;
	}

  	public static void main(String[] argv) throws MicroBenchException
	{
		Allgather bc = new Allgather();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount*bc.nprocess);
  	}
}
