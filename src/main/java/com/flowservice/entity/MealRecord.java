package com.flowservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用餐记录实体类
 * 存储用户的食物摄入记录和 AI 分析结果
 */
@Entity
@Table(name = "meal_records", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_eaten_at", columnList = "eaten_at"),
        @Index(name = "idx_health_score", columnList = "health_score")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealRecord {

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 用户 ID（支持 Apple ID 格式，如 000514.xxx.1422）
     */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /**
     * 吃这顿饭的时间（前端上传）
     */
    @Column(name = "eaten_at", nullable = false)
    private LocalDateTime eatenAt;

    /**
     * 来源：PHOTO / TEXT / VOICE 等，方便后面分析
     */
    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    /**
     * 食物照片在你对象存储里的地址，没图就 NULL
     */
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    /**
     * 0-100，用来画趋势图、做平均值
     */
    @Column(name = "health_score")
    private Integer healthScore;

    /**
     * LOW / MEDIUM / HIGH，快速过滤高风险餐
     */
    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    /**
     * 用户备注，可选
     */
    @Column(name = "note")
    private String note;

    /**
     * 模型返回的结构化结果原文（包括受影响器官、短中长期影响等）
     * 存储完整的 AI 返回 JSON
     */
    @Column(name = "ai_result_json", columnDefinition = "TEXT")
    private String aiResultJson;

    /**
     * 识别出的食物列表（JSON 数组格式）
     * AI 返回字段
     */
    @Column(name = "food_items", columnDefinition = "TEXT")
    private String foodItems;

    /**
     * 识别确定程度（0-1之间的小数）
     * AI 返回字段
     */
    @Column(name = "confidence")
    private Double confidence;

    /**
     * 营养是否均衡
     */
    @Column(name = "is_balanced")
    private Boolean isBalanced;

    /**
     * 营养评价概括（20字以内）
     */
    @Column(name = "nutrition_summary")
    private String nutritionSummary;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 营养详细信息
     */
    @OneToOne(mappedBy = "mealRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MealNutrition mealNutrition;
}
