/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.mpi1;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/* 
Based on                MPI.COMM_WORLD.Isend/Request.Wait, MPI.COMM_WORLD.Recv
MPI.Datatype            MPI.BYTE
*/

public class PingPing extends MicroBench 
{
  	protected PingPing() 
	{
		this.datatype = MPI.BYTE;
  	}

	/* IMB-MPI1 PingPing uses pair0=0 and pair1=nprocess-1; the rest idle */
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
  		int num = 1,  s_tag = 1, r_tag = receiveTag();
		Request r = null;
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
                      		r = MPI.COMM_WORLD.Isend(A, 0, num, datatype, pair1, s_tag);
                               	MPI.COMM_WORLD.Recv(B, 0, num, datatype, receiveSource(pair1), r_tag);
				r.Wait();
                        }
                        endwtime = MicroBench.Wtime();
                        myTime[0] = (double)((endwtime - startwtime)/iters);
                }
                else if(this.rank==pair1)
                {
                        startwtime = MicroBench.Wtime();
                        for (int i = 0; i < iters; i++)
                        {
 	   			r = MPI.COMM_WORLD.Isend(A, 0, num, datatype, pair0, s_tag);
                       		MPI.COMM_WORLD.Recv(B, 0, num, datatype, receiveSource(pair0), r_tag);
				r.Wait();
                        }
                        endwtime = MicroBench.Wtime();
                        myTime[0] = (double)((endwtime - startwtime)/iters);
                }
	}

  	public static void main(String[] argv) throws MicroBenchException
	{
		PingPing bc = new PingPing();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
  	}
}
