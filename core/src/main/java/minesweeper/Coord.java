package minesweeper;

/**
 * The 2-D coordinate of a tile.
 */
public final class Coord implements Cloneable {
	/**
	 * The x-coordinate.
	 */
	public final int x,
	/**
	 * The y-coordinate.
	 */
	y;

	/**
	 * Constructs a new coordinate using the specified values.
	 */
	public Coord(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Constructs a copy of the specified coordinate.
	 */
	public Coord(Coord c) {
		this(c.x, c.y);
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ')';
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Coord && ((Coord) obj).x == x && ((Coord) obj).y == y;
	}

	@Override
	public int hashCode() {
		return x * 31 + y;
	}

	public Coord clone() throws CloneNotSupportedException {
		return (Coord) super.clone();
	}
}
