/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.visualizer.javafx;

import javafx.scene.paint.Color;
import pathfinder.visualizer.CellState;

/**
 * Pre-allocated {@link Color} constants indexed by {@link CellState#ordinal()}. Eliminates
 * per-frame object allocation in the rendering loop.
 */
public final class CellColors {

  public static final Color FREE = Color.WHITE;
  public static final Color OBSTACLE = Color.rgb(40, 40, 40);
  public static final Color START = Color.rgb(0, 200, 83);
  public static final Color GOAL = Color.rgb(213, 0, 0);
  public static final Color PATH = Color.rgb(255, 214, 0);
  public static final Color OPEN_LIST = Color.rgb(129, 212, 250);
  public static final Color CLOSED_LIST = Color.rgb(179, 229, 252);
  public static final Color ROBOT = Color.rgb(156, 39, 176);

  public static final Color GRID_LINE = Color.rgb(200, 200, 200);

  private static final Color[] LOOKUP = {
    FREE, OBSTACLE, START, GOAL, PATH, OPEN_LIST, CLOSED_LIST, ROBOT
  };

  private static final int[] ARGB_LOOKUP = new int[LOOKUP.length];

  static {
    for (int i = 0; i < LOOKUP.length; i++) {
      ARGB_LOOKUP[i] = colorToArgb(LOOKUP[i]);
    }
  }

  private CellColors() {}

  /** O(1) lookup — no allocation, no branching. */
  public static Color forState(CellState state) {
    return LOOKUP[state.ordinal()];
  }

  /**
   * O(1) packed-ARGB lookup for use with {@link pathfinder.visualizer.adapter.IGraphicsAdapter}.
   */
  public static int argbForState(CellState state) {
    return ARGB_LOOKUP[state.ordinal()];
  }

  private static int colorToArgb(Color c) {
    int a = (int) (c.getOpacity() * 255);
    int r = (int) (c.getRed() * 255);
    int g = (int) (c.getGreen() * 255);
    int b = (int) (c.getBlue() * 255);
    return (a << 24) | (r << 16) | (g << 8) | b;
  }
}
