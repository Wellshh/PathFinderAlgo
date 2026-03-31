/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.visualizer.javafx;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import pathfinder.engine.AlgorithmMetrics;

/**
 * Read-only panel displaying side-by-side performance metrics for two algorithms. Automatically
 * updates when the bound {@link SimulationConfigModel}'s metrics properties change.
 */
public class StatisticsPanel extends GridPane {

  private final Label nameALabel = new Label("-");
  private final Label nameBLabel = new Label("-");
  private final Label timeALabel = new Label("-");
  private final Label timeBLabel = new Label("-");
  private final Label replanALabel = new Label("-");
  private final Label replanBLabel = new Label("-");
  private final Label pathALabel = new Label("-");
  private final Label pathBLabel = new Label("-");

  public StatisticsPanel(SimulationConfigModel config) {
    buildLayout();
    bindMetrics(config);
  }

  private void buildLayout() {
    setPadding(new Insets(8));
    setHgap(16);
    setVgap(4);

    Label header = new Label("Battle Statistics");
    header.setStyle("-fx-font-weight: bold;");
    add(header, 0, 0, 3, 1);

    add(new Label(""), 0, 1);
    add(boldLabel("Algorithm A"), 1, 1);
    add(boldLabel("Algorithm B"), 2, 1);

    add(new Label("Name:"), 0, 2);
    add(nameALabel, 1, 2);
    add(nameBLabel, 2, 2);

    add(new Label("Compute Time:"), 0, 3);
    add(timeALabel, 1, 3);
    add(timeBLabel, 2, 3);

    add(new Label("Replan Time:"), 0, 4);
    add(replanALabel, 1, 4);
    add(replanBLabel, 2, 4);

    add(new Label("Path Length:"), 0, 5);
    add(pathALabel, 1, 5);
    add(pathBLabel, 2, 5);
  }

  private void bindMetrics(SimulationConfigModel config) {
    config.metricsAProperty().addListener((obs, oldVal, newVal) -> applyMetricsA(newVal));
    config.metricsBProperty().addListener((obs, oldVal, newVal) -> applyMetricsB(newVal));
  }

  private void applyMetricsA(AlgorithmMetrics m) {
    if (m == null) {
      nameALabel.setText("-");
      timeALabel.setText("-");
      replanALabel.setText("-");
      pathALabel.setText("-");
    } else {
      nameALabel.setText(m.algorithmName());
      timeALabel.setText(String.format("%.3f ms", m.computeTimeMs()));
      replanALabel.setText(
          m.replanTimeNanos() > 0 ? String.format("%.3f ms", m.replanTimeMs()) : "-");
      pathALabel.setText(m.pathLength() > 0 ? String.valueOf(m.pathLength()) : "No path");
    }
  }

  private void applyMetricsB(AlgorithmMetrics m) {
    if (m == null) {
      nameBLabel.setText("-");
      timeBLabel.setText("-");
      replanBLabel.setText("-");
      pathBLabel.setText("-");
    } else {
      nameBLabel.setText(m.algorithmName());
      timeBLabel.setText(String.format("%.3f ms", m.computeTimeMs()));
      replanBLabel.setText(
          m.replanTimeNanos() > 0 ? String.format("%.3f ms", m.replanTimeMs()) : "-");
      pathBLabel.setText(m.pathLength() > 0 ? String.valueOf(m.pathLength()) : "No path");
    }
  }

  private static Label boldLabel(String text) {
    Label l = new Label(text);
    l.setStyle("-fx-font-weight: bold;");
    return l;
  }
}
