package pathfinder.visualizer;

import java.util.Collection;
import java.util.List;
import pathfinder.model.Point;

/**
 * Observer callback from the algorithm layer to the UI layer. Implementations should marshal UI
 * updates to the appropriate thread (e.g. {@code Platform.runLater()} for JavaFX).
 *
 * @param <P> the spatial coordinate type
 */
public interface PathUpdateListener<P extends Point> {

  /** Invoked when a new or updated path has been computed. */
  void onPathUpdated(List<P> newPath);

  /** Invoked periodically during search to report open/closed sets. */
  void onSearchProgressUpdated(Collection<P> openList, Collection<P> closedList);

  /** Invoked when the algorithm begins a computation cycle. */
  void onAlgorithmStarted();

  /** Invoked when the algorithm finishes; {@code pathFound} indicates success. */
  void onAlgorithmFinished(boolean pathFound);
}
