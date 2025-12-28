package com.flowservice.service;

import com.flowservice.model.AsyncTaskInfo;
import com.flowservice.model.AsyncTaskInfo.TaskStatus;
import com.flowservice.model.AsyncTaskInfo.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsyncTaskStorageService 单元测试
 * 测试任务存储服务的核心功能
 */
@DisplayName("异步任务存储服务测试")
class AsyncTaskStorageServiceTest {

    private AsyncTaskStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new AsyncTaskStorageService();
    }

    @Test
    @DisplayName("创建任务 - 应返回带有正确初始状态的任务")
    void createTask_shouldReturnTaskWithCorrectInitialStatus() {
        // Given
        String userId = "test-user-001";
        Long recordId = 123L;
        TaskType taskType = TaskType.GLUCOSE_TREND;

        // When
        AsyncTaskInfo task = storageService.createTask(taskType, userId, recordId);

        // Then
        assertNotNull(task);
        assertNotNull(task.getTaskId());
        assertEquals(taskType, task.getTaskType());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals(userId, task.getUserId());
        assertEquals(recordId, task.getRecordId());
        assertNotNull(task.getCreatedAt());
        assertNull(task.getResult());
        assertNull(task.getCompletedAt());
    }

    @Test
    @DisplayName("获取任务 - 已存在的任务应能正确获取")
    void getTask_existingTask_shouldReturnTask() {
        // Given
        AsyncTaskInfo createdTask = storageService.createTask(
                TaskType.EATING_ORDER, "user-001", 100L);
        String taskId = createdTask.getTaskId();

        // When
        Optional<AsyncTaskInfo> result = storageService.getTask(taskId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(taskId, result.get().getTaskId());
        assertEquals(TaskType.EATING_ORDER, result.get().getTaskType());
    }

    @Test
    @DisplayName("获取任务 - 不存在的任务应返回空")
    void getTask_nonExistingTask_shouldReturnEmpty() {
        // When
        Optional<AsyncTaskInfo> result = storageService.getTask("non-existing-task-id");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("标记任务运行中 - 状态应更新为 RUNNING")
    void markRunning_shouldUpdateStatusToRunning() {
        // Given
        AsyncTaskInfo task = storageService.createTask(TaskType.GLUCOSE_TREND, "user", 1L);
        String taskId = task.getTaskId();

        // When
        storageService.markRunning(taskId);

        // Then
        AsyncTaskInfo updatedTask = storageService.getTask(taskId).orElseThrow();
        assertEquals(TaskStatus.RUNNING, updatedTask.getStatus());
    }

    @Test
    @DisplayName("标记任务完成 - 状态应更新为 COMPLETED 且包含结果")
    void markCompleted_shouldUpdateStatusAndSetResult() {
        // Given
        AsyncTaskInfo task = storageService.createTask(TaskType.GLUCOSE_TREND, "user", 1L);
        String taskId = task.getTaskId();
        Object result = java.util.Map.of("peakValue", 7.8, "peakTime", "30-60分钟");

        // When
        storageService.markCompleted(taskId, result);

        // Then
        AsyncTaskInfo updatedTask = storageService.getTask(taskId).orElseThrow();
        assertEquals(TaskStatus.COMPLETED, updatedTask.getStatus());
        assertNotNull(updatedTask.getResult());
        assertNotNull(updatedTask.getCompletedAt());
    }

    @Test
    @DisplayName("标记任务失败 - 状态应更新为 FAILED 且包含错误信息")
    void markFailed_shouldUpdateStatusAndSetErrorMessage() {
        // Given
        AsyncTaskInfo task = storageService.createTask(TaskType.EATING_ORDER, "user", 1L);
        String taskId = task.getTaskId();
        String errorMessage = "AI 服务调用超时";

        // When
        storageService.markFailed(taskId, errorMessage);

        // Then
        AsyncTaskInfo updatedTask = storageService.getTask(taskId).orElseThrow();
        assertEquals(TaskStatus.FAILED, updatedTask.getStatus());
        assertEquals(errorMessage, updatedTask.getErrorMessage());
        assertNotNull(updatedTask.getCompletedAt());
    }

    @Test
    @DisplayName("删除任务 - 删除后应无法获取")
    void removeTask_shouldRemoveTaskFromStorage() {
        // Given
        AsyncTaskInfo task = storageService.createTask(TaskType.HEALTH_SCORE, "user", 1L);
        String taskId = task.getTaskId();

        // When
        storageService.removeTask(taskId);

        // Then
        assertTrue(storageService.getTask(taskId).isEmpty());
    }

    @Test
    @DisplayName("任务计数 - 应正确统计存储的任务数量")
    void getTaskCount_shouldReturnCorrectCount() {
        // Given
        assertEquals(0, storageService.getTaskCount());

        // When
        storageService.createTask(TaskType.GLUCOSE_TREND, "user1", 1L);
        storageService.createTask(TaskType.EATING_ORDER, "user2", 2L);
        storageService.createTask(TaskType.HEALTH_SCORE, "user3", 3L);

        // Then
        assertEquals(3, storageService.getTaskCount());
    }

    @Test
    @DisplayName("完整生命周期 - 任务应经历完整的状态流转")
    void taskLifecycle_shouldProgressThroughAllStates() {
        // Given
        AsyncTaskInfo task = storageService.createTask(TaskType.GLUCOSE_TREND, "user", 1L);
        String taskId = task.getTaskId();

        // 初始状态应为 PENDING
        assertEquals(TaskStatus.PENDING, storageService.getTask(taskId).get().getStatus());

        // 标记为运行中
        storageService.markRunning(taskId);
        assertEquals(TaskStatus.RUNNING, storageService.getTask(taskId).get().getStatus());

        // 标记为完成
        storageService.markCompleted(taskId, "result data");
        AsyncTaskInfo completedTask = storageService.getTask(taskId).get();
        assertEquals(TaskStatus.COMPLETED, completedTask.getStatus());
        assertEquals("result data", completedTask.getResult());
    }
}
