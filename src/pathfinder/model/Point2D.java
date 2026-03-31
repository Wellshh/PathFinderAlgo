/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.model;

import java.util.Objects;

/** Concrete 2D integer coordinate */
public class Point2D extends Point {
  public final int x;
  public final int y;

  public Point2D(int x, int y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public int getDim() {
    return 2;
  }

  @Override
  public double getDimDiff(int dimIndex, Point other) throws IllegalArgumentException {
    if (dimIndex < 0 || dimIndex >= getDim()) {
      throw new IllegalArgumentException("Invalid dimension index: " + dimIndex);
    }
    Point2D o = (Point2D) other;
    if (dimIndex == 0) {
      return x - o.x;
    } else if (dimIndex == 1) {
      return y - o.y;
    } else {
      return 0;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Point2D)) return false;
    Point2D other = (Point2D) o;
    return x == other.x && y == other.y;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }

  @Override
  public String toString() {
    return "(" + x + "," + y + ")";
  }
}
