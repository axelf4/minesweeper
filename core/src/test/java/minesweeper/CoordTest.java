package minesweeper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CoordTest {
	@Test
	public void testToString() {
		assertEquals("(6, 9)", new Coord(6, 9).toString());
	}

	@Test
	public void testEquals() {
		assertEquals(new Coord(1, 2), new Coord(1, 2));
	}
}
