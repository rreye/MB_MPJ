/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import mpi.*;

public abstract class MicroBench 
{
	static final String NAME = "MPJ Benchmark Suite";
	static final String VERSION = "1.0.0";
	protected static final int MEGA = 1048576;
	protected static final double MEGA_D = MEGA*1.0;
  	protected static int MIN_SIZE = 1;		//Minimun Size in bytes
  	protected static int MAX_SIZE = 4*MEGA; 		//Maximun Size in bytes
	static int MAX_MESSAGE_SIZE = 64;	//Maximun Message Size in megabytes
	static double MAX_MEMORY_ALLOC = 1.0;	//Maximun memory per process for send/receive buffers in GiB
	protected static double MAX_TIME = 10.0;	//Maximun seconds per measured sample
  	protected static int ITERS = 1000;		//Iterations per length
	protected static boolean WARM_UP = true;	//Perform warmup before measurements
	protected static boolean SYNC = true;		//IMB-MPI1: barrier after each collective iteration
	protected static boolean ROOT_SHIFT = false;	//IMB-MPI1: rotate the root of rooted collectives
	protected static final int WARMUP_PERCENT = 10;	//IMB warmup percentage
	protected static final int MIN_WARMUP_ITERS = 10;	//IMB minimum warmup iterations
	protected static int WINDOW_SIZE = 256;		//Messages in flight for Uniband/Biband
	protected static int[] MESSAGE_SIZES = null;	//Optional explicit size list
	protected static boolean MSGWR = true;		//IMB-P2P: touch send buffers
	protected static boolean MSGRD = true;		//IMB-P2P: touch receive buffers
	protected static int PAUSE_USEC = 100000;	//IMB-P2P: pause between samples
	protected static boolean SUPPORTS_TOUCH = false;	//Whether -msgwr/-msgrd apply
	protected static boolean SUPPORTS_MPI1_OPTIONS = true;	//Whether -mem/-time apply

	/* Optional CSV output of the result table. Only rank 0 writes it. */
	protected static boolean CSV_OUTPUT = false;
	protected static String CSV_OUTPUT_FILE = null;
	private static PrintWriter csvWriter = null;

	/* For all sizes < OVERALL_VOL, the repetition count is eventually
	   reduced so that not more than OVERALL_VOL bytes overall are processed.

	   This avoids unnecessary repetitions for large message sizes.

           Finally, the real repetition count for message size X is
	
	   max(1,min(ITERS,OVERALL_VOL/X))
	*/
	static int OVERALL_VOL = 40*MEGA;	

  	protected static final int INCR_SIZE = 2;
	protected static final int NBARRIERS = 2;
	static final double tento9 = new Long(1000000000L).doubleValue();
	static String[] args;
	static double tmin;
        static double tmax;
        static double tavg;
        static double bw;

	protected int nprocess = -1;
	protected int rank = -1;

	/* Send (A) and receive (B) buffers */
  	protected Object A = null;
  	protected Object B = null;
  	protected Datatype datatype = null;
	protected int datatype_size = 0;
	protected long bufferCount = 0;
  	protected Op op = null;

	/* Reported timings */
	protected double myTime[] = new double[1];
	protected double times[];
	private boolean resultsHeaderPrinted = false;

	/* Microbenchmark abstract method */
  	protected abstract void Benchrun(int size,int iters) throws MPIException;

	/*
	 * Per-benchmark capabilities. They replace the instanceof checks that
	 * used to couple MicroBench to specific benchmark classes.
	 */

	/* Minimum number of processes required by the benchmark */
	protected int minRanks()
	{
		return 1;
	}

	/* Whether the benchmark requires an even number of processes */
	protected boolean requiresEvenRanks()
	{
		return false;
	}

	/* Whether the benchmark supports this exact number of processes */
	protected boolean validRankCount(int nprocess)
	{
		return true;
	}

	/* Whether -msgwr/-msgrd are meaningful for this benchmark */
	protected boolean supportsTouchOptions()
	{
		return false;
	}

	/* IMB-P2P uses a pause and a dissemination barrier around each sample. */
	protected boolean customSamplePreparation()
	{
		return false;
	}

	protected void samplePause()
	{
	}

	protected void sampleBarrier() throws MPIException
	{
		MPI.COMM_WORLD.Barrier();
	}

	/* Whether the IMB-MPI1 -mem/-time options apply */
	protected boolean supportsMpi1Options()
	{
		return true;
	}

	/* Whether -time can cap the measured iterations */
	protected boolean usesTimeLimit()
	{
		return supportsMpi1Options();
	}

	/* IMB defaults, overridden by P2PBench for the IMB-P2P schedule */
	protected int defaultIterations()
	{
		return 1000;
	}

	protected int defaultOverallVolumeMiB()
	{
		return 40;
	}

	/* IMB-MPI1 and IMB-P2P warm up for every message size */
	protected boolean warmUpEachSize()
	{
		return true;
	}

