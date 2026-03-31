/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.visualizer.model;

/**
 * A movable entity with sub-cell floating-point coordinates. Decoupled from the integer grid so the
 * renderer can display smooth interpolated movement between cells.
 *
 * <p>Each render tick, call {@link #updateInterpolation(double)} with the elapsed time in seconds.
 * The entity linearly interpolates from its start position toward the target, applying an
 * ease-in-out cubic easing curve for a polished feel.
 */
public class RobotEntity {

  private final String id;
  private int colorArgb;

  private double startX, startY;
  private double targetX, targetY;
  private double currentX, currentY;
  private double progress = 1.0;
  private double speed = 4.0;

  public RobotEntity(String id, int colorArgb, double startCellX, double startCellY) {
    this.id = id;
    this.colorArgb = colorArgb;
    this.startX = startCellX;
    this.startY = startCellY;
    this.targetX = startCellX;
    this.targetY = startCellY;
    this.currentX = startCellX;
    this.currentY = startCellY;
  }

  public String getId() {
    return id;
  }

  public int getColorArgb() {
    return colorArgb;
  }

  public void setColorArgb(int colorArgb) {
    this.colorArgb = colorArgb;
  }

  public double getCurrentX() {
    return currentX;
  }

  public double getCurrentY() {
    return currentY;
  }

  public double getTargetX() {
    return targetX;
  }

  public double getTargetY() {
    return targetY;
  }

  /** Speed in cells per second. */
  public double getSpeed() {
    return speed;
  }

  public void setSpeed(double cellsPerSecond) {
    this.speed = cellsPerSecond;
  }

  /**
   * @return true if the entity has reached its target.
   */
  public boolean isIdle() {
    return progress >= 1.0;
  }

  /**
   * Begin interpolating toward a new target cell center. If already animating, the current rendered
   * position becomes the new start point for a smooth transition.
   */
  public void setTarget(double cellX, double cellY) {
    this.startX = this.currentX;
    this.startY = this.currentY;
    this.targetX = cellX;
    this.targetY = cellY;
    this.progress = 0.0;
  }

  /** Convenience overload accepting integer cell coordinates. */
  public void setTarget(int cellX, int cellY) {
    setTarget((double) cellX, (double) cellY);
  }

  /** Snap the entity to a position instantly without animation. */
  public void snapTo(double cellX, double cellY) {
    this.startX = cellX;
    this.startY = cellY;
    this.targetX = cellX;
    this.targetY = cellY;
    this.currentX = cellX;
    this.currentY = cellY;
    this.progress = 1.0;
  }

  /**
   * Advance the interpolation by {@code deltaTime} seconds.
   *
   * @return {@code true} if the position changed (caller should mark region dirty).
   */
  public boolean updateInterpolation(double deltaTime) {
    if (progress >= 1.0) return false;

    double dx = targetX - startX;
    double dy = targetY - startY;
    double distance = Math.sqrt(dx * dx + dy * dy);
    if (distance < 1e-9) {
      progress = 1.0;
      currentX = targetX;
      currentY = targetY;
      return true;
    }

    double step = (speed * deltaTime) / distance;
    progress = Math.min(1.0, progress + step);

    double easedT = easeInOutCubic(progress);
    currentX = lerp(startX, targetX, easedT);
    currentY = lerp(startY, targetY, easedT);
    return true;
  }

  // -------------------- Math utilities --------------------

  private static double lerp(double a, double b, double t) {
    return a + (b - a) * t;
  }

  private static double easeInOutCubic(double t) {
    return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
  }
}
