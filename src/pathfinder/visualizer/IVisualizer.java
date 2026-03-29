package pathfinder.visualizer;

/**
 * Generic visualizer interface for rendering a {@link GridViewModel}. Implementations may target
 * JavaFX Canvas, Swing, console, or web backends.
 */
public interface IVisualizer {

  /** Bind this visualizer to a grid model and prepare rendering resources. */
  void initialize(GridViewModel model);

  /** Display the visualizer window / output. */
  void show();

  /** Release rendering resources and close the display. */
  void dispose();

  /** Register a listener that receives user interaction events (clicks, drags). */
  void addInteractionListener(GridInteractionListener listener);
}
