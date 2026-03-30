package pathfinder.visualizer.javafx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import pathfinder.algorithm.dstarlite.DStarLitePathFinder;
import pathfinder.engine.AlgorithmSlot;
import pathfinder.engine.AllParallelScheduler;
import pathfinder.engine.SimulationController;
import pathfinder.model.EdgeUpdate;
import pathfinder.model.Point2D;
import pathfinder.testenv.environment.Grid2DEnvironment;
import pathfinder.visualizer.CellState;
import pathfinder.visualizer.GridInteractionListener;
import pathfinder.visualizer.GridViewModel;
import pathfinder.visualizer.model.AlgorithmStateLayer;
import pathfinder.visualizer.model.RobotEntity;

/**
 * Demonstrates two D* Lite instances racing on the same grid with different start positions. Each
 * algorithm has its own overlay layer and robot entity, composited via alpha blending.
 */
public class MultiAlgoDemo extends Application {

  private static final int GRID_W = 50;
  private static final int GRID_H = 35;
  private static final double CELL_PX = 18.0;

  private static final int COLOR_ALGO_A = 0xFFE65100; // deep orange
  private static final int COLOR_ALGO_B = 0xFF1565C0; // blue

  private final Grid2DEnvironment env = new Grid2DEnvironment(GRID_W, GRID_H);
  private final GridViewModel viewModel = new GridViewModel(GRID_W, GRID_H);

  private final DStarLitePathFinder<Point2D> pfA = new DStarLitePathFinder<>();
  private final DStarLitePathFinder<Point2D> pfB = new DStarLitePathFinder<>();

  private final Point2D startA = new Point2D(2, 5);
  private final Point2D startB = new Point2D(2, GRID_H - 6);
  private final Point2D goal = new Point2D(GRID_W - 3, GRID_H / 2);

  private AlgorithmSlot<Point2D> slotA;
  private AlgorithmSlot<Point2D> slotB;

  private final SimulationController simController = new SimulationController();
  private final Label statusBar = new Label("Multi-Algo Battle — click to place obstacles");

