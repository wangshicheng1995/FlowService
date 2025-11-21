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

        return tags;
    }
}
