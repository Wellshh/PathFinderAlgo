package pathfinder.visualizer;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import pathfinder.model.Point2D;
import pathfinder.visualizer.model.AlgorithmStateLayer;
import pathfinder.visualizer.model.BaseMapLayer;
import pathfinder.visualizer.model.MapCellState;
import pathfinder.visualizer.model.RobotEntity;

/**
 * Observable grid model backed by a multi-layer architecture. Serves as the single source of truth
 * for all rendering.
 *
 * <p>Internally delegates to {@link BaseMapLayer} (obstacles, start, goal) and zero or more {@link
 * AlgorithmStateLayer}s (one per algorithm). A backward-compatible facade using legacy {@link
 * CellState} is retained so existing callers compile unchanged.
 *
 * <p>Thread-safety: mutation methods are {@code synchronized}.
 */
public class GridViewModel {

  private final int width;
  private final int height;

  private final BaseMapLayer baseLayer;
  private final List<AlgorithmStateLayer> algoLayers = new CopyOnWriteArrayList<>();
  private final List<RobotEntity> robots = new CopyOnWriteArrayList<>();

  // Legacy flat view kept in sync for backward compatibility
  private final CellState[][] legacyCells;
  private final BitSet legacyDirtyFlags;

  public GridViewModel(int width, int height) {
    this.width = width;
    this.height = height;
    this.baseLayer = new BaseMapLayer(width, height);
    this.legacyCells = new CellState[height][width];
    this.legacyDirtyFlags = new BitSet(width * height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        legacyCells[y][x] = CellState.FREE;
      }
    }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  // -------------------- Layer access (new API) --------------------

  public BaseMapLayer getBaseLayer() {
    return baseLayer;
  }

  public List<AlgorithmStateLayer> getAlgoLayers() {
    return algoLayers;
  }

  public AlgorithmStateLayer addAlgorithmLayer(String algorithmId, int colorArgb) {
    AlgorithmStateLayer layer = new AlgorithmStateLayer(algorithmId, width, height, colorArgb);
    algoLayers.add(layer);
    return layer;
  }

  public AlgorithmStateLayer getAlgorithmLayer(String algorithmId) {
    for (AlgorithmStateLayer layer : algoLayers) {
      if (layer.getAlgorithmId().equals(algorithmId)) {
        return layer;
      }
    }
    return null;
  }

  // -------------------- Robot entities --------------------

  public List<RobotEntity> getRobots() {
    return robots;
  }

  public RobotEntity addRobot(String id, int colorArgb, double startX, double startY) {
    RobotEntity robot = new RobotEntity(id, colorArgb, startX, startY);
    robots.add(robot);
    return robot;
  }

  public RobotEntity getRobot(String id) {
    for (RobotEntity r : robots) {
      if (r.getId().equals(id)) return r;
    }
    return null;
  }

  // -------------------- Legacy facade (backward compat) --------------------

  /**
   * @deprecated Use {@link BaseMapLayer} and {@link AlgorithmStateLayer} directly.
   */
  @Deprecated
  public CellState getCellState(int x, int y) {
    return legacyCells[y][x];
  }

  /**
   * @deprecated Use layer-specific setters instead.
   */
  @Deprecated
  public synchronized void setCellState(int x, int y, CellState state) {
    if (legacyCells[y][x] != state) {
      legacyCells[y][x] = state;
      legacyDirtyFlags.set(y * width + x);

      syncToBaseLayer(x, y, state);
    }
  }

  /**
   * @deprecated The layered model has per-layer dirty sets. Use layer-specific consumeDirtySet().
   */
  @Deprecated
  public synchronized BitSet consumeDirtySet() {
    BitSet snapshot = (BitSet) legacyDirtyFlags.clone();
    legacyDirtyFlags.clear();
    return snapshot;
  }

  public synchronized void markAllDirty() {
    legacyDirtyFlags.set(0, width * height);
    baseLayer.markAllDirty();
    for (AlgorithmStateLayer layer : algoLayers) {
      layer.markAllDirty();
    }
  }

  // -------------------- Legacy bulk update helpers --------------------

  /**
   * @deprecated Use {@link AlgorithmStateLayer#updatePath(List)} instead.
   */
  @Deprecated
  public synchronized void updatePath(List<Point2D> path) {
    clearLegacyState(CellState.PATH);
    if (path != null) {
      for (Point2D p : path) {
        if (inBounds(p.x, p.y) && legacyCells[p.y][p.x] == CellState.FREE) {
          legacyCells[p.y][p.x] = CellState.PATH;
          legacyDirtyFlags.set(p.y * width + p.x);
        }
      }
    }
  }

  /**
   * @deprecated Use {@link AlgorithmStateLayer#updateSearchProgress(Collection, Collection)}.
   */
  @Deprecated
  public synchronized void updateSearchProgress(
      Collection<Point2D> openList, Collection<Point2D> closedList) {
    clearLegacyState(CellState.OPEN_LIST);
    clearLegacyState(CellState.CLOSED_LIST);
    if (closedList != null) {
      for (Point2D p : closedList) {
        if (inBounds(p.x, p.y) && legacyCells[p.y][p.x] == CellState.FREE) {
          legacyCells[p.y][p.x] = CellState.CLOSED_LIST;
          legacyDirtyFlags.set(p.y * width + p.x);
        }
      }
    }
    if (openList != null) {
      for (Point2D p : openList) {
        if (inBounds(p.x, p.y) && legacyCells[p.y][p.x] == CellState.FREE) {
          legacyCells[p.y][p.x] = CellState.OPEN_LIST;
          legacyDirtyFlags.set(p.y * width + p.x);
        }
      }
    }
  }

  /**
   * @deprecated Use {@link pathfinder.visualizer.model.RobotEntity} instead.
   */
  @Deprecated
  public synchronized void setRobotPosition(Point2D pos) {
    clearLegacyState(CellState.ROBOT);
    if (pos != null && inBounds(pos.x, pos.y)) {
      legacyCells[pos.y][pos.x] = CellState.ROBOT;
      legacyDirtyFlags.set(pos.y * width + pos.x);
    }
  }

  // -------------------- Internal helpers --------------------

  private void syncToBaseLayer(int x, int y, CellState state) {
    switch (state) {
      case FREE -> baseLayer.setState(x, y, MapCellState.FREE);
      case OBSTACLE -> baseLayer.setState(x, y, MapCellState.OBSTACLE);
      case START -> baseLayer.setState(x, y, MapCellState.START);
      case GOAL -> baseLayer.setState(x, y, MapCellState.GOAL);
      default -> {
        // Algorithm-specific states (PATH, OPEN_LIST, CLOSED_LIST, ROBOT)
        // are overlays and do not affect the base layer.
      }
    }
  }

  private void clearLegacyState(CellState target) {
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (legacyCells[y][x] == target) {
          legacyCells[y][x] = CellState.FREE;
          legacyDirtyFlags.set(y * width + x);
        }
      }
    }
  }

  private boolean inBounds(int x, int y) {
    return x >= 0 && x < width && y >= 0 && y < height;
  }
}
