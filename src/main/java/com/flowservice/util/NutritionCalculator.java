package com.flowservice.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

/**
 * 营养目标计算器
 * 根据用户的身体数据和健康目标计算每日营养摄入目标
 * 
 * 计算公式说明：
 * 1. BMR (基础代谢率) 使用 Mifflin-St Jeor 公式（被认为是最准确的）
 * 2. TDEE (每日总能量消耗) = BMR × 活动系数
 * 3. 目标热量 = TDEE ± 调整值（根据健康目标）
 * 4. 宏量营养素根据目标热量按比例分配
 */
@Slf4j
public class NutritionCalculator {

    /**
     * 活动水平系数
     * 参考：Harris-Benedict 活动因子
     */
    private static final double ACTIVITY_SEDENTARY = 1.2; // 久坐不动
    private static final double ACTIVITY_LIGHT = 1.375; // 轻度活动（每周1-3天）
    private static final double ACTIVITY_MODERATE = 1.55; // 中度活动（每周3-5天）
    private static final double ACTIVITY_ACTIVE = 1.725; // 高度活动（每周6-7天）
    private static final double ACTIVITY_VERY_ACTIVE = 1.9; // 非常活跃（体力劳动或专业运动员）

    /**
     * 健康目标的热量调整值（千卡/天）
     */
    private static final int CALORIE_DEFICIT_FOR_WEIGHT_LOSS = 500; // 减重：每日减少 500 千卡（约每周减 0.5kg）
    private static final int CALORIE_SURPLUS_FOR_WEIGHT_GAIN = 300; // 增重：每日增加 300 千卡

    /**
     * 宏量营养素热量系数
     */
    private static final double CALORIES_PER_GRAM_PROTEIN = 4.0;
    private static final double CALORIES_PER_GRAM_CARB = 4.0;
    private static final double CALORIES_PER_GRAM_FAT = 9.0;

    /**
     * 宏量营养素比例（根据不同目标）
     * 格式：{蛋白质比例, 碳水比例, 脂肪比例}
     */
    private static final double[] MACRO_RATIO_BALANCED = { 0.25, 0.50, 0.25 }; // 均衡饮食
    private static final double[] MACRO_RATIO_WEIGHT_LOSS = { 0.30, 0.40, 0.30 }; // 减重：高蛋白
    private static final double[] MACRO_RATIO_WEIGHT_GAIN = { 0.25, 0.55, 0.20 }; // 增重：高碳水
    private static final double[] MACRO_RATIO_BLOOD_SUGAR = { 0.30, 0.35, 0.35 }; // 控糖：低碳水

    /**
     * 最低热量限制（保护健康）
     */
    private static final int MIN_CALORIES_MALE = 1500;
    private static final int MIN_CALORIES_FEMALE = 1200;

    /**
     * 营养目标计算结果
     */
    @Data
    @AllArgsConstructor
    public static class NutritionTargets {
        private int targetCalories; // 目标热量（千卡）
        private int targetProtein; // 目标蛋白质（克）
        private int targetCarb; // 目标碳水（克）
        private int targetFat; // 目标脂肪（克）
        private double bmr; // 基础代谢率（用于调试）
        private double tdee; // 每日总能量消耗（用于调试）
    }

    /**
     * 计算用户的营养目标
     *
     * @param gender        性别：male/female/other
     * @param birthYear     出生年份
     * @param heightCm      身高（厘米）
     * @param weightKg      体重（公斤）
     * @param activityLevel 活动水平：sedentary/light/moderate/active/veryActive
     * @param healthGoal    健康目标：loseWeight/maintain/gainWeight/improveHealth/controlBloodSugar
     * @return 计算出的营养目标
     */
    public static NutritionTargets calculate(
            String gender,
            Integer birthYear,
            Double heightCm,
            Double weightKg,
            String activityLevel,
            String healthGoal) {

        // 参数校验
        if (gender == null || birthYear == null || heightCm == null ||
                weightKg == null || activityLevel == null || healthGoal == null) {
            log.warn("营养目标计算参数不完整，使用默认值");
            return getDefaultTargets();
        }

        // 1. 计算年龄
        int currentYear = LocalDate.now().getYear();
        int age = currentYear - birthYear;

        // 年龄合理性校验
        if (age < 0 || age > 150) {
            log.warn("年龄计算异常 ({}), 使用默认值", age);
            return getDefaultTargets();
        }

        // 2. 计算 BMR (基础代谢率) - Mifflin-St Jeor 公式
        double bmr = calculateBMR(gender, age, heightCm, weightKg);
        log.debug("BMR 计算完成: gender={}, age={}, height={}, weight={}, bmr={}",
                gender, age, heightCm, weightKg, bmr);

        // 3. 计算 TDEE (每日总能量消耗)
        double activityFactor = getActivityFactor(activityLevel);
        double tdee = bmr * activityFactor;
        log.debug("TDEE 计算完成: activityLevel={}, factor={}, tdee={}",
                activityLevel, activityFactor, tdee);

        // 4. 根据健康目标调整热量
        int targetCalories = adjustCaloriesForGoal(tdee, healthGoal, gender);
        log.debug("目标热量调整完成: healthGoal={}, targetCalories={}", healthGoal, targetCalories);

        // 5. 计算宏量营养素目标
        double[] macroRatios = getMacroRatios(healthGoal);
        int targetProtein = (int) Math.round((targetCalories * macroRatios[0]) / CALORIES_PER_GRAM_PROTEIN);
        int targetCarb = (int) Math.round((targetCalories * macroRatios[1]) / CALORIES_PER_GRAM_CARB);
        int targetFat = (int) Math.round((targetCalories * macroRatios[2]) / CALORIES_PER_GRAM_FAT);

        log.info("营养目标计算完成: calories={}, protein={}g, carb={}g, fat={}g",
                targetCalories, targetProtein, targetCarb, targetFat);

        return new NutritionTargets(targetCalories, targetProtein, targetCarb, targetFat, bmr, tdee);
    }

