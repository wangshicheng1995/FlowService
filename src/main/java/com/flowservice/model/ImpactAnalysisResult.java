package com.flowservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * 饮食影响分析结果 DTO
 * 对应 AI 返回的短中长期影响分析
 */
@Data
public class ImpactAnalysisResult {

    /**
     * 短期影响（一次或几天内的可能感受）
     */
    @JsonProperty("short_term")
    private String shortTerm;

    /**
     * 中期影响（连续 1~4 周这样吃时的可能变化）
     */
    @JsonProperty("mid_term")
    private String midTerm;

    /**
     * 长期风险（持续 3 个月甚至更久时的潜在风险）
     */
    @JsonProperty("long_term")
    private String longTerm;

    /**
     * 风险标签列表
     */
    @JsonProperty("risk_tags")
    private List<String> riskTags;
}
