/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.engine;

/**
 * Immutable snapshot of algorithm performance data collected after a single execution or replan.
 *
 * @param algorithmName human-readable name of the algorithm
 * @param computeTimeNanos wall-clock time spent in the initial full {@code computePath()}, nanos
 * @param replanTimeNanos wall-clock time of the most recent incremental replan (0 if none yet)
 * @param pathLength number of waypoints in the computed path (0 if no path found)
 */
public record AlgorithmMetrics(
    String algorithmName, long computeTimeNanos, long replanTimeNanos, int pathLength) {

  /** Convenience accessor returning initial compute time in milliseconds. */
  public double computeTimeMs() {
    return computeTimeNanos / 1_000_000.0;
  }

  /** Convenience accessor returning replan time in milliseconds. */
  public double replanTimeMs() {
    return replanTimeNanos / 1_000_000.0;
  }
}
