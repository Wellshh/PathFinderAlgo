/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.visualizer.adapter;

import java.util.function.Consumer;

/**
 * Platform-agnostic input abstraction. Implementations translate toolkit-specific pointer events
 * into {@link NormalizedInputEvent}s and forward them to registered consumers.
 */
public interface IInputAdapter {

  /** Register a handler that receives all pointer events from the canvas surface. */
  void setInputHandler(Consumer<NormalizedInputEvent> handler);
}
