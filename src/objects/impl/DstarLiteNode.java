package objects.impl;

import objects.BaseNode;
import objects.Coordinate;

public class DstarLiteNode extends BaseNode implements Comparable<DstarLiteNode>{
	/** 
	 * The objective function value of distance to goal, initialized to infinity.
	 * Might be outdated if new obstacle is encountered by robot.
	 */
	public double g = Double.POSITIVE_INFINITY;
	/** 
	 * One-step lookahead based on what we know of the objective function value
	 * of distance to goal, initialized to infinity.
	 * Always updated - info from neighbors.
	 */
	public double rhs = Double.POSITIVE_INFINITY;
	/**
	 * The top key/priority of a node, 
	 * equals to the minimum of its g and rhs values plus
	 * a focusing heuristic, h: min(g(s), rhs(s)) + h(start, s)
	 */
	private double k_top = 0.0;
	/**
	 * The bottom key/priority of a node,
	 * equals to the minimum of its g and rhs values:
	 * min(g(s), rhs(s))
	 */
	private double k_bot = 0.0;
	
	public double getKtop() { return this.k_top;}
	
	public void setKtop(double val) { this.k_top = val; }
	
	public double getKbot() { return this.k_bot;}
	
	public void setKbot(double val) { this.k_bot = val; }
	
	/** Constant variable for comparison between Doubles */
	private static final double EPSILON = 1e-5;
	
	public DstarLiteNode(Coordinate pos) { super(pos); }
	
	public DstarLiteNode(DstarLiteNode copyFrom) {
		super(copyFrom.pos);
		this.g = copyFrom.g;
		this.rhs = copyFrom.rhs;
		this.setKbot(copyFrom.getKbot());
		this.setKtop(copyFrom.getKtop());
	}
	

	@Override
	public int compareTo(DstarLiteNode other) {
		if (this.k_top - DstarLiteNode.EPSILON > other.k_top ) return 1;
		else if (this.k_top < other.k_top - DstarLiteNode.EPSILON) return -1;
		return Double.compare(this.k_bot, other.k_bot);
	}
	
	public boolean lt(DstarLiteNode other) {
		return this.compareTo(other) < 0;
	}
	
	public boolean gt(DstarLiteNode other) {
	    return this.compareTo(other) > 0;
	}

	public boolean eq(DstarLiteNode other) {
	    return this.compareTo(other) == 0;
	}
	

}
