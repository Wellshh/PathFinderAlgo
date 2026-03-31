/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.algorithm.dstarlite;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pathfinder.model.EdgeUpdate;
import pathfinder.model.Point2D;
import pathfinder.testenv.environment.Grid2DEnvironment;
import pathfinder.testenv.sensor.SimpleGridSensor;

/**
 * TDD regression tests for D* Lite dynamic replan. These tests reproduce the "No path" bug observed
 * in the Multi-Algorithm Battle Demo when obstacles are added at runtime.
 *
 * <p>Strategy: each test creates a scenario where a valid path MUST exist, performs incremental D*
 * Lite replan, and asserts the path is found. Failure means the incremental mechanism is broken.
 */
@DisplayName("D* Lite Dynamic Replan Bug Reproduction")
class DStarLiteDynamicReplanBugTest {

  // ======================== Helpers ========================

  /**
   * Generates EdgeUpdates for turning cell (x,y) into an obstacle. Mirrors what SimpleGridSensor
   * produces: one update per incoming edge. Also marks the cell as obstacle in the environment
   * (simulating the sensor's knownMap sync that happens before D* Lite sees the updates).
   */
  private static List<EdgeUpdate<Point2D>> placeObstacle(Grid2DEnvironment env, int x, int y) {
    Point2D cell = new Point2D(x, y);
    List<Point2D> preds = env.getPredecessors(cell);
    List<EdgeUpdate<Point2D>> updates = new ArrayList<>();
    for (Point2D pred : preds) {
      double realCost = Double.POSITIVE_INFINITY;
      double knownCost = env.getTraversalCost(pred, cell);
      if (Double.compare(realCost, knownCost) != 0) {
        updates.add(new EdgeUpdate<>(pred, cell, Double.POSITIVE_INFINITY));
      }
    }
    env.setObstacle(x, y);
    return updates;
  }

  /** Asserts incremental D* Lite agrees with a fresh-init computation on the same env. */
  private static void assertIncrementalMatchesFresh(
      DStarLitePathFinder<Point2D> incrementalPf,
      Grid2DEnvironment env,
      Point2D currentPos,
      Point2D goal,
      String context) {
    boolean incrementalHasPath = !incrementalPf.getPath().isEmpty();
    DStarLitePathFinder<Point2D> freshPf = new DStarLitePathFinder<>();
    freshPf.initialize(env, currentPos, goal);
    freshPf.computePath();
    boolean freshHasPath = !freshPf.getPath().isEmpty();

    if (freshHasPath && !incrementalHasPath) {
      fail(
          context
              + " — fresh computation finds a path but incremental says No Path. "
              + "This proves a bug in D* Lite's incremental update mechanism.");
    }
  }

  // ======================== Test Scenarios ========================

  @Nested
  @DisplayName("1. Single replan (obstacle placed, robot at start)")
  class SingleReplanAtStart {

    @Test
    @DisplayName("One obstacle on the straight-line path")
    void singleObstacleOnPath() {
      Grid2DEnvironment env = new Grid2DEnvironment(20, 15);
      Point2D start = new Point2D(1, 7);
      Point2D goal = new Point2D(18, 7);

      DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
      pf.initialize(env, start, goal);
      pf.computePath();
      assertFalse(pf.getPath().isEmpty(), "Initial path must exist");

      List<EdgeUpdate<Point2D>> updates = placeObstacle(env, 10, 7);
      pf.setStart(start);
      pf.updateAllEdgeCosts(updates);
      pf.computePath();

      assertFalse(pf.getPath().isEmpty(), "Should route around single obstacle");
      assertIncrementalMatchesFresh(pf, env, start, goal, "single obstacle");
    }

    @Test
    @DisplayName("Short wall (3 cells) blocking the path center")
    void shortWallOnPath() {
      Grid2DEnvironment env = new Grid2DEnvironment(20, 15);
      Point2D start = new Point2D(1, 7);
      Point2D goal = new Point2D(18, 7);

      DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
      pf.initialize(env, start, goal);
      pf.computePath();

      List<EdgeUpdate<Point2D>> all = new ArrayList<>();
      for (int y = 6; y <= 8; y++) {
        all.addAll(placeObstacle(env, 10, y));
      }
      pf.setStart(start);
      pf.updateAllEdgeCosts(all);
      pf.computePath();

      assertFalse(pf.getPath().isEmpty(), "Should route around short wall");
      assertIncrementalMatchesFresh(pf, env, start, goal, "short wall");
    }
  }

