package pathfinder.model.node;

import pathfinder.model.Point;
import pathfinder.util.HasPriorityKey;

public class AStarNode<P extends Point> extends BaseNode<P>
    implements Comparable<AStarNode<P>>, HasPriorityKey<Double> {
  /* Node id */
  public final int id;

  /** Distance from start location to current location */
  public double g = Double.POSITIVE_INFINITY;

  /** Heuristic estimation of distance from current location to destination */
  public double h = 0.0;

  /** Pointer to **predecessor** */
  public AStarNode<P> parent = null;

  public AStarNode(P pos, int id) {
    super(pos);
    this.id = id;
  }

  /** The added cost function */
  public double getF() {
    return g + h;
  }

  @Override
  public int compareTo(AStarNode<P> other) {
    int fComp = Double.compare(this.getF(), other.getF());
    if (fComp != 0) return fComp;
    // tie-breaking by h value
    else return Double.compare(h, other.h);
  }

  @Override
  public Double getPriorityKey() {
    return getF();
  }

  @Override
  public void setPriorityKey(Double key) {
    this.g = key - h;
  }
}