  private final ExecutorService algorithmExecutor =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "algo-worker");
            t.setDaemon(true);
            return t;
          });

  @Override
  public void start(Stage primaryStage) {
    initAlgorithms();
    initLayers();
    syncModelFromEnv();

    JavaFXGridVisualizer visualizer = new JavaFXGridVisualizer();
    visualizer.setCellSize(CELL_PX);
    visualizer.initialize(viewModel);
    visualizer.addInteractionListener(createInteractionListener());

    simController.setScheduler(new AllParallelScheduler());
    simController.setLogicTicksPerSecond(5.0);

    simController.addRenderTickListener(dt -> visualizer.renderFrame(dt));

    BorderPane root = new BorderPane();
    root.setCenter(visualizer.asPane());
    root.setBottom(statusBar);

    Scene scene = new Scene(root);
    primaryStage.setTitle("Multi-Algorithm Battle Demo");
    primaryStage.setScene(scene);
    primaryStage.setResizable(false);
    primaryStage.show();

    visualizer.show();
  }

  @Override
  public void stop() {
    algorithmExecutor.shutdownNow();
  }

  // -------------------- Initialization --------------------

  private void initAlgorithms() {
    placeSampleWall();
    pfA.initialize(env, startA, goal);
    pfA.computePath();
    pfB.initialize(env, startB, goal);
    pfB.computePath();
  }

  private void initLayers() {
    AlgorithmStateLayer layerA = viewModel.addAlgorithmLayer("D*Lite-A", COLOR_ALGO_A);
    layerA.setAlpha(0.6);
    RobotEntity robotA = viewModel.addRobot("robot-A", COLOR_ALGO_A, startA.x, startA.y);

    AlgorithmStateLayer layerB = viewModel.addAlgorithmLayer("D*Lite-B", COLOR_ALGO_B);
    layerB.setAlpha(0.6);
    RobotEntity robotB = viewModel.addRobot("robot-B", COLOR_ALGO_B, startB.x, startB.y);

    slotA = new AlgorithmSlot<>("D*Lite-A", pfA, layerA, robotA);
    slotB = new AlgorithmSlot<>("D*Lite-B", pfB, layerB, robotB);
  }

  private void placeSampleWall() {
    int wallX = GRID_W / 2;
    for (int y = 2; y < GRID_H - 2; y++) {
      if (y == GRID_H / 2) continue;
      env.setObstacle(wallX, y);
    }
  }

  @SuppressWarnings("deprecation")
  private void syncModelFromEnv() {
    for (int y = 0; y < GRID_H; y++) {
      for (int x = 0; x < GRID_W; x++) {
        if (env.isObstacle(x, y)) {
          viewModel.setCellState(x, y, CellState.OBSTACLE);
        }
      }
    }
    viewModel.setCellState(startA.x, startA.y, CellState.START);
    viewModel.setCellState(startB.x, startB.y, CellState.START);
    viewModel.setCellState(goal.x, goal.y, CellState.GOAL);

    publishPath(slotA, pfA);
    publishPath(slotB, pfB);
  }

  // -------------------- Event wiring --------------------

  private GridInteractionListener createInteractionListener() {
    return new GridInteractionListener() {
      @Override
      public void onCellToggled(int x, int y, boolean isNowObstacle) {
        algorithmExecutor.submit(() -> handleObstacleToggle(x, y, isNowObstacle));
      }

      @Override
      public void onStartMoved(int x, int y) {}

      @Override
      public void onGoalMoved(int x, int y) {}

      @Override
      public void onReplanRequested() {
        algorithmExecutor.submit(() -> replanBoth());
      }
    };
  }

  // -------------------- Algorithm work --------------------

  private void handleObstacleToggle(int x, int y, boolean isNowObstacle) {
    Platform.runLater(() -> statusBar.setText("Replanning both algorithms..."));

    double newCellCost = isNowObstacle ? Double.POSITIVE_INFINITY : 1.0;
    Point2D target = new Point2D(x, y);

    List<EdgeUpdate<Point2D>> updates = new ArrayList<>();
    for (Point2D neighbor : env.getNeighbors(target)) {
      updates.add(new EdgeUpdate<>(neighbor, target, newCellCost));
    }

    if (isNowObstacle) {
      env.setObstacle(x, y);
    } else {
      env.removeObstacle(x, y);
    }

    if (!isNowObstacle) {
      for (Point2D neighbor : env.getNeighbors(target)) {
        updates.add(new EdgeUpdate<>(target, neighbor, env.getTraversalCost(target, neighbor)));
      }
    }

    replanOne(pfA, startA, updates);
    replanOne(pfB, startB, updates);

    Platform.runLater(
        () -> {
          publishPath(slotA, pfA);
          publishPath(slotB, pfB);
          statusBar.setText(
              String.format(
                  "A: %d nodes | B: %d nodes", pfA.getPath().size(), pfB.getPath().size()));
        });
  }

  private void replanOne(
      DStarLitePathFinder<Point2D> pf, Point2D start, List<EdgeUpdate<Point2D>> updates) {
    pf.setStart(start);
    pf.updateAllEdgeCosts(updates);
    pf.computePath();
  }

  private void replanBoth() {
    pfA.setStart(startA);
    pfA.computePath();
    pfB.setStart(startB);
    pfB.computePath();
    Platform.runLater(
        () -> {
          publishPath(slotA, pfA);
          publishPath(slotB, pfB);
          statusBar.setText("Replanned both");
        });
  }

  private void publishPath(AlgorithmSlot<Point2D> slot, DStarLitePathFinder<Point2D> pf) {
    slot.getStateLayer().updatePath(pf.getPath().toList());
  }

  // -------------------- Entry point --------------------

  public static void main(String[] args) {
    launch(args);
  }
}
