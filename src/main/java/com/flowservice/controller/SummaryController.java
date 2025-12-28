package com.flowservice.controller;

import com.flowservice.model.ApiResponse;
import com.flowservice.model.DailyCalorieResponse;
import com.flowservice.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Summary 页面控制器
 * 提供统计概览相关的 API 接口
 */
@Slf4j
@RestController
@RequestMapping("/summary")
@RequiredArgsConstructor
@Tag(name = "统计概览", description = "Summary 页面数据查询接口，包括每日热量明细、周/月统计等")
public class SummaryController {

    private final SummaryService summaryService;

    /**
     * 获取用户在指定时间范围内的每日热量明细
     * 用于柱状图展示
     *
     * @param userId    用户 ID（必填）
     * @param startDate 开始日期（可选，默认为 7 天前）
     * @param endDate   结束日期（可选，默认为今天）
     * @return 每日热量列表
     */
    @Operation(summary = "查询每日热量明细", description = "获取用户在指定时间范围内的每日热量数据，用于柱状图展示\n\n" +
            "如果不传日期参数，默认查询最近 7 天（包括今天）的数据\n\n" +
            "返回数据按日期升序排列，每条数据包含：\n" +
            "- date: 日期\n" +
            "- calories: 当日总热量（千卡）\n" +
            "- mealCount: 当日用餐次数")
    @GetMapping("/calories/daily")
    public ApiResponse<List<DailyCalorieResponse>> getDailyCalories(
            @Parameter(description = "用户 ID（必填），支持 Apple ID 格式", required = true, example = "000514.xxx.1422") @RequestParam String userId,
            @Parameter(description = "开始日期，格式 yyyy-MM-dd，默认为 7 天前", example = "2025-12-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd，默认为今天", example = "2025-12-26") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("查询每日热量明细: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        List<DailyCalorieResponse> response = summaryService.getDailyCalories(userId, startDate, endDate);
        return ApiResponse.success("查询成功", response);
    }
}
