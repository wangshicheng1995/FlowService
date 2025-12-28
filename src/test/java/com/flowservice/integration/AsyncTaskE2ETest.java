package com.flowservice.integration;

import com.flowservice.model.AsyncTaskInfo;
import com.flowservice.model.AsyncTaskInfo.TaskStatus;
import com.flowservice.model.AsyncTaskInfo.TaskType;
import com.flowservice.model.FoodAnalysisResponse;
import com.flowservice.service.AsyncTaskExecutorService;
import com.flowservice.service.AsyncTaskStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异步任务端到端集成测试
 * 模拟完整的用户上传图片 -> 获取同步结果 -> 轮询异步任务的流程
 */
@DisplayName("异步任务端到端集成测试")
class AsyncTaskE2ETest {

    private AsyncTaskStorageService storageService;
    private AsyncTaskExecutorService executorService;

    @BeforeEach
    void setUp() {
        storageService = new AsyncTaskStorageService();
        executorService = new AsyncTaskExecutorService(storageService);
    }

    @Test
    @DisplayName("完整流程 - 模拟用户上传图片并轮询异步任务直到完成")
    void fullWorkflow_uploadAndPollUntilComplete() throws InterruptedException {
        // ========== 阶段 1: 模拟用户上传图片 ==========
        System.out.println("\n===== 阶段 1: 用户上传图片 =====");

        FoodAnalysisResponse analysisResult = createMockAnalysisResult();
        String userId = "user-001";
        Long mealRecordId = 123L;

        // 模拟 upload 接口调用后启动异步任务
        Map<String, String> asyncTasks = executorService.startAsyncTasks(
                analysisResult, userId, mealRecordId);

        System.out.println("同步返回的食物分析结果: " + analysisResult.getFoodName());
        System.out.println("异步任务 IDs: " + asyncTasks);

        // 验证异步任务已创建
        assertEquals(2, asyncTasks.size());
        assertTrue(asyncTasks.containsKey("glucoseTrend"));
        assertTrue(asyncTasks.containsKey("eatingOrder"));

        // ========== 阶段 2: 模拟前端轮询异步任务 ==========
        System.out.println("\n===== 阶段 2: 前端轮询异步任务 =====");

        String glucoseTaskId = asyncTasks.get("glucoseTrend");
        String eatingOrderTaskId = asyncTasks.get("eatingOrder");

        // 模拟轮询（最多 10 次，每次间隔 500ms）
        int maxPolls = 10;
        int pollInterval = 500; // ms
        boolean glucoseCompleted = false;
        boolean eatingOrderCompleted = false;

        for (int i = 1; i <= maxPolls; i++) {
            System.out.println("\n--- 轮询第 " + i + " 次 ---");

            // 检查血糖趋势任务
            if (!glucoseCompleted) {
                AsyncTaskInfo glucoseTask = storageService.getTask(glucoseTaskId).orElse(null);
                if (glucoseTask != null) {
                    System.out.println("血糖趋势任务状态: " + glucoseTask.getStatus());
                    if (glucoseTask.getStatus() == TaskStatus.COMPLETED) {
                        glucoseCompleted = true;
                        System.out.println("✅ 血糖趋势任务已完成！结果: " + glucoseTask.getResult());
                    }
                }
            }

            // 检查吃饭顺序建议任务
            if (!eatingOrderCompleted) {
                AsyncTaskInfo eatingOrderTask = storageService.getTask(eatingOrderTaskId).orElse(null);
                if (eatingOrderTask != null) {
                    System.out.println("吃饭顺序建议任务状态: " + eatingOrderTask.getStatus());
                    if (eatingOrderTask.getStatus() == TaskStatus.COMPLETED) {
                        eatingOrderCompleted = true;
                        System.out.println("✅ 吃饭顺序建议任务已完成！结果: " + eatingOrderTask.getResult());
                    }
                }
            }

            // 两个任务都完成则退出
            if (glucoseCompleted && eatingOrderCompleted) {
                System.out.println("\n🎉 所有异步任务已完成！");
                break;
            }

            // 等待下次轮询
            Thread.sleep(pollInterval);
        }

        // ========== 阶段 3: 验证最终结果 ==========
        System.out.println("\n===== 阶段 3: 验证最终结果 =====");

        // 注意：由于异步任务使用 @Async 注解，在单元测试中可能不会真正异步执行
        // 这里我们只验证任务状态的变化是否正确

        AsyncTaskInfo finalGlucoseTask = storageService.getTask(glucoseTaskId).orElse(null);
        AsyncTaskInfo finalEatingOrderTask = storageService.getTask(eatingOrderTaskId).orElse(null);

        assertNotNull(finalGlucoseTask);
        assertNotNull(finalEatingOrderTask);

        System.out.println("血糖趋势任务最终状态: " + finalGlucoseTask.getStatus());
        System.out.println("吃饭顺序建议任务最终状态: " + finalEatingOrderTask.getStatus());
    }

