package pathfinder.model;

/**
 * Data Transfer Object representing a perceived change in edge traversal cost. Used by {@link
 * IEnvironmentSensor} to communicate environment changes to the planner.
 *
 * @param <P> the spatial coordinate type
 */
public class EdgeUpdate<P extends Point> {
  public final P from;
  public final P to;
  public final double newCost;

  public EdgeUpdate(P from, P to, double newCost) {
    this.from = from;
    this.to = to;
    this.newCost = newCost;
  }
}
