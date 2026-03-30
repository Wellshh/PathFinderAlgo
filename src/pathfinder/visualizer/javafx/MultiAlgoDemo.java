package pathfinder.visualizer.javafx;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.animation.AnimationTimer;
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
import pathfinder.engine.SimulationRunner;
import pathfinder.factory.AlgorithmFactory;
import pathfinder.factory.AlgorithmType;
import pathfinder.model.EdgeUpdate;
import pathfinder.model.Point2D;
import pathfinder.testenv.environment.Grid2DEnvironment;
import pathfinder.testenv.sensor.SimpleGridSensor;
import pathfinder.visualizer.CellState;
import pathfinder.visualizer.GridInteractionListener;
import pathfinder.visualizer.GridViewModel;
import pathfinder.visualizer.model.AlgorithmStateLayer;
import pathfinder.visualizer.model.RobotEntity;

/**
 * Runtime-configurable multi-algorithm battle and simulation demo. Supports two modes:
 *
 * <ul>
 *   <li><b>Battle mode</b>: one-shot comparison of two algorithms on the full ground-truth map
 *   <li><b>Simulation mode</b>: robots autonomously move, sense hidden obstacles via {@link
 *       SimpleGridSensor}, and replan incrementally (Start/Pause/Stop controls)
 * </ul>
 *
 * <p>Obstacle clicks during simulation modify only the ground truth; robots discover them through
 * their sensor.
 */
public class MultiAlgoDemo extends Application {

  private static final int GRID_W = 50;
  private static final int GRID_H = 35;
  private static final double CELL_PX = 18.0;
  private static final int SENSE_RADIUS = 3;

  private static final int COLOR_ALGO_A = 0xFFE65100; // deep orange
  private static final int COLOR_ALGO_B = 0xFF1565C0; // blue

  private final Grid2DEnvironment groundTruth = new Grid2DEnvironment(GRID_W, GRID_H);
  private Grid2DEnvironment knownMapA;
  private Grid2DEnvironment knownMapB;

  private final GridViewModel viewModel = new GridViewModel(GRID_W, GRID_H);

  private final Point2D start = new Point2D(2, GRID_H / 2);
  private final Point2D goal = new Point2D(GRID_W - 3, GRID_H / 2);

  private AlgorithmSlot<Point2D> slotA;
  private AlgorithmSlot<Point2D> slotB;

  private final SimulationController simController = new SimulationController();
  private SimulationConfigModel configModel;
  private final BattleEngine<Point2D> battleEngine = new BattleEngine<>();
  private SimulationRunner<Point2D> simRunner;
  private AnimationTimer tickPump;

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
    runInitialBattle();

    JavaFXGridVisualizer visualizer = new JavaFXGridVisualizer();
    visualizer.setCellSize(CELL_PX);
    visualizer.initialize(viewModel);
    visualizer.addInteractionListener(createInteractionListener());

    simController.setScheduler(new AllParallelScheduler());
    simController.setLogicTicksPerSecond(5.0);

    // The visualizer renders via simController's render tick, driven by our tick pump.
    // Do NOT call visualizer.show() — we drive rendering ourselves.
    simController.addRenderTickListener(dt -> visualizer.renderFrame(dt));
    createTickPump();

    ControlPanel controlPanel = new ControlPanel(configModel);
    controlPanel.setOnBattleRequested(() -> algorithmExecutor.submit(this::runInitialBattle));

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

