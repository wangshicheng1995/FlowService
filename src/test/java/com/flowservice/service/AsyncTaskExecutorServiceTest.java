package com.flowservice.service;

import com.flowservice.model.AsyncTaskInfo;
import com.flowservice.model.AsyncTaskInfo.TaskStatus;
import com.flowservice.model.AsyncTaskInfo.TaskType;
import com.flowservice.model.FoodAnalysisResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AsyncTaskExecutorService 单元测试
 * 测试异步任务执行服务的功能
 */
@DisplayName("异步任务执行服务测试")
@ExtendWith(MockitoExtension.class)
class AsyncTaskExecutorServiceTest {

    private AsyncTaskExecutorService executorService;
    private AsyncTaskStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new AsyncTaskStorageService();
        executorService = new AsyncTaskExecutorService(storageService);
    }

    @Test
    @DisplayName("启动异步任务 - 应创建所有预定义类型的任务")
    void startAsyncTasks_shouldCreateAllTaskTypes() {
        // Given
        FoodAnalysisResponse analysisResult = createMockAnalysisResult();
        String userId = "test-user-001";
        Long mealRecordId = 123L;

        // When
        Map<String, String> taskMap = executorService.startAsyncTasks(analysisResult, userId, mealRecordId);

        // Then
        assertNotNull(taskMap);
        assertEquals(2, taskMap.size());
        assertTrue(taskMap.containsKey("glucoseTrend"));
        assertTrue(taskMap.containsKey("eatingOrder"));

        // 验证任务已被创建
        String glucoseTrendTaskId = taskMap.get("glucoseTrend");
        String eatingOrderTaskId = taskMap.get("eatingOrder");

        assertTrue(storageService.getTask(glucoseTrendTaskId).isPresent());
        assertTrue(storageService.getTask(eatingOrderTaskId).isPresent());
    }

    @Test
    @DisplayName("任务 ID 映射 - 应使用正确的任务类型代码作为 Key")
    void startAsyncTasks_shouldUseCorrectTaskTypeCodes() {
        // Given
        FoodAnalysisResponse analysisResult = createMockAnalysisResult();

        // When
        Map<String, String> taskMap = executorService.startAsyncTasks(analysisResult, "user", 1L);

        // Then
        // 验证 Key 是任务类型代码
        assertEquals(TaskType.GLUCOSE_TREND.getCode(), "glucoseTrend");
        assertEquals(TaskType.EATING_ORDER.getCode(), "eatingOrder");

        assertTrue(taskMap.containsKey(TaskType.GLUCOSE_TREND.getCode()));
        assertTrue(taskMap.containsKey(TaskType.EATING_ORDER.getCode()));
    }

    @Test
    @DisplayName("任务状态 - 任务存在且状态有效")
    void startAsyncTasks_tasksShouldHaveValidStatus() {
        // Given
        FoodAnalysisResponse analysisResult = createMockAnalysisResult();

        // When
        Map<String, String> taskMap = executorService.startAsyncTasks(analysisResult, "user", 1L);

        // Then
        for (String taskId : taskMap.values()) {
            AsyncTaskInfo task = storageService.getTask(taskId).orElseThrow();
            // 在单线程测试环境中，@Async 不生效，任务会同步执行并完成
            // 因此只验证任务存在且状态是有效的（PENDING, RUNNING, 或 COMPLETED）
            assertNotNull(task.getStatus());
            assertTrue(
                    task.getStatus() == TaskStatus.PENDING ||
                            task.getStatus() == TaskStatus.RUNNING ||
                            task.getStatus() == TaskStatus.COMPLETED,
                    "Task status should be PENDING, RUNNING, or COMPLETED");
        }
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

        // 创建营养信息
        FoodAnalysisResponse.Nutrition nutrition = new FoodAnalysisResponse.Nutrition();
        nutrition.setEnergyKcal(650);
        nutrition.setProteinG(35);
        nutrition.setFatG(38);
        nutrition.setCarbG(45);
        response.setNutrition(nutrition);

        return response;
    }
}
