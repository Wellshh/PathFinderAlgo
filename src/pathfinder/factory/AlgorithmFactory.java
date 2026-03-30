package pathfinder.factory;

import pathfinder.algorithm.astar.AStarPathFinder;
import pathfinder.algorithm.dstarlite.DStarLitePathFinder;
import pathfinder.api.IPathFinder;
import pathfinder.model.Point2D;

/**
 * Factory class responsible for instantiating pathfinders based on the requested AlgorithmType.
 * Ensures that the engine and UI layers remain decoupled from specific algorithm implementations.
 */
public class AlgorithmFactory {

  /**
   * Creates a new instance of the specified pathfinder algorithm.
   *
   * @param type the type of algorithm to instantiate
   * @return a new IPathFinder instance
   * @throws IllegalArgumentException if the algorithm type is unknown
   */
  public static IPathFinder<Point2D> createPathFinder(AlgorithmType type) {
    switch (type) {
      case ASTAR:
        return new AStarPathFinder<>();
      case DSTAR_LITE:
        return new DStarLitePathFinder<>();
      default:
        throw new IllegalArgumentException("Unknown algorithm type: " + type);
    }
  }
}
