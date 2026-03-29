package objects;

import java.util.List;

public interface Environment {
	double getTraversalCost(Coordinate from, Coordinate to);
	List<Coordinate> getNeighbors(Coordinate cur);
	double heuristic(Coordinate a, Coordinate b);
}
