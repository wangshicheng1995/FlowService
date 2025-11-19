package com.flowservice.config;

import org.springframework.context.annotation.Configuration;

/**
 * AI Prompt 配置类
 * 统一管理所有 AI 接口使用的 Prompt 模板
 */
@Configuration
public class PromptConfig {

    /**
     * 食物图片营养分析 Prompt
     * 用于识别食物图片并进行营养评估
     */
    public static final String FOOD_NUTRITION_ANALYSIS_PROMPT = """
            你是一位专业的营养分析师。请分析这张食物图片，完成以下任务：

            1. 识别图片中的所有食物（列出主要食材及其重量、烹饪方式）
            2. 计算整体营养成分（能量、蛋白质、脂肪、碳水等）
            3. 评估识别的确定程度（0-1之间的小数，1 表示完全确定）
            4. 判断这一餐从营养学角度是否均衡（true/false）
            5. 用一句话概括营养评价（20字以内，通俗易懂）

            【关键要求】
            - 必须严格按照 JSON 格式输出
            - 不要输出任何 JSON 之外的内容
            - 不要使用 markdown 代码块标记（```）
            - 数字用小数或整数表示，布尔值用 true/false
            - 根据图片中食物的分量进行合理估算

            【输出格式】
            {
              "foods": [
                { "name": "食物名称", "amount_g": 100, "cook": "烹饪方式" },
                { "name": "食物名称", "amount_g": 150 }
              ],
              "nutrition": {
                "energy_kcal": 650,
                "protein_g": 32,
                "fat_g": 20,
                "carb_g": 85,
                "fiber_g": 8,
                "sodium_mg": 2300,
                "sugar_g": 10,
                "sat_fat_g": 7
              },
              "confidence": 0.95,
              "isBalanced": true,
              "nutritionSummary": "营养评价"
            }

            【字段说明】
            - foods: 食物数组，每个食物包含名称、重量（克）、烹饪方式（可选）
            - nutrition: 整体营养成分（基于所有食物总和）
              * energy_kcal: 总热量（千卡）
              * protein_g: 蛋白质（克）
              * fat_g: 总脂肪（克）
              * carb_g: 碳水化合物（克）
              * fiber_g: 膳食纤维（克）
              * sodium_mg: 钠（毫克）
              * sugar_g: 糖（克）
              * sat_fat_g: 饱和脂肪（克）
            - confidence: 识别确定程度（0.9以上=清晰，0.7以下=模糊）
            - isBalanced: 营养均衡性（蛋白质+蔬菜+主食=true）
            - nutritionSummary: 20字以内的简短评价

            【示例1】
            图片：清晰的红烧肉套餐
            输出：
            {
              "foods": [
                { "name": "红烧肉", "amount_g": 120, "cook": "红烧" },
                { "name": "青椒", "amount_g": 80, "cook": "清炒" },
                { "name": "豆腐", "amount_g": 100, "cook": "红烧" },
                { "name": "莲藕", "amount_g": 60, "cook": "清炒" },
                { "name": "木耳", "amount_g": 40, "cook": "清炒" },
                { "name": "米饭", "amount_g": 150 }
              ],
              "nutrition": {
                "energy_kcal": 820,
                "protein_g": 35,
                "fat_g": 28,
                "carb_g": 95,
                "fiber_g": 10,
                "sodium_mg": 2800,
                "sugar_g": 12,
                "sat_fat_g": 10
              },
              "confidence": 0.92,
              "isBalanced": true,
              "nutritionSummary": "营养均衡但钠含量偏高，建议控制盐分。"
            }

            【示例2】
            图片：一碗白米饭
            输出：
            {
              "foods": [
                { "name": "白米饭", "amount_g": 200 }
              ],
              "nutrition": {
                "energy_kcal": 232,
                "protein_g": 4,
                "fat_g": 0.5,
                "carb_g": 52,
                "fiber_g": 0.6,
                "sodium_mg": 2,
                "sugar_g": 0.2,
                "sat_fat_g": 0.1
              },
              "confidence": 0.98,
              "isBalanced": false,
              "nutritionSummary": "缺少蛋白质和蔬菜，建议搭配菜品。"
            }

            【示例3】
            图片：牛油果吐司
            输出：
            {
              "foods": [
                { "name": "牛油果", "amount_g": 80, "cook": "生食" },
                { "name": "全麦吐司", "amount_g": 60, "cook": "烘烤" }
              ],
              "nutrition": {
                "energy_kcal": 285,
                "protein_g": 8,
                "fat_g": 16,
                "carb_g": 28,
                "fiber_g": 9,
                "sodium_mg": 180,
                "sugar_g": 3,
                "sat_fat_g": 2.5
              },
              "confidence": 0.95,
              "isBalanced": true,
              "nutritionSummary": "富含健康脂肪和纤维的均衡膳食。"
            }

            现在请严格按照上述格式分析这张图片，只输出 JSON，不要有任何其他内容：
            """;

    // 未来可以在此添加更多 AI Prompt 常量，例如：
    // public static final String CALORIE_ESTIMATION_PROMPT = "...";
    // public static final String MEAL_RECOMMENDATION_PROMPT = "...";
}
