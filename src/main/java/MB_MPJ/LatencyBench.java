/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import mpi.*;

public class LatencyBench extends MicroBench {

	private static boolean CSV = false;
	private static String CSV_FILE = "latency_results.csv";
	private static int WARMUP_ITERS = 50000;
	private static final List<SizeResult> results = new ArrayList<SizeResult>();

	private static class SizeResult {
		private final int sizeBytes;
		private final double[] latenciesUs;

		SizeResult(int sizeBytes, double[] latenciesUs) {
			this.sizeBytes = sizeBytes;
			this.latenciesUs = latenciesUs;
		}
	}

	public LatencyBench(int rank, int nprocess) {
		this.rank = rank;
		this.nprocess = nprocess;
	}

	protected void Benchrun(int size, int iters) {
	}

	void Benchrun() throws MPIException {

		double[] myTimes = new double[ITERS];
		Datatype mpi_datatype = MPI.BYTE;
		byte A[] = new byte[MAX_SIZE];

		if (rank == 0)
			System.out
					.println("\n LatencyBench ******* TEST STARTED *********");

		int size = 1;

		// WARMUP loop
		for (int i = 0; i < WARMUP_ITERS; i++) {
			MPI.COMM_WORLD.Barrier();
			MicroBench.Wtime();
			if (this.rank == 0) {
				MPI.COMM_WORLD.Send(A, 0, size, mpi_datatype, 1, 1000 + i);
				MPI.COMM_WORLD.Recv(A, 0, size, mpi_datatype, 1, 2000 + i);
			} else if (this.rank == 1) {
				MPI.COMM_WORLD.Recv(A, 0, size, mpi_datatype, 0, 1000 + i);
				MPI.COMM_WORLD.Send(A, 0, size, mpi_datatype, 0, 2000 + i);
			}
		}

		double startwtime, endwtime;

		for (size = MIN_SIZE; size <= MAX_SIZE; size *= INCR_SIZE) {

			for (int i = 0; i < ITERS; i++) {
				startwtime = MicroBench.Wtime();
				if (this.rank == 0) {
					MPI.COMM_WORLD.Send(A, 0, size, mpi_datatype, 1, 1000 + i);
					MPI.COMM_WORLD.Recv(A, 0, size, mpi_datatype, 1, 2000 + i);
				} else if (this.rank == 1) {
					MPI.COMM_WORLD.Recv(A, 0, size, mpi_datatype, 0, 1000 + i);
					MPI.COMM_WORLD.Send(A, 0, size, mpi_datatype, 0, 2000 + i);
				}
				endwtime = MicroBench.Wtime();
				myTimes[i] = (endwtime - startwtime) / 2.0;
			}

			if (this.rank == 0)
				printResults(size, myTimes, ITERS);

			MPI.COMM_WORLD.Barrier();
		}

		if (rank == 0) {
			System.out
					.println("\n LatencyBench ******* TEST COMPLETED *********");

			if (CSV) {
				writeCsv();
				System.out.println("\n CSV results written to " + CSV_FILE);
			}
		}

		MPI.COMM_WORLD.Barrier();
	}// Benchrun

	public static void main(String[] argv) throws MPIException {
		new LatencyBench(argv);
	}

	public LatencyBench(String[] argv) throws MPIException {

		/* Initialise MPI */
		String[] args = MPI.Init(argv);
		rank = MPI.COMM_WORLD.Rank();
		nprocess = MPI.COMM_WORLD.Size();

		LatencyBench bc = new LatencyBench(rank, nprocess);

		WARMUP_ITERS = 50000;
		ITERS = 20000;
		MIN_SIZE = 1;
		MAX_SIZE = 4096;

		/* -help and option errors work regardless of the rank count. */
		processInput(args, rank);

		if (nprocess != 2) {
			if (rank == 0)
				System.out.println("Run this latency benchmark with 2 processes");
			MPI.Finalize();
			System.exit(0);
		}

		if (MAX_SIZE < MIN_SIZE || MAX_SIZE < 0 || MIN_SIZE < 0 || ITERS <= 0
				|| WARMUP_ITERS <= 0) {
			System.out.println("Incorrect input parameter value");
			System.exit(0);
		}

		bc.Benchrun();

		/* Finalise MPI */
		MPI.Finalize();
	}

