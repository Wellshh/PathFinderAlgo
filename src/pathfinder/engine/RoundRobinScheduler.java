/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.engine;

import java.util.Collections;
import java.util.List;

/** Selects one algorithm per tick in round-robin order. */
public class RoundRobinScheduler implements AlgorithmScheduler {

  @Override
  public List<Integer> selectForTick(int tickNumber, int totalAlgorithms) {
    if (totalAlgorithms == 0) return Collections.emptyList();
    return List.of(tickNumber % totalAlgorithms);
  }
}
