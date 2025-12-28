package com.flowservice.controller;

import com.flowservice.model.AsyncTaskInfo;
import com.flowservice.model.AsyncTaskInfo.TaskStatus;
import com.flowservice.model.AsyncTaskInfo.TaskType;
import com.flowservice.service.AsyncTaskStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AsyncTaskController 单元测试
 * 测试任务状态查询接口的业务逻辑
 */
@DisplayName("异步任务控制器测试")
class AsyncTaskControllerTest {

        private AsyncTaskController controller;
        private AsyncTaskStorageService taskStorageService;

        private AsyncTaskInfo sampleTask;

        @BeforeEach
        void setUp() {
                taskStorageService = mock(AsyncTaskStorageService.class);
                controller = new AsyncTaskController(taskStorageService);

                sampleTask = AsyncTaskInfo.builder()
                                .taskId("test-task-uuid-001")
                                .taskType(TaskType.GLUCOSE_TREND)
                                .status(TaskStatus.PENDING)
                                .userId("user-001")
                                .recordId(123L)
                                .createdAt(LocalDateTime.now())
                                .build();
        }

        @Test
        @DisplayName("查询任务状态 - 任务存在时应返回任务信息")
        void getTaskStatus_existingTask_shouldReturnTaskInfo() {
                // Given
                when(taskStorageService.getTask("test-task-uuid-001"))
                                .thenReturn(Optional.of(sampleTask));

                // When
                var response = controller.getTaskStatus("test-task-uuid-001");

                // Then
                assertEquals(200, response.getCode());
                assertEquals("查询成功", response.getMessage());
                assertNotNull(response.getData());
                assertEquals("test-task-uuid-001", response.getData().getTaskId());
                assertEquals(TaskType.GLUCOSE_TREND, response.getData().getTaskType());
                assertEquals(TaskStatus.PENDING, response.getData().getStatus());
        }

        @Test
        @DisplayName("查询任务状态 - 任务不存在时应返回 404 错误")
        void getTaskStatus_nonExistingTask_shouldReturn404() {
                // Given
                when(taskStorageService.getTask("non-existing-task-id"))
                                .thenReturn(Optional.empty());

                // When
                var response = controller.getTaskStatus("non-existing-task-id");

                // Then
                assertEquals(404, response.getCode());
                assertEquals("任务不存在或已过期", response.getMessage());
                assertNull(response.getData());
        }

        @Test
        @DisplayName("查询已完成任务 - 应返回包含结果的任务信息")
        void getTaskStatus_completedTask_shouldReturnResult() {
                // Given
                AsyncTaskInfo completedTask = AsyncTaskInfo.builder()
                                .taskId("completed-task-uuid")
                                .taskType(TaskType.GLUCOSE_TREND)
                                .status(TaskStatus.COMPLETED)
                                .userId("user-001")
                                .recordId(123L)
                                .createdAt(LocalDateTime.now().minusSeconds(5))
                                .completedAt(LocalDateTime.now())
                                .result(Map.of("peakValue", 7.8, "peakTime", "30-60分钟"))
                                .build();

                when(taskStorageService.getTask("completed-task-uuid"))
                                .thenReturn(Optional.of(completedTask));

                // When
                var response = controller.getTaskStatus("completed-task-uuid");

                // Then
                assertEquals(200, response.getCode());
                assertEquals(TaskStatus.COMPLETED, response.getData().getStatus());
                assertNotNull(response.getData().getResult());
        }

        @Test
        @DisplayName("查询失败任务 - 应返回包含错误信息的任务")
        void getTaskStatus_failedTask_shouldReturnErrorMessage() {
                // Given
                AsyncTaskInfo failedTask = AsyncTaskInfo.builder()
                                .taskId("failed-task-uuid")
                                .taskType(TaskType.EATING_ORDER)
                                .status(TaskStatus.FAILED)
                                .userId("user-001")
                                .recordId(123L)
                                .createdAt(LocalDateTime.now().minusSeconds(5))
                                .completedAt(LocalDateTime.now())
                                .errorMessage("AI 服务调用超时")
                                .build();

                when(taskStorageService.getTask("failed-task-uuid"))
                                .thenReturn(Optional.of(failedTask));

                // When
                var response = controller.getTaskStatus("failed-task-uuid");

                // Then
                assertEquals(200, response.getCode());
                assertEquals(TaskStatus.FAILED, response.getData().getStatus());
                assertEquals("AI 服务调用超时", response.getData().getErrorMessage());
        }

        @Test
        @DisplayName("批量查询任务 - 应返回多个任务信息")
        void getTaskStatusBatch_shouldReturnMultipleTasks() {
                // Given
                AsyncTaskInfo task1 = AsyncTaskInfo.builder()
                                .taskId("task-1")
                                .taskType(TaskType.GLUCOSE_TREND)
                                .status(TaskStatus.COMPLETED)
                                .createdAt(LocalDateTime.now())
                                .build();

                AsyncTaskInfo task2 = AsyncTaskInfo.builder()
                                .taskId("task-2")
                                .taskType(TaskType.EATING_ORDER)
                                .status(TaskStatus.RUNNING)
                                .createdAt(LocalDateTime.now())
                                .build();

                when(taskStorageService.getTask("task-1")).thenReturn(Optional.of(task1));
                when(taskStorageService.getTask("task-2")).thenReturn(Optional.of(task2));
                when(taskStorageService.getTask("task-3")).thenReturn(Optional.empty());

                // When
                var response = controller.getTaskStatusBatch("task-1,task-2,task-3");

                // Then
                assertEquals(200, response.getCode());
                assertEquals(2, response.getData().size());
                assertEquals("task-1", response.getData().get(0).getTaskId());
                assertEquals("task-2", response.getData().get(1).getTaskId());
        }
}