  @Nested
  @DisplayName("2. Sequential one-by-one obstacle additions with replan after each")
  class SequentialAdditions {

    @Test
    @DisplayName("Build a wall cell by cell, replan after every addition")
    void wallBuiltCellByCell() {
      Grid2DEnvironment env = new Grid2DEnvironment(20, 15);
      Point2D start = new Point2D(1, 7);
      Point2D goal = new Point2D(18, 7);

      DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
      pf.initialize(env, start, goal);
      pf.computePath();

      // Build wall at x=10, y=3..11 one cell at a time (top & bottom open)
      for (int y = 3; y <= 11; y++) {
        List<EdgeUpdate<Point2D>> updates = placeObstacle(env, 10, y);
        pf.setStart(start);
        pf.updateAllEdgeCosts(updates);
        pf.computePath();

        assertFalse(pf.getPath().isEmpty(), "Path must exist (top/bottom open) after wall y=" + y);
        assertIncrementalMatchesFresh(pf, env, start, goal, "wall y=" + y);
      }
    }

    @Test
    @DisplayName("Two separate walls built alternately")
    void twoWallsAlternate() {
      Grid2DEnvironment env = new Grid2DEnvironment(30, 20);
      Point2D start = new Point2D(1, 10);
      Point2D goal = new Point2D(28, 10);

      DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
      pf.initialize(env, start, goal);
      pf.computePath();

      // Alternate between wall at x=10 and x=20, leave top open
      int[][] cells = {
        {10, 5}, {20, 5}, {10, 6}, {20, 6}, {10, 7}, {20, 7},
        {10, 8}, {20, 8}, {10, 9}, {20, 9}, {10, 10}, {20, 10},
        {10, 11}, {20, 11}, {10, 12}, {20, 12}, {10, 13}, {20, 13},
        {10, 14}, {20, 14}, {10, 15}, {20, 15},
      };

      for (int[] c : cells) {
        List<EdgeUpdate<Point2D>> updates = placeObstacle(env, c[0], c[1]);
        pf.setStart(start);
        pf.updateAllEdgeCosts(updates);
        pf.computePath();

        assertFalse(
            pf.getPath().isEmpty(),
            "Path must exist (top open) after obstacle (" + c[0] + "," + c[1] + ")");
        assertIncrementalMatchesFresh(pf, env, start, goal, "obstacle (" + c[0] + "," + c[1] + ")");
      }
    }
  }

  @Nested
  @DisplayName("3. Robot walks + discovers obstacles (full simulation loop)")
  class WalkAndDiscover {

    @Test
    @DisplayName("Hidden wall discovered mid-walk, robot detours through gap")
    void hiddenWallMidWalk() {
      Grid2DEnvironment knownMap = new Grid2DEnvironment(25, 15);
      Grid2DEnvironment groundTruth = new Grid2DEnvironment(25, 15);

      // Hidden wall at x=12, y=2..12, gap at y=3..4
      for (int y = 2; y <= 12; y++) {
        if (y == 3 || y == 4) continue;
        groundTruth.setObstacle(12, y);
      }

      Point2D start = new Point2D(2, 7);
      Point2D goal = new Point2D(22, 7);

      DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
      pf.initialize(knownMap, start, goal);
      pf.computePath();
      assertFalse(pf.getPath().isEmpty());

      SimpleGridSensor sensor = new SimpleGridSensor(groundTruth, knownMap, 3);
      Point2D robotPos = start;

      for (int step = 0; step < 100 && !robotPos.equals(goal); step++) {
        List<EdgeUpdate<Point2D>> updates = sensor.sense(robotPos);
        if (!updates.isEmpty()) {
          pf.setStart(robotPos);
          pf.updateAllEdgeCosts(updates);
          pf.computePath();

          assertFalse(
              pf.getPath().isEmpty(), "Path must exist at step " + step + " pos=" + robotPos);
          assertIncrementalMatchesFresh(
              pf, knownMap, robotPos, goal, "step " + step + " pos=" + robotPos);
        }

        Point2D next = pf.getNextWaypoint(robotPos);
        assertNotNull(next, "Must have waypoint at step " + step + " pos=" + robotPos);
        robotPos = next;
      }
      assertEquals(goal, robotPos, "Robot must reach goal");
    }

