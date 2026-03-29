package pathfinder.visualizer;

/**
 * Observer callback from the UI layer to the algorithm layer. Fired by mouse/keyboard interaction
 * on the visualizer canvas. Implementations typically dispatch work to a background thread.
 */
public interface GridInteractionListener {

  /**
   * A cell's obstacle state was toggled by the user.
   *
   * @param x grid column
   * @param y grid row
   * @param isNowObstacle true if the cell was set to obstacle, false if cleared
   */
  void onCellToggled(int x, int y, boolean isNowObstacle);

  /** The user repositioned the start marker. */
  void onStartMoved(int x, int y);

  /** The user repositioned the goal marker. */
  void onGoalMoved(int x, int y);

  /** The user explicitly requested a replan (e.g. pressed a key). */
  void onReplanRequested();
}
