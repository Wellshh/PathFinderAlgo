/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.model.node;

import pathfinder.model.Point;

/**
 * Abstract base node for all algorithm-specific node types.
 *
 * @param <P> the spatial coordinate type
 */
public abstract class BaseNode<P extends Point> {
  public final P pos;

  public BaseNode(P pos) {
    this.pos = pos;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BaseNode)) return false;
    return this.pos.equals(((BaseNode<?>) o).pos);
  }

  @Override
  public int hashCode() {
    return pos.hashCode();
  }
}
