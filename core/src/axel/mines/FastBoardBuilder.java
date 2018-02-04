package axel.mines;

import com.badlogic.gdx.utils.Array;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;
import java.util.*;
import java.util.concurrent.*;

public class FastBoardBuilder {
	private final byte[][] field;
	private final int numMines;
	/** Chance of a mine at every tile. */
	private float chance;

	FastBoardBuilder(int width, int height, int numMines) {
		field = new byte[width][height];
		this.numMines = numMines;
		chance = (float) numMines / (width * height);
	}

	private class ForkGen extends RecursiveAction {
		// The x and width
		final int start, length;
		int currNumMines;
		protected static final int threshold = 20;

		ForkGen(int start, int length, int currNumMines) {
			this.start = start;
			this.length = length;
			this.currNumMines = currNumMines;
		}

		protected void computeDirectly() {
			Array<Coord> available = new Array<>();
			Random random = ThreadLocalRandom.current();

			outer:
			for (int x = start; x < start + length; ++x) {
				for (int y = 0; y < field[0].length; ++y) {
					float z = random.nextFloat();

					if (z <= chance) {
						field[x][y] |= Board.MINE_BIT;
						if (--currNumMines <= 0) {
							break outer;
						}
					} else if (z <= 1.06f * chance){
						available.add(new Coord(x, y));
					}
				}
			}

			while (currNumMines-- > 0) {
				int i = random.nextInt(available.size);
				Coord c = available.get(i);
				available.removeIndex(i);
				field[c.x][c.y] |= Board.MINE_BIT;
			}
		}

		@Override
		protected void compute() {
			if (length < threshold) {
				computeDirectly();
				return;
			}

			int split = length / 2;
			int firstNumMines = currNumMines / 2;
			invokeAll(new ForkGen(start, split, firstNumMines),
					new ForkGen(start + split, length - split, currNumMines - firstNumMines));
		}
	}

	public Board build() {
		ForkGen forkGen = new ForkGen(0, field.length, numMines);
		ForkJoinPool pool = new ForkJoinPool();
		pool.invoke(forkGen);

		Board board = new Board(field, field.length * field[0].length - numMines);
		board.countNeighbouringMines();
		return board;
	}
}
