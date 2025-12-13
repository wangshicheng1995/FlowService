package com.flowservice.service;

import com.flowservice.model.CalorieStatisticsResponse;
import com.flowservice.model.DashboardDataResponse;

import java.time.LocalDate;

/**
 * 首页服务接口
 * 提供用户首页相关的业务功能
 */
public interface HomeService {

    /**
     * 获取用户在指定时间范围内的食物总热量
     * 
     * @param userId    用户 ID
     * @param startDate 开始日期（可选，默认为当天）
     * @param endDate   结束日期（可选，默认为当天）
     * @return 热量统计结果
     */
    CalorieStatisticsResponse getTotalCalories(String userId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取首页仪表盘聚合数据
     * 一次性返回首页需要的所有后端数据
     *
     * @param userId 用户 ID
     * @param date   查询日期（可选，默认为当天）
     * @return 仪表盘聚合数据
     */
    DashboardDataResponse getDashboardData(String userId, LocalDate date);
}
