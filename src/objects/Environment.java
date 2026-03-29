package objects;

import java.util.List;

public interface Environment<N> {
	double getTraversalCost(N from, N to);
	double heuristic(N a, N b);
	
	List<N> getNeighbors(N cur);
	List<N> getSuccessors(N cur);
	List<N> getPredecessors(N cur);
}
