package com.flowservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用餐营养信息实体类
 * 存储 AI 分析出的详细营养成分
 */
@Entity
@Table(name = "meal_nutrition")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealNutrition {

    /**
     * 关联 meal_records 主键
     */
    @Id
    @Column(name = "meal_id")
    private Long mealId;

    /**
     * 关联的主表对象
     */
    @OneToOne
    @MapsId
    @JoinColumn(name = "meal_id")
    private MealRecord mealRecord;

    /**
     * 总热量（千卡）
     */
    @Column(name = "energy_kcal")
    private Integer energyKcal;

    /**
     * 蛋白质（克）
     */
    @Column(name = "protein_g")
    private Integer proteinG;

    /**
     * 总脂肪（克）
     */
    @Column(name = "fat_g")
    private Integer fatG;

    /**
     * 碳水化合物（克）
     */
    @Column(name = "carb_g")
    private Integer carbG;

    /**
     * 膳食纤维（克）
     */
    @Column(name = "fiber_g")
    private Integer fiberG;

    /**
     * 钠（毫克）
     */
    @Column(name = "sodium_mg")
    private Integer sodiumMg;

    /**
     * 糖（克）
     */
    @Column(name = "sugar_g")
    private Integer sugarG;

    /**
     * 饱和脂肪（克）
     */
    @Column(name = "sat_fat_g")
    private Double satFatG;

    /**
     * 优质蛋白来源列表（JSON 数组格式）
     * 存储识别出的优质蛋白食材，如 ["鸡蛋", "鲈鱼", "虾仁"]
     */
    @Column(name = "high_quality_proteins", columnDefinition = "TEXT")
    private String highQualityProteins;
}
