package com.flowservice.service;

import com.flowservice.model.AsyncTaskInfo;
import com.flowservice.model.AsyncTaskInfo.TaskStatus;
import com.flowservice.model.AsyncTaskInfo.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步任务存储服务
 * MVP 阶段使用内存存储（ConcurrentHashMap）
 * 生产环境可替换为 Redis 实现
 */
@Slf4j
@Service
public class AsyncTaskStorageService {

    /**
     * 任务存储（内存）
     * Key: taskId
     * Value: AsyncTaskInfo
     */
    private final Map<String, AsyncTaskInfo> taskStorage = new ConcurrentHashMap<>();

    /**
     * 任务过期时间（小时）
     */
    private static final int TASK_EXPIRY_HOURS = 24;

    /**
     * 创建新任务
     *
     * @param taskType 任务类型
     * @param userId   用户 ID
     * @param recordId 关联记录 ID
     * @return 创建的任务信息
     */
    public AsyncTaskInfo createTask(TaskType taskType, String userId, Long recordId) {
        String taskId = UUID.randomUUID().toString();

        AsyncTaskInfo taskInfo = AsyncTaskInfo.builder()
                .taskId(taskId)
                .taskType(taskType)
                .status(TaskStatus.PENDING)
                .userId(userId)
                .recordId(recordId)
                .createdAt(LocalDateTime.now())
                .build();

        taskStorage.put(taskId, taskInfo);
        log.info("创建异步任务: taskId={}, type={}, userId={}", taskId, taskType.getCode(), userId);

        return taskInfo;
    }

    /**
     * 获取任务信息
     *
     * @param taskId 任务 ID
     * @return 任务信息（可能为空）
     */
    public Optional<AsyncTaskInfo> getTask(String taskId) {
        return Optional.ofNullable(taskStorage.get(taskId));
    }

    /**
     * 更新任务状态为运行中
     *
     * @param taskId 任务 ID
     */
    public void markRunning(String taskId) {
        AsyncTaskInfo task = taskStorage.get(taskId);
        if (task != null) {
            task.setStatus(TaskStatus.RUNNING);
            log.debug("任务状态更新为 RUNNING: taskId={}", taskId);
        }
    }

    /**
     * 标记任务完成并设置结果
     *
     * @param taskId 任务 ID
     * @param result 任务结果
     */
    public void markCompleted(String taskId, Object result) {
        AsyncTaskInfo task = taskStorage.get(taskId);
        if (task != null) {
            task.setStatus(TaskStatus.COMPLETED);
            task.setResult(result);
            task.setCompletedAt(LocalDateTime.now());
            log.info("任务完成: taskId={}, type={}", taskId, task.getTaskType().getCode());
        }
    }

    /**
     * 标记任务失败
     *
     * @param taskId       任务 ID
     * @param errorMessage 错误信息
     */
    public void markFailed(String taskId, String errorMessage) {
        AsyncTaskInfo task = taskStorage.get(taskId);
        if (task != null) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            task.setCompletedAt(LocalDateTime.now());
            log.error("任务失败: taskId={}, error={}", taskId, errorMessage);
        }
    }

    /**
     * 删除任务
     *
     * @param taskId 任务 ID
     */
    public void removeTask(String taskId) {
        taskStorage.remove(taskId);
        log.debug("任务已删除: taskId={}", taskId);
    }

    /**
     * 获取当前存储的任务数量
     */
    public int getTaskCount() {
        return taskStorage.size();
    }

    /**
     * 定时清理过期任务（每小时执行一次）
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredTasks() {
        LocalDateTime expiryTime = LocalDateTime.now().minusHours(TASK_EXPIRY_HOURS);
        int removedCount = 0;

        for (Map.Entry<String, AsyncTaskInfo> entry : taskStorage.entrySet()) {
            AsyncTaskInfo task = entry.getValue();
            if (task.getCreatedAt().isBefore(expiryTime)) {
                taskStorage.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("清理过期任务: 删除 {} 个任务, 剩余 {} 个", removedCount, taskStorage.size());
        }
    }
}
