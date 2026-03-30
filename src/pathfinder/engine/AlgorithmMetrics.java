package pathfinder.engine;

/**
 * Immutable snapshot of algorithm performance data collected after a single execution. Used by
 * {@link BattleEngine} to report results and by the UI layer to display comparisons.
 *
 * @param algorithmName human-readable name of the algorithm
 * @param computeTimeNanos wall-clock time spent in {@code computePath()}, in nanoseconds
 * @param pathLength number of waypoints in the computed path (0 if no path found)
 */
public record AlgorithmMetrics(String algorithmName, long computeTimeNanos, int pathLength) {

  /** Convenience accessor returning compute time in milliseconds. */
  public double computeTimeMs() {
    return computeTimeNanos / 1_000_000.0;
  }
}
