package com.flowservice.util;

import com.flowservice.config.NutritionGuidelineConfig;
import com.flowservice.entity.MealNutrition;
import com.flowservice.model.NutritionRatio;
import com.flowservice.model.NutritionTag;

import java.util.HashSet;
import java.util.Set;

/**
 * 健康标签计算器
 */
public class HealthTagCalculator {

    public static NutritionRatio calcRatio(MealNutrition n) {
        NutritionRatio r = new NutritionRatio();
        r.setSodiumRatio(
                n.getSodiumMg() != null ? n.getSodiumMg() / NutritionGuidelineConfig.SODIUM_DAILY_LIMIT_MG : 0);
        r.setSugarRatio(n.getSugarG() != null ? n.getSugarG() / NutritionGuidelineConfig.SUGAR_DAILY_LIMIT_G : 0);
        r.setSatFatRatio(n.getSatFatG() != null ? n.getSatFatG() / NutritionGuidelineConfig.SAT_FAT_DAILY_LIMIT_G : 0);
        r.setFiberRatio(n.getFiberG() != null ? n.getFiberG() / NutritionGuidelineConfig.FIBER_DAILY_MIN_G : 0);
        return r;
    }

    public static Set<NutritionTag> calcTags(com.flowservice.entity.MealRecord record) {
        Set<NutritionTag> tags = new HashSet<>();

        // 1. Calculate tags from nutrition info if available
        if (record.getMealNutrition() != null) {
            tags.addAll(calcTags(record.getMealNutrition()));
        }

        // 2. Add BALANCED_MEAL if isBalanced is true
        if (Boolean.TRUE.equals(record.getIsBalanced())) {
            tags.add(NutritionTag.BALANCED_MEAL);
        }

        // 3. Add GENERIC_HIGH_RISK if riskLevel is HIGH
        if ("HIGH".equalsIgnoreCase(record.getRiskLevel())) {
            tags.add(NutritionTag.GENERIC_HIGH_RISK);
        }

        return tags;
    }

    public static Set<NutritionTag> calcTags(MealNutrition n) {
        Set<NutritionTag> tags = new HashSet<>();

        // ---------- 钠 ----------
        double sodium = n.getSodiumMg() != null ? n.getSodiumMg() : 0;
        if (sodium >= 2000) {
            tags.add(NutritionTag.VERY_HIGH_SODIUM);
        } else if (sodium >= 1000) {
            tags.add(NutritionTag.HIGH_SODIUM);
        } else if (sodium >= 600) {
            tags.add(NutritionTag.MEDIUM_SODIUM);
        } else {
            tags.add(NutritionTag.LOW_SODIUM);
        }

        // ---------- 糖 ----------
        double sugar = n.getSugarG() != null ? n.getSugarG() : 0;
        if (sugar >= 40) {
            tags.add(NutritionTag.VERY_HIGH_SUGAR);
        } else if (sugar >= 25) {
            tags.add(NutritionTag.HIGH_SUGAR);
        } else if (sugar >= 12) {
            tags.add(NutritionTag.MEDIUM_SUGAR);
        } else {
            tags.add(NutritionTag.LOW_SUGAR);
        }

        // ---------- 饱和脂肪 ----------
        double satFat = n.getSatFatG() != null ? n.getSatFatG() : 0;
        if (satFat >= 20) {
            tags.add(NutritionTag.VERY_HIGH_SAT_FAT);
        } else if (satFat >= 10) {
            tags.add(NutritionTag.HIGH_SAT_FAT);
        } else if (satFat >= 5) {
            tags.add(NutritionTag.MEDIUM_SAT_FAT);
        } else {
            tags.add(NutritionTag.LOW_SAT_FAT);
        }

        // ---------- 纤维 ----------
        double fiber = n.getFiberG() != null ? n.getFiberG() : 0;
        if (fiber < 4) {
            tags.add(NutritionTag.VERY_LOW_FIBER);
        } else if (fiber < 8) {
            tags.add(NutritionTag.LOW_FIBER);
        } else if (fiber < 12) {
            tags.add(NutritionTag.MEDIUM_FIBER);
        } else {
            tags.add(NutritionTag.HIGH_FIBER);
        }

        // ---------- 新增保护标签计算逻辑 (MVP 简化版) ----------

        // HIGH_FIBER_MEAL: 纤维 > 10g
        if (fiber > 10) {
            tags.add(NutritionTag.HIGH_FIBER_MEAL);
        }

        // VEGETABLE_RICH: 纤维 > 8g 且 热量 < 400 (假设)
        double energy = n.getEnergyKcal() != null ? n.getEnergyKcal() : 0;
        if (fiber > 8 && energy < 400) {
            tags.add(NutritionTag.VEGETABLE_RICH);
        }

        // LEAN_PROTEIN: 蛋白质 > 20g 且 脂肪 < 10g
        double protein = n.getProteinG() != null ? n.getProteinG() : 0;
        double fat = n.getFatG() != null ? n.getFatG() : 0;
        if (protein > 20 && fat < 10) {
            tags.add(NutritionTag.LEAN_PROTEIN);
        }

        // BALANCED_MEAL: 蛋白质 > 15g, 纤维 > 5g, 脂肪 < 20g
        if (protein > 15 && fiber > 5 && fat < 20) {
            tags.add(NutritionTag.BALANCED_MEAL);
        }

        return tags;
    }
}