	protected int warmupIterations(int iters)
	{
		int warmup = iters / WARMUP_PERCENT;

		if(warmup < MIN_WARMUP_ITERS)
			warmup = Math.min(MIN_WARMUP_ITERS, iters);

		return warmup;
	}

	/* Whether the benchmark needs send/receive buffers */
	protected boolean needsBuffers()
	{
		return true;
	}

	/* Whether the benchmark runs a single iteration instead of a size sweep */
	protected boolean singleIteration()
	{
		return false;
	}

	/* Multiplier for the size of the send buffer (Exchange uses two offsets) */
	protected int sendBufferMultiplier()
	{
		return 1;
	}

	/* Report a single time column (point-to-point style) */
	protected boolean singleTimeColumn()
	{
		return false;
	}

	/* Use the narrow benchmark header separator */
	protected boolean compactHeader()
	{
		return singleTimeColumn() || singleIteration();
	}

	/* Messages moved per process and iteration, used for bandwidth */
	protected int bandwidthMultiplier()
	{
		return 1;
	}

	/* Whether the result table includes a bandwidth column */
	protected boolean reportsBandwidth()
	{
		return true;
	}

   	protected static double Wtime()
        {
                return (double) (System.nanoTime()/MicroBench.tento9);
        }

	public void init(String[] argv) throws MicroBenchException
	{
		try
		{
			args = MPI.Init(argv);
        	        this.rank = MPI.COMM_WORLD.Rank();
                	this.nprocess = MPI.COMM_WORLD.Size();

			MicroBench.MIN_SIZE = 1;
			MicroBench.MAX_SIZE = 4*MEGA;
			MicroBench.MESSAGE_SIZES = null;
			MicroBench.WARM_UP = true;
			MicroBench.SYNC = true;
			MicroBench.ROOT_SHIFT = false;
			MicroBench.WINDOW_SIZE = 256;
			MicroBench.MSGWR = true;
			MicroBench.MSGRD = true;
			MicroBench.PAUSE_USEC = 100000;
			MicroBench.SUPPORTS_TOUCH = this.supportsTouchOptions();
			MicroBench.SUPPORTS_MPI1_OPTIONS = this.supportsMpi1Options();
			MicroBench.MAX_MEMORY_ALLOC = 1.0;
			MicroBench.MAX_TIME = 10.0;
			MicroBench.ITERS = this.defaultIterations();
			MicroBench.OVERALL_VOL = this.defaultOverallVolumeMiB()*MEGA;
			MicroBench.CSV_OUTPUT = false;
			MicroBench.CSV_OUTPUT_FILE = null;
			MicroBench.csvWriter = null;

			/* Informational options work even with too few ranks. */
			MicroBench.processInformationalOptions(args,this.rank);

			if(this.nprocess < this.minRanks() ||
			   (this.requiresEvenRanks() && (this.nprocess % 2) != 0) ||
			   !this.validRankCount(this.nprocess))
			{
				if(this.rank == 0)
				{
					System.out.println(" ");
					System.out.println("# !! Benchmark "+this.suiteAndName()+" invalid for "+this.nprocess+" processes !!");
					System.out.println(" ");
				}
				MPI.Finalize();
                                System.exit(0);
			}

			MicroBench.processInput(args,this.rank);

			if(MicroBench.CSV_OUTPUT && MicroBench.CSV_OUTPUT_FILE == null)
				MicroBench.CSV_OUTPUT_FILE = this.defaultCsvFileName();

			if(!this.needsBuffers())
				return;

    			this.datatype_size = MicroBench.getDatatypeSizeInBytes(this.datatype);
	                this.bufferCount = MAX_SIZE/datatype_size;
		}
		catch(Exception e)
		{
			throw new MicroBenchException(e);
		}
	}

	public void run(long sendCount, long recvCount) throws MicroBenchException
	{
		try
		{
			if(this.needsBuffers())
	                {
				long bufferCountA = sendCount * this.sendBufferMultiplier();
				long bufferCountB = recvCount;

				MicroBench.checkRequiredMemory(bufferCountA, bufferCountB, this.datatype_size, this.nprocess);
				A = MicroBench.createBuffer(this.datatype, bufferCountA);
	        	        B = MicroBench.createBuffer(this.datatype, recvCount);
			}

			/*
			 * MPJ Express' tree-based Gather dereferences recvbuf on every
			 * rank, even when rank != root, so allocate the full timing
			 * array on all ranks. FastMPJ only uses it on the root.
			 */
			times = new double[this.nprocess];

                	if(this.rank==0)
			{
                        	this.printHeader();
			}

			int[] sizes = messageSizes();

			//BENCHMARK
	                for (int sizeIndex=0; sizeIndex<sizes.length; sizeIndex++)
        	        {
				int size = sizes[sizeIndex];
				int iters = iterationCount(size);

				if(this.customSamplePreparation())
				{
					this.samplePause();
					this.sampleBarrier();
				}

				if(MicroBench.WARM_UP && this.warmUpEachSize())
				{
					int warmupIters = this.warmupIterations(iters);

					if(warmupIters > 0)
					{
						Benchrun(size, warmupIters);

						/* IMB-P2P synchronizes after warmup; IMB-MPI1 does not. */
						if(this.customSamplePreparation())
							this.sampleBarrier();
					}
				}

				int measuredIters = this.measure(size, iters);

				if(this.rank==0)
                        	        this.printResults(size, measuredIters, this.times);

				if(this.singleIteration())
					break;

				if(!this.customSamplePreparation())
                	        	MPI.COMM_WORLD.Barrier();
         		}

			MPI.COMM_WORLD.Barrier();
			if(this.rank==0)
			{
				System.out.println(" ");System.out.println(" ");
				System.out.println("# All processes entering MPI.Finalize");
				System.out.println(" ");
			}

			MPI.Finalize();
		}
		catch(MPIException e)
		{
			throw new MicroBenchException(e);
		}
		finally
		{
			this.closeCsv();
		}
	}

