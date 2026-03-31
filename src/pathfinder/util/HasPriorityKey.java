/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.util;

public interface HasPriorityKey<H> {
  H getPriorityKey();

  void setPriorityKey(H key);
}
