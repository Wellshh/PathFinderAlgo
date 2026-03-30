package pathfinder.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import pathfinder.model.Environment;
import pathfinder.model.Point;

/**
 * Reusable, UI-agnostic engine that executes N algorithms on the same environment and collects
 * performance metrics for each. Designed for algorithm-vs-algorithm comparison ("battle").
 *
 * <p>Usage:
 *
 * <pre>{@code
 * BattleEngine<Point2D> engine = new BattleEngine<>();
 * List<AlgorithmMetrics> results = engine.runBattle(env, start, goal, slots);
 * }</pre>
 *
 * @param <P> the spatial coordinate type
 */
public class BattleEngine<P extends Point> {

  /**
   * Initializes every slot's pathfinder with the given environment, start, and goal, then executes
   * {@code computePath()} on each while measuring wall-clock time. Results are stored in each
   * slot's {@link AlgorithmSlot#setLastMetrics(AlgorithmMetrics)} and also returned as a list.
   *
   * <p>This method does NOT publish paths to visualization layers (separation of concerns). The
   * caller is responsible for reading {@code slot.getPathFinder().getPath()} and forwarding to the
   * UI layer.
   *
   * @param env the shared environment topology
   * @param start the common start position
   * @param goal the common goal position
   * @param slots algorithm slots to execute (supports any N >= 1)
   * @return unmodifiable list of metrics in the same order as {@code slots}
   */
  public List<AlgorithmMetrics> runBattle(
      Environment<P> env, P start, P goal, List<AlgorithmSlot<P>> slots) {

    List<AlgorithmMetrics> results = new ArrayList<>(slots.size());

    for (AlgorithmSlot<P> slot : slots) {
      slot.getPathFinder().initialize(env, start, goal);

      long t0 = System.nanoTime();
      slot.getPathFinder().computePath();
      long elapsed = System.nanoTime() - t0;

      int pathLen = slot.getPathFinder().getPath().size();
      AlgorithmMetrics metrics = new AlgorithmMetrics(slot.getName(), elapsed, pathLen);
      slot.setLastMetrics(metrics);

      results.add(metrics);
    }

    return Collections.unmodifiableList(results);
  }
}