	/* Private Methods */
	private int measure(int size, int iters) throws MPIException
	{
		double startwtime = MicroBench.Wtime();
		Benchrun(size, iters);
		double sampleTime = MicroBench.Wtime() - startwtime;

		if(this.customSamplePreparation())
		{
			this.samplePause();
			this.sampleBarrier();
		}
		else
			MPI.COMM_WORLD.Barrier();

		MPI.COMM_WORLD.Gather(this.myTime, 0, 1, MPI.DOUBLE, this.times, 0, 1, MPI.DOUBLE, 0);

		if(!this.usesTimeLimit() || MicroBench.MAX_TIME <= 0.0)
			return iters;

		double[] localTime = new double[1];

		for(int attempt = 0; attempt < 2; attempt++)
		{
			double[] maxTime = new double[1];
			localTime[0] = sampleTime;
			MPI.COMM_WORLD.Allreduce(localTime, 0, maxTime, 0, 1, MPI.DOUBLE, MPI.MAX);

			if(maxTime[0] <= MicroBench.MAX_TIME || iters <= 1)
				break;

			int limited = (int)Math.floor(iters * (MicroBench.MAX_TIME / maxTime[0]));

			if(limited < 1)
				limited = 1;

			if(limited >= iters)
				break;

			iters = limited;
			startwtime = MicroBench.Wtime();
			Benchrun(size, iters);
			sampleTime = MicroBench.Wtime() - startwtime;
			MPI.COMM_WORLD.Barrier();
			MPI.COMM_WORLD.Gather(this.myTime, 0, 1, MPI.DOUBLE, this.times, 0, 1, MPI.DOUBLE, 0);
		}

		return iters;
	}

	static int iterationCount(int size)
	{
		if(size <= 0)
			return MicroBench.ITERS;

		return Math.max(1, Math.min((MicroBench.OVERALL_VOL/size), MicroBench.ITERS));
	}

	static int[] messageSizes()
	{
		if(MicroBench.MESSAGE_SIZES != null)
			return MicroBench.MESSAGE_SIZES;

		int count = 0;

		for(long size = MicroBench.MIN_SIZE; size <= MicroBench.MAX_SIZE; size *= MicroBench.INCR_SIZE)
			count++;

		int[] sizes = new int[count];
		int index = 0;

		for(long size = MicroBench.MIN_SIZE; size <= MicroBench.MAX_SIZE; size *= MicroBench.INCR_SIZE)
			sizes[index++] = (int)size;

		return sizes;
	}

	private static void checkRequiredMemory(long bufferCountA, long bufferCountB, int datatype_size, int nprocess)
	{
		//Size in Gigabytes
		double bufferSizeA = (double)((bufferCountA*(datatype_size*1.0))/(MEGA_D*1024.0));
		double bufferSizeB = (double)((bufferCountB*(datatype_size*1.0))/(MEGA_D*1024.0));

		if(bufferSizeA > MicroBench.MAX_MEMORY_ALLOC)
			throw new MicroBenchException("The size of one of the required buffers to run this microbenchmark with <"+nprocess+"> processes is <"+bufferSizeA+"> GiB. Try to reduce the maximum message size with -e|-E or increase -mem");
		if(bufferSizeB > MicroBench.MAX_MEMORY_ALLOC)
			throw new MicroBenchException("The size of one of the required buffers to run this microbenchmark with <"+nprocess+"> processes is <"+bufferSizeB+"> GiB. Try to reduce the maximum message size with -e|-E or increase -mem");
	}

