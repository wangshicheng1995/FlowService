package com.flowservice.util;

import com.flowservice.util.NutritionCalculator.NutritionTargets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NutritionCalculator 单元测试
 */
class NutritionCalculatorTest {

    @Test
    @DisplayName("成年男性减重目标 - 计算营养目标")
    void testMaleWeightLoss() {
        // 30岁男性，175cm，75kg，中等活动，减重目标
        NutritionTargets targets = NutritionCalculator.calculate(
                "male",
                1995, // 出生年份
                175.0, // 身高
                75.0, // 体重
                "moderate",
                "loseWeight");

        assertNotNull(targets);

        // BMR ≈ 10*75 + 6.25*175 - 5*30 + 5 = 750 + 1093.75 - 150 + 5 = 1698.75
        // TDEE = BMR * 1.55 ≈ 2633
        // 目标热量 = TDEE - 500 ≈ 2133
        assertTrue(targets.getTargetCalories() > 1800 && targets.getTargetCalories() < 2400,
                "目标热量应在合理范围内: " + targets.getTargetCalories());

        // 减重比例: 蛋白质30%, 碳水40%, 脂肪30%
        assertTrue(targets.getTargetProtein() > 100,
                "蛋白质目标应大于100g: " + targets.getTargetProtein());
        assertTrue(targets.getTargetCarb() > 150,
                "碳水目标应大于150g: " + targets.getTargetCarb());
        assertTrue(targets.getTargetFat() > 50,
                "脂肪目标应大于50g: " + targets.getTargetFat());

        System.out.println("男性减重目标计算结果:");
        System.out.println("  BMR: " + targets.getBmr());
        System.out.println("  TDEE: " + targets.getTdee());
        System.out.println("  目标热量: " + targets.getTargetCalories() + " kcal");
        System.out.println("  蛋白质: " + targets.getTargetProtein() + "g");
        System.out.println("  碳水: " + targets.getTargetCarb() + "g");
        System.out.println("  脂肪: " + targets.getTargetFat() + "g");
    }

    @Test
    @DisplayName("成年女性维持体重目标 - 计算营养目标")
    void testFemaleMaintain() {
        // 28岁女性，162cm，55kg，轻度活动，维持目标
        NutritionTargets targets = NutritionCalculator.calculate(
                "female",
                1997,
                162.0,
                55.0,
                "light",
                "maintain");

        assertNotNull(targets);

        // BMR ≈ 10*55 + 6.25*162 - 5*28 - 161 = 550 + 1012.5 - 140 - 161 = 1261.5
        // TDEE = BMR * 1.375 ≈ 1735
        assertTrue(targets.getTargetCalories() > 1500 && targets.getTargetCalories() < 2000,
                "目标热量应在合理范围内: " + targets.getTargetCalories());

        System.out.println("\n女性维持体重目标计算结果:");
        System.out.println("  BMR: " + targets.getBmr());
        System.out.println("  TDEE: " + targets.getTdee());
        System.out.println("  目标热量: " + targets.getTargetCalories() + " kcal");
        System.out.println("  蛋白质: " + targets.getTargetProtein() + "g");
        System.out.println("  碳水: " + targets.getTargetCarb() + "g");
        System.out.println("  脂肪: " + targets.getTargetFat() + "g");
    }

    @Test
    @DisplayName("成年男性增重目标 - 计算营养目标")
    void testMaleWeightGain() {
        // 25岁男性，180cm，65kg，高度活动，增重目标
        NutritionTargets targets = NutritionCalculator.calculate(
                "male",
                2000,
                180.0,
                65.0,
                "active",
                "gainWeight");

        assertNotNull(targets);

        // 增重应该比维持热量更高
        assertTrue(targets.getTargetCalories() > 2500,
                "增重目标热量应较高: " + targets.getTargetCalories());

        // 增重比例: 蛋白质25%, 碳水55%, 脂肪20%
        // 碳水占比较高
        assertTrue(targets.getTargetCarb() > targets.getTargetProtein(),
                "增重时碳水应高于蛋白质");

        System.out.println("\n男性增重目标计算结果:");
        System.out.println("  BMR: " + targets.getBmr());
        System.out.println("  TDEE: " + targets.getTdee());
        System.out.println("  目标热量: " + targets.getTargetCalories() + " kcal");
        System.out.println("  蛋白质: " + targets.getTargetProtein() + "g");
        System.out.println("  碳水: " + targets.getTargetCarb() + "g");
        System.out.println("  脂肪: " + targets.getTargetFat() + "g");
    }

