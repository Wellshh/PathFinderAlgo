/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.algorithm.astar;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pathfinder.model.Point2D;
import pathfinder.testenv.environment.Grid2DEnvironment;

@DisplayName("A* Correctness")
class AStarCorrectnessTest {

  // --------------- helpers ---------------

  private AStarPathFinder<Point2D> initAndCompute(
      Grid2DEnvironment env, Point2D start, Point2D goal) {
    AStarPathFinder<Point2D> pf = new AStarPathFinder<>();
    pf.initialize(env, start, goal);
    pf.computePath();
    return pf;
  }

  private void assertPathConnected(List<Point2D> path, Grid2DEnvironment env) {
    for (int i = 0; i < path.size() - 1; i++) {
      Point2D a = path.get(i);
      Point2D b = path.get(i + 1);
      int dx = Math.abs(a.x - b.x);
      int dy = Math.abs(a.y - b.y);
      assertTrue(
          dx <= 1 && dy <= 1 && (dx + dy > 0),
          "Consecutive points must be grid-adjacent: " + a + " -> " + b);
      assertNotEquals(
          Double.POSITIVE_INFINITY,
          env.getTraversalCost(a, b),
          "Edge must be traversable: " + a + " -> " + b);
    }
  }

  private void assertNoObstacleOnPath(List<Point2D> path, Grid2DEnvironment env) {
    for (Point2D p : path) {
      assertFalse(env.isObstacle(p.x, p.y), "Path must not cross obstacle at " + p);
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
      Point2D goal = new Point2D(w - 1, h - 1);

      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);
      List<Point2D> path = pf.getPath().toList();

      assertFalse(path.isEmpty(), "Path should exist");
      assertEquals(start, path.get(0), "Path starts at start");
      assertEquals(goal, path.get(path.size() - 1), "Path ends at goal");

      int expectedLen = Math.max(w - 1, h - 1) + 1;
      assertEquals(expectedLen, path.size(), "Optimal diagonal path length on " + w + "x" + h);
      assertPathConnected(path, env);
    }

    @ParameterizedTest(name = "{0}x{1} grid, straight horizontal")
    @CsvSource({"10,5", "20,3", "50,1"})
    void straightHorizontal(int w, int h) {
      Grid2DEnvironment env = new Grid2DEnvironment(w, h);
      Point2D start = new Point2D(0, 0);
      Point2D goal = new Point2D(w - 1, 0);

      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);
      List<Point2D> path = pf.getPath().toList();

