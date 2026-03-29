package objects;

import java.util.Objects;

public class Coordinate {
	public final int x;
	public final int y;
	
	public Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Coordinate)) return false;
		Coordinate other = (Coordinate) o;
		return x == other.x && y == other.y;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}
}
