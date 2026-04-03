/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.api;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Public API for configuring PathFinder logging.
 *
 * <p>All algorithm loggers are rooted at "pathfinder", so a single call to {@link
 * #setLevel(Level)} controls the verbosity of the entire library.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Enable debug output
 * PathFinderLogging.setLevel(Level.FINE);
 *
 * // Suppress all output below WARNING (default)
 * PathFinderLogging.setLevel(Level.WARNING);
 * }</pre>
 */
public final class PathFinderLogging {

  /** Root logger name shared by every pathfinder sub-logger. */
  static final String LOGGER_ROOT = "pathfinder";

  private static final ConsoleHandler HANDLER;

  static {
    // Single-line format: "[LEVEL] loggerName - message"
    System.setProperty(
        "java.util.logging.SimpleFormatter.format", "[%4$s] %2$s - %5$s%6$s%n");
    HANDLER = new ConsoleHandler();
    HANDLER.setLevel(Level.ALL);
    HANDLER.setFormatter(new SimpleFormatter());

    Logger root = Logger.getLogger(LOGGER_ROOT);
    root.setUseParentHandlers(false);
    root.addHandler(HANDLER);
    // Default: suppress debug (FINE) output to keep production output clean
    root.setLevel(Level.WARNING);
  }

  private PathFinderLogging() {}

  /**
   * Sets the logging level for all PathFinder loggers.
   *
   * <p>Use {@link Level#FINE} to enable debug-level output. Use {@link Level#WARNING} or {@link
   * Level#OFF} to suppress output.
   *
   * @param level the desired logging level
   */
  public static void setLevel(Level level) {
    Logger.getLogger(LOGGER_ROOT).setLevel(level);
    HANDLER.setLevel(level);
  }

  /**
   * Returns the current logging level of the root PathFinder logger.
   *
   * @return current {@link Level}
   */
  public static Level getLevel() {
    return Logger.getLogger(LOGGER_ROOT).getLevel();
  }
}
