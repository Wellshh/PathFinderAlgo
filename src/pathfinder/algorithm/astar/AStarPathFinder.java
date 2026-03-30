package pathfinder.algorithm.astar;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import pathfinder.api.IPathFinder;
import pathfinder.api.PathContainer;
import pathfinder.api.PathContainer.O1PathContainer;
import pathfinder.model.Environment;
import pathfinder.model.Point;
import pathfinder.model.node.AStarNode;
import pathfinder.util.data_structure.BinaryHeap;
import pathfinder.util.data_structure.IndexedBH;

/**
 * A* algorithm implementation adapted from elsewhere, the original code and very nice explanation
 * is available here. Few modifications for performance gain: 1. use (indexed) binary heap for
 * openlist for - O(logn) add - O(logn) update with the help of a hashmap 2. use bitmap for
 * closedlist for O(1) operations（for small map only） 3. early termination when goal node is popped
 * from open list
 *
 * @see <a href= "http://www.cokeandcode.com/pathfinding">http://www.cokeandcode.com/pathfinding</a>
 * @author Junyan Bai
 */
public class AStarPathFinder<P extends Point> implements IPathFinder<P> {
  private Environment<P> env;
  private O1PathContainer<P> path = new O1PathContainer<>();

  private IndexedBH<AStarNode<P>, Double> openList;

  /** Bitmap closed list: O(1) check/set via node id */
  private BitSet closedList;

  private int maxSearchDepth;

  /** Sparsely store the explored nodes to save space */
  private HashMap<P, AStarNode<P>> sparseMap;

  private int nextNodeId = 0;

  /** Core states of robot. */
  private P startPos;

  private P goalPos;

  private int bitIndex(AStarNode<P> node) {
    return node.id;
  }

  /**
   * Safely return a node to ensure each coordinate on map corresponds to **ONE AND ONLY** node
   * instance.
   */
  private AStarNode<P> getOrCreateNode(P pos) {
    return sparseMap.computeIfAbsent(pos, k -> new AStarNode<>(k, nextNodeId++));
  }

  public void setMaxSearchDepth(int maxSearchDepth) {
    this.maxSearchDepth = maxSearchDepth;
  }

  public int getMaxSearchDepth() {
    return this.maxSearchDepth;
  }

  public AStarPathFinder(int maxSearchDepth) {
    this.maxSearchDepth = maxSearchDepth;
  }

  public AStarPathFinder() {
    this(10000);
  }

  @Override
  public PathContainer<P> getPath() {
    return path;
  }

  @Override
  public void initialize(Environment<P> env, P start, P goal) {
    this.env = env;
    this.startPos = start;
    this.goalPos = goal;
    this.nextNodeId = 0;

    this.openList =
        new IndexedBH<AStarNode<P>, Double>(
            new BinaryHeap<AStarNode<P>>((a1, a2) -> a1.compareTo(a2)));
    this.closedList = new BitSet(env.getSpaceCost());
    this.sparseMap = new HashMap<>();
    this.path = new O1PathContainer<>();

    AStarNode<P> startNode = getOrCreateNode(start);
    startNode.g = 0;
    startNode.h = env.heuristic(start, goal);

    openList.insert(startNode);
  }

  @Override
  public P getNextWaypoint(P current) {
    return path.next(current);
  }

  @Override
  public void computePath() {
    int steps = 0;
    while (!openList.isEmpty() && steps < maxSearchDepth) {
      AStarNode<P> curNode = openList.poll();

      // Early termination: goal reached (optimal under consistent heuristic)
      if (curNode.pos.equals(goalPos)) {
        buildPath();
        return;
      }

      closedList.set(bitIndex(curNode));
      steps++;

      for (P succ : env.getSuccessors(curNode.pos)) {
        AStarNode<P> neighborNode = getOrCreateNode(succ);

        // With a consistent heuristic, closed nodes are already optimal
        if (closedList.get(bitIndex(neighborNode))) {
          continue;
        }

        double newG = curNode.g + env.getTraversalCost(curNode.pos, succ);

        if (newG >= neighborNode.g) {
          continue;
        }

        neighborNode.g = newG;
        neighborNode.h = env.heuristic(succ, goalPos);
        neighborNode.parent = curNode;

        if (openList.contains(neighborNode)) {
          openList.update(neighborNode, neighborNode.getF());
        } else {
          openList.insert(neighborNode);
        }
      }
    }
    System.out.println("[A*]: No path found");
  }

  /** Build optimal path by backtracking through the parent pointer */
  private void buildPath() {
    path = new O1PathContainer<>();
    List<P> reversed = new ArrayList<>();
    AStarNode<P> curNode = getOrCreateNode(goalPos);
    while (curNode != null) {
      reversed.add(curNode.pos);
      curNode = curNode.parent;
    }
    Collections.reverse(reversed);
    for (P p : reversed) {
      path.add(p);
    }
  }
}