    @Test
    @DisplayName("User clicks obstacles in real-time while robot walks")
    void realtimeObstaclePlacementDuringWalk() {
      Grid2DEnvironment knownMap = new Grid2DEnvironment(30, 20);
      Grid2DEnvironment groundTruth = new Grid2DEnvironment(30, 20);

      Point2D start = new Point2D(2, 10);
      Point2D goal = new Point2D(27, 10);

      DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
      pf.initialize(knownMap, start, goal);
      pf.computePath();

      SimpleGridSensor sensor = new SimpleGridSensor(groundTruth, knownMap, 3);
      Point2D robotPos = start;

      // Walk 4 steps first
      for (int s = 0; s < 4; s++) {
        Point2D next = pf.getNextWaypoint(robotPos);
        assertNotNull(next);
        robotPos = next;
      }

      // Simulate user clicking: add wall at x=15, y=5..15 (leaving top & bottom open)
      for (int y = 5; y <= 15; y++) {
        groundTruth.setObstacle(15, y);
      }

      // Continue simulation: walk until sensing detects the wall
      for (int step = 0; step < 100 && !robotPos.equals(goal); step++) {
        List<EdgeUpdate<Point2D>> updates = sensor.sense(robotPos);
        if (!updates.isEmpty()) {
          pf.setStart(robotPos);
          pf.updateAllEdgeCosts(updates);
          pf.computePath();

          assertFalse(
              pf.getPath().isEmpty(),
              "Path must exist (top/bottom open) at step " + step + " pos=" + robotPos);
          assertIncrementalMatchesFresh(
              pf, knownMap, robotPos, goal, "step " + step + " pos=" + robotPos);
        }

        Point2D next = pf.getNextWaypoint(robotPos);
        assertNotNull(next, "Must have waypoint at step " + step + " pos=" + robotPos);
        robotPos = next;
      }
      assertEquals(goal, robotPos, "Robot must reach goal");
    }
  }

  @Nested
  @DisplayName("4. Robot walks halfway then obstacles placed directly via API")
  class WalkThenDirectObstacle {

    @Test
    @DisplayName("Robot at mid-grid, wall placed ahead, k_m > 0")
    void midGridWallWithKmAccumulated() {
      Grid2DEnvironment env = new Grid2DEnvironment(20, 15);
      Point2D start = new Point2D(1, 7);
      Point2D goal = new Point2D(18, 7);

      DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
      pf.initialize(env, start, goal);
      pf.computePath();

      // Walk robot 5 steps (accumulates k_m via setStart + updateAllEdgeCosts)
      Point2D robotPos = start;
      for (int i = 0; i < 5; i++) {
        Point2D next = pf.getNextWaypoint(robotPos);
        assertNotNull(next, "Waypoint at step " + i);
        robotPos = next;
        // Simulate a no-change sensing cycle to accumulate k_m
        pf.setStart(robotPos);
        pf.updateAllEdgeCosts(List.of());
      }

      // Place wall 4 cells ahead of robot
      int wallX = robotPos.x + 4;
      List<EdgeUpdate<Point2D>> wallUpdates = new ArrayList<>();
      for (int y = 3; y <= 11; y++) {
        wallUpdates.addAll(placeObstacle(env, wallX, y));
      }

      pf.setStart(robotPos);
      pf.updateAllEdgeCosts(wallUpdates);
      pf.computePath();

      assertFalse(
          pf.getPath().isEmpty(),
          "Path must exist from " + robotPos + " (top/bottom open, wall at x=" + wallX + ")");
      assertIncrementalMatchesFresh(pf, env, robotPos, goal, "mid-grid wall x=" + wallX);
    }

