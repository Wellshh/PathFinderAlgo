package pathfinder.algorithm.dstarlite;

import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import pathfinder.api.IDynamicPathFinder;
import pathfinder.api.PathContainer;
import pathfinder.api.PathContainer.O1PathContainer;
import pathfinder.model.EdgeUpdate;
import pathfinder.model.Environment;
import pathfinder.model.Point;
import pathfinder.model.node.DStarLiteNode;
import pathfinder.util.UtilityFunc;

public class DStarLitePathFinder<P extends Point> implements IDynamicPathFinder<P> {
  /* The map */
  private Environment<P> env;
  /* The planned path */
  private O1PathContainer<P> path = new O1PathContainer<>();

  /** An "open list" to store the "inconsistent" nodes */
  private PriorityQueue<DStarLiteNode<P>> openList;

  /** Sparsely store the explored nodes to save space */
  private HashMap<P, DStarLiteNode<P>> sparseMap;

  /**
   * Assist priority queue for lazy removal of outdated nodes: `PriorityQueue.remove()` in java
   * costs O(n) time in average, instead of removing the outdated node, we insert the same node with
   * updated key value and later poll() the old nodes with the help of the hashmap.
   */
  private HashMap<DStarLiteNode<P>, KeyRecord> openHash;

  /** Core states of robot. */
  private P startPos;

  private P goalPos;

  /** For k_m: the heuristic difference */
  private P lastPos;

  /** Key modifier introduced in second version of D* Lite Algorithm, as per [S. Koenig, 2002]. */
  private double k_m = 0.0;

  /** For double type comparison */
  private static final double EPISILON = 1e-5;

  /** Stopping counts to avoid infinite loop */
  private int maxSteps;

  /** Internal class for quick node snapshot */
  private static class KeyRecord {
    double k_top, k_bot;

    KeyRecord(double k_top, double k_bot) {
      this.k_top = k_top;
      this.k_bot = k_bot;
    }

    boolean matches(DStarLiteNode<?> node) {
      return Math.abs(this.k_top - node.getKtop()) <= EPISILON
          && Math.abs(this.k_bot - node.getKbot()) <= EPISILON;
    }
  }

  /**
   * Internal enum class for return value of ComputeShortestPath - SUCCESS: - PQ_EMPTY: -
   * MAX_STEPS_REACHED: - EARLY_EXIT:
   */
  private enum ComputeReturn {
    SUCCESS,
    PQ_EMPTY,
    MAX_STEPS_REACHED,
    EARLY_EXIT,
  }

  public DStarLitePathFinder(int maxSteps) {
    this.openList = new PriorityQueue<DStarLiteNode<P>>((node1, node2) -> node1.compareTo(node2));
    this.sparseMap = new HashMap<>();
    this.openHash = new HashMap<>();
    this.maxSteps = maxSteps;
  }

  /** Default Constructor */
  public DStarLitePathFinder() {
    this(80000);
  }

  /**
   * Advances the algorithm's start position to the robot's current location. Must be called before
   * {@link #updateAllEdgeCosts} so that k_m is adjusted correctly (D* Lite V2, [S. Koenig, 2002]
   * line {20}).
   */
  public void setStart(P newStart) {
    this.startPos = newStart;
    getOrCreateNode(newStart);
  }

  /** Setter of path */
  public void setPath(O1PathContainer<P> path) {
    this.path = path;
  }

  /** Getter of path container */
  @Override
  public PathContainer<P> getPath() {
    return path;
  }

  @Override
  public void initialize(Environment<P> env, P start, P goal) {
    this.env = env;
    this.startPos = start;
    this.goalPos = goal;
    this.lastPos = start;
    this.k_m = 0;

    openList.clear();
    sparseMap.clear();
    openHash.clear();

    DStarLiteNode<P> goalNode = new DStarLiteNode<>(goal);
    DStarLiteNode<P> startNode = new DStarLiteNode<>(start);
    goalNode.rhs = 0;

    sparseMap.put(goal, goalNode);
    // initialize all cost functions of start node to heuristic focus
    add2map(startNode);

    calculateKey(goalNode);
    openHash.put(goalNode, new KeyRecord(goalNode.getKtop(), goalNode.getKbot()));
    openList.add(goalNode);
  }

  /* Compute the shortest path from start to goal */
  @Override
  public void computePath() {
    ComputeReturn result = computeShortestPath();
    switch (result) {
      case EARLY_EXIT:
      case PQ_EMPTY:
      case SUCCESS:
        System.out.printf("Path found -- compute return: %s \n", result);
        break;
      case MAX_STEPS_REACHED:
        System.out.printf("Max steps reached -- compute return: %s \n", result);
        setPath(new O1PathContainer<>()); // empty path
    }

    path = buildPathAlongGradient();
    setPath(path);
    if (path.isEmpty()) {
      System.out.println("No path found during buildPathAlongGradient!\n");
    }
  }

