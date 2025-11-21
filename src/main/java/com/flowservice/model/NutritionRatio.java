package com.flowservice.model;

import lombok.Data;

/**
 * 营养占比 DTO
 * 一顿饭在一天推荐量中的占比
 */
@Data
public class NutritionRatio {
    private double sodiumRatio; // 钠 / 日上限
    private double sugarRatio; // 糖 / 日上限
    private double satFatRatio; // 饱和脂肪 / 日上限
    private double fiberRatio; // 纤维 / 日推荐最低
}
