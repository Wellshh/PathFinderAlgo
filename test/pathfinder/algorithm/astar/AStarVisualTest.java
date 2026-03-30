package pathfinder.algorithm.astar;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pathfinder.api.IPathFinder;
import pathfinder.model.Point2D;
import pathfinder.testbase.AbstractVisualPathFinderTest;
import pathfinder.testenv.environment.Grid2DEnvironment;
import pathfinder.testenv.visualizer.ConsoleGridVisualizer;

/**
 * Visual tests for A*. Each {@code @Test} prints a full console-rendered grid with per-phase
 * timing.
 *
 * <p>Run a single test from the IDE to inspect one scenario without noise from the others.
 */
@DisplayName("A* Visual Scenarios")
class AStarVisualTest extends AbstractVisualPathFinderTest<Point2D> {

  @Override
  protected IPathFinder<Point2D> createPathFinder() {
    return new AStarPathFinder<>();
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
    printTimingSummary("A* Simple 10x10");
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
    printTimingSummary("A* Wall-with-gap 15x15");
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
    printTimingSummary("A* No-path 10x10");
  }

  // --------------- Scenario 4: maze-like corridor ---------------

  @Test
  @DisplayName("Visual: Maze-like corridor on 10x10 grid")
  void visualMazeLikeCorridor() {
    int W = 10, H = 10;
    Grid2DEnvironment grid = new Grid2DEnvironment(W, H);

    for (int y = 0; y <= 7; y++) grid.setObstacle(3, y);
    for (int y = 2; y <= 9; y++) grid.setObstacle(6, y);

    env = grid;
    start = new Point2D(1, 5);
    goal = new Point2D(8, 5);
    visualizer = new ConsoleGridVisualizer(W, H);

    List<Point2D> path = initAndComputePath();

    System.out.println("Path length = " + path.size());
    printTimingSummary("A* Maze-corridor 10x10");
  }

  // --------------- Scenario 5: large grid walk-through ---------------

  @Test
  @DisplayName("Visual: Large 20x20 grid with wall and gap")
  void visualLargeGrid() {
    int W = 20, H = 20;
    Grid2DEnvironment grid = new Grid2DEnvironment(W, H);

    for (int y = 2; y <= 17; y++) {
      if (y >= 9 && y <= 11) continue;
      grid.setObstacle(10, y);
    }

    env = grid;
    start = new Point2D(2, 10);
    goal = new Point2D(17, 10);
    visualizer = new ConsoleGridVisualizer(W, H);

    List<Point2D> path = initAndComputePath();

    System.out.println("Path length = " + path.size());

    // Walk the path step-by-step with visualization
    System.out.println("[Walk] Robot begins...");
    Point2D robotPos = start;
    int step = 0;

    while (!robotPos.equals(goal)) {
      step++;
      Point2D next = pathFinder.getNextWaypoint(robotPos);
      if (next == null) {
        System.out.println("[Step " + step + "] BLOCKED -- no waypoint");
        break;
      }
      robotPos = next;

      if (step % 3 == 0 || robotPos.equals(goal)) {
        visualizer.renderWithRobot(env, start, goal, path, robotPos, "Walking step " + step);
      }
    }

    System.out.println(
        robotPos.equals(goal)
            ? "[Walk] Reached goal in " + step + " steps."
            : "[Walk] FAILED after " + step + " steps.");

    printTimingSummary("A* Large-grid 20x20");
  }
}
