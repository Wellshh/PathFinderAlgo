package pathfinder.testenv.environment;

import java.util.ArrayList;
import java.util.List;

import pathfinder.model.Environment;
import pathfinder.model.Point2D;

/**
 * Concrete 2D grid implementation of {@link Environment} for testing.
 * Supports 8-directional movement with configurable cell costs.
 * Obstacle cells have cost {@code Double.POSITIVE_INFINITY}.
 */
public class Grid2DEnvironment implements Environment<Point2D> {

	private static final double SQRT2 = Math.sqrt(2.0);

	private static final int[][] DIRS = {
		{-1, -1}, {-1, 0}, {-1, 1},
		{ 0, -1},          { 0, 1},
		{ 1, -1}, { 1, 0}, { 1, 1}
	};

	private final int width;
	private final int height;

	/**
	 * Per-cell traversal weight. A value of {@code Double.POSITIVE_INFINITY}
	 * means the cell is blocked (obstacle). The edge cost from A to B equals
	 * {@code cellCost[B.y][B.x] * distance(A,B)}.
	 */
	private final double[][] cellCost;

	public Grid2DEnvironment(int width, int height) {
		this.width = width;
		this.height = height;
		this.cellCost = new double[height][width];
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				cellCost[r][c] = 1.0;
			}
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setObstacle(int x, int y) {
		cellCost[y][x] = Double.POSITIVE_INFINITY;
	}

	public void removeObstacle(int x, int y) {
		cellCost[y][x] = 1.0;
	}

	public boolean isObstacle(int x, int y) {
		return cellCost[y][x] == Double.POSITIVE_INFINITY;
	}

	public boolean inBounds(int x, int y) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}

	// -------- Environment<Point2D> contract --------

	@Override
	public double getTraversalCost(Point2D from, Point2D to) {
		if (!inBounds(to.x, to.y) || isObstacle(to.x, to.y)) {
			return Double.POSITIVE_INFINITY;
		}
		boolean diagonal = (from.x != to.x) && (from.y != to.y);
		return cellCost[to.y][to.x] * (diagonal ? SQRT2 : 1.0);
	}

	@Override
	public void setTraversalCost(Point2D from, Point2D to, double cost) {
		if (inBounds(to.x, to.y)) {
			cellCost[to.y][to.x] = cost;
		}
	}

	@Override
	public double heuristic(Point2D a, Point2D b) {
		int dx = Math.abs(a.x - b.x);
		int dy = Math.abs(a.y - b.y);
		int minD = Math.min(dx, dy);
		int maxD = Math.max(dx, dy);
		return minD * SQRT2 + (maxD - minD);
	}

	@Override
	public List<Point2D> getNeighbors(Point2D cur) {
		List<Point2D> neighbors = new ArrayList<>(8);
		for (int[] d : DIRS) {
			int nx = cur.x + d[1];
			int ny = cur.y + d[0];
			if (inBounds(nx, ny) && !isObstacle(nx, ny)) {
				neighbors.add(new Point2D(nx, ny));
			}
		}
		return neighbors;
	}

	@Override
	public List<Point2D> getSuccessors(Point2D cur) {
		return getNeighbors(cur);
	}

	@Override
	public List<Point2D> getPredecessors(Point2D cur) {
		return getNeighbors(cur);
	}
}
