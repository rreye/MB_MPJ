# IMB benchmark coverage in MB_MPJ

Reference analysed: **Intel(R) MPI Benchmarks 2021.12** (upstream sources, not
included in this repository).

The `src_c/` and `src_cpp/` paths mentioned in this document are relative to
the upstream Intel MPI Benchmarks repository, not to this repository.

Target API: **mpiJava 1.2** (analysed with FastMPJ 1.0.7), that is, the
**MPI-1.2/1.3** subset exposed by `mpi.Comm`, `mpi.Intracomm`,
`mpi.Intercomm`, `mpi.Group`, `mpi.Datatype`, and so on.

Analysis date: 2026-11-05.

## 1. Criteria

A benchmark is considered **implementable** if every operation it measures is
available in MPI-1.2/1.3 and in the mpiJava API.

The following states are distinguished:

- **Covered**: a `MB_MPJ.mpi1.<Name>` or `MB_MPJ.p2p.<Name>` class implements
  the benchmark.
- **Equivalent**: the exact name does not exist, but the semantics are already
  covered by another class or can be expressed with an existing MPI-1.2 call.
- **Missing (implementable)**: it does not exist and can be implemented with
  MPI-1.2/1.3.
- **Not implementable**: it requires MPI-2, MPI-3 or MPI-4, or an API that
  mpiJava 1.2 does not expose.

Note on IMB-MPI1: the `ALLC` list in `src_c/IMB_bnames_mpi1.h` is outdated. The
real source of registered benchmarks is `src_cpp/MPI1/MPI1_benchmark.cpp`.
There are **25 benchmarks** there, of which **19 are marked as `DEFAULT`**.

## 2. What MB_MPJ currently implements

The structure mirrors IMB: IMB-MPI1 benchmarks live in the `MB_MPJ.mpi1`
package and IMB-P2P benchmarks in `MB_MPJ.p2p`. There are 29 benchmark classes
(21 from MPI1 and 8 from P2P) plus `LatencyBench`, which is specific to MB_MPJ:

| Class | Package | IMB origin | Notes |
|---|---|---|---|
| `PingPong` | `MB_MPJ.mpi1` | IMB-MPI1 | 2 active processes: `pair0=0`, `pair1=nprocs-1`; the rest idle |
| `PingPongAnySource` | `MB_MPJ.mpi1` | IMB-MPI1 | Same as `PingPong`, but receives with `MPI.ANY_SOURCE`/`MPI.ANY_TAG` |
| `PingPing` | `MB_MPJ.mpi1` | IMB-MPI1 | 2 active processes: `pair0=0`, `pair1=nprocs-1`; the rest idle |
| `PingPingAnySource` | `MB_MPJ.mpi1` | IMB-MPI1 | Same as `PingPing`, but receives with `MPI.ANY_SOURCE`/`MPI.ANY_TAG` |
| `PingPong` | `MB_MPJ.p2p` | IMB-P2P | All ranks in pairs; requires an even count |
| `PingPing` | `MB_MPJ.p2p` | IMB-P2P | All ranks in pairs; requires an even count |
| `Unirandom` | `MB_MPJ.p2p` | IMB-P2P | Random pair per iteration; one direction; reproducible RNG |
| `Birandom` | `MB_MPJ.p2p` | IMB-P2P | Random pair per iteration; bidirectional exchange |
| `Corandom` | `MB_MPJ.p2p` | IMB-P2P | Random ring; every rank sends and receives |
| `Stencil2D` | `MB_MPJ.p2p` | IMB-P2P | Periodic 2D grid; 4 neighbours; requires a factorable grid |
| `Stencil3D` | `MB_MPJ.p2p` | IMB-P2P | Periodic 3D grid; 6 neighbours; requires a factorable grid |
| `SendRecv_Replace` | `MB_MPJ.p2p` | IMB-P2P | Periodic chain with `Sendrecv_replace`; all ranks |
| `Sendrecv` | `MB_MPJ.mpi1` | IMB-MPI1 | Periodic chain |
| `Exchange` | `MB_MPJ.mpi1` | IMB-MPI1 | Exchange with left/right neighbours |
| `Uniband` | `MB_MPJ.mpi1` | IMB-MPI1 | Pairs `r <-> r+nprocs/2`; `-window_size` window; per-window ack |
| `Biband` | `MB_MPJ.mpi1` | IMB-MPI1 | Like `Uniband`, but bidirectional traffic in each pair |
| `Allreduce` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.FLOAT`, `MPI.SUM` |
| `Reduce` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.FLOAT`, `MPI.SUM` |
| `Reduce_scatter` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.FLOAT`, `MPI.SUM` |
| `Allgather` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.BYTE` |
| `Allgatherv` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.BYTE` |
| `Gather` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.BYTE` |
| `Gatherv` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.BYTE` |
| `Scatter` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.BYTE` |
| `Scatterv` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.BYTE` |
| `Alltoall` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.BYTE` |
| `Alltoallv` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.BYTE` |
| `Bcast` | `MB_MPJ.mpi1` | IMB-MPI1 | `MPI.BYTE` |
| `Barrier` | `MB_MPJ.mpi1` | IMB-MPI1 | A single iteration per size |
| `LatencyBench` | `MB_MPJ` | Not in IMB | Project-specific benchmark with CSV output |

`Scan` does not exist in IMB 2021.12 and has been removed from the project.

The two `PingPong`/`PingPing` implementations are not the same thing:
`MB_MPJ.mpi1` measures a single pair and reports `t[usec]`/`Mbytes/sec` (taking
the maximum of the pair, like IMB-MPI1), whereas `MB_MPJ.p2p` measures every
pair and aggregates on rank 0 to produce the IMB-P2P columns
(`#bytes #repetitions t[usec] Mbytes/sec Msg/sec`).

