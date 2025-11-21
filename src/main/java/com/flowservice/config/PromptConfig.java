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

  /**
   * 饮食对身体短中长期影响分析 Prompt
   * 基于已经计算好的营养数据 + 标签，让 AI 评估对身体的影响
   *
   * 使用方式建议：
   * - 用 {{NUTRITION_JSON}} 占位符传入一顿饭的营养数据（JSON 字符串）
   * - 用 {{TAGS_JSON}} 占位符传入你在后端算好的标签（JSON 字符串）
   */
  public static final String MEAL_IMPACT_ANALYSIS_PROMPT = """
      你是一名营养科医生和预防医学专家，现在需要根据一顿饭的营养数据和系统预先计算的标签，
      评估这顿饭对一个普通成年人的短期、中期、长期健康影响。

      注意：
      1. 只根据提供的数据进行合理推断，不要编造不存在的具体病名或夸张的数字。
      2. 使用通俗易懂、生活化的中文表述，例如“可能会感觉到…”、“风险会略有升高”等。
      3. 重点关注：能量、钠、糖、脂肪（尤其是饱和脂肪）、膳食纤维等指标。
      4. 根据标签判断重点风险，例如 high_sodium / high_sugar / high_sat_fat / low_fiber 等。
      5. 返回 JSON 时，不要输出任何解释性文字或多余内容。

      【本次进餐的营养数据（JSON）】
      {{NUTRITION_JSON}}

      【系统根据该营养数据计算出的标签（JSON）】
      {{TAGS_JSON}}

      请你综合以上信息，给出这顿饭对身体的：
      - 短期影响（一次或几天内的可能感受）
      - 中期影响（连续 1~4 周这样吃时的可能变化）
      - 长期风险（持续 3 个月甚至更久时的潜在风险）

      请严格按照下面这个 JSON 结构输出，不要添加任何额外字段，也不要有多余的文字说明：

      {
        "short_term": "字符串，描述短期可能的影响，例如：这顿饭的钠含量偏高，明天早上可能会轻微水肿，感觉有点口渴。",
        "mid_term": "字符串，描述连续 1~4 周这样吃的影响，例如：如果一周内多次这样吃，你的平均血压可能略有升高，更容易感到疲劳。",
        "long_term": "字符串，描述持续 3 个月及以上的风险，例如：如果长期保持这种饮食模式，心血管疾病和脂肪肝的风险会增加。",
        "risk_tags": [
          "根据本次分析总结出的风险标签，例如：high_sodium, low_fiber"
        ]
      }

      现在请只输出上述 JSON，不要包含任何多余文字。
      """;
}
