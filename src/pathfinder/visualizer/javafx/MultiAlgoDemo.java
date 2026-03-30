package pathfinder.visualizer.javafx;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pathfinder.api.IPathFinder;
import pathfinder.engine.AlgorithmMetrics;
import pathfinder.engine.AlgorithmSlot;
import pathfinder.engine.AllParallelScheduler;
import pathfinder.engine.BattleEngine;
import pathfinder.engine.SimulationController;
import pathfinder.factory.AlgorithmFactory;
import pathfinder.factory.AlgorithmType;
import pathfinder.model.Point2D;
import pathfinder.testenv.environment.Grid2DEnvironment;
import pathfinder.visualizer.CellState;
import pathfinder.visualizer.GridInteractionListener;
import pathfinder.visualizer.GridViewModel;
import pathfinder.visualizer.model.AlgorithmStateLayer;
import pathfinder.visualizer.model.RobotEntity;

/**
 * Runtime-configurable multi-algorithm battle demo. Algorithm selection, battle execution, and
 * performance comparison are all driven through {@link SimulationConfigModel} and {@link
 * BattleEngine}, with no hard-coded algorithm dependencies.
 */
public class MultiAlgoDemo extends Application {

  private static final int GRID_W = 50;
  private static final int GRID_H = 35;
  private static final double CELL_PX = 18.0;

  private static final int COLOR_ALGO_A = 0xFFE65100; // deep orange
  private static final int COLOR_ALGO_B = 0xFF1565C0; // blue

  private final Grid2DEnvironment env = new Grid2DEnvironment(GRID_W, GRID_H);
  private final GridViewModel viewModel = new GridViewModel(GRID_W, GRID_H);

  private final Point2D start = new Point2D(2, GRID_H / 2);
  private final Point2D goal = new Point2D(GRID_W - 3, GRID_H / 2);

  private AlgorithmSlot<Point2D> slotA;
  private AlgorithmSlot<Point2D> slotB;

  private final SimulationController simController = new SimulationController();
  private SimulationConfigModel configModel;
  private final BattleEngine<Point2D> battleEngine = new BattleEngine<>();
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
    configModel = new SimulationConfigModel(simController);

    placeSampleWall();
    rebuildSlots();
    syncObstaclesToViewModel();
    runBattle();

    JavaFXGridVisualizer visualizer = new JavaFXGridVisualizer();
    visualizer.setCellSize(CELL_PX);
    visualizer.initialize(viewModel);
    visualizer.addInteractionListener(createInteractionListener());

    simController.setScheduler(new AllParallelScheduler());
    simController.setLogicTicksPerSecond(5.0);
    simController.addRenderTickListener(dt -> visualizer.renderFrame(dt));

    ControlPanel controlPanel = new ControlPanel(configModel);
    controlPanel.setOnBattleRequested(() -> algorithmExecutor.submit(this::runBattle));

    StatisticsPanel statsPanel = new StatisticsPanel(configModel);

    VBox rightPane = new VBox(8, controlPanel, statsPanel);

    BorderPane root = new BorderPane();
    root.setCenter(visualizer.asPane());
    root.setRight(rightPane);
    root.setBottom(statusBar);

    configModel
        .selectedAlgorithmAProperty()
        .addListener((obs, oldVal, newVal) -> onAlgorithmChanged());
    configModel
        .selectedAlgorithmBProperty()
        .addListener((obs, oldVal, newVal) -> onAlgorithmChanged());

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

  // -------------------- Slot management --------------------

  private void rebuildSlots() {
    AlgorithmType typeA = configModel.getSelectedAlgorithmA();
    AlgorithmType typeB = configModel.getSelectedAlgorithmB();

    viewModel.getAlgoLayers().clear();
    viewModel.getRobots().clear();

    IPathFinder<Point2D> pfA = AlgorithmFactory.createPathFinder(typeA);
    AlgorithmStateLayer layerA = viewModel.addAlgorithmLayer(typeA.getDisplayName(), COLOR_ALGO_A);
    layerA.setAlpha(0.6);
    RobotEntity robotA = viewModel.addRobot("robot-A", COLOR_ALGO_A, start.x, start.y);
    slotA = new AlgorithmSlot<>(typeA.getDisplayName(), pfA, layerA, robotA);

    IPathFinder<Point2D> pfB = AlgorithmFactory.createPathFinder(typeB);
    AlgorithmStateLayer layerB = viewModel.addAlgorithmLayer(typeB.getDisplayName(), COLOR_ALGO_B);
    layerB.setAlpha(0.6);
    RobotEntity robotB = viewModel.addRobot("robot-B", COLOR_ALGO_B, start.x, start.y);
    slotB = new AlgorithmSlot<>(typeB.getDisplayName(), pfB, layerB, robotB);
  }

  private void onAlgorithmChanged() {
    algorithmExecutor.submit(
        () -> {
          rebuildSlots();
          runBattle();
        });
  }

  // -------------------- Environment setup --------------------

  private void placeSampleWall() {
    int wallX = GRID_W / 2;
    for (int y = 2; y < GRID_H - 2; y++) {
      if (y == GRID_H / 2) continue;
      env.setObstacle(wallX, y);
    }
  }

  @SuppressWarnings("deprecation")
  private void syncObstaclesToViewModel() {
    for (int y = 0; y < GRID_H; y++) {
      for (int x = 0; x < GRID_W; x++) {
        if (env.isObstacle(x, y)) {
          viewModel.setCellState(x, y, CellState.OBSTACLE);
        } else {
          viewModel.setCellState(x, y, CellState.FREE);
        }
      }
    }
    viewModel.setCellState(start.x, start.y, CellState.START);
    viewModel.setCellState(goal.x, goal.y, CellState.GOAL);
  }

  // -------------------- Battle execution --------------------

  private void runBattle() {
    Platform.runLater(() -> statusBar.setText("Running battle..."));

    List<AlgorithmMetrics> results =
        battleEngine.runBattle(env, start, goal, List.of(slotA, slotB));

    publishPath(slotA);
    publishPath(slotB);

    AlgorithmMetrics mA = results.get(0);
    AlgorithmMetrics mB = results.get(1);

    Platform.runLater(
        () -> {
          configModel.setMetricsA(mA);
          configModel.setMetricsB(mB);
          statusBar.setText(
              String.format(
                  "%s: %.3f ms, %d nodes | %s: %.3f ms, %d nodes",
                  mA.algorithmName(),
                  mA.computeTimeMs(),
                  mA.pathLength(),
                  mB.algorithmName(),
                  mB.computeTimeMs(),
                  mB.pathLength()));
        });
  }

  private void publishPath(AlgorithmSlot<Point2D> slot) {
    slot.getStateLayer().updatePath(slot.getPathFinder().getPath().toList());
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
        algorithmExecutor.submit(() -> runBattle());
      }
    };
  }

  private void handleObstacleToggle(int x, int y, boolean isNowObstacle) {
    if (isNowObstacle) {
      env.setObstacle(x, y);
    } else {
      env.removeObstacle(x, y);
    }

    rebuildSlots();
    syncObstaclesToViewModel();
    runBattle();
  }

  // -------------------- Entry point --------------------

  public static void main(String[] args) {
    launch(args);
  }
}
