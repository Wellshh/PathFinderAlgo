package pathfinder.algorithm.dstarlite;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pathfinder.model.EdgeUpdate;
import pathfinder.model.Point2D;
import pathfinder.testenv.environment.Grid2DEnvironment;
import pathfinder.testenv.sensor.SimpleGridSensor;

@DisplayName("D* Lite Correctness")
class DStarLiteCorrectnessTest {

	// --------------- helpers ---------------

	private DStarLitePathFinder<Point2D> initAndCompute(
			Grid2DEnvironment env, Point2D start, Point2D goal) {
		DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
		pf.initialize(env, start, goal);
		pf.computePath();
		return pf;
	}

	private void assertPathConnected(List<Point2D> path,
			Grid2DEnvironment env) {
		for (int i = 0; i < path.size() - 1; i++) {
			Point2D a = path.get(i);
			Point2D b = path.get(i + 1);
			int dx = Math.abs(a.x - b.x);
			int dy = Math.abs(a.y - b.y);
			assertTrue(dx <= 1 && dy <= 1 && (dx + dy > 0),
					"Consecutive points must be grid-adjacent: " + a + " -> " + b);
			assertNotEquals(Double.POSITIVE_INFINITY,
					env.getTraversalCost(a, b),
					"Edge must be traversable: " + a + " -> " + b);
		}
	}

	private void assertNoObstacleOnPath(List<Point2D> path,
			Grid2DEnvironment env) {
		for (Point2D p : path) {
			assertFalse(env.isObstacle(p.x, p.y),
					"Path must not cross obstacle at " + p);
		}
	}

	// ===================================================================
	// 1. Parameterized simple-path tests (open grid, varying size)
	// ===================================================================

	@Nested
	@DisplayName("1. Simple path -- open grid")
	class SimplePath {

		@ParameterizedTest(name = "{0}x{1} grid")
		@CsvSource({"5,5", "10,10", "20,20", "50,50"})
		void diagonalPathOnOpenGrid(int w, int h) {
			Grid2DEnvironment env = new Grid2DEnvironment(w, h);
			Point2D start = new Point2D(0, 0);
			Point2D goal  = new Point2D(w - 1, h - 1);

			DStarLitePathFinder<Point2D> pf = initAndCompute(env, start, goal);
			List<Point2D> path = pf.getPath().toList();

			assertFalse(path.isEmpty(), "Path should exist");
			assertEquals(start, path.get(0), "Path starts at start");
			assertEquals(goal, path.get(path.size() - 1), "Path ends at goal");

			// Optimal diagonal: max(w-1, h-1) steps + 1 node
			int expectedLen = Math.max(w - 1, h - 1) + 1;
			assertEquals(expectedLen, path.size(),
					"Optimal diagonal path length on " + w + "x" + h);
			assertPathConnected(path, env);
		}

		@ParameterizedTest(name = "{0}x{1} grid, straight horizontal")
		@CsvSource({"10,5", "20,3", "50,1"})
		void straightHorizontal(int w, int h) {
			Grid2DEnvironment env = new Grid2DEnvironment(w, h);
			Point2D start = new Point2D(0, 0);
			Point2D goal  = new Point2D(w - 1, 0);

			DStarLitePathFinder<Point2D> pf = initAndCompute(env, start, goal);
			List<Point2D> path = pf.getPath().toList();

			assertFalse(path.isEmpty());
			assertEquals(start, path.get(0));
			assertEquals(goal, path.get(path.size() - 1));
			assertEquals(w, path.size(), "Horizontal path length");
			assertPathConnected(path, env);
		}
	}

	// ===================================================================
	// 2. Paths with known obstacles
	// ===================================================================

	@Nested
	@DisplayName("2. Path with obstacles")
	class WithObstacles {

		@ParameterizedTest(name = "{0}x{1} grid, wall with one gap")
		@CsvSource({"10,10", "20,20", "30,30"})
		void wallWithSingleGap(int w, int h) {
			Grid2DEnvironment env = new Grid2DEnvironment(w, h);
			int wallX = w / 2;
			int gapY  = h / 2;

			for (int y = 0; y < h; y++) {
				if (y != gapY) env.setObstacle(wallX, y);
			}

			Point2D start = new Point2D(1, h / 2);
			Point2D goal  = new Point2D(w - 2, h / 2);

			DStarLitePathFinder<Point2D> pf = initAndCompute(env, start, goal);
			List<Point2D> path = pf.getPath().toList();

			assertFalse(path.isEmpty(), "Path should exist through gap");
			assertEquals(start, path.get(0));
			assertEquals(goal, path.get(path.size() - 1));
			assertNoObstacleOnPath(path, env);
			assertPathConnected(path, env);

			// Must pass through the gap column
			boolean passesGap = path.stream()
					.anyMatch(p -> p.x == wallX && p.y == gapY);
			assertTrue(passesGap, "Path must pass through the wall gap");
		}