	private void printHeader()
	{
		System.out.println("#------------------------------------------------------------");
		System.out.println("#                "+MicroBench.NAME+" "+MicroBench.VERSION);
		System.out.println("#------------------------------------------------------------");
		System.out.println("# Date                  : "+new java.util.Date().toString());
		System.out.println("# Machine               : "+System.getProperty("os.arch"));
		System.out.println("# System                : "+System.getProperty("os.name"));
		System.out.println("# Release               : "+System.getProperty("os.version"));
		System.out.println("# JVM Vendor            : "+System.getProperty("java.vm.vendor"));
		System.out.println("# JVM Name              : "+System.getProperty("java.vm.name"));
		System.out.println("# JVM Version           : "+System.getProperty("java.version"));
		System.out.println("# JAVA_HOME             : "+System.getProperty("java.home"));
 		System.out.print("# MPJ Thread Environment: ");

		int level = MPI.queryThread();

		switch(level)
		{
			case MPI.THREAD_SINGLE:		System.out.println("MPI.THREAD_SINGLE");break;
  			case MPI.THREAD_FUNNELED:	System.out.println("MPI_THREAD_FUNNELED");break;
  			case MPI.THREAD_SERIALIZED:	System.out.println("MPI_THREAD_SERIALIZED\n");break;
  			case MPI.THREAD_MULTIPLE:	System.out.println("MPI_THREAD_MULTIPLE");break;
			default:			System.out.println("MPI.THREAD_SINGLE");break;
		}

		System.out.println(" ");
		System.out.println("# Calling sequence was:");
		System.out.println(" ");
		System.out.print("# "+this.suiteAndName()+" ");
  		for (int i = 0; i < args.length; i++)
                	System.out.print(args[i]+" ");

		System.out.println(" ");
		System.out.println(" ");
		System.out.println("# Minimum message length in bytes: "+MicroBench.MIN_SIZE);
		System.out.println("# Maximum message length in bytes: "+MicroBench.MAX_SIZE);
		System.out.println("#");

		System.out.println("# MPI_Datatype                   : MPI.BYTE");
                System.out.println("# MPI_Datatype for reductions    : MPI.FLOAT");
		System.out.println("# MPI.Op                         : MPI.SUM");
		System.out.println(" ");


		String separator = this.compactHeader()
			? "#------------------------------------------------------------"
			: "#------------------------------------------------------------------------------------------";

		System.out.println(separator);
                System.out.println("# Benchmarking "+this.benchmarkTitle());
                System.out.println("# #processes = "+this.nprocess);
		System.out.println(separator);
	}

	/* Printable benchmark identity, including the IMB suite */
	protected String suiteAndName()
	{
		return this.getClass().getName();
	}

	/* Benchmark title shown in the report header */
	protected String benchmarkTitle()
	{
		return this.getClass().getName();
	}

	protected void printHeaderResults()
	{
		if(singleTimeColumn())
			System.out.println(" #bytes \t #repetitions \t t[usec] \t Mbytes/sec");
		else if(singleIteration())
			System.out.println(" #repetitions \t t_min[usec] \t t_max[usec] \t t_avg[usec]");
		else if(reportsBandwidth())
			System.out.println(" #bytes \t #repetitions \t t_min[usec] \t t_max[usec] \t t_avg[usec] \t Mbytes/sec");
		else
			System.out.println(" #bytes \t #repetitions \t t_min[usec] \t t_max[usec] \t t_avg[usec]");
	}

	/* Default CSV file: one file per benchmark class. */
	protected String defaultCsvFileName()
	{
		return this.getClass().getName().replace('.', '_') + ".csv";
	}

	/* CSV header for the base result table. Overridden by each family. */
	protected void printCsvHeader()
	{
		if(singleTimeColumn())
			writeCsvHeader("size_bytes,repetitions,t_us,mb_per_sec");
		else if(singleIteration())
			writeCsvHeader("repetitions,t_min_us,t_max_us,t_avg_us");
		else if(reportsBandwidth())
			writeCsvHeader("size_bytes,repetitions,t_min_us,t_max_us,t_avg_us,mb_per_sec");
		else
			writeCsvHeader("size_bytes,repetitions,t_min_us,t_max_us,t_avg_us");
	}

	/* Print the table and CSV headers once, before the first result row. */
	protected void printResultsHeader()
	{
		if(resultsHeaderPrinted)
			return;

		resultsHeaderPrinted = true;
		printHeaderResults();
		printCsvHeader();
	}

	private void openCsv()
	{
		if(!MicroBench.CSV_OUTPUT || this.rank != 0 || MicroBench.csvWriter != null)
			return;

		if(MicroBench.CSV_OUTPUT_FILE == null)
			MicroBench.CSV_OUTPUT_FILE = this.defaultCsvFileName();

		try
		{
			MicroBench.csvWriter = new PrintWriter(new FileWriter(MicroBench.CSV_OUTPUT_FILE));
		}
		catch(IOException e)
		{
			throw new MicroBenchException("Could not write CSV file <"+MicroBench.CSV_OUTPUT_FILE+">", e);
		}
	}

	protected void writeCsvHeader(String header)
	{
		if(!MicroBench.CSV_OUTPUT || this.rank != 0)
			return;

		openCsv();
		MicroBench.csvWriter.println(header);
		MicroBench.csvWriter.flush();
	}

	protected void writeCsvRow(String row)
	{
		if(!MicroBench.CSV_OUTPUT || this.rank != 0)
			return;

		openCsv();
		MicroBench.csvWriter.println(row);
		MicroBench.csvWriter.flush();
	}