All `Mbytes/sec` columns follow IMB: they are **decimal MB**
(10^6 bytes/s), not MiB/s. IMB-MPI1 collectives show times only
(`t_min`, `t_max`, `t_avg`) and do not include bandwidth.

## 3. IMB-MPI1 coverage

IMB-MPI1 registers 25 benchmarks. MB_MPJ covers the 17 historical default
benchmarks, the two `AnySource` variants and `Uniband`/`Biband`; in addition,
`PingPongSpecificSource` and `PingPingSpecificSource` are equivalent to
`PingPong` and `PingPing`. No implementable IMB-MPI1 benchmarks remain pending
within the plan (`Reduce_scatter_block` is discarded). The other two default
benchmarks in IMB 2021.12 are `Reduce_local` and `Reduce_scatter_block`.

| IMB benchmark | Default | In MB_MPJ | State |
|---|---:|---|---|
| `PingPong` | Yes | `PingPong` | Covered |
| `PingPongSpecificSource` | No | `PingPong` | Equivalent: `PingPong` already uses a specific source |
| `PingPongAnySource` | No | `PingPongAnySource` | Covered |
| `PingPing` | Yes | `PingPing` | Covered |
| `PingPingSpecificSource` | No | `PingPing` | Equivalent: `PingPing` already uses a specific source |
| `PingPingAnySource` | No | `PingPingAnySource` | Covered |
| `Sendrecv` | Yes | `Sendrecv` | Covered |
| `Exchange` | Yes | `Exchange` | Covered |
| `Allreduce` | Yes | `Allreduce` | Covered |
| `Reduce` | Yes | `Reduce` | Covered |
| `Reduce_local` | Yes | — | **Not implementable**: `MPI_Reduce_local` is MPI-2.2 and is not in the API |
| `Reduce_scatter` | Yes | `Reduce_scatter` | Covered |
| `Reduce_scatter_block` | Yes | — | **Discarded**: will not be implemented (explicit project decision) |
| `Allgather` | Yes | `Allgather` | Covered |
| `Allgatherv` | Yes | `Allgatherv` | Covered |
| `Gather` | Yes | `Gather` | Covered |
| `Gatherv` | Yes | `Gatherv` | Covered |
| `Scatter` | Yes | `Scatter` | Covered |
| `Scatterv` | Yes | `Scatterv` | Covered |
| `Alltoall` | Yes | `Alltoall` | Covered |
| `Alltoallv` | Yes | `Alltoallv` | Covered |
| `Bcast` | Yes | `Bcast` | Covered |
| `Barrier` | Yes | `Barrier` | Covered |
| `Uniband` | No | `Uniband` | Covered |
| `Biband` | No | `Biband` | Covered |