    tickPump.start();
  }

  @Override
  public void stop() {
    if (tickPump != null) tickPump.stop();
    algorithmExecutor.shutdownNow();
  }

  /**
   * AnimationTimer that pumps {@link SimulationController#onRenderTick(double)} every frame.
   * This is the single driver for both logic ticks (simulation steps) and render callbacks.
   */
  private void createTickPump() {
    tickPump =
        new AnimationTimer() {
          private long lastNanos = 0;

          @Override
          public void handle(long nowNanos) {
            double dt = 0.0;
            if (lastNanos > 0) {
              dt = (nowNanos - lastNanos) / 1_000_000_000.0;
            }
            lastNanos = nowNanos;
            simController.onRenderTick(dt);
          }
        };
  }

  // -------------------- Slot & environment management --------------------

  private void rebuildSlots() {
    AlgorithmType typeA = configModel.getSelectedAlgorithmA();
    AlgorithmType typeB = configModel.getSelectedAlgorithmB();

    viewModel.getAlgoLayers().clear();
    viewModel.getRobots().clear();
    simController.clearAlgorithmStepActions();

    knownMapA = copyEnvironment(groundTruth);
    knownMapB = copyEnvironment(groundTruth);

    IPathFinder<Point2D> pfA = AlgorithmFactory.createPathFinder(typeA);
    AlgorithmStateLayer layerA =
        viewModel.addAlgorithmLayer(typeA.getDisplayName(), COLOR_ALGO_A);
    layerA.setAlpha(0.6);
    RobotEntity robotA = viewModel.addRobot("robot-A", COLOR_ALGO_A, start.x, start.y);
    slotA = new AlgorithmSlot<>(typeA.getDisplayName(), pfA, layerA, robotA);

    IPathFinder<Point2D> pfB = AlgorithmFactory.createPathFinder(typeB);
    AlgorithmStateLayer layerB =
        viewModel.addAlgorithmLayer(typeB.getDisplayName(), COLOR_ALGO_B);
    layerB.setAlpha(0.6);
    RobotEntity robotB = viewModel.addRobot("robot-B", COLOR_ALGO_B, start.x, start.y);
    slotB = new AlgorithmSlot<>(typeB.getDisplayName(), pfB, layerB, robotB);

    setupSimulationRunner();
    syncViewModelFromGroundTruth();
  }

  private void setupSimulationRunner() {
    simRunner = new SimulationRunner<>(goal);

    SimpleGridSensor sensorA = new SimpleGridSensor(groundTruth, knownMapA, SENSE_RADIUS);
    SimpleGridSensor sensorB = new SimpleGridSensor(groundTruth, knownMapB, SENSE_RADIUS);

    simRunner.registerSlot(slotA, knownMapA, sensorA);
    simRunner.registerSlot(slotB, knownMapB, sensorB);

    simRunner.setOnMetricsUpdated(this::onSlotMetricsUpdated);
    simRunner.setOnEnvironmentDiscovered(this::onObstaclesDiscovered);

    simController.clearAlgorithmStepActions();
    simController.addAlgorithmStepAction(() -> simRunner.stepSlot(slotA));
    simController.addAlgorithmStepAction(() -> simRunner.stepSlot(slotB));
  }

  private void onAlgorithmChanged() {
    algorithmExecutor.submit(
        () -> {
          simController.reset();
          rebuildSlots();
          runInitialBattle();
        });
  }

  // -------------------- Environment setup --------------------

  private void placeSampleWall() {
    int wallX = GRID_W / 2;
    for (int y = 2; y < GRID_H - 2; y++) {
      if (y == GRID_H / 2) continue;
      groundTruth.setObstacle(wallX, y);
    }
  }

  private Grid2DEnvironment copyEnvironment(Grid2DEnvironment source) {
    Grid2DEnvironment copy = new Grid2DEnvironment(GRID_W, GRID_H);
    for (int y = 0; y < GRID_H; y++) {
      for (int x = 0; x < GRID_W; x++) {
        if (source.isObstacle(x, y)) {
          copy.setObstacle(x, y);
        }
      }
    }
    return copy;
  }

  @SuppressWarnings("deprecation")
  private void syncViewModelFromGroundTruth() {
    for (int y = 0; y < GRID_H; y++) {
      for (int x = 0; x < GRID_W; x++) {
        if (groundTruth.isObstacle(x, y)) {
          viewModel.setCellState(x, y, CellState.OBSTACLE);
        } else {
          viewModel.setCellState(x, y, CellState.FREE);
        }
      }
    }
    viewModel.setCellState(start.x, start.y, CellState.START);
    viewModel.setCellState(goal.x, goal.y, CellState.GOAL);
  }

  // -------------------- Battle execution (initial plan) --------------------

  private void runInitialBattle() {
    Platform.runLater(() -> statusBar.setText("Running initial plan..."));

    battleEngine.runBattle(knownMapA, start, goal, List.of(slotA));
    battleEngine.runBattle(knownMapB, start, goal, List.of(slotB));

    publishPath(slotA);
    publishPath(slotB);

    AlgorithmMetrics mA = slotA.getLastMetrics();
    AlgorithmMetrics mB = slotB.getLastMetrics();

    Platform.runLater(
        () -> {
          configModel.setMetricsA(mA);
          configModel.setMetricsB(mB);
          statusBar.setText(
              String.format(
                  "%s: %.3f ms, %d nodes | %s: %.3f ms, %d nodes — Press Start to simulate",
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

  // -------------------- Simulation callbacks --------------------

  private void onSlotMetricsUpdated(AlgorithmSlot<Point2D> slot) {
    AlgorithmMetrics m = slot.getLastMetrics();
    if (m == null) return;
    Platform.runLater(
        () -> {
          if (slot == slotA) {
            configModel.setMetricsA(m);
          } else if (slot == slotB) {
            configModel.setMetricsB(m);
          }
          statusBar.setText(
              String.format(
                  "%s replan: %.3f ms | path: %d",
                  m.algorithmName(), m.replanTimeMs(), m.pathLength()));
        });
  }

  @SuppressWarnings("deprecation")
  private void onObstaclesDiscovered(
      AlgorithmSlot<Point2D> slot, List<EdgeUpdate<Point2D>> updates) {
    Platform.runLater(
        () -> {
          for (EdgeUpdate<Point2D> u : updates) {
            Point2D to = u.to;
            if (to.x >= 0 && to.x < GRID_W && to.y >= 0 && to.y < GRID_H) {
              if (u.newCost == Double.POSITIVE_INFINITY) {
                viewModel.setCellState(to.x, to.y, CellState.OBSTACLE);
              }
            }
          }
        });
  }

  // -------------------- Event wiring --------------------

  private GridInteractionListener createInteractionListener() {
    return new GridInteractionListener() {
      @Override
      public void onCellToggled(int x, int y, boolean isNowObstacle) {
        if (isNowObstacle) {
          groundTruth.setObstacle(x, y);
        } else {
          groundTruth.removeObstacle(x, y);
        }
      }

      @Override
      public void onStartMoved(int x, int y) {}

      @Override
      public void onGoalMoved(int x, int y) {}

      @Override
      public void onReplanRequested() {
        algorithmExecutor.submit(() -> runInitialBattle());
      }
    };
  }

  // -------------------- Entry point --------------------

  public static void main(String[] args) {
    launch(args);
  }
}
