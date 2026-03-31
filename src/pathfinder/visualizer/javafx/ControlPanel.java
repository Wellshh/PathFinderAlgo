/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.visualizer.javafx;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import pathfinder.engine.SimulationState;
import pathfinder.factory.AlgorithmType;

/**
 * Reusable JavaFX control panel that bidirectionally binds to a {@link SimulationConfigModel}. All
 * UI widgets operate through the config interface and never touch the engine directly.
 */
public class ControlPanel extends VBox {

  private final SimulationConfigModel config;

  private final Button startPauseBtn = new Button("Start");
  private final Button stopBtn = new Button("Stop");
  private final Button resetBtn = new Button("Reset");
  private final Slider speedSlider = new Slider(0.1, 5.0, 1.0);
  private final Slider tickRateSlider = new Slider(1, 60, 10);
  private final CheckBox showOpenCb = new CheckBox("Open List");
  private final CheckBox showClosedCb = new CheckBox("Closed List");
  private final Label speedLabel = new Label("Speed: 1.0x");
  private final Label tickRateLabel = new Label("Tick Rate: 10.0/s");

  private final ComboBox<AlgorithmType> algoACombo = new ComboBox<>();
  private final ComboBox<AlgorithmType> algoBCombo = new ComboBox<>();
  private final CheckBox battleModeCb = new CheckBox("Battle Mode");
  private final Button battleBtn = new Button("Start Battle");

  private Runnable onBattleRequested;
  private Runnable onResetRequested;

  public ControlPanel(SimulationConfigModel config) {
    this.config = config;
    buildLayout();
    bindProperties();
    setupActions();
  }

  /** Register a callback invoked when the user clicks "Start Battle". */
  public void setOnBattleRequested(Runnable handler) {
    this.onBattleRequested = handler;
  }

  /** Register a callback invoked when the user clicks "Reset". */
  public void setOnResetRequested(Runnable handler) {
    this.onResetRequested = handler;
  }

  private void buildLayout() {
    setPadding(new Insets(8));
    setSpacing(6);

    HBox transportRow = new HBox(8, startPauseBtn, stopBtn, resetBtn);
    transportRow.setAlignment(Pos.CENTER_LEFT);

    speedSlider.setShowTickLabels(true);
    speedSlider.setMajorTickUnit(1.0);
    speedSlider.setBlockIncrement(0.1);

    tickRateSlider.setShowTickLabels(true);
    tickRateSlider.setMajorTickUnit(10);
    tickRateSlider.setBlockIncrement(1);

    HBox speedRow = new HBox(8, speedLabel, speedSlider);
    speedRow.setAlignment(Pos.CENTER_LEFT);

    HBox tickRateRow = new HBox(8, tickRateLabel, tickRateSlider);
    tickRateRow.setAlignment(Pos.CENTER_LEFT);

    HBox toggleRow = new HBox(12, showOpenCb, showClosedCb);
    toggleRow.setAlignment(Pos.CENTER_LEFT);

    // Algorithm selection & battle controls
    StringConverter<AlgorithmType> converter =
        new StringConverter<>() {
          @Override
          public String toString(AlgorithmType t) {
            return t == null ? "" : t.getDisplayName();
          }

          @Override
          public AlgorithmType fromString(String s) {
            return null;
          }
        };

    algoACombo.setItems(FXCollections.observableArrayList(AlgorithmType.values()));
    algoACombo.setConverter(converter);
    algoBCombo.setItems(FXCollections.observableArrayList(AlgorithmType.values()));
    algoBCombo.setConverter(converter);

    HBox algoRow = new HBox(8, new Label("Algo A:"), algoACombo, new Label("Algo B:"), algoBCombo);
    algoRow.setAlignment(Pos.CENTER_LEFT);

    battleBtn.setDisable(true);
    HBox battleRow = new HBox(12, battleModeCb, battleBtn);
    battleRow.setAlignment(Pos.CENTER_LEFT);

    getChildren()
        .addAll(
            transportRow, speedRow, tickRateRow, toggleRow, new Separator(), algoRow, battleRow);
  }

  private void bindProperties() {
    speedSlider.valueProperty().bindBidirectional(config.playbackSpeedProperty());
    tickRateSlider.valueProperty().bindBidirectional(config.logicTicksPerSecondProperty());
    showOpenCb.selectedProperty().bindBidirectional(config.showOpenListProperty());
    showClosedCb.selectedProperty().bindBidirectional(config.showClosedListProperty());

    algoACombo.valueProperty().bindBidirectional(config.selectedAlgorithmAProperty());
    algoBCombo.valueProperty().bindBidirectional(config.selectedAlgorithmBProperty());
    battleModeCb.selectedProperty().bindBidirectional(config.battleModeEnabledProperty());

    config
        .playbackSpeedProperty()
        .addListener(
            (obs, oldVal, newVal) ->
                speedLabel.setText(String.format("Speed: %.1fx", newVal.doubleValue())));

    config
        .logicTicksPerSecondProperty()
        .addListener(
            (obs, oldVal, newVal) ->
                tickRateLabel.setText(String.format("Tick Rate: %.0f/s", newVal.doubleValue())));

    config
        .simulationStateProperty()
        .addListener((obs, oldVal, newVal) -> updateButtonLabels(newVal));

    config
        .battleModeEnabledProperty()
        .addListener((obs, oldVal, newVal) -> battleBtn.setDisable(!newVal));
  }

  private void setupActions() {
    startPauseBtn.setOnAction(
        e -> {
          SimulationState state = config.getSimulationState();
          if (state == SimulationState.IDLE || state == SimulationState.PAUSED) {
            config.requestStart();
          } else if (state == SimulationState.RUNNING) {
            config.requestPause();
          }
        });

    stopBtn.setOnAction(e -> config.requestStop());
    resetBtn.setOnAction(
        e -> {
          if (onResetRequested != null) {
            onResetRequested.run();
          }
        });

    battleBtn.setOnAction(
        e -> {
          if (onBattleRequested != null) {
            onBattleRequested.run();
          }
        });
  }

  private void updateButtonLabels(SimulationState state) {
    switch (state) {
      case IDLE, PAUSED -> {
        startPauseBtn.setText("Start");
        startPauseBtn.setDisable(false);
      }
      case RUNNING -> {
        startPauseBtn.setText("Pause");
        startPauseBtn.setDisable(false);
      }
      case FINISHED -> {
        startPauseBtn.setText("Start");
        startPauseBtn.setDisable(true);
      }
    }
  }
}
