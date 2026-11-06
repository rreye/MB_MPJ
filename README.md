# MB_MPJ

**Message-Passing in Java (MPJ) Benchmarks Suite**

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

MB_MPJ is an independent Java reimplementation of the benchmark suite provided
by [Intel(R) MPI Benchmarks](https://github.com/intel/mpi-benchmarks), written
against the mpiJava API. It covers the IMB-MPI1 and IMB-P2P benchmarks that can
be expressed with MPI-1.2/1.3, keeps the IMB command-line names and defaults,
and adds optional CSV output.

- 21 IMB-MPI1 benchmarks, 8 IMB-P2P benchmarks and `LatencyBench`.
- Uses only MPI-1.2/1.3 features exposed by the mpiJava API.
- One standalone class with a `main` per benchmark.
- IMB-compatible options (`-iter`, `-msglog`, `-warm_up`, `-window_size`, ...).
- Optional CSV output, one file per benchmark.
- Unit tests for parsing, sizing, CSV and IMB-P2P logic; no MPI runtime needed.

## Using MB_MPJ

MB_MPJ is distributed as a prebuilt `mbmpj.jar` in the GitHub releases. You do
not need Maven or the source code to run the benchmarks.

1. Download `mbmpj.jar` from the latest release:
   <https://github.com/rreye/MB_MPJ/releases/latest>.
2. Install a mpiJava implementation. The examples below use FastMPJ 1.0.7, but
   any compatible implementation, such as MPJ Express, can be used.
3. Launch a benchmark with the implementation's launcher.

The jar does not bundle the mpiJava implementation. It is compiled against the
mpiJava API and uses the FastMPJ or MPJ Express classes already installed on
the system.

Runtime requirements:

| Component | Version |
|---|---|
| JDK | 8 or newer |
| mpiJava implementation | FastMPJ 1.0.7, MPJ Express, or another compatible mpiJava API |

The next section shows how to launch a benchmark.

## Running benchmarks

The examples use FastMPJ. With another mpiJava implementation, use its launcher
and classpath conventions. `MBMPJ_JAR` is the path to the downloaded release jar
(or to `target/mbmpj.jar` if you built from source):

```bash
export MBMPJ_JAR=/path/to/mbmpj.jar
```

FastMPJ needs a machines file listing the hosts, for example:

```text
compute-0-0.local
compute-0-1.local
```

Run a benchmark:

```bash
fmpjrun -np 2 -hostfile machines \
  -cp "$MBMPJ_JAR" \
  -class MB_MPJ.mpi1.PingPong
```

Show every option of a benchmark:

```bash
fmpjrun -np 2 -hostfile machines \
  -cp "$MBMPJ_JAR" \
  -class MB_MPJ.mpi1.PingPong -help
```

List the available benchmarks:

```bash
fmpjrun -np 2 -cp "$MBMPJ_JAR" \
  -class MB_MPJ.mpi1.PingPong -list
```

## Rank requirements

| Benchmark family | Required processes |
|---|---|
| IMB-MPI1 collectives (`Allgather`, `Bcast`, `Reduce`, ...) | No explicit constraint |
| `MB_MPJ.mpi1.PingPong` / `PingPing` (and `AnySource`) | At least 2; only ranks `0` and `n-1` are active |
| `MB_MPJ.mpi1.Exchange`, `Sendrecv` | No explicit constraint (use 2 or more for meaningful timings) |
| `MB_MPJ.mpi1.Uniband`, `Biband` | Even number, at least 2; pairs `r <-> r + n/2` |
| `MB_MPJ.p2p.PingPong` / `PingPing` | Even number, at least 2; pairs `r <-> (r + n/2) % n` |
| `MB_MPJ.p2p.Unirandom`, `Birandom`, `Corandom` | At least 2, any count |
| `MB_MPJ.p2p.SendRecv_Replace` | At least 2, any count |
| `MB_MPJ.p2p.Stencil2D` | A count that factors into a periodic 2D grid (4, 6, 8, 9, 10, ...) |
| `MB_MPJ.p2p.Stencil3D` | A count that factors into a periodic 3D grid (8, 12, 16, 18, ...) |
| `MB_MPJ.LatencyBench` | Exactly 2 |

If the count is not valid, rank 0 prints an invalid-process message and the
program exits with status 0.

## Benchmarks

### IMB-MPI1 (`MB_MPJ.mpi1`)

**Single transfers**

| Class | Description |
|---|---|
| `PingPong` | One active pair, sequential send/receive |
| `PingPongAnySource` | `PingPong` receiving with `MPI.ANY_SOURCE`/`MPI.ANY_TAG` |
| `PingPing` | One active pair, overlapped non-blocking exchange |
| `PingPingAnySource` | `PingPing` receiving with `MPI.ANY_SOURCE`/`MPI.ANY_TAG` |

**Collectives**

| Class | Description |
|---|---|
| `Allgather` | `MPI.BYTE`, all-gather |
| `Allgatherv` | `MPI.BYTE`, all-gather with variable counts |
| `Allreduce` | `MPI.FLOAT`, `MPI.SUM` |
| `Alltoall` | `MPI.BYTE`, all-to-all |
| `Alltoallv` | `MPI.BYTE`, all-to-all with variable counts |
| `Barrier` | One iteration per size |
| `Bcast` | `MPI.BYTE`, broadcast |
| `Gather` | `MPI.BYTE`, gather |
| `Gatherv` | `MPI.BYTE`, gather with variable counts |
| `Reduce` | `MPI.FLOAT`, `MPI.SUM` |
| `Reduce_scatter` | `MPI.FLOAT`, `MPI.SUM` |
| `Scatter` | `MPI.BYTE`, scatter |
| `Scatterv` | `MPI.BYTE`, scatter with variable counts |

**Parallel transfers**

| Class | Description |
|---|---|
| `Exchange` | Boundary exchange with left and right neighbours (4 messages per iteration) |
| `Sendrecv` | Periodic chain with `Sendrecv` (2 messages per iteration) |
| `Uniband` | Paired unidirectional bandwidth with a window of messages in flight |
| `Biband` | Like `Uniband`, bidirectional in each pair |

### IMB-P2P (`MB_MPJ.p2p`)

| Class | Description |
|---|---|
| `PingPong` | Every rank in a pair; rank 0 aggregates the timings |
| `PingPing` | Overlapped variant of `PingPong` |
| `Unirandom` | Random sender/receiver pair per iteration, one direction |
| `Birandom` | Random pair per iteration, bidirectional exchange |
| `Corandom` | Random ring where every rank sends and receives |
| `Stencil2D` | Periodic 2D grid, 4 neighbours |
| `Stencil3D` | Periodic 3D grid, 6 neighbours |
| `SendRecv_Replace` | Periodic chain with `Sendrecv_replace` |

### LatencyBench

`MB_MPJ.LatencyBench` is specific to MB_MPJ. It runs with exactly 2 processes
and reports mean, minimum, maximum and percentile latencies. With `-csv` it
also writes every raw sample.

## Command-line options

The main options use the IMB names and defaults. `All` means every `MicroBench`
benchmark; `LatencyBench` has its own smaller set, listed afterwards.

| Option | Applies to | Default / notes |
|---|---|---|
| `-iter <msgspersample[,overall_vol[,msgs_nonaggr]]>` | All | MPI1 `1000,40,100`; P2P `100000,800,100` |
| `-msgsz <size>` | All | Single message size in bytes (a comma-separated list is also accepted) |
| `-msgsize <s1,s2,...>` | All | Explicit comma-separated size list |
| `-msglog <min:max>` | All | Power-of-two range; `0` maps to 1 byte |
| `-warm_up <on\|off>` | All | At every size: MPI1 10% with a minimum of 10; P2P 10% with a minimum of 1 |
| `-window_size <n>` | `Uniband`, `Biband` | `256` messages in flight |
| `-csv [file]` | All | Write the result table to CSV (rank 0); `<Class>.csv` by default |
| `-s`, `-e` | All | Start/end size in bytes (`1`, `4194304`) |
| `-i`, `-o` | All | Aliases of the two first `-iter` fields; `-o` is in MiB |
| `-list`, `-benchmarks` | All | Print the benchmark names and exit |
| `-v` | All | Print the version and exit |
| `-help` | All | Print the usage message and exit |
| `-mem <GiB>` | IMB-MPI1 | `1.0` GiB per process |
| `-time <seconds>` | IMB-MPI1 | `10.0` s per message size |
| `-sync <on\|off>` | IMB-MPI1 | Barrier after each collective iteration (`on`) |
| `-root_shift <on\|off>` | IMB-MPI1 | Rotate the root of rooted collectives (`off`) |
| `-msgwr <on\|off>` | IMB-P2P | Touch send-buffer cache lines (`on`) |
| `-msgrd <on\|off>` | IMB-P2P | Touch receive-buffer cache lines (`on`) |
| `-pause <microseconds>` | IMB-P2P | Pause between samples (`100000`) |

Notes:

- `-mem` and `-time` are rejected in IMB-P2P benchmarks; `-msgwr`, `-msgrd`
  and `-pause` are rejected in IMB-MPI1 benchmarks.
- `-mem` aborts if the maximum buffers would exceed the limit. `-time` may
  reduce the iterations of a sample that runs too long.
- `-off_cache` is not implemented and is rejected as an unknown option.
- The pause and the dissemination barrier used by IMB-P2P are outside the
  measured interval.

`LatencyBench` accepts a smaller set of options:

| Option | Default |
|---|---|
| `-s <size>` | `1` |
| `-e <size>` | `4096` |
| `-i <iterations>` | `20000` |
| `-w <warmup_iterations>` | `50000` |
| `-csv [file]` | `latency_results.csv` |
| `-h` | Print usage |

## Output and CSV

`Mbytes/sec` columns use decimal megabytes per second (10^6 bytes/s), exactly
as IMB does; they are not MiB/s. IMB-MPI1 collectives report only
`t_min`/`t_max`/`t_avg` and have no bandwidth column.

Every `MicroBench` benchmark accepts `-csv [file]`. Only rank 0 writes, with one
row per message size and six decimal places. Without a file name, the default is
the class name with dots replaced by underscores, for example
`MB_MPJ_mpi1_PingPong.csv`. The columns mirror the stdout table:

| Benchmark family | CSV columns |
|---|---|
| IMB-MPI1 `PingPong`/`PingPing` | `size_bytes,repetitions,t_us,mb_per_sec` |
| IMB-MPI1 `Exchange`, `Sendrecv` | `size_bytes,repetitions,t_min_us,t_max_us,t_avg_us,mb_per_sec` |
| IMB-MPI1 collectives | `size_bytes,repetitions,t_min_us,t_max_us,t_avg_us` |
| IMB-MPI1 `Barrier` | `repetitions,t_min_us,t_max_us,t_avg_us` |
| IMB-MPI1 `Uniband`/`Biband` | `size_bytes,repetitions,mb_per_sec,msg_per_sec` |
| IMB-P2P `PingPong`/`PingPing`/`SendRecv_Replace` | `size_bytes,repetitions,t_us,mb_per_sec,msg_per_sec` |
| IMB-P2P random and stencil benchmarks | `size_bytes,repetitions,latency_us,mb_per_sec,msg_per_sec` |

`LatencyBench` uses its own raw-sample schema. One row per sample:

```text
size_bytes,iteration,latency_us
```

```bash
fmpjrun -np 2 -cp "$MBMPJ_JAR" \
  -class MB_MPJ.LatencyBench -csv latency.csv
```

This makes it easy to compute summary statistics or build line, histogram and
box-plot charts with external tools.

## FastMPJ note

Some FastMPJ installations ship `lib/mpj-test.jar` with old `MB_MPJ` classes.
FastMPJ places that jar before the `-cp` argument, so remove or rename it when
running the release jar (or a Maven-built jar); otherwise the stale benchmark
code is loaded.

## IMB coverage

MB_MPJ implements the MPI-1.2/1.3 subset of Intel MPI Benchmarks:

- `MB_MPJ.mpi1`: the 17 historical IMB-MPI1 defaults, the two `AnySource`
  variants, and `Uniband`/`Biband`.
- `MB_MPJ.p2p`: all 8 IMB-P2P benchmarks.
- `MB_MPJ.LatencyBench`: an MB_MPJ-specific latency benchmark.

All planned IMB-MPI1 and IMB-P2P benchmarks that can be implemented with
MPI-1.2/1.3 are covered. `Reduce_local` is not implementable with the mpiJava
1.2 API and `Reduce_scatter_block` was explicitly discarded. The remaining IMB
suites need APIs that mpiJava 1.2 does not expose: IMB-NBC (MPI-3 nonblocking
collectives), IMB-RMA and IMB-EXT (one-sided), IMB-IO (MPI-IO), IMB-MPI4
(persistent collectives) and IMB-MT (threads).

See [`docs/IMB_BENCHMARKS.md`](docs/IMB_BENCHMARKS.md) for the full coverage
matrix, the per-suite inventory and the implementation notes.

## Building from source

Building is only needed if you want to modify MB_MPJ or produce your own jar.
The prebuilt release jar is enough to run the benchmarks.

Build requirements:

| Component | Version |
|---|---|
| JDK | 11 or newer (the jar targets Java 8 bytecode) |
| Maven | 3.6 or newer |
| mpiJava implementation | FastMPJ 1.0.7 by default; MPJ Express or another mpiJava API can be used |

The POM declares the mpiJava API as a `provided` dependency. FastMPJ and MPJ
Express are not in Maven Central, so Maven needs the implementation jar
installed locally (or available from a repository you configure). For example,
with FastMPJ:

```bash
mvn install:install-file \
  -Dfile="$FMPJ_HOME/lib/mpj.jar" \
  -DgroupId=org.fastmpj -DartifactId=mpj -Dversion=1.0.7 \
  -Dpackaging=jar
```

To use another implementation, override the coordinates in `pom.xml`:

```xml
<mpj.groupId>org.fastmpj</mpj.groupId>
<mpj.artifactId>mpj</mpj.artifactId>
<mpj.version>1.0.7</mpj.version>
```

Then build and test:

```bash
mvn clean package    # -> target/mbmpj.jar
mvn test             # unit tests; no MPI runtime required
```

The current build is tested with FastMPJ 1.0.7 on Linux. You can also launch a
benchmark directly from `target/classes` after `mvn compile`.

The repository contains the Java sources, the Maven build, the documentation
and the license files. The upstream IMB C/C++ sources and the local development
scripts are not included.

### Repository layout

| Path | Contents |
|---|---|
| `src/main/java/MB_MPJ/` | Shared framework (`MicroBench`, CSV, `LatencyBench`) |
| `src/main/java/MB_MPJ/mpi1/` | IMB-MPI1 benchmarks |
| `src/main/java/MB_MPJ/p2p/` | IMB-P2P benchmarks |
| `src/test/java/` | Unit tests for parsing, sizing, CSV and IMB-P2P logic |
| `docs/IMB_BENCHMARKS.md` | IMB coverage matrix and implementation notes |

## License

MB_MPJ is distributed under the MIT License. See the [`LICENSE`](LICENSE) file
for the full text.

MB_MPJ is an independent Java reimplementation; it is not affiliated with,
endorsed by, or sponsored by Intel, and its results are not official
Intel(R) MPI Benchmarks results. The original Intel MPI Benchmarks sources are
not included in this repository; they are available upstream at
<https://github.com/intel/mpi-benchmarks> under the BSD 3-Clause License. See
the [`NOTICE`](NOTICE) file for the third-party attribution and license text.
