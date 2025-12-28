package com.flowservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 每日热量响应 DTO
 * 用于柱状图展示每日热量数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyCalorieResponse {

    /**
     * 日期
     */
    @JsonProperty("date")
    private LocalDate date;

    /**
     * 当日总热量（千卡）
     */
    @JsonProperty("calories")
    private Integer calories;

    /**
     * 当日用餐次数
     */
    @JsonProperty("mealCount")
    private Integer mealCount;
}
