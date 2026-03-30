package pathfinder.visualizer;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import pathfinder.visualizer.adapter.IGraphicsAdapter;
import pathfinder.visualizer.adapter.IInputAdapter;
import pathfinder.visualizer.adapter.NormalizedInputEvent;
import pathfinder.visualizer.model.AlgoCellState;
import pathfinder.visualizer.model.AlgorithmStateLayer;
import pathfinder.visualizer.model.BaseMapLayer;
import pathfinder.visualizer.model.MapCellState;
import pathfinder.visualizer.model.RobotEntity;

/**
 * Framework-agnostic grid visualizer that implements the multi-layer rendering pipeline. Subclasses
 * provide platform-specific adapters and drive the render loop.
 *
 * <h3>Render pipeline (executed per frame):</h3>
 *
 * <ol>
 *   <li>Update robot interpolation positions
 *   <li>Consume dirty sets from base map layer
 *   <li>Paint dirty base cells
 *   <li>For each algorithm layer (Painter's Algorithm, back to front): consume dirty set, apply
 *       alpha, paint non-NONE cells
 *   <li>Paint robot entities at sub-cell float coordinates
 *   <li>Draw grid lines over dirty cells
 * </ol>
 */
public abstract class AbstractGridVisualizer implements IVisualizer {

  // -------------------- Default colors (packed ARGB) --------------------

  protected static final int COLOR_FREE = 0xFFFFFFFF;
  protected static final int COLOR_OBSTACLE = 0xFF282828;
  protected static final int COLOR_START = 0xFF00C853;
  protected static final int COLOR_GOAL = 0xFFD50000;
  protected static final int COLOR_PATH = 0xFFFFD600;
  protected static final int COLOR_OPEN = 0xFF81D4FA;
  protected static final int COLOR_CLOSED = 0xFFB3E5FC;
  protected static final int COLOR_GRID_LINE = 0xFFC8C8C8;

  private static final double DEFAULT_CELL_SIZE = 20.0;
  private static final double GRID_LINE_WIDTH = 0.5;

  private GridViewModel model;
  private double cellSize = DEFAULT_CELL_SIZE;

  private final List<GridInteractionListener> listeners = new CopyOnWriteArrayList<>();

  private int lastDragCellX = -1;
  private int lastDragCellY = -1;

  // -------------------- Abstract hooks --------------------

  /** Return the platform-specific graphics adapter. */
  protected abstract IGraphicsAdapter getGraphics();

  /** Return the platform-specific input adapter. */
  protected abstract IInputAdapter getInput();

  /** Platform-specific: start the render loop. */
  protected abstract void startRenderLoop();

  /** Platform-specific: stop the render loop. */
  protected abstract void stopRenderLoop();

  // -------------------- IVisualizer contract --------------------

  @Override
  public void initialize(GridViewModel model) {
    this.model = model;
    getInput().setInputHandler(this::handleInput);
    model.markAllDirty();
  }

  @Override
  public void show() {
    startRenderLoop();
  }

  @Override
  public void dispose() {
    stopRenderLoop();
  }

  @Override
  public void addInteractionListener(GridInteractionListener listener) {
    listeners.add(listener);
  }

  // -------------------- Accessors --------------------

  public GridViewModel getModel() {
    return model;
  }

  public void setCellSize(double size) {
    this.cellSize = size;
    if (model != null) {
      model.markAllDirty();
    }
  }

  public double getCellSize() {
    return cellSize;
  }

  // -------------------- Core render pipeline --------------------