Summary:

- 21 benchmarks covered directly.
- 2 functional equivalences (`PingPongSpecificSource`,
  `PingPingSpecificSource`).
- No implementable IMB-MPI1 benchmarks remain pending within the plan.
- 1 not implementable with MPI-1.2/1.3 (`Reduce_local`).
- 1 explicitly discarded (`Reduce_scatter_block`).

## 4. IMB-P2P coverage

IMB-P2P contains 8 benchmarks. All of them use MPI-1.2 point-to-point
communication exclusively. The only non-MPI-1.2 call in the original code is
`MPI_Alloc_mem`, which in Java is replaced by ordinary arrays.

| IMB-P2P benchmark | In MB_MPJ | State |
|---|---|---|
| `PingPong` | `MB_MPJ.p2p.PingPong` | Covered: all ranks in pairs, requires an even count |
| `PingPing` | `MB_MPJ.p2p.PingPing` | Covered: all ranks in pairs, requires an even count |
| `Unirandom` | `MB_MPJ.p2p.Unirandom` | Covered: random pair, one direction |
| `Birandom` | `MB_MPJ.p2p.Birandom` | Covered: random pair, bidirectional exchange |
| `Corandom` | `MB_MPJ.p2p.Corandom` | Covered: random ring, all ranks |
| `Stencil2D` | `MB_MPJ.p2p.Stencil2D` | Covered: periodic 2D grid, 4 neighbours |
| `Stencil3D` | `MB_MPJ.p2p.Stencil3D` | Covered: periodic 3D grid, 6 neighbours |
| `SendRecv_Replace` | `MB_MPJ.p2p.SendRecv_Replace` | Covered: periodic chain with `Sendrecv_replace`, all ranks |

### 4.1. Checking `PingPong`/`PingPing` in IMB-P2P

It was verified in the code and in real runs that IMB-P2P does **not** limit
these benchmarks to 2 active processes:

- `imb_p2p_pingpong.c` and `imb_p2p_pingping.c` reject `nranks < 2` and odd
  counts (`nranks & 1`).
- Each rank computes `partner = (rank + nranks/2) % nranks`; ranks
  `rank < nranks/2` send first and the rest receive first. Result:
  `nranks/2` simultaneous pairs.
- Test with 6 ranks (`IMB-P2P PingPong`): all 6 processes took part, in pairs
  `0<->3`, `1<->4`, `2<->5`.

Therefore, the 2-rank limit that MB_MPJ had was a **real coverage gap**. It has
already been fixed: `MB_MPJ.p2p.PingPong` and `MB_MPJ.p2p.PingPing` accept an
even number of processes and use all ranks in pairs.

The confusion comes from IMB-MPI1: `src_c/IMB_pingpong.c` uses only
`c_info->pair0` and `c_info->pair1`, so in IMB-MPI1 only 2 processes are active
even when more are launched. That is why MB_MPJ keeps two distinct classes:
`MB_MPJ.mpi1.PingPong` uses `pair0=0` and `pair1=nprocs-1` and leaves the others
idle, like IMB-MPI1, whereas `MB_MPJ.p2p.PingPong` uses all ranks, like IMB-P2P.

Summary: 8 covered and no implementable benchmark pending.

## 5. IMB suites not implementable with MPI-1.2/1.3

| Suite | Number of benchmarks | Requirement | Reason |
|---|---:|---|---|
| IMB-NBC | 13 by default / 26 with `_pure` variants | Non-blocking collectives (MPI-3) | mpiJava 1.2 has no `Ibcast`, `Iallreduce`, `Ibarrier`, etc. |
| IMB-RMA | 17 by default / 19 with `Get_local` and `Get_all_local` | MPI-3 RMA | There is no `mpi.Win` in the API |
| IMB-EXT | 6 | MPI-2 one-sided | There is no `mpi.Win` in the API |
| IMB-IO | 37 | MPI-2 MPI-IO | There is no `mpi.File` in the API |
| IMB-MPI4 | 13 by default / 26 with `_pure` variants | Persistent collectives (MPI-4) | There are no collective `MPI_*_init` calls in the API |
| IMB-MT | 16 | `MPI_THREAD_MULTIPLE` + OpenMP | Outside the scope of MPI-1.2/1.3 and of the current execution model |
| IMB-MPI1-GPU / IMB-RMA-GPU | GPU variants | GPU memory / extensions | Not applicable to the mpiJava API |
| HALO | Experimental | OpenMP + MPI | Not part of `make all`; generic example suite |

