package pathfinder.visualizer.javafx;

import java.util.function.Consumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import pathfinder.visualizer.adapter.IInputAdapter;
import pathfinder.visualizer.adapter.NormalizedInputEvent;

/**
 * JavaFX implementation of {@link IInputAdapter}. Installs mouse handlers on a {@link Canvas} and
 * translates JavaFX {@link MouseEvent}s into {@link NormalizedInputEvent}s.
 */
public class JavaFXInputAdapter implements IInputAdapter {

  private final Canvas canvas;
  private Consumer<NormalizedInputEvent> handler;

  public JavaFXInputAdapter(Canvas canvas) {
    this.canvas = canvas;
    canvas.setOnMousePressed(this::onPressed);
    canvas.setOnMouseDragged(this::onDragged);
    canvas.setOnMouseReleased(this::onReleased);
  }

  @Override
  public void setInputHandler(Consumer<NormalizedInputEvent> handler) {
    this.handler = handler;
  }

  private void onPressed(MouseEvent e) {
    dispatch(NormalizedInputEvent.EventType.PRESSED, e);
  }

  private void onDragged(MouseEvent e) {
    dispatch(NormalizedInputEvent.EventType.DRAGGED, e);
  }

  private void onReleased(MouseEvent e) {
    dispatch(NormalizedInputEvent.EventType.RELEASED, e);
  }

  private void dispatch(NormalizedInputEvent.EventType type, MouseEvent e) {
    if (handler == null) return;
    handler.accept(new NormalizedInputEvent(type, mapButton(e.getButton()), e.getX(), e.getY()));
  }

  private static NormalizedInputEvent.Button mapButton(MouseButton btn) {
    return switch (btn) {
      case PRIMARY -> NormalizedInputEvent.Button.PRIMARY;
      case SECONDARY -> NormalizedInputEvent.Button.SECONDARY;
      case MIDDLE -> NormalizedInputEvent.Button.MIDDLE;
      default -> NormalizedInputEvent.Button.NONE;
    };
  }
}
