package com.flowservice.service;

import com.flowservice.model.NutritionTag;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/**
 * 根据 AI 的整体评价 + HealthTagCalculator 计算出的 NutritionTag
 * 决定本次 impact 分析采用哪种策略（不分析 / 轻提示 / 完整风险分析），
 * 并给出整体 riskLevel 等信息。
 *
 * 注意：
 * 1. 这里是骨架代码，具体包名、工具类、日志等可由实际项目调整；
 * 2. NutritionTag 已经在项目里存在（如 VERY_HIGH_SODIUM / HIGH_SODIUM 等），
 * 这里直接复用；
 * 3. 这里的判断逻辑是一个“合理默认值”，可按产品体验继续微调。
 */
@Service
public class ImpactDecisionService {

    /**
     * impact 分析的模式：
     * - NONE：不做个性化风险分析，只展示整体评价 + 通用 tips；
     * - LIGHT_TIPS：整体评价为「大致健康」，只有轻微/中等风险时使用，语气温和，偏建议；
     * - FULL_RISK_ANALYSIS：存在明显或严重风险时使用，分短/中/长期描述。
     */
    public enum ImpactStrategy {
        NONE,
        LIGHT_TIPS,
        FULL_RISK_ANALYSIS
    }

    /**
     * 面向前端的整体风险等级，可以和 UI 上的颜色/文案绑定。
     */
    public enum NutritionRiskLevel {
        NONE, // 没什么值得特别提醒的风险
        MILD, // 有一些小问题，可以作为「小提示」
        MODERATE, // 需要注意的水平
        HIGH // 明显不健康，需要重点提示
    }

    /**
     * 上下文：决策时需要用到的关键信息。
     * 未来可以按需要继续往里加字段（例如用户疾病史、三高风险等）。
     */
    public static class MealHealthContext {
        /** 第一次 AI 分析返回的整体是否营养均衡（isBalanced）。 */
        private boolean aiBalanced;

        /** HealthTagCalculator.calcTags() 返回的所有营养标签。 */
        private Set<NutritionTag> nutritionTags = EnumSet.noneOf(NutritionTag.class);

        public MealHealthContext(boolean aiBalanced, Set<NutritionTag> nutritionTags) {
            this.aiBalanced = aiBalanced;
            this.nutritionTags = nutritionTags != null ? nutritionTags : EnumSet.noneOf(NutritionTag.class);
        }

        public boolean isAiBalanced() {
            return aiBalanced;
        }

        public Set<NutritionTag> getNutritionTags() {
            return nutritionTags;
        }
    }

    /**
     * 决策结果：供 Controller 或 Service 层直接使用，
     * 也可以映射到上传接口返回的 overallEvaluation 字段里。
     */
    public static class ImpactDecision {
        private ImpactStrategy strategy;
        private NutritionRiskLevel riskLevel;

        /**
         * 可选：整体评分（0~100），用于前端进度条/仪表盘表现。
         * 这里先用一个简单默认值（根据 riskLevel 映射），
         * 也可以交给 AI 或进一步算法生成。
         */
        private Integer overallScore;

        public ImpactDecision(ImpactStrategy strategy,
                NutritionRiskLevel riskLevel,
                Integer overallScore) {
            this.strategy = strategy;
            this.riskLevel = riskLevel;
            this.overallScore = overallScore;
        }

        public ImpactStrategy getStrategy() {
            return strategy;
        }

        public NutritionRiskLevel getRiskLevel() {
            return riskLevel;
        }

        public Integer getOverallScore() {
            return overallScore;
        }
    }

    /**
     * 对外的主方法：
     * 根据 MealHealthContext 决定 ImpactStrategy + NutritionRiskLevel。
     *
     * 示例规则（可调）：
     * - 有 VERY_HIGH_* 标签 => riskLevel = HIGH + FULL_RISK_ANALYSIS；
     * - 出现多个 HIGH_* 标签 => riskLevel = MODERATE/HIGH，通常 FULL_RISK_ANALYSIS；
     * - 只有 MEDIUM_* / LOW_* 时：
     * - 且 aiBalanced = true => riskLevel = MILD + LIGHT_TIPS；
     * - 且 aiBalanced = false => riskLevel = MODERATE + LIGHT_TIPS；
     * - 完全没有风险标签 => riskLevel = NONE + NONE（仅通用 tips）。
     */
    public ImpactDecision decide(MealHealthContext context) {
        Set<NutritionTag> tags = context.getNutritionTags();
        boolean aiBalanced = context.isAiBalanced();

        boolean hasVeryHigh = hasAnyVeryHigh(tags);
        boolean hasHigh = hasAnyHigh(tags);
        int highCount = countHighLevel(tags);
        boolean hasOnlyMild = hasOnlyMildIssues(tags);

        NutritionRiskLevel riskLevel;
        ImpactStrategy strategy;
        int score;

        if (tags.isEmpty()) {
            // 没有任何风险标签：整体安全
            riskLevel = NutritionRiskLevel.NONE;
            strategy = ImpactStrategy.NONE;
            score = aiBalanced ? 90 : 80;
        } else if (hasVeryHigh || highCount >= 2) {
            // 非常高 / 多个高风险：需要完整风险分析
            riskLevel = NutritionRiskLevel.HIGH;
            strategy = ImpactStrategy.FULL_RISK_ANALYSIS;
            score = aiBalanced ? 70 : 60; // AI 说均衡也只是结构均衡，不代表没风险
        } else if (hasHigh) {
            // 单个高风险或整体偏问题
            riskLevel = NutritionRiskLevel.MODERATE;
            strategy = ImpactStrategy.FULL_RISK_ANALYSIS;
            score = aiBalanced ? 75 : 65;
        } else if (hasOnlyMild) {
            // 只有轻微问题：用温和口吻做小提示
            riskLevel = aiBalanced ? NutritionRiskLevel.MILD : NutritionRiskLevel.MODERATE;
            strategy = ImpactStrategy.LIGHT_TIPS;
            score = aiBalanced ? 80 : 70;
        } else {
            // 兜底：有一些中等问题，但不算非常严重
            riskLevel = NutritionRiskLevel.MODERATE;
            strategy = aiBalanced ? ImpactStrategy.LIGHT_TIPS : ImpactStrategy.FULL_RISK_ANALYSIS;
            score = aiBalanced ? 75 : 65;
        }

        return new ImpactDecision(strategy, riskLevel, score);
    }

    // ======= 内部工具方法（需要根据 NutritionTag 实际枚举名做适配） =======

    private boolean hasAnyVeryHigh(Set<NutritionTag> tags) {
        return tags.stream().anyMatch(tag -> tag.name().startsWith("VERY_HIGH_") || tag.name().startsWith("VERY_LOW_"));
    }

    private boolean hasAnyHigh(Set<NutritionTag> tags) {
        return tags.stream().anyMatch(tag -> tag.name().startsWith("HIGH_") || tag.name().startsWith("LOW_"));
    }

    private int countHighLevel(Set<NutritionTag> tags) {
        return (int) tags.stream().filter(tag -> tag.name().startsWith("VERY_HIGH_") ||
                tag.name().startsWith("HIGH_") ||
                tag.name().startsWith("VERY_LOW_")).count();
    }

    // Helper method to check if tags only contain mild issues
    private boolean hasOnlyMildIssues(Set<NutritionTag> tags) {
        // This is a placeholder logic, adjust based on actual NutritionTag values
        // Assuming tags that are NOT HIGH or VERY_HIGH are mild
        return !hasAnyHigh(tags) && !hasAnyVeryHigh(tags);
    }
}
