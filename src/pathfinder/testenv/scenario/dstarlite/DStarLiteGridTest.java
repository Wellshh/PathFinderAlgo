package pathfinder.testenv.scenario.dstarlite;

import java.util.List;

import pathfinder.algorithm.dstarlite.DStarLitePathFinder;
import pathfinder.api.IPathFinder;
import pathfinder.model.EdgeUpdate;
import pathfinder.model.Point2D;
import pathfinder.testenv.core.AbstractPathFinderTest;
import pathfinder.testenv.environment.Grid2DEnvironment;
import pathfinder.testenv.sensor.SimpleGridSensor;
import pathfinder.testenv.visualizer.ConsoleGridVisualizer;

/**
 * Concrete test for D* Lite on a 20x20 grid.
 *
 * Scenario layout:
 * - Vertical wall at x=10, from y=2..13 with a known gap at y=9..11
 * - Ground truth has hidden obstacles at (10,9), (10,10), (10,11) that
 * completely block the gap.
 * - Start: (2,10) Goal: (17,10)
 * - Initial path goes through the gap; after sensing, the robot must
 * replan around the south end of the wall.
 */
public class DStarLiteGridTest extends AbstractPathFinderTest<Point2D> {

	private static final int W = 20;
	private static final int H = 20;

	private Grid2DEnvironment groundTruth;
	private SimpleGridSensor sensor;

	@Override
	protected String getTestName() {
		return "D* Lite - Dynamic Replan (20x20)";
	}

	@Override
	protected void setupEnvironment() {
		Grid2DEnvironment knownMap = new Grid2DEnvironment(W, H);
		groundTruth = new Grid2DEnvironment(W, H);

		// Vertical wall at x=10, y=2..13 on both maps.
		for (int y = 2; y <= 13; y++) {
			if (y >= 9 && y <= 11) {
				continue; // known gap
			}
			knownMap.setObstacle(10, y);
			groundTruth.setObstacle(10, y);
		}

		// Hidden obstacles that block the gap (ground truth only).
		groundTruth.setObstacle(10, 9);
		groundTruth.setObstacle(10, 10);
		groundTruth.setObstacle(10, 11);

		this.env = knownMap;
		this.start = new Point2D(2, 10);
		this.goal = new Point2D(17, 10);
		this.sensor = new SimpleGridSensor(groundTruth, knownMap, 3);
		this.visualizer = new ConsoleGridVisualizer(W, H);
	}

	@Override
	protected IPathFinder<Point2D> createPathFinder() {
		return new DStarLitePathFinder<>();
	}

	@Override
	protected void runDynamicScenario() {
		DStarLitePathFinder<Point2D> dynPF = (DStarLitePathFinder<Point2D>) pathFinder;
		List<Point2D> currentPath = pathFinder.getPath().toList();

		System.out.println("[Dynamic] Robot starts walking along the initial path...");

		Point2D robotPos = start;
		int step = 0;

		while (!robotPos.equals(goal)) {
			step++;

			// Sense environment from current position.
			long currentTime = System.nanoTime();
			List<EdgeUpdate<Point2D>> updates = sensor.sense(robotPos);
			long senseNanos = System.nanoTime() - currentTime;

			if (!updates.isEmpty()) {
				System.out.printf("[Step %d] Robot at (%d,%d) -- detected %d edge changes (sense: %.3f ms)%n",
						step, robotPos.x, robotPos.y, updates.size(),
						senseNanos / 1_000_000.0);

				// D* Lite V2: advance start to robot's current position, then replan.
				currentTime = System.nanoTime();
				dynPF.setStart(robotPos);
				dynPF.updateAllEdgeCosts(updates);
				dynPF.computePath();
				long replanNanos = System.nanoTime() - currentTime;

				recordTiming("Replan_Step" + step, replanNanos);

				currentPath = pathFinder.getPath().toList();
				System.out.printf("[Step %d] Replanned in %.3f ms, new path length = %d%n",
						step, replanNanos / 1_000_000.0, currentPath.size());

				visualizer.renderWithRobot(env, start, goal, currentPath, robotPos,
						"After Replan (step " + step + ")");
			}

			// Advance robot one step.
			Point2D next = pathFinder.getNextWaypoint(robotPos);
			if (next == null) {
				System.out.printf("[Step %d] No waypoint available -- path blocked!%n", step);
				break;
			}
			robotPos = next;

			// Render every few steps to avoid flooding the console.
			if (step % 5 == 0 || robotPos.equals(goal)) {
				visualizer.renderWithRobot(env, start, goal, currentPath, robotPos,
						"Walking (step " + step + ")");
			}
		}

		if (robotPos.equals(goal)) {
			System.out.printf("[Dynamic] Robot reached goal in %d steps.%n", step);
		} else {
			System.out.printf("[Dynamic] Robot FAILED to reach goal after %d steps.%n", step);
		}
	}
}