### 5.1. IMB-NBC

`Ibcast`, `Iallgather`, `Iallgatherv`, `Igather`, `Igatherv`, `Iscatter`,
`Iscatterv`, `Ialltoall`, `Ialltoallv`, `Ireduce`, `Ireduce_scatter`,
`Iallreduce`, `Ibarrier`, and their `_pure` variants.

### 5.2. IMB-RMA

`Unidir_put`, `Unidir_get`, `Bidir_put`, `Bidir_get`, `One_put_all`,
`One_get_all`, `All_put_all`, `All_get_all`, `Put_local`, `Get_local`,
`Put_all_local`, `Get_all_local`, `Exchange_put`, `Exchange_get`,
`Accumulate`, `Get_accumulate`, `Fetch_and_op`, `Compare_and_swap`,
`Truly_passive_put`.

### 5.3. IMB-EXT

`Window`, `Unidir_Get`, `Unidir_Put`, `Bidir_Get`, `Bidir_Put`,
`Accumulate`.

### 5.4. IMB-IO

Write: `S_Write_Indv`, `S_IWrite_Indv`, `S_Write_Expl`,
`S_IWrite_Expl`, `P_Write_Indv`, `P_IWrite_Indv`, `P_Write_Shared`,
`P_IWrite_Shared`, `P_Write_Priv`, `P_IWrite_Priv`, `P_Write_Expl`,
`P_IWrite_Expl`, `C_Write_Indv`, `C_IWrite_Indv`, `C_Write_Shared`,
`C_IWrite_Shared`, `C_Write_Expl`, `C_IWrite_Expl`.

Read: `S_Read_Indv`, `S_IRead_Indv`, `S_Read_Expl`, `S_IRead_Expl`,
`P_Read_Indv`, `P_IRead_Indv`, `P_Read_Shared`, `P_IRead_Shared`,
`P_Read_Priv`, `P_IRead_Priv`, `P_Read_Expl`, `P_IRead_Expl`,
`C_Read_Indv`, `C_IRead_Indv`, `C_Read_Shared`, `C_IRead_Shared`,
`C_Read_Expl`, `C_IRead_Expl`, plus `Open_Close`.

### 5.5. IMB-MPI4

`Bcast_persist`, `Allgather_persist`, `Allgatherv_persist`,
`Gather_persist`, `Gatherv_persist`, `Scatter_persist`,
`Scatterv_persist`, `Alltoall_persist`, `Alltoallv_persist`,
`Reduce_persist`, `Reduce_scatter_persist`, `Allreduce_persist`,
`Barrier_persist`, and their `_pure` variants.

### 5.6. IMB-MT

`PingPongMT`, `PingPingMT`, `SendRecvMT`, `ExchangeMT`, `UniBandMT`,
`BiBandMT`, `BarrierMT`, `BcastMT`, `ReduceMT`, `AllReduceMT`, `GatherMT`,
`GathervMT`, `AllgatherMT`, `AllgathervMT`, `AlltoallMT`, `AlltoallvMT`.

## 6. Remaining coverage

No benchmarks remain pending: all IMB-MPI1 and IMB-P2P benchmarks implementable
with MPI-1.2/1.3 are covered. `Reduce_local` is not implementable and
`Reduce_scatter_block` was explicitly discarded.

### 6.1. Benchmarks that do not require a new implementation

- `PingPongSpecificSource`: the current `PingPong` already uses a specific
  source.
- `PingPingSpecificSource`: the current `PingPing` already uses a specific
  source.

If exact compatibility with the IMB names is wanted, they can be accepted as
aliases of the existing classes, without duplicating code.

## 7. Framework status

`MicroBench` already includes the changes required by the current benchmarks:

