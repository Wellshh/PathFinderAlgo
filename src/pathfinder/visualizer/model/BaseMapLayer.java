package pathfinder.visualizer.model;

import java.util.BitSet;

/**
 * Static environment layer storing obstacle/free/start/goal state per cell. Owns its own dirty
 * {@link BitSet} for incremental rendering.
 */
public class BaseMapLayer {

  private final int width;
  private final int height;
  private final MapCellState[][] cells;
  private final BitSet dirtyFlags;

  public BaseMapLayer(int width, int height) {
    this.width = width;
    this.height = height;
    this.cells = new MapCellState[height][width];
    this.dirtyFlags = new BitSet(width * height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        cells[y][x] = MapCellState.FREE;
      }
    }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public MapCellState getState(int x, int y) {
    return cells[y][x];
  }

  public synchronized void setState(int x, int y, MapCellState state) {
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

  public boolean inBounds(int x, int y) {
    return x >= 0 && x < width && y >= 0 && y < height;
  }
}
