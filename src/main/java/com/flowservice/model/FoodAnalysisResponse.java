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
     * 识别出的食物列表（包含名称、重量、烹饪方式）
     */
    @JsonProperty("foods")
    private List<FoodItem> foods;

    /**
     * 整体营养成分信息
     */
    @JsonProperty("nutrition")
    private Nutrition nutrition;

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

    /**
     * 食物项 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FoodItem {
        /**
         * 食物名称
         */
        @JsonProperty("name")
        private String name;

        /**
         * 食物重量（克）
         */
        @JsonProperty("amount_g")
        private Integer amountG;

        /**
         * 烹饪方式（可选）
         */
        @JsonProperty("cook")
        private String cook;
    }

    /**
     * 营养成分 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Nutrition {
        /**
         * 总热量（千卡）
         */
        @JsonProperty("energy_kcal")
        private Integer energyKcal;

        /**
         * 蛋白质（克）
         */
        @JsonProperty("protein_g")
        private Integer proteinG;

        /**
         * 总脂肪（克）
         */
        @JsonProperty("fat_g")
        private Integer fatG;

        /**
         * 碳水化合物（克）
         */
        @JsonProperty("carb_g")
        private Integer carbG;

        /**
         * 膳食纤维（克）
         */
        @JsonProperty("fiber_g")
        private Integer fiberG;

        /**
         * 钠（毫克）
         */
        @JsonProperty("sodium_mg")
        private Integer sodiumMg;

        /**
         * 糖（克）
         */
        @JsonProperty("sugar_g")
        private Integer sugarG;

        /**
         * 饱和脂肪（克）
         */
        @JsonProperty("sat_fat_g")
        private Double satFatG;
    }

    /**
     * 饮食影响分析结果
     */
    @JsonProperty("impact_analysis")
    private ImpactAnalysisResult impactAnalysis;
}