		@Test
		@DisplayName("Maze-like corridor")
		void mazeLikeCorridor() {
			Grid2DEnvironment env = new Grid2DEnvironment(10, 10);

			// Two staggered walls creating a corridor
			for (int y = 0; y <= 7; y++) env.setObstacle(3, y);
			for (int y = 2; y <= 9; y++) env.setObstacle(6, y);

			Point2D start = new Point2D(1, 5);
			Point2D goal  = new Point2D(8, 5);

			DStarLitePathFinder<Point2D> pf = initAndCompute(env, start, goal);
			List<Point2D> path = pf.getPath().toList();

			assertFalse(path.isEmpty());
			assertEquals(start, path.get(0));
			assertEquals(goal, path.get(path.size() - 1));
			assertNoObstacleOnPath(path, env);
			assertPathConnected(path, env);
		}
	}

	// ===================================================================
	// 3. No-path scenarios
	// ===================================================================

	@Nested
	@DisplayName("3. No path")
	class NoPath {

		@Test
		@DisplayName("Goal completely surrounded by obstacles")
		void goalSurrounded() {
			Grid2DEnvironment env = new Grid2DEnvironment(10, 10);
			Point2D goal = new Point2D(5, 5);

			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx == 0 && dy == 0) continue;
					env.setObstacle(goal.x + dx, goal.y + dy);
				}
			}

			Point2D start = new Point2D(0, 0);
			DStarLitePathFinder<Point2D> pf = initAndCompute(env, start, goal);

			assertTrue(pf.getPath().isEmpty(),
					"Path should be empty when goal is walled off");
		}

		@Test
		@DisplayName("Start completely surrounded by obstacles")
		void startSurrounded() {
			Grid2DEnvironment env = new Grid2DEnvironment(10, 10);
			Point2D start = new Point2D(5, 5);

			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx == 0 && dy == 0) continue;
					env.setObstacle(start.x + dx, start.y + dy);
				}
			}

			Point2D goal = new Point2D(9, 9);
			DStarLitePathFinder<Point2D> pf = initAndCompute(env, start, goal);

			assertTrue(pf.getPath().isEmpty(),
					"Path should be empty when start is walled off");
		}

		@Test
		@DisplayName("Full wall with no gap divides the grid")
		void fullWallNoGap() {
			Grid2DEnvironment env = new Grid2DEnvironment(10, 10);

			for (int y = 0; y < 10; y++) env.setObstacle(5, y);

			Point2D start = new Point2D(2, 5);
			Point2D goal  = new Point2D(8, 5);
			DStarLitePathFinder<Point2D> pf = initAndCompute(env, start, goal);

			assertTrue(pf.getPath().isEmpty(),
					"Path should be empty when wall fully divides grid");
		}
	}

	// ===================================================================
	// 4. Dynamic replanning (obstacle detection & edge cost updates)
	// ===================================================================

	@Nested
	@DisplayName("4. Dynamic replan")
	class DynamicReplan {

		@Test
		@DisplayName("Hidden wall blocks initial path, robot replans and reaches goal")
		void hiddenWallReplan() {
			Grid2DEnvironment knownMap    = new Grid2DEnvironment(20, 20);
			Grid2DEnvironment groundTruth = new Grid2DEnvironment(20, 20);

			// Both maps share a partial wall
			for (int y = 2; y <= 13; y++) {
				if (y >= 9 && y <= 11) continue;  // known gap
				knownMap.setObstacle(10, y);
				groundTruth.setObstacle(10, y);
			}
			// Ground truth blocks the gap
			groundTruth.setObstacle(10, 9);
			groundTruth.setObstacle(10, 10);
			groundTruth.setObstacle(10, 11);

			Point2D start = new Point2D(2, 10);
			Point2D goal  = new Point2D(17, 10);

			DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
			pf.initialize(knownMap, start, goal);
			pf.computePath();

			List<Point2D> initialPath = pf.getPath().toList();
			assertFalse(initialPath.isEmpty(), "Initial path should exist");

			// Simulate walk with sensing
			SimpleGridSensor sensor = new SimpleGridSensor(groundTruth, knownMap, 3);
			Point2D robotPos = start;
			boolean replanned = false;

			for (int step = 0; step < 50 && !robotPos.equals(goal); step++) {
				List<EdgeUpdate<Point2D>> updates = sensor.sense(robotPos);
				if (!updates.isEmpty()) {
					pf.setStart(robotPos);
					pf.updateAllEdgeCosts(updates);
					pf.computePath();
					replanned = true;
				}
				Point2D next = pf.getNextWaypoint(robotPos);
				if (next == null) break;
				robotPos = next;
			}

			assertTrue(replanned, "Should have triggered at least one replan");
			assertEquals(goal, robotPos, "Robot must reach goal after replan");
		}

		@Test
		@DisplayName("Hidden gap-block forces detour on 15x15 grid")
		void hiddenGapBlock15x15() {
			Grid2DEnvironment knownMap    = new Grid2DEnvironment(15, 15);
			Grid2DEnvironment groundTruth = new Grid2DEnvironment(15, 15);

			// Known wall at x=7, y=0..12 with visible gap at y=6..8
			for (int y = 0; y <= 12; y++) {
				if (y >= 6 && y <= 8) continue;
				knownMap.setObstacle(7, y);
				groundTruth.setObstacle(7, y);
			}
			// Hidden: ground truth blocks the gap
			groundTruth.setObstacle(7, 6);
			groundTruth.setObstacle(7, 7);
			groundTruth.setObstacle(7, 8);

			Point2D start = new Point2D(2, 7);
			Point2D goal  = new Point2D(12, 7);

			DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
			pf.initialize(knownMap, start, goal);
			pf.computePath();
			assertFalse(pf.getPath().isEmpty(), "Initial path through gap");

			SimpleGridSensor sensor = new SimpleGridSensor(groundTruth, knownMap, 3);
			Point2D robotPos = start;
			boolean replanned = false;

			for (int step = 0; step < 50 && !robotPos.equals(goal); step++) {
				List<EdgeUpdate<Point2D>> updates = sensor.sense(robotPos);
				if (!updates.isEmpty()) {
					pf.setStart(robotPos);
					pf.updateAllEdgeCosts(updates);
					pf.computePath();
					replanned = true;
				}
				Point2D next = pf.getNextWaypoint(robotPos);
				if (next == null) break;
				robotPos = next;
			}

			assertTrue(replanned, "Should have triggered a replan");
			assertEquals(goal, robotPos, "Robot must reach goal via south detour");
		}

		@Test
		@DisplayName("Two hidden walls trigger multiple replans")
		void multipleReplans() {
			// 30x20 grid with two known walls (x=10, x=20), each with a
			// visible gap. Ground truth blocks both gaps, forcing two replans.
			Grid2DEnvironment knownMap    = new Grid2DEnvironment(30, 20);
			Grid2DEnvironment groundTruth = new Grid2DEnvironment(30, 20);

			// Wall 1: x=10, y=2..17, known gap at y=9..11
			for (int y = 2; y <= 17; y++) {
				if (y >= 9 && y <= 11) continue;
				knownMap.setObstacle(10, y);
				groundTruth.setObstacle(10, y);
			}
			groundTruth.setObstacle(10, 9);
			groundTruth.setObstacle(10, 10);
			groundTruth.setObstacle(10, 11);

			// Wall 2: x=20, y=2..17, known gap at y=9..11
			for (int y = 2; y <= 17; y++) {
				if (y >= 9 && y <= 11) continue;
				knownMap.setObstacle(20, y);
				groundTruth.setObstacle(20, y);
			}
			groundTruth.setObstacle(20, 9);
			groundTruth.setObstacle(20, 10);
			groundTruth.setObstacle(20, 11);

			Point2D start = new Point2D(2, 10);
			Point2D goal  = new Point2D(27, 10);

			DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
			pf.initialize(knownMap, start, goal);
			pf.computePath();
			assertFalse(pf.getPath().isEmpty());

			SimpleGridSensor sensor = new SimpleGridSensor(groundTruth, knownMap, 3);
			Point2D robotPos = start;
			Set<Integer> replanSteps = new HashSet<>();

			for (int step = 0; step < 120 && !robotPos.equals(goal); step++) {
				List<EdgeUpdate<Point2D>> updates = sensor.sense(robotPos);
				if (!updates.isEmpty()) {
					pf.setStart(robotPos);
					pf.updateAllEdgeCosts(updates);
					pf.computePath();
					replanSteps.add(step);
				}
				Point2D next = pf.getNextWaypoint(robotPos);
				if (next == null) break;
				robotPos = next;
			}

			assertTrue(replanSteps.size() >= 2,
					"Should trigger at least 2 replans for 2 walls (found "
					+ replanSteps.size() + ")");
			assertEquals(goal, robotPos, "Robot must reach goal");
		}
	}
}
