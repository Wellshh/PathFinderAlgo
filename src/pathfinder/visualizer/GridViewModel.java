package pathfinder.visualizer;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import pathfinder.model.Point2D;

/**
 * Observable grid model that serves as the single source of truth for Canvas rendering. Tracks
 * per-cell dirty flags so the renderer can perform incremental redraws instead of full-screen
 * clears.
 *
 * <p>Thread-safety: mutation methods are {@code synchronized} because the algorithm worker thread
 * writes state while the JavaFX Application Thread reads it via {@link #consumeDirtySet()}.
 */
public class GridViewModel {

  private final int width;
  private final int height;
  private final CellState[][] cells;
  private final BitSet dirtyFlags;

  public GridViewModel(int width, int height) {
    this.width = width;
    this.height = height;
    this.cells = new CellState[height][width];
    this.dirtyFlags = new BitSet(width * height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        cells[y][x] = CellState.FREE;
      }
    }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  /**
   * Returns the state without synchronization — safe when called from the FX thread after {@link
   * #consumeDirtySet()} has been obtained.
   */
  public CellState getCellState(int x, int y) {
    return cells[y][x];
  }

  public synchronized void setCellState(int x, int y, CellState state) {
    if (cells[y][x] != state) {
      cells[y][x] = state;
      dirtyFlags.set(y * width + x);
    }
  }

  /**
   * Atomically returns all dirty indices and clears the internal set. The caller iterates the
   * returned BitSet to repaint only changed cells.
   */
  public synchronized BitSet consumeDirtySet() {
    BitSet snapshot = (BitSet) dirtyFlags.clone();
    dirtyFlags.clear();
    return snapshot;
  }

  /** Marks every cell as dirty so the next render frame repaints the full grid. */
  public synchronized void markAllDirty() {
    dirtyFlags.set(0, width * height);
  }

  // -------------------- Bulk update helpers --------------------

  /**
   * Clears all PATH cells back to FREE, then marks the new path. Only changed cells become dirty —
   * avoids redundant repaints.
   */
  public synchronized void updatePath(List<Point2D> path) {
    clearState(CellState.PATH);
    if (path != null) {
      for (Point2D p : path) {
        if (inBounds(p.x, p.y) && cells[p.y][p.x] == CellState.FREE) {
          cells[p.y][p.x] = CellState.PATH;
          dirtyFlags.set(p.y * width + p.x);
        }
      }
    }
  }

  /** Replaces OPEN_LIST / CLOSED_LIST cells with the new sets. */
  public synchronized void updateSearchProgress(
      Collection<Point2D> openList, Collection<Point2D> closedList) {
    clearState(CellState.OPEN_LIST);
    clearState(CellState.CLOSED_LIST);
    if (closedList != null) {
      for (Point2D p : closedList) {
        if (inBounds(p.x, p.y) && cells[p.y][p.x] == CellState.FREE) {
          cells[p.y][p.x] = CellState.CLOSED_LIST;
          dirtyFlags.set(p.y * width + p.x);
        }
      }
    }
    if (openList != null) {
      for (Point2D p : openList) {
        if (inBounds(p.x, p.y) && cells[p.y][p.x] == CellState.FREE) {
          cells[p.y][p.x] = CellState.OPEN_LIST;
          dirtyFlags.set(p.y * width + p.x);
        }
      }
    }
  }

  public synchronized void setRobotPosition(Point2D pos) {
    clearState(CellState.ROBOT);
    if (pos != null && inBounds(pos.x, pos.y)) {
      cells[pos.y][pos.x] = CellState.ROBOT;
      dirtyFlags.set(pos.y * width + pos.x);
    }
  }

  // -------------------- Internal helpers --------------------

  private void clearState(CellState target) {
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (cells[y][x] == target) {
          cells[y][x] = CellState.FREE;
          dirtyFlags.set(y * width + x);
        }
      }
    }
  }

  private boolean inBounds(int x, int y) {
    return x >= 0 && x < width && y >= 0 && y < height;
  }
}
