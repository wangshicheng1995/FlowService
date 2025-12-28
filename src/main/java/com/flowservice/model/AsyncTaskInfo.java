package com.flowservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 异步任务信息 DTO
 * 用于存储和返回异步任务的状态和结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTaskInfo {

    /**
     * 任务唯一标识
     */
    @JsonProperty("taskId")
    private String taskId;

    /**
     * 任务类型
     */
    @JsonProperty("taskType")
    private TaskType taskType;

    /**
     * 任务状态
     */
    @JsonProperty("status")
    private TaskStatus status;

    /**
     * 任务结果数据（JSON 格式，完成后填充）
     */
    @JsonProperty("result")
    private Object result;

    /**
     * 错误信息（失败时填充）
     */
    @JsonProperty("errorMessage")
    private String errorMessage;

    /**
     * 任务创建时间
     */
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    /**
     * 任务完成时间
     */
    @JsonProperty("completedAt")
    private LocalDateTime completedAt;

    /**
     * 关联的用户 ID
     */
    @JsonProperty("userId")
    private String userId;

    /**
     * 关联的记录 ID（如 mealRecordId）
     */
    @JsonProperty("recordId")
    private Long recordId;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        /** 等待执行 */
        PENDING,
        /** 执行中 */
        RUNNING,
        /** 已完成 */
        COMPLETED,
        /** 执行失败 */
        FAILED,
        /** 已取消 */
        CANCELLED
    }

    /**
     * 任务类型枚举
     * 每种任务类型对应一个异步 AI 分析功能
     */
    public enum TaskType {
        /** 血糖趋势预测 */
        GLUCOSE_TREND("glucoseTrend", "血糖趋势预测"),

        /** 吃饭顺序建议 */
        EATING_ORDER("eatingOrder", "吃饭顺序建议"),

        /** 健康评分分析 */
        HEALTH_SCORE("healthScore", "健康评分分析");

        private final String code;
        private final String displayName;

        TaskType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
