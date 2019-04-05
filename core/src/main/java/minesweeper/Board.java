package minesweeper;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Board implements Cloneable {
	private static final int NEIGHBOURING_MASK = 0xF;
	public static final int MINE_BIT = 1 << 4;
	/**
	 * Cleared.
	 */
	public static final int REVEALED_BIT = 1 << 5;
	public static final int FLAG_BIT = 1 << 6;

	/**
	 * A two-dimensional array of bit fields representing the field.
	 * <p>
	 * A square with both the revealed and flagged bits set is only cleared.
	 */
	private final byte[][] field;
	/**
	 * Number of remaining mine-free squares.
	 */
	private int remaining;

	public Board(Board b) {
		field = new byte[b.getWidth()][b.getHeight()];
		for (int i = 0; i < field.length; ++i)
			System.arraycopy(b.field[i], 0, field[i], 0, field[0].length);
		remaining = b.remaining;
	}

	public Board(byte[][] field, int remaining) {
		this.field = field;
		this.remaining = remaining;
	}

	public static Board generate(int width, int height, int numMines) {
		Random random = new Random();
		long seed = System.currentTimeMillis();
		System.out.println("Using seed: " + seed);
		random.setSeed(seed);
		int remaining = width * height - numMines;
		byte[][] field = new byte[width][height];
		while (numMines > 0) {
			int x = random.nextInt(width), y = random.nextInt(height);
			if (field[x][y] != 0) continue;
			field[x][y] |= MINE_BIT;
			--numMines;
		}

		Board result = new Board(field, remaining);
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
		return remaining;
	}

	public Board clone() throws CloneNotSupportedException {
		return (Board) super.clone();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Board && Arrays.deepEquals(field, ((Board) obj).field);
	}

	public void countNeighbouringMines() {
		for (int x = 0; x < getWidth(); ++x) {
			for (int y = 0; y < getHeight(); ++y) {
				if ((getTile(x, y) & MINE_BIT) == 0) continue;
				getNeighbouringTiles(x, y).forEach(c -> ++field[c.x][c.y]);
			}
		}
	}

	public byte getNeighbouringMineCount(int x, int y) {
		return (byte) (field[x][y] & NEIGHBOURING_MASK);
	}

	public void toggleFlag(int x, int y) {
		if ((getTile(x, y) & REVEALED_BIT) == 0)
			field[x][y] ^= FLAG_BIT;
	}

	public boolean isOutOfBounds(Coord c) {
		return c.x < 0 || c.x >= getWidth() || c.y < 0 || c.y >= getHeight();
	}

	/**
	 * Returns a stream of the neighbours of the specified coordinate.
	 *
	 * @param x The x-component of the coordinate.
	 * @param y The y-component of the coordinate.
	 * @return A stream of the coordinates of the neighbouring tiles.
	 */
	public Stream<Coord> getNeighbouringTiles(int x, int y) {
		return IntStream.rangeClosed(x > 0 ? x - 1 : x, x < getWidth() - 1 ? x + 1 : x)
				.mapToObj(cx -> IntStream.rangeClosed(y > 0 ? y - 1 : y, y < getHeight() - 1 ? y + 1 : y)
						.filter(cy -> cx != x || cy != y)
						.mapToObj(cy -> new Coord(cx, cy)))
				.flatMap(Function.identity());
	}

	@Override
	public int hashCode() {
		return 31 * Objects.hash(remaining) + Arrays.hashCode(field);
	}

	@Override
	public String toString() {
		return Arrays.stream(field).map(col -> IntStream.range(0, col.length)
				.map(i -> col[i])
				.mapToObj(c -> "" + ((c & MINE_BIT) == 0 ? Character.forDigit(c & NEIGHBOURING_MASK, 10) : 'X')).collect(Collectors.joining()))
				.collect(Collectors.joining("\n"));
	}

	public static final class Bounds {
		public static final Bounds ZERO_SIZE = new Bounds(0, 0, 0, 0);
		/**
		 * The inclusive X-coordinate of the start of the region.
		 */
		public int minX,
		/**
		 * The inclusive Y-coordinate of the start of the region.
		 */
		minY,
		/**
		 * The exclusive X-coordinate of the end of the region.
		 */
		maxX,
		/**
		 * The exclusive Y-coordinate of the end of the region.
		 */
		maxY;

		public Bounds(int minX, int minY, int maxX, int maxY) {
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
		}

		public Bounds(Coord c) {
			this(c.x, c.y, c.x + 1, c.y + 1);
		}

		public static Bounds combine(Bounds a, Bounds b) {
			return new Bounds(Math.min(a.minX, b.minX), Math.min(a.minY, b.minY),
					Math.max(a.maxX, b.maxX), Math.max(a.maxY, b.maxY));
		}

		@Override
		public String toString() {
			return "Bounds{minX=" + minX + ", minY=" + minY + ", maxX=" + maxX + ", maxY=" + maxY + '}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Bounds bounds = (Bounds) o;
			return minX == bounds.minX && minY == bounds.minY && maxX == bounds.maxX && maxY == bounds.maxY;
		}

		@Override
		public int hashCode() {
			return Objects.hash(minX, minY, maxX, maxY);
		}
	}

	public static final class ClearTileResult {
		/**
		 * The tile region that needs to be redrawn.
		 */
		public final Bounds dirtyRegion;
		/**
		 * Whether a mine was detonated by the clear.
		 */
		public final boolean wasMine;

		private ClearTileResult(Bounds dirtyRegion, boolean wasMine) {
			this.dirtyRegion = dirtyRegion;
			this.wasMine = wasMine;
		}
	}

	/**
	 * Scans for the top-left corner of the continuous region of plain squares.
	 * <p>
	 * Assumes that the specified square is empty.
	 *
	 * @param x The starting x-coordinate.
	 * @param y The starting y-coordinate.
	 * @return The coordinate of the top-left corner.
	 */
	private Coord scanTopLeftCorner(int x, int y) {
		assert getTile(x, y) == 0 : "The specified square must be empty.";
		for (; ; ) {
			if (y > 0 && x > 0 && (field[x - 1][y - 1] & ~FLAG_BIT) == 0) {
				--x;
				--y;
			} else if (x > 0 && (field[x - 1][y] & ~FLAG_BIT) == 0) --x;
			else if (y > 0 && (field[x][y - 1] & ~FLAG_BIT) == 0) --y;
			else break;
		}
		return new Coord(x, y);
	}

	/**
	 * Recursively reveal squares using a flood fill algorithm.
	 * <p>
	 * The following invariants must be upheld:
	 * <ul>
	 * <li>The starting coordinate c must be an empty square.
	 * <li>And c should effectively be a top-left corner.
	 * <li>All empty squares that should be touched are not already revealed.
	 * </ul>
	 *
	 * @param c The starting coordinate.
	 * @return The bounds of the dirty area.
	 */
	private Bounds floodFill(Coord c) {
		assert !isOutOfBounds(c) : "The specified coordinate is out of bounds";
		assert getTile(c) == 0 : "The square is not empty.";
		int x = c.x, y = c.y;
		Bounds b = new Bounds(c);

		if (x > 0 && (field[x - 1][y] & REVEALED_BIT) == 0) {
			field[x - 1][y] |= REVEALED_BIT; // Left of top row
			--remaining;
			--b.minX;
		}
		if (y > 0) {
			if ((field[x][y - 1] & REVEALED_BIT) == 0) {
				field[x][y - 1] |= REVEALED_BIT; // Above start of first row
				--remaining;
			}
			if (x > 0 && (field[x - 1][y - 1] & REVEALED_BIT) == 0) {
				field[x - 1][y - 1] |= REVEALED_BIT; // Top-left corner
				--remaining;
			}
			--b.minY;
		}

		int prx = x; // Previous x-coordinate of rightmost empty square
		rowLoop:
		do {
			int rx = x;
			if (/* Was row above */ x != prx) {
				// See if left edge of this row moved relative to the previous row
				if ((field[x][y] & ~FLAG_BIT) != 0) {
					// |***|
					// | **| This row starts more to the right than the last
					do {
						if ((field[x][y] & REVEALED_BIT) == 0) {
							field[x][y] |= REVEALED_BIT;
							--remaining;
						}
						if (x >= prx) break rowLoop; // Scanned whole bottom of previous row
					} while (x + 1 < getWidth() && (field[++x][y] & ~FLAG_BIT) != 0);
					rx = x--;
				} else {
					// | **|
					// |*X*| This row extends further to the left than the last
					for (; x > 0; ) {
						if ((field[--x][y] & REVEALED_BIT) == 0) {
							field[x][y] |= REVEALED_BIT;
							--remaining;
						}
						// If extends above to the left (valid since row existed above)
						if ((field[x][y - 1] & ~FLAG_BIT) == 0)
							b = Bounds.combine(b, floodFill(scanTopLeftCorner(x, y - 1)));
						else if ((field[x][y - 1] & REVEALED_BIT) == 0) {
							field[x][y - 1] |= REVEALED_BIT;
							--remaining;
						}
						if ((field[x][y] & ~(REVEALED_BIT | FLAG_BIT)) != 0) break;
					}
				}
			} else if (x > 0) --x;

			// At this point x is at the leftmost revealed square and this row
			// is guaranteed to contain empty squares
			// Scan across to the right (include the non-emty square to the right of the row)
			for (; ; ++rx) {
				if ((field[rx][y] & REVEALED_BIT) == 0) {
					field[rx][y] |= REVEALED_BIT;
					--remaining;
				}
				if ((field[rx][y] & ~(REVEALED_BIT | FLAG_BIT)) != 0 || rx + 1 >= getWidth()) break;
			}
			// Now rx is the rightmost x-coordinate touched on this row

			if (rx < prx) {
				// |*** |
				// |*X *| This row is shorter than last; might reach further right
				for (int end = Math.min(prx, getWidth() - 1), sx = rx; ++sx <= end; ) {
					if ((field[sx][y] & ~FLAG_BIT) == 0) b = Bounds.combine(b, floodFill(new Coord(sx, y)));
					else if ((field[sx][y] & REVEALED_BIT) == 0) {
						field[sx][y] |= REVEALED_BIT;
						--remaining;
					}
				}
			} else if (rx > prx && y > 0) {
				// |** *|
				// |***X| If this row is longer than the last; could extend upwards to the right
				for (int ux = prx + 1; ux <= rx; ++ux) {
					if ((field[ux][y - 1] & ~FLAG_BIT) == 0)
						b = Bounds.combine(b, floodFill(scanTopLeftCorner(ux, y - 1)));
					else if ((field[ux][y - 1] & REVEALED_BIT) == 0) {
						field[ux][y - 1] |= REVEALED_BIT;
						--remaining;
					}
				}
			}

			if (x < b.minX) b.minX = x;
			if (rx >= b.maxX) b.maxX = rx + 1;
			prx = rx;
		} while (++y < getHeight());

		if (y >= b.maxY) b.maxY = Math.min(y + 1, getHeight());
		return b;
	}

	/**
	 * Clear a tile.
	 * <p>
	 * Mutates this {@link Board} in-place.
	 * Even if a mine was stumbled upon the flood fill will still complete.
	 * Any flags on squares that get revealed should be considered removed.
	 *
	 * @param coords The coordinates of the squares to clear.
	 * @return The result of the operation.
	 */
	public ClearTileResult clearTiles(Coord... coords) {
		boolean wasMine = Arrays.stream(coords).anyMatch(c -> (getTile(c) & (FLAG_BIT | MINE_BIT)) == MINE_BIT);
		Bounds bounds = Arrays.stream(coords)
				.peek(c -> {
					if (isOutOfBounds(c))
						throw new IllegalArgumentException("Specified coordinate is out of bounds.");
				})
				.filter(c -> (getTile(c) & (REVEALED_BIT | FLAG_BIT)) == 0)
				.map(c -> {
					if (getTile(c) != 0) {
						field[c.x][c.y] |= REVEALED_BIT;
						--remaining;
						return new Bounds(c);
					}
					return floodFill(scanTopLeftCorner(c.x, c.y)); // If empty: recursively clear adjacent squares
				})
				.reduce(Bounds::combine).orElse(Bounds.ZERO_SIZE);

		return new ClearTileResult(bounds, wasMine);
	}
}
