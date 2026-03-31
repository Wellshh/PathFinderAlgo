/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.model;

import java.util.List;

/**
 * Represents the perception capability of an agent/robot. Scans the environment and reports edge
 * cost changes relative to the agent's prior knowledge.
 *
 * @param <P> the spatial coordinate type
 */
public interface IEnvironmentSensor<P extends Point> {

  /**
   * Scans the environment from the current position within the sensor's range. Returns a list of
   * edge cost updates that differ from previous knowledge.
   */
  List<EdgeUpdate<P>> sense(P currentPosition);
}
