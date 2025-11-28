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
     * 新增：本次用餐的整体评价。
     * 前端可以只依赖这个对象来展示「这顿饭整体如何」+「风险等级」+「采用了哪种解释模式」。
     */
    @JsonProperty("overallEvaluation")
    private OverallEvaluation overallEvaluation;

    /**
     * 结构化的 impact 分析结果。
     * - 当 strategy = LIGHT_TIPS 时，shortTerm/midTerm/longTerm 可以为空，只需要一个简短建议文本；
     * - 当 strategy = FULL_RISK_ANALYSIS 时，要求三段描述都填充。
     */
    @JsonProperty("impact")
    private Impact impact;

    /**
     * 新的整体评价结构。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallEvaluation {

        /**
         * 第一次 AI 对整体是否营养均衡的判断（等价于 data.isBalanced，单独再放一份便于前端使用）。
         */
        private Boolean aiIsBalanced;

        /**
         * 后端根据 NutritionTag + ImpactDecisionService 决定的整体风险等级。
         * 例如：NONE / MILD / MODERATE / HIGH。
         */
        private com.flowservice.service.ImpactDecisionService.NutritionRiskLevel riskLevel;

        /**
         * 本次对用户展示 impact 时采用的策略：
         * - NONE：仅展示整体评价和一些通用 tips；
         * - LIGHT_TIPS：整体较健康，只给轻微提醒；
         * - FULL_RISK_ANALYSIS：存在明显风险，给出短中长期详细分析。
         */
        private com.flowservice.service.ImpactDecisionService.ImpactStrategy impactStrategy;

        /**
         * 可选：整体评分（0~100），由 ImpactDecisionService 决定。
         * 前端可用来画环形进度条或打分控件。
         */
        private Integer overallScore;

        /**
         * 标签级别的总结，用于展示「小贴士」或「风险点列表」。
         */
        private List<TagSummary> tagSummaries;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TagSummary {
            /**
             * 对应后端的 NutritionTag 枚举。
             */
            private NutritionTag tag;

            /**
             * 每个标签自身的严重程度，可复用 NutritionRiskLevel，
             * 或者只用简单的 "LOW" / "MEDIUM" / "HIGH" 字符串。
             */
            private com.flowservice.service.ImpactDecisionService.NutritionRiskLevel severity;

            /**
             * 给前端展示用的中文名，例如「钠略偏高」「纤维偏低」。
             */
            private String displayName;

            /**
             * 对该标签的简短解释或建议，一般 1~2 句话。
             */
            private String description;
        }
    }

    /**
     * 新版 impact 分析结构。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Impact {

        /**
         * 对整段 impact 的总览性描述，前端可以优先展示这一段。
         */
        private String primaryText;

        private String shortTerm;
        private String midTerm;
        private String longTerm;

        /**
         * 结构化 riskTags，直接对接后端 NutritionTag，便于前端打标。
         */
        private List<NutritionTag> riskTags;
    }
}
