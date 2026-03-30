package pathfinder.algorithm.astar;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import pathfinder.api.IPathFinder;
import pathfinder.model.Environment;
import pathfinder.model.Point;
import pathfinder.model.node.AStarNode;
import pathfinder.util.data_structure.BinaryHeap;
import pathfinder.util.data_structure.IndexedBH;

/**
 * A* algorithm implementation adapted from elsewhere, the original code and
 * very nice explanation
 * is available here. Few modifications for performance gain: 1. use (indexed)
 * binary heap for
 * openlist for - O(logn) add - O(logn) update with the help of a hashmap 2. use
 * bitmap for
 * closedlist for O(1) operations（for small map only）
 *
 * @see <a href=
 *      "http://www.cokeandcode.com/pathfinding">http://www.cokeandcode.com/pathfinding</a>
 * @author Junyan Bai
 */
public class AStarPathFinder<P extends Point> implements IPathFinder<P> {
  /* The map */
  private Environment<P> env;
  /* The planned path */
  private List<P> path;

  /* The set of nodes that we do not yet consider fully searched */
  private IndexedBH<AStarNode<P>, Double> openList;
  /* Index map for binary heap */
  private HashMap<AStarNode<P>, Integer> bhIndexMap;

  /* The set of nodes that have been searched through */
  private BitSet closedList;

  /* The maximum depth of search we're willing to accept before giving up */
  private int maxSearchDepth;

  /** Sparesely store the explored nodes to save space */
  private HashMap<P, AStarNode<P>> sparseMap;

  /* The next node id to assign, unique in each instance */
  private int nextNodeId = 0;

  /** Core states of robot. */
  private P startPos;

  private P goalPos;

  /*
   * Generate bitIndex by flattening point coordinates.
   * future: implement this.
   */
  private int bitIndex(AStarNode<P> node) {
    return node.id;
  }

  /**
   * Safely return a node to ensure each coordinate on map corresponds to **ONE
   * AND ONLY** node
   * instance.
   */
  private AStarNode<P> getOrCreateNode(P pos) {
    return sparseMap.computeIfAbsent(pos, k -> new AStarNode<>(k, nextNodeId++));
  }

  /* Setter of maxSearchDepth */
  public void setMaxSearchDepth(int maxSeachDepth) {
    this.maxSearchDepth = maxSeachDepth;
  }

  /* Getter of maxSearchDepth */
  public int getMaxSearchDepth() {
    return this.maxSearchDepth;
  }

  public AStarPathFinder(int maxSearchDepth) {
    this.maxSearchDepth = maxSearchDepth;
    // min-heap
    this.openList = new IndexedBH<AStarNode<P>, Double>(
        new BinaryHeap<AStarNode<P>>((a1, a2) -> a1.compareTo(a2)));
    this.closedList = new BitSet(env.getSpaceCost());
  }

  /* Default constructor */
  public AStarPathFinder() {
    this.maxSearchDepth = 10000;
  }

  public List<P> getPath() {
    return path;
  }

  /** Setter of path */
  public void setPath(List<P> path) {
    this.path = path;
  }

  @Override
  public void initialize(Environment<P> env, P start, P goal) {
    this.env = env;
    this.startPos = start;
    this.goalPos = goal;

    openList.clear();
    bhIndexMap.clear();
    closedList.clear();
    sparseMap.clear();

    AStarNode<P> startNode = getOrCreateNode(start);
    AStarNode<P> goalNode = getOrCreateNode(goal);
    startNode.g = 0;
    goalNode.g = 0;

    // put start node into open list
    openList.insert(startNode);

    // put goal node into map
    sparseMap.put(goal, goalNode);
  }

  @Override
  public P getNextWaypoint(P current) {
    int index = path.indexOf(current);
    if (index == -1) {
      return null;
    }
    return path.get(index + 1);
  }

  @Override
  public void computePath() {
    int curDepth = 0;
    // while we haven't found the goal and haven't exceeded our max search depth
    while (!openList.isEmpty() && curDepth < maxSearchDepth) {
      AStarNode<P> curNode = openList.poll();
      // mark the current node as explored
      closedList.set(bitIndex(curNode));

      // scan through all its successors and update cost
      List<P> succs = env.getSuccessors(curNode.pos);
      for (P succ : succs) {
        // ensure node is on map
        getOrCreateNode(succ);
        AStarNode<P> neighborNode = getOrCreateNode(succ);

        // update cost functions and heuristics
        double newG = curNode.g + env.getTraversalCost(curNode.pos, succ);
        double h = env.heuristic(succ, goalPos);
        double f = newG + h;

        // the new path is not getting better, skip
        if (newG >= neighborNode.g) {
          continue;
        }

        // path is indeed better

        // if in closed list, put it back into open list for re-evaluation
        if (closedList.get(bitIndex(neighborNode))) {
          closedList.clear(bitIndex(neighborNode));
        }

        // update and put renewed node into map
        neighborNode.g = newG;
        neighborNode.h = h;
        neighborNode.parent = curNode;
        maxSearchDepth = Math.max(maxSearchDepth, curDepth + 1);
        sparseMap.put(succ, neighborNode);

        // if in open list, update priority key
        // else insert
        if (openList.contains(neighborNode)) {
          openList.update(neighborNode, f);
        } else
          openList.insert(neighborNode);
      }
    }

    AStarNode<P> goalNode = getOrCreateNode(goalPos);
    if (goalNode.parent == null)
      System.out.println("[A*]: No path found");
    else
      buildPath();
  }

  /** Build optimal path by backpropating through the parent pointer */
  private void buildPath() {
    AStarNode<P> curNode = getOrCreateNode(goalPos);
    while (curNode.parent != null) {
      path.add(curNode.pos);
      curNode = curNode.parent;
    }
    path.add(startPos);
    Collections.reverse(path);
  }
}
