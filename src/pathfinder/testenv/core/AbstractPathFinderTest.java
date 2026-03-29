package pathfinder.testenv.core;

import java.util.LinkedHashMap;
import java.util.Map;

import pathfinder.api.IPathFinder;
import pathfinder.model.Environment;
import pathfinder.model.Point;
import pathfinder.testenv.visualizer.ITestVisualizer;

/**
 * Template Method base class for path-finder algorithm tests.
 * Defines a 5-phase lifecycle, each phase timed with {@code System.nanoTime()}.
 *
 * Subclasses implement the abstract hooks to wire up a specific algorithm
 * and environment configuration.
 *
 * @param <P> the spatial coordinate type
 */
public abstract class AbstractPathFinderTest<P extends Point> {

	protected Environment<P> env;
	protected IPathFinder<P> pathFinder;
	protected ITestVisualizer<P> visualizer;
	protected P start;
	protected P goal;

	protected final Map<String, Long> timings = new LinkedHashMap<>();

	/** Returns a human-readable name for the test (shown in output). */
	protected abstract String getTestName();

	/** Phase 1 hook: build the environment, set start/goal, attach visualizer. */
	protected abstract void setupEnvironment();

	/** Phase 2 hook: factory method to create the algorithm instance. */
	protected abstract IPathFinder<P> createPathFinder();

	/** Phase 4 hook: run dynamic re-planning scenario (no-op for static algorithms). */
	protected abstract void runDynamicScenario();

	/**
	 * Template method -- drives the full test lifecycle.
	 * Marked {@code final} so subclasses cannot alter the phase order.
	 */
	public final void run() {
		long currentTime;

		System.out.println("========================================");
		System.out.println("  TEST: " + getTestName());
		System.out.println("========================================");

		// Phase 1: Setup environment
		currentTime = System.nanoTime();
		setupEnvironment();
		timings.put("SetupEnvironment", System.nanoTime() - currentTime);
		visualizer.renderEnvironment(env, start, goal, null, "Initial Environment");

		// Phase 2: Create & initialize algorithm
		currentTime = System.nanoTime();
		pathFinder = createPathFinder();
		pathFinder.initialize(env, start, goal);
		timings.put("Initialize", System.nanoTime() - currentTime);

		// Phase 3: Compute initial path
		currentTime = System.nanoTime();
		pathFinder.computePath();
		timings.put("ComputePath", System.nanoTime() - currentTime);
		visualizer.renderEnvironment(env, start, goal,
				pathFinder.getPath().toList(), "Initial Path");

		// Phase 4: Dynamic re-planning scenario
		currentTime = System.nanoTime();
		runDynamicScenario();
		timings.put("DynamicScenario", System.nanoTime() - currentTime);

		// Phase 5: Print timing summary
		printTimingSummary();
	}

	/** Utility for subclasses to record additional timing entries. */
	protected void recordTiming(String label, long nanos) {
		timings.put(label, nanos);
	}

	private void printTimingSummary() {
		System.out.println("\n--- Timing Summary [" + getTestName() + "] ---");
		long total = 0;
		for (Map.Entry<String, Long> e : timings.entrySet()) {
			double ms = e.getValue() / 1_000_000.0;
			System.out.printf("  %-25s %10.3f ms%n", e.getKey(), ms);
			total += e.getValue();
		}
		System.out.printf("  %-25s %10.3f ms%n", "TOTAL", total / 1_000_000.0);
		System.out.println("-------------------------------------------\n");
	}
}
