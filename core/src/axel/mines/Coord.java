package axel.mines;

public final class Coord implements Cloneable {
	public final int x, y;

	public Coord(int x, int y) {
		this.x = x;
		this.y = y;
	}

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