    @Test
    @DisplayName("Multiple walls placed sequentially while robot advances")
    void multipleWallsWhileAdvancing() {
      Grid2DEnvironment env = new Grid2DEnvironment(40, 20);
      Point2D start = new Point2D(1, 10);
      Point2D goal = new Point2D(38, 10);

      DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
      pf.initialize(env, start, goal);
      pf.computePath();

      Point2D robotPos = start;

      // Walk a few steps, add wall, walk more, add another wall
      for (int i = 0; i < 5; i++) {
        Point2D next = pf.getNextWaypoint(robotPos);
        assertNotNull(next);
        robotPos = next;
      }

      // First wall at x=12
      List<EdgeUpdate<Point2D>> wall1 = new ArrayList<>();
      for (int y = 4; y <= 16; y++) {
        wall1.addAll(placeObstacle(env, 12, y));
      }
      pf.setStart(robotPos);
      pf.updateAllEdgeCosts(wall1);
      pf.computePath();
      assertFalse(pf.getPath().isEmpty(), "Path must exist after first wall");
      assertIncrementalMatchesFresh(pf, env, robotPos, goal, "first wall");

      // Walk a few more steps
      for (int i = 0; i < 5; i++) {
        Point2D next = pf.getNextWaypoint(robotPos);
        if (next == null) break;
        robotPos = next;
      }

      // Second wall at x=25
      List<EdgeUpdate<Point2D>> wall2 = new ArrayList<>();
      for (int y = 4; y <= 16; y++) {
        wall2.addAll(placeObstacle(env, 25, y));
      }
      pf.setStart(robotPos);
      pf.updateAllEdgeCosts(wall2);
      pf.computePath();

      assertFalse(pf.getPath().isEmpty(), "Path must exist after second wall");
      assertIncrementalMatchesFresh(pf, env, robotPos, goal, "second wall");
    }
  }

  @Nested
  @DisplayName("5. Large grid stress — matches user's 50x35 demo config")
  class LargeGridStress {

    @Test
    @DisplayName("50x35 grid, complex wall pattern, sequential obstacle clicks")
    void largeGridSequentialClicks() {
      int W = 50, H = 35;
      Grid2DEnvironment env = new Grid2DEnvironment(W, H);
      Point2D start = new Point2D(2, H / 2);
      Point2D goal = new Point2D(W - 3, H / 2);

      DStarLitePathFinder<Point2D> pf = new DStarLitePathFinder<>();
      pf.initialize(env, start, goal);
      pf.computePath();

      Point2D robotPos = start;

      // Walk 6 steps
      for (int i = 0; i < 6; i++) {
        Point2D next = pf.getNextWaypoint(robotPos);
        assertNotNull(next);
        robotPos = next;
      }

      // Simulate user rapidly clicking to build a wall at x=25, y=5..29
      // leaving a 2-cell gap at y=8..9
      for (int y = 5; y <= 29; y++) {
        if (y == 8 || y == 9) continue;
        List<EdgeUpdate<Point2D>> updates = placeObstacle(env, 25, y);
        pf.setStart(robotPos);
        pf.updateAllEdgeCosts(updates);
        pf.computePath();

        assertFalse(pf.getPath().isEmpty(), "Path must exist (gap at y=8-9) after wall at y=" + y);
      }

      // Walk some more
      for (int i = 0; i < 3; i++) {
        Point2D next = pf.getNextWaypoint(robotPos);
        if (next == null) break;
        robotPos = next;
      }

      // Add another partial wall at x=35
      for (int y = 10; y <= 24; y++) {
        List<EdgeUpdate<Point2D>> updates = placeObstacle(env, 35, y);
        pf.setStart(robotPos);
        pf.updateAllEdgeCosts(updates);
        pf.computePath();

        assertFalse(pf.getPath().isEmpty(), "Path must exist (top/bottom open) after wall2 y=" + y);
        assertIncrementalMatchesFresh(pf, env, robotPos, goal, "wall2 y=" + y);
      }
    }
  }
}
