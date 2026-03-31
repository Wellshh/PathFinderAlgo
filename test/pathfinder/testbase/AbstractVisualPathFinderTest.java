/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.testbase;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pathfinder.api.IPathFinder;
import pathfinder.model.Environment;
import pathfinder.model.Point;
import pathfinder.testenv.visualizer.ITestVisualizer;

/**
 * Template Method base class for visual path-finder tests. Provides a standard lifecycle (init,
 * compute, visualize) with {@code System.nanoTime()} timing at each phase.
 *
 * <p>Subclasses override {@link #createPathFinder()} and set up {@code env / start / goal /
 * visualizer} before calling {@link #initAndComputePath()}.
 *
 * @param <P> the spatial coordinate type
 */
public abstract class AbstractVisualPathFinderTest<P extends Point> {

  protected Environment<P> env;
  protected IPathFinder<P> pathFinder;
  protected ITestVisualizer<P> visualizer;
  protected P start;
  protected P goal;
  protected Map<String, Long> timings;

  /** Factory method -- each algorithm subclass instantiates its own planner. */
  protected abstract IPathFinder<P> createPathFinder();

  /**
   * Runs phases 1-2 of the test lifecycle: 1) Create & initialize the algorithm (timed) 2) Compute
   * the initial path (timed + visualized) Returns the computed path for further assertions or
   * visual walking.
   */
  protected List<P> initAndComputePath() {
    timings = new LinkedHashMap<>();
    long currentTime;

    // Phase 1: Create & initialize
    currentTime = System.nanoTime();
    pathFinder = createPathFinder();
    pathFinder.initialize(env, start, goal);
    timings.put("Initialize", System.nanoTime() - currentTime);

    visualizer.renderEnvironment(env, start, goal, null, "Initial Environment");

    // Phase 2: Compute path
    currentTime = System.nanoTime();
    pathFinder.computePath();
    timings.put("ComputePath", System.nanoTime() - currentTime);

    List<P> path = pathFinder.getPath().toList();
    visualizer.renderEnvironment(env, start, goal, path, "Computed Path");
    return path;
  }

  protected void recordTiming(String label, long nanos) {
    timings.put(label, nanos);
  }

  protected void printTimingSummary(String testName) {
    System.out.println("\n--- Timing Summary [" + testName + "] ---");
    long total = 0;
    for (Map.Entry<String, Long> e : timings.entrySet()) {
      double ms = e.getValue() / 1_000_000.0;
      System.out.printf("  %-30s %10.3f ms%n", e.getKey(), ms);
      total += e.getValue();
    }
    System.out.printf("  %-30s %10.3f ms%n", "TOTAL", total / 1_000_000.0);
    System.out.println("----------------------------------------------\n");
  }
}