  /**
   * Execute one full render frame. Called by the platform render loop with the wall-clock delta.
   * Can also be invoked externally (e.g. from a {@link pathfinder.engine.SimulationController}
   * render-tick listener).
   *
   * @param deltaTimeSeconds seconds since the last frame
   */
  public void renderFrame(double deltaTimeSeconds) {
    if (model == null) return;
    IGraphicsAdapter gfx = getGraphics();

    // 1. Update robot interpolation
    for (RobotEntity robot : model.getRobots()) {
      robot.updateInterpolation(deltaTimeSeconds);
    }

    // 2. Check if there's anything to paint (union of all dirty sets)
    boolean hasLayeredContent = !model.getAlgoLayers().isEmpty() || !model.getRobots().isEmpty();

    if (hasLayeredContent) {
      renderLayered(gfx, deltaTimeSeconds);
    } else {
      renderLegacy(gfx);
    }
  }

  /** New multi-layer rendering path: base layer + algorithm overlays + robot entities. */
  private void renderLayered(IGraphicsAdapter gfx, double deltaTimeSeconds) {
    BaseMapLayer base = model.getBaseLayer();
    int w = model.getWidth();

    BitSet baseDirty = base.consumeDirtySet();
    BitSet unionDirty = (BitSet) baseDirty.clone();

    for (AlgorithmStateLayer algoLayer : model.getAlgoLayers()) {
      BitSet algoDirty = algoLayer.consumeDirtySet();
      unionDirty.or(algoDirty);
    }

    if (unionDirty.isEmpty() && allRobotsIdle()) return;

    gfx.beginFrame();
    gfx.setGlobalAlpha(1.0);

    // Base map layer
    for (int idx = unionDirty.nextSetBit(0); idx >= 0; idx = unionDirty.nextSetBit(idx + 1)) {
      int cx = idx % w;
      int cy = idx / w;
      double px = cx * cellSize;
      double py = cy * cellSize;

      MapCellState mapState = base.getState(cx, cy);
      gfx.fillRect(px, py, cellSize, cellSize, mapColorFor(mapState));
    }

    // Algorithm overlay layers (Painter's Algorithm)
    for (AlgorithmStateLayer algoLayer : model.getAlgoLayers()) {
      gfx.setGlobalAlpha(algoLayer.getAlpha());
      for (int idx = unionDirty.nextSetBit(0); idx >= 0; idx = unionDirty.nextSetBit(idx + 1)) {
        int cx = idx % w;
        int cy = idx / w;
        AlgoCellState algoState = algoLayer.getState(cx, cy);
        if (algoState == AlgoCellState.NONE) continue;

        double px = cx * cellSize;
        double py = cy * cellSize;
        gfx.fillRect(px, py, cellSize, cellSize, algoColorFor(algoState, algoLayer));
      }
    }

    // Grid lines
    gfx.setGlobalAlpha(1.0);
    for (int idx = unionDirty.nextSetBit(0); idx >= 0; idx = unionDirty.nextSetBit(idx + 1)) {
      int cx = idx % w;
      int cy = idx / w;
      double px = cx * cellSize;
      double py = cy * cellSize;
      gfx.strokeRect(px, py, cellSize, cellSize, COLOR_GRID_LINE, GRID_LINE_WIDTH);
    }

    // Robot entities at sub-cell float coords
    for (RobotEntity robot : model.getRobots()) {
      double rx = robot.getCurrentX() * cellSize + cellSize / 2.0;
      double ry = robot.getCurrentY() * cellSize + cellSize / 2.0;
      double radius = cellSize * 0.35;
      gfx.fillOval(rx, ry, radius, radius, robot.getColorArgb());
    }

    gfx.endFrame();
  }

  /**
   * Legacy single-layer rendering path using the deprecated flat CellState model. Used when no
   * algorithm layers or robots have been registered.
   */
  @SuppressWarnings("deprecation")
  private void renderLegacy(IGraphicsAdapter gfx) {
    BitSet dirty = model.consumeDirtySet();
    if (dirty.isEmpty()) return;

    gfx.beginFrame();
    gfx.setGlobalAlpha(1.0);
    int w = model.getWidth();

    for (int idx = dirty.nextSetBit(0); idx >= 0; idx = dirty.nextSetBit(idx + 1)) {
      int cx = idx % w;
      int cy = idx / w;
      double px = cx * cellSize;
      double py = cy * cellSize;

      CellState state = model.getCellState(cx, cy);
      gfx.fillRect(px, py, cellSize, cellSize, legacyColorFor(state));
      gfx.strokeRect(px, py, cellSize, cellSize, COLOR_GRID_LINE, GRID_LINE_WIDTH);
    }

    gfx.endFrame();
  }

