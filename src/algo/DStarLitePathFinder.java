package algo;

import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import objects.Coordinate;
import objects.Environment;
import objects.impl.DstarLiteNode;
import util.UtilityFunc;

public class DStarLitePathFinder implements BasePathFinder {
	private Environment env;
	/** An "open list" to store the "inconsistent" nodes*/
	private PriorityQueue<DstarLiteNode> openList;
	/** Sparsely store the explored nodes to save space*/
	private HashMap<Coordinate, DstarLiteNode> sparseMap;
	/** Assist priority queue for lazy removal of outdated nodes:
	 * `PriorityQueue.remove()` in java costs O(n) time in average,
	 * instead of removing the outdated node, we insert the same node
	 * with updated key value and later poll() the old nodes with the 
	 * help of the hashmap.
	 * */
	private HashMap<DstarLiteNode, KeyRecord> openHash;
	
	/** 
	 * Core states of robot. 
	* */
	private Coordinate startPos;
	private Coordinate goalPos;
	/** For k_m: the heuristic difference*/
	private Coordinate lastPos;
	
	/** Key modifier introduced in second version of D* Lite Algorithm,
	 * as per [S. Koenig, 2002].
	 * */
	private double k_m = 0.0;
	
	/** For double type comparison*/
	private static final double EPISILON = 1e-5;
	
	/** Internal class for quick node snapshot*/
	private static class KeyRecord {
		double k_top, k_bot;
		KeyRecord(double k_top, double k_bot){ this.k_top = k_top; this.k_bot = k_bot;}
		boolean matches(DstarLiteNode node) {
			return Math.abs(this.k_top - node.getKtop()) <= EPISILON &&
					Math.abs(this.k_bot - node.getKbot()) <= EPISILON;
		}
	}
	
	public DStarLitePathFinder(Environment env) {
		this.env = env;
		this.openList = new PriorityQueue<DstarLiteNode>((node1, node2) -> node1.compareTo(node2));
		this.sparseMap = new HashMap<>();
		this.openHash = new HashMap<>();
		
	}

	@Override
	public void initialize(Coordinate start, Coordinate goal) {
		this.startPos = start;
		this.goalPos = goal;
		this.lastPos = start;
		this.k_m = 0;
		
		openList.clear();
		sparseMap.clear();
		openHash.clear();
		
		DstarLiteNode goalNode = new DstarLiteNode(goal);
		goalNode.rhs = 0;
		
		sparseMap.put(goal, goalNode);
		
		calculateKey(goalNode);
		openHash.put(goalNode, new KeyRecord(goalNode.getKtop(), goalNode.getKbot()));
		openList.add(goalNode);
		
		
	}


	@Override
	public List<Coordinate> computePath() {
		
		return null;
	}
	
	/**
	 * Safely return a node to ensure each coordinates on map corresponds to 
	 * **ONE AND ONLY** node instance.
	 */
	private DstarLiteNode getOrCreateNode(Coordinate pos) {
		return sparseMap.computeIfAbsent(pos, k -> {
			DstarLiteNode newNode = new DstarLiteNode(k);
			
			// initial value of unexplored node has heuristic cost
			newNode.g = newNode.rhs = env.heuristic(goalPos, pos);
			return newNode;
		});
	}
	
	/** Get the node on map*/
	private DstarLiteNode getNodeOnMap (Coordinate pos) throws NullPointerException {
		DstarLiteNode node = sparseMap.get(pos);
		if (node == null) { 
			throw new NullPointerException("[Critical] Cannot find node on map!");
		}
		else { return node; }
	}
	
	/** Calkey(s) as per in [S.Koenig, 2002]:
	 * calculate k_top and k_bot.
	 * */
	private void calculateKey(DstarLiteNode node) {
		double min_val = Math.min(node.g, node.rhs);
		// Inroduced in D* Lite V2:
		// --- Originally we are supposed to subtract k_top by k_m 
		// --- everytime the robot moves but we intentionally "leave" it to avoid scanning
		// --- through the whole openList. So everytime a newly updated node is added,
		// --- we simply add k_m so that the relative orders in priority queue won't change.
		node.setKtop(min_val + env.heuristic(node.pos, startPos) + k_m);
		node.setKbot(min_val);
	}
	
	/** "Relax" the current node. 
	 * This function would only be called when:
	 * 		1. when g(s) of its successor s is changed.
	 * 		2. when edge costs changed, i.e. an new obstacle is encountered by the robot.
	 * As per [S. Koenig, 2002]
	 */
	private void updateVertex(DstarLiteNode node) {
		// the neighboring nodes were added into sparseMap before updateVertex
		Coordinate curPos = node.pos;
		if (curPos != goalPos) {
			List<Coordinate> succs = env.getNeighbors(curPos);
			double newRHS = Double.POSITIVE_INFINITY;
			for (Coordinate succ: succs) {
				DstarLiteNode succNode = getNodeOnMap(succ);
				double newRHSCandidate = env.getTraversalCost(succ, curPos) + succNode.g;
				newRHS = Math.min(newRHSCandidate, newRHS);
			}
			if (!UtilityFunc.isClose(newRHS, node.rhs)) setRHS(node, newRHS);
			
			// instead of remove the outdated node in PQ, we add the update one in and check later
			// if (u in U) U.Remove(u), as per [S. Koenig, 2002], takes O(n) time
			if (!UtilityFunc.isClose(node.rhs, node.g)) insert(node);
		}
	}
	
	
	// --------- Adapted from https://github.com/Wellshh/DStarLiteJava/blob/master/DStarLite.java,  -------- 
	// @author daniel beard
	
		/**
		 * Back propagate from goal to start, update cost functions along the way.
		 * As per [S. Koenig, 2002] - ComputeShortestPath() except for two main modifications:
		 * 		1. We stop planning after a number of steps, 'maxsteps' we do this
		 * 		because this algorithm can plan forever if the start is surrounded by obstacles;
		 * 		2. We lazily remove states from the open list so we never have to iterate through it.
		 */
		private void computeShortestPath() {
			DstarLiteNode top_node_pq = openList.peek();
			while (top_node_pq.)
		}

	
		
		/**
		 * Sets the rhs value for node.
		 */
		private void setRHS(DstarLiteNode node, double rhs) {
			add2map(node);
			DstarLiteNode tmp = getNodeOnMap(node.pos);
			tmp.rhs = rhs;
		}	
		
		/**
		 * Checks if a node is in the map, if not it adds it in.
		 */
		private void add2map(DstarLiteNode node) {
			getOrCreateNode(node.pos);
		}
		
		/**
		 * Insert the node into PQ and hashmap.
		 */
		private void insert(DstarLiteNode node) {
			calculateKey(node);
			
			// store the new key-value pair into openHash for later check
			// since sparseMap ensures uniqueness of node, put() will override the old one
			KeyRecord kr = new KeyRecord(node.getKtop(), node.getKbot());
			openHash.put(node, kr);
			
			// copy a new object and insert into PQ
			// IF NOT COPY, the priority key(ktop/kbot) may be modified outside
			DstarLiteNode copied = new DstarLiteNode(node);
			openList.add(copied);
		}
			
	// -----------------------

}
