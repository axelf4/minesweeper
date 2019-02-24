package minesweeper;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Board implements Cloneable {
	public static final int NEIGHBOURING_MASK = 0xF;
	public static final int MINE_BIT = 1 << 4;
	/**
	 * Cleared.
	 */
	public static final int REVEALED_BIT = 1 << 5;
	public static final int FLAG_BIT = 1 << 6;
	public static final int EMPTY_MASK = ~(MINE_BIT | REVEALED_BIT | FLAG_BIT);

	private final byte[][] field;
	/**
	 * Number of remaining tiles to be cleared.
	 */
	private final int left;

	public Board(Board b) {
		field = new byte[b.getWidth()][b.getHeight()];
		for (int i = 0; i < field.length; ++i)
			System.arraycopy(b.field[i], 0, field[i], 0, field[0].length);
		left = b.left;
	}

	public Board(byte[][] field, int left) {
		this.field = field;
		this.left = left;
	}

	public static Board generate(int width, int height, int numMines) {
		byte[][] field = new byte[width][height];
		Random random = new Random();

		while (numMines > 0) {
			int x = random.nextInt(width), y = random.nextInt(height);
			if (field[x][y] != 0) continue;
			field[x][y] |= MINE_BIT;
			--numMines;
		}

		int left = width * height - numMines;
		Board result = new Board(field, left);
		result.countNeighbouringMines();
		return result;
	}

	public int getWidth() {
		return field.length;
	}

	public int getHeight() {
		return field[0].length;
	}

	public int getTile(int x, int y) {
		if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight())
			throw new IllegalArgumentException("Tile is out of bounds.");
		return field[x][y];
	}

	public int getTile(Coord c) {
		return getTile(c.x, c.y);
	}

	public int getRemainingTiles() {
		return left;
	}

	public Board clone() throws CloneNotSupportedException {
		return (Board) super.clone();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Board && Arrays.deepEquals(field, ((Board) obj).field);
	}

	public void countNeighbouringMines() {
		int mines = 0;
		for (int x = 0; x < getWidth(); ++x) {
			for (int y = 0; y < getHeight(); ++y) {
				if ((getTile(x, y) & MINE_BIT) == 0) continue;
				++mines;
				for (final Coord neighbour : getNeighbouringTiles(x, y)) {
					++field[neighbour.x][neighbour.y];
				}
			}
		}
		System.out.println("Finished calculating mine counts. minCount: " + mines);
	}

	public byte getNeighbouringMineCount(int x, int y) {
		return (byte) (field[x][y] & NEIGHBOURING_MASK);
	}

	public Board toggleFlag(int x, int y) {
		final Board copy = new Board(this);
		int tile = getTile(x, y);
		if ((tile & REVEALED_BIT) == 0) {
			copy.field[x][y] ^= FLAG_BIT;
		}
		return copy;
	}

	/*
	 * Flood fill algorithm:
	 * start on tile that has no neighbouring mines.
	 * move up in corner
	 * mark top left as revealed
	 */

	private static final int FLOOD_MASK = NEIGHBOURING_MASK | REVEALED_BIT | FLAG_BIT;

	private Coord scanTopLeftCorner(int x, int y) {
		for (; ; ) {
			if (y > 0 && x > 0 && getTile(x - 1, y - 1) == 0) {
				--x;
				--y;
			} else if (x > 0 && getTile(x - 1, y) == 0) --x;
			else if (y > 0 && getTile(x, y - 1) == 0) --y;
			else break;
		}
		return new Coord(x, y);
	}

	/*void regionFloodFill(Region region, int regionIndex, int x, int y) {
			final Deque<Coord> stack = new ArrayDeque<>();
			// In upper-left corner
			stack.push(scanTopLeftCorner(x, y));

			do {
				final Coord c = stack.pop();
				x = c.x;
				y = c.y;

				int lastRowLength = 0;
				rowLoop:
				do {
					// Store the start coordinates of the row
					int sx = x, rowLength = 0;

					if (lastRowLength != 0) {
						if (tileRegions[x][y] != 0) {
							// Handle case like |***|
							//                  | **|
							do if (--lastRowLength == 0) break rowLoop; while (tileRegions[++x][y] != 0);
							sx = x;
						} else {
							// If row has gotten wider on the left
							// | **|
							// |*X*|

							for (; x > 0 && tileRegions[x - 1][y] == 0; ++rowLength, ++lastRowLength) {
								tileRegions[--x][y] = regionIndex;
								region.tiles.add(new Coord(x, y));

								// If cavities appeared above
								// |Y **|
								// |*X**|
								if (y > 0 && tileRegions[x][y - 1] == 0)
									stack.addLast(scanTopLeftCorner(x, y - 1));
							}
						}
					}

					// Scan across to the right in the current row
					for (; sx < getWidth() && tileRegions[sx][y] == 0; ++rowLength, ++sx) {
						tileRegions[sx][y] = regionIndex;
						region.tiles.add(new Coord(sx, y));
					}

					if (rowLength < lastRowLength + 1) {
						// |*****|
						// |*** *|
						for (int end = x + lastRowLength; ++sx <= end && sx < getWidth();)
							if (tileRegions[sx][y] == 0)
								stack.addLast(new Coord(sx, y));
					} else if (rowLength > lastRowLength && y > 0) {
						// |*** *|
						// |**** |
						// If the row is longer than the last we must look up
						for (int ux = x + lastRowLength; ++ux < sx; )
							if (tileRegions[ux][y - 1] == 0)
								stack.addLast(scanTopLeftCorner(ux, y - 1));
					}

					lastRowLength = rowLength;
				} while (lastRowLength != 0 && ++y < getHeight());
			} while (!stack.isEmpty());
		}
		*/

	private void floodFill(final Coord c) {
		int x = c.x;
		int y = c.y;

		// If top-left tile is not flagged
		if (x > 0 && y > 0
				&& (getTile(x - 1, y - 1) & FLAG_BIT) == 0) {
			field[x - 1][y - 1] |= REVEALED_BIT;
		}

		int lastRowLength = 0;

		rowLoop:
		do {
			int rowLength = 0, sx = x;

			if (lastRowLength > 0) {
				if (getTile(x, y) != 0) {
					// Handle
					// |***|
					// | **|
					do {
						if (--lastRowLength == 0) break rowLoop;
					} while (++x < getWidth() && getTile(x, y) == 0);
					sx = x;
				} else {
					// Handle
					// | **|
					// |***|
					for (; x > 0; ++rowLength, ++lastRowLength) {
						int tile = getTile(--x, y);
						if ((tile & FLAG_BIT) != 0) break;
						field[x][y] |= REVEALED_BIT;
						if ((tile & NEIGHBOURING_MASK) != 0) break;
						if (y > 0 && (getTile(x, y - 1) & FLOOD_MASK) == 0) {
							floodFill(scanTopLeftCorner(x, y - 1));
						}
					}
				}
			}

			// Scan across to the right
			for (; ; ) {
				field[sx][y] |= REVEALED_BIT;
				++sx;
				++rowLength;
				if (sx < getWidth()) break;
				int tile = getTile(sx, y);
				if ((tile & FLAG_BIT) != 0) break;
				if ((tile & NEIGHBOURING_MASK) != 0) {
					field[sx][y] |= REVEALED_BIT;
					break;
				}
			}

			if (rowLength < lastRowLength + 1) {
				// |*****|
				// |*** *|
				for (int end = x + lastRowLength; ++sx <= end && sx < getWidth(); ) {
					if ((getTile(sx, y) & FLOOD_MASK) == 0) {
						floodFill(new Coord(sx, y));
					}
				}
			} else if (rowLength > lastRowLength && y > 0) {
				// |*** *|
				// |**** |
				// If the row is longer than the last we must look up
				for (int ux = x + lastRowLength + 1; ux++ < sx && ux < getWidth(); ) {
					if ((getTile(ux, y - 1) & FLOOD_MASK) == 0) {
						floodFill(scanTopLeftCorner(ux, y - 1));
					}
				}
			}

			// TODO check lower right corner

			lastRowLength = rowLength;
		} while (lastRowLength != 0 && ++y < getHeight());
	}

	public ClearTileResult clearTiles(final Coord... coords) {
		if (coords.length == 0) return new ClearTileResult(this, 0, 0, 0, 0, false);

		int minX = coords[0].x, minY = coords[0].y, maxX = coords[0].x, maxY = coords[0].y;

		final Deque<Coord> stack = new ArrayDeque<>();
		for (Coord c : coords) {
			final int type = getTile(c.x, c.y);
			if ((type & (MINE_BIT | FLAG_BIT)) == MINE_BIT)
				return new ClearTileResult(this, minX, minY, maxX, maxY, true);
			else if ((type & (MINE_BIT | REVEALED_BIT | FLAG_BIT)) == 0)
				stack.push(c);
		}
		if (stack.isEmpty()) return new ClearTileResult(this, minX, minY, maxX, maxY, false);

		byte[][] copy = new byte[getWidth()][getHeight()];
		for (int i = 0; i < field.length; ++i)
			System.arraycopy(field[i], 0, copy[i], 0, field[0].length);
		int left = this.left;

			/*while (!stack.isEmpty()) {
				final Coord c = stack.pop();

				if ((getTile(c) & NEIGHBOURING_MASK) == 0) {
					floodFill(scanTopLeftCorner(c.x, c.y));
				} else {
					copy.field[c.x][c.y] |= REVEALED_BIT;
				}
			}*/

		while (!stack.isEmpty()) {
			final Coord c = stack.pop();
			if (c.x < 0 || c.y < 0 || c.x >= getWidth() || c.y >= getHeight()) continue;

			if ((copy[c.x][c.y] & ~EMPTY_MASK) == 0) {
				copy[c.x][c.y] |= REVEALED_BIT;
				--left;
				if (c.x < minX) minX = c.x;
				if (c.y < minY) minY = c.y;
				if (c.x + 1 > maxX) maxX = c.x + 1;
				if (c.y + 1 > maxY) maxY = c.y + 1;

				if (getNeighbouringMineCount(c.x, c.y) == 0) {
					stack.push(new Coord(c.x - 1, c.y - 1));
					stack.push(new Coord(c.x, c.y - 1));
					stack.push(new Coord(c.x + 1, c.y - 1));
					stack.push(new Coord(c.x - 1, c.y));
					stack.push(new Coord(c.x + 1, c.y));
					stack.push(new Coord(c.x - 1, c.y + 1));
					stack.push(new Coord(c.x, c.y + 1));
					stack.push(new Coord(c.x + 1, c.y + 1));
				}
			}
		}

		return new ClearTileResult(new Board(copy, left), minX, minY, maxX, maxY, false);
	}

	public boolean isOutOfBounds(Coord c) {
		return c.x < 0 || c.x >= getWidth() || c.y < 0 || c.y >= getHeight();
	}

	public Coord[] getNeighbouringTiles(int x, int y) {
		return Arrays.stream(new Coord[]{new Coord(x - 1, y - 1), new Coord(x, y - 1), new Coord(x + 1, y - 1),
				new Coord(x - 1, y), new Coord(x + 1, y),
				new Coord(x - 1, y + 1), new Coord(x, y + 1), new Coord(x + 1, y + 1)})
				.filter(c -> !isOutOfBounds(c)).toArray(Coord[]::new);
	}

	@Override
	public int hashCode() {
		return 31 * Objects.hash(left) + Arrays.hashCode(field);
	}

	@Override
	public String toString() {
		return Arrays.stream(field).map(col -> IntStream.range(0, col.length)
				.map(i -> col[i])
				.mapToObj(c -> "" + ((c & MINE_BIT) == 0 ? Character.forDigit(c & NEIGHBOURING_MASK, 10) : 'X')).collect(Collectors.joining()))
				.collect(Collectors.joining("\n"));
	}

	public static final class ClearTileResult {
		public final Board board;
		/**
		 * The inclusive X-coordinate of the start of the dirty region.
		 */
		public final int minX,
		/**
		 * The inclusive Y-coordinate of the start of the dirty region.
		 */
		minY,
		/**
		 * The exclusive X-coordinate of the end of the dirty region.
		 */
		maxX,
		/**
		 * The exclusive Y-coordinate of the end of the dirty region.
		 */
		maxY;
		public final boolean wasMine;

		private ClearTileResult(Board board, int minX, int minY, int maxX, int maxY, boolean wasMine) {
			this.board = board;
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			this.wasMine = wasMine;
		}
	}
}
