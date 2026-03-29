package pathfinder.model.node;

import pathfinder.model.Point;

public class DStarLiteNode<P extends Point> extends BaseNode<P>
    implements Comparable<DStarLiteNode<P>> {
  /**
   * The objective function value of distance to goal, initialized to infinity. Might be outdated if
   * new obstacle is encountered by robot.
   */
  public double g = Double.POSITIVE_INFINITY;

  /**
   * One-step lookahead based on what we know of the objective function value of distance to goal,
   * initialized to infinity. Always updated - info from neighbors.
   */
  public double rhs = Double.POSITIVE_INFINITY;

  /**
   * The top key/priority of a node, equals to the minimum of its g and rhs values plus a focusing
   * heuristic, h: min(g(s), rhs(s)) + h(start, s)
   */
  private double kTop = 0.0;

  /**
   * The bottom key/priority of a node, equals to the minimum of its g and rhs values: min(g(s),
   * rhs(s))
   */
  private double kBottom = 0.0;

  public double getKtop() {
    return this.kTop;
  }

  public void setKtop(double val) {
    this.kTop = val;
  }

  public double getKbot() {
    return this.kBottom;
  }

  public void setKbot(double val) {
    this.kBottom = val;
  }

  /** Constant variable for comparison between Doubles */
  private static final double EPSILON = 1e-5;

  public DStarLiteNode(P pos) {
    super(pos);
  }

  public DStarLiteNode(DStarLiteNode<P> copyFrom) {
    super(copyFrom.pos);
    this.g = copyFrom.g;
    this.rhs = copyFrom.rhs;
    this.setKbot(copyFrom.getKbot());
    this.setKtop(copyFrom.getKtop());
  }

  @Override
  public int compareTo(DStarLiteNode<P> other) {
    if (this.kTop - DStarLiteNode.EPSILON > other.kTop) return 1;
    else if (this.kTop < other.kTop - DStarLiteNode.EPSILON) return -1;
    return Double.compare(this.kBottom, other.kBottom);
  }

  public boolean lt(DStarLiteNode<P> other) {
    return this.compareTo(other) < 0;
  }

  public boolean gt(DStarLiteNode<P> other) {
    return this.compareTo(other) > 0;
  }

  public boolean eq(DStarLiteNode<P> other) {
    return this.compareTo(other) == 0;
  }
}
