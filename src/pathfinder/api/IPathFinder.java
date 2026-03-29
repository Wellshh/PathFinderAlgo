package pathfinder.api;

import pathfinder.model.Environment;
import pathfinder.model.Point;

/**
 * Base interface for all path planning algorithms (static and dynamic).
 * @param <P> the spatial coordinate type
 */
public interface IPathFinder<P extends Point> {

	/** Path container to store the path */
	PathContainer<P> getPath();

	/**
	 * Initializes the planner with the environment topology, start, and goal.
	 */
	void initialize(Environment<P> env, P start, P goal);

	/**
	 * Returns the next optimal node to move to from the current position.
	 * Returns null if no path exists.
	 */
	P getNextWaypoint(P current);

	/**
	 * Computes the full path from start to goal, update the internal path data structure.
	 */
	void computePath();
}
