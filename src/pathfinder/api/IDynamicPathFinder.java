/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.api;

import java.util.List;
import pathfinder.model.EdgeUpdate;
import pathfinder.model.Point;

/**
 * Interface for path planning algorithms that support dynamic environment changes (e.g., D* Lite,
 * LPA*). Extends {@link IPathFinder} with incremental update capabilities.
 *
 * @param <P> the spatial coordinate type
 */
public interface IDynamicPathFinder<P extends Point> extends IPathFinder<P> {

  /** Advances the algorithm's start position to the robot's current location. */
  void setStart(P newStart);

  /** Update all edge costs, call {@link notifyEdgeCostChange} internally. */
  void updateAllEdgeCosts(List<EdgeUpdate<P>> edgeUpdates);

  /**
   * Notifies the algorithm that the traversal cost of a directed edge has changed. The algorithm
   * should update its internal state incrementally.
   */
  void notifyEdgeCostChange(P from, P to, double newCost);

  /** Dynamically updates the goal node without fully re-initializing the algorithm. */
  void updateGoal(P newGoal);
}