    @Test
    @DisplayName("控糖目标 - 低碳水高蛋白")
    void testBloodSugarControl() {
        // 45岁女性，165cm，70kg，久坐，控糖目标
        NutritionTargets targets = NutritionCalculator.calculate(
                "female",
                1980,
                165.0,
                70.0,
                "sedentary",
                "controlBloodSugar");

        assertNotNull(targets);

        // 控糖比例: 蛋白质30%, 碳水35%, 脂肪35%
        // 碳水占比较低
        double carbCalories = targets.getTargetCarb() * 4.0;
        double proteinCalories = targets.getTargetProtein() * 4.0;

        // 蛋白质热量应接近或高于碳水热量（控糖饮食特点）
        assertTrue(Math.abs(proteinCalories - carbCalories) < 200,
                "控糖饮食时蛋白质和碳水热量应接近");

        System.out.println("\n控糖目标计算结果:");
        System.out.println("  BMR: " + targets.getBmr());
        System.out.println("  TDEE: " + targets.getTdee());
        System.out.println("  目标热量: " + targets.getTargetCalories() + " kcal");
        System.out.println("  蛋白质: " + targets.getTargetProtein() + "g");
        System.out.println("  碳水: " + targets.getTargetCarb() + "g");
        System.out.println("  脂肪: " + targets.getTargetFat() + "g");
    }

    @Test
    @DisplayName("最低热量保护 - 确保不低于安全值")
    void testMinimumCalorieProtection() {
        // 极端情况：非常瘦的女性，低活动，减重
        NutritionTargets targets = NutritionCalculator.calculate(
                "female",
                2000,
                155.0,
                40.0, // 体重很轻
                "sedentary",
                "loseWeight");

        assertNotNull(targets);

        // 女性最低热量保护：1200 kcal
        assertTrue(targets.getTargetCalories() >= 1200,
                "女性热量不应低于1200: " + targets.getTargetCalories());

        System.out.println("\n最低热量保护测试:");
        System.out.println("  目标热量: " + targets.getTargetCalories() + " kcal (最低1200)");
    }

    @Test
    @DisplayName("参数缺失时返回默认值")
    void testNullParameters() {
        NutritionTargets targets = NutritionCalculator.calculate(
                null, // 缺失性别
                1990,
                170.0,
                70.0,
                "moderate",
                "maintain");

        assertNotNull(targets);
        assertEquals(2000, targets.getTargetCalories(), "缺失参数时应返回默认热量");
        assertEquals(60, targets.getTargetProtein(), "缺失参数时应返回默认蛋白质");
    }

    @Test
    @DisplayName("Other性别使用平均值计算")
    void testOtherGender() {
        NutritionTargets targetsOther = NutritionCalculator.calculate(
                "other",
                1990,
                170.0,
                70.0,
                "moderate",
                "maintain");

        NutritionTargets targetsMale = NutritionCalculator.calculate(
                "male",
                1990,
                170.0,
                70.0,
                "moderate",
                "maintain");

        NutritionTargets targetsFemale = NutritionCalculator.calculate(
                "female",
                1990,
                170.0,
                70.0,
                "moderate",
                "maintain");

        // Other 的热量应该在 Male 和 Female 之间
        assertTrue(targetsOther.getTargetCalories() < targetsMale.getTargetCalories());
        assertTrue(targetsOther.getTargetCalories() > targetsFemale.getTargetCalories());

        System.out.println("\nOther性别计算结果:");
        System.out.println("  Male热量: " + targetsMale.getTargetCalories());
        System.out.println("  Other热量: " + targetsOther.getTargetCalories());
        System.out.println("  Female热量: " + targetsFemale.getTargetCalories());
    }
}
