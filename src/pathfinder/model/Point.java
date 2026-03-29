package pathfinder.model;

/**
 * Abstract base class for all spatial coordinate types. Subclasses must properly implement equals()
 * and hashCode() to ensure correct behavior in HashMaps and PriorityQueues.
 */
public abstract class Point {
  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();
}
