package pathfinder.testenv;

import pathfinder.testenv.scenario.dstarlite.DStarLiteGridTest;

/**
 * Entry point for running all path-finder algorithm tests.
 */
public class TestRunner {

	public static void main(String[] args) {
		new DStarLiteGridTest().run();
		// future: new AStarGridTest().run();
	}
}