  /** Get the next waypoint from the path */
  @Override
  public P getNextWaypoint(P current) {
    if (path.isEmpty()) return null;
    return path.next(current);
  }

  /**
   * Batch update: adjust k_m once per sensing cycle, then propagate each edge change. As per [S.
   * Koenig, 2002] - the main loop's edge-scanning phase.
   */
  @Override
  public void updateAllEdgeCosts(List<EdgeUpdate<P>> edgeUpdates) {
    if (edgeUpdates.isEmpty()) return;

    k_m += env.heuristic(lastPos, startPos);
    lastPos = startPos;

    for (EdgeUpdate<P> eu : edgeUpdates) {
      notifyEdgeCostChange(eu.from, eu.to, eu.newCost);
    }
  }

  /**
   * Single edge update: write the new cost into the environment, then propagate the inconsistency
   * through updateVertex.
   */
  @Override
  public void notifyEdgeCostChange(P from, P to, double newCost) {
    env.setTraversalCost(from, to, newCost);

    DStarLiteNode<P> fromNode = getOrCreateNode(from);
    // put to node on map for successors update
    getOrCreateNode(to);
    updateVertex(fromNode);
  }

  /**
   * Safely return a node to ensure each coordinate on map corresponds to **ONE AND ONLY** node
   * instance.
   */
  private DStarLiteNode<P> getOrCreateNode(P pos) {
    return sparseMap.computeIfAbsent(pos, k -> new DStarLiteNode<>(k));
  }

  /** Get the node on map, this should be used a single reference of truth for nodes fetching */
  private DStarLiteNode<P> getNodeOnMap(P pos) throws NullPointerException {
    DStarLiteNode<P> node = sparseMap.get(pos);
    if (node == null) {
      throw new NullPointerException("[Critical] Cannot find node on map!");
    } else {
      return node;
    }
  }

  /** Calkey(s) as per in [S.Koenig, 2002]: calculate k_top and k_bot. */
  private DStarLiteNode<P> calculateKey(DStarLiteNode<P> node) {
    double min_val = Math.min(node.g, node.rhs);
    // Inroduced in D* Lite V2:
    // --- Originally we are supposed to subtract k_top by k_m
    // --- everytime the robot moves but we intentionally "leave" it to avoid
    // scanning
    // --- through the whole openList. So everytime a newly updated node is added,
    // --- we simply add k_m so that the relative orders in priority queue won't
    // change.
    node.setKtop(min_val + env.heuristic(node.pos, startPos) + k_m);
    node.setKbot(min_val);

    return node;
  }

  /**
   * Relax the node at the given position. Always operates on the canonical node stored in sparseMap
   * so that the consistency check (rhs vs g) reflects the true state. As per [S. Koenig, 2002].
   */
  private void updateVertex(DStarLiteNode<P> node) {
    DStarLiteNode<P> canonical = getOrCreateNode(node.pos);
    P curPos = canonical.pos;
    if (!curPos.equals(goalPos)) {
      List<P> succs = env.getSuccessors(curPos);
      double newRHS = Double.POSITIVE_INFINITY;
      for (P succ : succs) {
        DStarLiteNode<P> succNode = getOrCreateNode(succ);
        double candidate = env.getTraversalCost(succ, curPos) + succNode.g;
        newRHS = Math.min(candidate, newRHS);
      }
      canonical.rhs = newRHS;

      // Lazy-insert instead of remove+re-insert:
      // if (u in U) U.Remove(u), as per [S. Koenig, 2002], takes O(n) time
      if (!UtilityFunc.isClose(canonical.rhs, canonical.g)) insert(canonical);
    }
  }

  /**
   * Check if a node is up-to-date in PQ or not by querying if it's in openHash.
   *
   * @return the unique original node or null
   */
  private DStarLiteNode<P> isUp2Date(DStarLiteNode<P> node) {
    // fetch the unique node on map
    DStarLiteNode<P> originalNode = getNodeOnMap(node.pos);
    KeyRecord latestKey = openHash.get(originalNode);

    if (latestKey != null && latestKey.matches(node)) {
      openHash.remove(originalNode);
      return originalNode;
    }
    return null;
  }

  // --------- Adapted from
  // https://github.com/Wellshh/DStarLiteJava/blob/master/DStarLite.java,
  // --------
  // @author daniel beard

