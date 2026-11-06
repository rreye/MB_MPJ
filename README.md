Message-Passing in Java (MPJ) Benchmarks Suite
----------------------------------------------

The code included in this microbenchmark suite is an independent Java
reimplementation of the benchmark suite provided by Intel(R) MPI Benchmarks:
https://github.com/intel/mpi-benchmarks

QUICK START GUIDE:

MB_MPJ requires an implementation of the mpiJava API. The examples in this
README use FastMPJ; other implementations such as MPJ Express can be used by
adapting the Maven dependency and the launch command.


APPENDIX 1: BUILDING MB_MPJ

1. Requirements:

   - JDK 11 or newer.
   - Maven 3.6 or newer.
   - An implementation of the mpiJava API (for example FastMPJ or MPJ Express).
     The POM defaults to FastMPJ 1.0.7 through the `mpj.groupId`,
     `mpj.artifactId` and `mpj.version` properties; override them for another
     implementation.

The current build has been tested with FastMPJ 1.0.7 on Linux.

This repository contains the Java sources, Maven build, documentation and
license files. The upstream IMB C/C++ sources and local development scripts are
not included.

2. Build the benchmark suite:

-bash-3.2$ mvn clean package

The jar is created at target/mbmpj.jar.

Optionally, `mvn test` runs the unit tests (option parsing, size and iteration
sweep, CSV formatting, IMB-P2P generator and stencil grids). They do not need
an MPI runtime.

3. Set MBMPJ_HOME to the project root:

-bash-3.2$ export MBMPJ_HOME=/path/to/MB_MPJ


APPENDIX 2: RUNNING MB_MPJ BENCHMARKS

The examples below assume FastMPJ. With another mpiJava implementation, use
its launcher and classpath conventions.

1. Define the machines file needed to run the microbenchmarks with your
mpiJava implementation. For example, with FastMPJ the "machines" file could
be:
        compute-0-0.local
        compute-0-1.local

2. Use the appropriate command to launch parallel programs with your mpiJava
implementation and run the desired benchmark. For example, with FastMPJ:

-bash-3.2$ fmpjrun -np 2 -hostfile machines -cp $MBMPJ_HOME/target/mbmpj.jar -class MB_MPJ.mpi1.PingPong

Or also:

-bash-3.2$ fmpjrun -np 2 -hostfile machines -cp $MBMPJ_HOME/target/classes -class MB_MPJ.mpi1.PingPong

It will run the IMB-MPI1 PingPong benchmark with 2 processes.

IMB-P2P benchmarks live in their own package and use every process. The
PingPong/PingPing pair requires an even number of processes; other P2P
benchmarks have their own rank requirements:

-bash-3.2$ fmpjrun -np 4 -hostfile machines -cp $MBMPJ_HOME/target/mbmpj.jar -class MB_MPJ.p2p.PingPong

3. To see all available options run any benchmark with -help parameter:

-bash-3.2$ fmpjrun -np 2 -hostfile machines -cp $MBMPJ_HOME/target/mbmpj.jar -class MB_MPJ.mpi1.PingPong -help

The main options use the IMB names and defaults: -iter, -msgsz/-msgsize,
-msglog, -warm_up and -window_size. The old -s/-e/-i/-o forms remain as
aliases; -i/-o map to the first and second values of -iter, and -o is in MiB.
IMB-P2P benchmarks also accept -msgwr and -msgrd (both on by default) to touch
the send/receive buffer cache lines, and -pause (microseconds between samples,
default 100000). IMB-MPI1 benchmarks accept -mem (GiB per
process, default 1.0), -time (seconds per message size, default 10.0), -sync
(barrier after each collective iteration, on by default) and -root_shift
(rotate the root of rooted collectives, off by default). The -off_cache option
is not implemented; passing it is rejected as an unknown option. All
benchmarks accept -csv [file] to write their result table to a CSV file (see
Appendix 4).


APPENDIX 3: AVAILABLE BENCHMARKS NAMES

MB_MPJ mirrors the IMB suite layout: IMB-MPI1 benchmarks are in the
MB_MPJ.mpi1 package and IMB-P2P benchmarks in MB_MPJ.p2p. Run any benchmark
with -list (or -benchmarks) to print this list.

