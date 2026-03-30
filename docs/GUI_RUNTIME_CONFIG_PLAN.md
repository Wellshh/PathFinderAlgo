# 运行时可配置GUI与算法Battle行动计划

## 宏观目录大纲

✅ 1. 算法集成与工厂层 (Algorithm Integration & Factory Layer)
✅ 2. 状态管理与配置层 (State Management & Configuration Layer)
✅ 3. UI布局与组件层 (UI Layout & Component Layer)
✅ 4. 执行与Battle引擎层 (Execution & Battle Engine Layer)

---

### ✅ 1. 算法集成与工厂层 (Algorithm Integration & Factory Layer)
**目标:** 设计一个通用的算法注册与实例化机制。
**为什么要做:** 当前的 `MultiAlgoDemo` 硬编码了 `DStarLitePathFinder`。为了支持用户在运行时动态选择不同的算法（如 A*, D* Lite, Dijkstra 等），我们需要一个抽象的工厂模式来按需创建算法实例，保证系统的“通用性”和“可扩展性”。
**具体步骤:**
- 创建 `AlgorithmType` 枚举或注册表，用于列出所有可用算法。
- 实现 `PathFinderFactory`，根据用户选择动态返回 `IPathFinder` 的具体实现。
- 确保所有算法实现统一的性能监控接口（如记录 compute time, 扩展节点数等）。

### ✅ 2. 状态管理与配置层 (State Management & Configuration Layer)
**目标:** 扩展现有的 `SimulationConfigModel`，支持多算法选择和Battle模式配置。
**为什么要做:** UI组件不应直接操作底层引擎，而是通过配置模型进行双向绑定。扩展模型可以统一管理用户的选择（如选了哪两个算法进行对比），实现UI与逻辑的解耦。
**具体步骤:**
- 在 `SimulationConfigModel` 中增加 `selectedAlgorithmA` 和 `selectedAlgorithmB` 属性。
- 增加 `battleModeEnabled` 属性，用于切换单算法演示与多算法对决模式。
- 增加用于存储和对比计算时间（Compute Time）、路径长度（Path Length）等统计数据的可观察属性。

### ✅ 3. UI布局与组件层 (UI Layout & Component Layer)
**目标:** 在界面上提供算法选择下拉框、Battle模式开关以及性能对比面板。
**为什么要做:** 为用户提供直观的交互入口，使其能在运行时自由配置和对比算法，而无需修改代码。
**具体步骤:**
- 扩展 `ControlPanel`，增加 `ComboBox` 用于选择算法A和算法B。
- 增加一个 `ToggleButton` 或 `CheckBox` 用于开启/关闭 Battle 模式。
- 在UI底部或侧边栏新增一个 `StatisticsPanel`，实时显示算法A与算法B的执行时间（ms）、遍历节点数等对比数据。

### ✅ 4. 执行与Battle引擎层 (Execution & Battle Engine Layer)
**目标:** 改造现有的 `SimulationController` 或 `MultiAlgoDemo` 逻辑，支持动态加载算法并收集性能指标。
**为什么要做:** 现有的逻辑是静态初始化的。我们需要引擎能够在用户切换算法或点击“Start Battle”时，重置环境并同时/顺序执行选定的算法，精确统计耗时，从而实现真正的“对比算法battle”。
**具体步骤:**
- 修改初始化逻辑，使其监听 `SimulationConfigModel` 中的算法变化，动态调用 `PathFinderFactory` 重新生成 `AlgorithmSlot`。
- 在 `computePath()` 前后增加高精度计时器（如 `System.nanoTime()`），收集计算时间。
- 将统计结果回传给状态管理层，触发UI更新。

---
**总结:** 本文档通过分层设计，将算法工厂、状态管理、UI组件与执行引擎解耦，旨在构建一个高扩展性的运行时可视化与算法对决平台。