package com.flowservice.model;

/**
 * 营养标签枚举
 */
public enum NutritionTag {
    // 钠
    VERY_HIGH_SODIUM,
    HIGH_SODIUM,
    MEDIUM_SODIUM,
    LOW_SODIUM,

    // 糖
    VERY_HIGH_SUGAR,
    HIGH_SUGAR,
    MEDIUM_SUGAR,
    LOW_SUGAR,

    // 饱和脂肪
    VERY_HIGH_SAT_FAT,
    HIGH_SAT_FAT,
    MEDIUM_SAT_FAT,
    LOW_SAT_FAT,

    // 纤维
    VERY_LOW_FIBER,
    LOW_FIBER,
    MEDIUM_FIBER,
    HIGH_FIBER,

    // 风险标签 (新增)
    GENERIC_HIGH_RISK, // 通用高风险（当 riskLevel=HIGH 但无详细营养数据时）
    HIGH_ENERGY_DENSE,
    PROCESSED_MEAT,
    DEEP_FRIED,
    SUGARY_DRINK,

    // 保护标签 (新增)
    HIGH_FIBER_MEAL,
    VEGETABLE_RICH,
    LEAN_PROTEIN,
    BALANCED_MEAL
}