	void closeCsv()
	{
		if(MicroBench.csvWriter == null)
			return;

		MicroBench.csvWriter.close();
		MicroBench.csvWriter = null;
	}

	protected void printResults(int size, int iters, double[] times)
  	{
		double sum = 0.0;

		printResultsHeader();

		double[] sorted = times.clone();
		java.util.Arrays.sort(sorted);

		//seconds2micro-seconds
		for(int i=0;i<sorted.length;i++)
		{
			sorted[i] = (double) 1E6*sorted[i];
			sum = (double) sum + sorted[i];
		}

		tmin = (double) sorted[0];
		tmax = (double) sorted[sorted.length-1];
		tavg = (double) (sum/sorted.length);

		if(singleIteration())
		{
	     		System.out.format(java.util.Locale.ENGLISH," %7d \t %8.2f \t %8.2f \t %8.2f\n",iters,tmin,tmax,tavg);
			writeCsvRow(String.format(Locale.ENGLISH,"%d,%.6f,%.6f,%.6f",iters,tmin,tmax,tavg));
			return ;
		}

		if(!reportsBandwidth())
		{
	     		System.out.format(java.util.Locale.ENGLISH,"%7d \t %7d \t %8.2f \t %8.2f \t %8.2f\n",size,iters,tmin,tmax,tavg);
			writeCsvRow(String.format(Locale.ENGLISH,"%d,%d,%.6f,%.6f,%.6f",size,iters,tmin,tmax,tavg));
			return;
		}

		/*
		IMB reports Mbytes/sec as decimal megabytes per second: size bytes
		divided by the slowest time in microseconds gives bytes/usec, which
		is exactly 1000000 bytes/second (1 Mbyte/second in IMB).
		The maximum time is used for the bandwidth, as in IMB.
		*/
		double reportTime = tmax;
		bw = (double) ((bandwidthMultiplier()*size) / reportTime);

		if(singleTimeColumn())
		{
	     		System.out.format(java.util.Locale.ENGLISH,"%7d \t %7d \t %8.2f \t %8.2f\n",size,iters,reportTime,bw);
			writeCsvRow(String.format(Locale.ENGLISH,"%d,%d,%.6f,%.6f",size,iters,reportTime,bw));
		}
		else
		{
	     		System.out.format(java.util.Locale.ENGLISH,"%7d \t %7d \t %8.2f \t %8.2f \t %8.2f \t %8.2f\n",size,iters,tmin,tmax,tavg,bw);
			writeCsvRow(String.format(Locale.ENGLISH,"%d,%d,%.6f,%.6f,%.6f,%.6f",size,iters,tmin,tmax,tavg,bw));
		}
	}

	private static Object createBuffer(Datatype datatype, long count) throws MicroBenchException
	{
		if(count <= 0) 
			throw new MicroBenchException("Invalid count specified ("+count+")");

		if(datatype == MPI.BYTE)
			return new byte[(int)count];
 		else if(datatype == MPI.FLOAT)
			return new float[(int)count];
 		else if(datatype == MPI.DOUBLE)
			return new double[(int)count];
 		else if(datatype == MPI.INT)
			return new int[(int)count];
		else
			throw new MicroBenchException("Invalid Datatype specified");
	}

	private static int getDatatypeSizeInBytes(Datatype datatype)
	{
		if(datatype == MPI.BYTE)
                        return Byte.SIZE/8;
                else if(datatype == MPI.FLOAT)
                        return Float.SIZE/8;
                else if(datatype == MPI.DOUBLE)
                        return Double.SIZE/8;
                else if(datatype == MPI.INT)
                        return Integer.SIZE/8;
                else
                        throw new MicroBenchException("Invalid Datatype specified");
	}

	private static void checkTouchOption(String option, int rank) throws MPIException
	{
		if(MicroBench.SUPPORTS_TOUCH)
			return;

		if(rank == 0)
			System.out.println("Option "+option+" is only valid for IMB-P2P benchmarks");

		MPI.Finalize();
		System.exit(0);
	}

	private static void checkMpi1Option(String option, int rank) throws MPIException
	{
		if(MicroBench.SUPPORTS_MPI1_OPTIONS)
			return;

		if(rank == 0)
			System.out.println("Option "+option+" is only valid for IMB-MPI1 benchmarks");

		MPI.Finalize();
		System.exit(0);
	}

	static double parsePositiveDouble(String value, String option)
	{
		try
		{
			double parsed = Double.parseDouble(value);

			if(parsed <= 0.0 || Double.isNaN(parsed) || Double.isInfinite(parsed))
				throw new NumberFormatException();

			return parsed;
		}
		catch(NumberFormatException e)
		{
			throw new MicroBenchException("Invalid value for "+option+": "+value+". It must be a positive number");
		}
	}

