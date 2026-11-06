/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/* 
Based on		MPI.COMM_WORLD.Send, MPI.COMM_WORLD.Recv
MPI.Datatype            MPI.BYTE
*/

public class PingPong extends MicroBench 
{
  	protected PingPong() 
	{
		this.datatype = MPI.BYTE;
  	}

	/* IMB-MPI1 PingPong uses pair0=0 and pair1=nprocess-1; the rest idle */
	protected int minRanks()
	{
		return 2;
	}

	protected boolean singleTimeColumn()
	{
		return true;
	}

	/* Specific-source variant: receive only from the partner, any tag */
	protected int receiveSource(int partner)
	{
		return partner;
	}

	protected int receiveTag()
	{
		return MPI.ANY_TAG;
	}

  	protected void Benchrun(int size,int iters) throws MPIException 
	{
		double startwtime,endwtime;
		int num = 1, s_tag = 1, r_tag = receiveTag();
		int pair0 = 0, pair1 = this.nprocess - 1;

    		if(size>this.datatype_size)
                        num = size/datatype_size;

 		for(int j=0; j< MicroBench.NBARRIERS; j++)
	        	MPI.COMM_WORLD.Barrier();

		if(this.rank==pair0)
		{
        		startwtime = MicroBench.Wtime();
	              	for (int i = 0; i < iters; i++)
		        {
                      		MPI.COMM_WORLD.Send(A, 0, num, datatype, pair1, s_tag);
                               	MPI.COMM_WORLD.Recv(B, 0, num, datatype, receiveSource(pair1), r_tag);
	                }
          		endwtime = MicroBench.Wtime();
	        	myTime[0] = (double)(((endwtime - startwtime)/iters)/2.0);
		}
		else if(this.rank==pair1)
                {
  	 		startwtime = MicroBench.Wtime();
	              	for (int i = 0; i < iters; i++)
		        {
                       		MPI.COMM_WORLD.Recv(B, 0, num, datatype, receiveSource(pair0), r_tag);
                        	MPI.COMM_WORLD.Send(A, 0, num, datatype, pair0, s_tag);
			}
	                endwtime = MicroBench.Wtime();
		        myTime[0] = (double)(((endwtime - startwtime)/iters)/2.0);
		}
	}

  	public static void main(String[] argv) throws MicroBenchException
	{
		PingPong bc = new PingPong();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
  	}
}
