/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.testenv.visualizer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pathfinder.model.Environment;
import pathfinder.model.Point2D;
import pathfinder.testenv.environment.Grid2DEnvironment;

/**
 * Console-based grid visualizer. Renders a 2D grid as text with symbols: . = free cell, # =
 * obstacle, S = start, G = goal, * = path, @ = robot position
 */
public class ConsoleGridVisualizer implements ITestVisualizer<Point2D> {

  private final int width;
  private final int height;

  public ConsoleGridVisualizer(int width, int height) {
    this.width = width;
    this.height = height;
  }

  @Override
  public void renderEnvironment(
      Environment<Point2D> env, Point2D start, Point2D goal, List<Point2D> path, String title) {
    renderWithRobot(env, start, goal, path, null, title);
  }

  @Override
  public void renderWithRobot(
      Environment<Point2D> env,
      Point2D start,
      Point2D goal,
      List<Point2D> path,
      Point2D robotPos,
      String title) {

    Grid2DEnvironment grid = (Grid2DEnvironment) env;

    Set<Point2D> pathSet = new HashSet<>();
    if (path != null) {
      pathSet.addAll(path);
    }

    StringBuilder sb = new StringBuilder();
    sb.append("\n=== ").append(title).append(" ===\n");

    // Column header
    sb.append("   ");
    for (int x = 0; x < width; x++) {
      sb.append(String.format("%2d", x));
    }
    sb.append('\n');

    for (int y = 0; y < height; y++) {
      sb.append(String.format("%2d ", y));
      for (int x = 0; x < width; x++) {
        char ch;
        Point2D p = new Point2D(x, y);

        if (robotPos != null && p.equals(robotPos)) {
          ch = '@';
        } else if (p.equals(start)) {
          ch = 'S';
        } else if (p.equals(goal)) {
          ch = 'G';
        } else if (grid.isObstacle(x, y)) {
          ch = '#';
        } else if (pathSet.contains(p)) {
          ch = '*';
        } else {
          ch = '.';
        }
        sb.append(' ').append(ch);
      }
      sb.append('\n');
    }

    System.out.println(sb);
  }
}