1. **Per-benchmark rank-count validation**: solved with the `minRanks()`,
   `requiresEvenRanks()` and `validRankCount()` capabilities.
   `MB_MPJ.p2p.PingPong`, `MB_MPJ.p2p.PingPing`, `MB_MPJ.mpi1.Uniband` and
   `MB_MPJ.mpi1.Biband` require an even number of ranks (`>= 2`); the stencils
   require a factorable 2D or 3D grid.
2. **IMB-P2P result output**: solved with `MB_MPJ.p2p.P2PBench`, which prints
   the `#bytes #repetitions t[usec] Mbytes/sec Msg/sec` columns and aggregates
   the times of all ranks.
3. **Uniband/Biband output**: solved with `MB_MPJ.mpi1.MsgRateBench`, which
   prints the `#bytes #repetitions Mbytes/sec Msg/sec` columns, like the IMB
   `ParallelTransferMsgRate` type.
4. **Configurable window**: solved with the `-window_size` option (256 by
   default), which controls the messages in flight for `Uniband`/`Biband`.
5. **Reproducible randomness**: solved with `MB_MPJ.p2p.RandomBench`, which uses
   the same linear congruential generator as IMB-P2P and aggregates the time
   and message count of all ranks on rank 0.
6. **Stencil grids and buffers**: solved with `MB_MPJ.p2p.StencilBench`. Each
   benchmark computes the grid like IMB and uses one send buffer and one
   receive buffer per neighbour.
7. **Warmup**: solved with the same IMB rule: MPI1 warms up once with 10% of the
   iterations of the first size and a minimum of 10; P2P warms up at each size
   with 10% and a minimum of 1. It is controlled with `-warm_up on|off`.
8. **Name registration**: the existing benchmarks are listed in
   `MicroBench.printBenchmarksNames()` and in the `README`; new classes must be
   added to both places.
9. **Command-line options**: solved with the IMB names and semantics, keeping
   aliases for the old options; `-msgwr`/`-msgrd` were also added for IMB-P2P,
   with `on` by default, along with `-mem`/`-time`/`-sync` and `-root_shift` for
   IMB-MPI1. `-off_cache` is not implemented and is rejected as an unknown
   option.

### 7.1. Option equivalence

| MB_MPJ | IMB | Default value |
|---|---|---|
| `-iter <msgspersample[,overall_vol[,msgs_nonaggr]]>` | `-iter` | MPI1 `1000,40,100`; P2P `100000,800,100` |
| `-mem <GiB>` | `-mem` (MPI1) | `1.0` GiB per process |
| `-time <seconds>` | `-time` (MPI1) | `10.0` s per size |
| `-msgsz <size>` | `-msgsz` (P2P) | — |
| `-msgsize <s1,s2,...>` | `-msgsize` (MPI1) | — |
| `-msglog <min:max>` | `-msglog` | `0:22` (0 is translated to 1 byte) |
| `-warm_up <on\|off>` | `-warm_up` (MPI1) | `on`: 10% with a minimum of 10 (MPI1) or a minimum of 1 (P2P) |
| `-window_size <n>` | `-window_size` | `256` |
| `-msgwr <on\|off>` | `-msgwr` (P2P) | `on`: touches a 64 B cache line of the send buffer |
| `-msgrd <on\|off>` | `-msgrd` (P2P) | `on`: reads a 64 B cache line of the receive buffer |
| `-pause <microseconds>` | `-pause` (P2P) | `100000` µs between samples |
| `-sync <on\|off>` | `-sync` (MPI1) | `on`: barrier after each collective iteration |
| `-root_shift <on\|off>` | `-root_shift` (MPI1) | `off`: root fixed at 0; `on`: rotates the root |
| `-list` | `-list` | — |
| `-help` | `-help` | — |
| `-s`, `-e` | — (explicit range in bytes) | `1`, `4194304` |
| `-i`, `-o` | aliases of `-iter` | `1000` / `40` MiB (MPI1); `100000` / `800` MiB (P2P) |
| `-csv [file]` | — | writes the result table to CSV (rank 0); `<Class>.csv` by default |
| `-benchmarks` | alias of `-list` | — |

