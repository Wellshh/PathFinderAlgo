/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.visualizer.model;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import pathfinder.model.Point2D;

/**
 * Overlay layer storing per-algorithm search state (OPEN/CLOSED/PATH). Each algorithm instance owns
 * one layer so that multiple algorithms can visualize concurrently on the same grid.
 */
public class AlgorithmStateLayer {

  private final String algorithmId;
  private final int width;
  private final int height;
  private final AlgoCellState[][] cells;
  private final BitSet dirtyFlags;

  private int colorArgb;
  private double alpha = 1.0;

  public AlgorithmStateLayer(String algorithmId, int width, int height, int colorArgb) {
    this.algorithmId = algorithmId;
    this.width = width;
    this.height = height;
    this.colorArgb = colorArgb;
    this.cells = new AlgoCellState[height][width];
    this.dirtyFlags = new BitSet(width * height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        cells[y][x] = AlgoCellState.NONE;
      }
    }
  }

  public String getAlgorithmId() {
    return algorithmId;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getColorArgb() {
    return colorArgb;
  }

  public void setColorArgb(int colorArgb) {
    this.colorArgb = colorArgb;
  }

  public double getAlpha() {
    return alpha;
  }

  public void setAlpha(double alpha) {
    this.alpha = alpha;
  }

  public AlgoCellState getState(int x, int y) {
    return cells[y][x];
  }

  public synchronized void setState(int x, int y, AlgoCellState state) {
    if (cells[y][x] != state) {
      cells[y][x] = state;
      dirtyFlags.set(y * width + x);
    }
  }

  public synchronized BitSet consumeDirtySet() {
    BitSet snapshot = (BitSet) dirtyFlags.clone();
    dirtyFlags.clear();
    return snapshot;
  }

  public synchronized void markAllDirty() {
    dirtyFlags.set(0, width * height);
  }

  // -------------------- Bulk update helpers --------------------

  /** Clears all PATH cells back to NONE, then marks the new path. */
  public synchronized void updatePath(List<Point2D> path) {
    clearCellState(AlgoCellState.PATH);
    if (path != null) {
      for (Point2D p : path) {
        if (inBounds(p.x, p.y) && cells[p.y][p.x] == AlgoCellState.NONE) {
          cells[p.y][p.x] = AlgoCellState.PATH;
          dirtyFlags.set(p.y * width + p.x);
        }
      }
    }
  }

  /** Replaces OPEN/CLOSED cells with the new sets. */
  public synchronized void updateSearchProgress(
      Collection<Point2D> openList, Collection<Point2D> closedList) {
    clearCellState(AlgoCellState.OPEN);
    clearCellState(AlgoCellState.CLOSED);
    if (closedList != null) {
      for (Point2D p : closedList) {
        if (inBounds(p.x, p.y) && cells[p.y][p.x] == AlgoCellState.NONE) {
          cells[p.y][p.x] = AlgoCellState.CLOSED;
          dirtyFlags.set(p.y * width + p.x);
        }
      }
    }
    if (openList != null) {
      for (Point2D p : openList) {
        if (inBounds(p.x, p.y) && cells[p.y][p.x] == AlgoCellState.NONE) {
          cells[p.y][p.x] = AlgoCellState.OPEN;
          dirtyFlags.set(p.y * width + p.x);
        }
      }
    }
  }

  /** Resets every cell to NONE and marks the entire layer dirty. */
  public synchronized void clearAll() {
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (cells[y][x] != AlgoCellState.NONE) {
          cells[y][x] = AlgoCellState.NONE;
          dirtyFlags.set(y * width + x);
        }
      }
    }
  }

  // -------------------- Internal --------------------

  private void clearCellState(AlgoCellState target) {
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (cells[y][x] == target) {
          cells[y][x] = AlgoCellState.NONE;
          dirtyFlags.set(y * width + x);
        }
      }
    }
  }

  private boolean inBounds(int x, int y) {
    return x >= 0 && x < width && y >= 0 && y < height;
  }
}
