package pathfinder.util;

public class UtilityFunc {
  public static final double EPISILON = 1e-5;

  /*
   * Returns true if two numbers are within EPISILON, false otherwise
   */
  public static boolean isClose(double num1, double num2) {
    if (num1 == Double.POSITIVE_INFINITY && num2 == Double.POSITIVE_INFINITY) return true;
    return Math.abs(num1 - num2) < EPISILON;
  }
}
