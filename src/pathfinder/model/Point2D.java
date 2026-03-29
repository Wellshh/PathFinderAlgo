package pathfinder.model;

import java.util.Objects;

/**
 * Concrete 2D integer coordinate, replacing the former Coordinate class.
 */
public class Point2D extends Point {
	public final int x;
	public final int y;

	public Point2D(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Point2D)) return false;
		Point2D other = (Point2D) o;
		return x == other.x && y == other.y;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}
}
