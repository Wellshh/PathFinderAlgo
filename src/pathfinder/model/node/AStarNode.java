package pathfinder.model.node;

import pathfinder.model.Point;

public class AStarNode<P extends Point> extends BaseNode<P> implements Comparable<AStarNode<P>> {
  /** Distance from start location to current location */
  public double g = Double.POSITIVE_INFINITY;

  /** Heuristic estimation of distance from current location to destination */
  public double h = 0.0;

  /** Pointer to **predecessor** */
  public AStarNode<P> parent = null;

  public AStarNode(P pos) {
    super(pos);
  }

  /** The added cost function */
  public double getF() {
    return g + h;
  }

  @Override
  public int compareTo(AStarNode<P> other) {
    return Double.compare(this.getF(), other.getF());
  }
}