    /**
     * 计算 BMR (基础代谢率)
     * 使用 Mifflin-St Jeor 公式（1990年提出，被认为比 Harris-Benedict 更准确）
     * 
     * 男性: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄(years) + 5
     * 女性: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄(years) - 161
     */
    private static double calculateBMR(String gender, int age, double heightCm, double weightKg) {
        double baseBMR = 10 * weightKg + 6.25 * heightCm - 5 * age;

        if ("male".equalsIgnoreCase(gender)) {
            return baseBMR + 5;
        } else if ("female".equalsIgnoreCase(gender)) {
            return baseBMR - 161;
        } else {
            // other 性别使用两者的平均值
            return baseBMR - 78;
        }
    }

    /**
     * 获取活动水平系数
     */
    private static double getActivityFactor(String activityLevel) {
        return switch (activityLevel.toLowerCase()) {
            case "sedentary" -> ACTIVITY_SEDENTARY;
            case "light" -> ACTIVITY_LIGHT;
            case "moderate" -> ACTIVITY_MODERATE;
            case "active" -> ACTIVITY_ACTIVE;
            case "veryactive" -> ACTIVITY_VERY_ACTIVE;
            default -> ACTIVITY_MODERATE; // 默认中等活动
        };
    }

    /**
     * 根据健康目标调整热量
     */
    private static int adjustCaloriesForGoal(double tdee, String healthGoal, String gender) {
        int adjustedCalories;

        switch (healthGoal.toLowerCase()) {
            case "loseweight":
                // 减重：减少 500 千卡
                adjustedCalories = (int) Math.round(tdee - CALORIE_DEFICIT_FOR_WEIGHT_LOSS);
                break;
            case "gainweight":
                // 增重：增加 300 千卡
                adjustedCalories = (int) Math.round(tdee + CALORIE_SURPLUS_FOR_WEIGHT_GAIN);
                break;
            case "maintain":
            case "improvehealth":
            case "controlbloodsugar":
            default:
                // 维持/改善健康/控糖：保持 TDEE
                adjustedCalories = (int) Math.round(tdee);
                break;
        }

        // 应用最低热量限制（保护健康）
        int minCalories = "female".equalsIgnoreCase(gender) ? MIN_CALORIES_FEMALE : MIN_CALORIES_MALE;
        if (adjustedCalories < minCalories) {
            log.warn("计算出的热量 ({}) 低于安全最低值 ({}), 使用最低值", adjustedCalories, minCalories);
            adjustedCalories = minCalories;
        }

        return adjustedCalories;
    }

    /**
     * 根据健康目标获取宏量营养素比例
     * 返回格式：{蛋白质比例, 碳水比例, 脂肪比例}
     */
    private static double[] getMacroRatios(String healthGoal) {
        return switch (healthGoal.toLowerCase()) {
            case "loseweight" -> MACRO_RATIO_WEIGHT_LOSS;
            case "gainweight" -> MACRO_RATIO_WEIGHT_GAIN;
            case "controlbloodsugar" -> MACRO_RATIO_BLOOD_SUGAR;
            default -> MACRO_RATIO_BALANCED;
        };
    }

    /**
     * 获取默认营养目标（当计算失败时使用）
     */
    private static NutritionTargets getDefaultTargets() {
        return new NutritionTargets(2000, 60, 250, 65, 0, 0);
    }
}
