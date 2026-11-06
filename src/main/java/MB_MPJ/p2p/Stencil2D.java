/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/*
IMB-P2P Stencil2D
Based on		MPI.COMM_WORLD.Irecv/Isend/Request.Waitall
MPI.Datatype		MPI.BYTE
Description		The ranks form a periodic 2D grid. Every rank posts a
			receive and a send to each of its four neighbours, waits
			for all eight operations and repeats. The grid dimensions
			are computed as in IMB-P2P.
*/

public class Stencil2D extends StencilBench
{
	private static final int NUMBER_OF_NEIGHBORS = 4;

	private Stencil2D()
	{
		this.datatype = MPI.BYTE;
	}

	static int[] gridFor(int nprocess)
	{
		int lengthX = 2;
		int lengthY = 2;

		while ((long)lengthX * lengthY < nprocess)
		{
			lengthX++;
			lengthY++;
		}

		if ((long)lengthX * lengthY > nprocess)
		{
			while (lengthY > 1 && nprocess % lengthY != 0)
				lengthY--;

			lengthX = nprocess / lengthY;
		}

		if (lengthX < 2 || lengthY < 2 || (long)lengthX * lengthY != nprocess)
			return null;

		return new int[] { lengthX, lengthY };
	}

	private static int getRank(int x, int y, int lengthX, int lengthY)
	{
		x = (x + lengthX) % lengthX;
		y = (y + lengthY) % lengthY;
		return lengthX * y + x;
	}

	protected int[] grid(int nprocess)
	{
		return gridFor(nprocess);
	}

	protected int neighbors()
	{
		return NUMBER_OF_NEIGHBORS;
	}

	protected String benchmarkTitle()
	{
		int[] dims = gridFor(this.nprocess);

		if(dims == null)
			return super.benchmarkTitle();

		return this.getClass().getName()+" ("+dims[1]+" x "+dims[0]+")";
	}

	protected void Benchrun(int size,int iters) throws MPIException
	{
		double startwtime,endwtime;
		int[] dims = gridFor(this.nprocess);

		if(dims == null)
			return;

		int lengthX = dims[0];
		int lengthY = dims[1];
		int x = this.rank % lengthX;
		int y = this.rank / lengthX;
		int[] neighbours = new int[NUMBER_OF_NEIGHBORS];
		int num = 1;

		neighbours[0] = getRank(x - 1, y, lengthX, lengthY);
		neighbours[1] = getRank(x + 1, y, lengthX, lengthY);
		neighbours[2] = getRank(x, y - 1, lengthX, lengthY);
		neighbours[3] = getRank(x, y + 1, lengthX, lengthY);

		if(size>this.datatype_size)
			num = size/datatype_size;

		byte[][] sendBuffers = new byte[NUMBER_OF_NEIGHBORS][num];
		byte[][] recvBuffers = new byte[NUMBER_OF_NEIGHBORS][num];
		Request[] requests = new Request[2 * NUMBER_OF_NEIGHBORS];

		startwtime = MicroBench.Wtime();
		for (int i = 0; i < iters; i++)
		{
			for (int n = 0; n < NUMBER_OF_NEIGHBORS; n++)
				touchSendBuffer(sendBuffers[n], num);

			for (int n = 0; n < NUMBER_OF_NEIGHBORS; n++)
				requests[n] = MPI.COMM_WORLD.Irecv(recvBuffers[n], 0, num, datatype, neighbours[n], 0);

			for (int n = 0; n < NUMBER_OF_NEIGHBORS; n++)
				requests[NUMBER_OF_NEIGHBORS + n] = MPI.COMM_WORLD.Isend(sendBuffers[n], 0, num, datatype, neighbours[n], 0);

			Request.Waitall(requests);

			for (int n = 0; n < NUMBER_OF_NEIGHBORS; n++)
				touchRecvBuffer(recvBuffers[n], num);
		}
		endwtime = MicroBench.Wtime();
		myTime[0] = endwtime - startwtime;
	}

	public static void main(String[] argv) throws MicroBenchException
	{
		Stencil2D bc = new Stencil2D();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
	}
}