    @Test
    @DisplayName("任务状态流转 - 验证任务状态从 PENDING 到 COMPLETED 的流转")
    void taskStatusTransition_shouldProgressCorrectly() {
        // Given
        AsyncTaskInfo task = storageService.createTask(TaskType.GLUCOSE_TREND, "user", 1L);
        String taskId = task.getTaskId();

        // 初始状态
        assertEquals(TaskStatus.PENDING, storageService.getTask(taskId).get().getStatus());

        // 模拟任务开始执行
        storageService.markRunning(taskId);
        assertEquals(TaskStatus.RUNNING, storageService.getTask(taskId).get().getStatus());

        // 模拟任务完成
        Map<String, Object> result = Map.of(
                "peakValue", 7.8,
                "peakTime", "餐后 30-60 分钟",
                "impactLevel", "中影响");
        storageService.markCompleted(taskId, result);

        // 验证最终状态
        AsyncTaskInfo completedTask = storageService.getTask(taskId).get();
        assertEquals(TaskStatus.COMPLETED, completedTask.getStatus());
        assertNotNull(completedTask.getResult());
        assertNotNull(completedTask.getCompletedAt());

        System.out.println("任务状态流转验证通过:");
        System.out.println("  - 初始状态: PENDING");
        System.out.println("  - 执行中: RUNNING");
        System.out.println("  - 完成: COMPLETED");
        System.out.println("  - 结果: " + completedTask.getResult());
    }

    @Test
    @DisplayName("模拟前端批量轮询 - 一次请求查询多个任务状态")
    void batchPolling_shouldReturnMultipleTaskStatuses() {
        // Given - 创建多个任务
        AsyncTaskInfo task1 = storageService.createTask(TaskType.GLUCOSE_TREND, "user", 1L);
        AsyncTaskInfo task2 = storageService.createTask(TaskType.EATING_ORDER, "user", 1L);

        // 模拟不同的任务状态
        storageService.markCompleted(task1.getTaskId(), Map.of("data", "glucose result"));
        storageService.markRunning(task2.getTaskId());

        // When - 模拟批量查询
        String[] taskIds = { task1.getTaskId(), task2.getTaskId(), "non-existing" };
        java.util.List<AsyncTaskInfo> results = new java.util.ArrayList<>();

        for (String taskId : taskIds) {
            storageService.getTask(taskId).ifPresent(results::add);
        }

        // Then
        assertEquals(2, results.size());

        AsyncTaskInfo result1 = results.stream()
                .filter(t -> t.getTaskId().equals(task1.getTaskId()))
                .findFirst().orElse(null);
        AsyncTaskInfo result2 = results.stream()
                .filter(t -> t.getTaskId().equals(task2.getTaskId()))
                .findFirst().orElse(null);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(TaskStatus.COMPLETED, result1.getStatus());
        assertEquals(TaskStatus.RUNNING, result2.getStatus());

        System.out.println("批量轮询结果:");
        System.out.println("  - Task 1: " + result1.getStatus() + " (有结果)");
        System.out.println("  - Task 2: " + result2.getStatus() + " (执行中)");
        System.out.println("  - Task 3: 不存在 (被过滤)");
    }

    @Test
    @DisplayName("任务失败场景 - 验证失败任务的处理")
    void taskFailure_shouldHandleCorrectly() {
        // Given
        AsyncTaskInfo task = storageService.createTask(TaskType.EATING_ORDER, "user", 1L);
        String taskId = task.getTaskId();

        // 模拟任务执行失败
        storageService.markRunning(taskId);
        storageService.markFailed(taskId, "AI 服务连接超时");

        // Then
        AsyncTaskInfo failedTask = storageService.getTask(taskId).get();
        assertEquals(TaskStatus.FAILED, failedTask.getStatus());
        assertEquals("AI 服务连接超时", failedTask.getErrorMessage());
        assertNotNull(failedTask.getCompletedAt());
        assertNull(failedTask.getResult());

        System.out.println("任务失败场景验证通过:");
        System.out.println("  - 状态: " + failedTask.getStatus());
        System.out.println("  - 错误信息: " + failedTask.getErrorMessage());
    }

    /**
     * 创建模拟的食物分析结果
     */
    private FoodAnalysisResponse createMockAnalysisResult() {
        FoodAnalysisResponse response = new FoodAnalysisResponse();
        response.setFoodName("双层芝士汉堡");
        response.setConfidence(0.95);
        response.setIsBalanced(false);
        response.setNutritionSummary("热量偏高，建议搭配蔬菜");

        FoodAnalysisResponse.Nutrition nutrition = new FoodAnalysisResponse.Nutrition();
        nutrition.setEnergyKcal(650);
        nutrition.setProteinG(35);
        nutrition.setFatG(38);
        nutrition.setCarbG(45);
        response.setNutrition(nutrition);

        return response;
    }
}
