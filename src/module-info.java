module PathFinderAlgo {
  requires transitive javafx.base;
  requires javafx.controls;
  requires javafx.graphics;

  exports pathfinder.api;
  exports pathfinder.engine;
  exports pathfinder.model;
  exports pathfinder.model.node;
  exports pathfinder.visualizer;
  exports pathfinder.visualizer.model;
  exports pathfinder.visualizer.adapter;
  exports pathfinder.visualizer.javafx;
  exports pathfinder.testenv.environment;
  exports pathfinder.testenv.sensor;
  exports pathfinder.testenv.visualizer;
  exports pathfinder.factory;
  exports pathfinder.algorithm.astar;
  exports pathfinder.algorithm.dstarlite;

  opens pathfinder.visualizer.javafx to
      javafx.graphics;
}
