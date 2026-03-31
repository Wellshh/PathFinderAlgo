/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Selects all algorithms to step every tick. */
public class AllParallelScheduler implements AlgorithmScheduler {

  @Override
  public List<Integer> selectForTick(int tickNumber, int totalAlgorithms) {
    if (totalAlgorithms == 0) return Collections.emptyList();
    List<Integer> all = new ArrayList<>(totalAlgorithms);
    for (int i = 0; i < totalAlgorithms; i++) {
      all.add(i);
    }
    return all;
  }
}
