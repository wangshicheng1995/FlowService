package com.flowservice.service;

import com.flowservice.model.DailyCalorieResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Summary 页面服务接口
 * 提供统计概览相关的业务功能
 */
public interface SummaryService {

    /**
     * 获取用户在指定时间范围内的每日热量明细
     * 用于柱状图展示
     *
     * @param userId    用户 ID
     * @param startDate 开始日期（可选，默认为 7 天前）
     * @param endDate   结束日期（可选，默认为今天）
     * @return 每日热量列表
     */
    List<DailyCalorieResponse> getDailyCalories(String userId, LocalDate startDate, LocalDate endDate);
}
