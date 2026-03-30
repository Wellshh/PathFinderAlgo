package pathfinder.engine;

import pathfinder.api.IPathFinder;
import pathfinder.model.Point;
import pathfinder.visualizer.model.AlgorithmStateLayer;
import pathfinder.visualizer.model.RobotEntity;

/**
 * Bundles an algorithm instance with its dedicated visualization resources: an overlay layer for
 * search-state rendering and a robot entity for smooth movement.
 *
 * @param <P> the spatial coordinate type
 */
public class AlgorithmSlot<P extends Point> {

  private final String name;
  private final IPathFinder<P> pathFinder;
  private final AlgorithmStateLayer stateLayer;
  private final RobotEntity robot;

  private AlgorithmMetrics lastMetrics;

  public AlgorithmSlot(
      String name, IPathFinder<P> pathFinder, AlgorithmStateLayer stateLayer, RobotEntity robot) {
    this.name = name;
    this.pathFinder = pathFinder;
    this.stateLayer = stateLayer;
    this.robot = robot;
  }

  public String getName() {
    return name;
  }

  public IPathFinder<P> getPathFinder() {
    return pathFinder;
  }

  public AlgorithmStateLayer getStateLayer() {
    return stateLayer;
  }

  public RobotEntity getRobot() {
    return robot;
  }

  /** Returns the most recent performance metrics, or {@code null} if not yet measured. */
  public AlgorithmMetrics getLastMetrics() {
    return lastMetrics;
  }

  public void setLastMetrics(AlgorithmMetrics metrics) {
    this.lastMetrics = metrics;
  }
}
