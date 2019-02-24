package minesweeper;

import org.junit.jupiter.api.Test;

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
		Board.ClearTileResult result = b.clearTiles(new Coord(2, 1));
		assertEquals(1, result.minX);
		assertEquals(0, result.minY);
		assertEquals(4, result.maxX);
		assertEquals(4, result.maxY);
		assertFalse(result.wasMine);
		assertEquals(new Board(new byte[][]{
				{MINE_BIT, 1, 2, 1 | MINE_BIT},
				{1 | REVEALED_BIT, 1 | REVEALED_BIT, 2 | REVEALED_BIT, 1 | MINE_BIT},
				{REVEALED_BIT, REVEALED_BIT, 1 | REVEALED_BIT, 1 | REVEALED_BIT},
				{REVEALED_BIT, REVEALED_BIT, REVEALED_BIT, REVEALED_BIT},
		}, 3), result.board);
	}
}
