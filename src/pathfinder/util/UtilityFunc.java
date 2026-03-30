package pathfinder.util;

import pathfinder.model.Point;

public class UtilityFunc {
  public static final double EPISILON = 1e-5;

  /*
   * Returns true if two numbers are within EPISILON, false otherwise
   */
  public static boolean isClose(double num1, double num2) {
    if (num1 == Double.POSITIVE_INFINITY && num2 == Double.POSITIVE_INFINITY) return true;
    return Math.abs(num1 - num2) < EPISILON;
  }

  // ------- Heuristic functions ---------

  /**
   * A Universal Heuristic Class Regardless of the point type (2D, 3D ....)
   *
   * @return Always return the heurisitc functional interface.
   */
  public static class Heuristics {
    /**
     * For robot movement in any directions.
     *
     * <p>1. Performance costs higher due to square root. 2. The heuristic value is not admissible.
     */
    public static <T extends Point> Heuristic<T> euclideanDis() {
      return (p1, p2) -> {
        double sum = 0;
        for (int i = 0; i < p1.getDim(); i++) {
          double diff = p1.getDimDiff(i, p2);
          sum += diff * diff;
        }
        return Math.sqrt(sum);
      };
    }

    /*
     * For robot movement horizontal or vertical only.
     */
    public static <T extends Point> Heuristic<T> manhattanDis() {
      return (p1, p2) -> {
        double sum = 0;
        for (int i = 0; i < p1.getDim(); i++) {
          double diff = p1.getDimDiff(i, p2);
          sum += Math.abs(diff);
        }
        return sum;
      };
    }

    /*
     * For robot movement horizontal, vertical or diagonal
     * with the same cost.
     */
    public static <T extends Point> Heuristic<T> chebyshevDis() {
      return (p1, p2) -> {
        double max = 0;
        for (int i = 0; i < p1.getDim(); i++) {
          max = Math.max(max, Math.abs(p1.getDimDiff(i, p2)));
        }
        return max;
      };
    }

    /**
     * For robot movement horizontal, vertical or diagonal with different costs.
     *
     * <p>This template implementation has high performance cost, may need overrided in low
     * dimensions.
     */
    // public static <T extends Point> Heuristic<T> octileDis(){
    // return (p1, p2) -> {

    // }
    // }
  }
  // ------- Heurusitc functions ---------
}