  // -------------------- Color mapping --------------------

  protected int mapColorFor(MapCellState state) {
    return switch (state) {
      case FREE -> COLOR_FREE;
      case OBSTACLE -> COLOR_OBSTACLE;
      case START -> COLOR_START;
      case GOAL -> COLOR_GOAL;
    };
  }

  protected int algoColorFor(AlgoCellState state, AlgorithmStateLayer layer) {
    return switch (state) {
      case NONE -> 0x00000000;
      case OPEN -> COLOR_OPEN;
      case CLOSED -> COLOR_CLOSED;
      case PATH -> layer.getColorArgb();
    };
  }

  protected int legacyColorFor(CellState state) {
    return switch (state) {
      case FREE -> COLOR_FREE;
      case OBSTACLE -> COLOR_OBSTACLE;
      case START -> COLOR_START;
      case GOAL -> COLOR_GOAL;
      case PATH -> COLOR_PATH;
      case OPEN_LIST -> COLOR_OPEN;
      case CLOSED_LIST -> COLOR_CLOSED;
      case ROBOT -> 0xFF9C27B0;
    };
  }

  // -------------------- Input handling --------------------

  private void handleInput(NormalizedInputEvent e) {
    if (e.getButton() != NormalizedInputEvent.Button.PRIMARY) return;

    switch (e.getType()) {
      case PRESSED -> {
        int cx = pixelToCellX(e.getCanvasX());
        int cy = pixelToCellY(e.getCanvasY());
        if (!inBounds(cx, cy)) return;
        lastDragCellX = cx;
        lastDragCellY = cy;
        toggleCell(cx, cy);
      }
      case DRAGGED -> {
        int cx = pixelToCellX(e.getCanvasX());
        int cy = pixelToCellY(e.getCanvasY());
        if (!inBounds(cx, cy)) return;
        if (cx == lastDragCellX && cy == lastDragCellY) return;
        lastDragCellX = cx;
        lastDragCellY = cy;
        toggleCell(cx, cy);
      }
      case RELEASED -> {
        lastDragCellX = -1;
        lastDragCellY = -1;
      }
    }
  }

  @SuppressWarnings("deprecation")
  private void toggleCell(int cx, int cy) {
    CellState current = model.getCellState(cx, cy);
    boolean isNowObstacle;

    if (current == CellState.OBSTACLE) {
      model.setCellState(cx, cy, CellState.FREE);
      isNowObstacle = false;
    } else if (current == CellState.FREE
        || current == CellState.PATH
        || current == CellState.OPEN_LIST
        || current == CellState.CLOSED_LIST) {
      model.setCellState(cx, cy, CellState.OBSTACLE);
      isNowObstacle = true;
    } else {
      return;
    }

    for (GridInteractionListener l : listeners) {
      l.onCellToggled(cx, cy, isNowObstacle);
    }
  }

  // -------------------- Coordinate utilities --------------------

  private int pixelToCellX(double px) {
    return (int) (px / cellSize);
  }

  private int pixelToCellY(double py) {
    return (int) (py / cellSize);
  }

  private boolean inBounds(int cx, int cy) {
    return model != null && cx >= 0 && cx < model.getWidth() && cy >= 0 && cy < model.getHeight();
  }

  private boolean allRobotsIdle() {
    for (RobotEntity r : model.getRobots()) {
      if (!r.isIdle()) return false;
    }
    return true;
  }
}