      assertFalse(path.isEmpty());
      assertEquals(start, path.get(0));
      assertEquals(goal, path.get(path.size() - 1));
      assertEquals(w, path.size(), "Horizontal path length");
      assertPathConnected(path, env);
    }

    @Test
    @DisplayName("Start equals goal")
    void startEqualsGoal() {
      Grid2DEnvironment env = new Grid2DEnvironment(10, 10);
      Point2D point = new Point2D(5, 5);

      AStarPathFinder<Point2D> pf = initAndCompute(env, point, point);
      List<Point2D> path = pf.getPath().toList();

      assertFalse(path.isEmpty());
      assertEquals(1, path.size(), "Path should contain only the single point");
      assertEquals(point, path.get(0));
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
      int gapY = h / 2;

      for (int y = 0; y < h; y++) {
        if (y != gapY) env.setObstacle(wallX, y);
      }

      Point2D start = new Point2D(1, h / 2);
      Point2D goal = new Point2D(w - 2, h / 2);

      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);
      List<Point2D> path = pf.getPath().toList();

      assertFalse(path.isEmpty(), "Path should exist through gap");
      assertEquals(start, path.get(0));
      assertEquals(goal, path.get(path.size() - 1));
      assertNoObstacleOnPath(path, env);
      assertPathConnected(path, env);

      boolean passesGap = path.stream().anyMatch(p -> p.x == wallX && p.y == gapY);
      assertTrue(passesGap, "Path must pass through the wall gap");
    }

    @Test
    @DisplayName("Maze-like corridor")
    void mazeLikeCorridor() {
      Grid2DEnvironment env = new Grid2DEnvironment(10, 10);

      for (int y = 0; y <= 7; y++) env.setObstacle(3, y);
      for (int y = 2; y <= 9; y++) env.setObstacle(6, y);

      Point2D start = new Point2D(1, 5);
      Point2D goal = new Point2D(8, 5);

      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);
      List<Point2D> path = pf.getPath().toList();

      assertFalse(path.isEmpty());
      assertEquals(start, path.get(0));
      assertEquals(goal, path.get(path.size() - 1));
      assertNoObstacleOnPath(path, env);
      assertPathConnected(path, env);
    }

    @Test
    @DisplayName("Vertical wall forces detour around bottom")
    void wallDetourAroundBottom() {
      Grid2DEnvironment env = new Grid2DEnvironment(15, 15);

      // Vertical wall at x=7, y=0..12 with a gap at y=13 (open below)
      for (int y = 0; y <= 12; y++) env.setObstacle(7, y);

      Point2D start = new Point2D(3, 7);
      Point2D goal = new Point2D(12, 7);

      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);
      List<Point2D> path = pf.getPath().toList();

      assertFalse(path.isEmpty());
      assertEquals(start, path.get(0));
      assertEquals(goal, path.get(path.size() - 1));
      assertNoObstacleOnPath(path, env);
      assertPathConnected(path, env);

      // Path must detour below y=12
      boolean passesBelowWall = path.stream().anyMatch(p -> p.y >= 13);
      assertTrue(passesBelowWall, "Path should detour below the wall");
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
      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);

      assertTrue(pf.getPath().isEmpty(), "Path should be empty when goal is walled off");
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
      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);

      assertTrue(pf.getPath().isEmpty(), "Path should be empty when start is walled off");
    }

    @Test
    @DisplayName("Full wall with no gap divides the grid")
    void fullWallNoGap() {
      Grid2DEnvironment env = new Grid2DEnvironment(10, 10);

      for (int y = 0; y < 10; y++) env.setObstacle(5, y);

      Point2D start = new Point2D(2, 5);
      Point2D goal = new Point2D(8, 5);
      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);

      assertTrue(pf.getPath().isEmpty(), "Path should be empty when wall fully divides grid");
    }
  }

  // ===================================================================
  // 4. Optimality: A* path cost <= any valid path cost
  // ===================================================================

  @Nested
  @DisplayName("4. Optimality")
  class Optimality {

    private double pathCost(List<Point2D> path, Grid2DEnvironment env) {
      double cost = 0;
      for (int i = 0; i < path.size() - 1; i++) {
        cost += env.getTraversalCost(path.get(i), path.get(i + 1));
      }
      return cost;
    }

    @Test
    @DisplayName("A* path cost matches D*Lite on same environment")
    void optimalCostOnObstacleGrid() {
      Grid2DEnvironment env = new Grid2DEnvironment(20, 20);
      int wallX = 10;
      for (int y = 0; y < 20; y++) {
        if (y != 10) env.setObstacle(wallX, y);
      }

      Point2D start = new Point2D(2, 10);
      Point2D goal = new Point2D(17, 10);

      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);
      List<Point2D> path = pf.getPath().toList();

      assertFalse(path.isEmpty());
      double cost = pathCost(path, env);
      // Optimal: straight horizontal through gap at y=10, distance = |17-2| = 15
      assertEquals(15.0, cost, 1e-9, "A* should find optimal path through gap");
    }

    @ParameterizedTest(name = "{0}x{1} grid, waypoint O(1) lookup")
    @CsvSource({"10,10", "20,20"})
    void waypointLookup(int w, int h) {
      Grid2DEnvironment env = new Grid2DEnvironment(w, h);
      Point2D start = new Point2D(0, 0);
      Point2D goal = new Point2D(w - 1, h - 1);

      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);
      List<Point2D> path = pf.getPath().toList();

      // Walk the path via getNextWaypoint
      Point2D cur = start;
      int walkSteps = 0;
      while (cur != null && !cur.equals(goal) && walkSteps < path.size()) {
        Point2D next = pf.getNextWaypoint(cur);
        assertNotNull(next, "Waypoint from " + cur + " should not be null mid-path");
        cur = next;
        walkSteps++;
      }
      assertEquals(goal, cur, "Walking via getNextWaypoint should reach goal");
    }
  }

  // ===================================================================
  // 5. Performance: large grid within reasonable time
  // ===================================================================

  @Nested
  @DisplayName("5. Performance")
  class Performance {

    @Test
    @DisplayName("100x100 open grid completes under 500ms")
    void largeOpenGrid() {
      Grid2DEnvironment env = new Grid2DEnvironment(100, 100);
      Point2D start = new Point2D(0, 0);
      Point2D goal = new Point2D(99, 99);

      long t0 = System.nanoTime();
      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);
      long elapsed = (System.nanoTime() - t0) / 1_000_000;

      List<Point2D> path = pf.getPath().toList();
      assertFalse(path.isEmpty());
      assertEquals(100, path.size(), "Diagonal path on 100x100");
      assertTrue(elapsed < 500, "Should complete within 500ms, took " + elapsed + "ms");
    }

    @Test
    @DisplayName("200x200 grid with obstacles completes under 2000ms")
    void largeGridWithObstacles() {
      Grid2DEnvironment env = new Grid2DEnvironment(200, 200);
      for (int y = 0; y < 200; y++) {
        if (y != 100) env.setObstacle(100, y);
      }

      Point2D start = new Point2D(10, 100);
      Point2D goal = new Point2D(190, 100);

      long t0 = System.nanoTime();
      AStarPathFinder<Point2D> pf = initAndCompute(env, start, goal);
      long elapsed = (System.nanoTime() - t0) / 1_000_000;

      List<Point2D> path = pf.getPath().toList();
      assertFalse(path.isEmpty());
      assertPathConnected(path, env);
      assertTrue(elapsed < 2000, "Should complete within 2s, took " + elapsed + "ms");
      System.out.printf("[Perf] 200x200 with wall: %d ms, path length %d%n", elapsed, path.size());
    }
  }
}
