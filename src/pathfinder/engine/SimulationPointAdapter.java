/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.engine;

import java.util.ArrayList;
import java.util.List;
import pathfinder.model.Point;
import pathfinder.model.Point2D;
import pathfinder.visualizer.model.RobotEntity;

/**
 * Bridges pathfinder coordinate type {@code P} and the 2D grid visualization model ({@link
 * Point2D}, {@link RobotEntity} pixel positions). Inject an implementation when {@link
 * SimulationRunner} uses a point type other than {@link Point2D}.
 */
public interface SimulationPointAdapter<P extends Point> {

  /** Builds {@code P} from the robot's current display position (typically grid-rounded). */
  P fromRobot(RobotEntity robot);

  /** Maps a path waypoint to grid cells for overlays and movement targets. */
  Point2D toPoint2D(P point);

  /** Converts a full path for {@link pathfinder.visualizer.model.AlgorithmStateLayer}. */
  default List<Point2D> pathToDisplayPoints(List<P> path) {
    if (path == null || path.isEmpty()) {
      return List.of();
    }
    ArrayList<Point2D> out = new ArrayList<>(path.size());
    for (P p : path) {
      out.add(toPoint2D(p));
    }
    return out;
  }

  /** Adapter for the standard integer 2D grid used by the JavaFX demos. */
  static SimulationPointAdapter<Point2D> forPoint2D() {
    return new SimulationPointAdapter<>() {
      @Override
      public Point2D fromRobot(RobotEntity robot) {
        return new Point2D((int) robot.getCurrentX(), (int) robot.getCurrentY());
      }

      @Override
      public Point2D toPoint2D(Point2D point) {
        return point;
      }
    };
  }
}
