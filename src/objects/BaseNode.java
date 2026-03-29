package objects;

public abstract class BaseNode {
	public final Coordinate pos;
	
	public BaseNode(Coordinate pos) {
		this.pos = pos;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BaseNode)) return false;
		return this.pos.equals(((BaseNode) o).pos);
	}
	
	@Override
	public int hashCode() {
		return pos.hashCode();
	}
}
