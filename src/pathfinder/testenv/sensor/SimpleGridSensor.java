package pathfinder.testenv.sensor;

import java.util.ArrayList;
import java.util.List;
import pathfinder.model.EdgeUpdate;
import pathfinder.model.IEnvironmentSensor;
import pathfinder.model.Point2D;
import pathfinder.testenv.environment.Grid2DEnvironment;

/**
 * Test-purpose sensor that compares a "ground truth" grid against the algorithm's "known" grid,
 * returning {@link EdgeUpdate}s for all edges within a configurable sensing radius whose costs
 * differ.
 */
public class SimpleGridSensor implements IEnvironmentSensor<Point2D> {

  private final Grid2DEnvironment groundTruth;
  private final Grid2DEnvironment knownMap;
  private final int senseRadius;

  /**
   * @param groundTruth the real-world map (hidden obstacles live here)
   * @param knownMap the map the algorithm currently knows about
   * @param senseRadius scanning radius around the agent's position
   */
  public SimpleGridSensor(
      Grid2DEnvironment groundTruth, Grid2DEnvironment knownMap, int senseRadius) {
    this.groundTruth = groundTruth;
    this.knownMap = knownMap;
    this.senseRadius = senseRadius;
  }

  @Override
  public List<EdgeUpdate<Point2D>> sense(Point2D currentPosition) {
    List<EdgeUpdate<Point2D>> updates = new ArrayList<>();

    int cx = currentPosition.x;
    int cy = currentPosition.y;

    for (int dy = -senseRadius; dy <= senseRadius; dy++) {
      for (int dx = -senseRadius; dx <= senseRadius; dx++) {
        int nx = cx + dx;
        int ny = cy + dy;
        if (!knownMap.inBounds(nx, ny)) {
          continue;
        }

        Point2D neighbor = new Point2D(nx, ny);

        // For every neighbor of this scanned cell, compare edge costs
        List<Point2D> preds = groundTruth.getPredecessors(neighbor);
        for (Point2D pred : preds) {
          double realCost = groundTruth.getTraversalCost(pred, neighbor);
          double knownCost = knownMap.getTraversalCost(pred, neighbor);

          if (Double.compare(realCost, knownCost) != 0) {
            // Sync the known map to match ground truth
            knownMap.setTraversalCost(pred, neighbor, realCost);
            updates.add(new EdgeUpdate<>(pred, neighbor, realCost));
          }
        }
      }
    }
    return updates;
  }
}
