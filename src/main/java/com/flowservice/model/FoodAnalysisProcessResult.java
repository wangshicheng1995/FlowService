package com.flowservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 食物分析处理结果
 * 包含分析结果和保存后的记录 ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAnalysisProcessResult {

    /**
     * 食物分析结果
     */
    private FoodAnalysisResponse analysisResponse;

    /**
     * 保存到数据库后的用餐记录 ID
     */
    private Long mealRecordId;

    /**
     * 用户 ID
     */
    private String userId;
}
