package com.flowservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Upload 接口响应 DTO
 * 包含同步返回的食物分析结果和异步任务 ID 映射
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    /**
     * 同步返回的食物分析结果
     */
    @JsonProperty("analysisResult")
    private FoodAnalysisResponse analysisResult;

    /**
     * 异步任务 ID 映射
     * Key: 任务类型代码（如 "glucoseTrend", "eatingOrder"）
     * Value: 任务 ID（UUID）
     * 
     * 前端可以根据 key 知道每个任务对应什么功能，按需轮询
     */
    @JsonProperty("asyncTasks")
    private Map<String, String> asyncTasks;

    /**
     * 用餐记录 ID（用于关联异步任务结果）
     */
    @JsonProperty("mealRecordId")
    private Long mealRecordId;
}
