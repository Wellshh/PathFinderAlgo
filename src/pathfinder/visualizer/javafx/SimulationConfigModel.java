package pathfinder.visualizer.javafx;

import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import pathfinder.engine.ISimulationConfig;
import pathfinder.engine.SimulationController;
import pathfinder.engine.SimulationState;

/**
 * JavaFX-backed implementation of {@link ISimulationConfig}. All properties are JavaFX observable
 * properties, enabling bidirectional binding with UI controls (sliders, toggle buttons, etc.).
 *
 * <p>Changes are automatically forwarded to the bound {@link SimulationController}.
 */
public class SimulationConfigModel implements ISimulationConfig {

  private final SimulationController controller;

  private final DoubleProperty playbackSpeed = new SimpleDoubleProperty(1.0);
  private final DoubleProperty logicTicksPerSecond = new SimpleDoubleProperty(10.0);
  private final ObjectProperty<SimulationState> simulationState =
      new SimpleObjectProperty<>(SimulationState.IDLE);
  private final BooleanProperty showOpenList = new SimpleBooleanProperty(true);
  private final BooleanProperty showClosedList = new SimpleBooleanProperty(true);

  public SimulationConfigModel(SimulationController controller) {
    this.controller = controller;

    playbackSpeed.addListener(
        (obs, oldVal, newVal) -> controller.setPlaybackSpeed(newVal.doubleValue()));
    logicTicksPerSecond.addListener(
        (obs, oldVal, newVal) -> controller.setLogicTicksPerSecond(newVal.doubleValue()));
    controller.addStateChangeListener(
        newState -> javafx.application.Platform.runLater(() -> simulationState.set(newState)));
  }

  // -------------------- JavaFX property accessors --------------------

  public DoubleProperty playbackSpeedProperty() {
    return playbackSpeed;
  }

  public DoubleProperty logicTicksPerSecondProperty() {
    return logicTicksPerSecond;
  }

  public ObjectProperty<SimulationState> simulationStateProperty() {
    return simulationState;
  }

  public BooleanProperty showOpenListProperty() {
    return showOpenList;
  }

  public BooleanProperty showClosedListProperty() {
    return showClosedList;
  }

  // -------------------- ISimulationConfig implementation --------------------

  @Override
  public double getPlaybackSpeed() {
    return playbackSpeed.get();
  }

  @Override
  public void setPlaybackSpeed(double speed) {
    playbackSpeed.set(speed);
  }

  @Override
  public void addPlaybackSpeedListener(Consumer<Double> listener) {
    playbackSpeed.addListener((obs, oldVal, newVal) -> listener.accept(newVal.doubleValue()));
  }

  @Override
  public SimulationState getSimulationState() {
    return simulationState.get();
  }

  @Override
  public void requestStart() {
    controller.start();
  }

  @Override
  public void requestPause() {
    controller.pause();
  }

  @Override
  public void requestResume() {
    controller.resume();
  }

  @Override
  public void requestStop() {
    controller.stop();
  }

  @Override
  public void requestReset() {
    controller.reset();
  }

  @Override
  public void addStateChangeListener(Consumer<SimulationState> listener) {
    simulationState.addListener((obs, oldVal, newVal) -> listener.accept(newVal));
  }

  @Override
  public double getLogicTicksPerSecond() {
    return logicTicksPerSecond.get();
  }

  @Override
  public void setLogicTicksPerSecond(double tps) {
    logicTicksPerSecond.set(tps);
  }

  @Override
  public void addTickRateListener(Consumer<Double> listener) {
    logicTicksPerSecond.addListener(
        (obs, oldVal, newVal) -> listener.accept(newVal.doubleValue()));
  }

  @Override
  public boolean isShowOpenList() {
    return showOpenList.get();
  }

  @Override
  public void setShowOpenList(boolean show) {
    showOpenList.set(show);
  }

  @Override
  public void addShowOpenListListener(Consumer<Boolean> listener) {
    showOpenList.addListener((obs, oldVal, newVal) -> listener.accept(newVal));
  }

  @Override
  public boolean isShowClosedList() {
    return showClosedList.get();
  }

  @Override
  public void setShowClosedList(boolean show) {
    showClosedList.set(show);
  }

  @Override
  public void addShowClosedListListener(Consumer<Boolean> listener) {
    showClosedList.addListener((obs, oldVal, newVal) -> listener.accept(newVal));
  }
}