	static int parsePositiveInt(String value, String option)
	{
		try
		{
			int parsed = Integer.parseInt(value);

			if(parsed <= 0)
				throw new NumberFormatException();

			return parsed;
		}
		catch(NumberFormatException e)
		{
			throw new MicroBenchException("Invalid value for "+option+": "+value+". It must be a positive integer");
		}
	}

	static int parseVolumeMiB(String value, String option)
	{
		int mib = parsePositiveInt(value, option);
		long bytes = (long)mib * MEGA;

		if(bytes > Integer.MAX_VALUE)
			throw new MicroBenchException("Invalid value for "+option+": "+value+" MiB is too large");

		return (int)bytes;
	}

	static boolean parseOnOff(String value, String option)
	{
		String v = value.toLowerCase(java.util.Locale.ENGLISH);

		if(v.equals("on") || v.equals("true") || v.equals("yes") || v.equals("1"))
			return true;

		if(v.equals("off") || v.equals("false") || v.equals("no") || v.equals("0"))
			return false;

		throw new MicroBenchException("Invalid value for "+option+": "+value+". Expected on or off");
	}

	static void parseIter(String value)
	{
		String[] values = value.split(",", -1);

		if(values.length < 1 || values.length > 3)
			throw new MicroBenchException("Invalid value for -iter: "+value+". Expected msgspersample[,overall_vol[,msgs_nonaggr]]");

		MicroBench.ITERS = parsePositiveInt(values[0], "-iter");

		if(values.length >= 2)
			MicroBench.OVERALL_VOL = parseVolumeMiB(values[1], "-iter");

		if(values.length >= 3)
			parsePositiveInt(values[2], "-iter");
	}

	static int[] parseSizeList(String value, String option)
	{
		String[] values = value.split(",", -1);
		int[] sizes = new int[values.length];
		int maxSize = 0;
		int maxAllowed = MicroBench.MAX_MESSAGE_SIZE * MicroBench.MEGA;

		for(int i = 0; i < values.length; i++)
		{
			sizes[i] = parsePositiveInt(values[i], option);

			if(sizes[i] > maxAllowed)
				throw new MicroBenchException("Invalid value for "+option+": "+sizes[i]+" exceeds "+maxAllowed+" bytes");

			if(sizes[i] > maxSize)
				maxSize = sizes[i];
		}

		MicroBench.MIN_SIZE = sizes[0];
		MicroBench.MAX_SIZE = maxSize;

		return sizes;
	}

	static void parseMsgLog(String value)
	{
		String[] values = value.split(":", -1);
		int minLog;
		int maxLog;
		int maxLogAllowed = Integer.numberOfTrailingZeros(MicroBench.MAX_MESSAGE_SIZE * MicroBench.MEGA);

		if(values.length == 1)
		{
			minLog = 0;
			maxLog = parsePositiveIntOrZero(values[0], "-msglog");
		}
		else if(values.length == 2)
		{
			minLog = parsePositiveIntOrZero(values[0], "-msglog");
			maxLog = parsePositiveIntOrZero(values[1], "-msglog");
		}
		else
		{
			throw new MicroBenchException("Invalid value for -msglog: "+value+". Expected min:max");
		}

		if(minLog > maxLog || maxLog > maxLogAllowed)
			throw new MicroBenchException("Invalid value for -msglog: "+value+". Expected 0 <= min <= max <= "+maxLogAllowed);

		MicroBench.MESSAGE_SIZES = null;
		MicroBench.MIN_SIZE = (minLog == 0) ? 1 : (1 << minLog);
		MicroBench.MAX_SIZE = 1 << maxLog;
	}

	static int parsePositiveIntOrZero(String value, String option)
	{
		try
		{
			int parsed = Integer.parseInt(value);

			if(parsed < 0)
				throw new NumberFormatException();

			return parsed;
		}
		catch(NumberFormatException e)
		{
			throw new MicroBenchException("Invalid value for "+option+": "+value+". It must be a non-negative integer");
		}
	}

	private static void processInformationalOptions(String args[], int rank) throws MPIException
	{
		for (int i = 0; i < args.length; i++) 
		{
			if(args[i].equals("-h") || args[i].equals("-H") || args[i].equals("-help")) 
			{
				if(rank==0) MicroBench.printUsage();
				MPI.Finalize();
				System.exit(0);
			}
			if(args[i].equals("-v") || args[i].equals("-V"))
			{
				if(rank==0)
				{
					System.out.println("#------------------------------------------------------------");
					System.out.println("#                "+MicroBench.NAME+" "+MicroBench.VERSION);			                
					System.out.println("#------------------------------------------------------------");
				}
				MPI.Finalize();
				System.exit(0);
			}
			if(args[i].equals("-benchmarks") || args[i].equals("-B") || args[i].equals("-list"))
			{
				if(rank==0) MicroBench.printBenchmarksNames();
				MPI.Finalize();
				System.exit(0);
			}
		}
	}