1. IMB-MPI1: Single Transfers Benchmarks
        MB_MPJ.mpi1.PingPong
        MB_MPJ.mpi1.PingPongAnySource
        MB_MPJ.mpi1.PingPing
        MB_MPJ.mpi1.PingPingAnySource

2. IMB-MPI1: Collective Transfers Benchmarks
        MB_MPJ.mpi1.Allgather
        MB_MPJ.mpi1.Allgatherv
        MB_MPJ.mpi1.Alltoall
        MB_MPJ.mpi1.Alltoallv
        MB_MPJ.mpi1.Allreduce
        MB_MPJ.mpi1.Barrier
        MB_MPJ.mpi1.Bcast
        MB_MPJ.mpi1.Gather
        MB_MPJ.mpi1.Gatherv
        MB_MPJ.mpi1.Reduce
        MB_MPJ.mpi1.Reduce_scatter
        MB_MPJ.mpi1.Scatter
        MB_MPJ.mpi1.Scatterv

3. IMB-MPI1: Parallel Transfers Benchmarks
        MB_MPJ.mpi1.Exchange
        MB_MPJ.mpi1.Sendrecv
        MB_MPJ.mpi1.Uniband
        MB_MPJ.mpi1.Biband

4. IMB-P2P Benchmarks
        MB_MPJ.p2p.PingPong
        MB_MPJ.p2p.PingPing
        MB_MPJ.p2p.Unirandom
        MB_MPJ.p2p.Birandom
        MB_MPJ.p2p.Corandom
        MB_MPJ.p2p.Stencil2D
        MB_MPJ.p2p.Stencil3D
        MB_MPJ.p2p.SendRecv_Replace


APPENDIX 4: CSV OUTPUT

All benchmarks accept -csv [file] to write their result table to a CSV file.
Only rank 0 writes it, with one row per message size. Without a file name, the
default is the benchmark class name with dots replaced by underscores, for
example MB_MPJ_mpi1_PingPong.csv. The columns mirror the benchmark table:

- IMB-MPI1 PingPong/PingPing: size_bytes,repetitions,t_us,mb_per_sec
- IMB-MPI1 Exchange and Sendrecv:
  size_bytes,repetitions,t_min_us,t_max_us,t_avg_us,mb_per_sec
- IMB-MPI1 collective benchmarks without a bandwidth column:
  size_bytes,repetitions,t_min_us,t_max_us,t_avg_us
- IMB-MPI1 Barrier: repetitions,t_min_us,t_max_us,t_avg_us
- IMB-MPI1 Uniband/Biband: size_bytes,repetitions,mb_per_sec,msg_per_sec
- IMB-P2P PingPong/PingPing and SendRecv_Replace:
  size_bytes,repetitions,t_us,mb_per_sec,msg_per_sec
- IMB-P2P random and stencil benchmarks:
  size_bytes,repetitions,latency_us,mb_per_sec,msg_per_sec

LatencyBench uses a different, raw-sample schema. It runs with exactly 2
processes. Besides printing latency statistics, it can write the raw
measurements to a CSV file with -csv:

-bash-3.2$ fmpjrun -np 2 -cp $MBMPJ_HOME/target/mbmpj.jar -class MB_MPJ.LatencyBench -csv latency.csv

If no file is given, latency_results.csv is used. The CSV columns are:

        size_bytes,iteration,latency_us

The file contains one row per sample, so external tools can compute summary
statistics and build line, histogram, or box-plot charts from it.

FastMPJ note: some FastMPJ installations include lib/mpj-test.jar with old
MB_MPJ classes. FastMPJ loads that jar before the -cp jar, so make sure it is
not ahead of the current application jar.


APPENDIX 5: IMB BENCHMARK COVERAGE

MB_MPJ implements the MPI-1.2/1.3 subset of Intel MPI Benchmarks. The
benchmarks are organised like the IMB suite itself:

- MB_MPJ.mpi1: 21 IMB-MPI1 benchmarks (the 17 historical defaults,
  PingPongAnySource, PingPingAnySource, Uniband and Biband). IMB 2021.12 also
  includes Reduce_local and Reduce_scatter_block as defaults; see below.