	private static void printResults(int size, double latencies[], int iters) {
		double sum = 0.0;

		for (int i = 0; i < latencies.length; i++) {
			// seconds2micro-seconds
			latencies[i] = (double) 1E6 * latencies[i];
			sum = (double) sum + latencies[i];
		}

		if (CSV)
			results.add(new SizeResult(size, latencies.clone()));

		Arrays.sort(latencies);
		double tavg = (double) (sum / latencies.length);
		double tmin = (double) latencies[0];
		double tmedian = (double) latencies[latencies.length / 2];
		double t90percent = (double) latencies[(90 * latencies.length) / 100];
		double t95percent = (double) latencies[(95 * latencies.length) / 100];
		double t99percent = (double) latencies[(99 * latencies.length) / 100];
		double tmax = (double) latencies[latencies.length - 1];

		System.out
				.format(Locale.ENGLISH,
						"\n %8d bytes%7d iterations --> Tavg=%5.2f\tTmin=%5.2f\tT50p=%5.2f\tT90p=%5.2f\tT95p=%5.2f\tT99p=%5.2f\tTmax=%7.2f usec",
						size, iters, tavg, tmin, tmedian, t90percent,
						t95percent, t99percent, tmax);
	}

	private static void writeCsv() {
		try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE))) {
			writer.println("size_bytes,iteration,latency_us");

			for (SizeResult result : results) {
				for (int i = 0; i < result.latenciesUs.length; i++) {
					writer.printf(Locale.ENGLISH, "%d,%d,%.6f%n",
							result.sizeBytes, i + 1, result.latenciesUs[i]);
				}
			}
		} catch (IOException e) {
			throw new MicroBenchException("Could not write CSV file <"
					+ CSV_FILE + ">", e);
		}
	}

	private static void processInput(String args[], int rank) throws MPIException
	{
                for (int i = 0; i < args.length; i++)
                {
                        if(args[i].equals("-h") || args[i].equals("-H"))
                        {
                                if(rank==0) LatencyBench.printUsage();
                                MPI.Finalize();
                                System.exit(0);
                        }
                }

                for (int i = 0; i < args.length; i++)
                {
                        if(args[i].equals("-s") || args[i].equals("-S"))
                        {
                                if(i+1>=args.length)
                                {
                                        if(rank==0) LatencyBench.printUsage();
                                        MPI.Finalize();
                                        System.exit(0);
                                }
                                MIN_SIZE = new Integer(args[i+1]).intValue();
                                i++;
                        }
			else if(args[i].equals("-e") || args[i].equals("-E"))
                        {
                                if(i+1>=args.length)
                                {
                                        if(rank==0) LatencyBench.printUsage();
                                        MPI.Finalize();
                                        System.exit(0);
                                }
                                MAX_SIZE = new Integer(args[i+1]).intValue();
                                i++;
                        }
			else if (args[i].equals("-i") || args[i].equals("-I"))
                        {
                                if(i+1>=args.length)
                                {
                                        if(rank==0) LatencyBench.printUsage();
                                        MPI.Finalize();
                                        System.exit(0);
                                }
                                ITERS = new Integer(args[i+1]).intValue();
                                i++;
                        }
                        else if (args[i].equals("-w") || args[i].equals("-W"))
                        {
                                if(i+1>=args.length)
                                {
                                        if(rank==0) LatencyBench.printUsage();
                                        MPI.Finalize();
                                        System.exit(0);
                                }
                                WARMUP_ITERS = new Integer(args[i+1]).intValue();
                                i++;
                        }
                        else if(args[i].equalsIgnoreCase("-csv"))
                        {
                                CSV = true;
                                if(i+1<args.length && !args[i+1].startsWith("-"))
                                {
                                        CSV_FILE = args[++i];
                                }
                        }
                        else
                        {
                                if(rank==0) LatencyBench.printUsage();
                                MPI.Finalize();
                                System.exit(0);
                        }
		}
	}

	private static void printUsage() {
		System.out
				.println("\n#################################################################\n"
						+ "\n   -s|-S <start_len>"
						+ "\n                     Starting length in bytes"
						+ "\n   -e|-E <end_len>"
						+ "\n                     Ending length in bytes"
						+ "\n   -i|-I <iterations>"
						+ "\n                     Number of iterations"
						+ "\n   -w|-W <warmup_iter>"
						+ "\n                     Number of warmup iterations"
						+ "\n   -csv [file]"
						+ "\n                     Write raw latency measurements to CSV"
						+ "\n                     (default: latency_results.csv)"
						+ "\n   -h|-H"
						+ "\n                     This help message"
						+ "\n#################################################################"
						+ "\n");
	}
}
