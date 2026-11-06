/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ.p2p;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/* Grid factorisation rules used to validate stencil rank counts. */
class StencilGridTest
{
	@Test
	void stencil2dAcceptsFactorableGrids()
	{
		assertArrayEquals(new int[] {2, 2}, Stencil2D.gridFor(4));
		assertArrayEquals(new int[] {2, 3}, Stencil2D.gridFor(6));
		assertArrayEquals(new int[] {4, 2}, Stencil2D.gridFor(8));
		assertArrayEquals(new int[] {3, 3}, Stencil2D.gridFor(9));
		assertArrayEquals(new int[] {3, 4}, Stencil2D.gridFor(12));
		assertArrayEquals(new int[] {4, 4}, Stencil2D.gridFor(16));
	}

	@Test
	void stencil2dRejectsCountsWithoutATwoDimensionalGrid()
	{
		assertNull(Stencil2D.gridFor(1));
		assertNull(Stencil2D.gridFor(2));
		assertNull(Stencil2D.gridFor(3));
		assertNull(Stencil2D.gridFor(5));
		assertNull(Stencil2D.gridFor(7));
	}

	@Test
	void stencil3dAcceptsFactorableGrids()
	{
		int[] cube = Stencil3D.gridFor(8);
		assertNotNull(cube);
		assertEquals(8L, (long)cube[0] * cube[1] * cube[2]);

		int[] grid = Stencil3D.gridFor(12);
		assertNotNull(grid);
		assertEquals(12L, (long)grid[0] * grid[1] * grid[2]);

		int[] big = Stencil3D.gridFor(27);
		assertNotNull(big);
		assertEquals(27L, (long)big[0] * big[1] * big[2]);
	}

	@Test
	void stencil3dRejectsCountsWithoutAThreeDimensionalGrid()
	{
		assertNull(Stencil3D.gridFor(1));
		assertNull(Stencil3D.gridFor(4));
		assertNull(Stencil3D.gridFor(6));
		assertNull(Stencil3D.gridFor(9));
	}
}
