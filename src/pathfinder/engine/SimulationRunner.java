/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import pathfinder.api.IDynamicPathFinder;
import pathfinder.api.IPathFinder;
import pathfinder.model.EdgeUpdate;
import pathfinder.model.Environment;
import pathfinder.model.IEnvironmentSensor;
import pathfinder.model.Point;
import pathfinder.model.Point2D;

/**
 * UI-agnostic simulation engine that drives the sense-replan-move loop for each registered
 * algorithm slot. Designed to be called from {@link SimulationController}'s logic tick.
 *
 * <p>Each slot has its own known map and sensor. On every tick where the robot is idle (reached its
 * waypoint), the runner: senses the environment, replans if changes were detected, advances the
 * robot to the next waypoint, and publishes updated path data.
 *
 * @param <P> the spatial coordinate type
 */
public class SimulationRunner<P extends Point> {

  private final P goal;

  private final SimulationPointAdapter<P> pointAdapter;

  private final Map<AlgorithmSlot<P>, SlotContext<P>> contexts = new HashMap<>();

  private Consumer<AlgorithmSlot<P>> onMetricsUpdated;
  private BiConsumer<AlgorithmSlot<P>, List<EdgeUpdate<P>>> onEnvironmentDiscovered;

  public SimulationRunner(P goal, SimulationPointAdapter<P> pointAdapter) {
    this.goal = goal;
    this.pointAdapter = pointAdapter;
  }

  /** Register a slot with its dedicated known map and sensor. */
  public void registerSlot(
      AlgorithmSlot<P> slot, Environment<P> knownMap, IEnvironmentSensor<P> sensor) {
    P startPos = pointAdapter.fromRobot(slot.getRobot());
    contexts.put(slot, new SlotContext<>(knownMap, sensor, startPos));
  }

  public void unregisterAll() {
    contexts.clear();
  }

  /** Called when metrics are updated after a replan. */
  public void setOnMetricsUpdated(Consumer<AlgorithmSlot<P>> listener) {
    this.onMetricsUpdated = listener;
  }

  /** Called when the sensor discovers new environment changes (for visual sync). */
  public void setOnEnvironmentDiscovered(
      BiConsumer<AlgorithmSlot<P>, List<EdgeUpdate<P>>> listener) {
    this.onEnvironmentDiscovered = listener;
  }

  /** Returns true if the slot's robot has reached the goal. */
  public boolean isFinished(AlgorithmSlot<P> slot) {
    SlotContext<P> ctx = contexts.get(slot);
    return ctx != null && goal.equals(ctx.currentPos);
  }

  /**
   * Advance one simulation step for the given slot. Should be called once per logic tick.
   *
   * <p>Only acts when the robot is idle (animation complete). Flow: sense -> replan if needed ->
   * get next waypoint -> move robot.
   */
  public void stepSlot(AlgorithmSlot<P> slot) {
    SlotContext<P> ctx = contexts.get(slot);
    if (ctx == null || isFinished(slot)) return;
    if (!slot.getRobot().isIdle()) return;

    P currentPos = ctx.currentPos;

    // 1. Sense environment from current position
    List<EdgeUpdate<P>> updates = ctx.sensor.sense(currentPos);

    // 2. Replan if changes detected
    if (!updates.isEmpty()) {
      notifyDiscovery(slot, updates);
      replan(slot, ctx, currentPos, updates);
    }

    // 3. Get next waypoint and move
    P next = slot.getPathFinder().getNextWaypoint(currentPos);
    if (next == null) {
      return;
    }

    ctx.currentPos = next;
    Point2D nextCell = pointAdapter.toPoint2D(next);
    slot.getRobot().setTarget(nextCell.x, nextCell.y);

    // 4. Publish updated path visualization
    List<Point2D> pathPoints =
        pointAdapter.pathToDisplayPoints(slot.getPathFinder().getPath().toList());
    slot.getStateLayer().updatePath(pathPoints);
  }

  // -------------------- Internal --------------------

  private void replan(
      AlgorithmSlot<P> slot, SlotContext<P> ctx, P currentPos, List<EdgeUpdate<P>> updates) {

    IPathFinder<P> pf = slot.getPathFinder();
    long t0 = System.nanoTime();

    if (pf instanceof IDynamicPathFinder<P> dpf) {
      dpf.setStart(currentPos);
      dpf.updateAllEdgeCosts(updates);
      dpf.computePath();
    } else {
      pf.initialize(ctx.knownMap, currentPos, goal);
      pf.computePath();
    }

    long replanNanos = System.nanoTime() - t0;

    AlgorithmMetrics prev = slot.getLastMetrics();
    long initialTime = prev != null ? prev.computeTimeNanos() : 0;
    int pathLen = pf.getPath().size();
    AlgorithmMetrics updated =
        new AlgorithmMetrics(slot.getName(), initialTime, replanNanos, pathLen);
    slot.setLastMetrics(updated);

    if (onMetricsUpdated != null) {
      onMetricsUpdated.accept(slot);
    }
  }

  private void notifyDiscovery(AlgorithmSlot<P> slot, List<EdgeUpdate<P>> updates) {
    if (onEnvironmentDiscovered != null) {
      onEnvironmentDiscovered.accept(slot, updates);
    }
  }

  /** Per-slot mutable state held by the runner. */
  private static class SlotContext<P extends Point> {
    final Environment<P> knownMap;
    final IEnvironmentSensor<P> sensor;
    P currentPos;

    SlotContext(Environment<P> knownMap, IEnvironmentSensor<P> sensor, P startPos) {
      this.knownMap = knownMap;
      this.sensor = sensor;
      this.currentPos = startPos;
    }
  }
}
