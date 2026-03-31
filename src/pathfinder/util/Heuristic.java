/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.util;

import pathfinder.model.Point;

@FunctionalInterface
public interface Heuristic<T extends Point> {
  double compute(T a, T b);
}
