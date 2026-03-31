/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.engine;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Central simulation clock that drives a dual-tick architecture: <b>logic ticks</b> advance
 * algorithm state at a configurable rate, while <b>render ticks</b> fire at the display's refresh
 * rate (driven externally by the platform visualizer).
 *
 * <p>This class is completely UI-framework-agnostic. The platform visualizer is responsible for
 * calling {@link #onRenderTick(double)} at its frame rate (e.g. from a JavaFX AnimationTimer).
 *
 * <p>Algorithm slots are stepped via the pluggable {@link AlgorithmScheduler} strategy.
 */
public class SimulationController {

  private SimulationState state = SimulationState.IDLE;
  private double playbackSpeed = 1.0;
  private double logicTicksPerSecond = 10.0;
  private double logicTickAccumulator = 0.0;
  private int tickCounter = 0;

  private AlgorithmScheduler scheduler = new AllParallelScheduler();
  private final List<Runnable> algorithmStepActions = new CopyOnWriteArrayList<>();
  private final List<Consumer<Double>> renderTickListeners = new CopyOnWriteArrayList<>();
  private final List<Consumer<SimulationState>> stateChangeListeners = new CopyOnWriteArrayList<>();

  // -------------------- State machine --------------------

  public SimulationState getState() {
    return state;
  }

  public void start() {
    if (state == SimulationState.IDLE || state == SimulationState.PAUSED) {
      setState(SimulationState.RUNNING);
    }
  }

  public void pause() {
    if (state == SimulationState.RUNNING) {
      setState(SimulationState.PAUSED);
    }
  }

  public void resume() {
    if (state == SimulationState.PAUSED) {
      setState(SimulationState.RUNNING);
    }
  }

  public void stop() {
    setState(SimulationState.FINISHED);
  }

  public void reset() {
    tickCounter = 0;
    logicTickAccumulator = 0.0;
    setState(SimulationState.IDLE);
  }

  private void setState(SimulationState newState) {
    if (this.state == newState) return;
    this.state = newState;
    for (Consumer<SimulationState> listener : stateChangeListeners) {
      listener.accept(newState);
    }
  }

  // -------------------- Configuration --------------------

  public double getPlaybackSpeed() {
    return playbackSpeed;
  }

  public void setPlaybackSpeed(double speed) {
    this.playbackSpeed = Math.max(0.0, speed);
  }

  public double getLogicTicksPerSecond() {
    return logicTicksPerSecond;
  }

  public void setLogicTicksPerSecond(double tps) {
    this.logicTicksPerSecond = Math.max(0.1, tps);
  }

  public void setScheduler(AlgorithmScheduler scheduler) {
    this.scheduler = scheduler;
  }

  public int getTickCounter() {
    return tickCounter;
  }

  // -------------------- Algorithm slot registration --------------------

  /**
   * Register a step action for one algorithm slot. The action is invoked on the thread that calls
   * {@link #onRenderTick(double)} (typically the UI thread), so implementations should be fast or
   * dispatch to a worker.
   */
  public void addAlgorithmStepAction(Runnable stepAction) {
    algorithmStepActions.add(stepAction);
  }

  public void clearAlgorithmStepActions() {
    algorithmStepActions.clear();
  }

  // -------------------- Listeners --------------------

  /** Register a callback invoked every render tick with the delta time in seconds. */
  public void addRenderTickListener(Consumer<Double> listener) {
    renderTickListeners.add(listener);
  }

  public void addStateChangeListener(Consumer<SimulationState> listener) {
    stateChangeListeners.add(listener);
  }

  // -------------------- Tick pump --------------------

  /**
   * Called by the platform visualizer on every display frame. Advances the logic clock by the
   * effective delta time (scaled by playback speed) and fires accumulated logic ticks.
   *
   * @param deltaTimeSeconds wall-clock time since the last render frame, in seconds
   */
  public void onRenderTick(double deltaTimeSeconds) {
    if (state == SimulationState.RUNNING) {
      double effectiveDelta = deltaTimeSeconds * playbackSpeed;
      logicTickAccumulator += effectiveDelta;

      double tickInterval = 1.0 / logicTicksPerSecond;
      while (logicTickAccumulator >= tickInterval) {
        logicTickAccumulator -= tickInterval;
        fireLogicTick();
      }
    }

    for (Consumer<Double> listener : renderTickListeners) {
      listener.accept(deltaTimeSeconds);
    }
  }

  private void fireLogicTick() {
    int totalAlgos = algorithmStepActions.size();
    List<Integer> selected = scheduler.selectForTick(tickCounter, totalAlgos);
    for (int idx : selected) {
      if (idx >= 0 && idx < totalAlgos) {
        algorithmStepActions.get(idx).run();
      }
    }
    tickCounter++;
  }
}