	private static void processInput(String args[], int rank)
	{
		try
		{		
			for (int i = 0; i < args.length; i++) 
			{
				if(args[i].equals("-s") || args[i].equals("-S"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.MESSAGE_SIZES = null;
					MicroBench.MIN_SIZE = new Integer(args[i+1]).intValue();
					if(MicroBench.MIN_SIZE<=0 || MicroBench.MIN_SIZE > MicroBench.MAX_SIZE)
					{
						throw new MicroBenchException("The minimum message size specified ("+MicroBench.MIN_SIZE+") is invalid. It must be >=1 and <="+MicroBench.MAX_SIZE);
					}
					i++;
				}
				else if (args[i].equals("-e") || args[i].equals("-E")) 
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.MESSAGE_SIZES = null;
					MicroBench.MAX_SIZE = new Integer(args[i+1]).intValue();
					if(MicroBench.MAX_SIZE/MEGA_D > MicroBench.MAX_MESSAGE_SIZE || MicroBench.MAX_SIZE < MicroBench.MIN_SIZE)
					{
						throw new MicroBenchException("The maximum message size specified ("+MicroBench.MAX_SIZE+") is invalid. It must be >="+MicroBench.MIN_SIZE+" and <="+MicroBench.MAX_MESSAGE_SIZE*MEGA);
					}
					i++;
				}
				else if (args[i].equals("-i") || args[i].equals("-I")) 
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.ITERS = new Integer(args[i+1]).intValue();
					if(MicroBench.ITERS<=0)
					{
						throw new MicroBenchException("The iterations parameter is invalid ("+MicroBench.ITERS+"). It must be >0");
					}
					i++;
				}
				else if (args[i].equals("-o") || args[i].equals("-O"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.OVERALL_VOL = MicroBench.parseVolumeMiB(args[i+1], "-o");
					i++;
				}
				else if (args[i].equals("-iter"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.parseIter(args[i+1]);
					i++;
				}
				else if (args[i].equals("-mem"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.checkMpi1Option("-mem", rank);
					MicroBench.MAX_MEMORY_ALLOC = MicroBench.parsePositiveDouble(args[i+1], "-mem");
					i++;
				}
				else if (args[i].equals("-time"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.checkMpi1Option("-time", rank);
					MicroBench.MAX_TIME = MicroBench.parsePositiveDouble(args[i+1], "-time");
					i++;
				}
				else if (args[i].equals("-sync"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.checkMpi1Option("-sync", rank);
					MicroBench.SYNC = MicroBench.parseOnOff(args[i+1], "-sync");
					i++;
				}
				else if (args[i].equals("-root_shift"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.checkMpi1Option("-root_shift", rank);
					MicroBench.ROOT_SHIFT = MicroBench.parseOnOff(args[i+1], "-root_shift");
					i++;
				}
				else if (args[i].equals("-warm_up"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.WARM_UP = MicroBench.parseOnOff(args[i+1], "-warm_up");
					i++;
				}
				else if (args[i].equals("-msgwr"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.checkTouchOption("-msgwr", rank);
					MicroBench.MSGWR = MicroBench.parseOnOff(args[i+1], "-msgwr");
					i++;
				}
				else if (args[i].equals("-msgrd"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.checkTouchOption("-msgrd", rank);
					MicroBench.MSGRD = MicroBench.parseOnOff(args[i+1], "-msgrd");
					i++;
				}
				else if (args[i].equals("-pause"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.checkTouchOption("-pause", rank);
					MicroBench.PAUSE_USEC = MicroBench.parsePositiveIntOrZero(args[i+1], "-pause");
					i++;
				}
				else if (args[i].equals("-msgsz"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.MESSAGE_SIZES = MicroBench.parseSizeList(args[i+1], "-msgsz");
					i++;
				}
				else if (args[i].equals("-msgsize"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.MESSAGE_SIZES = MicroBench.parseSizeList(args[i+1], "-msgsize");
					i++;
				}
				else if (args[i].equals("-msglog"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.parseMsgLog(args[i+1]);
					i++;
				}
				else if (args[i].equals("-window_size"))
				{
					if(i+1>=args.length)
					{
						if(rank==0) MicroBench.printUsage();
						MPI.Finalize();
						System.exit(0);
					}
					MicroBench.WINDOW_SIZE = new Integer(args[i+1]).intValue();
					if(MicroBench.WINDOW_SIZE<=0)
					{
						throw new MicroBenchException("The window size parameter is invalid ("+MicroBench.WINDOW_SIZE+"). It must be >0");
					}
					i++;
				}
				else if (args[i].equalsIgnoreCase("-csv"))
				{
					MicroBench.CSV_OUTPUT = true;

					if(i+1<args.length && !args[i+1].startsWith("-"))
						MicroBench.CSV_OUTPUT_FILE = args[++i];
				}
				else 
				{
					if(rank==0) MicroBench.printUsage();
					MPI.Finalize();
					System.exit(0);
				}
			}
		}
		catch(MPIException e)
		{
			throw new MicroBenchException(e);
		}
	}

	private static void printUsage() 
	{
			System.out.println(   
				"\n#########################################################################\n"+     
				"\n MB_MPJ.mpi1.BenchmarkName | MB_MPJ.p2p.BenchmarkName [OPTIONS]\n"+
				"\n OPTIONS "+
				"\n   -iter <msgspersample[,overall_vol[,msgs_nonaggr]]>"+
				"\n			IMB iteration control ("+MicroBench.ITERS+","+
				(MicroBench.OVERALL_VOL/MicroBench.MEGA)+",100)"+
				"\n			msgspersample: max. iterations per size"+
				"\n			overall_vol: overall volume in MiB"+
				(MicroBench.SUPPORTS_MPI1_OPTIONS ?
				 "\n   -mem <GB>"+
				 "\n			Max. memory per process for buffers ("+MicroBench.MAX_MEMORY_ALLOC+" GiB)"+
				 "\n   -time <seconds>"+
				 "\n			Max. runtime per message size ("+MicroBench.MAX_TIME+" s)"+
				 "\n   -sync <on|off>"+
				 "\n			Barrier after each collective iteration (on)"+
				 "\n   -root_shift <on|off>"+
				 "\n			Rotate the root of rooted collectives (off)" : "")+
				"\n   -msgsz <size>"+
				"\n			Single message size in bytes"+
				"\n   -msgsize <s1,s2,...>"+
				"\n			Explicit comma-separated message size list"+
				"\n   -msglog <min:max>"+
				"\n			Power-of-two message range (0 maps to 1 byte)"+
				"\n   -warm_up <on|off>"+
				"\n			IMB warmup (10% with a minimum of 10 for MPI1)"+
				"\n   -window_size <window_size>"+
				"\n			Messages in flight for Uniband/Biband ("+MicroBench.WINDOW_SIZE+")"+
				(MicroBench.SUPPORTS_TOUCH ?
				 "\n   -msgwr <on|off>"+
				 "\n			Touch send-buffer cache lines before sending (on)"+
				 "\n   -msgrd <on|off>"+
				 "\n			Touch receive-buffer cache lines after receiving (on)"+
				 "\n   -pause <microseconds>"+
				 "\n			Pause between samples ("+MicroBench.PAUSE_USEC+" us)" : "")+
				"\n   -s|-S <start_len>"+
				"\n			Starting length in bytes ("+MicroBench.MIN_SIZE+")"+
				"\n   -e|-E <end_len>"+
				"\n			Ending length in bytes ("+MicroBench.MAX_SIZE+")"+
				"\n   -i|-I <iterations>"+
				"\n			Alias for -iter msgspersample ("+MicroBench.ITERS+")"+
				"\n   -o|-O <overall_vol>"+
				"\n			Alias for -iter overall_vol in MiB ("+MicroBench.OVERALL_VOL/1048576+" MiB)"+
				"\n   -csv [file]"+
				"\n			Write the result table to CSV (default: <BenchmarkClass>.csv)"+
				"\n   -list|-B|-benchmarks"+
				"\n			View benchmarks names and exit"+
				"\n   -v|-V"+
				"\n			Print version and exit"+ 
				"\n   -help|-h|-H"+
				"\n			This help message"+ 
				"\n Note: Value in parentheses of each option is the default value"+ 
				"\n#########################################################################"+
				"\n");
	}

	private static void printBenchmarksNames()
	{
		System.out.println("1. IMB-MPI1: Single Transfers Benchmarks");
		System.out.println("\t "+MB_MPJ.mpi1.PingPong.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.PingPongAnySource.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.PingPing.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.PingPingAnySource.class.getName());
		System.out.println("2. IMB-MPI1: Collective Transfers Benchmarks");
		System.out.println("\t "+MB_MPJ.mpi1.Allgather.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Allgatherv.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Alltoall.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Alltoallv.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Allreduce.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Barrier.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Bcast.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Gather.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Gatherv.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Reduce.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Reduce_scatter.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Scatter.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Scatterv.class.getName());
		System.out.println("3. IMB-MPI1: Parallel Transfers Benchmarks");
		System.out.println("\t "+MB_MPJ.mpi1.Exchange.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Sendrecv.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Uniband.class.getName());
		System.out.println("\t "+MB_MPJ.mpi1.Biband.class.getName());
		System.out.println("4. IMB-P2P Benchmarks");
		System.out.println("\t "+MB_MPJ.p2p.PingPong.class.getName());
		System.out.println("\t "+MB_MPJ.p2p.PingPing.class.getName());
		System.out.println("\t "+MB_MPJ.p2p.Unirandom.class.getName());
		System.out.println("\t "+MB_MPJ.p2p.Birandom.class.getName());
		System.out.println("\t "+MB_MPJ.p2p.Corandom.class.getName());
		System.out.println("\t "+MB_MPJ.p2p.Stencil2D.class.getName());
		System.out.println("\t "+MB_MPJ.p2p.Stencil3D.class.getName());
		System.out.println("\t "+MB_MPJ.p2p.SendRecv_Replace.class.getName());
	}
}
