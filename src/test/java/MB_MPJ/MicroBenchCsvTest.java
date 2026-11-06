/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/* CSV schema and formatting for the base result table. */
class MicroBenchCsvTest
{
	private static class StubBench extends MicroBench
	{
		boolean singleTime;
		boolean singleIteration;
		boolean bandwidth = true;

		protected void Benchrun(int size, int iters)
		{
		}

		protected boolean singleTimeColumn()
		{
			return this.singleTime;
		}

		protected boolean singleIteration()
		{
			return this.singleIteration;
		}

		protected boolean reportsBandwidth()
		{
			return this.bandwidth;
		}
	}

	private static final double[] TIMES = {1E-6, 2E-6};

	@TempDir
	Path tempDir;

	private PrintStream originalOut;
	private boolean savedCsvOutput;
	private String savedCsvFile;
	private Path csvFile;
	private StubBench bench;

	@BeforeEach
	void setUp()
	{
		this.originalOut = System.out;
		System.setOut(new PrintStream(new ByteArrayOutputStream(), true));

		this.savedCsvOutput = MicroBench.CSV_OUTPUT;
		this.savedCsvFile = MicroBench.CSV_OUTPUT_FILE;
		this.csvFile = this.tempDir.resolve("results.csv");
		MicroBench.CSV_OUTPUT = true;
		MicroBench.CSV_OUTPUT_FILE = this.csvFile.toString();

		this.bench = new StubBench();
		this.bench.rank = 0;
	}

	@AfterEach
	void tearDown()
	{
		this.bench.closeCsv();
		MicroBench.CSV_OUTPUT = this.savedCsvOutput;
		MicroBench.CSV_OUTPUT_FILE = this.savedCsvFile;
		System.setOut(this.originalOut);
	}

	private List<String> lines() throws Exception
	{
		return Files.readAllLines(this.csvFile, StandardCharsets.UTF_8);
	}

	@Test
	void defaultTableHasSizeRepetitionsTimesAndBandwidth() throws Exception
	{
		this.bench.printResults(1024, 10, TIMES);

		List<String> lines = lines();

		assertEquals("size_bytes,repetitions,t_min_us,t_max_us,t_avg_us,mb_per_sec", lines.get(0));
		assertEquals("1024,10,1.000000,2.000000,1.500000,512.000000", lines.get(1));
	}

	@Test
	void pointToPointTableHasASingleTimeColumn() throws Exception
	{
		this.bench.singleTime = true;

		this.bench.printResults(1024, 10, TIMES);

		List<String> lines = lines();

		assertEquals("size_bytes,repetitions,t_us,mb_per_sec", lines.get(0));
		assertEquals("1024,10,2.000000,512.000000", lines.get(1));
	}

	@Test
	void collectiveTableHasNoBandwidthColumn() throws Exception
	{
		this.bench.bandwidth = false;

		this.bench.printResults(1024, 10, TIMES);

		List<String> lines = lines();

		assertEquals("size_bytes,repetitions,t_min_us,t_max_us,t_avg_us", lines.get(0));
		assertEquals("1024,10,1.000000,2.000000,1.500000", lines.get(1));
	}

	@Test
	void barrierTableHasOnlyRepetitionsAndTimes() throws Exception
	{
		this.bench.singleIteration = true;

		this.bench.printResults(0, 10, TIMES);

		List<String> lines = lines();

		assertEquals("repetitions,t_min_us,t_max_us,t_avg_us", lines.get(0));
		assertEquals("10,1.000000,2.000000,1.500000", lines.get(1));
	}

	@Test
	void headerIsWrittenOnlyOnce() throws Exception
	{
		this.bench.printResults(1024, 10, TIMES);
		this.bench.printResults(2048, 5, TIMES);

		List<String> lines = lines();

		assertEquals(3, lines.size());
		assertTrue(lines.get(0).startsWith("size_bytes"));
		assertEquals(1, lines.stream().filter(line -> line.startsWith("size_bytes")).count());
	}

	@Test
	void defaultFileNameDerivesFromTheClass()
	{
		String name = this.bench.defaultCsvFileName();

		assertEquals(StubBench.class.getName().replace('.', '_') + ".csv", name);
		assertFalse(name.substring(0, name.length() - 4).contains("."));
	}
}
