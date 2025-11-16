package com.flowservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 食物营养分析响应 DTO
 * 对应 AI 返回的食物识别和营养评估结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodAnalysisResponse {

    /**
     * 识别出的食物列表（中文）
     */
    @JsonProperty("foodItems")
    private List<String> foodItems;

    /**
     * 识别确定程度（0-1之间的小数，1表示完全确定）
     */
    @JsonProperty("confidence")
    private Double confidence;

    /**
     * 营养是否均衡
     */
    @JsonProperty("isBalanced")
    private Boolean isBalanced;

    /**
     * 营养评价概括（20字以内）
     */
    @JsonProperty("nutritionSummary")
    private String nutritionSummary;
}
