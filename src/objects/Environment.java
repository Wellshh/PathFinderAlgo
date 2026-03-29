package objects;

import java.util.List;

public interface Environment {
	double getTraversalCost(Coordinate from, Coordinate to);
	double heuristic(Coordinate a, Coordinate b);
	
	List<Coordinate> getNeighbors(Coordinate cur);
	List<Coordinate> getSuccessors(Coordinate cur);
	List<Coordinate> getPredecessors(Coordinate cur);
}
