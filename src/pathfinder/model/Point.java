/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.model;

/**
 * Abstract base class for all spatial coordinate types. Subclasses must properly implement equals()
 * and hashCode() to ensure correct behavior in HashMaps and PriorityQueues.
 */
public abstract class Point {
  /** Get the dimension of the point */
  public abstract int getDim();

  /* Get one dimension difference between two points */
  public abstract double getDimDiff(int dimIndex, Point other) throws IllegalArgumentException;

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();
}
