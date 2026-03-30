package pathfinder.visualizer.javafx;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import pathfinder.visualizer.adapter.IGraphicsAdapter;

/**
 * JavaFX implementation of {@link IGraphicsAdapter}. Wraps a {@link Canvas}'s {@link
 * GraphicsContext} and translates packed-ARGB colors into JavaFX {@link Color} instances.
 */
public class JavaFXGraphicsAdapter implements IGraphicsAdapter {

  private final Canvas canvas;
  private final GraphicsContext gc;

  public JavaFXGraphicsAdapter(Canvas canvas) {
    this.canvas = canvas;
    this.gc = canvas.getGraphicsContext2D();
  }

  public Canvas getCanvas() {
    return canvas;
  }

  @Override
  public void beginFrame() {
    gc.save();
  }

  @Override
  public void endFrame() {
    gc.restore();
  }

  @Override
  public void fillRect(double x, double y, double w, double h, int argb) {
    gc.setFill(toColor(argb));
    gc.fillRect(x, y, w, h);
  }

  @Override
  public void strokeRect(double x, double y, double w, double h, int argb, double lineWidth) {
    gc.setStroke(toColor(argb));
    gc.setLineWidth(lineWidth);
    gc.strokeRect(x, y, w, h);
  }

  @Override
  public void fillOval(double cx, double cy, double rx, double ry, int argb) {
    gc.setFill(toColor(argb));
    gc.fillOval(cx - rx, cy - ry, rx * 2, ry * 2);
  }

  @Override
  public void drawText(String text, double x, double y, int argb, double fontSize) {
    gc.setFill(toColor(argb));
    gc.setFont(Font.font(fontSize));
    gc.fillText(text, x, y);
  }

  @Override
  public void setGlobalAlpha(double alpha) {
    gc.setGlobalAlpha(alpha);
  }

  @Override
  public int getCanvasWidth() {
    return (int) canvas.getWidth();
  }

  @Override
  public int getCanvasHeight() {
    return (int) canvas.getHeight();
  }

  private static Color toColor(int argb) {
    int a = (argb >>> 24) & 0xFF;
    int r = (argb >>> 16) & 0xFF;
    int g = (argb >>> 8) & 0xFF;
    int b = argb & 0xFF;
    return Color.rgb(r, g, b, a / 255.0);
  }
}