  /**
   * Back propagate from goal to start, update cost functions along the way. As per [S. Koenig,
   * 2002] - ComputeShortestPath() except for two main modifications: 1. We stop planning after a
   * number of steps, 'maxsteps' we do this because this algorithm can plan forever if the start is
   * surrounded by obstacles; 2. We lazily remove states from the open list so we never have to
   * iterate through it.
   */
  private ComputeReturn computeShortestPath() {
    if (openList.isEmpty()) return ComputeReturn.PQ_EMPTY;

    DStarLiteNode<P> startNode = getNodeOnMap(startPos);
    int loop_cnt = 0;

    /**
     * Three looping conditions: 1. list is not empty 2. topkeys in PQ is lower than that of start
     * node: the current might not be optimal 3. start node is "inconsistent": new edge cost updated
     */
    while (!openList.isEmpty()) {
      calculateKey(startNode);
      DStarLiteNode<P> topPQ = openList.peek();
      boolean startConsistent = UtilityFunc.isClose(startNode.g, startNode.rhs);
      if (!topPQ.lt(startNode) && startConsistent) break;

      if (loop_cnt++ > maxSteps) {
        System.out.println("Maxsteps is reached during computeShortestPath!");
        return ComputeReturn.MAX_STEPS_REACHED;
      }

      // Lazy removal: keep popping until an up-to-date node is found
      DStarLiteNode<P> up2date_node;
      while (true) {
        if (openList.isEmpty()) return ComputeReturn.PQ_EMPTY;
        up2date_node = isUp2Date(openList.poll());
        if (up2date_node != null) break;
      }

      // k_old comparison (D* Lite v2: detects key changes from robot movement)
      DStarLiteNode<P> k_old = new DStarLiteNode<>(up2date_node);
      if (k_old.lt(calculateKey(up2date_node))) {
        insert(up2date_node);
      } else if (up2date_node.g > up2date_node.rhs) {
        // Overconsistent: shorter path found
        setG(up2date_node, up2date_node.rhs);
        for (P pred : env.getPredecessors(up2date_node.pos)) {
          updateVertex(getOrCreateNode(pred));
        }
      } else {
        // Underconsistent: path got longer due to edge cost increase
        setG(up2date_node, Double.POSITIVE_INFINITY);
        for (P pred : env.getPredecessors(up2date_node.pos)) {
          updateVertex(getOrCreateNode(pred));
        }
        updateVertex(up2date_node);
      }
    }

    return ComputeReturn.SUCCESS;
  }

  /** Sets the g value for node. */
  private void setG(DStarLiteNode<P> node, double g) {
    add2map(node);
    DStarLiteNode<P> tmp = getNodeOnMap(node.pos);
    tmp.g = g;
  }

  /** Checks if a node is in the map, if not it adds it in. */
  private void add2map(DStarLiteNode<P> node) {
    getOrCreateNode(node.pos);
  }

  /** Insert the node into PQ and hashmap. */
  private void insert(DStarLiteNode<P> node) {
    calculateKey(node);

    // store the new key-value pair into openHash for later check
    // since sparseMap ensures uniqueness of node, put() will override the old one
    KeyRecord kr = new KeyRecord(node.getKtop(), node.getKbot());
    openHash.put(node, kr);

    // copy a new object and insert into PQ
    // IF NOT COPY, the priority key(ktop/kbot) may be modified outside
    DStarLiteNode<P> copied = new DStarLiteNode<>(node);
    openList.add(copied);
  }

  /**
   * Dynamically relocate the goal without a full re-initialization.
   *
   * <p>Adapted from the reference approach (daniel beard's D* Lite): The reference stores traversal
   * costs inside cellHash, so it must save non-default-cost cells before clearing, then re-add
   * them. In our architecture, costs live in {@link Environment} and survive the reset, so the
   * save-and-restore step is unnecessary.
   */
  @Override
  public void updateGoal(P newGoal) {
    sparseMap.clear();
    openHash.clear();
    openList.clear();
    k_m = 0;

    this.goalPos = newGoal;
    this.lastPos = startPos;

    DStarLiteNode<P> goalNode = new DStarLiteNode<>(newGoal);
    goalNode.rhs = 0;
    sparseMap.put(newGoal, goalNode);

    add2map(new DStarLiteNode<>(startPos));

    calculateKey(goalNode);
    openHash.put(goalNode, new KeyRecord(goalNode.getKtop(), goalNode.getKbot()));
    openList.add(goalNode);

    path.clear();
  }

  /** Construct the shortest path along the gradient of g value */
  private O1PathContainer<P> buildPathAlongGradient() {
    O1PathContainer<P> path = new O1PathContainer<>();
    P current = startPos;
    path.add(current);
    int safetyCounter = 0;
    while (!current.equals(goalPos)) {
      if (safetyCounter++ > maxSteps) {
        System.out.println("Max steps reached during buildPathAlongGradient!");
        path.clear();
        return path;
      }
      double minG = Double.POSITIVE_INFINITY;
      P next = null;
      for (P successor : env.getSuccessors(current)) {
        DStarLiteNode<P> successorNode = sparseMap.get(successor);
        if (successorNode != null && successorNode.g < minG) {
          minG = successorNode.g;
          next = successor;
        }
      }
      if (next == null || minG == Double.POSITIVE_INFINITY) {
        path.clear();
        return path;
      }
      current = next;
      path.add(current);
    }
    return path;
  }

  // -----------------------

}
