package pathfinder.visualizer.javafx;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import pathfinder.visualizer.AbstractGridVisualizer;
import pathfinder.visualizer.GridViewModel;
import pathfinder.visualizer.adapter.IGraphicsAdapter;
import pathfinder.visualizer.adapter.IInputAdapter;

/**
 * JavaFX-specific thin subclass of {@link AbstractGridVisualizer}. Provides a hardware-accelerated
 * {@link Canvas} backend and drives the render loop via {@link AnimationTimer}.
 *
 * <p><b>Threading contract</b>: Must be constructed and used on the JavaFX Application Thread.
 */
public class JavaFXGridVisualizer extends AbstractGridVisualizer {

  private Canvas canvas;
  private JavaFXGraphicsAdapter graphicsAdapter;
  private JavaFXInputAdapter inputAdapter;
  private AnimationTimer renderLoop;

  private long lastFrameNanos = 0;

  @Override
  public void initialize(GridViewModel model) {
    this.canvas = new Canvas(model.getWidth() * getCellSize(), model.getHeight() * getCellSize());
    this.graphicsAdapter = new JavaFXGraphicsAdapter(canvas);
    this.inputAdapter = new JavaFXInputAdapter(canvas);

    createRenderLoop();
    super.initialize(model);
  }

  @Override
  protected IGraphicsAdapter getGraphics() {
    return graphicsAdapter;
  }

  @Override
  protected IInputAdapter getInput() {
    return inputAdapter;
  }

  @Override
  protected void startRenderLoop() {
    lastFrameNanos = 0;
    renderLoop.start();
  }

  @Override
  protected void stopRenderLoop() {
    if (renderLoop != null) {
      renderLoop.stop();
    }
  }

  @Override
  public void setCellSize(double size) {
    super.setCellSize(size);
    if (canvas != null && getModel() != null) {
      canvas.setWidth(getModel().getWidth() * size);
      canvas.setHeight(getModel().getHeight() * size);
    }
  }

  // -------------------- JavaFX-specific accessors --------------------

  public Canvas getCanvas() {
    return canvas;
  }

  public Pane asPane() {
    Pane pane = new Pane(canvas);
    pane.setPrefSize(canvas.getWidth(), canvas.getHeight());
    return pane;
  }

  // -------------------- Render loop --------------------

  private void createRenderLoop() {
    renderLoop =
        new AnimationTimer() {
          @Override
          public void handle(long nowNanos) {
            double dt = 0.0;
            if (lastFrameNanos > 0) {
              dt = (nowNanos - lastFrameNanos) / 1_000_000_000.0;
            }
            lastFrameNanos = nowNanos;
            renderFrame(dt);
          }
        };
  }
}
