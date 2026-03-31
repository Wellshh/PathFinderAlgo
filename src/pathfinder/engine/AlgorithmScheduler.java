/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.engine;

import java.util.List;

/**
 * Strategy interface that decides which algorithms advance during a given logic tick. Enables
 * round-robin, all-parallel, or custom scheduling policies.
 */
public interface AlgorithmScheduler {

  /**
   * Select which algorithm slots should step during this tick.
   *
   * @param tickNumber the current logic tick counter (0-based, monotonically increasing)
   * @param totalAlgorithms the total number of registered algorithm slots
   * @return indices of the slots that should execute this tick
   */
  List<Integer> selectForTick(int tickNumber, int totalAlgorithms);
}
