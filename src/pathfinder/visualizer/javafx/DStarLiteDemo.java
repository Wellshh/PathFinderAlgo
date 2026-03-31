/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

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
import pathfinder.model.EdgeUpdate;
import pathfinder.model.Point2D;
import pathfinder.testenv.environment.Grid2DEnvironment;
import pathfinder.visualizer.CellState;
import pathfinder.visualizer.GridInteractionListener;
import pathfinder.visualizer.GridViewModel;

/**
 * Standalone JavaFX demo that wires {@link DStarLitePathFinder} to the {@link JavaFXGridVisualizer}
 * through the observer event system.
 *
 * <p>Usage: click or drag on the canvas to toggle obstacles. Each toggle triggers an incremental
 * replan on a background thread; the updated path appears on the canvas without blocking the UI.
 */
public class DStarLiteDemo extends Application {

  private static final int GRID_W = 40;
  private static final int GRID_H = 30;
  private static final double CELL_PX = 20.0;

  private final Grid2DEnvironment env = new Grid2DEnvironment(GRID_W, GRID_H);
  private final GridViewModel viewModel = new GridViewModel(GRID_W, GRID_H);
  private final DStarLitePathFinder<Point2D> pathFinder = new DStarLitePathFinder<>();

  private final Point2D start = new Point2D(2, GRID_H / 2);
  private final Point2D goal = new Point2D(GRID_W - 3, GRID_H / 2);

  private final ExecutorService algorithmExecutor =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "algo-worker");
            t.setDaemon(true);
            return t;
          });

  private final Label statusBar = new Label("Ready — click to place obstacles");

  @Override
  public void start(Stage primaryStage) {
    initAlgorithm();
    syncModelFromEnv();

    JavaFXGridVisualizer visualizer = new JavaFXGridVisualizer();
    visualizer.setCellSize(CELL_PX);
    visualizer.initialize(viewModel);
    visualizer.addInteractionListener(createInteractionListener());

    BorderPane root = new BorderPane();
    root.setCenter(visualizer.asPane());
    root.setBottom(statusBar);

    Scene scene = new Scene(root);
    primaryStage.setTitle("D* Lite — Interactive Replan Demo");
    primaryStage.setScene(scene);
    primaryStage.setResizable(false);
    primaryStage.show();

    visualizer.show();
  }

  @Override
  public void stop() {
    algorithmExecutor.shutdownNow();
  }

  // -------------------- Algorithm setup --------------------

  private void initAlgorithm() {
    placeSampleWall();
    pathFinder.initialize(env, start, goal);
    pathFinder.computePath();
  }

  private void placeSampleWall() {
    int wallX = GRID_W / 2;
    for (int y = 2; y < GRID_H - 2; y++) {
      if (y == GRID_H / 2) continue; // leave a gap
      env.setObstacle(wallX, y);
    }
  }

  // -------------------- Model synchronisation --------------------

  private void syncModelFromEnv() {
    for (int y = 0; y < GRID_H; y++) {
      for (int x = 0; x < GRID_W; x++) {
        if (env.isObstacle(x, y)) {
          viewModel.setCellState(x, y, CellState.OBSTACLE);
        }
      }
    }
    viewModel.setCellState(start.x, start.y, CellState.START);
    viewModel.setCellState(goal.x, goal.y, CellState.GOAL);
    viewModel.updatePath(pathFinder.getPath().toList());
  }

  // -------------------- Event wiring --------------------

  private GridInteractionListener createInteractionListener() {
    return new GridInteractionListener() {

      @Override
      public void onCellToggled(int x, int y, boolean isNowObstacle) {
        algorithmExecutor.submit(() -> handleObstacleToggle(x, y, isNowObstacle));
      }

      @Override
      public void onStartMoved(int x, int y) {
        // Future extension
      }

      @Override
      public void onGoalMoved(int x, int y) {
        // Future extension
      }

      @Override
      public void onReplanRequested() {
        algorithmExecutor.submit(() -> replanAndPublish());
      }
    };
  }

  // -------------------- Algorithm work (runs on worker thread)
  // --------------------

  private void handleObstacleToggle(int x, int y, boolean isNowObstacle) {
    Platform.runLater(() -> statusBar.setText("Replanning..."));

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

    // For removed obstacles: also add edges FROM target to its new neighbors
    if (!isNowObstacle) {
      for (Point2D neighbor : env.getNeighbors(target)) {
        updates.add(new EdgeUpdate<>(target, neighbor, env.getTraversalCost(target, neighbor)));
      }
    }

    pathFinder.setStart(start);
    pathFinder.updateAllEdgeCosts(updates);
    pathFinder.computePath();

    List<Point2D> newPath = pathFinder.getPath().toList();
    boolean found = !newPath.isEmpty();

    Platform.runLater(
        () -> {
          viewModel.updatePath(newPath);
          statusBar.setText(
              found
                  ? "Path found (" + newPath.size() + " nodes)"
                  : "No path — obstacle blocks all routes");
        });
  }

  private void replanAndPublish() {
    pathFinder.setStart(start);
    pathFinder.computePath();
    List<Point2D> newPath = pathFinder.getPath().toList();

    Platform.runLater(
        () -> {
          viewModel.updatePath(newPath);
          statusBar.setText("Replanned (" + newPath.size() + " nodes)");
        });
  }

  // -------------------- Entry point --------------------

  public static void main(String[] args) {
    launch(args);
  }
}
