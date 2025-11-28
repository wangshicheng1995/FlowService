package com.flowservice.model;

import lombok.Data;

/**
 * 包含旧版和新版 Impact 分析结果的容器
 */
@Data
public class FullImpactAnalysisResult {
    private ImpactAnalysisResult legacyResult;
    private FoodAnalysisResponse.OverallEvaluation overallEvaluation;
    private FoodAnalysisResponse.Impact impact;
}
