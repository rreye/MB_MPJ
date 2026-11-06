/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import MB_MPJ.MicroBench;
import MB_MPJ.MicroBenchException;

import mpi.*;

/*
IMB-P2P Stencil3D
Based on		MPI.COMM_WORLD.Irecv/Isend/Request.Waitall
MPI.Datatype		MPI.BYTE
Description		The ranks form a periodic 3D grid. Every rank posts a
			receive and a send to each of its six neighbours, waits
			for all twelve operations and repeats. The grid dimensions
			are computed as in IMB-P2P.
*/

public class Stencil3D extends StencilBench
{
	private static final int NUMBER_OF_NEIGHBORS = 6;

	private Stencil3D()
	{
		this.datatype = MPI.BYTE;
	}

	static int[] gridFor(int nprocess)
	{
		int lengthX = 2;
		int lengthY = 2;
		int lengthZ = 2;

		while ((long)lengthX * lengthY * lengthZ < nprocess)
		{
			lengthX++;
			lengthY++;
			lengthZ++;
		}

		while ((long)lengthX * lengthY * lengthZ > nprocess)
		{
			int lx, ly, n = 0;

			lengthZ--;

			while (lengthZ > 1 && nprocess % lengthZ != 0)
				lengthZ--;

			if (lengthZ != 0)
				n = nprocess / lengthZ;

			lx = 2;
			ly = 2;

			while ((long)lx * ly < n)
			{
				lx++;
				ly++;
			}

			if ((long)lx * ly > n)
			{
				while (ly > 1 && n % ly != 0)
					ly--;

				lx = n / ly;
			}

			if (ly >= lengthZ)
			{
				lengthX = lx;
				lengthY = ly;
			}
		}

		if (lengthX < 2 || lengthY < 2 || lengthZ < 2 ||
		    (long)lengthX * lengthY * lengthZ != nprocess)
			return null;

		return new int[] { lengthX, lengthY, lengthZ };
	}

	private static int getRank(int x, int y, int z, int lengthX, int lengthY, int lengthZ)
	{
		x = (x + lengthX) % lengthX;
		y = (y + lengthY) % lengthY;
		z = (z + lengthZ) % lengthZ;
		return lengthX * lengthY * z + lengthX * y + x;
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

		return this.getClass().getName()+" ("+dims[2]+" x "+dims[1]+" x "+dims[0]+")";
	}

	protected void Benchrun(int size,int iters) throws MPIException
	{
		double startwtime,endwtime;
		int[] dims = gridFor(this.nprocess);

		if(dims == null)
			return;

		int lengthX = dims[0];
		int lengthY = dims[1];
		int lengthZ = dims[2];
		int z = this.rank / (lengthX * lengthY);
		int y = (this.rank - lengthX * lengthY * z) / lengthX;
		int x = (this.rank - lengthX * lengthY * z) % lengthX;
		int[] neighbours = new int[NUMBER_OF_NEIGHBORS];
		int num = 1;

		neighbours[0] = getRank(x - 1, y, z, lengthX, lengthY, lengthZ);
		neighbours[1] = getRank(x + 1, y, z, lengthX, lengthY, lengthZ);
		neighbours[2] = getRank(x, y - 1, z, lengthX, lengthY, lengthZ);
		neighbours[3] = getRank(x, y + 1, z, lengthX, lengthY, lengthZ);
		neighbours[4] = getRank(x, y, z - 1, lengthX, lengthY, lengthZ);
		neighbours[5] = getRank(x, y, z + 1, lengthX, lengthY, lengthZ);

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
		Stencil3D bc = new Stencil3D();
		bc.init(argv);
		bc.run(bc.bufferCount, bc.bufferCount);
	}
}
