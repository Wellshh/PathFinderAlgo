package pathfinder.algorithm.dstarlite;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pathfinder.api.IPathFinder;
import pathfinder.model.EdgeUpdate;
import pathfinder.model.Point2D;
import pathfinder.testbase.AbstractVisualPathFinderTest;
import pathfinder.testenv.environment.Grid2DEnvironment;
import pathfinder.testenv.sensor.SimpleGridSensor;
import pathfinder.testenv.visualizer.ConsoleGridVisualizer;

/**
 * Visual tests for D* Lite. Each {@code @Test} prints a full console-rendered walk-through with
 * per-phase timing.
 *
 * <p>Run a single test from the IDE to inspect one scenario without noise from the others.
 */
@DisplayName("D* Lite Visual Scenarios")
class DStarLiteVisualTest extends AbstractVisualPathFinderTest<Point2D> {

  @Override
  protected IPathFinder<Point2D> createPathFinder() {
    return new DStarLitePathFinder<>();
  }

  // --------------- Scenario 1: simple open grid ---------------

  @Test
  @DisplayName("Visual: Diagonal path on 10x10 open grid")
  void visualSimplePath() {
    env = new Grid2DEnvironment(10, 10);
    start = new Point2D(0, 0);
    goal = new Point2D(9, 9);
    visualizer = new ConsoleGridVisualizer(10, 10);

    List<Point2D> path = initAndComputePath();

    System.out.println("Path length = " + path.size());
    printTimingSummary("Simple 10x10");
  }

  // --------------- Scenario 2: wall with gap ---------------

  @Test
  @DisplayName("Visual: Wall with gap on 15x15 grid")
  void visualWallWithGap() {
    int W = 15, H = 15;
    Grid2DEnvironment grid = new Grid2DEnvironment(W, H);

    for (int y = 0; y < H; y++) {
      if (y != H / 2) grid.setObstacle(W / 2, y);
    }

    env = grid;
    start = new Point2D(1, H / 2);
    goal = new Point2D(W - 2, H / 2);
    visualizer = new ConsoleGridVisualizer(W, H);

    List<Point2D> path = initAndComputePath();

    System.out.println("Path length = " + path.size());
    printTimingSummary("Wall-with-gap 15x15");
  }

  // --------------- Scenario 3: no path (blocked goal) ---------------

  @Test
  @DisplayName("Visual: Goal surrounded -- no path")
  void visualNoPath() {
    int W = 10, H = 10;
    Grid2DEnvironment grid = new Grid2DEnvironment(W, H);
    Point2D g = new Point2D(5, 5);

    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        if (dx == 0 && dy == 0) continue;
        grid.setObstacle(g.x + dx, g.y + dy);
      }
    }

    env = grid;
    start = new Point2D(0, 0);
    goal = g;
    visualizer = new ConsoleGridVisualizer(W, H);

    List<Point2D> path = initAndComputePath();

    System.out.println("Path empty = " + path.isEmpty());
    printTimingSummary("No-path 10x10");
  }

  // --------------- Scenario 4: dynamic replan walk-through ---------------

  @Test
  @DisplayName("Visual: Dynamic replan with hidden wall (20x20)")
  void visualDynamicReplan() {
    int W = 20, H = 20;
    Grid2DEnvironment knownMap = new Grid2DEnvironment(W, H);
    Grid2DEnvironment groundTruth = new Grid2DEnvironment(W, H);

    // Shared partial wall with known gap
    for (int y = 2; y <= 13; y++) {
      if (y >= 9 && y <= 11) continue;
      knownMap.setObstacle(10, y);
      groundTruth.setObstacle(10, y);
    }
    // Hidden obstacles block the gap
    groundTruth.setObstacle(10, 9);
    groundTruth.setObstacle(10, 10);
    groundTruth.setObstacle(10, 11);

    env = knownMap;
    start = new Point2D(2, 10);
    goal = new Point2D(17, 10);
    visualizer = new ConsoleGridVisualizer(W, H);

    List<Point2D> path = initAndComputePath();

    // --- Walking simulation with dynamic sensing ---
    DStarLitePathFinder<Point2D> pf = (DStarLitePathFinder<Point2D>) pathFinder;
    SimpleGridSensor sensor = new SimpleGridSensor(groundTruth, knownMap, 3);

    System.out.println("[Walk] Robot begins...");
    Point2D robotPos = start;
    List<Point2D> currentPath = path;
    int step = 0;

    while (!robotPos.equals(goal)) {
      step++;
      List<EdgeUpdate<Point2D>> updates = sensor.sense(robotPos);

      if (!updates.isEmpty()) {
        System.out.printf("[Step %d] at %s -- %d edge changes%n", step, robotPos, updates.size());

        long currentTime = System.nanoTime();
        pf.setStart(robotPos);
        pf.updateAllEdgeCosts(updates);
        pf.computePath();
        long replanNanos = System.nanoTime() - currentTime;

        recordTiming("Replan_step" + step, replanNanos);
        currentPath = pf.getPath().toList();

        visualizer.renderWithRobot(
            env, start, goal, currentPath, robotPos, "Replan at step " + step);
      }

      Point2D next = pf.getNextWaypoint(robotPos);
      if (next == null) {
        System.out.println("[Step " + step + "] BLOCKED -- no waypoint");
        break;
      }
      robotPos = next;

      if (step % 5 == 0 || robotPos.equals(goal)) {
        visualizer.renderWithRobot(env, start, goal, currentPath, robotPos, "Walking step " + step);
      }
    }

    System.out.println(
        robotPos.equals(goal)
            ? "[Walk] Reached goal in " + step + " steps."
            : "[Walk] FAILED after " + step + " steps.");

    printTimingSummary("Dynamic-replan 20x20");
  }
}
