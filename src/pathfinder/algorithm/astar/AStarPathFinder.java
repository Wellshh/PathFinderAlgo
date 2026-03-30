package pathfinder.algorithm.astar;

import pathfinder.api.IPathFinder;
import pathfinder.api.PathContainer;
import pathfinder.api.PathContainer.O1PathContainer;
import pathfinder.model.Environment;
import pathfinder.model.Point;

public class AStarPathFinder<P extends Point> implements IPathFinder<P> {
  /* The map */
  private Environment<P> env;
  /* The planned path */
  private PathContainer<P> path = new O1PathContainer<>();


  @Override
  public PathContainer<P> getPath() {
    return path;
  }

  /** Setter of path */
  public void setPath(PathContainer<P> path) {
    this.path = path;
  }

  @Override
  public void initialize(Environment<P> env, P start, P goal) {
    // TODO Auto-generated method stub
  }

  @Override
  public P getNextWaypoint(P current) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void computePath() {
    // TODO Auto-generated method stub
  }
}
