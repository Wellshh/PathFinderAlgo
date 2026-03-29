package pathfinder.model;

import java.util.List;

/**
 * Defines the abstract topology of the environment for path planning algorithms.
 *
 * @param <P> the spatial coordinate type
 */
public interface Environment<P extends Point> {
  /** Get traversal cost for a directed edge */
  double getTraversalCost(P from, P to);

  /** Update cost function for a directed edge */
  void setTraversalCost(P from, P to, double cost);

  /** Get heuristic distance between two nodes */
  double heuristic(P a, P b);

  /** Get all neighbors of a node */
  List<P> getNeighbors(P cur);

  /** Get all successors of a node */
  List<P> getSuccessors(P cur);

  /** Get all predecessors of a node */
  List<P> getPredecessors(P cur);
}
