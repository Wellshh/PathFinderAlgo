/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.visualizer.adapter;

/**
 * Platform-agnostic abstraction over 2D drawing primitives. Implementations wrap a concrete
 * graphics backend (JavaFX Canvas, Swing Graphics2D, HTML5 Canvas, etc.).
 *
 * <p>Color values are packed ARGB integers: {@code 0xAARRGGBB}.
 */
public interface IGraphicsAdapter {

  /** Prepare the backend for a new frame (e.g. save state). */
  void beginFrame();

  /** Finalize the current frame (e.g. restore state, flush buffer). */
  void endFrame();

  void fillRect(double x, double y, double w, double h, int argb);

  void strokeRect(double x, double y, double w, double h, int argb, double lineWidth);

  void fillOval(double cx, double cy, double rx, double ry, int argb);

  void drawText(String text, double x, double y, int argb, double fontSize);

  /**
   * Sets the global opacity for subsequent draw calls. Value range: 0.0 (transparent) to 1.0
   * (opaque). Used for alpha-blending algorithm layers.
   */
  void setGlobalAlpha(double alpha);

  int getCanvasWidth();

  int getCanvasHeight();
}
