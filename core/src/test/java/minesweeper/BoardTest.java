package minesweeper;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static minesweeper.Board.MINE_BIT;
import static minesweeper.Board.REVEALED_BIT;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BoardTest {
	private final Board b = new Board(new byte[][]{
			{MINE_BIT, 1, 2, 1 | MINE_BIT},
			{1, 1, 2, 1 | MINE_BIT},
			{0, 0, 1, 1},
			{0, 0, 0, 0},
	}, 3);

	@Test
	void testConstructorCopyCorrect() {
		assertEquals(b, new Board(b));
	}

	@Test
	void testClearTilesGivenBoardCorrect() {
		Board board = new Board(b);
		Board.ClearTileResult result = board.clearTiles(new Coord(2, 1));
		assertEquals(new Board.Bounds(1, 0, 4, 4), result.dirtyRegion);
		assertFalse(result.wasMine);
		assertEquals(new Board(new byte[][]{
				{MINE_BIT, 1, 2, 1 | MINE_BIT},
				{1 | REVEALED_BIT, 1 | REVEALED_BIT, 2 | REVEALED_BIT, 1 | MINE_BIT},
				{REVEALED_BIT, REVEALED_BIT, 1 | REVEALED_BIT, 1 | REVEALED_BIT},
				{REVEALED_BIT, REVEALED_BIT, REVEALED_BIT, REVEALED_BIT},
		}, 3), board);
	}

	@Test
	void testGetNeighbouringTiles() {
		assertEquals(new HashSet<>(Arrays.asList(new Coord(2, 2), new Coord(3, 2), new Coord(2, 3))), b.getNeighbouringTiles(3, 3).collect(Collectors.toSet()));
	}
}
