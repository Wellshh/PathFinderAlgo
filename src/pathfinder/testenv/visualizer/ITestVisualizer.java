package pathfinder.testenv.visualizer;

import java.util.List;
import pathfinder.model.Environment;
import pathfinder.model.Point;

/**
 * Strategy interface for rendering the environment state during tests. Implementations can target
 * console output, Swing, JavaFX, etc.
 *
 * @param <P> the spatial coordinate type
 */
public interface ITestVisualizer<P extends Point> {

  /**
   * Renders the current environment state.
   *
   * @param env the environment topology
   * @param start the start position
   * @param goal the goal position
   * @param path the current path (may be null if no path has been computed yet)
   * @param title a descriptive title for this rendering frame
   */
  void renderEnvironment(Environment<P> env, P start, P goal, List<P> path, String title);

  /** Renders the environment with the robot's current position highlighted. */
  void renderWithRobot(Environment<P> env, P start, P goal, List<P> path, P robotPos, String title);
}
