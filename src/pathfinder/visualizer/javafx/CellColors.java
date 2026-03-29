package pathfinder.visualizer.javafx;

import javafx.scene.paint.Color;
import pathfinder.visualizer.CellState;

/**
 * Pre-allocated {@link Color} constants indexed by {@link CellState#ordinal()}.
 * Eliminates per-frame object allocation in the rendering loop.
 */
public final class CellColors {

    public static final Color FREE        = Color.WHITE;
    public static final Color OBSTACLE    = Color.rgb(40, 40, 40);
    public static final Color START       = Color.rgb(0, 200, 83);
    public static final Color GOAL        = Color.rgb(213, 0, 0);
    public static final Color PATH        = Color.rgb(255, 214, 0);
    public static final Color OPEN_LIST   = Color.rgb(129, 212, 250);
    public static final Color CLOSED_LIST = Color.rgb(179, 229, 252);
    public static final Color ROBOT       = Color.rgb(156, 39, 176);

    public static final Color GRID_LINE   = Color.rgb(200, 200, 200);

    private static final Color[] LOOKUP = {
        FREE, OBSTACLE, START, GOAL, PATH, OPEN_LIST, CLOSED_LIST, ROBOT
    };

    private CellColors() {}

    /** O(1) lookup — no allocation, no branching. */
    public static Color forState(CellState state) {
        return LOOKUP[state.ordinal()];
    }
}
