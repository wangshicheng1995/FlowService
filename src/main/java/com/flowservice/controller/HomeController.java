package com.flowservice.controller;

import com.flowservice.model.ApiResponse;
import com.flowservice.model.CalorieStatisticsResponse;
import com.flowservice.model.HealthStressScoreResponse;
import com.flowservice.service.HealthStressService;
import com.flowservice.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 首页控制器
 * 提供首页相关的 API 接口
 */
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@Tag(name = "首页", description = "首页数据查询接口，包括健康压力分数、热量统计等")
public class HomeController {

    private final HealthStressService healthStressService;
    private final HomeService homeService;

    /**
     * 获取健康压力分数
     * 从 HealthStressController 迁移而来
     *
     * @param userId 用户 ID（可选，默认为 1L）
     * @param date   日期（可选，默认为当天）
     * @return 健康压力分数响应
     */
    @Operation(summary = "获取健康压力分数", description = "根据用户当日的餐食记录计算健康压力值（范围 0-100）\n\n" +
            "- 40: 默认初始值\n" +
            "- < 40: 健康饮食为主\n" +
            "- > 40: 高风险饮食较多")
    @GetMapping("/stress-score")
    public HealthStressScoreResponse getHealthStressScore(
            @Parameter(description = "用户 ID，支持 Apple ID 格式", example = "000514.xxx.1422") @RequestParam(required = false) String userId,
            @Parameter(description = "查询日期，格式 yyyy-MM-dd，默认为当天", example = "2024-12-12") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        // 临时逻辑：如果没传 userId，使用默认 ID
        String targetUserId = (userId != null) ? userId : "default_user";

        int score = healthStressService.calculateDailyScore(targetUserId, targetDate);
        return new HealthStressScoreResponse(targetUserId, score);
    }

    /**
     * 获取用户在指定时间范围内的食物总热量
     *
     * @param userId    用户 ID（必填）
     * @param startDate 开始日期（可选，默认为当天）
     * @param endDate   结束日期（可选，默认为当天）
     * @return 热量统计响应，包装在 ApiResponse 中
     */
    @Operation(summary = "查询热量统计", description = "获取用户在指定时间范围内的食物总热量\n\n" +
            "如果不传日期参数，默认查询当天的数据")
    @GetMapping("/calories")
    public ApiResponse<CalorieStatisticsResponse> getTotalCalories(
            @Parameter(description = "用户 ID（必填），支持 Apple ID 格式", required = true, example = "000514.xxx.1422") @RequestParam String userId,
            @Parameter(description = "开始日期，格式 yyyy-MM-dd", example = "2025-12-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd", example = "2025-12-31") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        CalorieStatisticsResponse response = homeService.getTotalCalories(userId, startDate, endDate);
        return ApiResponse.success("查询成功", response);
    }
}
