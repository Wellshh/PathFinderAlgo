module PathFinderAlgo {
    requires javafx.controls;
    requires javafx.graphics;

    exports pathfinder.api;
    exports pathfinder.model;
    exports pathfinder.model.node;
    exports pathfinder.visualizer;
    exports pathfinder.visualizer.javafx;
    exports pathfinder.testenv.environment;
    exports pathfinder.testenv.sensor;
    exports pathfinder.testenv.visualizer;
    exports pathfinder.algorithm.dstarlite;

    opens pathfinder.visualizer.javafx to javafx.graphics;
}