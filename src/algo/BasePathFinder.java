package algo;

import java.util.List;

import objects.Coordinate;

public interface BasePathFinder {
	void initialize(Coordinate start, Coordinate goal);
	List<Coordinate> computePath();
	
}