- MB_MPJ.p2p: 8 IMB-P2P benchmarks (PingPong, PingPing, Unirandom,
  Birandom, Corandom, Stencil2D, Stencil3D and SendRecv_Replace).
- MB_MPJ.LatencyBench: an MB_MPJ-specific latency benchmark.

All planned IMB-MPI1 and IMB-P2P benchmarks that are implementable with
MPI-1.2/1.3 are covered; Reduce_scatter_block was explicitly discarded.

Mbytes/sec columns use decimal megabytes per second (10^6 bytes/s), exactly as
IMB does; they are not MiB/s. IMB-MPI1 collectives report only t_min/t_max/t_avg
and have no bandwidth column.

The two suites name their point-to-point benchmarks identically but measure
different things, so each has its own class:

- MB_MPJ.mpi1.PingPong and MB_MPJ.mpi1.PingPing follow IMB-MPI1: they use
  pair0=0 and pair1=nprocess-1 and require at least 2 processes. Any other
  process stays idle, as in IMB-MPI1, and the reported time is the maximum
  over the pair.
- MB_MPJ.p2p.PingPong and MB_MPJ.p2p.PingPing follow IMB-P2P: every process is
  paired with (rank + nprocess/2) % nprocess, an even number of processes is
  required, and rank 0 aggregates the timings into the IMB-P2P columns
  (#bytes #repetitions t[usec] Mbytes/sec Msg/sec).
- MB_MPJ.p2p.Unirandom, MB_MPJ.p2p.Birandom and MB_MPJ.p2p.Corandom use the
  same pseudo-random sequence as IMB-P2P, so every rank derives the same pairs
  or ring without exchanging extra information. Unirandom selects one
  sender/receiver pair per iteration, Birandom makes that pair exchange in both
  directions, and Corandom shuffles a ring where every rank sends to the next
  and receives from the previous. They need at least 2 processes and do not
  require an even number.
- MB_MPJ.p2p.Stencil2D and MB_MPJ.p2p.Stencil3D form a periodic 2D or 3D grid.
  Every process exchanges one message with each of its 4 or 6 neighbours using
  Irecv/Isend and Waitall. The number of processes must factor into a valid
  grid, as in IMB-P2P.
- MB_MPJ.p2p.SendRecv_Replace also follows IMB-P2P but uses a periodic chain:
  every process sends to (rank + 1) % nprocess and receives from
  (rank - 1 + nprocess) % nprocess with the same buffer. It needs at least 2
  processes and does not require an even number.
- MB_MPJ.mpi1.Uniband and MB_MPJ.mpi1.Biband follow IMB-MPI1: every process is
  paired with rank + nprocess/2, an even number of processes is required, and
  each pair moves a window of messages (-window_size, 256 by default) followed
  by a one-byte ack. Uniband sends in one direction; Biband sends and receives
  in both. Their output columns are
  (#bytes #repetitions Mbytes/sec Msg/sec).

Reduce_local cannot be implemented faithfully because MPI_Reduce_local is not
part of the mpiJava 1.2 API. Reduce_scatter_block was discarded: it is not
planned for implementation.

The remaining IMB suites require APIs that the mpiJava 1.2 API does not
expose: IMB-NBC (MPI-3 nonblocking collectives), IMB-RMA and IMB-EXT
(one-sided), IMB-IO (MPI-IO), IMB-MPI4 (persistent collectives) and IMB-MT
(threads).

See docs/IMB_BENCHMARKS.md for the full coverage matrix, the
per-suite inventory and the implementation roadmap.


LICENSE:

MB_MPJ is distributed under the MIT License. See the LICENSE file for the
full text.

MB_MPJ is an independent Java reimplementation; it is not affiliated with,
endorsed by, or sponsored by Intel, and its results are not official
Intel(R) MPI Benchmarks results. The original Intel MPI Benchmarks sources are
not included in this repository; they are available upstream at
https://github.com/intel/mpi-benchmarks and are licensed under the BSD
3-Clause License. See the `NOTICE` file for the third-party attribution and
license text.
