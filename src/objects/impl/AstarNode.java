package objects.impl;

import objects.BaseNode;
import objects.Coordinate;

public class AstarNode extends BaseNode implements Comparable<AstarNode> {
	/** Distance from start location to current location */
	public double g = Double.POSITIVE_INFINITY;
	/** Heuristic estimation of distance from current location to destination */
	public double h = 0.0;
	/** Pointer to **predecessor** */
	public AstarNode parent = null; 
	
	public AstarNode(Coordinate pos) { super(pos); }
	/** The added cost function */
	public double getF() {return g + h; }
	
	@Override
	public int compareTo(AstarNode other) {
		return Double.compare(this.getF(), other.getF());
	}
	
}
