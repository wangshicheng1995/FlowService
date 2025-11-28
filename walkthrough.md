# Walkthrough - 实现饮食影响决策层

我已在 `FlowService` 中实现了决策层，以解决“整体评价”与“风险标签”之间的冲突。

## 变更内容

### 1. 实现 `ImpactDecisionService`

创建了 `src/main/java/com/flowservice/service/ImpactDecisionService.java`，用于根据 `isBalanced`（是否均衡）和 `NutritionTag`（营养标签）决定影响分析策略。

- **策略 (Strategies)**: `NONE` (无), `LIGHT_TIPS` (轻提示), `FULL_RISK_ANALYSIS` (完整风险分析)。
- **风险等级 (Risk Levels)**: `NONE` (无), `MILD` (轻微), `MODERATE` (中等), `HIGH` (高)。
- **逻辑**:
    - 高/非常高风险标签 -> `FULL_RISK_ANALYSIS` + `HIGH` 风险。
    - 轻微风险标签 -> `LIGHT_TIPS` + `MILD`/`MODERATE` 风险。
    - 无标签 -> `NONE` 策略。

### 2. 更新 Prompt 模板

更新了 `src/main/java/com/flowservice/config/PromptConfig.java`，新增了两个 Prompt：
- `LIGHT_TIPS_PROMPT`: 语气温和，侧重于小的改进建议。
- `FULL_RISK_ANALYSIS_PROMPT`: 语气严肃但带有鼓励性，包含详细的短/中/长期分析。

### 3. 更新响应结构

更新了 `src/main/java/com/flowservice/model/FoodAnalysisResponse.java`，新增：
- `overallEvaluation`: 包含 `riskLevel` (风险等级), `impactStrategy` (影响策略) 等。
- `impact`: 包含结构化的影响分析 (`primaryText`, `shortTerm`, `midTerm`, `longTerm`, `riskTags`)。

### 4. 更新服务逻辑

- 修改了 `src/main/java/com/flowservice/service/ImpactAnalysisService.java`:
    - 注入了 `ImpactDecisionService`。
    - 更新了 `analyzeImpact` 方法，接收 `isBalanced` 和 `nutritionSummary` 参数。
    - 实现了调用 `ImpactDecisionService.decide` 并选择合适 Prompt 的逻辑。
    - 返回包含旧版和新版结果的 `FullImpactAnalysisResult`。

- 修改了 `src/main/java/com/flowservice/service/ImageProcessService.java`:
    - 更新了对 `analyzeImpact` 的调用。
    - 在响应中填充 `overallEvaluation` 和 `impact`。

## 验证

我使用 `mvn clean compile` 编译项目验证了更改。

- **最初问题**: 默认的 `mvn` 命令使用的是 Java 25，导致了 Lombok 兼容性错误 (`java.lang.ExceptionInInitializerError`)。
- **解决方案**: 我显式设置 `JAVA_HOME` 为 Java 17 路径 (`/Users/echo/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home`)。
- **结果**: **构建成功**。项目在新的更改下编译正确。

## 下一步

- 将更改部署到测试环境。
- 使用实际图片上传验证 API 响应。
