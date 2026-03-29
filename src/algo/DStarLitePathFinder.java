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
	/** Stopping counts to avoid infinite loop */
	private int maxSteps;
	
	
	
	/** Internal class for quick node snapshot*/
	private static class KeyRecord {
		double k_top, k_bot;
		KeyRecord(double k_top, double k_bot){ this.k_top = k_top; this.k_bot = k_bot;}
		boolean matches(DstarLiteNode node) {
			return Math.abs(this.k_top - node.getKtop()) <= EPISILON &&
					Math.abs(this.k_bot - node.getKbot()) <= EPISILON;
		}
	}
	
	/** Internal enum class for return value of ComputeShortestPath
	 *  - SUCCESS:  
	 *  - FAIL: 
	 *  - MAX_STEPS_REACHED: 
	 *  - EARLY_EXIT: 
	 * */
	private enum ComputeReturn {
		SUCCESS,
		FAIL,
		MAX_STEPS_REACHED,
		EARLY_EXIT,
	}
	
	public DStarLitePathFinder(Environment env, int maxSteps) {
		this.env = env;
		this.openList = new PriorityQueue<DstarLiteNode>((node1, node2) -> node1.compareTo(node2));
		this.sparseMap = new HashMap<>();
		this.openHash = new HashMap<>();
		this.maxSteps = maxSteps;
	}
	
	/** Default Constructor*/
	public DStarLitePathFinder() {
		this.maxSteps = 80000;
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
		DstarLiteNode startNode = new DstarLiteNode(start);
		goalNode.rhs = 0;
		
		
		sparseMap.put(goal, goalNode);
		// initialize all cost functions of start nodoe to heuristic focus
		add2map(startNode);
		
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
	
	/** Get the node on map,
	 * this should be used a single reference of truth for nodes fetching
	 * */
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
	private DstarLiteNode calculateKey(DstarLiteNode node) {
		double min_val = Math.min(node.g, node.rhs);
		// Inroduced in D* Lite V2:
		// --- Originally we are supposed to subtract k_top by k_m 
		// --- everytime the robot moves but we intentionally "leave" it to avoid scanning
		// --- through the whole openList. So everytime a newly updated node is added,
		// --- we simply add k_m so that the relative orders in priority queue won't change.
		node.setKtop(min_val + env.heuristic(node.pos, startPos) + k_m);
		node.setKbot(min_val);
		
		return node;
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
			List<Coordinate> succs = env.getSuccessors(curPos);
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
	
	/** Poll valid node from PQ: 
	 * 	valid means it's an up-to-date node in openHash.
	 * 
	 */
	private DstarLiteNode pollValidNode(boolean canEarlyExit) {
		while (!openList.isEmpty()) {
			DstarLiteNode poppedSnapshot = openList.poll();
			
			// fetch the unique node on map
			DstarLiteNode originalNode = getNodeOnMap(poppedSnapshot.pos);
			KeyRecord latestKey = openHash.get(originalNode);
			
			// check if it's openHash (valid)
			if (latestKey != null && latestKey.matches(poppedSnapshot)) {
				openHash.remove(originalNode);
				return originalNode;
			}
		}
		
		return null;
	}
	
	/**
	 * Check if a node is up-to-date in PQ or not by querying if it's in openHash.
	 * @return the unique original node or null
	 */
	private DstarLiteNode isUp2Date(DstarLiteNode node) {
		// fetch the unique node on map
		DstarLiteNode originalNode = getNodeOnMap(node.pos);
		KeyRecord latestKey = openHash.get(originalNode);
		
		if (latestKey != null
				&& latestKey.matches(node)) {
			openHash.remove(originalNode);
			return originalNode;
		}
		return null;
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
		private ComputeReturn computeShortestPath() {
			if (openList.isEmpty()) return ComputeReturn.FAIL;
			
			DstarLiteNode top_node_pq = openList.peek();
			DstarLiteNode startNode = getNodeOnMap(startPos);
			
			int loop_cnt = 0;
			/** Three looping conditions:
			 * 		1. list is not empty
			 * 		2. topkeys in PQ is lower than that of start node: the current might not be optimal
			 * 		3. start node is "inconsistent": new edge cost updated
			 * 
			 */
			while (!openList.isEmpty()
					&& top_node_pq.lt(calculateKey(startNode))
					&& !UtilityFunc.isClose(startNode.g, startNode.rhs)) {
				
				if (loop_cnt ++ > maxSteps) {
					//TODO: add logging layer
					System.out.println("Maxsteps is reached during computeShortestPath!");
					return ComputeReturn.MAX_STEPS_REACHED;
				}
				
				boolean testCanEarlyExit = UtilityFunc.isClose(startNode.g, startNode.rhs);
				
				// lazy remove
				// keep popping until an up-to-date node is found
				DstarLiteNode up2date_node;
				while(true) {
					if (openList.isEmpty()) return ComputeReturn.FAIL;
					up2date_node = isUp2Date(openList.poll());
					if (up2date_node == null) continue;
					
					// early exit if loop stopping condition is met
					if (up2date_node.lt(startNode) && testCanEarlyExit) return ComputeReturn.EARLY_EXIT;
					break;
				}
				
				// as per [S.Koenig, 2002], k_old is introduced in D* Lite v2
				// used to check if robot moves && edge cost changes(map is updated)
				
				DstarLiteNode k_old = new DstarLiteNode(up2date_node);
				if(k_old.lt(calculateKey(up2date_node))) {
					// up2date_node is out of date :(
					insert(up2date_node);
				} else if (up2date_node.g > up2date_node.rhs) {
					// node is overconsistent (may find shorter path)
					setG(up2date_node, up2date_node.rhs);
					
					// update all predecessors
					List<Coordinate> preds = env.getPredecessors(up2date_node.pos);
					for (Coordinate pred: preds) {
						updateVertex(new DstarLiteNode(pred));
					}
				} else {
					// node is underconsistent (path becomes longer due to map updates)
					setG(up2date_node, Double.POSITIVE_INFINITY);
					List<Coordinate> preds = env.getPredecessors(up2date_node.pos);
					// update all predecessors + current node
					for (Coordinate pred: preds) {
						updateVertex(new DstarLiteNode(pred));
					}
					updateVertex(up2date_node);
				}
			}
			
			// normal return -> looping condition met
			return ComputeReturn.SUCCESS;
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
		 * Sets the g value for node.
		 */
		private void setG(DstarLiteNode node, double g) {
			add2map(node);
			DstarLiteNode tmp = getNodeOnMap(node.pos);
			tmp.g = g;
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
