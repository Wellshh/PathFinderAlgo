✅ **1. 场景与需求分析 (Scenario & Requirements Analysis)**
➡️ **2. 核心架构与分层设计 (Core Architecture & Layered Design)**
⏳ **3. 接口定义与职责说明 (Interface Definitions & Responsibilities)**
⏳ **4. 算法与环境的适配机制 (Adaptation Mechanism)**

**当前任务目标：** 输出一份基于面向对象设计（OOD）的通用动态路径规划接口设计文档（.md格式）。
**为什么要做这件事情：** 之前你质疑了为什么需要 `IEnvironment`，但正如你现在意识到的，如果未来有多种算法（如 A*, D* Lite, LPA*, RRT* 等）都需要接入 Repast Simphony，且都需要感知环境变化，如果每个算法都直接和 Repast 的具体类（如 `Grid` 或 `ContinuousSpace`）强耦合，代码将完全失去复用性。我们需要通过 OOD 的**依赖倒置原则（DIP）**和**策略模式（Strategy Pattern）**，定义一套标准的“图结构”与“动态规划”契约，让 Agent、环境、算法三者彻底解耦。

*(注：你之前认为 D* Lite 自带环境所以不需要抽象，这是一个常见的架构误区。算法内部维护的 `cellHash` 只是算法的**计算缓存/状态**，而不是**真实世界的拓扑**。真实世界的拓扑必须被抽象出来，才能喂给不同的算法。)*

以下是为你设计的接口文档：

***

# 通用动态路径规划接口设计文档 (OOD)

## 1. 架构分层思想 (Architecture Layers)

为了实现“通用性”和“可扩展性”，我们将系统分为三个正交的层级：
1. **拓扑层 (Topology Layer)**：只负责描述世界是什么样子的（节点、边、连通性、基础代价）。
2. **算法层 (Algorithm Layer)**：只负责根据拓扑层的数据，计算出最优路径，并响应拓扑的变化。
3. **代理层 (Agent Layer)**：负责在真实/模拟环境中移动、感知未知障碍物，并将变化通知给算法层。

## 2. 接口定义 (Interface Definitions)

所有接口均采用泛型 `<N>` (Node/State)，代表空间中的一个位置节点。这保证了算法不仅能用于 2D Grid，也能用于 3D 空间或拓扑路网。

### 2.1 拓扑层：`IGraph<N>`
**职责：** 为所有路径规划算法提供统一的环境视图。无论是 Repast 的 Grid 还是其他物理引擎，只需实现此接口，任何算法都能直接读取。

```java
/**
 * Represents the abstract topology of the environment.
 * Algorithms use this to explore the map without knowing the underlying physics engine.
 */
public interface IGraph<N> {
    
    /**
     * Gets all traversable neighbors for a given node.
     */
    List<N> getNeighbors(N node);
    
    /**
     * Gets the actual transition cost between two adjacent nodes.
     * Returns Double.POSITIVE_INFINITY if the edge is blocked.
     */
    double getCost(N from, N to);
    
    /**
     * Calculates the heuristic distance between two nodes.
     */
    double getHeuristic(N a, N b);
}
```

### 2.2 算法层：`IPathPlanner<N>` 与 `IDynamicPathPlanner<N>`
**职责：** 抽象路径规划算法。我们将静态算法（如 A*，Dijkstra）和动态算法（如 D* Lite, LPA*）区分开来，符合**接口隔离原则（ISP）**。

```java
/**
 * Base interface for all path planning algorithms (Static & Dynamic).
 */
public interface IPathPlanner<N> {
    
    /**
     * Initializes the planner with the environment graph, start, and goal nodes.
     */
    void initialize(IGraph<N> graph, N start, N goal);
    
    /**
     * Computes or retrieves the next optimal node to move to from the current node.
     * Returns null if no path exists.
     */
    N getNextWaypoint(N current);
    
    /**
     * Computes the full path from current to goal.
     */
    List<N> getFullPath(N current);
}

/**
 * Interface specifically for algorithms that support dynamic environment changes (e.g., D* Lite).
 */
public interface IDynamicPathPlanner<N> extends IPathPlanner<N> {
    
    /**
     * Notifies the algorithm that the cost of an edge has changed due to agent perception.
     * The algorithm should update its internal state efficiently.
     */
    void notifyEdgeCostChange(N u, N v, double newCost);
    
    /**
     * Dynamically updates the goal node without fully re-initializing the algorithm.
     */
    void updateGoal(N newGoal);
}
```

### 2.3 代理感知层：`IEnvironmentSensor<N>`
**职责：** 挂载在 Agent 上，负责将 Repast 环境中的物理变化（如突然出现的障碍物）翻译为图结构中的边代价变化。

```java
/**
 * Represents the sensor of an agent that detects changes in the environment.
 */
public interface IEnvironmentSensor<N> {
    
    /**
     * Scans the surroundings from the current position.
     * Returns a list of edge cost updates that differ from the agent's previous knowledge.
     */
    List<EdgeUpdate<N>> scanEnvironment(N currentPosition);
}

/**
 * Data Transfer Object for edge cost changes.
 */
class EdgeUpdate<N> {
    public final N from;
    public final N to;
    public final double newCost;
    
    // Constructor omitted for brevity
}
```

## 3. 运行机制与适配示例 (Workflow & Adaptation)

### 3.1 D* Lite 算法的实现方式
当 `DStarLitePathFinder` 实现 `IDynamicPathPlanner<N>` 时：
* `initialize()`: 读取 `IGraph`，初始化 `rhs` 和 `g` 值，计算初始路径。
* `notifyEdgeCostChange()`: 触发 D* Lite 核心的 `UpdateVertex(u)` 逻辑，将受影响的节点加入优先队列 `U`。
* `getNextWaypoint()`: 执行 `ComputeShortestPath()` 后，根据梯度下降返回下一步。

### 3.2 传统 A* 算法的实现方式 (作为对比)
如果未来你需要接入 A* 算法，它也可以实现 `IDynamicPathPlanner<N>`，但内部策略不同：
* `notifyEdgeCostChange()`: A* 不支持局部更新，所以它在这里仅仅是清空内部缓存。
* `getNextWaypoint()`: 发现缓存失效，重新调用完整的 A* 搜索全图。
*(这就体现了 OOD 的多态性：Agent 调用的接口完全一致，但底层算法的应对策略各不相同。)*

### 3.3 Agent 的主循环 (Main Loop)
在 Repast 的 `step()` 方法中，Agent 的行为逻辑被彻底统一：
1. `sensor.scanEnvironment(currentPos)` 获取视野内变化的边。
2. 遍历变化，调用 `planner.notifyEdgeCostChange(u, v, cost)`。
3. `planner.getNextWaypoint(currentPos)` 获取下一步。
4. Agent 在 Repast 物理空间中移动到该点。

***

**一句话总结：通过抽象出 `IGraph`、`IDynamicPathPlanner` 和 `IEnvironmentSensor`，我们将拓扑数据、规划算法与物理感知彻底解耦，使得任何 Agent 都能在任何环境中无缝切换不同的路径规划算法。**

请确认此 OOD 设计文档是否满足你对“通用性”和“可扩展性”的要求？如果确认，我们将进入下一步：在当前工程中创建这些接口文件。