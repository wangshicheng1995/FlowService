package com.flowservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 首页仪表盘聚合数据响应
 * 包含首页需要展示的所有后端数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDataResponse {

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 查询日期
     */
    private LocalDate date;

    /**
     * 食物压力分数（0-100）
     * 40 为初始值，低于 40 表示健康饮食为主，高于 40 表示高风险饮食较多
     */
    private Integer stressScore;

    /**
     * 当日总热量（kcal）
     */
    private Integer totalCalories;

    /**
     * 当日就餐次数
     */
    private Integer mealCount;

    // ==================== TODO: 待开发的指标 ====================

    // TODO: 营养均衡指数（0-100）
    // private Double nutritionBalanceIndex;

    // TODO: 饮食质量指数（0-100）
    // private Double dietQualityIndex;

    // TODO: 糖负荷（克）
    // private Double sugarLoadG;

    // TODO: 盐负荷（毫克）
    // private Double sodiumLoadMg;

    // TODO: 油脂摄入（克）
    // private Double fatIntakeG;

    // TODO: 蛋白质摄入（克）
    // private Double proteinIntakeG;

    // TODO: 膳食纤维摄入（克）
    // private Double fiberIntakeG;

    // TODO: 饱和脂肪摄入（克）
    // private Double satFatIntakeG;
}