The overall volume of `-iter`/`-o` is in **MiB**, as in IMB. The only known
range difference is that MB_MPJ does not generate the size-0 message:
`-msglog 0` is interpreted as 1 byte. `-msgwr`/`-msgrd`/`-pause` are only
accepted in IMB-P2P benchmarks; in IMB-MPI1 they are rejected with a message.
Between P2P samples the pause and the IMB dissemination barrier are applied,
outside the measured time. `-mem` and `-time` are only accepted in IMB-MPI1. If
the maximum buffers exceed `-mem`, MB_MPJ aborts with an error; `-time` reduces
the iterations of a sample if its measured time exceeds the limit. `-sync` only
affects IMB-MPI1 collectives.

All benchmarks accept `-csv [file]` to dump the result table to CSV. Only rank
0 writes, and one file is generated per benchmark; if no name is given, the
class name with dots replaced by underscores is used (for example,
`MB_MPJ_mpi1_PingPong.csv`). The columns mirror the stdout table: times in
microseconds, decimal MB/s and Msg/s where applicable. `LatencyBench` keeps its
own schema of raw samples (`size_bytes,iteration,latency_us`).

`-off_cache` is not implemented and is rejected as an unknown option: it would
require rotating buffer offsets and changes in every MPI1 kernel.

MPI1 collectives time only the collective call of each iteration, like IMB. The
root update and the `-sync` barrier are outside the measured time. By default
`-root_shift` is `off`, so the root is 0.

## 8. Implementation status and next steps

Already implemented:

- `PingPongAnySource` and `PingPingAnySource`: they reuse the
  `PingPong`/`PingPing` kernel, changing only the receive source and tag to
  `MPI.ANY_SOURCE`/`MPI.ANY_TAG`.
- `SendRecv_Replace`: periodic chain with `MPI.COMM_WORLD.Sendrecv_replace`.
- `Uniband` and `Biband`: pairs `r <-> r+nprocs/2`, a window of messages in
  flight, `Request.Waitall` and a one-byte ack per window. `Biband` exchanges in
  both directions and doubles the reported bandwidth.
- `Unirandom`, `Birandom` and `Corandom`: they use the IMB-P2P pseudorandom
  sequence to choose pairs or shuffle a ring, and aggregate times and message
  counts on rank 0.
- `Stencil2D` and `Stencil3D`: they compute the periodic grid like IMB, use 4 or
  6 neighbours and post one receive and one send per neighbour before
  `Waitall`.

No benchmarks remain pending from the plan. `Reduce_local` is not implementable
and `Reduce_scatter_block` was explicitly discarded.

## 9. References in the upstream IMB code

- `src_cpp/MPI1/MPI1_benchmark.cpp`: IMB-MPI1 registration and flags.
- `src_c/IMB_pingpong.c` and `src_c/IMB_pingping.c`: PingPong/PingPing kernels,
  including `PingPongAnySource` and `PingPingAnySource`.
- `src_c/IMB_bandwidth.c`: `Uniband` and `Biband` kernels.
- `src_c/P2P/imb_p2p.c` and `src_c/P2P/imb_p2p_*.c`: IMB-P2P benchmarks,
  including `imb_p2p_unirandom.c`, `imb_p2p_birandom.c`,
  `imb_p2p_corandom.c`, `imb_p2p_stencil2d.c`, `imb_p2p_stencil3d.c` and
  `imb_p2p_sendrecv_replace.c`.
- `src_cpp/NBC/NBC_benchmark.cpp`: non-blocking collective benchmarks.
- `src_cpp/RMA/RMA_benchmark.cpp`: RMA benchmarks.
- `src_cpp/EXT/EXT_benchmark.cpp`: one-sided benchmarks.
- `src_cpp/IO/IO_benchmark.cpp`: MPI-IO benchmarks.
- `src_cpp/MPI4/MPI4_benchmark.cpp`: persistent collectives.
- `src_cpp/MT/MT_benchmark.cpp`: multithreaded benchmarks.
- mpiJava 1.2 API (FastMPJ 1.0.7 in the development environment): `mpi.Comm`,
  `mpi.Intracomm`, `mpi.Status`, `mpi.Request`, `mpi.Prequest`, etc.
