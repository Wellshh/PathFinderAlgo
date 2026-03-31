/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.engine;

import java.util.function.Consumer;

/**
 * Reactive configuration interface that decouples UI controls from the simulation engine. UI
 * widgets (sliders, buttons) modify properties through this interface; the engine observes changes
 * via listeners.
 */
public interface ISimulationConfig {

  // -------------------- Playback speed --------------------

  double getPlaybackSpeed();

  void setPlaybackSpeed(double speed);

  void addPlaybackSpeedListener(Consumer<Double> listener);

  // -------------------- Simulation state --------------------

  SimulationState getSimulationState();

  void requestStart();

  void requestPause();

  void requestResume();

  void requestStop();

  void requestReset();

  void addStateChangeListener(Consumer<SimulationState> listener);

  // -------------------- Logic tick rate --------------------

  double getLogicTicksPerSecond();

  void setLogicTicksPerSecond(double tps);

  void addTickRateListener(Consumer<Double> listener);

  // -------------------- Visual toggles --------------------

  boolean isShowOpenList();

  void setShowOpenList(boolean show);

  void addShowOpenListListener(Consumer<Boolean> listener);

  boolean isShowClosedList();

  void setShowClosedList(boolean show);

  void addShowClosedListListener(Consumer<Boolean> listener);

  // -------------------- Battle mode --------------------

  boolean isBattleModeEnabled();

  void setBattleModeEnabled(boolean enabled);

  void addBattleModeListener(Consumer<Boolean> listener);
}
