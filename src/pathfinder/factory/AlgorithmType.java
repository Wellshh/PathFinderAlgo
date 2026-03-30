package pathfinder.factory;

/**
 * Defines the available pathfinding algorithms in the system. Used for dynamic instantiation and UI
 * display.
 */
public enum AlgorithmType {
  ASTAR("A* Algorithm"),
  DSTAR_LITE("D* Lite Algorithm");

  private final String displayName;

  AlgorithmType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
