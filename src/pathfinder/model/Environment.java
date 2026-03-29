package pathfinder.model;

import java.util.List;

/**
 * Defines the abstract topology of the environment for path planning algorithms.
 * @param <P> the spatial coordinate type
 */
public interface Environment<P extends Point> {
	double getTraversalCost(P from, P to);
	double heuristic(P a, P b);

	List<P> getNeighbors(P cur);
	List<P> getSuccessors(P cur);
	List<P> getPredecessors(P cur);
}
