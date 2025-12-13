package com.flowservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 热量统计响应 DTO
 * 返回用户在指定时间范围内的食物总热量统计信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalorieStatisticsResponse {

    /**
     * 用户 ID（支持 Apple ID 格式）
     */
    @JsonProperty("userId")
    private String userId;

    /**
     * 统计开始时间
     */
    @JsonProperty("startTime")
    private LocalDateTime startTime;

    /**
     * 统计结束时间
     */
    @JsonProperty("endTime")
    private LocalDateTime endTime;

    /**
     * 总热量（千卡）
     */
    @JsonProperty("totalCalories")
    private Integer totalCalories;

    /**
     * 用餐记录数量
     */
    @JsonProperty("mealCount")
    private Integer mealCount;

    /**
     * 平均每餐热量（千卡）
     */
    @JsonProperty("averageCaloriesPerMeal")
    private Double averageCaloriesPerMeal;
}
