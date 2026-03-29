package pathfinder.visualizer.javafx;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import pathfinder.visualizer.CellState;
import pathfinder.visualizer.GridInteractionListener;
import pathfinder.visualizer.GridViewModel;
import pathfinder.visualizer.IVisualizer;

/**
 * High-performance JavaFX visualizer that renders a {@link GridViewModel}
 * onto a single hardware-accelerated {@link Canvas} node.
 *
 * <p><b>Rendering strategy</b>: An {@link AnimationTimer} fires every
 * vsync frame (~60 fps). Each tick it consumes the model's dirty set
 * and repaints only the changed cells — no full-screen {@code clearRect}.
 *
 * <p><b>Interaction</b>: Mouse press/drag toggles obstacles and notifies
 * registered {@link GridInteractionListener}s so the algorithm layer
 * can replan on a background thread.
 *
 * <p><b>Threading contract</b>: This class must be constructed and used
 * on the JavaFX Application Thread. Model mutations from worker threads
 * are safe because {@link GridViewModel} is internally synchronized.
 */
public class JavaFXGridVisualizer implements IVisualizer {

    private static final double DEFAULT_CELL_SIZE = 20.0;
    private static final double GRID_LINE_WIDTH = 0.5;

    private GridViewModel model;
    private Canvas canvas;
    private AnimationTimer renderLoop;
    private double cellSize = DEFAULT_CELL_SIZE;

    private final List<GridInteractionListener> listeners = new CopyOnWriteArrayList<>();

    // Drag state: avoid re-toggling the same cell within one gesture
    private int lastDragCellX = -1;
    private int lastDragCellY = -1;

    // -------------------- IVisualizer contract --------------------

    @Override
    public void initialize(GridViewModel model) {
        this.model = model;
        this.canvas = new Canvas(
                model.getWidth() * cellSize,
                model.getHeight() * cellSize);

        installMouseHandlers();
        createRenderLoop();

        model.markAllDirty();
    }

    @Override
    public void show() {
        renderLoop.start();
    }

    @Override
    public void dispose() {
        if (renderLoop != null) {
            renderLoop.stop();
        }
    }

    @Override
    public void addInteractionListener(GridInteractionListener listener) {
        listeners.add(listener);
    }

    // -------------------- Public accessors --------------------

    /** Returns the Canvas node to be embedded in a Scene graph. */
    public Canvas getCanvas() {
        return canvas;
    }

    /** Returns a Pane wrapping the Canvas, convenient for Scene construction. */
    public Pane asPane() {
        Pane pane = new Pane(canvas);
        pane.setPrefSize(canvas.getWidth(), canvas.getHeight());
        return pane;
    }

    public void setCellSize(double size) {
        this.cellSize = size;
        if (model != null) {
            canvas.setWidth(model.getWidth() * cellSize);
            canvas.setHeight(model.getHeight() * cellSize);
            model.markAllDirty();
        }
    }

    public double getCellSize() {
        return cellSize;
    }

    // -------------------- Dirty-region rendering engine --------------------

    private void createRenderLoop() {
        renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                renderDirtyRegions();
            }
        };
    }

    private void renderDirtyRegions() {
        BitSet dirty = model.consumeDirtySet();
        if (dirty.isEmpty()) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        int width = model.getWidth();

        for (int idx = dirty.nextSetBit(0); idx >= 0; idx = dirty.nextSetBit(idx + 1)) {
            int cx = idx % width;
            int cy = idx / width;
            double px = cx * cellSize;
            double py = cy * cellSize;

            CellState state = model.getCellState(cx, cy);
            gc.setFill(CellColors.forState(state));
            gc.fillRect(px, py, cellSize, cellSize);

            gc.setStroke(CellColors.GRID_LINE);
            gc.setLineWidth(GRID_LINE_WIDTH);
            gc.strokeRect(px, py, cellSize, cellSize);
        }
    }

    // -------------------- Mouse interaction --------------------

    private void installMouseHandlers() {
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
    }

    private void handleMousePressed(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) return;

        int cx = pixelToCellX(e.getX());
        int cy = pixelToCellY(e.getY());
        if (!inBounds(cx, cy)) return;

        lastDragCellX = cx;
        lastDragCellY = cy;
        toggleCell(cx, cy);
    }

    private void handleMouseDragged(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) return;

        int cx = pixelToCellX(e.getX());
        int cy = pixelToCellY(e.getY());
        if (!inBounds(cx, cy)) return;
        if (cx == lastDragCellX && cy == lastDragCellY) return;

        lastDragCellX = cx;
        lastDragCellY = cy;
        toggleCell(cx, cy);
    }

    private void handleMouseReleased(MouseEvent e) {
        lastDragCellX = -1;
        lastDragCellY = -1;
    }

    private void toggleCell(int cx, int cy) {
        CellState current = model.getCellState(cx, cy);
        boolean isNowObstacle;

        if (current == CellState.OBSTACLE) {
            model.setCellState(cx, cy, CellState.FREE);
            isNowObstacle = false;
        } else if (current == CellState.FREE || current == CellState.PATH
                || current == CellState.OPEN_LIST || current == CellState.CLOSED_LIST) {
            model.setCellState(cx, cy, CellState.OBSTACLE);
            isNowObstacle = true;
        } else {
            // START, GOAL, ROBOT — do not toggle
            return;
        }

        for (GridInteractionListener l : listeners) {
            l.onCellToggled(cx, cy, isNowObstacle);
        }
    }

    // -------------------- Coordinate conversion --------------------

    private int pixelToCellX(double px) {
        return (int) (px / cellSize);
    }

    private int pixelToCellY(double py) {
        return (int) (py / cellSize);
    }

    private boolean inBounds(int cx, int cy) {
        return cx >= 0 && cx < model.getWidth()
            && cy >= 0 && cy < model.getHeight();
    }
}
