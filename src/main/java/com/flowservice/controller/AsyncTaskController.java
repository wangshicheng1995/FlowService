package com.flowservice.controller;

import com.flowservice.model.ApiResponse;
import com.flowservice.model.AsyncTaskInfo;
import com.flowservice.service.AsyncTaskStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 异步任务控制器
 * 提供任务状态查询接口，供前端轮询
 */
@Slf4j
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
@Tag(name = "异步任务", description = "异步任务状态查询接口，用于轮询任务执行状态和获取结果")
public class AsyncTaskController {

    private final AsyncTaskStorageService taskStorageService;

    /**
     * 查询任务状态
     *
     * @param taskId 任务 ID
     * @return 任务信息（包含状态和结果）
     */
    @Operation(summary = "查询任务状态", description = "根据任务 ID 查询异步任务的执行状态和结果。\n\n" +
            "任务状态说明：\n" +
            "- PENDING: 等待执行\n" +
            "- RUNNING: 执行中\n" +
            "- COMPLETED: 已完成（result 字段包含结果数据）\n" +
            "- FAILED: 执行失败（errorMessage 字段包含错误信息）\n\n" +
            "建议轮询间隔：1-2 秒")
    @GetMapping("/{taskId}")
    public ApiResponse<AsyncTaskInfo> getTaskStatus(
            @Parameter(description = "任务 ID（由 upload 接口返回）", required = true) @PathVariable String taskId) {

        log.debug("查询任务状态: taskId={}", taskId);

        Optional<AsyncTaskInfo> taskOptional = taskStorageService.getTask(taskId);

        if (taskOptional.isEmpty()) {
            log.warn("任务不存在: taskId={}", taskId);
            return ApiResponse.error(404, "任务不存在或已过期");
        }

        AsyncTaskInfo task = taskOptional.get();
        log.debug("任务状态: taskId={}, status={}", taskId, task.getStatus());

        return ApiResponse.success("查询成功", task);
    }

    /**
     * 批量查询任务状态
     *
     * @param taskIds 任务 ID 列表（逗号分隔）
     * @return 任务信息列表
     */
    @Operation(summary = "批量查询任务状态", description = "一次查询多个任务的状态，减少请求次数。\n" +
            "任务 ID 用逗号分隔，例如：?taskIds=id1,id2,id3")
    @GetMapping("/batch")
    public ApiResponse<java.util.List<AsyncTaskInfo>> getTaskStatusBatch(
            @Parameter(description = "任务 ID 列表（逗号分隔）", required = true) @RequestParam String taskIds) {

        String[] ids = taskIds.split(",");
        java.util.List<AsyncTaskInfo> tasks = new java.util.ArrayList<>();

        for (String id : ids) {
            String trimmedId = id.trim();
            if (!trimmedId.isEmpty()) {
                taskStorageService.getTask(trimmedId).ifPresent(tasks::add);
            }
        }

        log.debug("批量查询任务状态: 请求 {} 个, 返回 {} 个", ids.length, tasks.size());

        return ApiResponse.success("查询成功", tasks);
    }
}
