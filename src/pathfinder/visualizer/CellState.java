package pathfinder.visualizer;

/**
 * Visual rendering state for each grid cell. Ordinal order is used for color lookup — do not
 * reorder without updating {@link pathfinder.visualizer.javafx.CellColors}.
 */
public enum CellState {
  FREE,
  OBSTACLE,
  START,
  GOAL,
  PATH,
  OPEN_LIST,
  CLOSED_LIST,
  ROBOT
}
