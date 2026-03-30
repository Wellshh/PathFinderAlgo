package pathfinder.visualizer.adapter;

/**
 * Platform-neutral pointer event with coordinates normalized to the canvas coordinate system. This
 * decouples the engine from any specific UI toolkit's event classes.
 */
public class NormalizedInputEvent {

  public enum EventType {
    PRESSED,
    DRAGGED,
    RELEASED
  }

  public enum Button {
    PRIMARY,
    SECONDARY,
    MIDDLE,
    NONE
  }

  private final EventType type;
  private final Button button;
  private final double canvasX;
  private final double canvasY;

  public NormalizedInputEvent(EventType type, Button button, double canvasX, double canvasY) {
    this.type = type;
    this.button = button;
    this.canvasX = canvasX;
    this.canvasY = canvasY;
  }

  public EventType getType() {
    return type;
  }

  public Button getButton() {
    return button;
  }

  /** X position in canvas pixel coordinates. */
  public double getCanvasX() {
    return canvasX;
  }

  /** Y position in canvas pixel coordinates. */
  public double getCanvasY() {
    return canvasY;
  }
}
